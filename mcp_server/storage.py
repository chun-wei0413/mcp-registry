
import chromadb
from sentence_transformers import SentenceTransformer
import uuid
import datetime
from typing import List, Dict, Any

class VectorStore:
    """
    A class to manage interactions with the ChromaDB vector database and sentence embeddings.
    """
    def __init__(self, db_path: str = "./chroma_db", collection_name: str = "mcp_knowledge_base"):
        """
        Initializes the VectorStore.

        Args:
            db_path (str): The path to the directory where the database will be persisted.
            collection_name (str): The name of the collection to use.
        """
        self.db_client = chromadb.PersistentClient(path=db_path)
        self.collection = self.db_client.get_or_create_collection(
            name=collection_name,
            metadata={"hnsw:space": "cosine"}  # Use cosine similarity
        )
        self.model = SentenceTransformer("all-MiniLM-L6-v2")

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
