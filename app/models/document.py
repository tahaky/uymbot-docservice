from pydantic import BaseModel, Field
from typing import Optional
import uuid


class DocumentCreate(BaseModel):
    title: str = Field(..., description="Title of the document")
    content: str = Field(..., description="Text content of the document")
    metadata: Optional[dict] = Field(default_factory=dict, description="Optional metadata")


class DocumentUpdate(BaseModel):
    title: Optional[str] = Field(None, description="Updated title")
    content: Optional[str] = Field(None, description="Updated text content")
    metadata: Optional[dict] = Field(None, description="Updated metadata")


class DocumentResponse(BaseModel):
    id: str
    title: str
    content: str
    metadata: dict


class SearchQuery(BaseModel):
    query: str = Field(..., description="Natural language search query")
    n_results: int = Field(default=5, ge=1, le=50, description="Number of results to return")
