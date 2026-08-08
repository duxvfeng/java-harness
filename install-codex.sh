#!/bin/bash
# ============================================
# Java Harness - Codex CLI 安装脚本
# ============================================

set -e  # 遇到错误立即退出

# 颜色定义
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${BLUE}============================================${NC}"
echo -e "${BLUE}Java Harness - Codex CLI 安装向导${NC}"
echo -e "${BLUE}============================================${NC}"
echo ""

# 检查是否在正确的目录
if [ ! -f ".codex-plugin/plugin.json" ] && [ ! -f "plugin.json" ]; then
    echo -e "${RED}错误: 未找到 .codex-plugin/plugin.json 文件${NC}"
    echo "请在包含 Java Harness 的目录中运行此脚本"
    exit 1
fi

# 检查 Codex 环境
echo -e "${YELLOW}🔍 检查 Codex CLI 环境...${NC}"
if command -v codex &> /dev/null; then
    echo -e "${GREEN}✅ Codex CLI 检测成功${NC}"
    CODEX_VERSION=$(codex --version 2>/dev/null || echo "unknown")
    echo "版本: $CODEX_VERSION"
else
    echo -e "${YELLOW}⚠️  未检测到 Codex CLI${NC}"
    echo "请确保已安装 Codex CLI 或使用手动安装方式"
fi

echo ""

# 创建 Codex 插件目录结构
echo -e "${YELLOW}📁 创建 Codex 插件目录结构...${NC}"
mkdir -p .codex-plugin/skills
mkdir -p .codex-plugin/assets
echo -e "${GREEN}✅ Codex 插件目录创建完成${NC}"

# 创建 Codex 配置目录
echo -e "${YELLOW}📁 创建 Codex 配置目录...${NC}"
mkdir -p .codex
echo -e "${GREEN}✅ Codex 配置目录创建完成${NC}"

echo ""

# 创建 Codex 配置文件
echo -e "${YELLOW}📋 配置 Codex 设置...${NC}"

if [ ! -f ".codex/config.toml" ]; then
    cat > .codex/config.toml << 'EOF'
# Java Harness Codex 配置
[harness]
backend = "codex"
platform = "codex"
EOF
    echo -e "${GREEN}✅ Codex 配置文件创建完成${NC}"
else
    echo -e "${YELLOW}⚠️  Codex 配置文件已存在${NC}"
fi

# 如果存在 harness.toml.bak，提示用户
if [ -f "harness.toml" ]; then
    echo -e "${GREEN}✅ 检测到 harness.toml 跨平台配置文件${NC}"
    echo "高级配置已就绪"
fi

echo ""

# 验证安装
echo -e "${YELLOW}🔍 验证安装...${NC}"

if [ -f ".codex-plugin/plugin.json" ]; then
    echo -e "${GREEN}✅ .codex-plugin/plugin.json 存在${NC}"
else
    echo -e "${RED}❌ .codex-plugin/plugin.json 缺失${NC}"
    exit 1
fi

if [ -f ".codex/config.toml" ]; then
    echo -e "${GREEN}✅ .codex/config.toml 存在${NC}"
else
    echo -e "${RED}❌ .codex/config.toml 缺失${NC}"
    exit 1
fi

# 检查 skills-codex 目录
if [ -d "skills-codex" ]; then
    echo -e "${GREEN}✅ skills-codex/ 目录存在${NC}"
    SKILL_COUNT=$(find skills-codex -name "SKILL.md" | wc -l)
    echo "包含 $SKILL_COUNT 个技能"
else
    echo -e "${YELLOW}⚠️  skills-codex/ 目录不存在（可选）${NC}"
fi

echo ""

# 设置权限
echo -e "${YELLOW}🔐 设置文件权限...${NC}"
chmod -R u+r .codex-plugin/
chmod -R u+r .codex/
chmod +x install-codex.sh 2>/dev/null || true
echo -e "${GREEN}✅ 权限设置完成${NC}"

echo ""
echo -e "${GREEN}============================================${NC}"
echo -e "${GREEN}🎉 Java Harness for Codex 安装完成！${NC}"
echo -e "${GREEN}============================================${NC}"
echo ""
echo -e "${BLUE}下一步：${NC}"
echo -e "1. 复制插件到 Codex 插件目录:"
echo -e "   ${YELLOW}cp -r . /Users/\$USER/.codex/plugins/cache/java-harness${NC}"
echo -e "2. 或使用 Codex CLI 安装:"
echo -e "   ${YELLOW}codex plugin install .${NC}"
echo -e "3. 验证安装: ${YELLOW}harness-version${NC}"
echo -e "4. 开始使用: ${YELLOW}harness-plan${NC}"
echo ""
echo -e "${BLUE}手动安装方式：${NC}"
echo -e "cd ~/.codex/plugins/cache"
echo -e "git clone https://gitee.com/duxvfeng/java-harness.git java-harness"
echo -e "cd java-harness"
echo -e "./install-codex.sh"
echo ""
echo -e "${BLUE}文档和帮助：${NC}"
echo -e "- 项目主页: https://gitee.com/duxvfeng/java-harness"
echo -e "- 双平台指南: 查看 README-DUAL-PLATFORM.md"
echo ""