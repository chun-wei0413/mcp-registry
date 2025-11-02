
from mcp.server import FastMCP
from typing import List, Optional
import pydantic

from storage import VectorStore

# 1. Initialize the storage backend
vector_store = VectorStore()

# 2. Initialize the FastMCP Server
server = FastMCP("MCP Knowledge Base Server")

# 3. Define Pydantic models for structured tool output
class KnowledgePoint(pydantic.BaseModel):
    id: str
    content: str
    topic: str
    similarity: Optional[float] = None
    timestamp: str

class SearchResult(pydantic.BaseModel):
    results: List[KnowledgePoint]

class RetrievalResult(pydantic.BaseModel):
    knowledge_points: List[KnowledgePoint]


# 4. Define MCP Tools and Resources using the correct decorator syntax

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


@server.tool()
def search_knowledge(query: str, top_k: int = 5, topic: Optional[str] = None) -> SearchResult:
    """
    Performs a semantic search for knowledge.

    Args:
        query: The natural language question to search for.
        top_k: The maximum number of results to return.
        topic: An optional topic to filter the search within.
    
    Returns:
        A list of the most relevant knowledge points found.
    """
    search_results = vector_store.search_knowledge(query, top_k, topic)
    return SearchResult(results=search_results)


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

