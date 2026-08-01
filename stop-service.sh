#!/bin/bash
set -e

echo "Stopping Claude Harness Services..."

# 查找并停止 Spring Boot Service
PIDS=$(ps aux | grep 'spring-service' | grep -v grep | awk '{print $2}')

if [ -z "$PIDS" ]; then
    echo "No running services found"
else
    echo "Stopping services (PIDs: $PIDS)"
    echo $PIDS | xargs kill
    echo "Services stopped successfully"
fi
