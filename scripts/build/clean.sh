#!/bin/bash
set -e

echo "==================================="
echo "Clean Claude Harness Java Native"
echo "==================================="

echo ""
echo "Cleaning all build artifacts..."
mvn clean

echo ""
echo "✅ Clean completed successfully!"
echo "All target directories and build artifacts removed."