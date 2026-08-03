# AI Snapshot Workflow

利用 agent-browser 的 `snapshot` 命令的 AI 代理工作流程。

---

## 概述

`snapshot` 命令获取页面的可访问性树，并为每个元素分配引用 ID（`@e1`, `@e2` 等）。这样可以：

1. **无需 CSS 选择器**: 不依赖动态 ID 或类名
2. **把握上下文**: 元素角色（button、input、link）清晰
3. **确定性操作**: 通过 `@e1` 等引用可靠操作

---

## 基本工作流程

### Step 1: 打开页面

```bash
agent-browser open https://example.com
```

### Step 2: 获取快照

```bash
agent-browser snapshot -i -c
```

**选项说明**:
- `-i, --interactive`: 仅显示可交互元素（按钮、链接、输入字段等）
- `-c, --compact`: 去除空的结构元素，紧凑输出

**输出示例**:
```
✓ Example Domain
  https://example.com/

- link "Home" [ref=e1]
- link "About" [ref=e2]
- button "Login" [ref=e3]
- input "Search" [ref=e4]
- button "Search" [ref=e5]
```

### Step 3: 通过元素引用操作

```bash
# 点击链接
agent-browser click @e1

# 输入搜索表单
agent-browser fill @e4 "search query"

# 点击搜索按钮
agent-browser click @e5
```

### Step 4: 确认结果

```bash
# 新状态的快照
agent-browser snapshot -i -c
```

---

## Snapshot 选项详细说明

### `-i, --interactive`

仅显示可交互元素。在筛选操作对象时有效。

```bash
# 仅可交互元素
agent-browser snapshot -i

# 所有元素（包括文本节点）
agent-browser snapshot
```

### `-c, --compact`

去除空的结构元素（div、span 等没有内容的元素）。

```bash
# 紧凑输出
agent-browser snapshot -c

# 包括结构显示
agent-browser snapshot
```

### `-d, --depth <n>`

限制树的深度。在大页面把握概要时有效。

```bash
# 深度 3
agent-browser snapshot -d 3
```

### `-s, --selector <sel>`

将范围限制在特定选择器。

```bash
# 仅表单内
agent-browser snapshot -s "form.login"

# 仅导航内
agent-browser snapshot -s "nav"
```

### 组合

```bash
# 推荐: 可交互 + 紧凑
agent-browser snapshot -i -c

# 仅表单内的可交互元素
agent-browser snapshot -i -c -s "form"

# 浅层树把握概要
agent-browser snapshot -i -d 2
```

---

## 按使用场景的工作流程

### 登录流程

```bash
# 1. 打开登录页面
agent-browser open https://example.com/login

# 2. 获取快照
agent-browser snapshot -i -c
# 输出:
# - input "Email" [ref=e1]
# - input "Password" [ref=e2]
# - button "Login" [ref=e3]
# - link "Forgot password?" [ref=e4]

# 3. 输入登录信息
agent-browser fill @e1 "user@example.com"
agent-browser fill @e2 "password123"

# 4. 点击登录按钮
agent-browser click @e3

# 5. 确认结果
agent-browser snapshot -i -c
agent-browser get url
```

### 表单提交

```bash
# 1. 打开表单页面
agent-browser open https://example.com/contact

# 2. 表单内快照
agent-browser snapshot -i -c -s "form"
# 输出:
# - input "Name" [ref=e1]
# - input "Email" [ref=e2]
# - textarea "Message" [ref=e3]
# - button "Send" [ref=e4]

# 3. 填写表单
agent-browser fill @e1 "John Doe"
agent-browser fill @e2 "john@example.com"
agent-browser fill @e3 "Hello, this is a test message."

# 4. 提交
agent-browser click @e4

# 5. 确认
agent-browser snapshot -i -c
```

### 导航探索

```bash
# 1. 打开首页
agent-browser open https://example.com

# 2. 确认导航
agent-browser snapshot -i -c -s "nav"
# 输出:
# - link "Home" [ref=e1]
# - link "Products" [ref=e2]
# - link "About" [ref=e3]
# - link "Contact" [ref=e4]

# 3. 进入 Products 页面
agent-browser click @e2

# 4. 确认新页面结构
agent-browser snapshot -i -c
```

### 动态内容操作

```bash
# 1. 打开页面
agent-browser open https://example.com/dashboard

# 2. 初始快照
agent-browser snapshot -i -c

# 3. 打开下拉菜单
agent-browser click @e5

# 4. 等待（动态内容加载）
agent-browser wait 500

# 5. 新快照（下拉菜单显示）
agent-browser snapshot -i -c
# 新元素出现:
# - menuitem "Option 1" [ref=e10]
# - menuitem "Option 2" [ref=e11]
# - menuitem "Option 3" [ref=e12]

# 6. 选择选项
agent-browser click @e11
```

---

## 故障排除

### 找不到元素

```bash
# 完整快照（所有元素）
agent-browser snapshot

# 特定选择器筛选
agent-browser snapshot -s "#target-element"

# 等待后重试
agent-browser wait 2000
agent-browser snapshot -i -c
```

### 动态页面

```bash
# JavaScript 执行后快照
agent-browser eval "document.querySelector('#load-more').click()"
agent-browser wait 1000
agent-browser snapshot -i -c
```

### iframe 内元素

```bash
# 主框架快照
agent-browser snapshot -i -c

# iframe 内无法直接访问，
# 通过 eval 在 iframe 内操作
agent-browser eval "document.querySelector('iframe').contentDocument.querySelector('button').click()"
```

---

## 最佳实践

### 1. 始终从快照开始

操作前务必获取快照，把握当前状态。

### 2. 默认使用可交互 + 紧凑

```bash
agent-browser snapshot -i -c
```

### 3. 操作后确认状态

```bash
agent-browser click @e1
agent-browser snapshot -i -c  # 确认结果
```

### 4. 适当添加等待

有动态内容时添加等待：

```bash
agent-browser click @e1
agent-browser wait 500
agent-browser snapshot -i -c
```

### 5. 利用会话

使用会话维持认证状态：

```bash
agent-browser --session myapp open https://example.com/login
# ... 登录操作 ...
# 之后，在同一会话中继续操作
agent-browser --session myapp open https://example.com/dashboard
```