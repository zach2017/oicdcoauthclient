@echo off
echo Setting Environment Variables for Vite...

:: Keycloak Configuration
setx VITE_KEYCLOAK_URL "http://localhost:8180"
setx VITE_KEYCLOAK_REALM "demo"
setx VITE_KEYCLOAK_CLIENT_ID "react-client"

:: API Configuration
setx VITE_API_URL "http://localhost:8080"

:: App URL
setx VITE_APP_URL "http://localhost:3000"

echo.
echo Success! Please restart your terminal or IDE for changes to take effect.
pause