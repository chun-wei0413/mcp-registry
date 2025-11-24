"""
Vector Store Service for knowledge management.

Handles embedding generation, storage, and retrieval using ChromaDB.
"""
import chromadb
from sentence_transformers import SentenceTransformer
import uuid
import json
from datetime import datetime
from typing import List, Dict, Any, Optional
from utils.markdown_parser import MarkdownParser


class VectorStoreService:
    """
    Service for managing vector storage with ChromaDB and sentence embeddings.
    Provides knowledge storage, retrieval, and intelligent chunking capabilities.
    """
    def __init__(self,
                 db_path: str = "./chroma_db",
                 collection_name: str = "mcp_knowledge_base",
                 embedding_model: str = "paraphrase-multilingual-MiniLM-L12-v2"):
        """
        Initializes the VectorStore.

        Args:
            db_path (str): The path to the directory where the database will be persisted.
            collection_name (str): The name of the collection to use.
            embedding_model (str): The SentenceTransformer model name.
                                  Default: paraphrase-multilingual-MiniLM-L12-v2 (supports multilingual)
        """
        self.db_client = chromadb.PersistentClient(path=db_path)
        self.collection = self.db_client.get_or_create_collection(
            name=collection_name,
            metadata={"hnsw:space": "cosine"}  # Use cosine similarity
        )
        self.model = SentenceTransformer(embedding_model)
        print(f"[OK] Loaded embedding model: {embedding_model}")

    @staticmethod
    def _format_result(doc_id: str, content: str, metadata: Dict[str, Any],
                      similarity: float = None) -> Dict[str, Any]:
        """Format a result dictionary from ChromaDB query."""
        # Parse code_blocks from metadata if present
        code_blocks = None
        if "code_blocks" in metadata:
            try:
                code_blocks = json.loads(metadata["code_blocks"])
            except json.JSONDecodeError:
                pass

        result = {
            "id": doc_id,
            "content": content,
            "topic": metadata.get("topic"),
            "timestamp": metadata.get("timestamp")
        }

        if similarity is not None:
            result["similarity"] = similarity

        # Add optional metadata fields
        for field in ["file_path", "section_title", "chunk_type"]:
            if field in metadata and metadata[field]:
                result[field] = metadata[field]

        if code_blocks:
            result["code_blocks"] = code_blocks

        return result

    def add_knowledge(self, topic: str, content: str) -> str:
        """
        Adds a new knowledge point to the vector store.

        Args:
            topic (str): The topic associated with the knowledge.
            content (str): The text content of the knowledge.

        Returns:
            str: The unique ID of the stored document.
        """
        doc_id = str(uuid.uuid4())
        timestamp = datetime.utcnow().isoformat()
        embedding = self.model.encode(content).tolist()

        self.collection.add(
            ids=[doc_id],
            embeddings=[embedding],
            documents=[content],
            metadatas=[{"topic": topic, "timestamp": timestamp}]
        )
        return doc_id

    def search_knowledge(self, query: str, top_k: int, topic: str = None) -> List[Dict[str, Any]]:
        """
        Performs a semantic search on the vector store.

        Args:
            query (str): The natural language query.
            top_k (int): The number of top results to return.
            topic (str, optional): A topic to filter the search. Defaults to None.

        Returns:
            List[Dict[str, Any]]: A list of result dictionaries with code blocks.
        """
        query_embedding = self.model.encode(query).tolist()

        query_params = {
            "query_embeddings": [query_embedding],
            "n_results": top_k
        }
        if topic:
            query_params["where"] = {"topic": topic}

        results = self.collection.query(**query_params)

        if not results or not results["ids"][0]:
            return []

        formatted_results = [
            self._format_result(
                doc_id=doc_id,
                content=results["documents"][0][i],
                metadata=results["metadatas"][0][i],
                similarity=results["distances"][0][i]
            )
            for i, doc_id in enumerate(results["ids"][0])
        ]
        return formatted_results

    def get_all_by_topic(self, topic: str) -> List[Dict[str, Any]]:
        """
        Retrieves all knowledge points for a specific topic.

        Args:
            topic (str): The topic to retrieve.

        Returns:
            List[Dict[str, Any]]: A list of result dictionaries with code blocks.
        """
        results = self.collection.get(where={"topic": topic})

        if not results or not results["ids"]:
            return []

        formatted_results = [
            self._format_result(
                doc_id=doc_id,
                content=results["documents"][i],
                metadata=results["metadatas"][i]
            )
            for i, doc_id in enumerate(results["ids"])
        ]
        return formatted_results

    def add_knowledge_with_chunking(self,
                                     content: str,
                                     metadata: Dict[str, Any],
                                     chunk_size: int = 4000,
                                     chunk_overlap: int = 200) -> List[str]:
        """
        Adds knowledge with intelligent chunking strategy.

        Args:
            content (str): The text content to store
            metadata (Dict[str, Any]): Metadata for the content (must include 'topic')
            chunk_size (int): Maximum characters per chunk
            chunk_overlap (int): Overlap between chunks for context preservation

        Returns:
            List[str]: List of document IDs created
        """
        file_path = metadata.get("file_path", "")
        file_ext = Path(file_path).suffix if file_path else ""

        # Determine chunking strategy based on file size and type
        if len(content) < 5000:
            # Small files: store as complete document
            return [self._add_single_chunk(content, metadata)]

        elif file_ext == ".md":
            # Markdown: split by headers
            return self._chunk_markdown(content, metadata, chunk_size)

        elif file_ext == ".java":
            # Java: split by class/methods (simplified for now)
            return self._chunk_code(content, metadata, chunk_size)

        else:
            # Generic: recursive character splitting
            return self._chunk_recursive(content, metadata, chunk_size, chunk_overlap)

    def _add_single_chunk(self, content: str, metadata: Dict[str, Any], code_blocks: List[Dict] = None) -> str:
        """
        Helper to add a single chunk to the vector store.

        Args:
            content: The text content (without code blocks) for embedding
            metadata: Metadata dictionary
            code_blocks: Optional list of code blocks associated with this chunk

        Returns:
            str: The document ID
        """
        doc_id = str(uuid.uuid4())
        timestamp = datetime.datetime.utcnow().isoformat()

        # IMPORTANT: Only embed the text content, NOT the code
        embedding = self.model.encode(content).tolist()

        # Prepare metadata
        full_metadata = {**metadata, "timestamp": timestamp, "chunk_type": "complete"}

        # Store code blocks as JSON string in metadata if provided
        if code_blocks:
            full_metadata["code_blocks"] = json.dumps(code_blocks)

        self.collection.add(
            ids=[doc_id],
            embeddings=[embedding],
            documents=[content],
            metadatas=[full_metadata]
        )
        return doc_id

    def _chunk_markdown(self, content: str, metadata: Dict[str, Any], max_size: int) -> List[str]:
        """
        Split markdown by headers with intelligent code block extraction.

        This method uses the MarkdownParser to:
        1. Separate code blocks from descriptive text
        2. Only embed the descriptive text (improves semantic search)
        3. Store code blocks in metadata for complete retrieval
        """
        doc_ids = []

        # Use intelligent chunking with code awareness
        chunks = MarkdownParser.chunk_with_code_awareness(content, max_size)

        for i, chunk in enumerate(chunks):
            chunk_metadata = {
                **metadata,
                "section_title": chunk['section_title'],
                "chunk_type": "section" if chunk['is_complete'] else "section_part",
            }

            # Add the chunk with separated code blocks
            # IMPORTANT: Only chunk['description'] is embedded, NOT the code
            doc_ids.append(
                self._add_single_chunk(
                    content=chunk['description'],
                    metadata=chunk_metadata,
                    code_blocks=chunk['code_blocks'] if chunk['code_blocks'] else None
                )
            )

        return doc_ids

    def _chunk_code(self, content: str, metadata: Dict[str, Any], max_size: int) -> List[str]:
        """Split code files (simplified - splits by size for now)."""
        # For simplicity, use recursive splitting for code
        # Advanced version could parse AST to split by class/method
        return self._chunk_recursive(content, metadata, max_size, chunk_overlap=100)

    def _chunk_recursive(self, content: str, metadata: Dict[str, Any],
                        chunk_size: int, chunk_overlap: int) -> List[str]:
        """Recursively split text into chunks with overlap."""
        doc_ids = []
        chunks = self._split_text_recursive(content, chunk_size, chunk_overlap)

        for i, chunk in enumerate(chunks):
            chunk_metadata = {
                **metadata,
                "chunk_type": "recursive",
                "part": i + 1,
                "total_parts": len(chunks)
            }
            doc_ids.append(self._add_single_chunk(chunk, chunk_metadata))

        return doc_ids

    def _split_text_recursive(self, text: str, chunk_size: int, chunk_overlap: int = 200) -> List[str]:
        """
        Recursively split text into chunks with overlap.
        Tries to split on paragraph boundaries first, then sentences.
        """
        if len(text) <= chunk_size:
            return [text]

        chunks = []
        separators = ['\n\n', '\n', '. ', ' ']

        start = 0
        while start < len(text):
            end = start + chunk_size

            if end >= len(text):
                chunks.append(text[start:].strip())
                break

            # Try to find a good split point
            split_point = end
            for separator in separators:
                pos = text.rfind(separator, start, end)
                if pos != -1:
                    split_point = pos + len(separator)
                    break

            chunks.append(text[start:split_point].strip())
            start = split_point - chunk_overlap if chunk_overlap > 0 else split_point

        return [c for c in chunks if c]  # Filter empty chunks
