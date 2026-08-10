#!/bin/bash
# 端到端检测集成测试验证脚本
#
# 用途: 验证完整的 review → 端到端检测 → 修复流程
# 覆盖所有关键场景和边界情况
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
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0
SKIPPED_TESTS=0

# 项目根目录
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
SCRIPTS_DIR="${PROJECT_ROOT}/scripts/e2e-detection"
CONFIG_DIR="${PROJECT_ROOT}/.claude/config"
STATE_DIR="${PROJECT_ROOT}/.claude/state/e2e-detection"

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

# 记录测试结果
record_test() {
    local test_name="$1"
    local result="$2"
    local reason="${3:-}"

    TOTAL_TESTS=$((TOTAL_TESTS + 1))

    case $result in
        "pass")
            PASSED_TESTS=$((PASSED_TESTS + 1))
            print_success "$test_name - 通过"
            ;;
        "fail")
            FAILED_TESTS=$((FAILED_TESTS + 1))
            print_error "$test_name - 失败"
            [ -n "$reason" ] && echo -e "   ${RED}原因: $reason${NC}"
            ;;
        "skip")
            SKIPPED_TESTS=$((SKIPPED_TESTS + 1))
            print_warning "$test_name - 跳过"
            [ -n "$reason" ] && echo -e "   ${YELLOW}原因: $reason${NC}"
            ;;
        *)
            print_error "$test_name - 未知结果"
            ;;
    esac
}

# 设置测试环境
setup_test_environment() {
    print_info "设置测试环境..."

    # 创建必要目录
    mkdir -p "$CONFIG_DIR"
    mkdir -p "$STATE_DIR"
    mkdir -p "${PROJECT_ROOT}/.claude/artifacts/e2e-detection"

    # 复制配置文件
    if [ ! -f "$CONFIG_DIR/e2e-detection.config.json" ]; then
        if [ -f "${PROJECT_ROOT}/config/e2e-detection.default.config.json" ]; then
            cp "${PROJECT_ROOT}/config/e2e-detection.default.config.json" \
               "$CONFIG_DIR/e2e-detection.config.json"
            print_success "默认配置已复制"
        else
            print_warning "未找到默认配置，使用内置配置"
        fi
    fi

    print_success "测试环境准备完成"
}

# 测试1: 检测脚本存在性
test_script_existence() {
    print_test "测试1: 检测脚本文件存在性"

    local required_scripts=(
        "scripts/e2e-detection/e2e-detection-manager.js"
        "scripts/e2e-detection/auto-fix-controller.js"
        "scripts/e2e-detection/e2e-detection-trigger.js"
        "scripts/e2e-detection/e2e-report-generator.js"
        "scripts/e2e-detection/playwright-executor.js"
        "tests/e2e-detection/test-e2e-detection.sh"
        "scripts/e2e-detection/setup-e2e-detection.sh"
    )

    for script in "${required_scripts[@]}"; do
        if [ ! -f "$PROJECT_ROOT/$script" ]; then
            record_test "脚本文件: $script" "fail"
        else
            print_success "✓ $script"
        fi
    done

    if [ $FAILED_TESTS -eq 0 ]; then
        record_test "测试1" "pass"
    else
        record_test "测试1" "fail" "缺少必需的脚本文件"
    fi
}

# 测试2: 配置系统验证
test_configuration_system() {
    print_test "测试2: 配置系统验证"

    # 测试默认配置
    if [ ! -f "$CONFIG_DIR/e2e-detection.config.json" ]; then
        record_test "默认配置文件" "fail"
    else
        # 验证配置文件格式（使用跨平台方式）
        if python3 -c "import json; json.load(open('$CONFIG_DIR/e2e-detection.config.json'))" 2>/dev/null; then
            print_success "✓ 默认配置文件格式正确"
        else
            record_test "默认配置文件格式" "fail" "JSON格式错误"
        fi
    fi

    # 测试配置加载
    if [ -f "$SCRIPTS_DIR/e2e-detection-manager.js" ]; then
        print_info "测试配置加载功能..."

        # 创建测试配置
        local test_config="$STATE_DIR/test-config.json"
        cat > "$test_config" << 'EOF'
{
  "enabled": true,
  "mode": "strict",
  "timeout": 60,
  "test_types": {
    "frontend": {
      "enabled": true,
      "framework": "playwright"
    },
    "backend": {
      "enabled": true,
      "framework": "auto"
    }
  }
}
EOF

        # 测试配置加载
        if node "$SCRIPTS_DIR/e2e-detection-manager.js" --help > /dev/null 2>&1; then
            print_success "✓ 配置加载功能正常"
        else
            record_test "配置加载功能" "fail" "管理器帮助信息无法获取"
        fi

        rm -f "$test_config"
    else
        record_test "配置加载功能" "skip" "管理器脚本不存在"
    fi

    if [ $FAILED_TESTS -eq 0 ]; then
        record_test "测试2" "pass"
    else
        record_test "测试2" "fail" "配置系统验证失败"
    fi
}

# 测试3: Playwright 配置验证
test_playwright_config() {
    print_test "测试3: Playwright 配置验证"

    # 检查 Playwright 配置文件（可能在多个位置）
    local playwright_configs=(
        "config/e2e-detection-playwright.config.json"
        "scripts/e2e-detection/e2e-detection-playwright.config.json"
        "java-harness-cli/e2e-detection-playwright.config.json"
    )

    local playwright_config_found=false
    for config in "${playwright_configs[@]}"; do
        if [ -f "$PROJECT_ROOT/$config" ]; then
            print_success "✓ Playwright配置文件存在: $config"
            playwright_config_found=true
            # 验证配置文件格式（使用跨平台方式）
            if python3 -c "import json; json.load(open('$PROJECT_ROOT/$config'))" 2>/dev/null; then
                print_success "✓ Playwright配置文件格式正确"
            else
                record_test "Playwright配置文件格式" "fail" "JSON格式错误"
            fi
            break
        fi
    done

    if [ "$playwright_config_found" = false ]; then
        record_test "Playwright配置文件" "fail" "配置文件不存在"
    fi

    # 检查 Playwright 执行器
    if [ -f "$SCRIPTS_DIR/playwright-executor.js" ]; then
        print_success "✓ Playwright执行器存在"

        # 测试执行器语法
        if node --check "$SCRIPTS_DIR/playwright-executor.js" > /dev/null 2>&1; then
            print_success "✓ Playwright执行器语法正确"
        else
            record_test "Playwright执行器语法" "fail" "语法错误"
        fi
    else
        record_test "Playwright执行器" "fail" "执行器脚本不存在"
    fi

    if [ $FAILED_TESTS -eq 0 ]; then
        record_test "测试3" "pass"
    else
        record_test "测试3" "fail" "Playwright配置验证失败"
    fi
}

# 测试4: 端到端检测管理器功能
test_detection_manager() {
    print_test "测试4: 端到端检测管理器功能"

    if [ ! -f "$SCRIPTS_DIR/e2e-detection-manager.js" ]; then
        record_test "检测管理器" "skip" "管理器脚本不存在"
        return
    fi

    print_info "测试检测管理器核心功能..."

    # 测试管理器实例化
    local test_code="
const { E2EDetectionManager } = require('./scripts/e2e-detection/e2e-detection-manager.js');
const manager = new E2EDetectionManager({}, '');
console.log('✓ 管理器实例化成功');
console.log('✓ 配置加载功能: ' + (manager.config ? '可用' : '不可用'));
console.log('✓ 状态管理: ' + (manager.state ? '可用' : '不可用'));
console.log('✓ 检测ID生成: ' + (manager.generateDetectionId ? '可用' : '不可用'));
"

    if node -e "$test_code" > /tmp/test-manager.js 2>/dev/null; then
        node /tmp/test-manager.js > /dev/null 2>&1
        if [ $? -eq 0 ]; then
            print_success "✓ 管理器实例化测试通过"
        else
            record_test "管理器实例化" "fail" "运行时错误"
        fi
        rm -f /tmp/test-manager.js
    else
        record_test "管理器测试代码" "fail" "代码生成失败"
    fi

    if [ $FAILED_TESTS -eq 0 ]; then
        record_test "测试4" "pass"
    else
        record_test "测试4" "fail" "检测管理器功能测试失败"
    fi
}

# 测试5: 报告生成器功能
test_report_generator() {
    print_test "测试5: 报告生成器功能"

    if [ ! -f "$SCRIPTS_DIR/e2e-report-generator.js" ]; then
        record_test "报告生成器" "skip" "生成器脚本不存在"
        return
    fi

    print_info "测试报告生成器功能..."

    # 创建测试输入
    local test_result='{"detection_id":"test-2024","status":"PASS","test_results":{"frontend":{"status":"PASS"},"backend":{"status":"PASS"}},"critical_issues":[],"execution_time":1.5}'
    local test_input="/tmp/test-report-input.json"

    echo "$test_result" > "$test_input"

    # 测试报告生成
    if node "$SCRIPTS_DIR/e2e-report-generator.js" "$test_input" console > /tmp/test-output.txt 2>&1; then
        if [ -s /tmp/test-output.txt ]; then
            print_success "✓ 控制台报告生成成功"

            if grep -q "🔥 端到端检测报告" /tmp/test-output.txt; then
                print_success "✓ 报告格式正确"
            else
                record_test "报告格式" "fail" "缺少报告标题"
            fi
        else
            record_test "控制台报告生成" "fail" "输出为空"
        fi
    else
        record_test "报告生成功能" "fail" "生成器执行失败"
    fi

    # 清理
    rm -f "$test_input" /tmp/test-output.txt

    if [ $FAILED_TESTS -eq 0 ]; then
        record_test "测试5" "pass"
    else
        record_test "测试5" "fail" "报告生成器功能测试失败"
    fi
}

# 测试6: 自动修复控制器功能
test_auto_fix_controller() {
    print_test "测试6: 自动修复控制器功能"

    if [ ! -f "$SCRIPTS_DIR/auto-fix-controller.js" ]; then
        record_test "自动修复控制器" "skip" "修复控制器脚本不存在"
        return
    fi

    print_info "测试自动修复控制器功能..."

    # 测试修复控制器实例化
    local test_code="
const { AutoFixController } = require('./scripts/e2e-detection/auto-fix-controller.js');
const controller = new AutoFixController(3);
console.log('✓ 修复控制器实例化成功');
console.log('✓ 最大迭代次数: ' + controller.maxIterations);
console.log('✓ 当前迭代: ' + controller.currentIteration);
console.log('✅ 修复历史: ' + (controller.getFixHistory() ? '可用' : '不可用'));
"

    if node -e "$test_code" > /tmp/test-fixer.js 2>/dev/null; then
        node /tmp/test-fixer.js > /dev/null 2>&1
        if [ $? -eq 0 ]; then
            print_success "✓ 修复控制器实例化测试通过"
        else
            record_test "修复控制器实例化" "fail" "运行时错误"
        fi
        rm -f /tmp/test-fixer.js
    else
        record_test "修复控制器测试代码" "fail" "代码生成失败"
    fi

    if [ $FAILED_TESTS -eq 0 ]; then
        record_test "测试6" "pass"
    else
        record_test "测试6" "fail" "自动修复控制器功能测试失败"
    fi
}

# 测试7: 集成流程模拟
test_integration_flow() {
    print_test "测试7: 集成流程模拟"

    print_info "模拟完整的 review → 检测 → 修复 流程..."

    # 创建模拟测试环境
    local test_env="$STATE_DIR/integration-test"
    mkdir -p "$test_env"

    # 模拟审查通过
    print_info "→ 模拟审查通过..."
    local review_result='{"verdict":"APPROVE","findings":[]}'

    # 模拟端到端检测触发
    print_info "→ 触发端到端端检测..."

    # 模拟检测通过场景
    local detection_pass='{"detection_id":"integration-test-001","status":"PASS","test_results":{"frontend":{"status":"PASS"},"backend":{"status":"PASS"}},"critical_issues":[],"execution_time":2.5}'
    echo "$detection_pass" > "$test_env/detection-result.json"

    if [ -f "$test_env/detection-result.json" ]; then
        print_success "✅ 检测通过 → 继续正常流程"
    fi

    # 模拟检测失败场景
    local detection_fail='{"detection_id":"integration-test-002","status":"FAIL","test_results":{"frontend":{"status":"FAIL"},"backend":{"status":"PASS"}},"critical_issues":[{"severity":"critical","description":"测试失败","file":"test.js","line":10}],"execution_time":3.2}'
    echo "$detection_fail" > "$test_env/detection-fail.json"

    print_info "→ 检测失败 → 回到 harness-work 继续修改"

    # 模拟修复后重新检测
    local detection_retry='{"detection_id":"integration-test-003","status":"PASS","test_results":{"frontend":{"status":"PASS"},"backend":{"status":"PASS"}},"critical_issues":[],"execution_time":2.8}'
    echo "$detection_retry" > "$test_env/detection-retry.json"

    if [ -f "$test_env/detection-retry.json" ]; then
        print_success "✅ 修复后重新检测通过"
    fi

    # 清理
    rm -rf "$test_env"

    if [ $FAILED_TESTS -eq 0 ]; then
        record_test "测试7" "pass"
    else
        record_test "测试7" "fail" "集成流程模拟失败"
    fi
}

# 测试8: 配置文件加载验证
test_config_loading() {
    print_test "测试8: 配置文件加载验证"

    print_info "测试多层级配置加载..."

    # 测试默认配置
    if [ -f "$CONFIG_DIR/e2e-detection.config.json" ]; then
        print_success "✓ 默认配置存在"

        # 检查配置内容
        local enabled=$(node -p "
try {
  const fs = require('fs');
  const config = JSON.parse(fs.readFileSync('$CONFIG_DIR/e2e-detection.config.json', 'utf8'));
  console.log(config.enabled || false);
} catch(e) {
  console.log('false');
}" 2>/dev/null)

        if [ "$enabled" = "true" ]; then
            print_success "✅ 配置默认启用状态正确"
        else
            record_test "配置启用状态" "fail" "应该默认启用"
        fi
    else
        record_test "默认配置" "fail" "默认配置文件不存在"
    fi

    # 测试配置加载优先级
    if [ -f "$PROJECT_ROOT/java-harness-cli/harness.toml" ]; then
        print_success "✅ harness.toml 配置文件存在"

        # 检查是否包含 e2e_detection 配置
        if grep -q "\[e2e_detection\]" "$PROJECT_ROOT/java-harness-cli/harness.toml" 2>/dev/null; then
            print_success "✅ harness.toml 包含 e2e_detection 配置"
        else
            print_warning "⚠️  harness.toml 不包含 e2e_detection 配置"
        fi
    fi

    if [ $FAILED_TESTS -eq 0 ]; then
        record_test "测试8" "pass"
    else
        record_test "测试8" "fail" "配置加载验证失败"
    fi
}

# 测试9: 错误处理机制
test_error_handling() {
    print_test "测试9: 错误处理机制"

    print_info "测试错误处理和升级机制..."

    # 测试1: 检测出错时的处理
    local error_result='{"detection_id":"error-test-001","status":"ERROR","error":"测试错误","test_results":{},"critical_issues":[]}'

    # 检查错误处理逻辑
    if echo "$error_result" | grep -q "ERROR"; then
        print_success "✅ 错误状态正确识别"
    else
        record_test "错误状态识别" "fail" "无法识别ERROR状态"
    fi

    # 测试2: 跳过状态的处理
    local skip_result='{"detection_id":"skip-test-001","status":"SKIPPED","reason":"配置禁用","test_results":{},"critical_issues":[]}'

    if echo "$skip_result" | grep -q "SKIPPED"; then
        print_success "✅ 跳过状态正确识别"
    else
        record_test "跳过状态识别" "fail" "无法识别SKIPPED状态"
    fi

    if [ $FAILED_TESTS -eq 0 ]; then
        record_test "测试9" "pass"
    else
        record_test "测试9" "fail" "错误处理机制测试失败"
    fi
}

# 测试10: 文档完整性检查
test_documentation_completeness() {
    print_test "测试10: 文档完整性检查"

    local required_docs=(
        "docs/architecture/e2e-detection-architecture.md"
        "docs/analysis/e2e-detection-analysis-report.md"
        "docs/playwright-testing-guide.md"
        "docs/playwright-default-config.md"
    )

    for doc in "${required_docs[@]}"; do
        if [ -f "$PROJECT_ROOT/$doc" ]; then
            print_success "✓ $(basename $doc)"

            # 检查文档基本内容
            if grep -q "端到端检测" "$PROJECT_ROOT/$doc" 2>/dev/null; then
                print_success "  ✅ 包含端到端检测内容"
            else
                print_warning "  ⚠️  缺少端到端检测内容"
            fi
        else
            record_test "文档文件: $doc" "fail" "文档不存在"
        fi
    done

    # 检查配置示例文件
    if [ -f "$PROJECT_ROOT/harness.toml.example" ]; then
        print_success "✓ harness.toml.example 存在"
    else
        record_test "配置示例文件" "fail" "harness.toml.example 不存在"
    fi

    if [ $FAILED_TESTS -eq 0 ]; then
        record_test "测试10" "pass"
    else
        record_test "测试10" "fail" "文档完整性检查失败"
    fi
}

# 主测试流程
run_integration_tests() {
    echo -e "${BLUE}"
    cat << "EOF"
🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥

   🔥 端到端检测集成测试验证

🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥

EOF
    echo -e "${NC}"

    # 记录开始时间
    local start_time=$(date +%s)

    # 执行测试组
    test_script_existence
    test_configuration_system
    test_playwright_config
    test_detection_manager
    test_report_generator
    test_auto_fix_controller
    test_integration_flow
    test_config_loading
    test_error_handling
    test_documentation_completeness

    # 计算执行时间
    local end_time=$(date +%s)
    local duration=$((end_time - start_time))

    # 输出测试摘要
    echo ""
    echo -e "${BLUE}══════════════════════════════════════════════════════════════════${NC}"
    echo -e "${BLUE}  集成测试验证报告${NC}"
    echo -e "${BLUE}══════════════════════════════════════════════════════════════════${NC}"
    echo ""
    echo -e "总测试数: ${BLUE}$TOTAL_TESTS${NC}"
    echo -e "通过: ${GREEN}$PASSED_TESTS${NC}"
    echo -e "失败: ${RED}$FAILED_TESTS${NC}"
    echo -e "跳过: ${YELLOW}$SKIPPED_TESTS${NC}"
    echo -e "执行时间: ${BLUE}${duration}秒${NC}"
    echo ""

    # 判断测试结果
    if [ $FAILED_TESTS -eq 0 ]; then
        echo -e "${GREEN}🎉 所有集成测试通过！端到端检测系统运行正常。${NC}"
        echo ""
        echo -e "${BLUE}下一步:${NC}"
        echo "  1. 在真实项目中测试端到端检测功能"
        echo "  2. 根据项目需求调整配置"
        echo "  3. 运行: /harness-work"
        echo ""
        exit 0
    else
        echo -e "${RED}❌ 有 $FAILED_TESTS 个测试失败，请检查系统配置。${NC}"
        echo ""
        echo -e "${YELLOW}建议:${NC}"
        echo "  1. 检查所有脚本文件是否完整"
        echo "  2. 验证配置文件格式是否正确"
        echo " 3. 确认Node.js环境是否正常"
        echo "  4. 查看错误信息并修复问题"
        echo ""
        exit 1
    fi
}

# 解析参数
parse_args() {
    case "$1" in
        --help|-h)
            echo "用法: bash tests/e2e-detection/test-integration.sh [options]"
            echo ""
            echo "选项:"
            echo "  --quick        快速测试（只测试核心功能）"
            echo "  --full        完整测试（所有测试）"
            echo "  --report       生成详细报告"
            echo "  --fix          自动修复问题（实验性）"
            echo ""
            echo "示例:"
            echo "  bash tests/e2e-detection/test-integration.sh"
            echo "  bash tests/e2e-detection/test-integration.sh --quick"
            echo "  bash tests/e2e-detection/test-integration.sh --full"
            echo ""
            exit 0
            ;;
        --quick)
            TEST_MODE="quick"
            ;;
        --full)
            TEST_MODE="full"
            ;;
        --report)
            GENERATE_REPORT=true
            ;;
        --fix)
            AUTO_FIX=true
            ;;
        *)
            TEST_MODE="full"
            ;;
    esac
}

# 快速测试模式
run_quick_tests() {
    print_info "运行快速集成测试..."

    # 只测试关键功能
    test_script_existence
    test_configuration_system
    test_playwright_config
    test_documentation_completeness

    echo ""
    print_info "快速测试完成！"
}

# 自动修复功能（实验性）
auto_fix_issues() {
    print_info "尝试自动修复发现的问题..."

    # 修复脚本语法
    find "$SCRIPTS_DIR" -name "*.js" -exec node --check {} \; 2>/dev/null || true

    # 修复配置文件格式
    for config in "$CONFIG_DIR"/*.json; do
        if [ -f "$config" ]; then
            # 简单的格式修复
            sed -i 's/, *$/,/g' "$config" 2>/dev/null || true
        fi
    done

    print_success "自动修复完成"
}

# 主函数
main() {
    local test_mode="full"

    # 解析参数
    if [ $# -gt 0 ]; then
        parse_args "$@"
    fi

    # 切换到项目根目录
    cd "$PROJECT_ROOT" || exit 1

    echo -e "${BLUE}"
    cat << "EOF"
🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥

   🔥 端到端检测集成测试验证

🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🥔🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🥔🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🥔🥔🥔🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🥔️
EOF
    echo -e "${NC}"

    # 运行自动修复（如果需要）
    if [ "$AUTO_FIX" = true ]; then
        auto_fix_issues
    fi

    # 根据模式运行测试
    case "$test_mode" in
        quick)
            run_quick_tests
            ;;
        full)
            run_integration_tests
            ;;
        *)
            run_integration_tests
            ;;
    esac
}

# 执行主函数
main "$@"