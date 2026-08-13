/**
 * 操作监控器
 *
 * 负责监控技能执行过程中的操作，检测边界违规
 */

export interface Operation {
  type: 'Write' | 'Edit' | 'Bash' | 'Read' | 'Grep' | 'Glob';
  target?: string;
  command?: string;
  file?: string;
  timestamp: number;
}

export interface Violations {
  codeModifications: Operation[];
  buildCommands: Operation[];
  deepGitAnalysis: Operation[];
  taskDefinitionModifications: Operation[];
  planningDecisions: Operation[];
  total: number;
}

export class OperationMonitor {
  private operations: Operation[] = [];
  private monitoring: boolean = false;

  startMonitoring(): void {
    this.monitoring = true;
    this.operations = [];
  }

  stopMonitoring(): void {
    this.monitoring = false;
  }

  recordOperation(operation: Operation): void {
    if (this.monitoring) {
      operation.timestamp = Date.now();
      this.operations.push(operation);
    }
  }

  detectViolations(): Violations {
    const violations: Violations = {
      codeModifications: [],
      buildCommands: [],
      deepGitAnalysis: [],
      taskDefinitionModifications: [],
      planningDecisions: [],
      total: 0
    };

    for (const op of this.operations) {
      // 检测代码修改违规
      if (this.isCodeModification(op)) {
        violations.codeModifications.push(op);
      }

      // 检测构建命令违规
      if (this.isBuildCommand(op)) {
        violations.buildCommands.push(op);
      }

      // 检测深度 Git 分析违规
      if (this.isDeepGitAnalysis(op)) {
        violations.deepGitAnalysis.push(op);
      }

      // 检测任务定义修改违规
      if (this.isTaskDefinitionModification(op)) {
        violations.taskDefinitionModifications.push(op);
      }

      // 检测规划决策违规
      if (this.isPlanningDecision(op)) {
        violations.planningDecisions.push(op);
      }
    }

    violations.total =
      violations.codeModifications.length +
      violations.buildCommands.length +
      violations.deepGitAnalysis.length +
      violations.taskDefinitionModifications.length +
      violations.planningDecisions.length;

    return violations;
  }

  private isCodeModification(op: Operation): boolean {
    if (op.type === 'Write' || op.type === 'Edit') {
      if (op.file) {
        return this.isSourceFile(op.file);
      }
    }
    return false;
  }

  private isBuildCommand(op: Operation): boolean {
    if (op.type === 'Bash' && op.command) {
      return this.isBuildCommandString(op.command);
    }
    return false;
  }

  private isDeepGitAnalysis(op: Operation): boolean {
    if (op.type === 'Bash' && op.command) {
      return this.isGitAnalysisCommand(op.command);
    }
    return false;
  }

  private isTaskDefinitionModification(op: Operation): boolean {
    if (op.type === 'Edit' && op.file === 'Plans.md') {
      // 检查是否修改了 Task/内容/DoD 字段
      return this.isTaskFieldModification(op);
    }
    return false;
  }

  private isPlanningDecision(op: Operation): boolean {
    if (op.type === 'Edit' && op.file === 'spec.md') {
      return true;
    }
    return false;
  }

  private isSourceFile(filePath: string): boolean {
    const sourcePatterns = [
      /src\/.*\.(java|ts|js|py|go|rs)$/,
      /app\/.*\.(java|ts|js|py|go|rs)$/,
      /lib\/.*\.(java|ts|js|py|go|rs)$/,
      /pkg\/.*\.(java|ts|js|py|go|rs)$/
    ];

    return sourcePatterns.some(pattern => pattern.test(filePath));
  }

  private isBuildCommandString(command: string): boolean {
    const buildPatterns = [
      /^mvn\s+(compile|package|install|build)$/,
      /^npm\s+(run\s+build|build)$/,
      /^gradlew?\s+(build|assemble|compile)$/,
      /^python\s+(setup\.py|build\.py)$/,
      /^go\s+build$/,
      /^cargo\s+build$/
    ];

    return buildPatterns.some(pattern => pattern.test(command));
  }

  private isGitAnalysisCommand(command: string): boolean {
    const analysisPatterns = [
      /^git\s+status/,
      /^git\s+log\s+(-\w+.*\s+.+)$/,
      /^git\s+diff\s+--stat/,
      /^git\s+diff\s+HEAD~/
    ];

    return analysisPatterns.some(pattern => pattern.test(command));
  }

  private isTaskFieldModification(op: Operation): boolean {
    // 简化检测：假设 Edit 操作包含任务字段修改
    // 实际实现中需要更精确的内容分析
    return false;
  }

  getOperations(): Operation[] {
    return [...this.operations];
  }

  getMonitoringStats(): { total: number; byType: Record<string, number> } {
    const byType: Record<string, number> = {};

    for (const op of this.operations) {
      byType[op.type] = (byType[op.type] || 0) + 1;
    }

    return {
      total: this.operations.length,
      byType
    };
  }
}