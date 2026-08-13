#!/bin/bash
# Task 14.2: 测试验证 harness-session 帮助提示
# 测试目标：验证 `/harness-session` 无参数时显示帮助，各子命令正常工作

set -e

SKILL_FILE="skills/harness-session/SKILL.md"
TESTS_PASSED=0
TESTS_FAILED=0

echo "🧪 Task 14.2: 测试 harness-session 帮助提示"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

# Test 1: 检查 SKILL.md 中的帮助提示定义
echo ""
echo "Test 1: 检查帮助提示定义"
if grep -q "无参数时的帮助提示" "$SKILL_FILE"; then
    echo "  ✅ 帮助提示章节存在"
    TESTS_PASSED=$((TESTS_PASSED + 1))
else
    echo "  ❌ 帮助提示章节不存在"
    TESTS_FAILED=$((TESTS_FAILED + 1))
fi

# Test 2: 检查主命令定义
echo ""
echo "Test 2: 检查主命令定义"
if grep -q 'name: "/harness-session"' "$SKILL_FILE"; then
    echo "  ✅ 主命令 /harness-session 已定义"
    TESTS_PASSED=$((TESTS_PASSED + 1))
else
    echo "  ❌ 主命令 /harness-session 未定义"
    TESTS_FAILED=$((TESTS_FAILED + 1))
fi

# Test 3: 检查子命令定义
echo ""
echo "Test 3: 检查子命令定义"
SUBCOMMANDS=("save" "restore" "list" "show" "cleanup")
ALL_DEFINED=true

for cmd in "${SUBCOMMANDS[@]}"; do
    if grep -q "name: \"/harness-session $cmd\"" "$SKILL_FILE"; then
        echo "  ✅ 子命令 /harness-session $cmd 已定义"
    else
        echo "  ❌ 子命令 /harness-session $cmd 未定义"
        ALL_DEFINED=false
        TESTS_FAILED=$((TESTS_FAILED + 1))
    fi
done

if $ALL_DEFINED; then
    TESTS_PASSED=$((TESTS_PASSED + 1))
fi

# Test 4: 检查帮助提示格式
echo ""
echo "Test 4: 检查帮助提示格式"
if grep -q "💾 save" "$SKILL_FILE" && grep -q "📋 restore" "$SKILL_FILE" && grep -q "📑 list" "$SKILL_FILE"; then
    echo "  ✅ 帮助提示格式正确（包含 emoji 图标）"
    TESTS_PASSED=$((TESTS_PASSED + 1))
else
    echo "  ❌ 帮助提示格式不完整"
    TESTS_FAILED=$((TESTS_FAILED + 1))
fi

# Test 5: 检查向后兼容别名
echo ""
echo "Test 5: 检查向后兼容别名"
if grep -q "向后兼容" "$SKILL_FILE" && grep -q "harness-save-session" "$SKILL_FILE"; then
    echo "  ✅ 向后兼容说明存在"
    TESTS_PASSED=$((TESTS_PASSED + 1))
else
    echo "  ❌ 向后兼容说明不存在"
    TESTS_FAILED=$((TESTS_FAILED + 1))
fi

# Test 6: 检查命令映射表
echo ""
echo "Test 6: 检查命令映射表"
if grep -q "/harness-save-session" "$SKILL_FILE" && grep -q "/harness-session save" "$SKILL_FILE"; then
    echo "  ✅ 命令映射表存在"
    TESTS_PASSED=$((TESTS_PASSED + 1))
else
    echo "  ❌ 命令映射表不存在"
    TESTS_FAILED=$((TESTS_FAILED + 1))
fi

# Test 7: 检查示例命令
echo ""
echo "Test 7: 检查示例命令"
if grep -q "示例：" "$SKILL_FILE" || grep -q "示例:" "$SKILL_FILE"; then
    echo "  ✅ 示例命令存在"
    TESTS_PASSED=$((TESTS_PASSED + 1))
else
    echo "  ❌ 示例命令不存在"
    TESTS_FAILED=$((TESTS_FAILED + 1))
fi

# Test 8: 检查 trigger 包含新命令
echo ""
echo "Test 8: 检查 trigger 包含新命令"
if grep -q 'trigger:.*\/harness-session' "$SKILL_FILE"; then
    echo "  ✅ trigger 包含新命令格式"
    TESTS_PASSED=$((TESTS_PASSED + 1))
else
    echo "  ❌ trigger 未包含新命令格式"
    TESTS_FAILED=$((TESTS_FAILED + 1))
fi

# 汇总结果
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "测试结果汇总"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "通过: $TESTS_PASSED"
echo "失败: $TESTS_FAILED"
echo "总计: $((TESTS_PASSED + TESTS_FAILED))"

if [ $TESTS_FAILED -eq 0 ]; then
    echo ""
    echo "✅ 所有测试通过！"
    exit 0
else
    echo ""
    echo "❌ 有测试失败"
    exit 1
fi
