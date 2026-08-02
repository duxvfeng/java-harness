#!/bin/bash
set -e

echo "==================================="
echo "Running Claude Harness Tests"
echo "==================================="

# Run all tests
mvn test

echo ""
echo "All tests passed!"
echo ""
echo "To run specific tests:"
echo "  mvn test -Dtest=HookCodecTest"
echo "  mvn test -Dtest=GuardrailEngineTest"
echo "  mvn test -Dtest=HookRouterTest"
