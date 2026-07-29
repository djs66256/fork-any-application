# 实现计划：Backend — PRD-10 签到与消息系统

> 创建日期：2026-07-29
> 对应技术方案：design-backend.md
> 对应需求：spec.md

## 概述

本期 Backend 需要在现有 `Route → Middleware → Service → Repository → Shared` 四层结构上，补齐签到状态/签到提交、菜单消息预览、系统消息列表、互动消息列表 5 个接口，并新增签到与系统消息的持久化能力。

实现顺序遵循轻量 TDD：先锁定 schema、认证、分页、空态与错误码 contract，再分层实现 `shared contract → repository(mock) → repository(supabase)+migration → service → route`，避免对现有 `auth`、`comments`、`player` 等接口造成回归影响。

## 测试场景列表

> 遵循轻量 TDD：先定义测试场景，再进入实现步骤。
> 各端测试要求见对应 CLAUDE.md。

| 编号 | 测试场景 | 输入 | 预期输出 | 类型 | 优先级 |
|------|---------|------|---------|------|--------|
| T-01 | 签到与消息 schema 解析默认值和合法参数 | 合法 `page/pageSize`、合法 `X-Installation-Id`、合法签到/消息实体 | query 被解析为 canonical 值；实体可通过 Zod 校验 | 单元测试 | P0 |
| T-02 | schema 拦截非法分页和非法安装标识 | `page=0`、`pageSize=21`、非法 UUID header | 抛校验错误；route 最终返回 `400 + VALIDATION_ERROR` | 单元测试 | P0 |
| T-03 | mock 签到仓储支持匿名/登录主体与同日幂等 | `userId` 或 `installationId`，同一业务日重复签到 | 重复提交不新增记录；能返回最近签到历史 | 单元测试 | P0 |
| T-04 | mock 消息仓储返回 preview、系统消息分页、互动消息分页 | seeded fixture + `page/pageSize` | preview 返回最新一条；列表返回 `{ data, pagination }`；互动消息仅依赖登录用户输入 | 单元测试 | P0 |
| T-05 | Supabase 签到/系统消息仓储正确映射数据与异常 | mock Supabase client 返回记录、唯一索引冲突、连接异常 | 查询结果可被 schema 解析；异常映射为 `SERVICE_UNAVAILABLE`；同日冲突按幂等语义收口 | 单元测试 | P0 |
| T-06 | `CheckInService` 处理主体优先级、7 日进度与新一轮重开 | 登录态 + header、匿名态、完成 7 天后的第 8 天 | 登录态优先账号；返回固定 7 个 `days`；第 8 天从第 1 天重开 | 单元测试 | P0 |
| T-07 | `MessageService` 处理 preview 空态、分页与互动消息鉴权边界 | 无系统消息、合法分页、缺失 `userId` | preview 空态返回 `null` 供 route 输出 204；系统消息分页正确；互动消息缺少登录态时抛 `AUTH_UNAUTHORIZED` | 单元测试 | P0 |
| T-08 | `GET /api/check-ins/status` 处理成功与匿名缺少 header | 匿名合法 header、登录态 token、匿名无 header | 成功返回 `SignInStatus`；缺少 header 返回 `400 + VALIDATION_ERROR` | 路由测试 | P0 |
| T-09 | `POST /api/check-ins` 处理签到成功、重复提交与非法 header | 合法主体、同日重复提交、非法 UUID header | 成功返回最新状态；重复提交保持 200 幂等成功；非法 header 返回 400 | 路由测试 | P0 |
| T-10 | `GET /api/messages/preview` 处理成功与空态 | 有系统消息 / 无系统消息 | 有数据时返回 200 + `MessagePreview`；无数据时返回 `204 No Content` | 路由测试 | P0 |
| T-11 | `GET /api/messages/system` 与 `GET /api/messages/interactions` 处理分页和鉴权 | 合法分页、非法分页、匿名访问 interactions、登录访问 interactions | system 返回 `{ data, pagination }`；非法分页返回 400；匿名 interactions 返回 `401 + AUTH_UNAUTHORIZED`；登录成功返回列表 | 路由测试 | P0 |
| T-12 | migration 可应用且表/索引满足设计 | 本地 Supabase + 新 migration | `check_in_records`、`system_messages` 表及索引创建成功 | 集成验证 | P1 |

## 实现步骤

### Step 1：先补 shared contract 测试并收口 schema、config、repository registry

- **关联测试**：T-01、T-02
- **目标文件**：`backend/src/lib/schemas.ts`、`backend/src/lib/config.ts`、`backend/src/lib/__tests__/schemas.test.ts`、`backend/src/lib/__tests__/config.test.ts`、`backend/src/repositories/interfaces/check-in.repository.interface.ts`、`backend/src/repositories/interfaces/system-message.repository.interface.ts`、`backend/src/repositories/interfaces/interaction-message.repository.interface.ts`、`backend/src/repositories/repository-registry.ts`
- **实现内容**：
  1. 先在 `schemas.test.ts` 中补齐签到与消息 contract 测试，锁定 `InstallationIdHeaderSchema`、分页 query、`SignInStatus`、`MessagePreview`、`SystemMessage`、`InteractionMessage` 以及列表响应结构。
  2. 在 `schemas.ts` 中新增签到/消息相关 schema，并继续复用现有 `PaginationSchema`；分页上限按设计收口为 `pageSize <= 20`。
  3. 在 `config.ts` 中新增签到和系统消息仓储选择配置，并为互动消息保留固定 `mock` 的配置口径；在 `config.test.ts` 中覆盖默认值。
  4. 新增 3 个 repository interface，并在 `repository-registry.ts` 中接入 `get/set/reset` 能力，保持 route 层继续通过 registry 获取依赖。
- **验证方式**：
  - 在 `backend/` 下运行 `npm run test -- src/lib/__tests__/schemas.test.ts`
  - 在 `backend/` 下运行 `npm run test -- src/lib/__tests__/config.test.ts`
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `backend/src/lib/schemas.ts` | 修改 | 新增安装标识、签到状态、消息实体与分页响应 schema |
| `backend/src/lib/config.ts` | 修改 | 新增签到/消息仓储配置项 |
| `backend/src/lib/__tests__/schemas.test.ts` | 修改 | 补充签到与消息 schema 正反向测试 |
| `backend/src/lib/__tests__/config.test.ts` | 修改 | 补充新配置默认值测试 |
| `backend/src/repositories/interfaces/check-in.repository.interface.ts` | 新增 | 定义签到仓储契约 |
| `backend/src/repositories/interfaces/system-message.repository.interface.ts` | 新增 | 定义系统消息仓储契约 |
| `backend/src/repositories/interfaces/interaction-message.repository.interface.ts` | 新增 | 定义互动消息仓储契约 |
| `backend/src/repositories/repository-registry.ts` | 修改 | 接入签到/消息仓储默认创建与注入能力 |

### Step 2：实现 mock repository，先闭合开发态与测试态数据链路

- **关联测试**：T-03、T-04
- **目标文件**：`backend/src/repositories/mock/check-in.mock.repository.ts`、`backend/src/repositories/mock/system-message.mock.repository.ts`、`backend/src/repositories/mock/interaction-message.mock.repository.ts`、`backend/src/repositories/__tests__/check-in.mock.repository.test.ts`、`backend/src/repositories/__tests__/system-message.mock.repository.test.ts`、`backend/src/repositories/__tests__/interaction-message.mock.repository.test.ts`
- **实现内容**：
  1. 在 mock 签到仓储中建立按 `subject_type + subject_id + business_date` 查询/写入的内存结构，为 service 层提供最近签到历史与同日幂等写入能力。
  2. 在 mock 系统消息仓储中提供固定 seeded fixture，支持 latest-one preview 和 `page/pageSize` 分页查询。
  3. 在 mock 互动消息仓储中提供按 `userId` 过滤的 seeded fixture，首版只实现登录用户列表读取，不新增匿名逻辑。
  4. 通过 repository tests 锁定排序、分页、空列表、幂等签到和 fixture contract，确保后续 service/route 可以稳定复用。
- **验证方式**：
  - 在 `backend/` 下运行 `npm run test -- src/repositories/__tests__/check-in.mock.repository.test.ts`
  - 在 `backend/` 下运行 `npm run test -- src/repositories/__tests__/system-message.mock.repository.test.ts`
  - 在 `backend/` 下运行 `npm run test -- src/repositories/__tests__/interaction-message.mock.repository.test.ts`
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `backend/src/repositories/mock/check-in.mock.repository.ts` | 新增 | 实现 mock 签到记录查询与幂等写入 |
| `backend/src/repositories/mock/system-message.mock.repository.ts` | 新增 | 实现系统消息 preview 与分页读取 |
| `backend/src/repositories/mock/interaction-message.mock.repository.ts` | 新增 | 实现登录态互动消息 seeded fixture |
| `backend/src/repositories/__tests__/check-in.mock.repository.test.ts` | 新增 | 覆盖签到 mock repository 幂等与主体读取 |
| `backend/src/repositories/__tests__/system-message.mock.repository.test.ts` | 新增 | 覆盖系统消息 preview 与分页 contract |
| `backend/src/repositories/__tests__/interaction-message.mock.repository.test.ts` | 新增 | 覆盖互动消息登录态列表 contract |

### Step 3：实现 Supabase repository 与 migration，补齐真实持久化能力

- **关联测试**：T-05、T-12
- **目标文件**：`backend/src/repositories/supabase/check-in.supabase.repository.ts`、`backend/src/repositories/supabase/system-message.supabase.repository.ts`、`backend/src/repositories/supabase/__tests__/check-in.supabase.repository.test.ts`、`backend/src/repositories/supabase/__tests__/system-message.supabase.repository.test.ts`、`backend/supabase/migrations/<timestamp>_create_signin_and_message_tables.sql`
- **实现内容**：
  1. 新建 migration，创建 `check_in_records` 和 `system_messages` 表及对应索引，不修改历史 migration。
  2. 在 `CheckInSupabaseRepository` 中实现最近签到记录查询与当日签到写入；遇到唯一索引冲突时按幂等成功语义回读最新状态，而不是向上抛 409。
  3. 在 `SystemMessageSupabaseRepository` 中实现最新 1 条摘要查询和分页列表读取，保证结果能通过 shared schema parse。
  4. 在 Supabase repository tests 中锁定行映射、空数据、唯一冲突、连接异常与脏数据异常的转换口径，统一折叠到 `SERVICE_UNAVAILABLE`。
- **验证方式**：
  - 在 `backend/` 下运行 `npm run test -- src/repositories/supabase/__tests__/check-in.supabase.repository.test.ts`
  - 在 `backend/` 下运行 `npm run test -- src/repositories/supabase/__tests__/system-message.supabase.repository.test.ts`
  - 在 `backend/` 下运行 `docker compose -f tests/docker-compose.yml up -d`
  - 在 `backend/` 下运行 `npx supabase db push`
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `backend/src/repositories/supabase/check-in.supabase.repository.ts` | 新增 | 实现签到记录查询与幂等写入 |
| `backend/src/repositories/supabase/system-message.supabase.repository.ts` | 新增 | 实现系统消息 preview 与分页查询 |
| `backend/src/repositories/supabase/__tests__/check-in.supabase.repository.test.ts` | 新增 | 覆盖签到 Supabase repository 映射与异常转换 |
| `backend/src/repositories/supabase/__tests__/system-message.supabase.repository.test.ts` | 新增 | 覆盖系统消息 Supabase repository contract |
| `backend/supabase/migrations/<timestamp>_create_signin_and_message_tables.sql` | 新增 | 创建签到与系统消息表、索引 |

### Step 4：实现 Service 层，统一业务日、主体优先级与消息空态编排

- **关联测试**：T-06、T-07
- **目标文件**：`backend/src/services/check-in/check-in.service.ts`、`backend/src/services/check-in/check-in.service.test.ts`、`backend/src/services/message/message.service.ts`、`backend/src/services/message/message.service.test.ts`
- **实现内容**：
  1. 新增 `CheckInService`，通过依赖注入接收签到仓储和可控的业务日提供器，在测试中固定 `server_date`，避免依赖真实时间。
  2. 在 service 中统一解析签到主体：有 `userId` 时优先账号态；无 `userId` 时要求合法 `installationId`；两者都缺失时抛 `VALIDATION_ERROR`。
  3. 实现 7 日签到板状态计算、`today_signed`、`current_streak`、`reward_copy`、`should_show_popup` 和“第 8 天新一轮”逻辑，并保证输出始终是固定 7 个 `days`。
  4. 新增 `MessageService`，统一处理 preview 空态、系统消息分页和互动消息登录门槛；互动消息缺少 `userId` 时抛 `AUTH_UNAUTHORIZED`，不把鉴权散落到 repository 层。
- **验证方式**：
  - 在 `backend/` 下运行 `npm run test -- src/services/check-in/check-in.service.test.ts`
  - 在 `backend/` 下运行 `npm run test -- src/services/message/message.service.test.ts`
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `backend/src/services/check-in/check-in.service.ts` | 新增 | 实现签到主体解析、7 日进度与幂等提交编排 |
| `backend/src/services/check-in/check-in.service.test.ts` | 新增 | 覆盖业务日、账号优先级、重复签到与新一轮重开 |
| `backend/src/services/message/message.service.ts` | 新增 | 实现消息 preview、系统消息列表、互动消息列表编排 |
| `backend/src/services/message/message.service.test.ts` | 新增 | 覆盖 preview 空态、分页与互动消息鉴权边界 |

### Step 5：实现 route helper 与 5 个接口，完成 route contract 回归

- **关联测试**：T-08、T-09、T-10、T-11
- **目标文件**：`backend/src/app/api/check-ins/parse-installation-id.ts`、`backend/src/app/api/check-ins/status/route.ts`、`backend/src/app/api/check-ins/route.ts`、`backend/src/app/api/messages/preview/route.ts`、`backend/src/app/api/messages/system/route.ts`、`backend/src/app/api/messages/interactions/route.ts`、`backend/src/app/api/__tests__/check-ins.status.test.ts`、`backend/src/app/api/__tests__/check-ins.test.ts`、`backend/src/app/api/__tests__/messages.preview.test.ts`、`backend/src/app/api/__tests__/messages.system.test.ts`、`backend/src/app/api/__tests__/messages.interactions.test.ts`
- **实现内容**：
  1. 参考现有 `player/parse-playback-session-id.ts` 新增 `parse-installation-id.ts`，集中校验 `X-Installation-Id`，缺失或非法时抛 `Errors.validationError(...)`。
  2. 在签到 routes 中复用 `resolveOptionalAuthContext()`、`withErrorHandler` 和 repository registry，确保账号态优先、匿名态兜底、同日重复提交返回 200 幂等成功。
  3. 在消息 routes 中分别实现 preview、system、interactions 三条读接口；preview route 负责把 service 的空态转成 `204 No Content`，interactions route 通过 `requireAuthContext()` 保证 401 contract。
  4. 通过 route tests 固定 header 校验、分页 query、preview 204、匿名/登录交互、`SERVICE_UNAVAILABLE` 透传等行为，避免 route 层侵入业务逻辑。
- **验证方式**：
  - 在 `backend/` 下运行 `npm run test -- src/app/api/__tests__/check-ins.status.test.ts`
  - 在 `backend/` 下运行 `npm run test -- src/app/api/__tests__/check-ins.test.ts`
  - 在 `backend/` 下运行 `npm run test -- src/app/api/__tests__/messages.preview.test.ts`
  - 在 `backend/` 下运行 `npm run test -- src/app/api/__tests__/messages.system.test.ts`
  - 在 `backend/` 下运行 `npm run test -- src/app/api/__tests__/messages.interactions.test.ts`
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `backend/src/app/api/check-ins/parse-installation-id.ts` | 新增 | 统一解析并校验 `X-Installation-Id` |
| `backend/src/app/api/check-ins/status/route.ts` | 新增 | 签到状态查询接口 |
| `backend/src/app/api/check-ins/route.ts` | 新增 | 签到提交接口 |
| `backend/src/app/api/messages/preview/route.ts` | 新增 | 菜单消息预览接口 |
| `backend/src/app/api/messages/system/route.ts` | 新增 | 系统消息分页接口 |
| `backend/src/app/api/messages/interactions/route.ts` | 新增 | 互动消息分页接口 |
| `backend/src/app/api/__tests__/check-ins.status.test.ts` | 新增 | 覆盖签到状态 route contract |
| `backend/src/app/api/__tests__/check-ins.test.ts` | 新增 | 覆盖签到提交 route contract |
| `backend/src/app/api/__tests__/messages.preview.test.ts` | 新增 | 覆盖 preview 成功与 204 空态 |
| `backend/src/app/api/__tests__/messages.system.test.ts` | 新增 | 覆盖系统消息分页与校验错误 |
| `backend/src/app/api/__tests__/messages.interactions.test.ts` | 新增 | 覆盖互动消息鉴权与分页 contract |

### Step 6：执行后端全量验证，确认新增模块不破坏既有接口

- **关联测试**：T-01 至 T-12
- **目标文件**：`backend/package.json`、`docs/specs/2026-07-29-prd-10-signin-messages/plan-backend.md`
- **实现内容**：
  1. 基于当前真实脚本口径，统一使用 `npm run test`、`npm run build`、`npm run lint` 作为回归验证命令，不臆造仓库中不存在的命令。
  2. 在功能开发完成后执行签到/消息定向测试、backend 全量测试、build、lint 与本地 migration 验证，确认新增路由没有破坏 `auth`、`comments`、`player` 等既有模块。
  3. 若全量验证暴露类型、导入或 Next route 兼容问题，回补到对应实现步骤中的目标文件，保持 plan 可直接指导 coding 阶段收敛。
- **验证方式**：
  - 在 `backend/` 下运行 `npm run test`
  - 在 `backend/` 下运行 `npm run build`
  - 在 `backend/` 下运行 `npm run lint`
  - 在 `backend/` 下运行 `docker compose -f tests/docker-compose.yml up -d`
  - 在 `backend/` 下运行 `npx supabase db push`
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `backend/package.json` | 只读确认 | 使用现有 test/build/lint 脚本作为统一验证入口 |
| `docs/specs/2026-07-29-prd-10-signin-messages/plan-backend.md` | 新增 | 固化实现顺序、验证口径与目标文件 |

## 依赖关系

```text
Step 1（shared contract）
  └──▶ Step 2（mock repositories）
          └──▶ Step 3（supabase repositories + migration）
                  └──▶ Step 4（services）
                          └──▶ Step 5（routes + route tests）
                                  └──▶ Step 6（full regression）
```

## 验证总览

- [ ] 签到/消息 shared schema 与 config 定向测试通过（`cd backend && npm run test -- src/lib/__tests__/schemas.test.ts`、`cd backend && npm run test -- src/lib/__tests__/config.test.ts`）
- [ ] Mock repository 定向测试通过（`cd backend && npm run test -- src/repositories/__tests__/check-in.mock.repository.test.ts`、`cd backend && npm run test -- src/repositories/__tests__/system-message.mock.repository.test.ts`、`cd backend && npm run test -- src/repositories/__tests__/interaction-message.mock.repository.test.ts`）
- [ ] Supabase repository 定向测试通过（`cd backend && npm run test -- src/repositories/supabase/__tests__/check-in.supabase.repository.test.ts`、`cd backend && npm run test -- src/repositories/supabase/__tests__/system-message.supabase.repository.test.ts`）
- [ ] Service 定向测试通过（`cd backend && npm run test -- src/services/check-in/check-in.service.test.ts`、`cd backend && npm run test -- src/services/message/message.service.test.ts`）
- [ ] Route 定向测试通过（`cd backend && npm run test -- src/app/api/__tests__/check-ins.status.test.ts`、`cd backend && npm run test -- src/app/api/__tests__/check-ins.test.ts`、`cd backend && npm run test -- src/app/api/__tests__/messages.preview.test.ts`、`cd backend && npm run test -- src/app/api/__tests__/messages.system.test.ts`、`cd backend && npm run test -- src/app/api/__tests__/messages.interactions.test.ts`）
- [ ] Backend 全量测试通过（`cd backend && npm run test`）
- [ ] Build 成功（`cd backend && npm run build`）
- [ ] 无新增 lint 错误（`cd backend && npm run lint`）
- [ ] 本地 Supabase migration 验证完成（`cd backend && docker compose -f tests/docker-compose.yml up -d`、`cd backend && npx supabase db push`）

## 变更文件汇总

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `backend/src/lib/schemas.ts` | 修改 | 新增签到/消息 schema 与列表 contract |
| `backend/src/lib/config.ts` | 修改 | 新增签到/消息仓储配置 |
| `backend/src/lib/__tests__/schemas.test.ts` | 修改 | 补充签到/消息 schema 测试 |
| `backend/src/lib/__tests__/config.test.ts` | 修改 | 补充配置默认值测试 |
| `backend/src/repositories/interfaces/check-in.repository.interface.ts` | 新增 | 定义签到仓储契约 |
| `backend/src/repositories/interfaces/system-message.repository.interface.ts` | 新增 | 定义系统消息仓储契约 |
| `backend/src/repositories/interfaces/interaction-message.repository.interface.ts` | 新增 | 定义互动消息仓储契约 |
| `backend/src/repositories/repository-registry.ts` | 修改 | 接入新仓储的 registry 能力 |
| `backend/src/repositories/mock/check-in.mock.repository.ts` | 新增 | 实现 mock 签到仓储 |
| `backend/src/repositories/mock/system-message.mock.repository.ts` | 新增 | 实现 mock 系统消息仓储 |
| `backend/src/repositories/mock/interaction-message.mock.repository.ts` | 新增 | 实现 mock 互动消息仓储 |
| `backend/src/repositories/__tests__/check-in.mock.repository.test.ts` | 新增 | 覆盖签到 mock repository |
| `backend/src/repositories/__tests__/system-message.mock.repository.test.ts` | 新增 | 覆盖系统消息 mock repository |
| `backend/src/repositories/__tests__/interaction-message.mock.repository.test.ts` | 新增 | 覆盖互动消息 mock repository |
| `backend/src/repositories/supabase/check-in.supabase.repository.ts` | 新增 | 实现 Supabase 签到仓储 |
| `backend/src/repositories/supabase/system-message.supabase.repository.ts` | 新增 | 实现 Supabase 系统消息仓储 |
| `backend/src/repositories/supabase/__tests__/check-in.supabase.repository.test.ts` | 新增 | 覆盖签到 Supabase repository |
| `backend/src/repositories/supabase/__tests__/system-message.supabase.repository.test.ts` | 新增 | 覆盖系统消息 Supabase repository |
| `backend/src/services/check-in/check-in.service.ts` | 新增 | 实现签到业务编排 |
| `backend/src/services/check-in/check-in.service.test.ts` | 新增 | 覆盖签到 service 关键场景 |
| `backend/src/services/message/message.service.ts` | 新增 | 实现消息业务编排 |
| `backend/src/services/message/message.service.test.ts` | 新增 | 覆盖消息 service 关键场景 |
| `backend/src/app/api/check-ins/parse-installation-id.ts` | 新增 | 统一解析安装标识 header |
| `backend/src/app/api/check-ins/status/route.ts` | 新增 | 签到状态查询接口 |
| `backend/src/app/api/check-ins/route.ts` | 新增 | 签到提交接口 |
| `backend/src/app/api/messages/preview/route.ts` | 新增 | 菜单消息预览接口 |
| `backend/src/app/api/messages/system/route.ts` | 新增 | 系统消息分页接口 |
| `backend/src/app/api/messages/interactions/route.ts` | 新增 | 互动消息分页接口 |
| `backend/src/app/api/__tests__/check-ins.status.test.ts` | 新增 | 覆盖签到状态 route |
| `backend/src/app/api/__tests__/check-ins.test.ts` | 新增 | 覆盖签到提交 route |
| `backend/src/app/api/__tests__/messages.preview.test.ts` | 新增 | 覆盖 preview route |
| `backend/src/app/api/__tests__/messages.system.test.ts` | 新增 | 覆盖系统消息 route |
| `backend/src/app/api/__tests__/messages.interactions.test.ts` | 新增 | 覆盖互动消息 route |
| `backend/supabase/migrations/<timestamp>_create_signin_and_message_tables.sql` | 新增 | 创建签到与系统消息表、索引 |
