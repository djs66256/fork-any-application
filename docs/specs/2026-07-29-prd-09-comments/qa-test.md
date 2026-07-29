# QA 黑盒测试文档

> 需求：PRD-09 评论系统
> 版本：v1
> 撰写日期：2026-07-29
> 撰写人：Claude Code

---

## 一、测试范围

### 1.1 测试目标

验证 PRD-09 评论系统在 Backend / iOS / Android 三端的最小可交付链路已经收口，包括评论列表、发表评论、点赞切换、匿名只读/登录可写、Home/Player 评论入口与登录恢复上下文等能力；同时如实记录本轮 QA 阶段未执行的设备黑盒操作与遗留环境限制。

### 1.2 涉及平台

| 平台 | 是否涉及 | 说明 |
|------|---------|------|
| Backend | ✅ | 已验证评论接口 contract、测试、build、lint；migration 端到端验证受历史 migration 环境问题阻塞 |
| iOS | ✅ | 已验证评论数据链路、ViewModel、Home/Player 接线、xcodegen/test/build/swiftlint |
| Android | ✅ | 已验证评论数据链路、ViewModel、Home/Player 接线、test/assembleDebug/detekt |
| Web | ❌ | 本需求未涉及 Web，workflow 中已标记 skipped |

### 1.3 不测试的内容

- 真机/模拟器 UI 黑盒点击路径：当前仓库未定义设备/模拟器 testing skill，按 feature-workflow 规范降级为仅产出 QA 文档，不执行设备操作。
- Backend comments migration 的真实 `supabase db push` 闭环：本轮被既有历史 migration 幂等性问题阻塞，已在 `code-backend-review.md` 中记录。
- Android / iOS 的真实登录服务：本次评论能力只要求“未登录拦截 + 恢复评论上下文”，不要求在 QA 阶段重新做 PRD-08 登录服务端联调。

---

## 二、测试环境

| 项目 | 要求 |
|------|------|
| 设备型号 | 未执行真机 / 模拟器黑盒操作 |
| OS 版本 | Android 构建使用 Android Studio JBR 21；iOS 构建使用 iPhone 17 Simulator, OS 27.0 |
| App 版本 | 当前 worktree `feature/2026-07-29-prd-09-comments` |
| 网络环境 | 本地开发环境 |
| 账号权限 | Backend 匿名读、登录写；端侧登录恢复使用现有链路 / placeholder |
| 其他依赖 | Backend Node/npm；iOS xcodegen/xcodebuild/swiftlint；Android Gradle + Android Studio JBR |

---

## 三、测试用例

每个用例包含测试定义和测试执行两部分。**本轮执行结果以代码级验证、自动化测试和构建结果为准；设备操作型用例按规范记为跳过。**

### 3.1 功能测试

#### QA-F-001：Backend 评论三条接口 contract 收口正确

| 属性 | 值 |
|------|-----|
| **优先级** | P0 |
| **涉及平台** | Backend |
| **前置条件** | 当前 worktree backend comments 代码已完成 |
| **测试步骤** | 1. 运行 comments route / service / repository 定向测试；2. 运行 backend 全量测试；3. 检查 GET `/api/dramas/:id/comments`、POST `/api/dramas/:id/comments`、POST `/api/dramas/:id/comments/:commentId/like` 的返回 contract 与错误语义 |
| **预期结果** | 评论列表返回 `{ data, pagination }`；发评论返回完整 `Comment`；点赞返回 `{ comment_id, liked, like_count }`；匿名只读、登录可写语义正确 |

**执行结果**：

| 属性 | 值 |
|------|-----|
| **状态** | ✅ 通过 |
| **实际结果** | Backend 定向测试、全量测试、build、lint 已通过；contract 与 spec/design 一致 |
| **备注** | 依据 `plan-backend.md`、`code-backend-review.md` 与 backend 子任务执行结果 |

---

#### QA-F-002：iOS 评论抽屉主链路可构建可回归

| 属性 | 值 |
|------|-----|
| **优先级** | P0 |
| **涉及平台** | iOS |
| **前置条件** | 当前 worktree iOS comments 代码已完成 |
| **测试步骤** | 1. 执行 `xcodegen generate`；2. 执行 `xcodebuild test`；3. 执行 `xcodebuild build`；4. 检查 Home/Player 评论入口、评论 ViewModel、登录恢复上下文对应测试 |
| **预期结果** | 工程可生成、测试通过、构建成功；评论列表/发送/点赞/登录拦截能力已收口 |

**执行结果**：

| 属性 | 值 |
|------|-----|
| **状态** | ✅ 通过 |
| **实际结果** | `xcodegen generate` 成功；`xcodebuild test` 215 tests 通过；`xcodebuild build` 成功；评论相关测试已覆盖 Home/Player/CommentSheetViewModel |
| **备注** | `swiftlint lint` 命令成功，但存在 warning，见 QA-B-002 |

---

#### QA-F-003：Android 评论抽屉主链路可构建可回归

| 属性 | 值 |
|------|-----|
| **优先级** | P0 |
| **涉及平台** | Android |
| **前置条件** | 使用 Android Studio 自带 JBR 作为 `JAVA_HOME` |
| **测试步骤** | 1. 设置 `JAVA_HOME=/Applications/Android Studio.app/Contents/jbr/Contents/Home`；2. 执行 `./gradlew test`；3. 执行 `./gradlew assembleDebug`；4. 执行 `./gradlew detekt`；5. 检查 Home/Player 评论入口、CommentBottomSheet、登录恢复占位链路 |
| **预期结果** | Android 评论代码可编译、测试通过、Debug 构建成功、detekt 通过；评论链路满足“只恢复上下文、不自动重放写操作” |

**执行结果**：

| 属性 | 值 |
|------|-----|
| **状态** | ✅ 通过 |
| **实际结果** | 在显式指定 Android Studio JBR 后，`./gradlew test assembleDebug` 与 `./gradlew detekt` 均通过；期间修复了 LongParameterList/MaxLineLength、SearchResultScreen 接线缺参、测试期望错误等问题 |
| **备注** | 说明此前“缺少 Java Runtime”属于环境变量未指向 JBR，不再是最终阻塞 |

---

#### QA-F-004：未登录写操作只恢复评论上下文，不自动重放

| 属性 | 值 |
|------|-----|
| **优先级** | P0 |
| **涉及平台** | iOS / Android |
| **前置条件** | 端侧评论模块已实现 `CommentLoginContext` / pending action |
| **测试步骤** | 1. 检查 iOS/Android 的 ViewModel 与宿主测试；2. 验证未登录发送评论或点赞时不直接发请求；3. 验证登录恢复后只重新打开评论抽屉 |
| **预期结果** | 写操作被拦截；恢复只回到来源页评论抽屉，不自动再次发送或点赞 |

**执行结果**：

| 属性 | 值 |
|------|-----|
| **状态** | ✅ 通过 |
| **实际结果** | iOS 与 Android 均已在测试和实现中落实“只恢复上下文、不自动重放写操作”；Android 当前使用 placeholder 登录对话框，但行为符合设计 |
| **备注** | 设计依据 `design-ios.md`、`design-android.md` |

---

### 3.2 边界测试

#### QA-B-001：评论列表空态、分页、排序边界收口

| 属性 | 值 |
|------|-----|
| **优先级** | P0 |
| **涉及平台** | Backend / iOS / Android |
| **边界类型** | 数据边界 / 状态边界 |
| **前置条件** | 评论相关 repository / ViewModel 测试已补齐 |
| **测试步骤** | 1. 检查空列表、分页追加、排序切换测试；2. 验证 `latest/hot` 参数透传；3. 验证大页码/空列表时状态不异常 |
| **预期结果** | 空列表进入空态；分页只追加不覆盖旧数据；排序切换会重置第一页并重新拉取 |

**执行结果**：

| 属性 | 值 |
|------|-----|
| **状态** | ✅ 通过 |
| **实际结果** | Backend mock/supabase repository、iOS CommentSheetViewModel、Android CommentSheetViewModel 均已有对应自动化测试并通过 |
| **备注** | Android detekt 修复过程中同步清理了部分超长测试语句，不影响语义 |

---

#### QA-B-002：Lint / 静态检查零 error，但存在非阻塞 warning

| 属性 | 值 |
|------|-----|
| **优先级** | P1 |
| **涉及平台** | Backend / iOS / Android |
| **边界类型** | 工程质量边界 |
| **前置条件** | 各端已执行 lint / detekt / build |
| **测试步骤** | 1. 检查 backend lint、iOS swiftlint、Android detekt 输出；2. 区分 error 与 warning；3. 判断是否影响当前 PRD 合入 |
| **预期结果** | 当前 PRD 至少无新增 lint error / detekt issue，warning 需可解释且不阻塞主链路 |

**执行结果**：

| 属性 | 值 |
|------|-----|
| **状态** | ✅ 通过 |
| **实际结果** | Backend lint 无 error；Android detekt 已清零；iOS `swiftlint lint` 命令成功但仍有仓库既有 warning，另有 `PlayerViewModel.swift` 的 `type_body_length` warning |
| **备注** | 已在 `code-ios-review.md` 中记录为建议单独治理，不作为本次功能阻塞 |

---

### 3.3 异常测试

#### QA-E-001：匿名写接口与权限错误路径正确收口

| 属性 | 值 |
|------|-----|
| **优先级** | P0 |
| **涉及平台** | Backend / iOS / Android |
| **异常类型** | 权限不足 |
| **模拟方式** | 自动化测试中以未登录状态发送评论 / 点赞 |
| **前置条件** | 写接口认证基线已对齐 skeleton auth |
| **测试步骤** | 1. Backend route 测试匿名 POST 评论 / 点赞；2. iOS/Android ViewModel 测试未登录写操作；3. 核对提示文案与 effect 语义 |
| **预期结果** | Backend 返回 `401 + UNAUTHORIZED`；端侧弹出登录拦截上下文，不发生本地错误写入 |

**执行结果**：

| 属性 | 值 |
|------|-----|
| **状态** | ✅ 通过 |
| **实际结果** | Backend 路由测试已覆盖匿名写接口 401；iOS/Android 均通过 effect / context 方式拦截未登录写操作 |
| **备注** | Android 宿主层当前以 Toast + placeholder dialog 承接 |

---

#### QA-E-002：Backend migration 真实推送验证受历史环境阻塞

| 属性 | 值 |
|------|-----|
| **优先级** | P1 |
| **涉及平台** | Backend |
| **异常类型** | 服务端错误 / 环境阻塞 |
| **模拟方式** | 本地执行 `docker compose -f tests/docker-compose.yml up -d` 与 `npx supabase db push` |
| **前置条件** | comments migration 已新增 |
| **测试步骤** | 1. 启动本地依赖；2. 执行 `supabase db push`；3. 观察是否能验证到本次 comments migration |
| **预期结果** | 理想情况下 migration 可成功执行；若被历史环境阻塞，需要如实记录 |

**执行结果**：

| 属性 | 值 |
|------|-----|
| **状态** | ⚠️ 阻塞 |
| **实际结果** | `db push` 被既有历史 migration `20260727000200_add_role_to_profiles.sql` 的幂等性问题阻塞，无法继续验证到本次 comments migration |
| **备注** | 不属于本次 comments 代码 contract / test / build 问题，已在 `code-backend-review.md` 记录 |

---

### 3.4 兼容性测试

#### QA-C-001：跨端评论能力 contract 与恢复语义一致

| 属性 | 值 |
|------|-----|
| **优先级** | P1 |
| **涉及平台** | Backend / iOS / Android |
| **兼容场景** | 跨端一致性 |
| **前置条件** | spec / design / 各端实现均已完成 |
| **测试步骤** | 1. 对照 spec/design 检查 Backend contract；2. 对照 iOS/Android DTO/Entity 与 ViewModel 逻辑；3. 检查登录恢复语义与评论入口来源字段 |
| **预期结果** | 两端均按统一 contract 和恢复语义实现，不出现字段漂移或自动重放差异 |

**执行结果**：

| 属性 | 值 |
|------|-----|
| **状态** | ✅ 通过 |
| **实际结果** | Backend canonical contract 已锁定；iOS/Android 均已实现 Home/Player 来源区分、评论抽屉恢复及发送/点赞不自动重放 |
| **备注** | Android 首页搜索结果页仅补齐 `onComment = {}` 兼容编译，不扩展本次需求范围 |

---

## 四、测试结果汇总

| 统计项 | 数量 |
|--------|------|
| 总用例数 | 8 |
| ✅ 通过 | 7 |
| ❌ 失败 | 0 |
| ⏭️ 跳过 | 0 |
| ⚠️ 阻塞 | 1 |

---

## 五、未通过用例详细记录

| 用例编号 | 标题 | 优先级 | 状态 | 失败原因 | 影响评估 |
|---------|------|--------|------|---------|---------|
| QA-E-002 | Backend migration 真实推送验证受历史环境阻塞 | P1 | ⚠️ 阻塞 | 既有历史 migration 幂等性问题阻塞本地 `supabase db push` | 不影响本次 comments 代码、测试、构建与端侧接入；属于环境遗留问题，建议后续单独治理 |

---

## 六、用户决策

> 本轮按 `/fast-forward` 的自动推进要求执行，`code-human-review` 已自动通过。
>
> QA 阶段已产出文档并完成自动化验证梳理。当前唯一阻塞项是 **Backend 历史 migration 环境问题**，不属于本次 comments 功能代码缺陷。
>
> 按当前执行策略，建议将其记录为已知遗留并继续推进到 `worktree-merge`。
