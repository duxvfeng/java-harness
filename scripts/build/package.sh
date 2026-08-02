#!/bin/bash
set -e

echo "==================================="
echo "Packaging Claude Harness Java Native"
echo "==================================="

# Check Maven
if ! command -v mvn &> /dev/null; then
    echo "Error: Maven is not installed"
    exit 1
fi

echo ""
echo "Packaging all modules..."
mvn package -DskipTests

echo ""
echo "✅ Package completed successfully!"
echo ""
echo "Artifacts created:"
find . -name "*.jar" -not -path "*/target/*" -not -name "*-sources.jar" -not -name "*-javadoc.jar" | head -10
echo ""
echo "To run tests:"
echo "  ./test.sh"
echo ""
echo "To install locally:"
echo "  ./install.sh"