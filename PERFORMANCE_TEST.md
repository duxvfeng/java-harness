# Claude Harness 性能测试指南

## 性能目标

| 组件 | 指标 | 目标值 | 测试方法 |
|------|------|--------|----------|
| **CLI Gateway** | Hook 响应时间 | < 10ms | IpcPerformanceTest |
| **Guardrail Engine** | 规则评估时间 | < 5ms | GuardrailTest |
| **Spring Service** | API 响应时间 | < 50ms | PerformanceTest |
| **数据库操作** | CRUD 操作 | < 100ms | PerformanceTest |
| **并行执行** | 性能提升 | > 2x | OrchestratorTest |

## 运行性能测试

### 1. CLI Gateway 性能测试

```bash
cd cli-native
mvn test-compile
java -cp target/classes:target/test-classes \
  com.chachamaru.harness.cli.IpcPerformanceTest
```

### 2. Spring Service 性能测试

```bash
# 确保服务正在运行
./start-service.sh

# 运行性能测试
mvn test -Dtest=PerformanceTest -pl spring-service
```

### 3. 集成性能测试

```bash
# 启动服务
./start-service.sh

# 运行完整测试套件
mvn test

# 运行集成测试
./test-integration.sh
```

## 性能基准

### Guardrail Engine 性能

- **单次评估**：~0.5-2ms
- **15 条规则完整评估**：~2-5ms
- **目标**：< 5ms ✅

### REST API 性能

- **健康检查**：~5-10ms
- **状态查询**：~10-30ms
- **会话创建**：~20-50ms
- **目标**：< 50ms ✅

### 数据库操作性能

- **Session 创建**：~10-30ms
- **Session 查询**：~5-15ms
- **WorkState 创建**：~10-30ms
- **目标**：< 100ms ✅

### 并行执行性能

- **4 个任务串行**：~400ms
- **4 个任务并行**：~100-150ms
- **性能提升**：~2.5-3x ✅

## 性能优化建议

### 1. 数据库优化

- 使用索引：已在 Flyway 迁移中配置
- 连接池：HikariCP（Spring Boot 默认）
- 批量操作：使用 MyBatis batch

### 2. API 优化

- 异步处理：使用 CompletableFuture
- 缓存：考虑添加 Redis 缓存
- 压缩：对大响应启用 gzip

### 3. Native Image 优化

- 移除未使用的类
- 优化反射配置
- 使用 Profile-guided optimization

## 性能监控

### Spring Boot Actuator

Spring Boot Service 提供了多个监控端点：

```bash
# 健康检查
curl http://localhost:8080/api/health

# 指标（需要启用）
curl http://localhost:8080/actuator/metrics

# 线程信息
curl http://localhost:8080/actuator/threaddump
```

### 自定义监控

- 添加 Micrometer metrics
- 集成 Prometheus
- 设置 Grafana 仪表板

## 故障排除

### 性能问题诊断

1. **Guardrail 评估慢**
   - 检查规则数量
   - 优化正则表达式
   - 使用缓存

2. **API 响应慢**
   - 检查数据库连接
   - 查看慢查询日志
   - 启用 SQL 性能分析

3. **内存占用高**
   - 检查缓存配置
   - 分析堆转储
   - 优化对象创建

## 持续监控

建议在生产环境中：

1. 设置性能告警阈值
2. 定期运行性能测试
3. 监控关键指标趋势
4. 记录性能基准数据
