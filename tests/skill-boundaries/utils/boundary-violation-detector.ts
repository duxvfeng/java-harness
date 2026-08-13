/**
 * 边界违规检测器
 *
 * 负责检测和报告技能边界违规
 */

import { Operation } from './operation-monitor';

export interface SkillBoundary {
  allowedOperations: string[];
  forbiddenOperations: string[];
  allowedFilePatterns: string[];
  forbiddenFilePatterns: string[];
}

export interface Violation {
  detected: boolean;
  type: string;
  severity: 'critical' | 'major' | 'minor';
  skill: string;
  operation: Operation;
  message: string;
}

export interface ViolationReport {
  totalViolations: number;
  criticalViolations: number;
  majorViolations: number;
  minorViolations: number;
  violations: Violation[];
  recommendations: string[];
}

export class BoundaryViolationDetector {
  private skillBoundaries: Map<string, SkillBoundary> = new Map();

  constructor() {
    this.loadSkillBoundaries();
  }

  async detectSkillViolation(params: {
    skill: string;
    operation: Operation;
  }): Promise<Violation> {
    const { skill, operation } = params;
    const boundary = this.skillBoundaries.get(skill);

    if (!boundary) {
      return {
        detected: false,
        type: 'unknown',
        severity: 'minor',
        skill,
        operation,
        message: `No boundary definition found for skill: ${skill}`
      };
    }

    // 检查是否为禁止操作
    if (this.isForbiddenOperation(operation, boundary)) {
      return {
        detected: true,
        type: this.getViolationType(operation),
        severity: 'critical',
        skill,
        operation,
        message: `${skill} violated boundary by performing forbidden operation: ${this.describeOperation(operation)}`
      };
    }

    // 检查是否操作了禁止文件
    if (this.operatesOnForbiddenFile(operation, boundary)) {
      return {
        detected: true,
        type: 'file_access_violation',
        severity: 'critical',
        skill,
        operation,
        message: `${skill} accessed forbidden file: ${operation.file || operation.target}`
      };
    }

    return {
      detected: false,
      type: 'none',
      severity: 'minor',
      skill,
      operation,
      message: 'No violation detected'
    };
  }

  async scanSession(operations: Array<{
    skill: string;
    operation: Operation;
  }>): Promise<Violation[]> {
    const violations: Violation[] = [];

    for (const { skill, operation } of operations) {
      const violation = await this.detectSkillViolation({ skill, operation });
      if (violation.detected) {
        violations.push(violation);
      }
    }

    return violations;
  }

  generateReport(violations: Violation[]): ViolationReport {
    const criticalViolations = violations.filter(v => v.severity === 'critical').length;
    const majorViolations = violations.filter(v => v.severity === 'major').length;
    const minorViolations = violations.filter(v => v.severity === 'minor').length;

    const recommendations = this.generateRecommendations(violations);

    return {
      totalViolations: violations.length,
      criticalViolations,
      majorViolations,
      minorViolations,
      violations,
      recommendations
    };
  }

  private loadSkillBoundaries(): void {
    // harness-plan 边界
    this.skillBoundaries.set('harness-plan', {
      allowedOperations: [
        'read_plans',
        'write_plans',
        'read_spec',
        'write_spec',
        'websearch',
        'brainstorming'
      ],
      forbiddenOperations: [
        'modify_source_code',
        'execute_build_commands',
        'deep_git_analysis',
        'modify_config_files'
      ],
      allowedFilePatterns: [
        'Plans.md',
        'spec.md',
        'docs/**/*.md',
        'CLAUDE.md',
        'README.md'
      ],
      forbiddenFilePatterns: [
        'src/**',
        'app/**',
        'lib/**',
        'pkg/**',
        '.env*',
        'config/**',
        'pom.xml',
        'package.json'
      ]
    });

    // harness-work 边界
    this.skillBoundaries.set('harness-work', {
      allowedOperations: [
        'modify_source_code',
        'execute_build_commands',
        'deep_git_analysis',
        'update_task_status'
      ],
      forbiddenOperations: [
        'modify_task_definition',
        'change_task_priority',
        'modify_product_spec',
        'add_new_tasks'
      ],
      allowedFilePatterns: [
        'src/**',
        'app/**',
        'lib/**',
        'pkg/**',
        '.env*',
        'config/**',
        'pom.xml',
        'package.json',
        'Plans.md' // 仅限状态更新
      ],
      forbiddenFilePatterns: [
        'spec.md' // 禁止修改产品规格
      ]
    });

    // harness-sync 边界
    this.skillBoundaries.set('harness-sync', {
      allowedOperations: [
        'deep_git_analysis',
        'update_task_status',
        'analyze_implement_state'
      ],
      forbiddenOperations: [
        'modify_task_definition',
        'modify_source_code',
        'execute_build_commands'
      ],
      allowedFilePatterns: [
        'Plans.md' // 仅限状态更新
      ],
      forbiddenFilePatterns: [
        'src/**',
        'spec.md'
      ]
    });
  }

  private isForbiddenOperation(operation: Operation, boundary: SkillBoundary): boolean {
    const operationType = this.classifyOperation(operation);
    return boundary.forbiddenOperations.includes(operationType);
  }

  private operatesOnForbiddenFile(operation: Operation, boundary: SkillBoundary): boolean {
    if (!operation.file && !operation.target) {
      return false;
    }

    const filePath = operation.file || operation.target || '';

    for (const pattern of boundary.forbiddenFilePatterns) {
      if (this.matchPattern(filePath, pattern)) {
        return true;
      }
    }

    return false;
  }

  private classifyOperation(operation: Operation): string {
    if (operation.type === 'Write' || operation.type === 'Edit') {
      if (this.isSourceFile(operation.file || '')) {
        return 'modify_source_code';
      }
      if (operation.file === 'Plans.md') {
        return 'update_task_status';
      }
    }

    if (operation.type === 'Bash' && operation.command) {
      if (this.isBuildCommandString(operation.command)) {
        return 'execute_build_commands';
      }
      if (this.isGitAnalysisCommand(operation.command)) {
        return 'deep_git_analysis';
      }
    }

    return 'unknown';
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
      /^gradlew?\s+(build|assemble|compile)$/
    ];

    return buildPatterns.some(pattern => pattern.test(command));
  }

  private isGitAnalysisCommand(command: string): boolean {
    const analysisPatterns = [
      /^git\s+status/,
      /^git\s+log\s+(-\w+.*\s+.+)$/,
      /^git\s+diff\s+--stat/
    ];

    return analysisPatterns.some(pattern => pattern.test(command));
  }

  private matchPattern(filePath: string, pattern: string): boolean {
    // 简化的 glob 匹配
    const regexPattern = pattern
      .replace(/\*/g, '.*')
      .replace(/\?/g, '.');

    const regex = new RegExp(regexPattern);
    return regex.test(filePath);
  }

  private getViolationType(operation: Operation): string {
    if (this.isSourceFile(operation.file || '')) {
      return 'code_modification';
    }
    if (this.isBuildCommandString(operation.command || '')) {
      return 'build_execution';
    }
    if (this.isGitAnalysisCommand(operation.command || '')) {
      return 'git_analysis';
    }
    return 'unknown';
  }

  private describeOperation(operation: Operation): string {
    if (operation.type === 'Bash') {
      return `executed command: ${operation.command}`;
    }
    if (operation.type === 'Write' || operation.type === 'Edit') {
      return `modified file: ${operation.file}`;
    }
    return `performed operation: ${operation.type}`;
  }

  private generateRecommendations(violations: Violation[]): string[] {
    const recommendations: string[] = [];

    // 按技能分组违规
    const violationsBySkill = new Map<string, Violation[]>();
    for (const violation of violations) {
      const skillViolations = violationsBySkill.get(violation.skill) || [];
      skillViolations.push(violation);
      violationsBySkill.set(violation.skill, skillViolations);
    }

    // 为每个技能生成建议
    for (const [skill, skillViolations] of violationsBySkill) {
      if (skillViolations.length > 0) {
        recommendations.push(`Review ${skill} boundary violations (${skillViolations.length} found)`);
        recommendations.push(`Update ${skill} SKILL.md to clarify boundaries`);
        recommendations.push(`Add boundary enforcement for ${skill}`);
      }
    }

    // 通用建议
    if (violations.some(v => v.severity === 'critical')) {
      recommendations.push('CRITICAL: Address boundary violations immediately');
      recommendations.push('Consider re-running with boundary enforcement enabled');
    }

    return recommendations;
  }
}