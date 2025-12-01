"""
Document Controller for MCP tools.

Handles document storage operations.
"""
from typing import Optional
import json
from pathlib import Path

from services.vector_store_service import VectorStoreService


def register_document_tools(server, vector_store: VectorStoreService):
    """
    Register document-related MCP tools.

    Args:
        server: FastMCP server instance
        vector_store: VectorStoreService instance
    """

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

            return f"Document stored successfully:\\n- File: {path.name}\\n- Topic: {topic}\\n- ID: {doc_id}\\n- Size: {len(content)} characters"

        except Exception as e:
            return f"Error storing document: {str(e)}"
