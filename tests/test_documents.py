import pytest
import tempfile
import os

from fastapi.testclient import TestClient

# Point ChromaDB to a temporary directory so tests don't pollute real data
os.environ.setdefault("CHROMA_PERSIST_DIR", tempfile.mkdtemp())

# Patch the service to use the temp directory before importing the app
import app.api.routes.documents as _doc_routes
from app.services.document_service import DocumentService

_doc_routes._service = DocumentService(persist_directory=os.environ["CHROMA_PERSIST_DIR"])

from app.main import app

client = TestClient(app)


# ─────────────────────────────────────────────────────────────
#  Helpers
# ─────────────────────────────────────────────────────────────

def _create_doc(title="Test Doc", content="Some content about testing.", metadata=None):
    payload = {"title": title, "content": content}
    if metadata:
        payload["metadata"] = metadata
    return client.post("/documents/", json=payload)


# ─────────────────────────────────────────────────────────────
#  Health
# ─────────────────────────────────────────────────────────────

def test_health():
    resp = client.get("/health")
    assert resp.status_code == 200
    assert resp.json() == {"status": "ok"}


# ─────────────────────────────────────────────────────────────
#  CREATE
# ─────────────────────────────────────────────────────────────

def test_create_document():
    resp = _create_doc(title="Hello", content="World content")
    assert resp.status_code == 201
    data = resp.json()
    assert data["title"] == "Hello"
    assert data["content"] == "World content"
    assert "id" in data


def test_create_document_with_metadata():
    resp = _create_doc(title="Meta Doc", content="Content with metadata", metadata={"source": "web"})
    assert resp.status_code == 201
    data = resp.json()
    assert data["metadata"]["source"] == "web"


# ─────────────────────────────────────────────────────────────
#  READ
# ─────────────────────────────────────────────────────────────

def test_get_document():
    created = _create_doc(title="Readable", content="Readable content").json()
    doc_id = created["id"]

    resp = client.get(f"/documents/{doc_id}")
    assert resp.status_code == 200
    data = resp.json()
    assert data["id"] == doc_id
    assert data["title"] == "Readable"


def test_get_document_not_found():
    resp = client.get("/documents/nonexistent-id")
    assert resp.status_code == 404


def test_list_documents():
    # Create at least one document
    _create_doc(title="List Test", content="For listing")
    resp = client.get("/documents/")
    assert resp.status_code == 200
    assert isinstance(resp.json(), list)
    assert len(resp.json()) >= 1


# ─────────────────────────────────────────────────────────────
#  UPDATE
# ─────────────────────────────────────────────────────────────

def test_update_document():
    created = _create_doc(title="Old Title", content="Old content").json()
    doc_id = created["id"]

    resp = client.put(f"/documents/{doc_id}", json={"title": "New Title", "content": "New content"})
    assert resp.status_code == 200
    data = resp.json()
    assert data["title"] == "New Title"
    assert data["content"] == "New content"


def test_update_document_partial():
    created = _create_doc(title="Partial", content="Original content").json()
    doc_id = created["id"]

    resp = client.put(f"/documents/{doc_id}", json={"title": "Updated Title"})
    assert resp.status_code == 200
    data = resp.json()
    assert data["title"] == "Updated Title"
    assert data["content"] == "Original content"


def test_update_document_not_found():
    resp = client.put("/documents/nonexistent-id", json={"title": "X"})
    assert resp.status_code == 404


# ─────────────────────────────────────────────────────────────
#  DELETE
# ─────────────────────────────────────────────────────────────

def test_delete_document():
    created = _create_doc(title="Delete Me", content="To be deleted").json()
    doc_id = created["id"]

    resp = client.delete(f"/documents/{doc_id}")
    assert resp.status_code == 204

    # Confirm it's gone
    resp = client.get(f"/documents/{doc_id}")
    assert resp.status_code == 404


def test_delete_document_not_found():
    resp = client.delete("/documents/nonexistent-id")
    assert resp.status_code == 404


# ─────────────────────────────────────────────────────────────
#  SEARCH
# ─────────────────────────────────────────────────────────────

def test_search_documents():
    _create_doc(title="Python Guide", content="Python is a high-level programming language.")
    _create_doc(title="FastAPI Tutorial", content="FastAPI is a modern web framework for Python.")

    resp = client.post("/documents/search/", json={"query": "web framework Python", "n_results": 2})
    assert resp.status_code == 200
    results = resp.json()
    assert isinstance(results, list)
    assert len(results) >= 1


def test_search_empty_collection_returns_empty():
    # Use a fresh isolated service with an empty collection
    with tempfile.TemporaryDirectory() as tmpdir:
        empty_service = DocumentService(persist_directory=tmpdir)
        original = _doc_routes._service
        _doc_routes._service = empty_service
        try:
            resp = client.post("/documents/search/", json={"query": "anything", "n_results": 5})
            assert resp.status_code == 200
            assert resp.json() == []
        finally:
            _doc_routes._service = original
