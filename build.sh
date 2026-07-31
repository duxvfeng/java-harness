#!/bin/bash
# Quick build script for Java Harness

set -e

echo "🚀 Building Claude Code Harness (Java)..."

cd "$(dirname "$0")"

# Clean and compile
echo "📦 Compiling project..."
mvn clean compile

if [ $? -eq 0 ]; then
    echo "✅ Build successful!"
    echo ""
    echo "📋 Build artifacts:"
    echo "  - Shared module: shared/target/harness-shared-4.0.0-java-SNAPSHOT.jar"
    echo "  - CLI module: cli-native/target/harness-cli-native-4.0.0-java-SNAPSHOT.jar"
    echo ""
    echo "🧪 To run the application:"
    echo "  java -cp cli-native/target/harness-cli-native-4.0.0-java-SNAPSHOT.jar com.chachamaru.harness.cli.HarnessCli"
    echo ""
    echo "🔧 To compile as Native Image (requires GraalVM):"
    echo "  cd cli-native && mvn -Pnative native:compile"
else
    echo "❌ Build failed!"
    exit 1
fi