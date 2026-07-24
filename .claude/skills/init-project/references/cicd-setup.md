# CI/CD 流水线设置

## GitHub Actions 配置

使用 `.github/workflows/ci.yml`，按变更路径选择性触发：

```yaml
name: CI
on:
  pull_request:
    branches: [master]
```

### 平台 Job

| Job | 触发条件 | 执行内容 |
|-----|---------|---------|
| Web | `web/**` 变更 | `npm ci` → `npm run lint` → `npm run typecheck` → `npm run build` |
| Backend | `backend/**` 变更 | `npm ci` → `npm run lint` → `npm run typecheck` → `npm run build` |
| Android | `android/**` 变更 | `./gradlew detekt` → `./gradlew assembleDebug` |
| iOS | `ios/**` 变更 | `xcodegen generate` + `swiftlint lint`（标记 `continue-on-error: true`） |

### Path Filter 策略

| 变更路径 | 行为 |
|---------|------|
| `web/**` 或 `backend/**` | 触发对应 TypeScript job |
| `android/**` | 触发 Android job |
| `ios/**` | 触发 iOS job（optional） |
| `docs/**`、`wiki/**`、`*.md` | 跳过所有 CI |
| 根目录文件 | 全量 CI |

### 约束

- iOS job 标记 `continue-on-error: true`（GitHub Actions 的 ubuntu runner 无 macOS 环境，不阻塞合并）
- 所有 job 设置合理的 timeout（10min）
- `npm ci` 使用 lockfile，确保依赖版本一致
