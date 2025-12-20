"""
Indexing Controller for MCP tools.

Handles batch indexing operations (context chunking).
"""
from typing import Optional, List

from models.knowledge_models import IndexingStats
from services.context_chunking_service import ContextChunkingService


def register_indexing_tools(server, context_chunking: ContextChunkingService):
    """
    Register indexing-related MCP tools.

    Args:
        server: FastMCP server instance
        context_chunking: ContextChunkingService instance
    """

    @server.tool()
    def batch_index_folder(
        source_dir: str,
        chunk_size: int = 4000,
        chunk_overlap: int = 200,
        file_extensions: Optional[List[str]] = None
    ) -> IndexingStats:
        """
        Batch index all files in a folder with intelligent chunking.

        This tool scans a directory recursively and indexes all supported files
        into the vector database with smart chunking strategies based on file type.

        Args:
            source_dir: Path to the directory to index (required).
            chunk_size: Maximum characters per chunk (default: 4000).
            chunk_overlap: Overlap between chunks for context preservation (default: 200).
            file_extensions: List of file extensions to index.
                           Default: ['.md', '.txt', '.java', '.py', '.js', '.ts', '.sh', '.json', '.yaml', '.yml']

        Returns:
            IndexingStats with detailed statistics about the indexing operation.

        Examples:
            # Index a documentation folder
            batch_index_folder(source_dir="./docs")

            # Index only Markdown files
            batch_index_folder(source_dir="./docs", file_extensions=[".md", ".txt"])

            # Custom chunk size for larger documents
            batch_index_folder(source_dir="./docs", chunk_size=6000, chunk_overlap=300)
        """
        try:
            stats = context_chunking.index_folder(
                source_dir=source_dir,
                chunk_size=chunk_size,
                chunk_overlap=chunk_overlap,
                file_extensions=file_extensions
            )

            return IndexingStats(**stats)

        except ValueError as e:
            # Return error stats
            return IndexingStats(
                total_files=0,
                processed_files=0,
                failed_files=0,
                total_chunks=0,
                skipped_files=0,
                duration_seconds=0.0,
                file_details=[{"error": str(e)}]
            )
        except Exception as e:
            return IndexingStats(
                total_files=0,
                processed_files=0,
                failed_files=0,
                total_chunks=0,
                skipped_files=0,
                duration_seconds=0.0,
                file_details=[{"error": f"Unexpected error: {str(e)}"}]
            )
