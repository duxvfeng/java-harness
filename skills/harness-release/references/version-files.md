# Version File Detection & Update

此技能处理的 4 类 version file 的检测与替换详细。

## 优先级

```
VERSION  >  package.json  >  pyproject.toml  >  Cargo.toml
```

项目中存在多个时，以优先级高的为正本。
通常假设仅存在其中 1 个。

## 检测与读取

### VERSION (单独文件)

```bash
cat VERSION | tr -d '\n'
```

仅 1 行，语义版本 (`x.y.z`)。

### package.json (npm)

```python
import json
with open("package.json") as f:
    data = json.load(f)
current_version = data["version"]
```

顶层 `"version": "x.y.z"`。

### pyproject.toml (Python)

支持 PEP 621 (`[project]`) 和 Poetry (`[tool.poetry]`) 双方:

```python
import tomllib
with open("pyproject.toml", "rb") as f:
    data = tomllib.load(f)

if "project" in data and "version" in data["project"]:
    current_version = data["project"]["version"]
elif "tool" in data and "poetry" in data["tool"]:
    current_version = data["tool"]["poetry"]["version"]
else:
    raise RuntimeError("pyproject.toml 中未找到 version")
```

**注意**: `pyproject.toml` 中有 `dynamic = ["version"]` 等从别的文件 (`_version.py` 等) 读取 version 的设置。此时技能不支持（请事先切换到 static version，或兼用 `VERSION` 文件）。

### Cargo.toml (Rust)

```python
import tomllib
with open("Cargo.toml", "rb") as f:
    data = tomllib.load(f)
current_version = data["package"]["version"]
```

## 替换

替换以"最小限度字段替换"进行。为不破坏格式样式和注释，推荐 regex 替换:

### VERSION

```bash
echo "$NEW_VERSION" > VERSION
```

### package.json

有 `jq` 时:
```bash
jq --arg v "$NEW_VERSION" '.version = $v' package.json > /tmp/package.json && mv /tmp/package.json package.json
```

无 `jq` 时用 Python:
```python
import json
with open("package.json", "r") as f:
    data = json.load(f)
data["version"] = NEW_VERSION
with open("package.json", "w") as f:
    json.dump(data, f, indent=2)
    f.write("\n")
```

### pyproject.toml / Cargo.toml

TOML 不想破坏替换样式，用 regex 仅替换第一个 `version = "..."` 行:

```python
import re
with open("pyproject.toml", "r") as f:
    content = f.read()

# 替换 [project] 或 [tool.poetry] 章节内的 version
section_pattern = None
if re.search(r"^\[project\]", content, re.M):
    section_pattern = r"(\[project\][^\[]*?version\s*=\s*\")[^\"]+(\")"
elif re.search(r"^\[tool\.poetry\]", content, re.M):
    section_pattern = r"(\[tool\.poetry\][^\[]*?version\s*=\s*\")[^\"]+(\")"

new_content = re.sub(
    section_pattern,
    rf"\g<1>{NEW_VERSION}\g<2>",
    content,
    count=1,
    flags=re.S,
)
with open("pyproject.toml", "w") as f:
    f.write(new_content)
```

Cargo.toml 同样 (`[package]` 章节内):

```python
section_pattern = r"(\[package\][^\[]*?version\s*=\s*\")[^\"]+(\")"
```

## 子包的处理

monorepo 中存在多个 version file 的案例（例: npm workspaces）不在本技能对象范围。
设计为仅根目录的 1 文件为正本。
想同步多个 package 时，请另外构建专用的 release 协调器。

## 不支持的 version 表现

以下不支持。请事先正规化为 SemVer 格式:

- `v1.0.0` (不允许开头的 `v`，仅 tag 有 `v` 前缀)
- `1.0.0-alpha.1` (保持 pre-release suffix 但不作为 bump 对象)
- `1.0.0+build.1` (保持 build metadata)
- Calendar versioning (`2024.01`)
