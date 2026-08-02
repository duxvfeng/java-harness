#!/bin/bash
set -e

echo "==================================="
echo "Installing Claude Harness Java Native"
echo "==================================="

# Check Maven
if ! command -v mvn &> /dev/null; then
    echo "Error: Maven is not installed"
    exit 1
fi

echo ""
echo "Installing dependencies and artifacts..."
mvn install -DskipTests

echo ""
echo "✅ Installation completed successfully!"
echo "All modules installed to local Maven repository"
echo ""
echo "To build and test:"
echo "  ./build.sh"
echo ""
echo "To clean build artifacts:"
echo "  ./clean.sh"