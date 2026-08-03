#!/bin/bash
# Java Harness - 一键安装脚本
# 适用于 Linux/macOS/Git Bash

set -e

echo "🚀 开始安装 Java Harness 插件..."

# 颜色定义
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# 插件信息
REPO_URL="https://gitee.com/duxvfeng/java-harness.git"
PLUGIN_DIR="$HOME/.claude/plugins/marketplaces/java-harness-marketplace"
TEMP_DIR="/tmp/java-harness-install"

echo -e "${BLUE}📍 插件将安装到：${PLUGIN_DIR}${NC}"
echo -e "${BLUE}📦 从以下地址下载：${REPO_URL}${NC}"

# 检查 Java 版本
echo -e "${YELLOW}🔍 检查 Java 环境...${NC}"
if ! command -v java &> /dev/null; then
    echo -e "${RED}❌ 未找到 Java，请先安装 Java 17 或更高版本${NC}"
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | awk -F '.' '{print $1"."$2}')
echo -e "${GREEN}✅ 发现 Java 版本：${JAVA_VERSION}${NC}"

# 检查必要的命令
echo -e "${YELLOW}🔍 检查必要工具...${NC}"
if ! command -v git &> /dev/null; then
    echo -e "${RED}❌ 未找到 git，请先安装 git${NC}"
    exit 1
fi

# 创建临时目录
echo -e "${YELLOW}📂 创建临时目录...${NC}"
rm -rf "$TEMP_DIR"
mkdir -p "$TEMP_DIR"

# 克隆仓库
echo -e "${YELLOW}📥 克隆仓库...${NC}"
git clone --depth 1 "$REPO_URL" "$TEMP_DIR" || {
    echo -e "${RED}❌ 克隆失败，请检查网络连接或仓库地址${NC}"
    exit 1
}

# 复制到插件目录
echo -e "${YELLOW}📦 安装插件...${NC}"
rm -rf "$PLUGIN_DIR"
mkdir -p "$PLUGIN_DIR"
cp -r "$TEMP_DIR"/.claude-plugin "$PLUGIN_DIR/"
cp -r "$TEMP_DIR"/bin "$PLUGIN_DIR/"
cp "$TEMP_DIR"/VERSION "$PLUGIN_DIR/" 2>/dev/null || true
cp "$TEMP_DIR"/README.md "$PLUGIN_DIR/" 2>/dev/null || true

# 设置执行权限
echo -e "${YELLOW}🔧 设置执行权限...${NC}"
chmod +x "$PLUGIN_DIR/bin/harness" 2>/dev/null || true
chmod +x "$PLUGIN_DIR/bin/harness.bat" 2>/dev/null || true

# 清理临时目录
echo -e "${YELLOW}🧹 清理临时文件...${NC}"
rm -rf "$TEMP_DIR"

# 验证安装
echo -e "${YELLOW}✅ 验证安装...${NC}"
if [ -f "$PLUGIN_DIR/bin/harness.jar" ]; then
    echo -e "${GREEN}✅ 插件文件安装成功！${NC}"
else
    echo -e "${RED}❌ 插件文件缺失，安装可能失败${NC}"
    exit 1
fi

# 测试启动
echo -e "${YELLOW}🧪 测试插件启动...${NC}"
if "$PLUGIN_DIR/bin/harness" --version &> /dev/null; then
    VERSION=$("$PLUGIN_DIR/bin/harness" --version)
    echo -e "${GREEN}✅ 插件启动成功！版本：${VERSION}${NC}"
else
    echo -e "${YELLOW}⚠️  插件启动测试失败，但文件已安装${NC}"
fi

# 完成
echo ""
echo -e "${GREEN}╔════════════════════════════════════════════════════════════╗${NC}"
echo -e "${GREEN}║           ✅ Java Harness 插件安装成功！                      ║${NC}"
echo -e "${GREEN}╚════════════════════════════════════════════════════════════╝${NC}"
echo ""
echo -e "${BLUE}📦 安装位置：${PLUGIN_DIR}${NC}"
echo -e "${BLUE}🚀 使用方式：${NC}"
echo -e "   直接调用："
echo -e "   ${GREEN}$PLUGIN_DIR/bin/harness --version${NC}"
echo -e "   ${GREEN}$PLUGIN_DIR/bin/harness --help${NC}"
echo ""
echo -e "${BLUE}📚 下一步：${NC}"
echo -e "   1. 在 Claude Code 中运行：${GREEN}/reload-plugins${NC}"
echo -e "   2. 查看插件列表：${GREEN}/plugin list${NC}"
echo -e "   3. 查看完整文档：${GREEN}$PLUGIN_DIR/README.md${NC}"
echo ""
echo -e "${YELLOW}💡 提示：如果遇到问题，请检查：${NC}"
echo -e "   - Java 版本是否 ≥ 17"
echo -e "   - 网络连接是否正常"
echo -e "   - 插件目录权限是否正确"
echo ""
