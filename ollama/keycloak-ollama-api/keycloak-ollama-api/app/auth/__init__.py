"""
Authentication module for Keycloak OAuth2/OIDC
"""

from app.auth.keycloak import (
    KeycloakAuth,
    User,
    get_current_user,
    require_role,
    has_role,
    has_any_role
)

__all__ = [
    "KeycloakAuth",
    "User",
    "get_current_user",
    "require_role",
    "has_role",
    "has_any_role"
]
