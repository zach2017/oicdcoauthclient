# Document Summarization API

A Python FastAPI service with Keycloak OAuth2/OIDC authentication that uses Ollama for document summarization and LLM queries.

## Architecture

```
┌─────────────────┐       ┌─────────────────┐       ┌─────────────────┐
│                 │       │                 │       │                 │
│   Client App    │──────▶│  FastAPI        │──────▶│   Ollama        │
│   (Frontend)    │       │  (Python API)   │       │   (LLM Server)  │
│                 │       │                 │       │                 │
└────────┬────────┘       └────────┬────────┘       └─────────────────┘
         │                         │
         │   OAuth2 Token          │   Validate Token
         │                         │
         ▼                         ▼
┌─────────────────────────────────────────────────┐
│                                                 │
│            Keycloak (Identity Provider)         │
│                                                 │
└─────────────────────────────────────────────────┘
```

## Features

- **OAuth2/OIDC Authentication**: Secure API access using Keycloak
- **JWT Token Validation**: Local validation or token introspection
- **Document Summarization**: Summarize text documents using Ollama
- **File Upload Support**: Upload and summarize PDF, DOCX, TXT, MD files
- **Custom Queries**: Send custom prompts to Ollama
- **Role-Based Access Control**: Fine-grained permissions via Keycloak roles
- **Docker Ready**: Complete Docker Compose setup included

## Quick Start

### Prerequisites

- Docker and Docker Compose
- Python 3.11+ (for local development)
- Ollama (if running locally)

### 1. Clone and Setup

```bash
# Clone the repository
cd keycloak-ollama-api

# Copy environment file
cp .env.example .env

# Edit .env with your settings
```

### 2. Start Services with Docker

```bash
# Start all services (Keycloak, Ollama, API)
docker-compose up -d

# Pull the default Ollama model (first time only)
docker-compose --profile setup run ollama-pull

# Check service health
docker-compose ps
```

### 3. Access Services

| Service  | URL                          | Credentials              |
|----------|------------------------------|--------------------------|
| API      | http://localhost:8000        | OAuth2 token required    |
| API Docs | http://localhost:8000/docs   | -                        |
| Keycloak | http://localhost:8080        | admin / admin            |
| Ollama   | http://localhost:11434       | -                        |

## Authentication

### Getting an Access Token

Use the Resource Owner Password Grant (for testing):

```bash
# Get token from Keycloak
curl -X POST "http://localhost:8080/realms/document-api-realm/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=document-api" \
  -d "client_secret=change-me-in-production" \
  -d "username=testuser" \
  -d "password=testpassword"
```

Response:
```json
{
  "access_token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expires_in": 300,
  "refresh_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "token_type": "Bearer"
}
```

### Using the Token

Include the token in the Authorization header:

```bash
curl -X GET "http://localhost:8000/api/v1/me" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

## API Endpoints

### Health Check (No Auth Required)

```bash
GET /health
```

### User Info

```bash
GET /api/v1/me
```

### Summarize Text

```bash
POST /api/v1/summarize
Content-Type: application/json

{
  "text": "Your long text to summarize...",
  "model": "llama3.2",
  "max_length": 300,
  "style": "concise"
}
```

### Upload and Summarize Document

```bash
POST /api/v1/summarize/document
Content-Type: multipart/form-data

file=@document.pdf
model=llama3.2
max_length=500
style=detailed
```

### Custom Query

```bash
POST /api/v1/query
Content-Type: application/x-www-form-urlencoded

prompt=What are the main themes?
context=Optional document context...
model=llama3.2
```

### List Available Models

```bash
GET /api/v1/models
```

## Summary Styles

| Style          | Description                                          |
|----------------|------------------------------------------------------|
| `concise`      | Brief summary capturing main points                  |
| `detailed`     | Comprehensive summary with nuances                   |
| `bullet_points`| Key information as bullet points                     |
| `executive`    | Business-focused summary with recommendations        |
| `academic`     | Structured academic summary                          |

## Configuration

### Environment Variables

| Variable                    | Description                              | Default                    |
|-----------------------------|------------------------------------------|----------------------------|
| `KEYCLOAK_SERVER_URL`       | Keycloak server URL                      | `http://localhost:8080`    |
| `KEYCLOAK_REALM`            | Keycloak realm name                      | `document-api-realm`       |
| `KEYCLOAK_CLIENT_ID`        | OAuth2 client ID                         | `document-api`             |
| `KEYCLOAK_CLIENT_SECRET`    | OAuth2 client secret                     | -                          |
| `KEYCLOAK_VERIFY_AUDIENCE`  | Verify token audience                    | `true`                     |
| `KEYCLOAK_VERIFY_ISSUER`    | Verify token issuer                      | `true`                     |
| `KEYCLOAK_USE_INTROSPECTION`| Use token introspection                  | `false`                    |
| `OLLAMA_URL`                | Ollama API URL                           | `http://localhost:11434`   |
| `OLLAMA_DEFAULT_MODEL`      | Default Ollama model                     | `llama3.2`                 |
| `OLLAMA_TIMEOUT`            | Ollama request timeout (seconds)         | `120.0`                    |

## Local Development

### Setup Virtual Environment

```bash
python -m venv venv
source venv/bin/activate  # Linux/Mac
# or
.\venv\Scripts\activate  # Windows

pip install -r requirements.txt
```

### Run Locally

```bash
# Make sure Keycloak and Ollama are running
uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
```

### Run Tests

```bash
pytest tests/ -v --cov=app
```

## Keycloak Configuration

### Pre-configured Users (Development)

| Username    | Password        | Roles                              |
|-------------|-----------------|-------------------------------------|
| `testuser`  | `testpassword`  | user, api-access, summarize, query  |
| `adminuser` | `adminpassword` | admin, all roles                    |

### Adding a New Client

1. Login to Keycloak Admin Console
2. Select `document-api-realm`
3. Go to Clients → Create client
4. Configure OAuth2 settings
5. Update `.env` with new client credentials

## Production Deployment

### Security Checklist

- [ ] Change all default passwords
- [ ] Use HTTPS for all services
- [ ] Configure proper CORS origins
- [ ] Enable token introspection for sensitive operations
- [ ] Set appropriate token lifespans
- [ ] Use PostgreSQL for Keycloak (not H2)
- [ ] Configure rate limiting
- [ ] Enable audit logging

### Docker Production Setup

```bash
# Use production profile
docker-compose --profile production up -d

# Or use external Keycloak/Ollama
docker-compose up -d api
```

## Project Structure

```
keycloak-ollama-api/
├── app/
│   ├── __init__.py
│   ├── main.py              # FastAPI application
│   ├── config.py            # Configuration management
│   ├── auth/
│   │   ├── __init__.py
│   │   └── keycloak.py      # Keycloak authentication
│   ├── models/
│   │   ├── __init__.py
│   │   └── schemas.py       # Pydantic models
│   └── services/
│       ├── __init__.py
│       └── ollama_service.py # Ollama integration
├── keycloak/
│   └── realm-export.json    # Keycloak realm config
├── tests/
│   └── ...
├── .env.example
├── docker-compose.yml
├── Dockerfile
├── requirements.txt
└── README.md
```

## Troubleshooting

### Common Issues

**1. Token validation fails**
- Ensure Keycloak is running and accessible
- Check `KEYCLOAK_SERVER_URL` matches your setup
- Verify client ID and secret are correct

**2. Ollama connection refused**
- Ensure Ollama is running: `ollama serve`
- Check `OLLAMA_URL` is correct
- Verify the model is pulled: `ollama list`

**3. CORS errors**
- Add your frontend URL to `CORS_ORIGINS`
- Ensure web origins are configured in Keycloak client

## License

MIT License - See LICENSE file for details.
