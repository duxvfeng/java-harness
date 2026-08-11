# Playwright 端到端测试使用指南

## 🎭 关于 Playwright 集成

Harness 端到端检测系统现已完全支持 **Playwright** 作为主要的端到端测试框架！

### 为什么选择 Playwright？

✅ **现代化测试工具**: 支持最新浏览器特性
✅ **跨浏览器支持**: Chromium、Firefox、WebKit 一套代码
✅ **快速可靠**: 自动等待机制，减少不稳定的测试
✅ **丰富功能**: 截图、录屏、追踪、网络拦截等
✅ **优秀工具**: VS Code 插件、调试器、代码生成器
✅ **活跃社区**: 定期更新，问题快速解决

---

## 🚀 快速开始

### 1. 安装 Playwright

```bash
# 在你的项目中安装
npm install --save-dev @playwright/test

# 安装浏览器
npx playwright install
```

### 2. 创建测试文件

```typescript
// e2e/basic.spec.ts
import { test, expect } from '@playwright/test';

test.describe('基本功能测试', () => {
  test('页面加载测试', async ({ page }) => {
    await page.goto('/');

    // 检查页面标题
    await expect(page).toHaveTitle(/./);

    // 检查主元素存在
    const mainElement = await page.$('main') || await page.$('body');
    expect(mainElement).not.toBeNull();
  });

  test('用户登录测试', async ({ page }) => {
    await page.goto('/login');

    // 填写登录表单
    await page.fill('input[name="username"]', 'testuser');
    await page.fill('input[name="password"]', 'password123');
    await page.click('button[type="submit"]');

    // 检查登录成功
    await expect(page).toHaveURL(/.*\/dashboard/);
    await expect(page.locator('.user-info')).toContainText('testuser');
  });
});
```

### 3. 配置文件

```typescript
// playwright.config.ts
import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
  testDir: './e2e',
  timeout: 30000,
  expect: {
    timeout: 5000
  },
  use: {
    headless: true,
    viewport: { width: 1280, height: 720 },
    ignoreHTTPSErrors: true,
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
    trace: 'retain-on-failure'
  },
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
    {
      name: 'firefox',
      use: { ...devices['Desktop Firefox'] },
    },
    {
      name: 'webkit',
      use: { ...devices['Desktop Safari'] },
    }
  ],
  reporter: 'html'
});
```

---

## 🔧 在 Harness 中使用

### 自动检测和执行

一旦你的项目配置了 Playwright，Harness 端到端检测系统会：

1. **自动检测**: 识别 `@playwright/test` 依赖和配置文件
2. **智能执行**: 使用专门的 Playwright 执行器运行测试
3. **结果分析**: 详细分析测试失败原因并提供修复建议
4. **自动修复**: 针对常见问题自动生成修复命令

### 配置端到端检测

在你的项目根目录创建配置文件：

```json
// .claude/config/e2e-detection.config.json
{
  "enabled": true,
  "test_types": {
    "frontend": {
      "enabled": true,
      "framework": "playwright",
      "playwright_config": {
        "browsers": ["chromium", "firefox", "webkit"],
        "timeout": 30000,
        "retries": 1,
        "headless": true
      }
    }
  }
}
```

---

## 📊 Playwright 测试场景

### 1. 用户认证流程

```typescript
test('用户登录流程', async ({ page }) => {
  // 访问登录页面
  await page.goto('/login');

  // 填写表单
  await page.fill('input[name="email"]', 'user@example.com');
  await page.fill('input[name="password"]', 'SecurePass123!');

  // 提交表单
  await page.click('button[type="submit"]');

  // 等待导航
  await page.waitForURL(/.*\/dashboard/);

  // 验证登录成功
  await expect(page.locator('.user-profile')).toBeVisible();
  await expect(page.locator('.user-profile')).toContainText('user@example.com');
});
```

### 2. API 集成测试

```typescript
test('API 数据获取测试', async ({ page }) => {
  // 监听 API 响应
  page.on('response', async (response) => {
    if (response.url().includes('/api/data')) {
      const data = await response.json();
      expect(data).toHaveProperty('results');
      expect(data.results).toBeInstanceOf(Array);
    }
  });

  // 触发 API 调用
  await page.goto('/data');
  await page.click('button.load-data');

  // 等待结果显示
  await expect(page.locator('.data-list')).toBeVisible();
});
```

### 3. 表单验证测试

```typescript
test('表单验证测试', async ({ page }) => {
  await page.goto('/contact');

  // 测试必填字段验证
  await page.click('button[type="submit"]');

  // 检查错误提示
  await expect(page.locator('.error[name]')).toHaveText('Name is required');
  await expect(page.locator('.error[email]')).toHaveText('Email is required');

  // 填写有效数据
  await page.fill('input[name="name"]', 'John Doe');
  await page.fill('input[name="email"]', 'john@example.com');
  await page.fill('textarea[name="message"]', 'Test message');

  // 提交表单
  await page.click('button[type="submit"]');

  // 检查成功消息
  await expect(page.locator('.success-message')).toBeVisible();
});
```

### 4. 错误处理测试

```typescript
test('错误处理测试', async ({ page, context }) => {
  // 模拟 API 错误
  await context.route('**/api/**', route => {
    route.fulfill({
      status: 500,
      contentType: 'application/json',
      body: JSON.stringify({ error: 'Internal Server Error' })
    });
  });

  await page.goto('/data');

  // 触发 API 调用
  await page.click('button.load-data');

  // 检查错误处理
  await expect(page.locator('.error-message')).toBeVisible();
  await expect(page.locator('.error-message')).toContainText('Internal Server Error');
});
```

---

## 🛠️ 高级功能

### 1. 多浏览器测试

```typescript
test.describe('跨浏览器测试', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/');
  });

  test('Chrome 测试', async ({ page, browserName }) => {
    expect(browserName).toBe('chromium');
    // Chrome 特定测试
  });

  test('Firefox 测试', async ({ page, browserName }) => {
    expect(browserName).toBe('firefox');
    // Firefox 特定测试
  });
});
```

### 2. 视觉回归测试

```typescript
test('页面截图对比', async ({ page }) => {
  await page.goto('/');
  await page.waitForLoadState('networkidle');

  // 截图对比
  await expect(page).toHaveScreenshot('homepage.png');
});
```

### 3. 性能测试

```typescript
test('页面性能测试', async ({ page }) => {
  // 开始性能监控
  await page.goto('/');

  // 获取性能指标
  const metrics = await page.evaluate(() => {
    const navigation = performance.getEntriesByType('navigation')[0] as PerformanceNavigationTiming;
    return {
      domContentLoaded: navigation.domContentLoadedEventEnd - navigation.domContentLoadedEventStart,
      loadComplete: navigation.loadEventEnd - navigation.loadEventStart
    };
  });

  // 验证性能指标
  expect(metrics.domContentLoaded).toBeLessThan(2000);
  expect(metrics.loadComplete).toBeLessThan(3000);
});
```

---

## 🎯 最佳实践

### 1. 测试组织

```typescript
// 按功能模块组织测试
test.describe('用户认证', () => {
  test.beforeEach(async ({ page }) => {
    // 每个测试前的准备
  });

  test('登录功能', async ({ page }) => {});
  test('登出功能', async ({ page }) => {});
  test('密码重置', async ({ page }) => {});
});
```

### 2. 页面对象模式

```typescript
// pages/LoginPage.ts
export class LoginPage {
  constructor(private page: Page) {}

  async goto() {
    await this.page.goto('/login');
  }

  async login(email: string, password: string) {
    await this.page.fill('input[name="email"]', email);
    await this.page.fill('input[name="password"]', password);
    await this.page.click('button[type="submit"]');
  }

  async waitForDashboard() {
    await this.page.waitForURL(/.*\/dashboard/);
  }
}

// 使用
test('用户登录', async ({ page }) => {
  const loginPage = new LoginPage(page);
  await loginPage.goto();
  await loginPage.login('user@example.com', 'password');
  await loginPage.waitForDashboard();
});
```

### 3. 环境变量管理

```typescript
// 使用环境变量
const BASE_URL = process.env.BASE_URL || 'http://localhost:3000';

test('环境变量测试', async ({ page }) => {
  await page.goto(`${BASE_URL}/login`);
  // ...
});
```

---

## 🐛 调试技巧

### 1. 使用 headed 模式

```bash
# 显示浏览器窗口
npx playwright test --headed
```

### 2. 调试模式

```bash
# 启动调试器
npx playwright test --debug
```

### 3. 慢动作模式

```typescript
test('慢动作测试', async ({ page }) => {
  await page.slowMotion(1000); // 1秒延迟
  await page.goto('/');
  // ...
});
```

### 4. 代码生成器

```bash
# 录制用户操作生成测试代码
npx playwright codegen http://localhost:3000
```

---

## 📈 与 Harness 集成

### 自动触发流程

```
代码审查通过 → 自动 Playwright 测试 → 结果分析 → 自动修复
```

### 失败自动修复

系统会自动尝试修复常见问题：

- **选择器问题**: 更新过时的 CSS 选择器
- **超时问题**: 调整测试超时时间
- **依赖问题**: 更新 Playwright 版本
- **配置问题**: 修复配置文件错误

### 测试报告

生成详细的测试报告：

```bash
# 查看 HTML 报告
npx playwright show-report
```

---

## 🔗 相关资源

- [Playwright 官方文档](https://playwright.dev)
- [Playwright 最佳实践](https://playwright.dev/docs/best-practices)
- [VS Code Playwright 插件](https://marketplace.visualstudio.com/items?itemName=ms-playwright.playwright)
- [Harness 端到端检测文档](../architecture/e2e-detection-architecture.md)

---

## ✅ 总结

使用 Playwright 进行端到端测试的优势：

✅ **现代化**: 支持最新 Web 标准和浏览器特性
✅ **可靠性**: 自动等待机制减少不稳定的测试
✅ **强大功能**: 截图、录屏、追踪等丰富功能
✅ **易用性**: 直观的 API 和优秀的开发者工具
✅ **跨平台**: 支持所有主流操作系统和浏览器
✅ **性能**: 并行测试执行，快速反馈
✅ **社区支持**: 活跃的社区和定期更新

开始使用 Playwright，提升你的端到端测试质量！🎭