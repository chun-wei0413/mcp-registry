"""
Knowledge Controller for MCP tools.

Handles knowledge search and learning operations.
"""
from typing import Optional
from models.knowledge_models import SearchResult
from services.vector_store_service import VectorStoreService


def register_knowledge_tools(server, vector_store: VectorStoreService):
    """
    Register knowledge-related MCP tools.

    Args:
        server: FastMCP server instance
        vector_store: VectorStoreService instance
    """

    @server.tool()
    def search_knowledge(query: str, top_k: int = 20, topic: Optional[str] = None) -> SearchResult:
        """
        Performs a semantic search for knowledge.

        Args:
            query: The natural language question to search for.
            top_k: The maximum number of results to return (default: 50).
            topic: An optional topic to filter the search within.

        Returns:
            A list of the most relevant knowledge points found.
        """
        search_results = vector_store.search_knowledge(query, top_k, topic)
        return SearchResult(results=search_results)

    @server.tool()
    def learn_knowledge(topic: str, content: str) -> str:
        """
        Learns and stores a new piece of knowledge.

        Args:
            topic: The category or topic of the knowledge (e.g., 'DDD', 'SOLID').
            content: The actual text content of the knowledge point.

        Returns:
            A confirmation message with the ID of the new knowledge point.
        """
        doc_id = vector_store.add_knowledge(topic, content)
        return f"Knowledge learned with ID: {doc_id}"
