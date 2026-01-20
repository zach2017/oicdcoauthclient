"""
Application Configuration

Manages all configuration settings using environment variables
with Pydantic settings management.
"""

from pydantic_settings import BaseSettings
from pydantic import Field
from typing import List, Optional
from functools import lru_cache


class Settings(BaseSettings):
    """
    Application settings loaded from environment variables
    
    All settings can be overridden via environment variables.
    Environment variables use the same name in uppercase.
    """
    
    # Application Settings
    APP_NAME: str = Field(default="Document Summarization API")
    DEBUG: bool = Field(default=False)
    HOST: str = Field(default="0.0.0.0")
    PORT: int = Field(default=8000)
    
    # CORS Settings
    CORS_ORIGINS: List[str] = Field(
        default=["http://localhost:3000", "http://localhost:8080"]
    )
    
    # Keycloak OAuth2/OIDC Settings
    KEYCLOAK_SERVER_URL: str = Field(
        default="http://localhost:8080",
        description="Keycloak server URL (e.g., http://keycloak:8080)"
    )
    KEYCLOAK_REALM: str = Field(
        default="master",
        description="Keycloak realm name"
    )
    KEYCLOAK_CLIENT_ID: str = Field(
        default="document-api",
        description="OAuth2 client ID registered in Keycloak"
    )
    KEYCLOAK_CLIENT_SECRET: Optional[str] = Field(
        default=None,
        description="OAuth2 client secret (required for confidential clients)"
    )
    
    # Token Validation Settings
    KEYCLOAK_VERIFY_AUDIENCE: bool = Field(
        default=True,
        description="Verify token audience matches client ID"
    )
    KEYCLOAK_VERIFY_ISSUER: bool = Field(
        default=True,
        description="Verify token issuer matches Keycloak realm"
    )
    KEYCLOAK_USE_INTROSPECTION: bool = Field(
        default=False,
        description="Use token introspection instead of local JWT validation"
    )
    
    # Ollama Settings
    OLLAMA_URL: str = Field(
        default="http://localhost:11434",
        description="Ollama API server URL"
    )
    OLLAMA_DEFAULT_MODEL: str = Field(
        default="llama3.2",
        description="Default Ollama model to use for generation"
    )
    OLLAMA_TIMEOUT: float = Field(
        default=120.0,
        description="Timeout in seconds for Ollama API requests"
    )
    
    # File Upload Settings
    MAX_UPLOAD_SIZE: int = Field(
        default=10 * 1024 * 1024,  # 10 MB
        description="Maximum file upload size in bytes"
    )
    
    # Logging Settings
    LOG_LEVEL: str = Field(default="INFO")
    
    class Config:
        env_file = ".env"
        env_file_encoding = "utf-8"
        case_sensitive = True
        extra = "ignore"


@lru_cache()
def get_settings() -> Settings:
    """
    Get cached settings instance
    
    Returns:
        Settings object with loaded configuration
    """
    return Settings()


# Global settings instance
settings = get_settings()
