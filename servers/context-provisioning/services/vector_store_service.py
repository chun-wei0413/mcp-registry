"""
Vector Store Service for knowledge management.

Handles embedding generation, storage, and retrieval using ChromaDB.
"""
import chromadb
from sentence_transformers import SentenceTransformer
import torch
import uuid
import json
from datetime import datetime
from typing import List, Dict, Any, Optional


class VectorStoreService:
    """
    Service for managing vector storage with ChromaDB and sentence embeddings.
    Provides knowledge storage, retrieval, and intelligent chunking capabilities.
    """
    def __init__(self,
                 db_path: str = "./chroma_db",
                 collection_name: str = "mcp_knowledge_base",
                 embedding_model: str = "google/embeddinggemma-300m"):
        """
        Initializes the VectorStore.

        Args:
            db_path (str): The path to the directory where the database will be persisted.
            collection_name (str): The name of the collection to use.
            embedding_model (str): The SentenceTransformer model name.
                                  Default: google/embeddinggemma-300m (supports multilingual, 768 dimensions)
        """
        self.db_client = chromadb.PersistentClient(path=db_path)
        self.collection = self.db_client.get_or_create_collection(
            name=collection_name,
            metadata={"hnsw:space": "cosine"}  # Use cosine similarity
        )

        # Initialize model with bfloat16 for optimal performance
        device = "cuda" if torch.cuda.is_available() else ("mps" if torch.backends.mps.is_available() else "cpu")
        self.model = SentenceTransformer(
            embedding_model,
            device=device,
            model_kwargs={"torch_dtype": torch.bfloat16} if device in ["cuda", "mps"] else {}
        )
        print(f"[OK] Loaded embedding model: {embedding_model} on device: {device}")

    @staticmethod
    def _format_query_prompt(query: str) -> str:
        """
        Format query text with EmbeddingGemma prompt template.

        Args:
            query (str): The user's search query

        Returns:
            str: Formatted query with task prefix
        """
        return f"task: search result | query: {query}"

    @staticmethod
    def _format_document_prompt(text: str, title: str = None) -> str:
        """
        Format document text with EmbeddingGemma prompt template.

        Args:
            text (str): The document text content
            title (str, optional): Optional document title

        Returns:
            str: Formatted document with title prefix
        """
        if title:
            return f"title: {title} | text: {text}"
        return f"text: {text}"

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

    @staticmethod
    def _distance_to_similarity(distance: float) -> float:
        """
        Convert a ChromaDB cosine distance into a cosine similarity.

        The collection is created with `hnsw:space: cosine`, so ChromaDB returns
        distance = 1 - cosine_similarity. Distance lives in [0, 2] (lower is better)
        while similarity lives in [-1, 1] (higher is better).

        Args:
            distance (float): The cosine distance reported by ChromaDB.

        Returns:
            float: The corresponding cosine similarity.
        """
        return 1.0 - distance

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

        # Format content with EmbeddingGemma prompt template
        formatted_content = self._format_document_prompt(content, title=topic)
        embedding = self.model.encode(formatted_content).tolist()

        self.collection.add(
            ids=[doc_id],
            embeddings=[embedding],
            documents=[content],  # Store original content, not the formatted version
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
        # Format query with EmbeddingGemma prompt template
        formatted_query = self._format_query_prompt(query)
        query_embedding = self.model.encode(formatted_query).tolist()

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
                similarity=self._distance_to_similarity(results["distances"][0][i])
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


