#!/bin/bash
# harness-review --auto 模式的实现脚本
# 此脚本展示如何实现自动审查模式

set -e

# 默认参数
BASE_REF=""
OUTPUT_FILE=""
MODE="strict"
TIMEOUT=30
DEBUG=false

# 解析参数
while [[ $# -gt 0 ]]; do
    case $1 in
        --auto)
            shift
            ;;
        --base-ref)
            BASE_REF="$2"
            shift 2
            ;;
        --output)
            OUTPUT_FILE="$2"
            shift 2
            ;;
        --mode)
            MODE="$2"
            shift 2
            ;;
        --debug)
            DEBUG=true
            shift
            ;;
        *)
            echo "未知参数: $1"
            exit 1
            ;;
    esac
done

# 验证必需参数
if [[ -z "$BASE_REF" ]]; then
    echo "错误: 缺少 --base-ref 参数" >&2
    exit 1
fi

if [[ -z "$OUTPUT_FILE" ]]; then
    echo "错误: 缺少 --output 参数" >&2
    exit 1
fi

# 获取项目根目录
PROJECT_ROOT="$(git rev-parse --show-toplevel)"
cd "$PROJECT_ROOT"

# 获取变更的文件
CHANGED_FILES=$(git diff --name-only "${BASE_REF}..HEAD")
if [[ -z "$CHANGED_FILES" ]]; then
    echo "警告: 没有检测到文件变更" >&2
    # 输出空的审查结果
    cat > "$OUTPUT_FILE" <<'EOF'
{
  "verdict": "APPROVE",
  "findings": [],
  "summary": "没有文件变更需要审查",
  "review_time": "2024-08-10T10:30:00Z",
  "performance": {
    "duration_ms": 0,
    "files_reviewed": 0
  }
}
EOF
    exit 0
fi

# 开始计时
START_TIME=$(date +%s%3N)

# 初始化结果数组
FINDINGS=[]
CRITICAL_COUNT=0
MAJOR_COUNT=0
MINOR_COUNT=0
RECOMMENDATION_COUNT=0

# 检测文件语言并应用相应标准
while IFS= read -r file; do
    if [[ ! -f "$file" ]]; then
        continue
    fi

    # 跳过非代码文件
    case "$file" in
        *.md|*.txt|*.json|*.yaml|*.yml|*.toml)
            continue
            ;;
    esac

    # 检测语言
    LANGUAGE=""
    case "$file" in
        *.java)
            LANGUAGE="java"
            ;;
        *.py|*.pyi)
            LANGUAGE="python"
            ;;
        *.vue)
            LANGUAGE="vue"
            ;;
        *.go)
            LANGUAGE="go"
            ;;
        *.js|*.ts|*.jsx|*.tsx)
            LANGUAGE="javascript"
            ;;
        *)
            continue  # 不支持的语言
            ;;
    esac

    if [[ "$DEBUG" == true ]]; then
        echo "审查文件: $file (语言: $LANGUAGE)" >&2
    fi

    # 这里应该调用相应的语言检查器
    # 简化版本：只是示例性的检查
    case "$LANGUAGE" in
        java)
            # 示例：检查Java文件的一些基础问题
            if grep -q "System.out.println" "$file"; then
                FINDINGS+=("$(cat <<EOF
{
  "severity": "minor",
  "file": "$file",
  "line": "$(grep -n 'System.out.println' "$file" | cut -d: -f1)",
  "rule": "no-system-out",
  "message": "避免使用 System.out.println",
  "suggestion": "使用日志框架如 SLF4J"
}
EOF
)")
                MINOR_COUNT=$((MINOR_COUNT + 1))
            fi
            ;;
        python)
            # 示例：检查Python文件的基础问题
            if grep -q "print(" "$file"; then
                FINDINGS+=("$(cat <<EOF
{
  "severity": "minor",
  "file": "$file",
  "line": "$(grep -n 'print(' "$file" | cut -d: -f1)",
  "rule": "no-print-statements",
  "message": "避免使用 print 语句",
  "suggestion": "使用日志框架"
}
EOF
)")
                MINOR_COUNT=$((MINOR_COUNT + 1))
            fi
            ;;
    esac

done <<< "$CHANGED_FILES"

# 计算持续时间
END_TIME=$(date +%s%3N)
DURATION=$((END_TIME - START_TIME))

# 判定 verdict
case "$MODE" in
    strict)
        if [[ $CRITICAL_COUNT -gt 0 ]] || [[ $MAJOR_COUNT -gt 0 ]]; then
            VERDICT="REQUEST_CHANGES"
        else
            VERDICT="APPROVE"
        fi
        ;;
    lenient)
        if [[ $CRITICAL_COUNT -gt 0 ]]; then
            VERDICT="REQUEST_CHANGES"
        else
            VERDICT="APPROVE"
        fi
        ;;
    *)
        echo "错误: 未知的模式 $MODE" >&2
        exit 1
        ;;
esac

# 构建JSON输出
cat > "$OUTPUT_FILE" <<EOF
{
  "verdict": "$VERDICT",
  "findings": [
$(IFS=,; echo "${FINDINGS[*]}")
  ],
  "summary": "审查完成: $CRITICAL_COUNT 个 critical, $MAJOR_COUNT 个 major, $MINOR_COUNT 个 minor, $RECOMMENDATION_COUNT 个 recommendation",
  "review_time": "$(date -u +"%Y-%m-%dT%H:%M:%SZ")",
  "performance": {
    "duration_ms": $DURATION,
    "files_reviewed": $(echo "$CHANGED_FILES" | wc -l)
  }
}
EOF

if [[ "$DEBUG" == true ]]; then
    echo "审查完成: verdict=$VERDICT, 耗时=${DURATION}ms" >&2
fi

exit 0
