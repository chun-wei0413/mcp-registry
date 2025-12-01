"""
Context Chunking Service for batch indexing.

Provides intelligent folder scanning and batch indexing capabilities.
"""
from pathlib import Path
from typing import List, Dict, Any, Optional, Set
from datetime import datetime

from services.vector_store_service import VectorStoreService


class ContextChunkingService:
    """
    Service for batch indexing files from a directory with intelligent chunking.
    """

    # Supported file extensions
    DEFAULT_EXTENSIONS = {'.md', '.txt', '.java', '.py', '.js', '.ts', '.sh', '.json', '.yaml', '.yml'}

    def __init__(self, vector_store: VectorStoreService):
        """
        Initialize the context chunking service.

        Args:
            vector_store: VectorStoreService instance for storage
        """
        self.vector_store = vector_store

    def scan_directory(self, source_dir: str, file_extensions: Optional[Set[str]] = None) -> List[Path]:
        """
        Recursively scan directory for supported files.

        Args:
            source_dir: Path to the directory to scan
            file_extensions: Set of file extensions to include (default: DEFAULT_EXTENSIONS)

        Returns:
            List of file paths to process
        """
        source_path = Path(source_dir).resolve()

        if not source_path.exists():
            raise ValueError(f"Source directory does not exist: {source_dir}")
        if not source_path.is_dir():
            raise ValueError(f"Source path is not a directory: {source_dir}")

        extensions = file_extensions if file_extensions else self.DEFAULT_EXTENSIONS

        files = []
        for file_path in source_path.rglob('*'):
            if file_path.is_file() and file_path.suffix in extensions:
                files.append(file_path)

        return files

    def extract_metadata(self, file_path: Path, source_dir: Path) -> Dict[str, Any]:
        """
        Extract metadata from file path.

        Args:
            file_path: Path to the file
            source_dir: Base source directory

        Returns:
            Dictionary of metadata
        """
        relative_path = file_path.relative_to(source_dir)
        parts = relative_path.parts

        # Determine category based on directory structure
        category = parts[0] if len(parts) > 1 else "root"

        # Extract topic from filename or parent directory
        topic = file_path.stem

        metadata = {
            "file_path": str(file_path),
            "relative_path": str(relative_path),
            "file_name": file_path.name,
            "file_type": file_path.suffix,
            "category": category,
            "topic": topic,
            "file_size": file_path.stat().st_size,
            "indexed_at": datetime.now().isoformat()
        }

        # Add sub-category if exists
        if len(parts) > 2:
            metadata["sub_category"] = parts[1]

        return metadata

    def process_file(self,
                    file_path: Path,
                    source_dir: Path,
                    chunk_size: int = 4000,
                    chunk_overlap: int = 200) -> Dict[str, Any]:
        """
        Process a single file and add to vector store.

        Args:
            file_path: Path to the file to process
            source_dir: Base source directory
            chunk_size: Maximum characters per chunk
            chunk_overlap: Overlap between chunks

        Returns:
            Dictionary with processing results
        """
        try:
            # Read file content
            content = file_path.read_text(encoding='utf-8')

            # Extract metadata
            metadata = self.extract_metadata(file_path, source_dir)

            # Add to vector store with chunking
            doc_ids = self.vector_store.add_knowledge_with_chunking(
                content=content,
                metadata=metadata,
                chunk_size=chunk_size,
                chunk_overlap=chunk_overlap
            )

            return {
                "success": True,
                "file_name": file_path.name,
                "num_chunks": len(doc_ids),
                "doc_ids": doc_ids
            }

        except Exception as e:
            return {
                "success": False,
                "file_name": file_path.name,
                "error": str(e)
            }

    def index_folder(self,
                    source_dir: str,
                    chunk_size: int = 4000,
                    chunk_overlap: int = 200,
                    file_extensions: Optional[List[str]] = None) -> Dict[str, Any]:
        """
        Index all files in a folder.

        Args:
            source_dir: Path to the directory to index
            chunk_size: Maximum characters per chunk
            chunk_overlap: Overlap between chunks
            file_extensions: List of file extensions to index

        Returns:
            Dictionary with indexing statistics
        """
        start_time = datetime.now()

        # Convert extensions list to set
        extensions = set(file_extensions) if file_extensions else self.DEFAULT_EXTENSIONS

        # Scan directory
        source_path = Path(source_dir).resolve()
        files = self.scan_directory(str(source_path), extensions)

        # Statistics
        stats = {
            "total_files": len(files),
            "processed_files": 0,
            "failed_files": 0,
            "total_chunks": 0,
            "skipped_files": 0,
            "file_details": []
        }

        # Process each file
        for file_path in files:
            result = self.process_file(file_path, source_path, chunk_size, chunk_overlap)

            if result["success"]:
                stats["processed_files"] += 1
                stats["total_chunks"] += result["num_chunks"]
            else:
                stats["failed_files"] += 1

            stats["file_details"].append(result)

        # Calculate duration
        end_time = datetime.now()
        stats["duration_seconds"] = (end_time - start_time).total_seconds()

        return stats
