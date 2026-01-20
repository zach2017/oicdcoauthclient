"""
FastAPI Application with Keycloak OAuth2/OIDC Authentication
Calls Ollama API for document summarization
"""

from fastapi import FastAPI, Depends, HTTPException, status, UploadFile, File, Form
from fastapi.security import OAuth2AuthorizationCodeBearer
from fastapi.middleware.cors import CORSMiddleware
from contextlib import asynccontextmanager
import logging

from app.auth.keycloak import KeycloakAuth, get_current_user, User
from app.services.ollama_service import OllamaService
from app.config import settings
from app.models.schemas import (
    SummarizeRequest,
    SummarizeResponse,
    HealthResponse,
    DocumentUploadResponse
)

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

# Initialize services
keycloak_auth = KeycloakAuth()
ollama_service = OllamaService()


@asynccontextmanager
async def lifespan(app: FastAPI):
    """Application lifespan manager"""
    logger.info("Starting up application...")
    logger.info(f"Keycloak Server: {settings.KEYCLOAK_SERVER_URL}")
    logger.info(f"Keycloak Realm: {settings.KEYCLOAK_REALM}")
    logger.info(f"Ollama URL: {settings.OLLAMA_URL}")
    yield
    logger.info("Shutting down application...")


app = FastAPI(
    title="Document Summarization API",
    description="OAuth2/OIDC protected API that uses Ollama for document summarization",
    version="1.0.0",
    lifespan=lifespan
)

# CORS middleware configuration
app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.CORS_ORIGINS,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.get("/health", response_model=HealthResponse, tags=["Health"])
async def health_check():
    """Health check endpoint - no authentication required"""
    ollama_status = await ollama_service.check_health()
    return HealthResponse(
        status="healthy",
        ollama_connected=ollama_status,
        keycloak_configured=bool(settings.KEYCLOAK_SERVER_URL)
    )


@app.get("/api/v1/me", tags=["User"])
async def get_user_info(current_user: User = Depends(get_current_user)):
    """Get current authenticated user information"""
    return {
        "username": current_user.username,
        "email": current_user.email,
        "roles": current_user.roles,
        "token_claims": current_user.token_claims
    }


@app.post("/api/v1/summarize", response_model=SummarizeResponse, tags=["Summarization"])
async def summarize_text(
    request: SummarizeRequest,
    current_user: User = Depends(get_current_user)
):
    """
    Summarize provided text using Ollama
    
    Requires valid OAuth2 token from Keycloak
    """
    logger.info(f"Summarization request from user: {current_user.username}")
    
    try:
        summary = await ollama_service.summarize_text(
            text=request.text,
            model=request.model,
            max_length=request.max_length,
            style=request.style
        )
        
        return SummarizeResponse(
            summary=summary,
            model_used=request.model or settings.OLLAMA_DEFAULT_MODEL,
            original_length=len(request.text),
            summary_length=len(summary),
            user=current_user.username
        )
    except Exception as e:
        logger.error(f"Summarization error: {str(e)}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Summarization failed: {str(e)}"
        )


@app.post("/api/v1/summarize/document", response_model=DocumentUploadResponse, tags=["Summarization"])
async def summarize_document(
    file: UploadFile = File(...),
    model: str = Form(default=None),
    max_length: int = Form(default=500),
    style: str = Form(default="concise"),
    current_user: User = Depends(get_current_user)
):
    """
    Upload and summarize a document (txt, pdf, md, etc.)
    
    Requires valid OAuth2 token from Keycloak
    """
    logger.info(f"Document upload from user: {current_user.username}, file: {file.filename}")
    
    # Validate file type
    allowed_extensions = {'.txt', '.md', '.pdf', '.docx', '.doc'}
    file_ext = '.' + file.filename.split('.')[-1].lower() if '.' in file.filename else ''
    
    if file_ext not in allowed_extensions:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=f"File type not supported. Allowed: {', '.join(allowed_extensions)}"
        )
    
    try:
        # Read file content
        content = await file.read()
        
        # Extract text based on file type
        if file_ext in {'.txt', '.md'}:
            text = content.decode('utf-8')
        elif file_ext == '.pdf':
            text = await ollama_service.extract_pdf_text(content)
        elif file_ext in {'.docx', '.doc'}:
            text = await ollama_service.extract_docx_text(content)
        else:
            text = content.decode('utf-8', errors='ignore')
        
        if not text.strip():
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="Could not extract text from document"
            )
        
        # Summarize the extracted text
        summary = await ollama_service.summarize_text(
            text=text,
            model=model,
            max_length=max_length,
            style=style
        )
        
        return DocumentUploadResponse(
            filename=file.filename,
            summary=summary,
            model_used=model or settings.OLLAMA_DEFAULT_MODEL,
            original_length=len(text),
            summary_length=len(summary),
            user=current_user.username
        )
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Document processing error: {str(e)}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Document processing failed: {str(e)}"
        )


@app.post("/api/v1/query", tags=["Query"])
async def query_ollama(
    prompt: str = Form(...),
    model: str = Form(default=None),
    context: str = Form(default=None),
    current_user: User = Depends(get_current_user)
):
    """
    Send a custom query to Ollama
    
    Requires valid OAuth2 token from Keycloak
    """
    logger.info(f"Query request from user: {current_user.username}")
    
    try:
        response = await ollama_service.query(
            prompt=prompt,
            model=model,
            context=context
        )
        
        return {
            "response": response,
            "model_used": model or settings.OLLAMA_DEFAULT_MODEL,
            "user": current_user.username
        }
    except Exception as e:
        logger.error(f"Query error: {str(e)}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Query failed: {str(e)}"
        )


@app.get("/api/v1/models", tags=["Models"])
async def list_models(current_user: User = Depends(get_current_user)):
    """
    List available Ollama models
    
    Requires valid OAuth2 token from Keycloak
    """
    try:
        models = await ollama_service.list_models()
        return {"models": models, "default": settings.OLLAMA_DEFAULT_MODEL}
    except Exception as e:
        logger.error(f"List models error: {str(e)}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Failed to list models: {str(e)}"
        )


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(
        "app.main:app",
        host=settings.HOST,
        port=settings.PORT,
        reload=settings.DEBUG
    )
