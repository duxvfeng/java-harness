#!/bin/bash
set -e

echo "==================================="
echo "Running Integration Tests"
echo "==================================="

# 确保服务正在运行
echo "Checking service availability..."
if ! curl -s http://localhost:8080/api/health > /dev/null; then
    echo "Error: Spring Boot Service is not running"
    echo "Please start the service first:"
    echo "  ./start-service.sh"
    exit 1
fi

echo "Service is running ✓"

# 测试健康检查
echo ""
echo "Test 1: Health Check"
curl -s http://localhost:8080/api/health | jq '.'

# 测试创建会话
echo ""
echo "Test 2: Create Session"
SESSION_RESPONSE=$(curl -s -X POST http://localhost:8080/api/state/sessions \
    -H "Content-Type: application/json" \
    -d '{"projectRoot":"/test/project"}')
echo "$SESSION_RESPONSE" | jq '.'

SESSION_ID=$(echo "$SESSION_RESPONSE" | jq -r '.id')
echo "Created session: $SESSION_ID"

# 测试查询会话
echo ""
echo "Test 3: Query Session"
curl -s http://localhost:8080/api/state/sessions/$SESSION_ID | jq '.'

# 测试创建工作状态
echo ""
echo "Test 4: Create Work State"
curl -s -X POST http://localhost:8080/api/state/work-states \
    -H "Content-Type: application/json" \
    -d "{\"sessionId\":\"$SESSION_ID\",\"status\":\"pending\"}" | jq '.'

# 测试查询工作状态
echo ""
echo "Test 5: Query Work States"
curl -s http://localhost:8080/api/state/work-states/$SESSION_ID | jq '.'

# 测试状态查询 API
echo ""
echo "Test 6: State Query API"
curl -s -X POST http://localhost:8080/api/state/query \
    -H "Content-Type: application/json" \
    -d "{\"sessionId\":\"$SESSION_ID\",\"queryType\":\"WORK_STATES\"}" | jq '.'

# 测试编排服务
echo ""
echo "Test 7: Orchestrator Health"
curl -s http://localhost:8080/api/orchestrator/health | jq '.'

echo ""
echo "==================================="
echo "All integration tests passed! ✓"
echo "==================================="
