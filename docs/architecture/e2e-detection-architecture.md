# 端到端检测架构设计文档

## 架构概述

### 系统定位
端到端检测系统是Harness质量保证体系的关键环节，位于代码审查通过后、任务完成前，确保系统功能完整性和运行时正确性。

### 核心目标
1. **自动触发**：审查通过后自动执行，无需人工干预
2. **智能修复**：检测失败时自动回到修改流程
3. **完整验证**：前后端集成测试，确保功能完整性
4. **高效执行**：快速反馈，支持并行测试
5. **可配置性**：支持不同项目类型和测试需求

---

## 系统架构

### 整体流程架构

```
┌─────────────────────────────────────────────────────────────┐
│                     Harness Work Flow                        │
├─────────────────────────────────────────────────────────────┤
│  1. Task Implementation (TDD → Sprint Contract → Code)       │
│  2. Code Review (harness-review --auto)                      │
│  3. 🔥 NEW: E2E Detection (自动触发)                          │
│     ├─ Frontend Tests (UI/Forms/Navigation)                   │
│     ├─ Backend Tests (API/Data/Error Handling)               │
│     ├─ Integration Tests (Data Flow/Exception Handling)       │
│     ├─ Performance Tests (Response Time/Concurrency)          │
│     └─ Security Tests (Auth/Data Protection)                  │
│  4. Result Processing                                        │
│     ├─ PASS → Cherry-pick → Complete                        │
│     └─ FAIL → Auto Fix Loop → Re-review → Re-detect          │
└─────────────────────────────────────────────────────────────┘
```

### 模块化组件架构

```
┌──────────────────────────────────────────────────────────────┐
│                    E2E Detection System                        │
├──────────────────────────────────────────────────────────────┤
│                                                               │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │   Trigger Manager (触发管理器)                           │ │
│  │   ┌───────────────────────────────────────────────────┐ │ │
│  │   │ • Review Pass Detector                            │ │ │
│  │   │ • Auto-Trigger Logic                              │ │ │
│  │   │ • Condition Checker                               │ │ │
│  │   └───────────────────────────────────────────────────┘ │ │
│  └─────────────────────────────────────────────────────────┘ │
│                          ↓                                    │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │   Test Runner (测试执行器)                             │ │
│  │   ┌───────────────────────────────────────────────────┐ │ │
│  │   │ • Frontend Test Executor                          │ │ │
│  │   │ • Backend Test Executor                           │ │ │
│  │   │ • Integration Test Executor                       │ │ │
│  │   │ • Performance Test Executor                        │ │ │
│  │   │ • Security Test Executor                          │ │ │
│  │   │ • Parallel Execution Controller                  │ │ │
│  │   └───────────────────────────────────────────────────┘ │ │
│  └─────────────────────────────────────────────────────────┘ │
│                          ↓                                    │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │   Result Analyzer (结果分析器)                          │ │
│  │   ┌───────────────────────────────────────────────────┐ │ │
│  │   │ • Test Result Aggregator                          │ │ │
│  │   │ • Severity Classifier                              │ │ │
│  │   │ • Issue Extractor                                 │ │ │
│  │   │ • PASS/FAIL Determiner                             │ │ │
│  │   └───────────────────────────────────────────────────┘ │ │
│  └─────────────────────────────────────────────────────────┘ │
│                          ↓                                    │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │   Auto-Fix Controller (自动修复控制器)                  │ │
│  │   ┌───────────────────────────────────────────────────┐ │ │
│  │   │ • Fix Command Generator                           │ │ │
│  │   │ • Fix Executor                                   │ │
│  │   │ • Fix Committer                                   │ │ │
│  │   │ • Retry Controller                                │ │ │
│  │   └───────────────────────────────────────────────────┘ │ │
│  └─────────────────────────────────────────────────────────┘ │
│                          ↓                                    │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │   Report Generator (报告生成器)                        │ │
│  │   ┌───────────────────────────────────────────────────┐ │ │
│  │   │ • Test Report Builder                              │ │ │
│  │   │ • Issue Reporter                                  │ │ │
│  │   │ • Fix Suggestion Provider                          │ │ │
│  │   │ • Performance Metrics Collector                    │ │ │
│  │   └───────────────────────────────────────────────────┘ │ │
│  └─────────────────────────────────────────────────────────┘ │
│                                                               │
└──────────────────────────────────────────────────────────────┘
```

---

## 核心组件设计

### 1. Trigger Manager (触发管理器)

#### 职责
- 检测审查通过事件
- 判断是否需要端到端检测
- 启动检测流程

#### 接口设计
```python
class TriggerManager:
    def should_trigger(self, review_result: dict, task_context: dict) -> bool:
        """
        判断是否应该触发端到端检测
        
        Args:
            review_result: harness-review 的返回结果
            task_context: 任务上下文信息
            
        Returns:
            bool: 是否触发端到端检测
        """
        # 检查审查是否通过
        if review_result.get("verdict") != "APPROVE":
            return False
        
        # 检查配置是否启用端到端检测
        if not self.config.is_e2e_enabled():
            return False
        
        # 检查任务类型是否需要端到端检测
        if not self._is_e2e_required(task_context):
            return False
        
        return True
    
    def trigger_detection(self, worktree_path: str, base_ref: str) -> str:
        """
        触发端到端检测
        
        Args:
            worktree_path: 工作树路径
            base_ref: 基准引用
            
        Returns:
            str: 检测任务ID
        """
        # 创建检测任务
        detection_id = self._create_detection_task(worktree_path, base_ref)
        
        # 启动检测
        self.test_runner.start_detection(detection_id)
        
        return detection_id
```

### 2. Test Runner (测试执行器)

#### 职责
- 并行执行不同类型的测试
- 管理测试超时和资源清理
- 收集测试结果

#### 接口设计
```python
class TestRunner:
    def __init__(self, config: E2EConfig):
        self.frontend_executor = FrontendTestExecutor(config)
        self.backend_executor = BackendTestExecutor(config)
        self.integration_executor = IntegrationTestExecutor(config)
        self.performance_executor = PerformanceTestExecutor(config)
        self.security_executor = SecurityTestExecutor(config)
        self.parallel_controller = ParallelExecutionController(config)
    
    def start_detection(self, detection_id: str) -> dict:
        """
        启动端到端检测
        
        Args:
            detection_id: 检测任务ID
            
        Returns:
            dict: 检测结果
        """
        # 准备测试环境
        self._prepare_test_environment()
        
        # 并行执行测试
        test_results = self.parallel_controller.execute_parallel([
            lambda: self.frontend_executor.run(),
            lambda: self.backend_executor.run(),
            lambda: self.integration_executor.run(),
            lambda: self.performance_executor.run(),
            lambda: self.security_executor.run()
        ])
        
        # 清理测试环境
        self._cleanup_test_environment()
        
        # 聚合结果
        return self._aggregate_results(test_results)
    
    def _aggregate_results(self, test_results: list) -> dict:
        """
        聚合测试结果
        
        Args:
            test_results: 各测试类型的结果列表
            
        Returns:
            dict: 聚合后的检测结果
        """
        return {
            "detection_id": self.current_detection_id,
            "timestamp": datetime.now().isoformat(),
            "status": self._determine_overall_status(test_results),
            "test_results": {
                "frontend": test_results[0],
                "backend": test_results[1],
                "integration": test_results[2],
                "performance": test_results[3],
                "security": test_results[4]
            },
            "critical_issues": self._extract_critical_issues(test_results),
            "performance_metrics": self._collect_performance_metrics(test_results),
            "execution_time": self._calculate_execution_time(test_results)
        }
```

### 3. Result Analyzer (结果分析器)

#### 职责
- 分析测试结果
- 分类问题严重程度
- 生成修复建议
- 判定PASS/FAIL

#### 接口设计
```python
class ResultAnalyzer:
    def analyze(self, detection_result: dict) -> dict:
        """
        分析检测结果
        
        Args:
            detection_result: 测试执行器的原始结果
            
        Returns:
            dict: 分析后的结果
        """
        overall_status = self._determine_overall_status(detection_result)
        
        analysis = {
            "overall_status": overall_status,
            "detection_id": detection_result["detection_id"],
            "critical_issues": [],
            "major_issues": [],
            "minor_issues": [],
            "recommendations": [],
            "fix_suggestions": [],
            "can_proceed": overall_status == "PASS"
        }
        
        if overall_status == "FAIL":
            # 提取关键问题
            analysis["critical_issues"] = self._extract_critical_issues(detection_result)
            
            # 生成修复建议
            analysis["fix_suggestions"] = self._generate_fix_suggestions(
                analysis["critical_issues"]
            )
        else:
            # 记录minor问题
            analysis["minor_issues"] = self._extract_minor_issues(detection_result)
        
        return analysis
    
    def _determine_overall_status(self, result: dict) -> str:
        """
        判定整体状态
        
        规则：
        - 任何critical → FAIL
        - 任何major → FAIL  
        - 性能测试超时2倍 → FAIL
        - 安全测试有漏洞 → FAIL
        - 仅minor/recommendation → PASS
        """
        test_results = result["test_results"]
        
        # 检查critical问题
        for test_type, test_result in test_results.items():
            if test_result.get("critical_issues"):
                return "FAIL"
        
        # 检查性能问题
        perf_result = test_results.get("performance", {})
        if perf_result.get("response_time_ratio", 0) > 2.0:
            return "FAIL"
        
        # 检查安全问题
        security_result = test_results.get("security", {})
        if security_result.get("vulnerabilities"):
            return "FAIL"
        
        return "PASS"
```

### 4. Auto-Fix Controller (自动修复控制器)

#### 职责
- 生成修复命令
- 执行修复操作
- 管理重试逻辑

#### 接口设计
```python
class AutoFixController:
    def __init__(self, max_iterations: int = 3):
        self.max_iterations = max_iterations
        self.current_iteration = 0
    
    def attempt_auto_fix(self, analysis_result: dict, worktree_path: str, 
                        worker_id: str = None) -> dict:
        """
        尝试自动修复
        
        Args:
            analysis_result: 结果分析器的问题报告
            worktree_path: 工作树路径
            worker_id: Worker ID (如需要重新审查)
            
        Returns:
            dict: 修复结果
        """
        if analysis_result["overall_status"] == "PASS":
            return {"success": True, "message": "无需修复"}
        
        if not analysis_result["critical_issues"]:
            return {"success": False, "message": "无法自动修复：无关键问题"}
        
        # 生成修复命令
        fix_commands = self._generate_fix_commands(analysis_result)
        
        # 执行修复
        fix_result = self._execute_fixes(fix_commands, worktree_path)
        
        if not fix_result["success"]:
            return {"success": False, "error": fix_result["error"]}
        
        # 提交修复
        commit_result = self._commit_fixes(worktree_path)
        
        if not commit_result["success"]:
            return {"success": False, "error": commit_result["error"]}
        
        return {
            "success": True,
            "fixes_applied": len(fix_commands),
            "commit_hash": commit_result["commit_hash"]
        }
    
    def _generate_fix_commands(self, analysis: dict) -> list:
        """
        生成修复命令
        
        Args:
            analysis: 问题分析结果
            
        Returns:
            list: 修复命令列表
        """
        fix_commands = []
        
        for issue in analysis["critical_issues"]:
            if issue["type"] == "frontend":
                fix_commands.extend(self._generate_frontend_fix(issue))
            elif issue["type"] == "backend":
                fix_commands.extend(self._generate_backend_fix(issue))
            elif issue["type"] == "integration":
                fix_commands.extend(self._generate_integration_fix(issue))
            elif issue["type"] == "performance":
                fix_commands.extend(self._generate_performance_fix(issue))
            elif issue["type"] == "security":
                fix_commands.extend(self._generate_security_fix(issue))
        
        return fix_commands
    
    def _execute_fixes(self, commands: list, worktree_path: str) -> dict:
        """
        执行修复命令
        
        Args:
            commands: 修复命令列表
            worktree_path: 工作树路径
            
        Returns:
            dict: 执行结果
        """
        for cmd in commands:
            result = subprocess.run(
                cmd,
                cwd=worktree_path,
                capture_output=True,
                text=True,
                timeout=30
            )
            
            if result.returncode != 0:
                return {
                    "success": False,
                    "error": f"修复命令失败: {cmd}",
                    "stderr": result.stderr
                }
        
        return {"success": True}
    
    def _commit_fixes(self, worktree_path: str) -> dict:
        """
        提交修复
        
        Args:
            worktree_path: 工作树路径
            
        Returns:
            dict: 提交结果
        """
        # Git add
        subprocess.run(
            ["git", "add", "-A"],
            cwd=worktree_path,
            capture_output=True
        )
        
        # Git commit
        result = subprocess.run(
            ["git", "commit", "-m", "fix: 端到端检测问题修复"],
            cwd=worktree_path,
            capture_output=True,
            text=True
        )
        
        if result.returncode != 0:
            return {"success": False, "error": result.stderr}
        
        # 获取commit hash
        hash_result = subprocess.run(
            ["git", "rev-parse", "HEAD"],
            cwd=worktree_path,
            capture_output=True,
            text=True
        )
        
        return {
            "success": True,
            "commit_hash": hash_result.stdout.strip()
        }
```

### 5. Report Generator (报告生成器)

#### 职责
- 生成人类可读的检测报告
- 提供修复建议
- 记录性能指标

#### 接口设计
```python
class ReportGenerator:
    def generate_report(self, detection_result: dict, analysis: dict) -> str:
        """
        生成检测报告
        
        Args:
            detection_result: 检测结果
            analysis: 分析结果
            
        Returns:
            str: Markdown格式的报告
        """
        report = []
        report.append("# 端到端检测报告\n")
        report.append(f"## 检测概要\n")
        report.append(f"- **检测ID**: {detection_result['detection_id']}\n")
        report.append(f"- **执行时间**: {detection_result['timestamp']}\n")
        report.append(f"- **整体状态**: {analysis['overall_status']}\n")
        report.append(f"- **执行时长**: {detection_result['execution_time']}秒\n")
        
        if analysis["overall_status"] == "FAIL":
            report.append(f"\n## ❌ 检测未通过\n")
            report.append(self._generate_failure_report(analysis))
        else:
            report.append(f"\n## ✅ 检测通过\n")
            if analysis.get("minor_issues"):
                report.append(self._generate_minor_issues_report(analysis))
        
        return "".join(report)
```

---

## 数据结构设计

### 检测结果数据结构

```python
@dataclass
class E2EDetectionResult:
    """端到端检测结果"""
    detection_id: str
    timestamp: str
    status: str  # "PASS" | "FAIL"
    
    # 各类型测试结果
    frontend_result: Optional[FrontendTestResult]
    backend_result: Optional[BackendTestResult]
    integration_result: Optional[IntegrationTestResult]
    performance_result: Optional[PerformanceTestResult]
    security_result: Optional[SecurityTestResult]
    
    # 问题汇总
    critical_issues: List[CriticalIssue]
    major_issues: List[MajorIssue]
    minor_issues: List[MinorIssue]
    
    # 性能指标
    performance_metrics: PerformanceMetrics
    
    # 执行信息
    execution_time: float
    test_types_executed: List[str]

@dataclass
class CriticalIssue:
    """关键问题"""
    issue_type: str  # "frontend" | "backend" | "integration" | "performance" | "security"
    severity: str
    file_path: str
    line_number: int
    description: str
    evidence: str
    fix_suggestion: str

@dataclass
class PerformanceMetrics:
    """性能指标"""
    total_execution_time: float
    frontend_response_time: float
    backend_response_time: float
    database_query_time: float
    memory_usage: str
    cpu_usage: str
    concurrent_users_supported: int
```

### 配置数据结构

```python
@dataclass
class E2EConfig:
    """端到端检测配置"""
    enabled: bool = True
    mode: str = "strict"  # "strict" | "lenient"
    timeout: int = 120  # 秒
    retry_on_failure: bool = True
    max_retries: int = 3
    
    # 测试类型配置
    test_types: TestTypesConfig = field(default_factory=TestTypesConfig)
    
    # 阈值配置
    thresholds: ThresholdConfig = field(default_factory=ThresholdConfig)

@dataclass
class TestTypesConfig:
    """测试类型配置"""
    frontend_enabled: bool = True
    backend_enabled: bool = True
    integration_enabled: bool = True
    performance_enabled: bool = False
    security_enabled: bool = True

@dataclass
class ThresholdConfig:
    """阈值配置"""
    response_time_max: float = 2.0  # 秒
    response_time_critical: float = 4.0  # 秒
    memory_usage_max: float = 0.8  # 80%
    cpu_usage_max: float = 0.9  # 90%
    concurrent_users_min: int = 10
```

---

## 集成接口设计

### 与 Harness-Work 集成

#### 修改点：`skills/harness-work/references/execution-modes.md`

**Breezing 模式集成**：
```python
# 当前代码（第305行）
if verdict == "APPROVE":
    cherry-pick latest_commit onto trunk
    remove the worker's worktree; delete the feature branch
    Plans.md: task.status = "cc:完了 [{hash}]"

# 集成端到端检测 ⭐
if verdict == "APPROVE":
    # 🔥 新增：端到端检测环节
    e2e_detector = E2EDetectionManager(task_context, contract_path)
    detection_result = e2e_detector.run_detection(worker_result.worktreePath, BASE_REF)
    
    if detection_result["status"] == "PASS":
        # 检测通过，继续正常流程
        cherry-pick latest_commit onto trunk
        remove the worker's worktree; delete the feature branch
        Plans.md: task.status = "cc:完了 [{hash}]"
    else:
        # 🔥 新增：失败时自动修复循环
        auto_fix = AutoFixController(max_iterations=3)
        fix_result = auto_fix.attempt_auto_fix(
            detection_result["analysis"],
            worker_result.worktreePath,
            worker_id
        )
        
        if fix_result["success"]:
            # 修复成功，重新审查
            verdict_result = call_harness_review_auto(
                base_ref=BASE_REF,
                worktree_path=worker_result.worktreePath,
                mode="strict"
            )
            
            if verdict_result["success"]:
                verdict = verdict_result["result"]["verdict"]
                
                if verdict == "APPROVE":
                    # 重新审查通过，重新端到端检测
                    detection_result = e2e_detector.run_detection(
                        worker_result.worktreePath, BASE_REF
                    )
                    
                    if detection_result["status"] == "PASS":
                        cherry-pick latest_commit onto trunk
                        Plans.md: task.status = "cc:完了 [{hash}]"
        else:
            # 自动修复失败，升级到用户
            escalate_to_user(detection_result, fix_result)
```

### 配置文件集成

#### `.claude/settings.json` 扩展

```json
{
  "harness": {
    "e2e_detection": {
      "enabled": true,
      "mode": "strict",
      "timeout": 120,
      "retry_on_failure": true,
      "max_retries": 3,
      
      "test_types": {
        "frontend": {
          "enabled": true,
          "framework": "auto",
          "test_paths": ["frontend/tests/", "src/test/ui/"]
        },
        "backend": {
          "enabled": true,
          "framework": "auto",
          "test_paths": ["backend/tests/", "src/test/api/"]
        },
        "integration": {
          "enabled": true,
          "test_scenarios": ["user_login", "data_flow", "error_handling"]
        },
        "performance": {
          "enabled": false,
          "response_time_max": 2000,
          "concurrent_users": 10
        },
        "security": {
          "enabled": true,
          "scan_vulnerabilities": true,
          "check_dependencies": true
        }
      },
      
      "thresholds": {
        "response_time": {
          "warning": 1500,
          "critical": 2000
        },
        "memory_usage": {
          "warning": 0.7,
          "critical": 0.8
        },
        "cpu_usage": {
          "warning": 0.8,
          "critical": 0.9
        }
      }
    }
  }
}
```

---

## 测试执行器设计

### Frontend Test Executor

```python
class FrontendTestExecutor:
    def __init__(self, config: E2EConfig):
        self.config = config
        self.test_framework = self._detect_framework()
    
    def run(self) -> dict:
        """
        运行前端测试
        
        Returns:
            dict: 测试结果
        """
        if not self.config.test_types.frontend_enabled:
            return {"skipped": True, "reason": "前端测试未启用"}
        
        # 检测测试框架
        if self.test_framework == "cypress":
            return self._run_cypress_tests()
        elif self.test_framework == "playwright":
            return self._run_playwright_tests()
        elif self.test_framework == "selenium":
            return self._run_selenium_tests()
        else:
            return self._run_manual_frontend_tests()
    
    def _detect_framework(self) -> str:
        """检测前端测试框架"""
        # 检查package.json中的测试依赖
        package_json = self._find_package_json()
        if package_json:
            if "cypress" in package_json.get("devDependencies", {}):
                return "cypress"
            elif "@playwright/test" in package_json.get("devDependencies", {}):
                return "playwright"
            elif "selenium-webdriver" in package_json.get("dependencies", {}):
                return "selenium"
        
        return "manual"
    
    def _run_cypress_tests(self) -> dict:
        """运行Cypress测试"""
        result = subprocess.run(
            ["npx", "cypress", "run"],
            capture_output=True,
            text=True,
            timeout=self.config.timeout
        )
        
        return self._parse_test_output(result)
    
    def _run_manual_frontend_tests(self) -> dict:
        """运行手动前端测试（基础检查）"""
        issues = []
        
        # 检查是否有前端文件
        frontend_files = self._find_frontend_files()
        if not frontend_files:
            return {"skipped": True, "reason": "未发现前端文件"}
        
        # 基础语法检查
        for file in frontend_files:
            if file.endswith((".html", ".htm", ".xhtml")):
                issues.extend(self._validate_html(file))
            elif file.endswith((".css", ".scss", ".less")):
                issues.extend(self._validate_css(file))
            elif file.endswith((".js", ".jsx", ".ts", ".tsx")):
                issues.extend(self._validate_javascript(file))
        
        return {
            "test_type": "frontend",
            "framework": "manual",
            "issues": issues,
            "critical_issues": [i for i in issues if i["severity"] == "critical"]
        }
```

### Backend Test Executor

```python
class BackendTestExecutor:
    def __init__(self, config: E2EConfig):
        self.config = config
        self.backend_type = self._detect_backend_type()
    
    def run(self) -> dict:
        """运行后端测试"""
        if not self.config.test_types.backend_enabled:
            return {"skipped": True, "reason": "后端测试未启用"}
        
        # 启动后端服务（如果需要）
        self._ensure_backend_running()
        
        # 运行API测试
        api_result = self._run_api_tests()
        
        # 运行数据库测试
        db_result = self._run_database_tests()
        
        return {
            "test_type": "backend",
            "backend_type": self.backend_type,
            "api_tests": api_result,
            "database_tests": db_result,
            "critical_issues": self._extract_critical_issues(api_result, db_result)
        }
    
    def _run_api_tests(self) -> dict:
        """运行API测试"""
        # 检测API测试框架
        if self._has_framework("pytest"):
            return self._run_pytest_api_tests()
        elif self._has_framework("junit"):
            return self._run_junit_api_tests()
        else:
            return self._run_manual_api_tests()
    
    def _ensure_backend_running(self):
        """确保后端服务运行"""
        # 检查服务健康状态
        if not self._is_backend_running():
            # 启动服务
            self._start_backend_service()
            # 等待服务就绪
            self._wait_for_backend_ready()
```

### Integration Test Executor

```python
class IntegrationTestExecutor:
    def run(self) -> dict:
        """运行集成测试"""
        integration_scenarios = self.config.test_types.integration.test_scenarios
        
        results = []
        for scenario in integration_scenarios:
            result = self._run_scenario(scenario)
            results.append(result)
        
        return {
            "test_type": "integration",
            "scenarios_tested": len(results),
            "scenarios_passed": len([r for r in results if r["passed"]]),
            "scenario_results": results,
            "critical_issues": self._extract_critical_issues(results)
        }
    
    def _run_scenario(self, scenario: str) -> dict:
        """运行单个集成场景"""
        if scenario == "user_login":
            return self._test_user_login_scenario()
        elif scenario == "data_flow":
            return self._test_data_flow_scenario()
        elif scenario == "error_handling":
            return self._test_error_handling_scenario()
        else:
            return {"skipped": True, "reason": f"未知场景: {scenario}"}
    
    def _test_user_login_scenario(self) -> dict:
        """测试用户登录集成场景"""
        issues = []
        
        # 1. 前端：输入用户名密码
        # 2. 前端：调用登录API
        # 3. 后端：验证用户信息
        # 4. 后端：生成token
        # 5. 后端：返回token
        # 6. 前端：保存token
        # 7. 前端：跳转到仪表板
        
        # 模拟测试
        try:
            # 测试登录API
            login_response = self._test_login_api("valid_user", "valid_password")
            if login_response["status"] != "success":
                issues.append({
                    "severity": "critical",
                    "description": "登录API失败",
                    "evidence": login_response
                })
            
            # 测试token验证
            token_validation = self._test_token_validation(login_response.get("token"))
            if not token_validation["valid"]:
                issues.append({
                    "severity": "critical",
                    "description": "Token验证失败",
                    "evidence": token_validation
                })
            
            # 测试前端跳转
            if not self._test_dashboard_redirect():
                issues.append({
                    "severity": "major",
                    "description": "仪表板跳转失败"
                })
        
        except Exception as e:
            issues.append({
                "severity": "critical",
                "description": f"集成场景测试异常: {str(e)}",
                "evidence": str(e)
            })
        
        return {
            "scenario": "user_login",
            "passed": len([i for i in issues if i["severity"] == "critical"]) == 0,
            "issues": issues
        }
```

---

## 配置系统设计

### 配置文件层次结构

```
.claude/
├── settings.json              # 用户级配置（最高优先级）
├── config/
│   ├── e2e-detection.config.json  # 项目级配置
│   └── thresholds.json          # 阈值配置
└── state/
    └── e2e-detection-state.json  # 运行时状态
```

### 配置加载优先级

```python
class ConfigLoader:
    def load_config(self) -> E2EConfig:
        """
        加载配置（优先级：用户 > 项目 > 默认）
        """
        # 1. 加载默认配置
        config = E2EConfig()
        
        # 2. 加载项目级配置
        project_config = self._load_project_config()
        if project_config:
            config = self._merge_config(config, project_config)
        
        # 3. 加载用户级配置
        user_config = self._load_user_config()
        if user_config:
            config = self._merge_config(config, user_config)
        
        # 4. 应用环境变量覆盖
        config = self._apply_env_overrides(config)
        
        return config
```

---

## 错误处理和日志

### 错误处理策略

```python
class E2EErrorHandler:
    def handle_detection_error(self, error: Exception, context: dict) -> dict:
        """
        处理检测错误
        
        Args:
            error: 异常对象
            context: 错误上下文
            
        Returns:
            dict: 错误处理结果
        """
        error_type = type(error).__name__
        
        if error_type == "TimeoutExpired":
            return {
                "success": False,
                "error_type": "timeout",
                "message": f"检测超时: {str(error)}",
                "suggestion": "考虑增加timeout配置或优化测试用例"
            }
        elif error_type == "ServiceUnavailable":
            return {
                "success": False,
                "error_type": "service_unavailable",
                "message": f"服务不可用: {str(error)}",
                "suggestion": "检查测试服务是否正常启动"
            }
        elif error_type == "TestFailure":
            return {
                "success": False,
                "error_type": "test_failure",
                "message": f"测试失败: {str(error)}",
                "suggestion": "检查测试日志了解具体失败原因"
            }
        else:
            return {
                "success": False,
                "error_type": "unknown",
                "message": f"未知错误: {str(error)}",
                "suggestion": "查看详细日志了解问题"
            }
```

### 日志系统

```python
class E2ELogger:
    def __init__(self, config: E2EConfig):
        self.log_file = config.log_file
        self.log_level = config.log_level
    
    def log_detection_start(self, detection_id: str):
        """记录检测开始"""
        self._log("INFO", f"端到端检测开始: {detection_id}")
    
    def log_detection_complete(self, detection_id: str, status: str):
        """记录检测完成"""
        self._log("INFO", f"端到端检测完成: {detection_id}, 状态: {status}")
    
    def log_test_result(self, test_type: str, result: dict):
        """记录测试结果"""
        self._log("INFO", f"测试类型: {test_type}, 结果: {result.get('status', 'unknown')}")
    
    def log_fix_attempt(self, iteration: int, issues_count: int):
        """记录修复尝试"""
        self._log("INFO", f"自动修复尝试: 第{iteration}次, 问题数: {issues_count}")
    
    def _log(self, level: str, message: str):
        """写入日志"""
        timestamp = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
        log_entry = f"[{timestamp}] [{level}] {message}\n"
        
        with open(self.log_file, "a") as f:
            f.write(log_entry)
```

---

## 性能优化设计

### 并行测试执行

```python
class ParallelExecutionController:
    def __init__(self, config: E2EConfig):
        self.max_parallel = config.max_parallel_workers
        self.executor = ThreadPoolExecutor(max_workers=self.max_parallel)
    
    def execute_parallel(self, tasks: list) -> list:
        """
        并行执行测试任务
        
        Args:
            tasks: 测试任务列表
            
        Returns:
            list: 执行结果列表
        """
        futures = []
        for task in tasks:
            future = self.executor.submit(task)
            futures.append(future)
        
        results = []
        for future in as_completed(futures, timeout=self.config.timeout):
            try:
                result = future.result()
                results.append(result)
            except Exception as e:
                results.append({
                    "test_type": "unknown",
                    "error": str(e),
                    "status": "ERROR"
                })
        
        return results
```

### 缓存机制

```python
class E2ECacheManager:
    def __init__(self):
        self.cache_dir = ".claude/cache/e2e-detection"
        self.cache_ttl = 3600  # 1小时
    
    def get_cached_result(self, detection_key: str) -> Optional[dict]:
        """获取缓存结果"""
        cache_file = os.path.join(self.cache_dir, f"{detection_key}.json")
        
        if not os.path.exists(cache_file):
            return None
        
        # 检查缓存是否过期
        file_time = os.path.getmtime(cache_file)
        if time.time() - file_time > self.cache_ttl:
            return None
        
        with open(cache_file, 'r') as f:
            return json.load(f)
    
    def save_cache_result(self, detection_key: str, result: dict):
        """保存缓存结果"""
        os.makedirs(self.cache_dir, exist_ok=True)
        cache_file = os.path.join(self.cache_dir, f"{detection_key}.json")
        
        with open(cache_file, 'w') as f:
            json.dump(result, f, indent=2)
```

---

## 总结

### 架构亮点

1. **模块化设计**: 5个核心组件各司其职，易于维护和扩展
2. **并行执行**: 支持5种测试类型并行运行，提高效率
3. **智能修复**: 自动修复循环，减少人工干预
4. **配置灵活**: 支持多层级配置，适应不同项目需求
5. **错误处理**: 完善的错误处理和日志系统
6. **性能优化**: 缓存机制和并行执行，提升检测速度

### 技术栈建议

- **前端测试**: Cypress / Playwright / Selenium
- **后端测试**: pytest / JUnit / Integration test framework
- **性能测试**: JMeter / k6 / Gatling
- **安全测试**: OWASP ZAP / Security scan tools
- **并行执行**: Python concurrent.futures / AsyncIO
- **配置管理**: JSON配置文件 + 环境变量覆盖

### 下一步行动

1. ✅ 架构设计完成
2. → Task 12.3: 创建端到端检测脚本
3. → Task 12.4: 实现自动触发机制  
4. → Task 12.5: 实现失败自动修复循环

---

**文档版本**: 1.0  
**创建时间**: 2026-08-10  
**作者**: Harness System  
**状态**: 设计完成，等待实现
