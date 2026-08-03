# Release Notes Format

将 CHANGELOG 的 `## [X.Y.Z]` 章节转换为 GitHub Release 用笔记的规则。

## CHANGELOG 草稿制作（内存中，Pre-Gate 步骤 7）

向 Confirmation Gate 提示前，在内存中计算以下内容（尚未写入文件）:

1. 切出 `## [Unreleased]` 的正文。
2. 制作在 `## [Unreleased]` 和 `## [<previous>]` 之间插入 `## [<new>] - YYYY-MM-DD` 的形式。
3. 更新末尾 compare link:
   - `[Unreleased]: .../compare/v<prev>...HEAD` → `v<new>...HEAD`
   - 追加 `[<new>]: .../compare/v<prev>...v<new>`。
4. repo URL 从既有 `[Unreleased]: ` 行动态抽出。

## 语言

- **GitHub Release notes: 英语** (面向公开仓库的标准)
- **CHANGELOG.md: 日语** (项目第一语言为日语时)

用日语写 CHANGELOG 时，创建 GitHub Release 时需要英译。
技能调用 Claude 生成 draft，在 Confirmation Gate 让用户确认。

## 必需要素

```markdown
## What's Changed

**<1-line value summary>**

### Before / After

| Before | After |
|--------|-------|
| <previous UX> | <new UX> |

---

### Added
- <item>

### Changed
- <item>

### Fixed
- <item>

---

🤖 Generated with [Claude Code](https://claude.com/claude-code)
```

## 要素的生成方法

### "What's Changed" 的摘要

从 CHANGELOG `[X.Y.Z]` 章节的 `### 主题` 行提取。
没有时从 Added/Changed/Fixed 的首项用 1 句摘要。

### Before / After 表格

从 CHANGELOG 的"迄今为止 / 今后"记述提取。
没有时从以下推测:
- Fixed 项目 → "<bug description>" vs "Fixed"
- Added 项目 → "<feature>无法使用" vs "可以使用"
- Changed 项目 → "<old behavior>" vs "<new behavior>"

### Added / Changed / Fixed

原样英译 CHANGELOG 的对应章节转记。

### 页脚

固定: `🤖 Generated with [Claude Code](https://claude.com/claude-code)`

## Draft 确认

Confirmation Gate 提示以下:

```
GitHub Release Preview:
━━━━━━━━━━━━━━━━━━━━━━
Title: v4.0.4 - Fix CI validation gap
Body (first 20 lines):

  ## What's Changed

  **Fixed a gap in validate-plugin.sh ...**
  ...

(Full body: 45 lines)
━━━━━━━━━━━━━━━━━━━━━━
```

用户指示"修正:..."时重新生成。

## 验证

向 workflow 传递 release notes 前，检查是否满足以下:

1. 存在 `## What's Changed` 章节
2. 存在**粗体摘要**行
3. 存在 `### Before / After` 表格
4. 存在页脚 `Generated with [Claude Code]`

不满足时返回 Gate 促修正。

## 多个更改的总结方式

CHANGELOG 的 `[X.Y.Z]` 有 2 个以上功能时:

- Title: 用最重要的 1 个代表 (或 "Multiple fixes and improvements")
- Body: 各功能用 `### N. <feature name>` 分割英译

同日发布多个版本不推荐（versioning.md）。用 batch release 汇总。
