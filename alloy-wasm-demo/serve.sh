#!/bin/bash

echo "════════════════════════════════════════════════════════"
echo "   Alloy WASM Demo - Local Server"
echo "════════════════════════════════════════════════════════"
echo ""

# Try to find an available HTTP server
if command -v python3 &> /dev/null; then
    echo "Starting server with Python 3..."
    echo "Open http://localhost:8000 in your browser"
    echo ""
    echo "Press Ctrl+C to stop the server"
    echo ""
    python3 -m http.server 8000
elif command -v python &> /dev/null; then
    echo "Starting server with Python 2..."
    echo "Open http://localhost:8000 in your browser"
    echo ""
    echo "Press Ctrl+C to stop the server"
    echo ""
    python -m SimpleHTTPServer 8000
elif command -v php &> /dev/null; then
    echo "Starting server with PHP..."
    echo "Open http://localhost:8000 in your browser"
    echo ""
    echo "Press Ctrl+C to stop the server"
    echo ""
    php -S localhost:8000
else
    echo "❌ No suitable HTTP server found!"
    echo ""
    echo "Please install one of the following:"
    echo "  - Python 3: apt-get install python3 (Linux) or brew install python3 (Mac)"
    echo "  - Node.js: then run 'npx http-server -p 8000'"
    echo ""
    echo "Or open index.html directly in your browser (may have CORS limitations)"
    exit 1
fi
