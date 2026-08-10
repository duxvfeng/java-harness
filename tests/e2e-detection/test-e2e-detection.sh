#!/bin/bash
# 端到端检测系统集成测试
#
# 用途: 验证端到端检测系统的完整功能
# 使用: bash tests/e2e-detection/test-e2e-detection.sh
#
# @author Harness System
# @version 1.0.0

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 测试计数器
TESTS_TOTAL=0
TESTS_PASSED=0
TESTS_FAILED=0

# 脚本目录
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

# 打印函数
print_info() {
    echo -e "${BLUE}ℹ️  $1${NC}"
}

print_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

print_error() {
    echo -e "${RED}❌ $1${NC}"
}

print_test() {
    echo -e "${BLUE}🧪 $1${NC}"
}

# 测试函数
run_test() {
    local test_name="$1"
    local test_command="$2"

    TESTS_TOTAL=$((TESTS_TOTAL + 1))
    print_test "测试 $TESTS_TOTAL: $test_name"

    if eval "$test_command" > /dev/null 2>&1; then
        print_success "测试 $TESTS_TOTAL 通过"
        TESTS_PASSED=$((TESTS_PASSED + 1))
        return 0
    else
        print_error "测试 $TESTS_TOTAL 失败"
        TESTS_FAILED=$((TESTS_FAILED + 1))
        return 1
    fi
}

# 测试组开始
start_test_group() {
    local group_name="$1"
    echo ""
    echo -e "${BLUE}═══════════════════════════════════════════════════${NC}"
    echo -e "${BLUE}  $group_name${NC}"
    echo -e "${BLUE}═══════════════════════════════════════════════════${NC}"
}

# 测试组结束
end_test_group() {
    local group_name="$1"
    echo ""
    print_info "$group_name 完成"
}

# 基础环境测试
test_basic_environment() {
    start_test_group "基础环境测试"

    # 测试1: Node.js 可用性
    run_test "Node.js 可用性" "command -v node"

    # 测试2: npm 可用性
    run_test "npm 可用性" "command -v npm"

    # 测试3: 项目目录结构
    run_test "scripts 目录存在" "[ -d '$PROJECT_ROOT/scripts/e2e-detection' ]"
    run_test "config 目录存在" "[ -d '$PROJECT_ROOT/config' ]"
    run_test "tests 目录存在" "[ -d '$PROJECT_ROOT/tests/e2e-detection' ]"

    end_test_group "基础环境测试"
}

# 脚本文件测试
test_script_files() {
    start_test_group "脚本文件测试"

    # 必需脚本文件
    local required_scripts=(
        "scripts/e2e-detection/e2e-detection-manager.js"
        "scripts/e2e-detection/auto-fix-controller.js"
        "scripts/e2e-detection/e2e-detection-trigger.js"
        "scripts/e2e-detection/e2e-report-generator.js"
        "scripts/e2e-detection/setup-e2e-detection.sh"
    )

    for script in "${required_scripts[@]}"; do
        run_test "脚本文件存在: $script" "[ -f '$PROJECT_ROOT/$script' ]"
    done

    end_test_group "脚本文件测试"
}

# 脚本语法测试
test_script_syntax() {
    start_test_group "脚本语法测试"

    # 测试 JavaScript 文件语法
    local js_scripts=(
        "scripts/e2e-detection/e2e-detection-manager.js"
        "scripts/e2e-detection/auto-fix-controller.js"
        "scripts/e2e-detection/e2e-detection-trigger.js"
        "scripts/e2e-detection/e2e-report-generator.js"
        "scripts/e2e-detection/update-config.js"
    )

    for script in "${js_scripts[@]}"; do
        run_test "JavaScript 语法检查: $script" "node --check '$PROJECT_ROOT/$script'"
    done

    # 测试 Bash 脚本语法
    local bash_scripts=(
        "scripts/e2e-detection/setup-e2e-detection.sh"
    )

    for script in "${bash_scripts[@]}"; do
        run_test "Bash 语法检查: $script" "bash -n '$PROJECT_ROOT/$script'"
    done

    end_test_group "脚本语法测试"
}

# 配置文件测试
test_config_files() {
    start_test_group "配置文件测试"

    # 测试默认配置文件
    run_test "默认配置文件存在" "[ -f '$PROJECT_ROOT/config/e2e-detection.default.config.json' ]"

    # 测试默认配置文件语法
    run_test "默认配置文件 JSON 语法" "node -e 'JSON.parse(require(\"fs\").readFileSync(\"$PROJECT_ROOT/config/e2e-detection.default.config.json\", \"utf8\"))'"

    # 检查是否已安装配置
    if [ -f "$PROJECT_ROOT/.claude/config/e2e-detection.config.json" ]; then
        run_test "用户配置文件存在" "[ -f '$PROJECT_ROOT/.claude/config/e2e-detection.config.json' ]"
        run_test "用户配置文件 JSON 语法" "node -e 'JSON.parse(require(\"fs\").readFileSync(\"$PROJECT_ROOT/.claude/config/e2e-detection.config.json\", \"utf8\"))'"
    else
        print_warning "用户配置文件不存在（可能未运行设置脚本）"
    fi

    end_test_group "配置文件测试"
}

# 脚本功能测试
test_script_functionality() {
    start_test_group "脚本功能测试"

    # 测试帮助信息
    run_test "管理器帮助信息" "node '$PROJECT_ROOT/scripts/e2e-detection/e2e-detection-manager.js' --help"
    run_test "触发器帮助信息" "node '$PROJECT_ROOT/scripts/e2e-detection/e2e-detection-trigger.js' help"

    # 测试报告生成器（使用示例输入）
    local test_result='{ "detection_id": "test-123", "timestamp": "2024-01-01T00:00:00Z", "status": "PASS", "test_results": {}, "critical_issues": [], "execution_time": 1.5 }'
    local test_input_file="/tmp/e2e-test-input.json"

    echo "$test_result" > "$test_input_file"
    run_test "报告生成器基本功能" "node '$PROJECT_ROOT/scripts/e2e-detection/e2e-report-generator.js' '$test_input_file' console"
    rm -f "$test_input_file"

    end_test_group "脚本功能测试"
}

# 目录权限测试
test_directory_permissions() {
    start_test_group "目录权限测试"

    # 检查必要目录是否可写
    local required_dirs=(
        ".claude/config"
        ".claude/state/e2e-detection"
        ".claude/artifacts/e2e-detection"
    )

    for dir in "${required_dirs[@]}"; do
        local dir_path="$PROJECT_ROOT/$dir"
        if [ -d "$dir_path" ]; then
            run_test "目录可写: $dir" "[ -w '$dir_path' ]"
        else
            print_warning "目录不存在: $dir"
        fi
    done

    end_test_group "目录权限测试"
}

# 集成测试
test_integration() {
    start_test_group "集成测试"

    # 测试1: 配置更新脚本
    print_info "测试配置更新功能..."

    local test_config="/tmp/e2e-test-config.json"
    cat > "$test_config" << 'EOF'
{
  "enabled": false,
  "mode": "lenient"
}
EOF

    node "$PROJECT_ROOT/scripts/e2e-detection/update-config.js" \
        --config "$test_config" \
        --enabled true \
        --mode strict > /dev/null 2>&1

    if [ $? -eq 0 ]; then
        # 检查更新后的配置
        local updated_config=$(cat "$test_config")
        if [[ "$updated_config" == *"\"enabled\": true"* ]] && [[ "$updated_config" == *"\"mode\": \"strict\""* ]]; then
            print_success "配置更新功能正常"
            TESTS_PASSED=$((TESTS_PASSED + 1))
        else
            print_error "配置更新结果不符合预期"
            TESTS_FAILED=$((TESTS_FAILED + 1))
        fi
        TESTS_TOTAL=$((TESTS_TOTAL + 1))
    else
        print_error "配置更新脚本执行失败"
        TESTS_FAILED=$((TESTS_FAILED + 1))
        TESTS_TOTAL=$((TESTS_TOTAL + 1))
    fi

    rm -f "$test_config"

    # 测试2: 报告生成（使用模拟数据）
    print_info "测试报告生成功能..."

    local mock_result="/tmp/e2e-mock-result.json"
    cat > "$mock_result" << 'EOF'
{
  "detection_id": "mock-test-123",
  "timestamp": "2024-01-01T12:00:00Z",
  "status": "FAIL",
  "test_results": {
    "frontend": {
      "status": "PASS",
      "execution_time": 1.2,
      "critical_issues": []
    },
    "backend": {
      "status": "FAIL",
      "execution_time": 2.3,
      "critical_issues": [
        {
          "severity": "critical",
          "description": "测试失败示例",
          "file": "test.js",
          "line": 42,
          "suggestion": "修复测试用例"
        }
      ]
    }
  },
  "critical_issues": [
    {
      "severity": "critical",
      "description": "后端测试失败",
      "file": "test.js",
      "line": 42,
      "suggestion": "修复测试用例",
      "test_type": "backend"
    }
  ],
  "execution_time": 3.5
}
EOF

    if node "$PROJECT_ROOT/scripts/e2e-detection/e2e-report-generator.js" "$mock_result" console > /tmp/e2e-test-output.txt 2>&1; then
        if [ -s "/tmp/e2e-test-output.txt" ]; then
            print_success "报告生成功能正常"
            TESTS_PASSED=$((TESTS_PASSED + 1))
        else
            print_error "报告生成输出为空"
            TESTS_FAILED=$((TESTS_FAILED + 1))
        fi
    else
        print_error "报告生成执行失败"
        TESTS_FAILED=$((TESTS_FAILED + 1))
    fi
    TESTS_TOTAL=$((TESTS_TOTAL + 1))

    # 清理临时文件
    rm -f "$mock_result" "/tmp/e2e-test-output.txt"

    end_test_group "集成测试"
}

# 文档测试
test_documentation() {
    start_test_group "文档测试"

    # 检查关键文档是否存在
    local required_docs=(
        "docs/architecture/e2e-detection-architecture.md"
        "docs/analysis/e2e-detection-analysis-report.md"
    )

    for doc in "${required_docs[@]}"; do
        if [ -f "$PROJECT_ROOT/$doc" ]; then
            run_test "文档存在: $doc" "[ -f '$PROJECT_ROOT/$doc' ]"
        else
            print_warning "文档不存在: $doc"
        fi
    done

    end_test_group "文档测试"
}

# 主测试流程
main() {
    echo -e "${BLUE}"
    cat << "EOF"
🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥

   🔥 端到端检测系统集成测试

🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥

EOF
    echo -e "${NC}"

    # 切换到项目根目录
    cd "$PROJECT_ROOT" || exit 1

    # 记录开始时间
    local start_time=$(date +%s)

    # 运行测试组
    test_basic_environment
    test_script_files
    test_script_syntax
    test_config_files
    test_script_functionality
    test_directory_permissions
    test_integration
    test_documentation

    # 计算执行时间
    local end_time=$(date +%s)
    local duration=$((end_time - start_time))

    # 输出测试摘要
    echo ""
    echo -e "${BLUE}═══════════════════════════════════════════════════${NC}"
    echo -e "${BLUE}  测试摘要${NC}"
    echo -e "${BLUE}═══════════════════════════════════════════════════${NC}"
    echo ""
    echo -e "总测试数: ${BLUE}$TESTS_TOTAL${NC}"
    echo -e "通过: ${GREEN}$TESTS_PASSED${NC}"
    echo -e "失败: ${RED}$TESTS_FAILED${NC}"
    echo -e "执行时间: ${BLUE}${duration}秒${NC}"
    echo ""

    # 判断测试结果
    if [ $TESTS_FAILED -eq 0 ]; then
        echo -e "${GREEN}🎉 所有测试通过！端到端检测系统运行正常。${NC}"
        echo ""
        echo -e "${BLUE}下一步:${NC}"
        echo "  1. 运行设置脚本: bash scripts/e2e-detection/setup-e2e-detection.sh"
        echo "  2. 配置项目: 编辑 .claude/config/e2e-detection.config.json"
        echo "  3. 运行检测: node scripts/e2e-detection/e2e-detection-manager.js <worktree> <base_ref>"
        echo ""
        exit 0
    else
        echo -e "${RED}❌ 有 $TESTS_FAILED 个测试失败，请检查系统配置。${NC}"
        echo ""
        echo -e "${YELLOW}建议:${NC}"
        echo "  1. 检查 Node.js 和 npm 是否正确安装"
        echo "  2. 运行设置脚本: bash scripts/e2e-detection/setup-e2e-detection.sh"
        echo "  3. 查看错误信息并修复问题"
        echo ""
        exit 1
    fi
}

# 执行主函数
main "$@"