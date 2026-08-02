#!/bin/bash
set -e

echo "==================================="
echo "Starting Claude Harness Services"
echo "==================================="

# 检查 Java 环境
if ! command -v java &> /dev/null; then
    echo "Error: Java is not installed"
    exit 1
fi

# 启动 Spring Boot Service
echo ""
echo "Starting Spring Boot Service..."
cd spring-service
mvn spring-boot:run &
SERVICE_PID=$!
echo "Spring Boot Service started (PID: $SERVICE_PID)"
cd ..

# 等待服务启动
echo "Waiting for service to be ready..."
sleep 10

# 检查服务状态
echo "Checking service health..."
curl -s http://localhost:8080/api/health | jq '.' || echo "Service check failed"

echo ""
echo "==================================="
echo "Services started successfully!"
echo "==================================="
echo ""
echo "Spring Boot Service:"
echo "  - PID: $SERVICE_PID"
echo "  - Health: http://localhost:8080/api/health"
echo "  - API Docs: http://localhost:8080/api/"
echo ""
echo "To stop the service, run:"
echo "  kill $SERVICE_PID"
echo ""
echo "Or run:"
echo "  ./stop-service.sh"
