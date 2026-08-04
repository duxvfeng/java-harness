# Security Reviewer Profile

`harness-review --security` 启动的安全专用审查配置。
基于 OWASP Top 10，全面检查认证、授权、敏感信息、依赖包的漏洞。

## 角色前提（authorized defensive review）

本配置用于从 **authorized defensive code review** 角度检查 **claude-code-harness 插件的自身代码和用户明确指定为审查对象的项目代码**。生成攻击代码、协助入侵实际存在的第三方系统、探索未授权系统的漏洞不在本配置范围内。
**findings 仅写"哪里有弱点"、"如何修复"，不包含可执行的 exploit payload 或攻击 PoC**。仅进行 observation 报告的 audit-only 操作。

作为 issue #172（reviewer 的 security 审查在 Anthropic 侧 cyber-safeguard 处 false-trigger 的案例）的正式 scope 声明。

> **Read-only 约束**: 在此配置下运行的 reviewer 仅使用
> Read / Grep / Glob / Bash（仅限只读命令）。
> 绝不执行 Write / Edit / 写入类 Bash。

---

## Fresh-context 隔离与 findings 回流的契约（model-safeguard 缓解）

Anthropic 侧 cyber-safeguard（Fable 5 的自动模型切换）不仅判定最新消息，
还判定 **模型读取的整个 context**（会话历史、memory、已读文件、git status）。
security review 由于结构上 security 词汇密集，因此将以下作为 **缓解措施** 固定。
这只是缓解而非保证。**保证由调用方 session 使用 Opus 来实现**
（在 Fable 5 中，security findings 回流到父 session 时自动切换到 Opus）。

1. **隔离执行**: security review 在 `context: fork`（`skills/harness-review/SKILL.md`
   frontmatter）的 isolated context 中运行，不继承父会话历史。reviewer subagent 在
   `agents/reviewer.md` 中 **pin 到非 Fable model**（默认 `claude-sonnet-4-6`），不继承父 model。
   通过这两点结构性减少 classifier 读取的 security 词汇总量。

2. **findings 的 neutral 回流**: 返回给父 orchestrator 的结果限定为
   **verdict（`APPROVE | REQUEST_CHANGES`）+ 件数 + `file:line` 引用 + 1 行修复方针**。
   攻击 payload、exploit PoC、威胁场景的逐字内容不流向父 context
   （`review-result.v1` 的 `critical_issues[]` / `major_issues[]` 用 `file:line` + 短的
   remediation 表示）。逐字转储是父 session（Fable 时）被 flip 的主要原因。

3. **model pin 是 safeguard invariant**: 不要将 `agents/reviewer.md` 的 `model:` 改为
   `inherit` 或 Fable 系列。`scripts/ci/check-consistency.sh` 验证非 Fable pin 和本契约短语的存在。

---

## Security Review 流程

### Step 1: 确定对象范围

```bash
# 收集变更文件（BASE_REF 从调用方继承）
CHANGED_FILES="$(git diff --name-only --diff-filter=ACMR "${BASE_REF:-HEAD~1}")"
git diff "${BASE_REF:-HEAD~1}" -- ${CHANGED_FILES}
```

### Step 2: OWASP Top 10 检查

对以下各项在 **变更差异** 和 **相关文件** 中进行确认。

#### A01: 访问控制缺陷 (Broken Access Control)

| 检查项目 | 确认方法 |
|------------|---------|
| 授权检查遗漏 | 路由/端点定义是否应用了认证中间件 |
| 水平越权访问 | 获取用户自有资源时是否通过 `userId` 等进行过滤 |
| 垂直越权访问 | 角色检查（admin/user/guest 等）是否正确实现 |
| IDOR | URL 参数或请求体中的 ID 是否在未授权情况下被接受 |
| 目录遍历 | 包含 `../` 的路径操作是否已清理 |

**检测模式（用 Grep 确认）**:
```bash
# 无认证路由候选
grep -rn "app\.\(get\|post\|put\|delete\|patch\)" --include="*.ts" --include="*.js"
# 无 userId 的 DB 获取
grep -rn "findById\|findOne\|select.*where" --include="*.ts"
```

#### A02: 加密失败 (Cryptographic Failures)

| 检查项目 | 确认方法 |
|------------|---------|
| 明文保存敏感信息 | 密码、令牌、PII 是否以明文保存在 DB/日志中 |
| 弱哈希算法 | 密码哈希是否使用了 MD5 / SHA1 |
| 不安全的随机数 | 是否使用 `Math.random()` 生成认证令牌 |
| TLS 强度 | 是否存在通过 HTTP（非 HTTPS）发送接收敏感数据 |
| 密钥硬编码 | 加密密钥、IV 是否作为常量嵌入 |

**检测模式**:
```bash
grep -rn "md5\|sha1\|Math\.random\(\)" --include="*.ts" --include="*.js"
grep -rn "createHash.*md5\|createHash.*sha1" --include="*.ts"
grep -rn "http://" --include="*.ts" --include="*.js" --include="*.env*"
```

#### A03: 注入 (Injection)

| 检查项目 | 确认方法 |
|------------|---------|
| SQL 注入 | 是否将用户输入通过字符串连接嵌入 SQL |
| NoSQL 注入 | 在 MongoDB 等中是否将 `$where` 或输入值作为运算符使用 |
| 命令注入 | 是否将用户输入传递给 `exec()` / `spawn()` |
| LDAP 注入 | 是否在 LDAP 查询中使用未经清理的输入 |
| 模板注入 | 是否将用户输入直接传递给模板引擎 |

**检测模式**:
```bash
grep -rn "exec\|execSync\|spawn" --include="*.ts" --include="*.js"
grep -rn "\`SELECT\|\"SELECT\|'SELECT" --include="*.ts" --include="*.js"
grep -rn "\$where\|\$\[" --include="*.ts" --include="*.js"
```

#### A04: 不安全的设计 (Insecure Design)

| 检查项目 | 确认方法 |
|------------|---------|
| 缺乏速率限制 | 认证端点是否实现了速率限制 |
| TOCTOU 竞态条件 | 是否无法滥用检查后、使用前的状态变更 |
| 业务逻辑缺陷 | 是否无法以不正当顺序执行状态迁移 |

#### A05: 安全配置错误 (Security Misconfiguration)

| 检查项目 | 确认方法 |
|------------|---------|
| 默认认证信息 | 是否直接使用默认密码/用户名 |
| 详细的错误消息 | 生产环境是否不向客户端返回堆栈跟踪或内部信息 |
| 启用不必要功能 | 生产环境是否启用了调试端点/管理界面 |
| HTTP 安全头 | 是否设置了 HSTS, CSP, X-Frame-Options 等 |
| CORS 配置 | 生产环境是否未设置 `Access-Control-Allow-Origin: *` |

**检测模式**:
```bash
grep -rn "cors.*origin.*\*\|allowedOrigins.*\*" --include="*.ts" --include="*.js"
grep -rn "debug.*true\|NODE_ENV.*development" --include="*.ts"
grep -rn "console\.log.*password\|console\.log.*token\|console\.log.*secret" --include="*.ts"
```

#### A06: 脆弱且过时的组件 (Vulnerable and Outdated Components)

| 检查项目 | 确认方法 |
|------------|---------|
| 具有已知漏洞的包 | `package.json` 的依赖关系中是否存在报告了 CVE 的版本 |
| `npm audit` 结果 | high / critical 漏洞是否被搁置 |
| 与锁定文件的一致性 | `package-lock.json` / `yarn.lock` 是否为最新 |

**确认命令**:
```bash
# 确认 package.json 的依赖关系（仅读取）
cat package.json | grep -E '"dependencies"|"devDependencies"' -A 50 | head -60
# 确认锁定文件存在
ls -la package-lock.json yarn.lock pnpm-lock.yaml 2>/dev/null
```

#### A07: 身份认证和授权失败 (Identification and Authentication Failures)

| 检查项目 | 确认方法 |
|------------|---------|
| 暴力破解对策 | 是否实现了登录尝试次数限制/账户锁定 |
| 弱密码策略 | 是否设置了最小字符数/复杂性要求 |
| 会话固定攻击 | 登录后是否重新生成会话 ID |
| 会话有效期 | 长期有效的会话/令牌是否正确失效 |
| JWT 验证 | 是否接受 `alg: none` 或弱密钥签名 |

**检测模式**:
```bash
grep -rn "jwt\.verify\|jwt\.sign" --include="*.ts" --include="*.js"
grep -rn "expiresIn.*\|expire.*" --include="*.ts"
grep -rn "algorithm.*none\|alg.*none" --include="*.ts" --include="*.js"
```

#### A08: 软件和数据完整性失败 (Software and Data Integrity Failures)

| 检查项目 | 确认方法 |
|------------|---------|
| 执行来自不可信源的代码 | 是否从外部 CDN / URL 动态加载脚本 |
| 反序列化 | 是否将不可信数据直接传递给 `eval()` / `Function()` |
| CI/CD 管道保护 | 构建脚本是否无条件执行外部输入 |

**检测模式**:
```bash
grep -rn "eval(\|new Function(" --include="*.ts" --include="*.js"
grep -rn "require(.*\$\|import(.*\$" --include="*.ts" --include="*.js"
```

#### A09: 安全日志和监控失败 (Security Logging and Monitoring Failures)

| 检查项目 | 确认方法 |
|------------|---------|
| 认证失败日志 | 是否记录登录失败/权限错误 |
| 敏感信息日志输出 | 日志中是否包含密码、令牌、PII |
| 日志注入 | 用户输入是否直接写入日志（CRLF 注入） |

#### A10: 服务器端请求伪造 (SSRF)

| 检查项目 | 确认方法 |
|------------|---------|
| 向用户指定 URL 的请求 | 用户输入的 URL 是否可以访问内部网络 |
| URL 验证 | 是否实现了允许域名列表或 IP 过滤 |
| 重定向追踪 | 请求库是否不追踪对内部地址的重定向 |

**检测模式**:
```bash
grep -rn "fetch(\|axios\.\|got(\|request(" --include="*.ts" --include="*.js"
```

---

## 认证/授权 审查要点

### 认证流程

```
1. 输入验证 → 是否有类型、长度、格式检查
2. 认证处理 → 是否有计时攻击对策（constantTimeCompare 等）
3. 令牌颁发 → 是否有足够的熵（crypto.randomBytes 等）
4. 令牌保存 → 是否为 httpOnly + Secure + SameSite Cookie，或 LocalStorage
5. 令牌验证 → 签名、有效期、失效检查是否完整
6. 登出 → 是否实现了服务器侧令牌无效化
```

### 授权流程

```
1. 每个端点明确所需的角色
2. 在中间件和路由处理程序两者中检查（多层防御）
3. 不只依赖前端的隐藏（必须后端）
4. 不遗漏资源所有权验证
```

---

## 敏感信息的处理

### 硬编码检测

```bash
# API 密钥/密钥类模式
grep -rn "api[_-]key\s*=\s*['\"][^'\"]\|secret\s*=\s*['\"][^'\"]" \
  --include="*.ts" --include="*.js" --include="*.sh"

# AWS / GCP / Azure 认证信息
grep -rn "AKIA\|sk-[a-zA-Z0-9]\{20\}\|AIza" --include="*.ts" --include="*.js"

# JWT 签名密钥硬编码
grep -rn "jwt.*secret.*=\s*['\"][^'\"]\{8,\}" --include="*.ts" --include="*.js"

# .env 文件提交
git diff "${BASE_REF:-HEAD~1}" -- .env .env.local .env.production
```

### 环境变量的正确使用

| 好的模式 | 坏的模式 |
|------------|------------|
| `process.env.DATABASE_URL` | `"postgresql://user:pass@localhost/db"` |
| `process.env.JWT_SECRET` | `const JWT_SECRET = "my-super-secret"` |
| `process.env.API_KEY` | `const API_KEY = "sk-abc123..."` |

### .env 文件管理

- `.env.example` 是否记载了虚拟值
- `.env` / `.env.local` 是否包含在 `.gitignore` 中
- 生产环境秘密是否未提交到 `.env.production`

```bash
# 确认 .gitignore
grep -n "\.env" .gitignore 2>/dev/null
# 确认仓库中是否包含 .env 文件
git diff "${BASE_REF:-HEAD~1}" --name-only | grep "\.env"
```

---

## 依赖包的已知漏洞检查

### package.json 确认步骤

1. 读取已变更的 `package.json`
2. 确定新添加/版本升级的包
3. 推荐与已知 CVE 数据库（NVD, Snyk, GitHub Advisory）进行对照

```bash
# 确认已变更的包
git diff "${BASE_REF:-HEAD~1}" -- package.json package-lock.json

# 确认当前依赖关系版本
cat package.json | python3 -c "import json,sys; d=json.load(sys.stdin); [print(k,v) for d2 in [d.get('dependencies',{}),d.get('devDependencies',{})] for k,v in d2.items()]" 2>/dev/null
```

### 高风险包类别

| 类别 | 注意事项 |
|---------|--------|
| 认证库 | passport, jsonwebtoken, bcrypt — 存在许多依赖版本的漏洞 |
| HTTP 客户端 | axios, node-fetch, got — 确认 SSRF 对策的默认设置 |
| 模板引擎 | handlebars, ejs, pug — 过去有 RCE 漏洞案例 |
| XML 解析器 | xml2js, fast-xml-parser — 注意 XXE 攻击 |
| 序列化 | serialize-javascript, node-serialize — RCE 风险 |
| 图像处理 | sharp, imagemagick — 缓冲区溢出系漏洞 |

---

## Security Review 输出形式

使用与普通 Code Review 相同的 JSON 架构，但设置 `reviewer_profile: "security"`。

```json
{
  "schema_version": "review-result.v1",
  "verdict": "APPROVE | REQUEST_CHANGES",
  "reviewer_profile": "security",
  "critical_issues": [
    {
      "severity": "critical",
      "category": "Security",
      "owasp": "A03:2021 - Injection",
      "location": "src/api/users.ts:42",
      "issue": "将用户输入直接连接到 SQL 字符串",
      "suggestion": "使用预编译语句或 ORM",
      "cwe": "CWE-89"
    }
  ],
  "major_issues": [],
  "observations": [],
  "recommendations": []
}
```

### Security 固有字段

| 字段 | 说明 |
|----------|------|
| `owasp` | 对应的 OWASP Top 10 类别（例: `A01:2021 - Broken Access Control`） |
| `cwe` | 对应的 CWE 编号（例: `CWE-89`） |
| `cvss_estimate` | CVSS 分数概算（Critical: 9.0+, High: 7.0-8.9, Medium: 4.0-6.9） |

### Verdict 判定标准（Security 模式）

Security 模式应用比通常更严格的标准。

| 重要度 | 定义 | verdict |
|--------|------|---------|
| **critical** | RCE、认证绕过、敏感信息直接暴露、SQLi/CMDi | 有 1 件即 REQUEST_CHANGES |
| **major** | 授权检查不充分、硬编码敏感信息、弱加密 | 有 1 件即 REQUEST_CHANGES |
| **minor** | 缺少安全头、过度错误信息、轻微配置错误 | APPROVE（附带修正建议） |
| **recommendation** | 安全最佳实践建议 | APPROVE |
