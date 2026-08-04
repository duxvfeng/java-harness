# Post-Gate Mechanics: PR/Main Merge, Plugin Tag, Verify Publish

主 `SKILL.md` 仅总结的三个 Post-Gate 步骤的完整命令序列：将 release PR 合并到默认分支、创建 Claude plugin tag、验证 tag 触发的发布工作流程。

## PR / Main Merge Gate

Post-Gate 的 release commit 后，在创建 tag 之前将 GitHub PR merge 到 default branch。

```bash
release_branch="$(git branch --show-current)"
default_branch="${HARNESS_RELEASE_DEFAULT_BRANCH:-main}"

git push -u origin "$release_branch"
gh pr create --base "$default_branch" --head "$release_branch" --title "chore: release v<new>" --body "<release summary>"
gh pr merge --merge --delete-branch=false

git fetch origin "$default_branch" --tags
git checkout "$default_branch"
git pull --ff-only origin "$default_branch"
git merge-base --is-ancestor "<release-commit>" "origin/$default_branch"
```

现有 PR 时不新建，更新现有 PR 的 body 后 merge。repository policy 要求 squash merge 时，确认不是 release commit hash，而是 release bump 的内容（version files + CHANGELOG + source commits）包含在 default branch 中。

tag 在此 Gate 完成后，对 default branch 的 HEAD 或 release commit 可到达的 commit 创建。不要用仅在 release branch 上存在的 commit 指向的 tag 创建 GitHub Release。

## Claude plugin project 的 tag 创建

有 `.claude-plugin/plugin.json` 的 project，在 PR/main merge 后在 default branch 上再次确认 version sync 后创建 plugin tag:

```bash
HARNESS_PLUGIN_ROOT="${CLAUDE_PLUGIN_ROOT:-.}"
python3 "${HARNESS_PLUGIN_ROOT}/scripts/check-release-version-sync.py" --root .

claude plugin tag .claude-plugin --dry-run
claude plugin tag .claude-plugin --push --remote origin
```

`claude plugin tag` 创建的 tag 是 `{plugin-name}--v{version}` 格式。现有 GitHub Release workflow 以 `vX.Y.Z` tag 为前提的 project，除了 plugin tag 外还要创建 `git tag -a v<new>`。插件配布的 tag 交给 `claude plugin tag`，GitHub Release 用 semver tag 作为 release automation 的兼容 surface 处理。

## Verify Workflow Publish

Tag push 后，`.github/workflows/release.yml` 自动公开 release。skill 用以下 verify 结果:

```bash
OWNER="$(git remote get-url origin | sed 's|.*github.com[:/]\([^/]*/[^/]*\)\.git|\1|')"
bash scripts/release-verify-publish.sh "v${NEW_VERSION}" "${OWNER}"
```

超时: 5 秒间隔 × 60 次 = 最大 5 分钟 polling。

- exit 0: PASS — `draft=false` 且 assets 4 platform 齐全已公开
- exit 2: WARN — timeout (tag 已 push 所以不 abort，促使人工判断)
- exit 3: ERROR — API error (权限/认证问题，需要手动调查)

Verify 通过 `gh api` 进行。GitHub CLI 的 release subcommand prefix 在 CC runtime hard floor 被 deny，所以不使用。
