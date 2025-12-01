"""
Resource Controller for MCP resources.

Handles MCP resource endpoints for knowledge retrieval.
"""
from models.knowledge_models import RetrievalResult
from services.vector_store_service import VectorStoreService


def register_resources(server, vector_store: VectorStoreService):
    """
    Register MCP resources.

    Args:
        server: FastMCP server instance
        vector_store: VectorStoreService instance
    """

    @server.resource("knowledge://{topic}")
    def retrieve_all_by_topic(topic: str) -> RetrievalResult:
        """
        Retrieves all knowledge points for a given topic.

        Args:
            topic: The exact topic to retrieve all knowledge points for.

        Returns:
            A list of all knowledge points associated with the topic.
        """
        retrieved_results = vector_store.get_all_by_topic(topic)
        return RetrievalResult(knowledge_points=retrieved_results)
