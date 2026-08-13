#!/usr/bin/env python3
"""
强制审查集成测试示例
展示如何在不同场景中使用强制审查集成
"""

import os
import sys
import tempfile
import subprocess
from pathlib import Path

# 添加脚本目录到路径
script_dir = Path(__file__).parent.parent.parent / "scripts" / "review"
sys.path.insert(0, str(script_dir))

from forced_review_gate import ForcedReviewGate, AutoFixReviewLoop


def test_basic_review():
    """测试基本审查流程"""
    print("=== 测试 1: 基本审查流程 ===")

    # 创建临时工作目录
    with tempfile.TemporaryDirectory() as tmpdir:
        # 初始化 git 仓库
        subprocess.run(["git", "init"], cwd=tmpdir, check=True, capture_output=True)
        subprocess.run(["git", "config", "user.email", "test@example.com"], cwd=tmpdir, check=True, capture_output=True)
        subprocess.run(["git", "config", "user.name", "Test User"], cwd=tmpdir, check=True, capture_output=True)

        # 创建初始提交
        test_file = Path(tmpdir) / "test.txt"
        test_file.write_text("Initial content")
        subprocess.run(["git", "add", "."], cwd=tmpdir, check=True, capture_output=True)
        subprocess.run(["git", "commit", "-m", "Initial commit"], cwd=tmpdir, check=True, capture_output=True)

        # 获取基准引用
        base_ref = subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=tmpdir, text=True).strip()

        # 修改文件
        test_file.write_text("Modified content with potential issues")
        subprocess.run(["git", "add", "."], cwd=tmpdir, check=True, capture_output=True)
        subprocess.run(["git", "commit", "-m", "Add changes"], cwd=tmpdir, check=True, capture_output=True)

        # 执行审查
        review_gate = ForcedReviewGate()
        result = review_gate.review(base_ref=base_ref, worktree_path=tmpdir, mode="lenient")

        # 显示结果
        print(f"审查结果: {result.get('verdict')}")
        print(f"审查消息: {result.get('summary', 'N/A')}")

        if review_gate.is_approved(result):
            print("✅ 测试通过: 审查通过")
        else:
            print("❌ 测试失败: 审查未通过")
            findings = review_gate.get_findings(result)
            print(f"发现 {len(findings)} 个问题")
            for finding in findings[:3]:
                print(f"  - {finding.get('severity')}: {finding.get('message')}")


def test_auto_fix_loop():
    """测试自动修复循环"""
    print("\n=== 测试 2: 自动修复循环 ===")

    with tempfile.TemporaryDirectory() as tmpdir:
        # 初始化 git 仓库
        subprocess.run(["git", "init"], cwd=tmpdir, check=True, capture_output=True)
        subprocess.run(["git", "config", "user.email", "test@example.com"], cwd=tmpdir, check=True, capture_output=True)
        subprocess.run(["git", "config", "user.name", "Test User"], cwd=tmpdir, check=True, capture_output=True)

        # 创建初始提交
        java_file = Path(tmpdir) / "Test.java"
        java_file.write_text("""
public class Test {
    public static void main(String[] args) {
        System.out.println("Hello");  // 应该被修复
        print("Debug message");        // 应该被修复
    }
}
""")
        subprocess.run(["git", "add", "."], cwd=tmpdir, check=True, capture_output=True)
        subprocess.run(["git", "commit", "-m", "Initial code"], cwd=tmpdir, check=True, capture_output=True)

        base_ref = subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=tmpdir, text=True).strip()

        # 定义自动修复函数
        def auto_fix_func(findings, worktree_path):
            """简单的自动修复函数"""
            fixed_count = 0

            for finding in findings:
                file_path = Path(worktree_path) / finding.get("file", "")
                if file_path.exists():
                    content = file_path.read_text()

                    # 简单的修复逻辑
                    if "System.out.println" in content:
                        content = content.replace("System.out.println", "LOGGER.info")
                        fixed_count += 1
                    if "print(" in content:
                        content = content.replace("print(", "logging.info(")
                        fixed_count += 1

                    file_path.write_text(content)

            return {"success": fixed_count > 0, "fixed_count": fixed_count}

        # 创建审查门控和自动修复循环
        review_gate = ForcedReviewGate()
        auto_fix_loop = AutoFixReviewLoop(review_gate, max_iterations=2)

        # 执行自动修复循环
        final_result = auto_fix_loop.fix_and_review(
            base_ref=base_ref,
            worktree_path=tmpdir,
            auto_fix_func=auto_fix_func,
            mode="lenient"
        )

        print(f"最终结果: {final_result.get('verdict')}")
        if review_gate.is_approved(final_result):
            print("✅ 测试通过: 自动修复后审查通过")
        else:
            print("⚠️  测试说明: 自动修复未完全解决所有问题（这是正常的）")


def test_skip_review():
    """测试跳过审查功能"""
    print("\n=== 测试 3: 跳过审查功能 ===")

    with tempfile.TemporaryDirectory() as tmpdir:
        # 初始化 git 仓库
        subprocess.run(["git", "init"], cwd=tmpdir, check=True, capture_output=True)
        subprocess.run(["git", "config", "user.email", "test@example.com"], cwd=tmpdir, check=True, capture_output=True)
        subprocess.run(["git", "config", "user.name", "Test User"], cwd=tmpdir, check=True, capture_output=True)

        # 创建初始提交
        test_file = Path(tmpdir) / "test.txt"
        test_file.write_text("Emergency fix content")
        subprocess.run(["git", "add", "."], cwd=tmpdir, check=True, capture_output=True)
        subprocess.run(["git", "commit", "-m", "Emergency fix"], cwd=tmpdir, check=True, capture_output=True)

        base_ref = subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=tmpdir, text=True).strip()

        # 测试跳过审查
        review_gate = ForcedReviewGate()
        result = review_gate.review(
            base_ref=base_ref,
            worktree_path=tmpdir,
            skip_reason="emergency_critical_fix"
        )

        print(f"审查结果: {result.get('verdict')}")
        print(f"跳过原因: {result.get('skip_reason', 'N/A')}")

        if result.get('verdict') == 'APPROVE' and result.get('skip_reason'):
            print("✅ 测试通过: 成功跳过审查")
        else:
            print("❌ 测试失败: 跳过审查功能异常")


def test_integration_with_config():
    """测试配置文件集成"""
    print("\n=== 测试 4: 配置文件集成 ===")

    # 检查配置文件是否存在
    config_file = Path(__file__).parent.parent.parent / "configs" / "harness-review-config.toml"

    if config_file.exists():
        print(f"✅ 配置文件存在: {config_file}")

        # 读取配置内容
        try:
            import tomli
            with open(config_file, 'rb') as f:
                config = tomli.load(f)

            # 显示关键配置
            if 'harness' in config and 'review' in config['harness']:
                review_config = config['harness']['review']
                print(f"审查启用: {review_config.get('enabled', True)}")
                print(f"审查模式: {review_config.get('mode', 'strict')}")
                print(f"最大重试: {review_config.get('max_iterations', 3)}")

            print("✅ 测试通过: 配置文件格式正确")
        except ImportError:
            print("⚠️  tomli 未安装，跳过配置文件内容检查")
    else:
        print(f"⚠️  配置文件不存在: {config_file}")


def main():
    """运行所有测试"""
    print("🚀 开始强制审查集成测试\n")

    try:
        test_basic_review()
        test_auto_fix_loop()
        test_skip_review()
        test_integration_with_config()

        print("\n✅ 所有测试完成")
        print("\n📋 集成要点:")
        print("1. 所有 harness-work 执行都强制经过代码审查")
        print("2. 支持自动修复循环，提高通过率")
        print("3. 紧急情况支持跳过审查")
        print("4. 通过配置文件灵活控制审查行为")

    except Exception as e:
        print(f"\n❌ 测试过程中出错: {e}")
        import traceback
        traceback.print_exc()


if __name__ == "__main__":
    main()