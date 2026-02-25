from fastapi import APIRouter, HTTPException, Query
from typing import List

from app.models.document import DocumentCreate, DocumentUpdate, DocumentResponse, SearchQuery
from app.services.document_service import DocumentService

router = APIRouter(prefix="/documents", tags=["documents"])

_service = DocumentService()


@router.post("/", response_model=DocumentResponse, status_code=201, summary="Create a new document")
def create_document(doc: DocumentCreate) -> DocumentResponse:
    return _service.create(doc)


@router.get("/", response_model=List[DocumentResponse], summary="List all documents")
def list_documents(
    limit: int = Query(default=100, ge=1, le=1000),
    offset: int = Query(default=0, ge=0),
) -> List[DocumentResponse]:
    return _service.list_all(limit=limit, offset=offset)


@router.get("/{doc_id}", response_model=DocumentResponse, summary="Get a document by ID")
def get_document(doc_id: str) -> DocumentResponse:
    doc = _service.get(doc_id)
    if doc is None:
        raise HTTPException(status_code=404, detail="Document not found")
    return doc


@router.put("/{doc_id}", response_model=DocumentResponse, summary="Update a document")
def update_document(doc_id: str, update: DocumentUpdate) -> DocumentResponse:
    doc = _service.update(doc_id, update)
    if doc is None:
        raise HTTPException(status_code=404, detail="Document not found")
    return doc


@router.delete("/{doc_id}", status_code=204, summary="Delete a document")
def delete_document(doc_id: str) -> None:
    if not _service.delete(doc_id):
        raise HTTPException(status_code=404, detail="Document not found")


@router.post("/search/", response_model=List[DocumentResponse], summary="Semantic search over documents")
def search_documents(query: SearchQuery) -> List[DocumentResponse]:
    return _service.search(query.query, query.n_results)
