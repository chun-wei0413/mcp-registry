"""
Vector Store Service for knowledge management.

Handles embedding generation, storage, and retrieval using ChromaDB.
"""
import chromadb
from sentence_transformers import SentenceTransformer
import uuid
import datetime
import re
from typing import List, Dict, Any, Optional
from pathlib import Path


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
        timestamp = datetime.datetime.utcnow().isoformat()
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
            List[Dict[str, Any]]: A list of result dictionaries.
        """
        query_embedding = self.model.encode(query).tolist()

        query_params = {
            "query_embeddings": [query_embedding],
            "n_results": top_k
        }
        if topic:
            query_params["where"] = {"topic": topic}

        results = self.collection.query(**query_params)

        # Format the results
        formatted_results = []
        if not results or not results["ids"][0]:
            return []

        for i, doc_id in enumerate(results["ids"][0]):
            metadata = results["metadatas"][0][i]
            formatted_results.append({
                "id": doc_id,
                "content": results["documents"][0][i],
                "topic": metadata.get("topic"),
                "similarity": results["distances"][0][i],
                "timestamp": metadata.get("timestamp")
            })
        
        return formatted_results

    def get_all_by_topic(self, topic: str) -> List[Dict[str, Any]]:
        """
        Retrieves all knowledge points for a specific topic.

        Args:
            topic (str): The topic to retrieve.

        Returns:
            List[Dict[str, Any]]: A list of result dictionaries.
        """
        results = self.collection.get(where={"topic": topic})

        # Format the results
        formatted_results = []
        if not results or not results["ids"]:
            return []

        for i, doc_id in enumerate(results["ids"]):
            metadata = results["metadatas"][i]
            formatted_results.append({
                "id": doc_id,
                "content": results["documents"][i],
                "topic": metadata.get("topic"),
                "timestamp": metadata.get("timestamp")
            })

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

    def _add_single_chunk(self, content: str, metadata: Dict[str, Any]) -> str:
        """Helper to add a single chunk to the vector store."""
        doc_id = str(uuid.uuid4())
        timestamp = datetime.datetime.utcnow().isoformat()
        embedding = self.model.encode(content).tolist()

        full_metadata = {**metadata, "timestamp": timestamp, "chunk_type": "complete"}

        self.collection.add(
            ids=[doc_id],
            embeddings=[embedding],
            documents=[content],
            metadatas=[full_metadata]
        )
        return doc_id

    def _chunk_markdown(self, content: str, metadata: Dict[str, Any], max_size: int) -> List[str]:
        """Split markdown by headers (H2, H3)."""
        doc_ids = []

        # Split by H2 headers
        sections = re.split(r'\n##\s+', content)

        for i, section in enumerate(sections):
            if not section.strip():
                continue

            # Extract section title
            lines = section.split('\n', 1)
            section_title = lines[0].strip() if lines else "Introduction"
            section_content = lines[1] if len(lines) > 1 else section

            # If section is still too large, split further
            if len(section_content) > max_size:
                sub_chunks = self._split_text_recursive(section_content, max_size)
                for j, chunk in enumerate(sub_chunks):
                    chunk_metadata = {
                        **metadata,
                        "section_title": section_title,
                        "chunk_type": "section_part",
                        "part": j + 1,
                        "total_parts": len(sub_chunks)
                    }
                    formatted_chunk = f"## {section_title} (Part {j+1})\n\n{chunk}"
                    doc_ids.append(self._add_single_chunk(formatted_chunk, chunk_metadata))
            else:
                chunk_metadata = {
                    **metadata,
                    "section_title": section_title,
                    "chunk_type": "section"
                }
                formatted_chunk = f"## {section_title}\n\n{section_content}"
                doc_ids.append(self._add_single_chunk(formatted_chunk, chunk_metadata))

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
