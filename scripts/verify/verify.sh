#!/bin/bash
set -e

echo "==================================="
echo "Verifying Claude Harness Java Native"
echo "==================================="

# Check Maven
if ! command -v mvn &> /dev/null; then
    echo "Error: Maven is not installed"
    exit 1
fi

echo ""
echo "Step 1: Clean and compile"
mvn clean compile

echo ""
echo "Step 2: Run tests"
mvn test

echo ""
echo "Step 3: Verify packaging"
mvn verify

echo ""
echo "Step 4: Check test coverage"
mvn jacoco:report

echo ""
echo "✅ Verification completed successfully!"
echo ""
echo "Test coverage report:"
find . -name "jacoco.exec" -o -name "index.html" | grep jacoco | head -5
echo ""
echo "To build native image:"
echo "  mvn -Pnative native:compile -DskipTests"