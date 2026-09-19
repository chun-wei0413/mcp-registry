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
        Performs a semantic search for knowledge using EmbeddingGemma prompt templates.

        The query will be formatted with the prompt template:
        "task: search result | query: {query}" before generating embeddings.

        Args:
            query: The natural language question to search for.
            top_k: The maximum number of results to return (default: 20).
            topic: An optional topic to filter the search within. This is an EXACT
                   match, not a prefix or fuzzy match. Topics created by the ingest
                   script look like "aggregate - Aggregate 定義與核心概念"
                   (i.e. "{category} - {section title}"), so passing a bare category
                   such as "aggregate" returns nothing. Prefer leaving this unset and
                   relying on semantic search; run scripts/verify_ai_docs.py to list
                   the topics that actually exist.

        Returns:
            A list of the most relevant knowledge points found, ordered from most to
            least similar. `similarity` is a cosine similarity in [-1, 1], where
            higher means more similar.

        Example:
            search_knowledge(
                query="How to protect business rules from external modification?",
                top_k=5
            )
        """
        search_results = vector_store.search_knowledge(query, top_k, topic)
        return SearchResult(results=search_results)

    @server.tool()
    def learn_knowledge(topic: str, content: str) -> str:
        """
        Learns and stores a new piece of knowledge using EmbeddingGemma prompt templates.

        The content will be formatted with the prompt template:
        "title: {topic} | text: {content}" before generating embeddings.

        Args:
            topic: The category or topic of the knowledge (e.g., 'DDD', 'SOLID').
                   This will be used as the title in the prompt template.
            content: The actual text content of the knowledge point.
                    Should be clear, descriptive text (not code).

        Returns:
            A confirmation message with the ID of the new knowledge point.

        Example:
            learn_knowledge(
                topic="DDD",
                content="An Aggregate is a cluster of domain objects treated as a single unit."
            )
        """
        doc_id = vector_store.add_knowledge(topic, content)
        return f"Knowledge learned with ID: {doc_id}"
