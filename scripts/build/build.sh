#!/bin/bash
set -e

echo "==================================="
echo "Building Claude Harness Java Native"
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
echo "Step 3: Package"
mvn package -DskipTests

echo ""
echo "Build completed successfully!"
echo "JAR file: cli-native/target/harness-cli-native-4.0.0-java-SNAPSHOT.jar"
echo ""
echo "To build native image, run:"
echo "  mvn -Pnative native:compile -DskipTests"
