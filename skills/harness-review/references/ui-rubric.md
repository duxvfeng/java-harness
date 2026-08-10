# UI Rubric Reviewer Profile

`harness-review --ui-rubric` 启动的，专用于外观质量的审查 profile。
不将 UI 的完成度以"感觉"结束，而是用 4 轴 0-10 评分来判定。

---

## 4 轴的思考方式

### 1. Design Quality

- 看什么: 信息整理、空白、视线引导、易读性
- 容易得低分的例子: 文字太挤、要素优先级不传达
- 容易得高分的例子: 自然传达应该看什么

### 2. Originality

- 看什么: 既视感少、有意图的个性、表现选择方式
- 容易得低分的例子: 照搬随处可见的定型布局
- 容易得高分的例子: 有符合品牌或课题的独特展示方式

### 3. Craft

- 看什么: 细部的精致、对齐、间隔、排版、状态变化
- 容易得低分的例子: 微妙的偏移、不一致的空白、粗糙的 hover / active
- 容易得高分的例子: 细部也一致、粗糙感少

### 4. Functionality

- 看什么: 能否毫不犹豫地使用、主要导线是否通畅、UI 是否实用
- 容易得低分的例子: 按钮和表单的意图难懂、主要导线中断
- 容易得高分的例子: 用户不迷茫下一步做什么

---

## 锚点例（0 / 5 / 10）

| 轴 | 0 点 | 5 点 | 10 点 |
|---|---|---|---|
| Design Quality | 不知道要展示什么、难读 | 最低限度可读但整理弱 | 信息优先级和视线引导明确 |
| Originality | 看起来像既制模板 | 一部分有工夫但印象弱 | 有符合课题的个性，留下印象 |
| Craft | 对齐和空白乱、细部粗糙 | 没有大的破绽但充实不够 | 空白、文字、状态变化都精致整理 |
| Functionality | 主要导线难懂、难用 | 主要操作可做但有迷茫场面 | 主要导线自然、不迷茫操作 |

---

## 判定方法

1. 4 轴分别用 0-10 评分
2. 如果有 `review.rubric_target`，则将其值作为各轴的阈值
3. 如果没有 `review.rubric_target`，则所有 4 轴使用 default threshold=6
4. 1 轴未达阈值也 `REQUEST_CHANGES`
5. 所有轴达阈值则 `APPROVE`

### `rubric_target` 的例子

```json
{
  "design": 7,
  "originality": 6,
  "craft": 8,
  "functionality": 9
}
```

---

## 输出方法

- `reviewer_profile` 必须设为 `"ui-rubric"`
- 在 `observations` 中用非专家也能理解的中文写出降分的理由
- 每轴至少附上"修正哪里能提分"

### 输出例子

```json
{
  "reviewer_profile": "ui-rubric",
  "verdict": "REQUEST_CHANGES",
  "ui_rubric": {
    "scores": {
      "design": 7,
      "originality": 5,
      "craft": 8,
      "functionality": 8
    },
    "targets": {
      "design": 6,
      "originality": 6,
      "craft": 6,
      "functionality": 6
    }
  }
}
```

---

## 判定的注意点

- 不仅因华丽而给高分
- 不因"罕见"而过度提高 Originality
- 如果可用性受损，优先严格看待 Functionality
- 不按设计偏好，而是按**意图和完成度**判断
