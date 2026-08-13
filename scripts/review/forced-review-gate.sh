#!/bin/bash
# 强制审查集成脚本
# 在harness-work完成后自动调用harness-review进行代码审查

set -euo pipefail

# 脚本根目录
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
HARNESS_PLUGIN_ROOT="${HARNESS_PLUGIN_ROOT:-${SCRIPT_DIR}}"

# 加载工具函数
source "${HARNESS_PLUGIN_ROOT}/scripts/config-utils.sh"

# 默认配置
REVIEW_MODE="${HARNESS_REVIEW_MODE:-strict}"
MAX_RETRIES="${HARNESS_MAX_REVIEW_ITERATIONS:-3}"
TIMEOUT="${HARNESS_REVIEW_TIMEOUT:-30}"
OUTPUT_FILE="${HARNESS_REVIEW_OUTPUT:-/tmp/harness-review-result-$$}.json"

# 参数解析
BASE_REF=""
WORKTREE_PATH=""
SKIP_REASON=""

usage() {
    cat <<EOF
强制审查集成脚本 - 确保所有harness-work执行都经过代码审查

用法:
    $0 --base-ref <commit> --worktree-path <path> [options]

参数:
    --base-ref <commit>        基准 commit SHA (必需)
    --worktree-path <path>     工作树路径 (必需)
    --mode <strict|lenient>    审查模式 (默认: strict)
    --max-retries <n>          最大重试次数 (默认: 3)
    --timeout <seconds>        超时时间 (默认: 30)
    --output <file>            输出文件路径 (默认: /tmp/harness-review-result-PID.json)
    --skip <reason>            跳过审查（仅紧急情况）

环境变量:
    HARNESS_REVIEW_MODE        审查模式 (strict|lenient)
    HARNESS_MAX_REVIEW_ITERATIONS  最大重试次数
    HARNESS_REVIEW_TIMEOUT     超时时间
    HARNESS_REVIEW_OUTPUT      输出文件路径
    HARNESS_SKIP_REVIEW        跳过审查 (true|false)

示例:
    # 基本使用
    $0 --base-ref abc123 --worktree-path /path/to/worktree

    # 宽松模式
    $0 --base-ref abc123 --worktree-path /path/to/worktree --mode lenient

    # 紧急情况跳过（不推荐）
    $0 --base-ref abc123 --worktree-path /path/to/worktree --skip "emergency_fix"

退出码:
    0 - 审查通过 (APPROVE)
    1 - 审查未通过 (REQUEST_CHANGES)
    2 - 审查执行失败
    3 - 参数错误
EOF
    exit 3
}

# 解析参数
while [[ $# -gt 0 ]]; do
    case $1 in
        --base-ref)
            BASE_REF="$2"
            shift 2
            ;;
        --worktree-path)
            WORKTREE_PATH="$2"
            shift 2
            ;;
        --mode)
            REVIEW_MODE="$2"
            shift 2
            ;;
        --max-retries)
            MAX_RETRIES="$2"
            shift 2
            ;;
        --timeout)
            TIMEOUT="$2"
            shift 2
            ;;
        --output)
            OUTPUT_FILE="$2"
            shift 2
            ;;
        --skip)
            SKIP_REASON="$2"
            shift 2
            ;;
        -h|--help)
            usage
            ;;
        *)
            echo "❌ 未知参数: $1" >&2
            usage
            ;;
    esac
done

# 验证必需参数
if [[ -z "$BASE_REF" ]] || [[ -z "$WORKTREE_PATH" ]]; then
    echo "❌ 缺少必需参数" >&2
    usage
fi

# 检查跳过条件
should_skip_review() {
    # 检查环境变量
    if [[ "${HARNESS_SKIP_REVIEW:-false}" == "true" ]]; then
        return 0
    fi

    # 检查参数
    if [[ -n "$SKIP_REASON" ]]; then
        return 0
    fi

    # 检查配置文件
    local harness_toml="${WORKTREE_PATH}/harness.toml"
    if [[ -f "$harness_toml" ]]; then
        if grep -q "skip_review = true" "$harness_toml" 2>/dev/null; then
            return 0
        fi
    fi

    return 1
}

# 创建跳过结果
create_skip_result() {
    local reason="$1"
    cat > "$OUTPUT_FILE" <<EOF
{
  "verdict": "APPROVE",
  "skip_reason": "${reason}",
  "findings": [],
  "summary": "审查被跳过: ${reason}",
  "review_time": "$(date -u +"%Y-%m-%dT%H:%M:%SZ")",
  "performance": {
    "duration_ms": 0,
    "files_reviewed": 0
  }
}
EOF
}

# 调用harness-review --auto
call_review_auto() {
    local base_ref="$1"
    local worktree_path="$2"
    local output_file="$3"
    local mode="$4"

    echo "🔍 调用强制审查..."

    # 检查harness-review是否可用
    if ! command -v harness-review &> /dev/null; then
        echo "❌ harness-review 命令不可用" >&2
        return 1
    fi

    # 调用harness-review --auto
    local start_time=$(date +%s%3N)

    harness-review --auto \
        --base-ref "$base_ref" \
        --output "$output_file" \
        --mode "$mode" \
        2>&1

    local exit_code=$?

    local end_time=$(date +%s%3N)
    local duration=$((end_time - start_time))

    if [[ $exit_code -eq 0 ]] && [[ -f "$output_file" ]]; then
        echo "✅ 审查完成 (耗时: ${duration}ms)"
        return 0
    else
        echo "❌ 审查失败 (退出码: $exit_code)" >&2
        return 1
    fi
}

# 轻量级审查降级方案
lightweight_review_fallback() {
    local base_ref="$1"
    local worktree_path="$2"
    local output_file="$3"

    echo "⚠️  使用轻量级审查降级方案..."

    local changed_files=()
    local findings=()

    # 获取变更文件
    while IFS= read -r file; do
        [[ -n "$file" ]] && changed_files+=("$file")
    done < <(git -C "$worktree_path" diff --name-only "$base_ref..HEAD" 2>/dev/null)

    if [[ ${#changed_files[@]} -eq 0 ]]; then
        # 没有变更，通过
        cat > "$output_file" <<EOF
{
  "verdict": "APPROVE",
  "findings": [],
  "summary": "无文件变更，轻量级审查通过",
  "review_time": "$(date -u +"%Y-%m-%dT%H:%M:%SZ")",
  "performance": {
    "duration_ms": 10,
    "files_reviewed": 0
  }
}
EOF
        return 0
    fi

    # 基础检查
    for file in "${changed_files[@]}"; do
        local file_path="${worktree_path}/${file}"

        if [[ ! -f "$file_path" ]]; then
            continue
        fi

        # 检查明显的错误模式
        if grep -q "TODO.*FIXME.*FIXME" "$file_path" 2>/dev/null; then
            findings+=("{\"severity\": \"minor\", \"file\": \"${file}\", \"line\": 0, \"message\": \"包含多个TODO/FIXME标记\"}")
        fi

        # 检查调试语句
        if grep -q "console.log\|System.out.println\|print(" "$file_path" 2>/dev/null; then
            findings+=("{\"severity\": \"major\", \"file\": \"${file}\", \"line\": 0, \"message\": \"包含调试输出语句\"}")
        fi
    done

    # 生成结果
    local verdict="APPROVE"
    local summary="轻量级审查完成"

    # 检查是否有major问题
    for finding in "${findings[@]}"; do
        if [[ "$finding" =~ "major" ]]; then
            verdict="REQUEST_CHANGES"
            summary="轻量级审查发现问题"
            break
        fi
    done

    # 生成JSON findings数组
    local findings_json="["
    local first=true
    for finding in "${findings[@]}"; do
        if [[ "$first" == "true" ]]; then
            first=false
        else
            findings_json+=","
        fi
        findings_json+="$finding"
    done
    findings_json+="]"

    cat > "$output_file" <<EOF
{
  "verdict": "${verdict}",
  "findings": ${findings_json},
  "summary": "${summary}",
  "review_time": "$(date -u +"%Y-%m-%dT%H:%M:%SZ")",
  "performance": {
    "duration_ms": 50,
    "files_reviewed": ${#changed_files[@]}
  }
}
EOF

    return 0
}

# 主执行逻辑
main() {
    echo "🚀 强制审查集成启动"
    echo "   基准: ${BASE_REF}"
    echo "   工作树: ${WORKTREE_PATH}"
    echo "   模式: ${REVIEW_MODE}"

    # 检查是否应该跳过审查
    if should_skip_review; then
        if [[ -n "$SKIP_REASON" ]]; then
            echo "⚠️  跳过审查: ${SKIP_REASON}"
            create_skip_result "$SKIP_REASON"
        else
            echo "⚠️  跳过审查: 配置或环境变量要求"
            create_skip_result "配置要求跳过"
        fi
        exit 0
    fi

    # 尝试完整审查
    if call_review_auto "$BASE_REF" "$WORKTREE_PATH" "$OUTPUT_FILE" "$REVIEW_MODE"; then
        # 审查成功，检查结果
        if [[ -f "$OUTPUT_FILE" ]]; then
            local verdict=$(jq -r '.verdict' "$OUTPUT_FILE" 2>/dev/null)
            echo "📋 审查结果: ${verdict}"

            if [[ "$verdict" == "APPROVE" ]]; then
                echo "✅ 审查通过"
                exit 0
            else
                echo "❌ 审查未通过"
                # 显示问题摘要
                local critical_count=$(jq '[.findings[] | select(.severity == "critical")] | length' "$OUTPUT_FILE" 2>/dev/null || echo "0")
                local major_count=$(jq '[.findings[] | select(.severity == "major")] | length' "$OUTPUT_FILE" 2>/dev/null || echo "0")

                echo "   Critical: ${critical_count}, Major: ${major_count}"

                exit 1
            fi
        else
            echo "❌ 输出文件不存在" >&2
            exit 2
        fi
    else
        # 完整审查失败，使用降级方案
        echo "⚠️  完整审查失败，使用轻量级降级方案"

        if lightweight_review_fallback "$BASE_REF" "$WORKTREE_PATH" "$OUTPUT_FILE"; then
            if [[ -f "$OUTPUT_FILE" ]]; then
                local verdict=$(jq -r '.verdict' "$OUTPUT_FILE" 2>/dev/null)
                echo "📋 降级审查结果: ${verdict}"

                if [[ "$verdict" == "APPROVE" ]]; then
                    echo "✅ 降级审查通过"
                    exit 0
                else
                    echo "❌ 降级审查未通过"
                    exit 1
                fi
            else
                echo "❌ 降级审查输出文件不存在" >&2
                exit 2
            fi
        else
            echo "❌ 降级审查执行失败" >&2
            exit 2
        fi
    fi
}

# 执行主函数
main "$@"