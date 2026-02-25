from fastapi import FastAPI

from app.api.routes.documents import router as documents_router

app = FastAPI(
    title="DocService – Vector DB Document API",
    description="CRUD operations for documents stored in a ChromaDB vector database.",
    version="1.0.0",
)

app.include_router(documents_router)


@app.get("/health", tags=["health"])
def health() -> dict:
    return {"status": "ok"}
