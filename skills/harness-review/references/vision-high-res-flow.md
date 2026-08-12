# Vision High-Res Flow (Opus 4.7)

在 harness-review 中充分利用 Opus 4.7 的高分辨率 vision 功能（短边最大 2576px）的典型场景流程。

> **分辨率上限**: 短边 2576px 为运营安全上限。超过此限制的图像建议预先调整大小。
> 详细指南请参考 [`docs/harness-project/opus-4-7-vision-usage.md`](../../../docs/harness-project/opus-4-7-vision-usage.md)。

---

## 场景 1: PDF 页面审查

审查规格说明书、设计文档、发行说明等 PDF 的情况。

### 流程

1. **确定页面范围**

     由于一次性传递整个 PDF 会消耗大量 token，首先掌握页面构成。

     ```
     Read tool: file_path="<path>.pdf", pages="1-5"
     ```

2. **确认每页的实际 DPI**

     PDF 的 DPI 较高时，渲染后短边可能超过 2576px。
     超过时请求降低 DPI 重新导出（详情请参考 usage 指南）。

3. **用 Read 读取审查对象页面**

     ```
     Read tool: file_path="<path>.pdf", pages="<对象页面范围>"
     ```

     Read tool 将 pages 参数指定的页面传递给 vision 模型。
     每次最多可指定 20 页。

4. **传递给 Reviewer agent**

     将读取的页面内容流入 harness-review 的审查流程（Step 2: 5 观点）。
     Reviewer 评估视觉布局、图表、代码片段。

5. **批处理（页面数多时）**

     超过 20 页的 PDF 以 20 页为单位进行批次分割。

     ```
     pages="1-20"  → 审查 → 记录指出
     pages="21-40" → 审查 → 记录指出
     ...
     最后整合 verdict
     ```

### 判定标准

PDF 审查将 reviewer_profile 视为 `static`，评估以下内容：

| 观点 | 检查内容 |
|------|------------|
| **Quality** | 图表说明是否充分、步骤前后关系是否明确 |
| **Accessibility** | 是否存在仅有图像无替代文本的页面 |
| **AI Residuals** | "TODO", "TBD", "Draft" 等未完成标记 |

---

## 场景 2: 设计图（Architecture Diagram）审查

审查系统构成图、ER 图、序列图等图像的情况。

### 流程

1. **确认图像分辨率**

     ```bash
     # macOS: 用 sips 确认分辨率
     sips -g pixelWidth -g pixelHeight diagram.png

     # 有 ImageMagick 的情况
     identify diagram.png
     ```

     短边在 2576px 以下可直接用 Read tool 传递。
     超过时预先调整大小（详情请参考 usage 指南）。

2. **用 Read tool 读取图像**

     ```
     Read tool: file_path="diagram.png"
     ```

     Opus 4.7 可视认最大 2576px，因此可解析细小的标签和箭头。

3. **准备传递给 Reviewer agent 的上下文**

     ```
     请审查以下架构图。
     对象: <系统名> 的 <图类型（构成图 / ER 图 / 序列图 等）>
     确认观点: <审查目的（整合性确认 / 变更差异确认 / 安全确认 等）>
     ```

4. **评估项目**

     | 观点 | 检查内容 |
     |------|------------|
     | **Security** | 图中是否反映认证流程、授权边界、加密要求 |
     | **Quality** | 组件间依赖关系是否明确、是否保持单一责任 |
     | **Performance** | 是否可视化易成为瓶颈的部位（同步处理 / N+1 / 无缓存等） |

5. **与实现代码的对照**

     设计图审查后，与对应实现代码通过 Code Review 流程对照确认整合性。

---

## 场景 3: UI 截图审查

用 `--ui-rubric` 选项对 Web / 移动 UI 截图进行评分的情况。

### 流程

1. **准备截图**

     获取对象页面/组件的截图。
     在 Retina / HiDPI 环境中通常成为逻辑像素的 2 倍尺寸。

     ```bash
     # macOS: screencapture 命令
     screencapture -x screenshot.png

     # 确认分辨率
     sips -g pixelWidth -g pixelHeight screenshot.png
     ```

2. **确认分辨率并调整大小（必要时）**

     短边超过 2576px 时调整大小（详情请参考 usage 指南）。
     2576px 以下可直接用 Read tool 传递。

3. **用 harness-review --ui-rubric 评估**

     ```
     /harness-review --ui-rubric
     ```

     执行前用 Read tool 读取截图，传递给 Reviewer agent：

     ```
     Read tool: file_path="screenshot.png"
     ```

4. **4 轴评分（参考 ui-rubric.md）**

     | 轴 | 评估内容 |
     |----|---------|
     | **Design Quality** | 视觉层次、留白、颜色整合性 |
     | **Originality** | 独创性、品牌表现 |
     | **Craft** | 像素精度、动画、微交互 |
     | **Functionality** | 用户流程的完整性、错误状态的考虑 |

5. **多分辨率比较（移动 / 平板 / 桌面）**

     在同一会话连续读取各分辨率截图，
     让 Reviewer agent 评估响应式支持。

     ```
     Read tool: file_path="mobile.png"    # 375×812 相当
     Read tool: file_path="tablet.png"    # 768×1024 相当
     Read tool: file_path="desktop.png"   # 1440×900 相当
     ```

---

## 与 Reviewer Agent 的连接方式

上述 3 个场景中，用 Read tool 读取图像 / PDF 后，
与 Reviewer agent 的连接均通过以下通用模式进行。

### breezing 模式的连接方式

Lead 从 Worker 接收到包含 vision 输入的任务时：

1. Worker 在 `files_changed` 中包含图像/PDF 路径返回
2. Lead 用 Read tool 读取该路径，添加 vision 上下文执行审查
3. Reviewer agent 以 `review-result.v1` 架构返回 verdict

```json
// 传递给 Reviewer 的附加上下文示例
{
  "vision_inputs": [
    { "type": "image", "path": "diagram.png", "role": "architecture_diagram" },
    { "type": "pdf",  "path": "spec.pdf",    "role": "specification", "pages": "1-10" }
  ],
  "review_context": "包含图像・PDF 的变更审查"
}
```

### 接收图像输入时 Reviewer 的行为

- Reviewer 将图像输入视为与"普通 diff 文本"等同，返回 `review-result.v1`
- `observations[].location` 中记载为 `"diagram.png:整体"` / `"spec.pdf:p3"` 等
- 仅凭图像无法判定 critical / major 时止步于 `minor` 或 `recommendation`
- 判定标准（critical / major / minor / recommendation）不因 vision 输入有无而变化

---

## 批处理指南

连续审查多个图像 / PDF 页面时：

| 情况 | 推荐方法 |
|------|--------------|
| PDF 20 页以下 | 1 次 Read 指定全部页面 |
| PDF 21 页以上 | 以 20 页为单位批次分割 → 整合指出 |
| 图像 1〜5 张 | 连续 Read → 一并审查 |
| 图像 6 张以上 | 以 5 张为单位批次 → 最后整合 verdict |
| 混有高分辨率图像 | 处理前预先调整大小（参考 usage 指南） |

批处理中累积各批次的 `observations`，
全批次完成后根据 `critical` / `major` 的有无决定最终 verdict。
