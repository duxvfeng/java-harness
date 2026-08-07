# java-harness Platform Setup

此插件需要根据您的操作系统配置正确的 hooks 文件。

## 快速配置

安装完成后，运行以下命令自动配置：

```bash
# 检测平台并自动配置
python3 -c "import platform, os, shutil; system = platform.system(); machine = platform.machine(); target = 'hooks/windows.json' if system == 'Windows' else f'hooks/{\"linux\" if system == \"Linux\" else \"macos\"}-{\"amd64\" if machine in [\"x86_64\", \"AMD64\"] else \"arm64\"}.json'; shutil.copy(target, 'hooks/hooks.json') if os.path.exists(target) else print(f'Platform not supported: {system} {machine}')"
```

## 手动配置

如果自动配置失败，请手动选择对应平台的配置文件：

### Windows
```bash
cp hooks/hooks.windows.json hooks/hooks.json
```

### Linux (AMD64/Intel)
```bash
cp hooks/hooks.linux-amd64.json hooks/hooks.json
```

### Linux (ARM64)
```bash
cp hooks/hooks.linux-arm64.json hooks/hooks.json
```

### macOS (Intel)
```bash
cp hooks/hooks.macos-amd64.json hooks/hooks.json
```

### macOS (Apple Silicon)
```bash
cp hooks/hooks.macos-arm64.json hooks/hooks.json
```

## 重新加载插件

配置完成后，重新加载插件：
```bash
/plugin reload
```

## 验证

```bash
# 检查 hooks 配置
cat hooks/hooks.json | grep -E "windows|linux|macos"
```
