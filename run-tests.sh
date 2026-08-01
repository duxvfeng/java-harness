#!/bin/bash
set -e

echo "==================================="
echo "Quick Test Runner"
echo "==================================="

# Run only fast unit tests
mvn test -q

echo ""
echo "✅ All unit tests passed!"
