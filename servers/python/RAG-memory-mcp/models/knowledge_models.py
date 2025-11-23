"""
Pydantic models for knowledge management.
"""
from typing import List, Optional, Dict, Any
from pydantic import BaseModel, Field


# Response Models

class CodeBlock(BaseModel):
    """A code block extracted from documentation."""
    language: str = Field(..., description="Programming language (e.g., 'java', 'python')")
    code: str = Field(..., description="The actual code content")
    position: int = Field(..., description="Position index in the original document")


class KnowledgePoint(BaseModel):
    """A single knowledge point with metadata."""
    id: str
    content: str
    topic: str
    similarity: Optional[float] = None
    timestamp: str

    # Extended metadata (optional)
    file_path: Optional[str] = None
    section_title: Optional[str] = None
    chunk_type: Optional[str] = None

    # Code blocks (separated from content for better semantic search)
    code_blocks: Optional[List[CodeBlock]] = Field(
        default=None,
        description="Code examples associated with this knowledge point"
    )


class SearchResult(BaseModel):
    """Search results containing multiple knowledge points."""
    results: List[KnowledgePoint]


class RetrievalResult(BaseModel):
    """Retrieval results for topic-based queries."""
    knowledge_points: List[KnowledgePoint]


# Request Models

class IndexFolderRequest(BaseModel):
    """Request for batch indexing a folder."""
    source_dir: str = Field(..., description="Path to the directory to index")
    chunk_size: int = Field(4000, description="Maximum characters per chunk")
    chunk_overlap: int = Field(200, description="Overlap between chunks")
    file_extensions: Optional[List[str]] = Field(
        None,
        description="File extensions to index (default: .md .txt .java .py .js .ts .sh .json .yaml .yml)"
    )


class IndexingStats(BaseModel):
    """Statistics from a batch indexing operation."""
    total_files: int
    processed_files: int
    failed_files: int
    total_chunks: int
    skipped_files: int
    duration_seconds: float
    file_details: Optional[List[Dict[str, Any]]] = None
