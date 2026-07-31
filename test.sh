#!/bin/bash
# Test script for Java Harness CLI Gateway

set -e

echo "🧪 Testing Java Harness CLI Gateway..."
echo ""

cd "$(dirname "$0")"

# Build project first
echo "📦 Building project..."
mvn clean compile -q

if [ $? -ne 0 ]; then
    echo "❌ Build failed!"
    exit 1
fi

echo "✅ Build successful!"
echo ""

# Get the classpath
CLASSPATH="cli-native/target/classes:shared/target/classes"

# Test 1: Safe operation (should allow)
echo "🔍 Test 1: Safe operation (Read README.md)"
OUTPUT=$(echo "$(cat test-input-safe.json)" | java -cp $CLASSPATH com.chachamaru.harness.cli.HarnessCli 2>&1 || true)
EXIT_CODE=$?
echo "Output: $OUTPUT"
echo "Exit code: $EXIT_CODE"
if [ "$EXIT_CODE" -eq 0 ]; then
    echo "✅ Test 1 PASSED - Safe operation allowed"
else
    echo "❌ Test 1 FAILED - Safe operation should be allowed"
fi
echo ""

# Test 2: Dangerous operation (should deny)
echo "🔍 Test 2: Dangerous operation (sudo rm -rf)"
OUTPUT=$(echo "$(cat test-input-dangerous.json)" | java -cp $CLASSPATH com.chachamaru.harness.cli.HarnessCli 2>&1 || true)
EXIT_CODE=$?
echo "Output: $OUTPUT"
echo "Exit code: $EXIT_CODE"
if [ "$EXIT_CODE" -eq 2 ]; then
    echo "✅ Test 2 PASSED - Dangerous operation blocked"
else
    echo "❌ Test 2 FAILED - Dangerous operation should be blocked"
fi
echo ""

echo "🎉 Testing completed!"
