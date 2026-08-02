#!/bin/bash
set -e

echo "==================================="
echo "Managing Claude Harness Dependencies"
echo "==================================="

# Check Maven
if ! command -v mvn &> /dev/null; then
    echo "Error: Maven is not installed"
    exit 1
fi

case "${1:-help}" in
    "list")
        echo ""
        echo "Listing all dependencies..."
        mvn dependency:list
        ;;
    "tree")
        echo ""
        echo "Showing dependency tree..."
        mvn dependency:tree
        ;;
    "analyze")
        echo ""
        echo "Analyzing dependency usage..."
        mvn dependency:analyze
        ;;
    "resolve")
        echo ""
        echo "Resolving all dependencies..."
        mvn dependency:resolve
        ;;
    "sources")
        echo ""
        echo "Downloading dependency sources..."
        mvn dependency:sources
        ;;
    "updates")
        echo ""
        echo "Checking for dependency updates..."
        mvn versions:display-dependency-updates
        ;;
    "clean")
        echo ""
        echo "Cleaning unused dependencies..."
        mvn dependency:purge-local-repository -DmanualInclude="com.chachamaru:claude-harness-parent"
        ;;
    *)
        echo ""
        echo "Usage: ./dependencies.sh <command>"
        echo ""
        echo "Available commands:"
        echo "  list       - List all dependencies"
        echo "  tree       - Show dependency tree"
        echo "  analyze    - Analyze dependency usage"
        echo "  resolve    - Resolve all dependencies"
        echo "  sources    - Download dependency sources"
        echo "  updates    - Check for dependency updates"
        echo "  clean      - Clean unused dependencies"
        echo ""
        exit 1
        ;;
esac

echo ""
echo "✅ Dependency operation completed!"