#!/usr/bin/env python3
"""
强制审查集成 Python API
提供编程接口来调用强制审查集成
"""

import subprocess
import json
import os
import tempfile
from pathlib import Path
from typing import Dict, Optional, Literal


class ForcedReviewGate:
    """强制审查门控类"""

    def __init__(
        self,
        script_path: Optional[str] = None,
        default_mode: Literal["strict", "lenient"] = "strict",
        default_timeout: int = 30
    ):
        """
        初始化强制审查门控

        Args:
            script_path: 审查脚本路径 (默认: scripts/review/forced-review-gate.sh)
            default_mode: 默认审查模式
            default_timeout: 默认超时时间（秒）
        """
        if script_path is None:
            # 默认脚本路径
            script_root = Path(__file__).parent.parent.parent
            script_path = script_root / "scripts" / "review" / "forced-review-gate.sh"

        self.script_path = Path(script_path)
        self.default_mode = default_mode
        self.default_timeout = default_timeout

        # 验证脚本存在
        if not self.script_path.exists():
            raise FileNotFoundError(f"审查脚本不存在: {self.script_path}")

    def review(
        self,
        base_ref: str,
        worktree_path: str,
        mode: Optional[Literal["strict", "lenient"]] = None,
        max_retries: Optional[int] = None,
        timeout: Optional[int] = None,
        skip_reason: Optional[str] = None,
        output_file: Optional[str] = None
    ) -> Dict:
        """
        执行强制审查

        Args:
            base_ref: 基准 commit SHA
            worktree_path: 工作树路径
            mode: 审查模式 (strict|lenient)
            max_retries: 最大重试次数
            timeout: 超时时间（秒）
            skip_reason: 跳过审查的原因
            output_file: 输出文件路径

        Returns:
            审查结果字典
        """
        # 使用默认值
        mode = mode or self.default_mode
        timeout = timeout or self.default_timeout

        # 创建临时输出文件
        if output_file is None:
            output_file = tempfile.mktemp(suffix=".json", prefix="harness-review-")

        # 构建命令
        cmd = [
            str(self.script_path),
            "--base-ref", base_ref,
            "--worktree-path", worktree_path,
            "--mode", mode,
            "--timeout", str(timeout),
            "--output", output_file
        ]

        if max_retries is not None:
            cmd.extend(["--max-retries", str(max_retries)])

        if skip_reason is not None:
            cmd.extend(["--skip", skip_reason])

        try:
            # 执行审查
            result = subprocess.run(
                cmd,
                capture_output=True,
                text=True,
                timeout=timeout,
                check=False
            )

            # 读取结果文件
            if os.path.exists(output_file):
                with open(output_file, 'r') as f:
                    review_result = json.load(f)

                # 清理临时文件
                try:
                    os.remove(output_file)
                except:
                    pass

                # 添加执行信息
                review_result["execution"] = {
                    "exit_code": result.returncode,
                    "stdout": result.stdout,
                    "stderr": result.stderr
                }

                return review_result
            else:
                return {
                    "verdict": "ERROR",
                    "error": "输出文件不存在",
                    "execution": {
                        "exit_code": result.returncode,
                        "stdout": result.stdout,
                        "stderr": result.stderr
                    }
                }

        except subprocess.TimeoutExpired:
            return {
                "verdict": "ERROR",
                "error": f"审查超时（超过 {timeout} 秒）"
            }
        except Exception as e:
            return {
                "verdict": "ERROR",
                "error": str(e)
            }

    def is_approved(self, review_result: Dict) -> bool:
        """检查审查是否通过"""
        return review_result.get("verdict") == "APPROVE"

    def get_findings(self, review_result: Dict, severity: Optional[str] = None) -> list:
        """
        获取审查发现

        Args:
            review_result: 审查结果
            severity: 过滤特定严重程度 (critical|major|minor|recommendation)

        Returns:
            问题列表
        """
        findings = review_result.get("findings", [])

        if severity is not None:
            return [f for f in findings if f.get("severity") == severity]

        return findings

    def format_summary(self, review_result: Dict) -> str:
        """格式化审查结果摘要"""
        verdict = review_result.get("verdict", "ERROR")

        if verdict == "APPROVE":
            return "✅ 审查通过"
        elif verdict == "REQUEST_CHANGES":
            findings = review_result.get("findings", [])
            critical_count = sum(1 for f in findings if f.get("severity") == "critical")
            major_count = sum(1 for f in findings if f.get("severity") == "major")

            return f"❌ 审查未通过 (Critical: {critical_count}, Major: {major_count})"
        else:
            return f"❌ 审查错误: {review_result.get('error', 'Unknown error')}"


class AutoFixReviewLoop:
    """自动修复审查循环"""

    def __init__(self, review_gate: ForcedReviewGate, max_iterations: int = 3):
        """
        初始化自动修复循环

        Args:
            review_gate: 审查门控实例
            max_iterations: 最大修复尝试次数
        """
        self.review_gate = review_gate
        self.max_iterations = max_iterations

    def fix_and_review(
        self,
        base_ref: str,
        worktree_path: str,
        auto_fix_func: callable,
        mode: str = "strict"
    ) -> Dict:
        """
        执行自动修复循环

        Args:
            base_ref: 基准 commit
            worktree_path: 工作树路径
            auto_fix_func: 自动修复函数，接收 findings 参数
            mode: 审查模式

        Returns:
            最终审查结果
        """
        iteration = 0
        current_base = base_ref

        while iteration < self.max_iterations:
            # 执行审查
            review_result = self.review_gate.review(
                base_ref=current_base,
                worktree_path=worktree_path,
                mode=mode
            )

            # 检查是否通过
            if self.review_gate.is_approved(review_result):
                print(f"✅ 审查通过（第 {iteration + 1} 次尝试）")
                return review_result

            # 获取需要修复的问题
            findings = self.review_gate.get_findings(review_result, "critical")
            findings.extend(self.review_gate.get_findings(review_result, "major"))

            if not findings:
                print("⚠️  没有需要修复的 critical/major 问题")
                return review_result

            print(f"🔧 第 {iteration + 1} 次修复：发现 {len(findings)} 个问题")

            # 尝试自动修复
            try:
                fix_result = auto_fix_func(findings, worktree_path)

                if not fix_result.get("success", False):
                    print(f"❌ 自动修复失败: {fix_result.get('error')}")
                    return review_result

                # 提交修复
                self._commit_fixes(worktree_path, fix_result.get("fixed_count", 0))

                # 更新基准引用
                current_base = self._get_current_head(worktree_path)

            except Exception as e:
                print(f"❌ 修复过程出错: {e}")
                return review_result

            iteration += 1

        print(f"⚠️  达到最大重试次数 ({self.max_iterations})")
        return review_result

    def _commit_fixes(self, worktree_path: str, fixed_count: int):
        """提交修复"""
        subprocess.run(
            ["git", "add", "-A"],
            cwd=worktree_path,
            check=True,
            capture_output=True
        )

        subprocess.run(
            ["git", "commit", "-m", f"fix: 审查问题修复 ({fixed_count} 个问题)"],
            cwd=worktree_path,
            check=True,
            capture_output=True
        )

    def _get_current_head(self, worktree_path: str) -> str:
        """获取当前 HEAD"""
        result = subprocess.run(
            ["git", "rev-parse", "HEAD"],
            cwd=worktree_path,
            check=True,
            capture_output=True,
            text=True
        )
        return result.stdout.strip()


# 使用示例
if __name__ == "__main__":
    # 创建审查门控实例
    review_gate = ForcedReviewGate()

    # 执行审查
    result = review_gate.review(
        base_ref="abc123",
        worktree_path="/path/to/worktree",
        mode="strict"
    )

    # 检查结果
    print(review_gate.format_summary(result))

    # 如果有问题，显示详情
    if not review_gate.is_approved(result):
        findings = review_gate.get_findings(result)
        for finding in findings[:5]:  # 显示前5个问题
            print(f"  {finding.get('severity')}: {finding.get('file')}:{finding.get('line')} - {finding.get('message')}")

    # 使用自动修复循环
    # def auto_fix_func(findings, worktree_path):
    #     # 实现自动修复逻辑
    #     return {"success": True, "fixed_count": len(findings)}
    #
    # auto_fix_loop = AutoFixReviewLoop(review_gate, max_iterations=3)
    # final_result = auto_fix_loop.fix_and_review(
    #     base_ref="abc123",
    #     worktree_path="/path/to/worktree",
    #     auto_fix_func=auto_fix_func
    # )