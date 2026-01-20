"""
Pydantic Schemas for Request/Response Models

Defines all data models used in API requests and responses.
"""

from pydantic import BaseModel, Field
from typing import Optional, List, Dict, Any
from enum import Enum


class SummaryStyle(str, Enum):
    """Available summary styles"""
    CONCISE = "concise"
    DETAILED = "detailed"
    BULLET_POINTS = "bullet_points"
    EXECUTIVE = "executive"
    ACADEMIC = "academic"


class AnalysisType(str, Enum):
    """Available analysis types"""
    GENERAL = "general"
    SENTIMENT = "sentiment"
    ENTITIES = "entities"
    TOPICS = "topics"


# Request Models

class SummarizeRequest(BaseModel):
    """Request model for text summarization"""
    text: str = Field(
        ...,
        min_length=10,
        description="Text to summarize"
    )
    model: Optional[str] = Field(
        default=None,
        description="Ollama model to use (uses default if not specified)"
    )
    max_length: int = Field(
        default=500,
        ge=50,
        le=5000,
        description="Approximate maximum length of summary in words"
    )
    style: str = Field(
        default="concise",
        description="Summary style: concise, detailed, bullet_points, executive, academic"
    )
    
    class Config:
        json_schema_extra = {
            "example": {
                "text": "This is a long document that needs to be summarized...",
                "model": "llama3.2",
                "max_length": 300,
                "style": "concise"
            }
        }


class QueryRequest(BaseModel):
    """Request model for custom queries"""
    prompt: str = Field(
        ...,
        min_length=1,
        description="Query or prompt to send to Ollama"
    )
    model: Optional[str] = Field(
        default=None,
        description="Ollama model to use"
    )
    context: Optional[str] = Field(
        default=None,
        description="Optional context to include with the query"
    )
    temperature: float = Field(
        default=0.7,
        ge=0.0,
        le=2.0,
        description="Generation temperature (0.0-2.0)"
    )
    
    class Config:
        json_schema_extra = {
            "example": {
                "prompt": "What are the key points?",
                "context": "Document text here...",
                "temperature": 0.7
            }
        }


class AnalyzeRequest(BaseModel):
    """Request model for document analysis"""
    text: str = Field(
        ...,
        min_length=10,
        description="Text to analyze"
    )
    analysis_type: AnalysisType = Field(
        default=AnalysisType.GENERAL,
        description="Type of analysis to perform"
    )
    model: Optional[str] = Field(
        default=None,
        description="Ollama model to use"
    )


class ChatMessage(BaseModel):
    """Single chat message"""
    role: str = Field(
        ...,
        pattern="^(user|assistant|system)$",
        description="Message role: user, assistant, or system"
    )
    content: str = Field(
        ...,
        min_length=1,
        description="Message content"
    )


class ChatRequest(BaseModel):
    """Request model for chat interactions"""
    messages: List[ChatMessage] = Field(
        ...,
        min_length=1,
        description="List of chat messages"
    )
    model: Optional[str] = Field(
        default=None,
        description="Ollama model to use"
    )
    temperature: float = Field(
        default=0.7,
        ge=0.0,
        le=2.0,
        description="Generation temperature"
    )


# Response Models

class HealthResponse(BaseModel):
    """Health check response"""
    status: str = Field(description="Service status")
    ollama_connected: bool = Field(description="Whether Ollama is accessible")
    keycloak_configured: bool = Field(description="Whether Keycloak is configured")


class SummarizeResponse(BaseModel):
    """Response model for summarization"""
    summary: str = Field(description="Generated summary")
    model_used: str = Field(description="Model used for summarization")
    original_length: int = Field(description="Character length of original text")
    summary_length: int = Field(description="Character length of summary")
    user: str = Field(description="Username of the requester")
    
    class Config:
        json_schema_extra = {
            "example": {
                "summary": "This document discusses...",
                "model_used": "llama3.2",
                "original_length": 5000,
                "summary_length": 500,
                "user": "john.doe"
            }
        }


class DocumentUploadResponse(BaseModel):
    """Response model for document upload and summarization"""
    filename: str = Field(description="Name of uploaded file")
    summary: str = Field(description="Generated summary")
    model_used: str = Field(description="Model used for summarization")
    original_length: int = Field(description="Character length of extracted text")
    summary_length: int = Field(description="Character length of summary")
    user: str = Field(description="Username of the requester")


class QueryResponse(BaseModel):
    """Response model for custom queries"""
    response: str = Field(description="Model response")
    model_used: str = Field(description="Model used")
    user: str = Field(description="Username of the requester")


class ChatResponse(BaseModel):
    """Response model for chat interactions"""
    response: str = Field(description="Assistant response")
    model_used: str = Field(description="Model used")
    user: str = Field(description="Username of the requester")


class AnalysisResponse(BaseModel):
    """Response model for document analysis"""
    analysis_type: str = Field(description="Type of analysis performed")
    result: str = Field(description="Analysis results")
    model_used: str = Field(description="Model used")
    user: str = Field(description="Username of the requester")


class ModelInfo(BaseModel):
    """Model information"""
    name: str = Field(description="Model name")
    size: Optional[int] = Field(default=None, description="Model size in bytes")
    modified_at: Optional[str] = Field(default=None, description="Last modified timestamp")
    digest: Optional[str] = Field(default=None, description="Model digest")


class ModelsResponse(BaseModel):
    """Response model for listing available models"""
    models: List[ModelInfo] = Field(description="List of available models")
    default: str = Field(description="Default model name")


class ErrorResponse(BaseModel):
    """Standard error response"""
    detail: str = Field(description="Error message")
    status_code: int = Field(description="HTTP status code")
    
    class Config:
        json_schema_extra = {
            "example": {
                "detail": "Token has expired",
                "status_code": 401
            }
        }


class UserInfo(BaseModel):
    """User information response"""
    username: str = Field(description="Username")
    email: Optional[str] = Field(description="User email")
    roles: List[str] = Field(description="User roles")
    groups: List[str] = Field(default=[], description="User groups")
