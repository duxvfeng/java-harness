#!/bin/bash
# 会话保存脚本 - 直接保存当前会话状态

TIMESTAMP=$(date +%Y%m%d-%H%M%S)
SESSION_DIR=".claude/state/session-saves"
SUMMARY="$1"

# 创建会话目录
mkdir -p "$SESSION_DIR"

# 保存会话信息
SESSION_FILE="$SESSION_DIR/session-${TIMESTAMP}.json"

cat > "$SESSION_FILE" << EOF
{
  "session_id": "session-${TIMESTAMP}",
  "timestamp": "${TIMESTAMP}",
  "summary": "${SUMMARY}",
  "saved_at": "$(date -Iseconds)",
  "project_root": "$(pwd)",
  "git_branch": "$(git rev-parse --abbrev-ref HEAD 2>/dev/null || echo 'unknown')",
  "git_commit": "$(git rev-parse HEAD 2>/dev/null || echo 'unknown')",
  "context": "Phase 12 完成 - 智能模型选择系统"
}
EOF

echo "✅ 会话已保存: ${SESSION_FILE}"
echo "📋 摘要: ${SUMMARY}"
echo "⏰ 时间: $(date)"
echo ""
echo "💡 使用以下命令恢复:"
echo "   cat ${SESSION_FILE} | grep -E '(session_id|timestamp|summary)'"