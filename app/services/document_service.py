import hashlib
import os
import struct
import chromadb
from chromadb import EmbeddingFunction, Documents, Embeddings
from chromadb.config import Settings
from typing import Optional
import uuid

from app.models.document import DocumentCreate, DocumentUpdate, DocumentResponse


class HashEmbeddingFunction(EmbeddingFunction):
    """Deterministic embedding function based on character n-grams + hashing.
    Suitable for local/offline use and testing.  For production, swap in a
    proper model-based embedding function (e.g. SentenceTransformerEmbeddingFunction).
    """

    DIM = 384

    def __init__(self) -> None:  # suppress "does not implement __init__" warning
        pass

    @staticmethod
    def name() -> str:
        return "hash-embedding"

    def get_config(self) -> dict:
        return {"dim": HashEmbeddingFunction.DIM}

    @staticmethod
    def build_from_config(config: dict) -> "HashEmbeddingFunction":
        return HashEmbeddingFunction()

    def __call__(self, input: Documents) -> Embeddings:  # type: ignore[override]
        results = []
        for text in input:
            vec = [0.0] * self.DIM
            for i in range(0, len(text) - 1):
                ngram = text[i : i + 3].encode()
                digest = hashlib.sha256(ngram).digest()
                for j in range(0, min(len(digest), self.DIM * 4), 4):
                    idx = (j // 4) % self.DIM
                    val = struct.unpack_from("f", digest, j)[0]
                    if not (val != val):  # skip NaN
                        vec[idx] += val
            norm = sum(v * v for v in vec) ** 0.5 or 1.0
            results.append([v / norm for v in vec])
        return results


class DocumentService:
    COLLECTION_NAME = "documents"

    def __init__(
        self,
        persist_directory: Optional[str] = None,
        embedding_function: Optional[EmbeddingFunction] = None,
    ):
        directory = persist_directory or os.getenv("CHROMA_PERSIST_DIR", "./chroma_db")
        self._client = chromadb.PersistentClient(
            path=directory,
            settings=Settings(anonymized_telemetry=False),
        )
        ef = embedding_function or HashEmbeddingFunction()
        self._collection = self._client.get_or_create_collection(
            name=self.COLLECTION_NAME,
            metadata={"hnsw:space": "cosine"},
            embedding_function=ef,
        )

    # ------------------------------------------------------------------ CREATE
    def create(self, doc: DocumentCreate) -> DocumentResponse:
        doc_id = str(uuid.uuid4())
        meta = {**(doc.metadata or {}), "title": doc.title}
        self._collection.add(
            ids=[doc_id],
            documents=[doc.content],
            metadatas=[meta],
        )
        return DocumentResponse(id=doc_id, title=doc.title, content=doc.content, metadata=doc.metadata or {})

    # -------------------------------------------------------------------- READ
    def get(self, doc_id: str) -> Optional[DocumentResponse]:
        result = self._collection.get(ids=[doc_id], include=["documents", "metadatas"])
        if not result["ids"]:
            return None
        meta = result["metadatas"][0]
        title = meta.pop("title", "")
        return DocumentResponse(
            id=result["ids"][0],
            title=title,
            content=result["documents"][0],
            metadata=meta,
        )

    def list_all(self, limit: int = 100, offset: int = 0) -> list[DocumentResponse]:
        result = self._collection.get(include=["documents", "metadatas"], limit=limit, offset=offset)
        docs = []
        for doc_id, content, meta in zip(result["ids"], result["documents"], result["metadatas"]):
            meta = dict(meta)
            title = meta.pop("title", "")
            docs.append(DocumentResponse(id=doc_id, title=title, content=content, metadata=meta))
        return docs

    # ------------------------------------------------------------------ UPDATE
    def update(self, doc_id: str, update: DocumentUpdate) -> Optional[DocumentResponse]:
        existing = self.get(doc_id)
        if existing is None:
            return None

        new_title = update.title if update.title is not None else existing.title
        new_content = update.content if update.content is not None else existing.content
        new_metadata = update.metadata if update.metadata is not None else existing.metadata

        meta = {**new_metadata, "title": new_title}
        self._collection.update(
            ids=[doc_id],
            documents=[new_content],
            metadatas=[meta],
        )
        return DocumentResponse(id=doc_id, title=new_title, content=new_content, metadata=new_metadata)

    # ------------------------------------------------------------------ DELETE
    def delete(self, doc_id: str) -> bool:
        existing = self.get(doc_id)
        if existing is None:
            return False
        self._collection.delete(ids=[doc_id])
        return True

    # ------------------------------------------------------------------ SEARCH
    def search(self, query: str, n_results: int = 5) -> list[DocumentResponse]:
        count = self._collection.count()
        if count == 0:
            return []
        n = min(n_results, count)
        result = self._collection.query(
            query_texts=[query],
            n_results=n,
            include=["documents", "metadatas"],
        )
        docs = []
        for doc_id, content, meta in zip(
            result["ids"][0], result["documents"][0], result["metadatas"][0]
        ):
            meta = dict(meta)
            title = meta.pop("title", "")
            docs.append(DocumentResponse(id=doc_id, title=title, content=content, metadata=meta))
        return docs
