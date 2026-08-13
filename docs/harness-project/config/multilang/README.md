# 多语言代码规范说明

> 本目录**没有 TOML 配置文件**，因为多语言审查不是通过 `harness.toml` 的
> `[harness.multilang]` 段配置的（该段在 Java 中没有解析器）。

## 多语言规范如何工作

多语言代码审查由 **`harness-review` 技能路由** 实现：审查时根据文件扩展名 /
内容模式检测语言，再应用对应的代码标准。相关逻辑与文档位于技能目录：

- 技能入口：`skills/harness-review/SKILL.md`
- 语言标准参考：`skills/harness-review/references/code-standards/`
  - `java-alibaba-guide.md`（阿里巴巴 Java 开发手册 · 黄山版）
  - `python-pep8.md`
  - `vue-style-guide.md`
  - `go-effective-go.md`
  - `architecture.md`

项目根 `CLAUDE.md` 的 "Multilingual Code Standards Support" 章节也定义了
语言 → 标准 → 严重级别的映射表。

## 语言映射（参考）

| 语言 | 扩展名 | 标准来源 | 默认严重级别 |
|------|--------|----------|--------------|
| Java | `.java` | Alibaba 黄山版 | major |
| Python | `.py`, `.pyi` | PEP 8 + 最佳实践 | moderate |
| Vue | `.vue` | Vue Style Guide | moderate |
| Go | `.go` | Effective Go | major |

## 如何自定义

如需调整某语言的审查范围或标准，请编辑对应的标准参考文档，或修改
`skills/harness-review/SKILL.md` 中的路由描述——而不是新增一个
`[harness.multilang]` 配置段（当前无效）。
