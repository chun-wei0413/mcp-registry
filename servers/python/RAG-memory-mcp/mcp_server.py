
from mcp.server import FastMCP
from typing import List, Optional
import pydantic
import json
from pathlib import Path

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
def store_document(file_path: str, topic: Optional[str] = None) -> str:
    """
    Reads and stores a document file into the knowledge base.

    Args:
        file_path: Absolute or relative path to the document file (.md, .json, .txt).
        topic: Optional topic/category for the document. If not provided, uses filename.

    Returns:
        A confirmation message with the document ID and number of chunks stored.
    """
    try:
        path = Path(file_path)
        if not path.exists():
            return f"Error: File '{file_path}' does not exist."

        # Read file content
        content = path.read_text(encoding='utf-8')

        # Determine topic
        if topic is None:
            topic = path.stem  # Use filename without extension as topic

        # Handle JSON files specially
        if path.suffix == '.json':
            try:
                json_data = json.loads(content)
                content = json.dumps(json_data, indent=2, ensure_ascii=False)
            except json.JSONDecodeError:
                pass  # Keep as plain text if not valid JSON

        # Store the document
        doc_id = vector_store.add_knowledge(topic, content)

        return f"Document stored successfully:\n- File: {path.name}\n- Topic: {topic}\n- ID: {doc_id}\n- Size: {len(content)} characters"

    except Exception as e:
        return f"Error storing document: {str(e)}"


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


# 5. Run the MCP server
if __name__ == "__main__":
    # Run FastMCP server (stdio transport by default)
    server.run()

