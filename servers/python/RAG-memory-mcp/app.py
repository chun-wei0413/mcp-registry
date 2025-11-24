"""
Application Factory for MCP Knowledge Base Server.

Creates and configures the FastMCP server instance with all services and controllers.
"""
import os
from mcp.server import FastMCP

# Services
from services.vector_store_service import VectorStoreService
from services.context_chunking_service import ContextChunkingService

# Controllers
from controllers.knowledge_controller import register_knowledge_tools
from controllers.document_controller import register_document_tools
from controllers.indexing_controller import register_indexing_tools
from controllers.resource_controller import register_resources


def create_app(
    db_path: str = None,
    collection_name: str = None,
    embedding_model: str = None,
    host: str = None,
    port: int = None
) -> FastMCP:
    """
    Create and configure the FastMCP server application.

    Configuration priority (highest to lowest):
    1. Function parameters (if provided)
    2. Environment variables
    3. Default values

    Args:
        db_path: Path to ChromaDB database directory
        collection_name: ChromaDB collection name
        embedding_model: SentenceTransformer model name
        host: Server host address
        port: Server port number

    Environment Variables:
        CHROMA_DB_PATH: Database path (default: ./chroma_db)
        COLLECTION_NAME: Collection name (default: ai_documentation)
                        Options:
                        - ai_documentation (1,116 chunks - RAG index for .ai directory) ⭐ RECOMMENDED
                        - mcp_knowledge_base (for manual knowledge points)
        EMBEDDING_MODEL: Model name (default: paraphrase-multilingual-MiniLM-L12-v2)
        MCP_SERVER_HOST: Server host (default: 0.0.0.0)
        MCP_SERVER_PORT: Server port (default: 3031)

    Returns:
        Configured FastMCP server instance
    """
    # ============================================================
    # 0. Load Configuration (Environment Variables + Defaults)
    # ============================================================

    # Read from environment variables with defaults
    db_path = db_path or os.getenv("CHROMA_DB_PATH", "./chroma_db")
    collection_name = collection_name or os.getenv("COLLECTION_NAME", "ai_documentation")
    embedding_model = embedding_model or os.getenv("EMBEDDING_MODEL", "paraphrase-multilingual-MiniLM-L12-v2")
    host = host or os.getenv("MCP_SERVER_HOST", "0.0.0.0")
    port = port or int(os.getenv("MCP_SERVER_PORT", "3031"))

    # ============================================================
    # 1. Initialize Services
    # ============================================================

    print(f"[*] Initializing services...")
    print(f"    - Database: {db_path}")
    print(f"    - Collection: {collection_name}")
    print(f"    - Embedding model: {embedding_model}")

    # Initialize Vector Store Service
    vector_store = VectorStoreService(
        db_path=db_path,
        collection_name=collection_name,
        embedding_model=embedding_model
    )

    # Initialize Context Chunking Service
    context_chunking = ContextChunkingService(vector_store=vector_store)

    print(f"[OK] Services initialized\n")

    # ============================================================
    # 2. Initialize FastMCP Server
    # ============================================================

    print(f"[*] Creating MCP Server...")
    print(f"    - Host: {host}")
    print(f"    - Port: {port}\n")

    server = FastMCP(
        "MCP Knowledge Base Server",
        host=host,
        port=port
    )

    # ============================================================
    # 3. Register Controllers (MCP Tools and Resources)
    # ============================================================

    print(f"[*] Registering controllers...")

    # Register knowledge tools: search_knowledge, learn_knowledge
    register_knowledge_tools(server, vector_store)
    print(f"    - Knowledge tools registered")

    # Register document tools: store_document
    register_document_tools(server, vector_store)
    print(f"    - Document tools registered")

    # Register indexing tools: batch_index_folder (context chunking)
    register_indexing_tools(server, context_chunking)
    print(f"    - Indexing tools registered")

    # Register resources: knowledge://{topic}
    register_resources(server, vector_store)
    print(f"    - Resources registered")

    print(f"[OK] All controllers registered\n")

    # ============================================================
    # 4. Return Configured Server
    # ============================================================

    return server
