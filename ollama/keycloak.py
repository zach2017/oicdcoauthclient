"""
Keycloak OAuth2/OIDC Authentication Module

This module handles token validation and user authentication
using Keycloak as the identity provider.
"""

from fastapi import Depends, HTTPException, status
from fastapi.security import OAuth2AuthorizationCodeBearer, HTTPBearer, HTTPAuthorizationCredentials
from jose import jwt, JWTError, ExpiredSignatureError
from jose.exceptions import JWKError
from pydantic import BaseModel
from typing import Optional, List, Dict, Any
import httpx
import logging
from functools import lru_cache
import time

from app.config import settings

logger = logging.getLogger(__name__)

# HTTP Bearer security scheme
security = HTTPBearer(
    scheme_name="Bearer",
    description="Enter the JWT token obtained from Keycloak"
)


class User(BaseModel):
    """User model representing authenticated user"""
    username: str
    email: Optional[str] = None
    roles: List[str] = []
    groups: List[str] = []
    token_claims: Dict[str, Any] = {}
    
    class Config:
        from_attributes = True


class KeycloakAuth:
    """
    Keycloak Authentication Handler
    
    Validates JWT tokens issued by Keycloak and extracts user information.
    """
    
    def __init__(self):
        self.server_url = settings.KEYCLOAK_SERVER_URL
        self.realm = settings.KEYCLOAK_REALM
        self.client_id = settings.KEYCLOAK_CLIENT_ID
        self.client_secret = settings.KEYCLOAK_CLIENT_SECRET
        self.algorithms = ["RS256"]
        self._jwks_cache = None
        self._jwks_cache_time = 0
        self._jwks_cache_duration = 3600  # 1 hour cache
        
    @property
    def issuer_url(self) -> str:
        """Get the issuer URL for the Keycloak realm"""
        return f"{self.server_url}/realms/{self.realm}"
    
    @property
    def jwks_url(self) -> str:
        """Get the JWKS URL for the Keycloak realm"""
        return f"{self.issuer_url}/protocol/openid-connect/certs"
    
    @property
    def token_url(self) -> str:
        """Get the token endpoint URL"""
        return f"{self.issuer_url}/protocol/openid-connect/token"
    
    @property
    def userinfo_url(self) -> str:
        """Get the userinfo endpoint URL"""
        return f"{self.issuer_url}/protocol/openid-connect/userinfo"
    
    @property
    def introspect_url(self) -> str:
        """Get the token introspection endpoint URL"""
        return f"{self.issuer_url}/protocol/openid-connect/token/introspect"
    
    async def get_jwks(self) -> Dict[str, Any]:
        """
        Fetch and cache JWKS (JSON Web Key Set) from Keycloak
        
        Returns:
            Dict containing the JWKS
        """
        current_time = time.time()
        
        # Return cached JWKS if still valid
        if self._jwks_cache and (current_time - self._jwks_cache_time) < self._jwks_cache_duration:
            return self._jwks_cache
        
        try:
            async with httpx.AsyncClient() as client:
                response = await client.get(self.jwks_url, timeout=10.0)
                response.raise_for_status()
                self._jwks_cache = response.json()
                self._jwks_cache_time = current_time
                logger.info("JWKS fetched successfully from Keycloak")
                return self._jwks_cache
        except httpx.HTTPError as e:
            logger.error(f"Failed to fetch JWKS: {str(e)}")
            if self._jwks_cache:
                logger.warning("Using cached JWKS")
                return self._jwks_cache
            raise HTTPException(
                status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
                detail="Unable to verify token: Keycloak unavailable"
            )
    
    async def get_signing_key(self, token: str) -> Dict[str, Any]:
        """
        Get the signing key for a token from JWKS
        
        Args:
            token: JWT token to get signing key for
            
        Returns:
            Dict containing the signing key
        """
        try:
            # Decode header without verification to get key ID
            unverified_header = jwt.get_unverified_header(token)
            kid = unverified_header.get("kid")
            
            if not kid:
                raise HTTPException(
                    status_code=status.HTTP_401_UNAUTHORIZED,
                    detail="Token missing key ID"
                )
            
            jwks = await self.get_jwks()
            
            for key in jwks.get("keys", []):
                if key.get("kid") == kid:
                    return key
            
            # Key not found, try refreshing JWKS
            self._jwks_cache = None
            jwks = await self.get_jwks()
            
            for key in jwks.get("keys", []):
                if key.get("kid") == kid:
                    return key
            
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail="Unable to find signing key"
            )
        except JWTError as e:
            logger.error(f"JWT error getting signing key: {str(e)}")
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail="Invalid token format"
            )
    
    async def validate_token(self, token: str) -> Dict[str, Any]:
        """
        Validate a JWT token from Keycloak
        
        Args:
            token: JWT token to validate
            
        Returns:
            Dict containing the validated token claims
        """
        try:
            signing_key = await self.get_signing_key(token)
            
            # Build the public key from JWKS
            from jose.backends import RSAKey
            rsa_key = {
                "kty": signing_key["kty"],
                "kid": signing_key["kid"],
                "use": signing_key.get("use", "sig"),
                "n": signing_key["n"],
                "e": signing_key["e"]
            }
            
            # Verify and decode the token
            payload = jwt.decode(
                token,
                rsa_key,
                algorithms=self.algorithms,
                audience=self.client_id if settings.KEYCLOAK_VERIFY_AUDIENCE else None,
                issuer=self.issuer_url if settings.KEYCLOAK_VERIFY_ISSUER else None,
                options={
                    "verify_aud": settings.KEYCLOAK_VERIFY_AUDIENCE,
                    "verify_iss": settings.KEYCLOAK_VERIFY_ISSUER,
                    "verify_exp": True,
                    "verify_iat": True,
                }
            )
            
            logger.debug(f"Token validated for user: {payload.get('preferred_username')}")
            return payload
            
        except ExpiredSignatureError:
            logger.warning("Token has expired")
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail="Token has expired",
                headers={"WWW-Authenticate": "Bearer"}
            )
        except JWTError as e:
            logger.error(f"JWT validation error: {str(e)}")
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail=f"Could not validate credentials: {str(e)}",
                headers={"WWW-Authenticate": "Bearer"}
            )
    
    async def introspect_token(self, token: str) -> Dict[str, Any]:
        """
        Introspect a token using Keycloak's introspection endpoint
        
        This is an alternative to local JWT validation that checks
        the token's status on the Keycloak server.
        
        Args:
            token: Token to introspect
            
        Returns:
            Dict containing introspection response
        """
        try:
            async with httpx.AsyncClient() as client:
                response = await client.post(
                    self.introspect_url,
                    data={
                        "token": token,
                        "client_id": self.client_id,
                        "client_secret": self.client_secret
                    },
                    timeout=10.0
                )
                response.raise_for_status()
                result = response.json()
                
                if not result.get("active", False):
                    raise HTTPException(
                        status_code=status.HTTP_401_UNAUTHORIZED,
                        detail="Token is not active",
                        headers={"WWW-Authenticate": "Bearer"}
                    )
                
                return result
        except httpx.HTTPError as e:
            logger.error(f"Token introspection failed: {str(e)}")
            raise HTTPException(
                status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
                detail="Token introspection failed"
            )
    
    def extract_user_from_claims(self, claims: Dict[str, Any]) -> User:
        """
        Extract user information from token claims
        
        Args:
            claims: Validated token claims
            
        Returns:
            User object with extracted information
        """
        # Extract roles from realm and client roles
        roles = []
        
        # Realm roles
        realm_access = claims.get("realm_access", {})
        roles.extend(realm_access.get("roles", []))
        
        # Client roles
        resource_access = claims.get("resource_access", {})
        client_roles = resource_access.get(self.client_id, {})
        roles.extend(client_roles.get("roles", []))
        
        # Extract groups
        groups = claims.get("groups", [])
        
        return User(
            username=claims.get("preferred_username", claims.get("sub")),
            email=claims.get("email"),
            roles=list(set(roles)),  # Remove duplicates
            groups=groups,
            token_claims=claims
        )


# Create singleton instance
keycloak_auth = KeycloakAuth()


async def get_current_user(
    credentials: HTTPAuthorizationCredentials = Depends(security)
) -> User:
    """
    FastAPI dependency to get the current authenticated user
    
    Args:
        credentials: HTTP Bearer credentials
        
    Returns:
        Authenticated User object
    """
    token = credentials.credentials
    
    if settings.KEYCLOAK_USE_INTROSPECTION:
        # Use token introspection (checks with Keycloak server)
        claims = await keycloak_auth.introspect_token(token)
    else:
        # Local JWT validation
        claims = await keycloak_auth.validate_token(token)
    
    return keycloak_auth.extract_user_from_claims(claims)


async def require_role(required_roles: List[str]):
    """
    Factory function to create a dependency that requires specific roles
    
    Args:
        required_roles: List of roles that are required (user must have at least one)
        
    Returns:
        Dependency function
    """
    async def role_checker(current_user: User = Depends(get_current_user)) -> User:
        user_roles = set(current_user.roles)
        required = set(required_roles)
        
        if not user_roles.intersection(required):
            raise HTTPException(
                status_code=status.HTTP_403_FORBIDDEN,
                detail=f"Insufficient permissions. Required roles: {required_roles}"
            )
        
        return current_user
    
    return role_checker


def has_role(user: User, role: str) -> bool:
    """
    Check if a user has a specific role
    
    Args:
        user: User object
        role: Role to check for
        
    Returns:
        True if user has the role, False otherwise
    """
    return role in user.roles


def has_any_role(user: User, roles: List[str]) -> bool:
    """
    Check if a user has any of the specified roles
    
    Args:
        user: User object
        roles: List of roles to check for
        
    Returns:
        True if user has at least one of the roles, False otherwise
    """
    return bool(set(user.roles).intersection(set(roles)))
