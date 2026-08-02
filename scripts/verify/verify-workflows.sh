#!/bin/bash
# 默认工作流验证脚本

echo "=== 默认工作流验证 ==="
echo ""

WORKFLOW_DIR="workflows/default"
PASSED=0
TOTAL=4

# 验证工作流文件存在
echo "1. 验证工作流文件存在"
for workflow in init plan work review; do
    FILE="$WORKFLOW_DIR/$workflow.yaml"
    if [ -f "$FILE" ]; then
        echo "   ✓ $workflow.yaml 存在"
        ((PASSED++))
    else
        echo "   ✗ $workflow.yaml 不存在"
    fi
done
echo ""

# 验证基本结构
echo "2. 验证基本结构"
for workflow in init plan work review; do
    FILE="$WORKFLOW_DIR/$workflow.yaml"
    if [ -f "$FILE" ]; then
        if grep -q "phase:" "$FILE" && grep -q "steps:" "$FILE"; then
            echo "   ✓ $workflow.yaml 包含 phase 和 steps"
            ((PASSED++))
        else
            echo "   ✗ $workflow.yaml 结构不完整"
        fi
    fi
done
echo ""

# 验证关键步骤
echo "3. 验证关键步骤"

# init.yaml
if grep -q "analyze-project" "$WORKFLOW_DIR/init.yaml"; then
    echo "   ✓ init.yaml 包含 analyze-project 步骤"
    ((PASSED++))
else
    echo "   ✗ init.yaml 缺少 analyze-project 步骤"
fi

if grep -q "project_type == 'ambiguous'" "$WORKFLOW_DIR/init.yaml"; then
    echo "   ✓ init.yaml 包含条件表达式"
    ((PASSED++))
fi

# plan.yaml
if grep -q "plan-feature" "$WORKFLOW_DIR/plan.yaml"; then
    echo "   ✓ plan.yaml 包含 plan-feature 步骤"
    ((PASSED++))
else
    echo "   ✗ plan.yaml 缺少 plan-feature 步骤"
fi

echo ""

# 统计步骤数量
echo "4. 统计步骤数量"
for workflow in init plan work review; do
    FILE="$WORKFLOW_DIR/$workflow.yaml"
    if [ -f "$FILE" ]; then
        COUNT=$(grep -c "^  - id:" "$FILE")
        echo "   ✓ $workflow.yaml 有 $COUNT 个步骤"
        ((PASSED++))
    fi
done
echo ""

echo "=== 验证总结 ==="
echo "总检查项: $TOTAL"
echo "通过: $PASSED"
echo ""

if [ $PASSED -eq $TOTAL ]; then
    echo "✅ 所有验证通过！"
    exit 0
else
    echo "⚠️  有 $(($TOTAL - $PASSED)) 项验证失败"
    exit 1
fi
