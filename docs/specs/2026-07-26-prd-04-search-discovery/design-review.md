# 技术方案 Review：PRD-04 搜索发现

> Review 日期：2026-07-26
> Review 循环：第 2 轮
> 审查者：AI Agent

## 审查结果总览

### Shared 设计 (design.md)

| 维度 | 检查项数 | 通过 | 问题 | 已修复 |
|------|---------|------|------|--------|
| 与 Spec 一致性 | 7 | 7 | 0 | 0 |
| 功能完整性 | 4 | 4 | 0 | 0 |
| API 完整性 | 5 | 5 | 0 | 0 |
| 数据模型一致性 | 4 | 4 | 0 | 0 |
| 边界与错误处理 | 7 | 7 | 0 | 0 |
| 安全考虑 | 4 | 4 | 0 | 0 |
| 性能考虑 | 3 | 3 | 0 | 0 |

### 平台设计 (design-{platform}.md)

| 平台 | 与 Spec 一致性 | 功能完整性 | 架构 | 文件变更 | API 调用 | 状态管理 | 测试策略 | 总体 |
|------|--------------|----------|------|---------|---------|---------|---------|------|
| Backend | ✅ | ✅ | ✅ | ❌ | ✅ | ✅ | ✅ | ❌ |
| iOS | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| Android | ✅ | ✅ | ✅ | ❌ | ❌ | ❌ | ✅ | ❌ |
| Web | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A |

## 发现的问题

### Shared 设计问题

本轮未发现 shared design 阻塞或关注问题。`design.md` 与 `spec.md` 在功能范围、API 契约、数据模型、边界与性能/安全约束上整体一致。

### 平台设计问题

#### 问题 backend-1：扩展 Repository 接口后遗漏 Supabase 实现与测试变更，文件变更清单不完整

- **严重程度**：🔴 阻塞
- **平台**：backend
- **维度**：文件变更
- **描述**：`design-backend.md` 计划在 `backend/src/repositories/interfaces/drama.repository.interface.ts` 中新增 `search(...)` 与 `listHotSearches()`，但「核心文件变更」没有纳入现存的 `backend/src/repositories/supabase/drama.supabase.repository.ts` 及其测试文件。当前仓库中 `DramaSupabaseRepository` 已实现 `DramaRepositoryInterface`；一旦接口扩展而 Supabase 实现未同步补齐，TypeScript 编译会直接失败，且仓内两套 repository 能力将发生结构性失配。
- **修复状态**：✅ 已修复
- **修复说明**：已确认 `design-backend.md` 的核心文件变更、Repository 一致性约束与测试落地要求已对齐真实代码结构，明确覆盖 `DramaService`、`DramaRepositoryInterface`、`DramaSupabaseRepository`，以及 mock/supabase 两套测试路径；其中 Supabase 测试路径已与仓库实际目录保持一致。

#### 问题 android-1：Android 方案未闭合 API base URL 与 canonical `/api/dramas/*` 路径的对齐方案

- **严重程度**：🔴 阻塞
- **平台**：android
- **维度**：API 调用
- **描述**：shared design 明确新增接口为 `GET /api/dramas/search` 与 `GET /api/dramas/hot-search`，而 Android 方案只在 `ApiService` 中新增 `@GET("dramas/search")` / `@GET("dramas/hot-search")`，未说明如何保证 base URL 最终拼出 canonical `/api/dramas/*` 路径。当前仓库默认 Android 配置位于 `android/app/build.gradle.kts`，其 `api.base.url` 回退值仍是 `http://10.0.2.2:3000/api/v1`；若不在方案中明确修正配置或 endpoint 拼接策略，新搜索接口在默认开发环境下将命中错误地址，无法满足 spec 中“开发环境可用”的目标。
- **修复状态**：✅ 已修复
- **修复说明**：已在 `design-android.md` 中补充 base URL 与 endpoint 的对齐方案：保持 `ApiService` 相对路径写法，同时将 `AppConfig.apiBaseUrl` / `android/app/build.gradle.kts` 的默认回退值校准到 canonical `/api/` 前缀，并将对应文件修改与测试纳入方案范围。

#### 问题 android-2：Android 方案缺少后端错误包体解析设计，无法真正落地错误码映射与一致的失败态

- **严重程度**：🟡 关注
- **平台**：android
- **维度**：状态管理
- **描述**：`design-android.md` 在错误映射表中声明要区分 `VALIDATION_ERROR` / `INTERNAL_ERROR` / `NETWORK_ERROR`，但网络层设计没有给出如何把后端 `{ error: { code, message } }` 解析为 `ApiResult.Error`。当前仓库已有 `ErrorDto` 和 `ApiResult.Error`，但现存 `DramaRemoteDataSource` 仍是对异常统一返回 `ApiResult.Exception`。如果搜索方案沿用该模式，结果页与热搜区只能拿到泛化异常，无法实现方案里承诺的错误码级提示、局部错误归类和跨端一致的失败语义。
- **修复状态**：✅ 已修复
- **修复说明**：已在 `design-android.md` 中补充 Android 搜索网络层的错误解析链路：新增 error envelope DTO，约定将后端 `{ error: { code, message } }` 解析为 `ApiResult.Error`，并要求 `SearchRemoteDataSourceTest` 覆盖 400/500 错误包体与异常兜底场景。

## 跨端一致性检查

| 检查项 | 状态 | 说明 |
|--------|------|------|
| API 调用与 Shared 设计一致 | ✅ | iOS、Backend 与 Android 均已对齐共享设计；Android 已明确 canonical `/api/dramas/*` 的 base URL 校准方案。 |
| 数据模型各端一致 | ✅ | 搜索结果继续复用 `Drama` canonical schema；热搜与本地历史模型约束整体一致。 |
| 共享逻辑覆盖 | ✅ | 首页入口、搜索发现页/结果页、快捷入口、历史写入时机、结果卡片动作语义均已覆盖。 |
| 错误处理策略一致 | ✅ | iOS 与 Android 均已对齐 `{ error: { code, message } }` 包体解析和错误态承接设计。 |
| 真实代码落地清单一致性 | ✅ | Backend 文档已与真实代码结构对齐，Android 两项问题亦已完成收敛。 |

## 上一轮问题修复验证

- `android-1`：✅ 已验证。`design-android.md` 已明确要求将 Android `api.base.url` 默认回退值从旧的 `/api/v1` 校准到 canonical `/api/` 前缀，并保持 `ApiService` 相对路径写法，使搜索接口最终命中 `GET /api/dramas/search` 与 `GET /api/dramas/hot-search`。
- `android-2`：✅ 已验证。`design-android.md` 已补充 `{ error: { code, message } } -> ApiResult.Error` 的解析链路、错误 DTO 约束与 `SearchRemoteDataSourceTest` 的 400/500 覆盖要求。
- `backend-1`：✅ 已验证。`design-backend.md` 已与真实 backend 代码结构对齐，明确纳入 `DramaService`、`DramaRepositoryInterface`、`DramaSupabaseRepository`，以及 mock/supabase 两套 repository 测试路径，Supabase 测试路径也已修正为真实仓库目录。

## 遗留问题（需人工决策）

本轮无必须由人工拍板的架构分歧；均属于设计文档可直接补齐的问题。

## 结论

- [x] ✅ 所有问题已修复，可进入下一阶段（design-human-review）
- [ ] ⚠️ 仍有遗留问题，需要先完成下一轮复核后再推进

结论说明：

1. 本轮复核后，首轮发现的 **3** 条问题均已完成收敛，其中 Android 2 条问题与 Backend 1 条问题都已完成验证。
2. Shared 与各端方案当前已在 API、错误处理、真实文件清单与跨端约束上形成闭环。
3. 当前 **可进入 `design-human-review`**。
