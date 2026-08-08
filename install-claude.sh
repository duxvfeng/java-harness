#!/bin/bash
# ============================================
# Java Harness - Claude Code 安装脚本
# ============================================

set -e  # 遇到错误立即退出

# 颜色定义
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${BLUE}============================================${NC}"
echo -e "${BLUE}Java Harness - Claude Code 安装向导${NC}"
echo -e "${BLUE}============================================${NC}"
echo ""

# 检查是否在正确的目录
if [ ! -f "plugin.json" ] && [ ! -f ".claude-plugin/plugin.json" ]; then
    echo -e "${RED}错误: 未找到 plugin.json 文件${NC}"
    echo "请在包含 Java Harness 的目录中运行此脚本"
    exit 1
fi

# 检查 Claude Code 环境
echo -e "${YELLOW}🔍 检查 Claude Code 环境...${NC}"
if [ -d "$HOME/.claude" ] || [ ! -z "$CLAUDE_API_KEY" ]; then
    echo -e "${GREEN}✅ Claude Code 环境检测成功${NC}"
else
    echo -e "${YELLOW}⚠️  未检测到 Claude Code 环境${NC}"
    echo "请确保已安装 Claude Code"
fi

echo ""

# 创建插件目录结构
echo -e "${YELLOW}📁 创建插件目录结构...${NC}"
mkdir -p .claude-plugin/skills
mkdir -p .claude-plugin/assets
mkdir -p .claude-plugin/workflows
echo -e "${GREEN}✅ 目录结构创建完成${NC}"

echo ""

# 复制配置文件
echo -e "${YELLOW}📋 配置插件设置...${NC}"

# 如果存在 harness.toml.bak，提示用户
if [ -f "harness.toml" ]; then
    echo -e "${GREEN}✅ 检测到 harness.toml 配置文件${NC}"
    echo "跨平台配置已就绪"
fi

echo ""

# 验证安装
echo -e "${YELLOW}🔍 验证安装...${NC}"

if [ -f ".claude-plugin/plugin.json" ]; then
    echo -e "${GREEN}✅ plugin.json 存在${NC}"
else
    echo -e "${RED}❌ plugin.json 缺失${NC}"
    exit 1
fi

if [ -d ".claude-plugin/skills" ]; then
    echo -e "${GREEN}✅ skills/ 目录存在${NC}"
else
    echo -e "${RED}❌ skills/ 目录缺失${NC}"
    exit 1
fi

echo ""

# 设置权限
echo -e "${YELLOW}🔐 设置文件权限...${NC}"
chmod -R u+r .claude-plugin/
echo -e "${GREEN}✅ 权限设置完成${NC}"

echo ""
echo -e "${GREEN}============================================${NC}"
echo -e "${GREEN}🎉 Java Harness 安装完成！${NC}"
echo -e "${GREEN}============================================${NC}"
echo ""
echo -e "${BLUE}下一步：${NC}"
echo -e "1. 在 Claude Code 中运行: ${YELLOW}/reload-plugins${NC}"
echo -e "2. 验证安装: ${YELLOW}/harness-version${NC}"
echo -e "3. 开始使用: ${YELLOW}/harness-plan${NC}"
echo ""
echo -e "${BLUE}文档和帮助：${NC}"
echo -e "- 项目主页: https://gitee.com/duxvfeng/java-harness"
echo -e "- 使用指南: 查看 README.md"
echo ""