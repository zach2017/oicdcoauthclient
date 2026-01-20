#!/usr/bin/env python3
"""
Example Client for Document Summarization API

This script demonstrates how to:
1. Authenticate with Keycloak to get an access token
2. Use the token to call the Document Summarization API
3. Handle different types of requests (text, document upload)

Usage:
    python examples/client.py
"""

import httpx
import json
import sys
from pathlib import Path


# Configuration
KEYCLOAK_URL = "http://localhost:8080"
KEYCLOAK_REALM = "document-api-realm"
CLIENT_ID = "document-api"
CLIENT_SECRET = "change-me-in-production"
API_URL = "http://localhost:8000"

# Test user credentials
USERNAME = "testuser"
PASSWORD = "testpassword"


def get_token() -> str:
    """
    Get an access token from Keycloak using Resource Owner Password Grant
    
    Returns:
        Access token string
    """
    token_url = f"{KEYCLOAK_URL}/realms/{KEYCLOAK_REALM}/protocol/openid-connect/token"
    
    data = {
        "grant_type": "password",
        "client_id": CLIENT_ID,
        "client_secret": CLIENT_SECRET,
        "username": USERNAME,
        "password": PASSWORD,
    }
    
    print(f"🔐 Requesting token from Keycloak...")
    
    response = httpx.post(token_url, data=data)
    
    if response.status_code != 200:
        print(f"❌ Failed to get token: {response.status_code}")
        print(response.text)
        sys.exit(1)
    
    token_data = response.json()
    print(f"✅ Token obtained! Expires in {token_data.get('expires_in')} seconds")
    
    return token_data["access_token"]


def get_headers(token: str) -> dict:
    """Create authorization headers"""
    return {
        "Authorization": f"Bearer {token}",
        "Content-Type": "application/json"
    }


def check_health():
    """Check API health (no auth required)"""
    print("\n📊 Checking API health...")
    
    response = httpx.get(f"{API_URL}/health")
    
    if response.status_code == 200:
        data = response.json()
        print(f"   Status: {data['status']}")
        print(f"   Ollama Connected: {data['ollama_connected']}")
        print(f"   Keycloak Configured: {data['keycloak_configured']}")
    else:
        print(f"❌ Health check failed: {response.status_code}")


def get_user_info(token: str):
    """Get current user information"""
    print("\n👤 Getting user info...")
    
    response = httpx.get(
        f"{API_URL}/api/v1/me",
        headers=get_headers(token)
    )
    
    if response.status_code == 200:
        data = response.json()
        print(f"   Username: {data['username']}")
        print(f"   Email: {data.get('email', 'N/A')}")
        print(f"   Roles: {', '.join(data['roles'])}")
    else:
        print(f"❌ Failed to get user info: {response.status_code}")
        print(response.text)


def list_models(token: str):
    """List available Ollama models"""
    print("\n🤖 Listing available models...")
    
    response = httpx.get(
        f"{API_URL}/api/v1/models",
        headers=get_headers(token)
    )
    
    if response.status_code == 200:
        data = response.json()
        print(f"   Default model: {data['default']}")
        print("   Available models:")
        for model in data.get('models', []):
            name = model.get('name', 'Unknown')
            print(f"      - {name}")
    else:
        print(f"❌ Failed to list models: {response.status_code}")
        print(response.text)


def summarize_text(token: str, text: str, style: str = "concise"):
    """
    Summarize provided text
    
    Args:
        token: Access token
        text: Text to summarize
        style: Summary style (concise, detailed, bullet_points, executive)
    """
    print(f"\n📝 Summarizing text ({style} style)...")
    print(f"   Original length: {len(text)} characters")
    
    response = httpx.post(
        f"{API_URL}/api/v1/summarize",
        headers=get_headers(token),
        json={
            "text": text,
            "max_length": 200,
            "style": style
        },
        timeout=120.0  # Longer timeout for LLM processing
    )
    
    if response.status_code == 200:
        data = response.json()
        print(f"   Summary length: {data['summary_length']} characters")
        print(f"   Model used: {data['model_used']}")
        print(f"\n   Summary:\n   {'-' * 50}")
        print(f"   {data['summary']}")
        print(f"   {'-' * 50}")
    else:
        print(f"❌ Summarization failed: {response.status_code}")
        print(response.text)


def upload_and_summarize(token: str, file_path: str):
    """
    Upload a document and summarize it
    
    Args:
        token: Access token
        file_path: Path to the document file
    """
    path = Path(file_path)
    
    if not path.exists():
        print(f"❌ File not found: {file_path}")
        return
    
    print(f"\n📄 Uploading and summarizing: {path.name}")
    
    with open(path, 'rb') as f:
        files = {'file': (path.name, f, 'application/octet-stream')}
        data = {
            'max_length': '300',
            'style': 'executive'
        }
        
        response = httpx.post(
            f"{API_URL}/api/v1/summarize/document",
            headers={"Authorization": f"Bearer {token}"},
            files=files,
            data=data,
            timeout=120.0
        )
    
    if response.status_code == 200:
        data = response.json()
        print(f"   Filename: {data['filename']}")
        print(f"   Original length: {data['original_length']} characters")
        print(f"   Summary length: {data['summary_length']} characters")
        print(f"\n   Summary:\n   {'-' * 50}")
        print(f"   {data['summary']}")
        print(f"   {'-' * 50}")
    else:
        print(f"❌ Document summarization failed: {response.status_code}")
        print(response.text)


def custom_query(token: str, prompt: str, context: str = None):
    """
    Send a custom query to the API
    
    Args:
        token: Access token
        prompt: Query prompt
        context: Optional context
    """
    print(f"\n💬 Sending query: {prompt[:50]}...")
    
    data = {'prompt': prompt}
    if context:
        data['context'] = context
    
    response = httpx.post(
        f"{API_URL}/api/v1/query",
        headers={"Authorization": f"Bearer {token}"},
        data=data,
        timeout=120.0
    )
    
    if response.status_code == 200:
        result = response.json()
        print(f"\n   Response:\n   {'-' * 50}")
        print(f"   {result['response']}")
        print(f"   {'-' * 50}")
    else:
        print(f"❌ Query failed: {response.status_code}")
        print(response.text)


def main():
    """Main function demonstrating API usage"""
    print("=" * 60)
    print("  Document Summarization API - Example Client")
    print("=" * 60)
    
    # Check API health first (no auth required)
    check_health()
    
    # Get access token
    try:
        token = get_token()
    except Exception as e:
        print(f"❌ Error getting token: {e}")
        print("\nMake sure Keycloak is running and configured correctly.")
        sys.exit(1)
    
    # Get user info
    get_user_info(token)
    
    # List available models
    list_models(token)
    
    # Summarize some sample text
    sample_text = """
    Artificial intelligence (AI) has rapidly transformed from a niche research area 
    into a technology that touches nearly every aspect of modern life. Machine learning, 
    a subset of AI, enables computers to learn from data and improve their performance 
    over time without being explicitly programmed. Deep learning, which uses neural 
    networks with many layers, has achieved remarkable success in areas such as image 
    recognition, natural language processing, and game playing.
    
    The applications of AI are vast and growing. In healthcare, AI systems can analyze 
    medical images to detect diseases earlier than human doctors. In finance, AI 
    algorithms process vast amounts of market data to make trading decisions in 
    milliseconds. Autonomous vehicles use AI to navigate complex environments. Virtual 
    assistants like Siri and Alexa use natural language processing to understand and 
    respond to human speech.
    
    However, the rise of AI also brings significant challenges. There are concerns about 
    job displacement as AI systems become capable of performing tasks previously done by 
    humans. Privacy issues arise from the vast amounts of data required to train AI 
    systems. Questions of accountability emerge when AI systems make decisions that 
    affect people's lives. Ensuring that AI systems are fair and do not perpetuate 
    existing biases is an ongoing challenge.
    
    Despite these challenges, investment in AI research and development continues to 
    grow. Companies and governments around the world recognize the strategic importance 
    of AI and are racing to develop and deploy AI technologies. The future of AI holds 
    both tremendous promise and significant responsibility to ensure these powerful 
    technologies benefit humanity as a whole.
    """
    
    # Try different summary styles
    summarize_text(token, sample_text, "concise")
    summarize_text(token, sample_text, "bullet_points")
    
    # Send a custom query
    custom_query(
        token, 
        "What are the three most important ethical considerations for AI development?",
        context=sample_text
    )
    
    print("\n" + "=" * 60)
    print("  Demo Complete!")
    print("=" * 60)


if __name__ == "__main__":
    main()
