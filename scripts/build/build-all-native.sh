#!/bin/bash
# build-all-native.sh
# 多平台Native Image构建脚本
# 检测当前平台并编译对应的二进制文件，输出格式：harness-{os}-{arch}

set -e  # 遇到错误立即退出

# 颜色定义
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${BLUE}==================================="
echo -e "Building Java Harness Native Binary"
echo -e "===================================${NC}"

# 获取脚本所在目录的父目录（项目根目录）
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

# 平台检测函数
detect_platform() {
    local os
    local arch

    os=$(uname -s | tr '[:upper:]' '[:lower:]')
    arch=$(uname -m)

    # 标准化操作系统名称
    case "$os" in
        darwin)
            os="darwin"
            ;;
        linux)
            os="linux"
            ;;
        mingw*|msys*|cygwin*)
            os="windows"
            ;;
        *)
            os="$os"
            ;;
    esac

    # 标准化架构名称
    case "$arch" in
        x86_64|amd64)
            arch="amd64"
            ;;
        aarch64|arm64)
            arch="arm64"
            ;;
        i386|i686)
            arch="386"
            ;;
        *)
            arch="$arch"
            ;;
    esac

    echo "${os}-${arch}"
}

# 检测当前平台
PLATFORM=$(detect_platform)
OS_PART="${PLATFORM%-*}"
ARCH_PART="${PLATFORM#*-}"

# 设置可执行文件扩展名
EXT=""
if [ "$OS_PART" = "windows" ]; then
    EXT=".exe"
fi

# 目标二进制文件名
BINARY_NAME="harness-${OS_PART}-${ARCH_PART}${EXT}"

echo -e "${YELLOW}📍 检测到的平台: ${PLATFORM}${NC}"
echo -e "${YELLOW}📦 目标二进制文件名: ${BINARY_NAME}${NC}"
echo ""

# 检查Maven是否安装
if ! command -v mvn &> /dev/null; then
    echo -e "${RED}❌ Maven未安装，请先安装Maven${NC}"
    exit 1
fi

echo -e "${GREEN}✅ Maven已安装${NC}"
echo ""

# 进入项目根目录
cd "$PROJECT_ROOT"

echo -e "${BLUE}🔨 开始Native Image编译...${NC}"
echo ""

# 执行Maven Native Image构建
mvn -Pnative \
    -DskipTests \
    -Dnative.os="${OS_PART}" \
    -Dnative.arch="${ARCH_PART}" \
    -Dnative.image.name="${BINARY_NAME}" \
    native:compile

echo ""
echo -e "${GREEN}✅ 编译完成${NC}"
echo ""

# 源二进制文件路径（Maven输出目录）
SOURCE_BINARY="java-harness-cli/target/${BINARY_NAME}"

# 检查编译产物是否存在
if [ ! -f "$SOURCE_BINARY" ]; then
    echo -e "${RED}❌ 编译产物未找到: ${SOURCE_BINARY}${NC}"
    exit 1
fi

# 创建bin目录
mkdir -p bin

# 复制二进制文件到bin目录
echo -e "${BLUE}📦 复制二进制文件到 bin/ 目录...${NC}"
cp "$SOURCE_BINARY" "bin/${BINARY_NAME}"

# 设置执行权限（仅对非Windows系统）
if [ "$OS_PART" != "windows" ]; then
    chmod +x "bin/${BINARY_NAME}"
fi

echo -e "${GREEN}✅ 二进制文件已创建: bin/${BINARY_NAME}${NC}"
echo ""

# 显示文件信息
if [ "$OS_PART" != "windows" ]; then
    FILE_SIZE=$(ls -lh "bin/${BINARY_NAME}" | awk '{print $5}')
    echo -e "${BLUE}📊 文件大小: ${FILE_SIZE}${NC}"
fi

echo ""
echo -e "${GREEN}╔════════════════════════════════════════════════════════════╗${NC}"
echo -e "${GREEN}║           Native Image 构建成功！                          ║${NC}"
echo -e "${GREEN}╚════════════════════════════════════════════════════════════╝${NC}"
echo ""
echo -e "${BLUE}🚀 二进制文件位置: bin/${BINARY_NAME}${NC}"
echo -e "${BLUE}🧪 测试运行: ./bin/harness --version${NC}"
echo ""

# 测试运行（如果当前平台支持）
if [ "$OS_PART" != "windows" ]; then
    echo -e "${YELLOW}🧪 测试二进制文件...${NC}"
    if "./bin/${BINARY_NAME}" --version &> /dev/null; then
        VERSION=$("./bin/${BINARY_NAME}" --version)
        echo -e "${GREEN}✅ 测试成功！版本: ${VERSION}${NC}"
    else
        echo -e "${YELLOW}⚠️  测试失败，但文件已创建${NC}"
    fi
fi

echo ""
echo -e "${BLUE}📚 下一步: 运行 './bin/harness --help' 查看所有可用命令${NC}"
echo ""
