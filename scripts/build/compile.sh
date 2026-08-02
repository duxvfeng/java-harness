#!/bin/bash
set -e

echo "==================================="
echo "Compiling Claude Harness Java Native"
echo "==================================="

# Check Maven
if ! command -v mvn &> /dev/null; then
    echo "Error: Maven is not installed"
    exit 1
fi

echo ""
echo "Compiling all modules..."
mvn compile

echo ""
echo "✅ Compilation completed successfully!"
echo ""
echo "To run tests:"
echo "  ./test.sh"
echo ""
echo "To package:"
echo "  ./package.sh"