#!/bin/bash
# 端到端检测系统设置脚本
#
# 用途: 初始化和配置端到端检测系统
# 使用: bash scripts/e2e-detection/setup-e2e-detection.sh [options]
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

# 脚本目录
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

# 配置文件路径
CONFIG_DIR=".claude/config"
STATE_DIR=".claude/state/e2e-detection"
ARTIFACTS_DIR=".claude/artifacts/e2e-detection"

# 默认选项
ENABLE_E2E=true
MODE="strict"
TIMEOUT=120
MAX_RETRIES=3
AUTO_FIX=true

# 显示帮助信息
show_help() {
    cat << EOF
🔥 端到端检测系统设置脚本

用法: bash setup-e2e-detection.sh [options]

选项:
  --enable              启用端到端检测 (默认)
  --disable             禁用端到端检测
  --mode MODE           设置模式 (strict|lenient, 默认: strict)
  --timeout SECONDS     设置超时时间 (默认: 120)
  --max-retries NUM     设置最大重试次数 (默认: 3)
  --auto-fix            启用自动修复 (默认)
  --no-auto-fix         禁用自动修复
  --help                显示此帮助信息

示例:
  bash setup-e2e-detection.sh --enable --mode strict
  bash setup-e2e-detection.sh --timeout 180 --max-retries 5
  bash setup-e2e-detection.sh --disable

EOF
}

# 打印信息
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

# 创建必要的目录
create_directories() {
    print_info "创建必要的目录..."

    mkdir -p "$CONFIG_DIR"
    mkdir -p "$STATE_DIR"
    mkdir -p "$ARTIFACTS_DIR"

    print_success "目录创建完成"
}

# 复制默认配置
setup_default_config() {
    print_info "设置默认配置..."

    local config_file="$PROJECT_ROOT/$CONFIG_DIR/e2e-detection.config.json"
    local default_config="$PROJECT_ROOT/config/e2e-detection.default.config.json"

    if [ ! -f "$config_file" ]; then
        if [ -f "$default_config" ]; then
            cp "$default_config" "$config_file"
            print_success "默认配置已复制"
        else
            print_warning "默认配置文件不存在，创建基础配置..."
            create_basic_config "$config_file"
        fi
    else
        print_info "配置文件已存在，将更新设置"
    fi

    # 更新配置
    update_config "$config_file"
}

# 创建基础配置
create_basic_config() {
    local config_file="$1"

    cat > "$config_file" << EOF
{
  "enabled": $ENABLE_E2E,
  "mode": "$MODE",
  "timeout": $TIMEOUT,
  "retry_on_failure": true,
  "max_retries": $MAX_RETRIES,
  "auto_fix": {
    "enabled": $AUTO_FIX,
    "max_iterations": $MAX_RETRIES
  },
  "test_types": {
    "frontend": {
      "enabled": true
    },
    "backend": {
      "enabled": true
    },
    "integration": {
      "enabled": true
    },
    "performance": {
      "enabled": false
    },
    "security": {
      "enabled": true
    }
  }
}
EOF

    print_success "基础配置已创建"
}

# 更新配置
update_config() {
    local config_file="$1"

    # 使用 node.js 脚本更新配置
    local update_script="$PROJECT_ROOT/scripts/e2e-detection/update-config.js"

    if [ -f "$update_script" ]; then
        node "$update_script" \
            --config "$config_file" \
            --enabled "$ENABLE_E2E" \
            --mode "$MODE" \
            --timeout "$TIMEOUT" \
            --max-retries "$MAX_RETRIES" \
            --auto-fix "$AUTO_FIX" || true

        print_success "配置已更新"
    else
        print_warning "配置更新脚本不存在，跳过"
    fi
}

# 设置脚本权限
setup_permissions() {
    print_info "设置脚本权限..."

    # 为所有 .sh 文件设置执行权限
    find "$PROJECT_ROOT/scripts/e2e-detection" -name "*.sh" -type f -exec chmod +x {} \;

    # 为所有 .js 文件设置读取权限
    find "$PROJECT_ROOT/scripts/e2e-detection" -name "*.js" -type f -exec chmod +r {} \;

    print_success "权限设置完成"
}

# 验证安装
verify_installation() {
    print_info "验证安装..."

    local all_good=true

    # 检查目录
    if [ ! -d "$CONFIG_DIR" ]; then
        print_error "配置目录不存在: $CONFIG_DIR"
        all_good=false
    fi

    if [ ! -d "$STATE_DIR" ]; then
        print_error "状态目录不存在: $STATE_DIR"
        all_good=false
    fi

    if [ ! -d "$ARTIFACTS_DIR" ]; then
        print_error "工件目录不存在: $ARTIFACTS_DIR"
        all_good=false
    fi

    # 检查脚本
    local required_scripts=(
        "e2e-detection-manager.js"
        "auto-fix-controller.js"
        "e2e-detection-trigger.js"
        "e2e-report-generator.js"
    )

    for script in "${required_scripts[@]}"; do
        if [ ! -f "$PROJECT_ROOT/scripts/e2e-detection/$script" ]; then
            print_error "必需脚本不存在: $script"
            all_good=false
        fi
    done

    # 检查配置
    if [ ! -f "$CONFIG_DIR/e2e-detection.config.json" ]; then
        print_error "配置文件不存在"
        all_good=false
    fi

    if [ "$all_good" = true ]; then
        print_success "安装验证通过"
        return 0
    else
        print_error "安装验证失败"
        return 1
    fi
}

# 显示配置摘要
show_config_summary() {
    local config_file="$PROJECT_ROOT/$CONFIG_DIR/e2e-detection.config.json"

    if [ -f "$config_file" ]; then
        print_info "当前配置摘要:"
        echo ""

        if command -v jq &> /dev/null; then
            jq -r '
                "🔥 端到端检测配置:\n" +
                "  启用状态: " + (.enabled | tostring) + "\n" +
                "  运行模式: " + .mode + "\n" +
                "  超时时间: " + (.timeout | tostring) + "秒\n" +
                "  最大重试: " + (.max_retries | tostring) + "次\n" +
                "  自动修复: " + (.auto_fix.enabled | tostring) + "\n" +
                "\n启用的测试类型:" +
                (.test_types | to_entries[] | select(.value.enabled) | "  - " + .key)
            ' "$config_file" 2>/dev/null || print_warning "jq 不可用，无法解析配置"
        else
            print_warning "jq 不可用，显示原始JSON"
            cat "$config_file"
        fi
    else
        print_warning "配置文件不存在"
    fi
}

# 运行测试
run_tests() {
    print_info "运行系统测试..."

    local test_script="$PROJECT_ROOT/tests/e2e-detection/test-e2e-detection.sh"

    if [ -f "$test_script" ]; then
        bash "$test_script"
    else
        print_warning "测试脚本不存在: $test_script"
        print_info "创建基本测试..."
        create_basic_test
    fi
}

# 创建基本测试
create_basic_test() {
    local test_dir="$PROJECT_ROOT/tests/e2e-detection"
    local test_file="$test_dir/test-e2e-detection.sh"

    mkdir -p "$test_dir"

    cat > "$test_file" << 'EOF'
#!/bin/bash
# 端到端检测系统基本测试

set -e

echo "🧪 运行端到端检测系统测试..."

# 测试1: 检查管理器脚本
echo "测试1: 检查管理器脚本..."
if [ -f "scripts/e2e-detection/e2e-detection-manager.js" ]; then
    echo "✅ 管理器脚本存在"
else
    echo "❌ 管理器脚本不存在"
    exit 1
fi

# 测试2: 检查配置文件
echo "测试2: 检查配置文件..."
if [ -f ".claude/config/e2e-detection.config.json" ]; then
    echo "✅ 配置文件存在"
else
    echo "❌ 配置文件不存在"
    exit 1
fi

# 测试3: 检查Node.js
echo "测试3: 检查Node.js..."
if command -v node &> /dev/null; then
    echo "✅ Node.js 可用: $(node --version)"
else
    echo "❌ Node.js 不可用"
    exit 1
fi

# 测试4: 检查脚本语法
echo "测试4: 检查脚本语法..."
for script in scripts/e2e-detection/*.js; do
    if node --check "$script" 2>/dev/null; then
        echo "✅ $(basename $script) 语法正确"
    else
        echo "❌ $(basename $script) 语法错误"
        exit 1
    fi
done

echo ""
echo "🎉 所有测试通过!"

EOF

    chmod +x "$test_file"
    bash "$test_file"
}

# 主函数
main() {
    echo -e "${BLUE}"
    cat << "EOF"
🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥

   🔥 端到端检测系统设置

🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥

EOF
    echo -e "${NC}"

    # 解析参数
    while [[ $# -gt 0 ]]; do
        case $1 in
            --enable)
                ENABLE_E2E=true
                shift
                ;;
            --disable)
                ENABLE_E2E=false
                shift
                ;;
            --mode)
                MODE="$2"
                shift 2
                ;;
            --timeout)
                TIMEOUT="$2"
                shift 2
                ;;
            --max-retries)
                MAX_RETRIES="$2"
                shift 2
                ;;
            --auto-fix)
                AUTO_FIX=true
                shift
                ;;
            --no-auto-fix)
                AUTO_FIX=false
                shift
                ;;
            --help)
                show_help
                exit 0
                ;;
            *)
                print_error "未知选项: $1"
                show_help
                exit 1
                ;;
        esac
    done

    # 切换到项目根目录
    cd "$PROJECT_ROOT" || exit 1

    # 执行设置步骤
    create_directories
    setup_default_config
    setup_permissions

    echo ""
    print_success "端到端检测系统设置完成!"
    echo ""

    # 验证安装
    if verify_installation; then
        echo ""

        # 显示配置摘要
        show_config_summary
        echo ""

        # 可选: 运行测试
        if [ "$AUTO_FIX" = true ]; then
            print_info "是否要运行系统测试? (y/N)"
            read -r response
            if [[ "$response" =~ ^([yY][eE][sS]|[yY])$ ]]; then
                run_tests
            fi
        fi

        echo ""
        print_success "设置完成! 端到端检测系统已就绪。"
        echo ""
        print_info "下一步:"
        echo "  1. 检查配置: cat .claude/config/e2e-detection.config.json"
        echo "  2. 手动测试: node scripts/e2e-detection/e2e-detection-manager.js --help"
        echo "  3. 查看文档: docs/architecture/e2e-detection-architecture.md"
        echo ""

    else
        print_error "设置验证失败，请检查错误信息"
        exit 1
    fi
}

# 执行主函数
main "$@"