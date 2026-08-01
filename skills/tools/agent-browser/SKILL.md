---
name: agent-browser
description: "Browser automation through the repo agent-browser CLI. Explicit helper for navigation, forms, screenshots, scraping, and web-app checks. Prefer Browser Use or Playwright when available. Do NOT load for: sharing URLs, embedding links, or editing screenshot files."
description-en: "Browser automation through the repo agent-browser CLI. Explicit helper for navigation, forms, screenshots, scraping, and web-app checks. Prefer Browser Use or Playwright when available. Do NOT load for: sharing URLs, embedding links, or editing screenshot files."
description-ja: "repo の agent-browser CLI でブラウザ操作を行う明示補助スキル。ページ遷移、フォーム、スクショ、スクレイピング、Webアプリ確認向け。利用可能なら Browser Use / Playwright を優先。URL共有、リンク埋め込み、スクショ画像編集には使わない。"
description-zh: "通过仓库的 agent-browser CLI 进行浏览器自动化的辅助技能。适用于页面导航、表单填写、截图、网页抓取和 Web 应用检查。如可用优先使用 Browser Use 或 Playwright。不适用于：分享链接、嵌入链接或编辑截图文件。"
allowed-tools: ["Bash", "Read"]
user-invocable: false
disable-model-invocation: true
context: fork
argument-hint: "[url] [--headless]"
---

# Agent Browser Skill

浏览器自动化技能。使用 agent-browser CLI 执行 UI 调试、验证和自动操作。

---

## 触发短语

此技能会在以下短语下自动启动：

- "打开页面"、"检查 URL"
- "点击"、"输入"、"填写表单"
- "截图"
- "检查 UI"、"测试屏幕"
- "open this page", "click on", "fill the form", "screenshot"

---

## 功能详情

| 功能 | 详情 |
|------|------|
| **浏览器自动化** | 参见 [references/browser-automation.md](${CLAUDE_SKILL_DIR}/references/browser-automation.md) |
| **AI 快照工作流** | 参见 [references/ai-snapshot-workflow.md](${CLAUDE_SKILL_DIR}/references/ai-snapshot-workflow.md) |

## 执行步骤

### Step 0: 检查 agent-browser

```bash
# 检查安装
which agent-browser

# 如未安装
npm install -g agent-browser
agent-browser install
```

### Step 1: 分类用户请求

| 请求类型 | 对应操作 |
|----------------|---------------|
| 打开 URL | `agent-browser open <url>` |
| 点击元素 | 快照 → `agent-browser click @ref` |
| 表单输入 | 快照 → `agent-browser fill @ref "text"` |
| 状态确认 | `agent-browser snapshot -i -c` |
| 截图 | `agent-browser screenshot <path>` |
| 调试 | `agent-browser --headed open <url>` |

### Step 2: AI 快照工作流（推荐）

大多数操作中，首先**获取快照**然后通过元素引用进行操作：

```bash
# 1. 打开页面
agent-browser open https://example.com

# 2. 获取快照（AI 用，仅交互元素）
agent-browser snapshot -i -c

# 输出示例:
# - link "Home" [ref=e1]
# - button "Login" [ref=e2]
# - input "Email" [ref=e3]
# - input "Password" [ref=e4]
# - button "Submit" [ref=e5]

# 3. 通过元素引用操作
agent-browser click @e2           # 点击 Login 按钮
agent-browser fill @e3 "user@example.com"
agent-browser fill @e4 "password123"
agent-browser click @e5           # 提交
```

### Step 3: 确认结果

```bash
# 通过快照确认当前状态
agent-browser snapshot -i -c

# 或检查 URL
agent-browser get url

# 获取截图
agent-browser screenshot result.png
```

---

## 快速参考

### 基本操作

| 命令 | 说明 |
|---------|------|
| `open <url>` | 打开 URL |
| `snapshot -i -c` | AI 专用快照 |
| `click @e1` | 点击元素 |
| `fill @e1 "text"` | 填写表单 |
| `type @e1 "text"` | 输入文本 |
| `press Enter` | 按键 |
| `screenshot [path]` | 截图 |
| `close` | 关闭浏览器 |

### 导航

| 命令 | 说明 |
|---------|------|
| `back` | 后退 |
| `forward` | 前进 |
| `reload` | 刷新 |

### 信息获取

| 命令 | 说明 |
|---------|------|
| `get text @e1` | 获取文本 |
| `get html @e1` | 获取 HTML |
| `get url` | 当前 URL |
| `get title` | 页面标题 |

### 等待

| 命令 | 说明 |
|---------|------|
| `wait @e1` | 等待元素 |
| `wait 1000` | 等待 1 秒 |

### 调试

| 命令 | 说明 |
|---------|------|
| `--headed` | 显示浏览器 |
| `console` | 控制台日志 |
| `errors` | 页面错误 |
| `highlight @e1` | 高亮元素 |

---

## 会话管理

并行管理多个标签页/会话：

```bash
# 指定会话
agent-browser --session admin open https://admin.example.com
agent-browser --session user open https://example.com

# 会话列表
agent-browser session list

# 在特定会话中操作
agent-browser --session admin snapshot -i -c
```

---

## 与 MCP 浏览器工具的分工

| 工具 | 推荐度 | 用途 |
|--------|--------|------|
| **agent-browser** | ★★★ | 首选。AI 专用快照功能强大 |
| chrome-devtools MCP | ★★☆ | Chrome 已打开时使用 |
| playwright MCP | ★★☆ | 复杂 E2E 测试 |

**原则**: 首先尝试 agent-browser，仅在失败时使用 MCP 工具。

---

## 注意事项

- agent-browser 默认为无头模式
- 使用 `--headed` 选项可显示浏览器
- 会话保持到显式 `close` 为止
- 需要认证的网站请利用会话功能
