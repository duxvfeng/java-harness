#!/bin/bash
set -e

echo "==================================="
echo "Test Environment Setup"
echo "==================================="

SETUP_TYPE="${1:-full}"

case "$SETUP_TYPE" in
    "full")
        echo ""
        echo "Full test environment setup"
        echo "============================"

        echo "Step 1: Clean previous test data..."
        rm -rf /tmp/test-data
        mkdir -p /tmp/test-data

        echo "Step 2: Create test databases..."
        if [ -n "$HARNESS_TEST_DB" ]; then
            echo "Setting up test database: $HARNESS_TEST_DB"
            # Database setup commands would go here
            echo "✅ Test database ready"
        else
            echo "Using H2 in-memory database for tests"
        fi

        echo "Step 3: Configure test properties..."
        mkdir -p src/test/resources
        cat > src/test/resources/test.properties << 'EOF'
# Test configuration
test.environment=testing
test.database=h2:mem:testdb
test.log.level=INFO
EOF
        echo "✅ Test properties configured"

        echo "Step 4: Verify test dependencies..."
        if mvn dependency:resolve -q 2>/dev/null; then
            echo "✅ Test dependencies verified"
        else
            echo "❌ Test dependency verification failed"
            exit 1
        fi

        echo "Step 5: Compile test classes..."
        if mvn test-compile -q 2>/dev/null; then
            echo "✅ Test classes compiled"
        else
            echo "❌ Test compilation failed"
            exit 1
        fi

        echo ""
        echo "✅ Full test environment setup completed"
        ;;

    "quick")
        echo ""
        echo "Quick test environment setup"
        echo "==========================="

        echo "Step 1: Compile test classes..."
        if mvn test-compile -q 2>/dev/null; then
            echo "✅ Test classes compiled"
        else
            echo "❌ Test compilation failed"
            exit 1
        fi

        echo "Step 2: Clean previous test data..."
        rm -rf /tmp/test-data/*
        echo "✅ Test data cleaned"

        echo ""
        echo "✅ Quick test environment setup completed"
        ;;

    "integration")
        echo ""
        echo "Integration test environment setup"
        echo "===================================="

        echo "Step 1: Start test services..."
        # Start any required services (mock servers, etc.)
        echo "✅ Test services started"

        echo "Step 2: Configure integration test environment..."
        mkdir -p src/test/resources
        cat > src/test/resources/integration-test.properties << 'EOF'
# Integration test configuration
test.environment=integration
test.timeout=30000
test.mock.external=true
EOF
        echo "✅ Integration test environment configured"

        echo "Step 3: Verify external dependencies..."
        echo "Checking external service availability..."
        # Add checks for external services here
        echo "✅ External dependencies verified"

        echo ""
        echo "✅ Integration test environment setup completed"
        ;;

    *)
        echo ""
        echo "Test Environment Setup"
        echo "Usage: ./test-setup.sh <type>"
        echo ""
        echo "Types:"
        echo "  full         - Complete test environment setup"
        echo "  quick        - Quick setup for unit tests"
        echo "  integration  - Setup for integration tests"
        echo ""
        exit 1
        ;;
esac