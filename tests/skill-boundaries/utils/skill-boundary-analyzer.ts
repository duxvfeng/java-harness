/**
 * 技能边界分析器
 *
 * 负责分析技能文件中的边界定义和违规检测
 */

export interface BoundarySection {
  allowedOperations: string[];
  forbiddenOperations: string[];
  allowedFilePatterns: string[];
  forbiddenFilePatterns: string[];
  greyAreas: string[];
}

export class SkillBoundaryAnalyzer {
  private skillName: string;
  private skillContent: string;

  constructor(skillName: string, skillContent: string) {
    this.skillName = skillName;
    this.skillContent = skillContent;
  }

  static async load(skillName: string): Promise<SkillBoundaryAnalyzer> {
    // 模拟加载技能文件
    const skillContent = await this.loadSkillFile(skillName);
    return new SkillBoundaryAnalyzer(skillName, skillContent);
  }

  hasBoundarySection(): boolean {
    return this.skillContent.includes('## 技能边界定义') ||
           this.skillContent.includes('## 技能职责定义');
  }

  hasRoleDefinition(): boolean {
    return this.skillContent.includes('### 🎯 核心职责范围');
  }

  hasAllowedOperations(): boolean {
    return this.skillContent.includes('### ✅ 允许的操作');
  }

  hasForbiddenOperations(): boolean {
    return this.skillContent.includes('### ❌ 禁止的操作');
  }

  getAllowedOperations(): string[] {
    const allowedSection = this.extractSection('### ✅ 允许的操作', '### ❌ 禁止的操作');
    return this.extractListItems(allowedSection);
  }

  getForbiddenOperations(): string[] {
    const forbiddenSection = this.extractSection('### ❌ 禁止的操作', '### 🔵 灰色地带');
    return this.extractListItems(forbiddenSection);
  }

  getAllowedFilePatterns(): string[] {
    const patterns: string[] = [];
    const regex = /- ✅\s+(.*?\(.*?\))/g;
    let match;

    while ((match = regex.exec(this.skillContent)) !== null) {
      patterns.push(match[1]);
    }

    return patterns;
  }

  getForbiddenFilePatterns(): string[] {
    const patterns: string[] = [];
    const regex = /- ❌\s+(.*?\(.*?\))/g;
    let match;

    while ((match = regex.exec(this.skillContent)) !== null) {
      patterns.push(match[1]);
    }

    return patterns;
  }

  private extractSection(startMarker: string, endMarker: string): string {
    const startIndex = this.skillContent.indexOf(startMarker);
    const endIndex = this.skillContent.indexOf(endMarker, startIndex);

    if (startIndex === -1 || endIndex === -1) {
      return '';
    }

    return this.skillContent.substring(startIndex, endIndex);
  }

  private extractListItems(section: string): string[] {
    const items: string[] = [];
    const regex = /- [✅❌⚠️]\s+\*\*(.*?)\*\*/g;
    let match;

    while ((match = regex.exec(section)) !== null) {
      items.push(match[1]);
    }

    return items;
  }

  private static async loadSkillFile(skillName: string): Promise<string> {
    // 实际实现中从文件系统加载
    return `# ${skillName} content...`;
  }
}