#!/bin/bash
# 会话列表脚本

SESSION_DIR=".claude/state/session-saves"
COUNT="${1:-5}"

echo "📋 最近的会话 (最新 ${COUNT} 个):"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

if [ ! -d "$SESSION_DIR" ]; then
    echo "❌ 没有找到会话目录: $SESSION_DIR"
    exit 1
fi

# 列出最近的会话文件
find "$SESSION_DIR" -name "session-*.json" -type f | sort -r | head -n "$COUNT" | while read -r file; do
    SESSION_ID=$(basename "$file" .json)
    TIMESTAMP=$(grep '"timestamp"' "$file" | sed 's/.*"timestamp"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/')
    SUMMARY=$(grep '"summary"' "$file" | sed 's/.*"summary"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/')

    echo ""
    echo "📁 ${SESSION_ID}"
    echo "   ⏰ ${TIMESTAMP}"
    echo "   📝 ${SUMMARY}"
    echo "   📍 $(pwd)"
done

echo ""
echo "💡 使用方式:"
echo "   查看详情: cat .claude/state/session-saves/<session-id>.json"