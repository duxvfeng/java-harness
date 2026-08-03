# Bump Level Detection

从 `[Unreleased]` 章节内容推定 bump level (patch/minor/major) 的逻辑。

## 判定规则

扫描 `[Unreleased]` 直下所有 `### <category>` 标题，按以下优先级判定:

```
1. 包含 "### Breaking Changes"             → major
2. 包含 "### Removed"                      → major
3. 包含 "### Added" (无上述情况)             → minor
4. 包含 "### Deprecated" (无上述情况)        → minor
5. 仅 "### Fixed" / "### Changed" / "### Security" → patch
6. 无任何子章节 (空)              → error
```

## 实现

```python
import re

def detect_bump(changelog_text: str) -> str:
    """Return 'major' | 'minor' | 'patch'. Raises on empty [Unreleased]."""
    # 抽取 [Unreleased] 章节
    m = re.search(
        r"## \[Unreleased\]\s*\n(.*?)(?=\n## \[|\Z)",
        changelog_text,
        re.S,
    )
    if not m:
        raise RuntimeError("[Unreleased] 章节未找到")
    body = m.group(1).strip()
    if not body:
        raise RuntimeError("[Unreleased] 为空。无发布对象")

    # 收集标题
    headings = set(re.findall(r"^### (.+?)\s*$", body, re.M))

    if "Breaking Changes" in headings or "Removed" in headings:
        return "major"
    if "Added" in headings or "Deprecated" in headings:
        return "minor"
    if headings & {"Fixed", "Changed", "Security"}:
        return "patch"
    raise RuntimeError(f"[Unreleased] 中无识别的子章节: {headings}")
```

## 为何 Deprecated 是 minor

根据 Keep a Changelog 规格，Deprecated 是"将来 Removed 的预定通告"。
与功能添加/变更同等的用户影响，因此处理为 minor。
实际 Removed 时点升为 major。

## 用户 override

明示指定 `/release patch|minor|major` 时跳过此自动判定，使用指定值。
但 **bump 对象章节为空** 时即使 override 也中止（因为无发布内容）。

## 不支持表记差异

以下不识别:

| 常见错误表记 | 正确表记 |
|-----------------|-----------|
| `### Features` | `### Added` |
| `### Bug Fixes` / `### Fix` | `### Fixed` |
| `### BREAKING CHANGE` / `### Breaking` | `### Breaking Changes` |
| `### Enhancements` | `### Changed` 或 `### Added` |

调用 `/release` 前对齐 KaCL 的标准标题。
Gate 前检测无法识别的标题时发出警告，促使用户修正。

## pre-release / build metadata 的处理

当前版本为 `1.0.0-alpha.1` 等 pre-release suffix 时，此技能

1. 忽略 suffix 部分计算 bump（`1.0.0-alpha.1` → patch → `1.0.1`）
2. suffix 废弃（不做 `1.0.1-alpha.1`）

希望 pre-release 状态下 bump 时，即使 override 指定 bump 行为也不变。
有意继续 pre-release 的项目此技能不支持。

## 空 [Unreleased] 的处理

`/release` 以空 [Unreleased] 调用时，提议以下:

- "无发布对象。请给 `[Unreleased]` 添加 `### Fixed` 等，或如希望仅标记的维护发布请考虑 `--empty` 标志"

`--empty` 标志本技能不支持（原则上不创建空 release）。
