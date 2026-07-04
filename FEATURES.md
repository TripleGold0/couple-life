# Couple Life · 项目功能详细说明

> 本文档面向开发者与产品同学，详细描述 Couple Life 各模块的业务能力、交互流程、关键接口与数据结构。
> 项目总览与启动方式见 [README.md](README.md)。

## 目录

- [总体架构](#总体架构)
- [1. 账号体系](#1-账号体系)
- [2. 情侣关系](#2-情侣关系)
- [3. 每日打卡](#3-每日打卡)
- [4. 旅行记录](#4-旅行记录)
- [5. 共享相册](#5-共享相册)
- [6. 首页数据看板](#6-首页数据看板)
- [7. 情侣情感调节 Agent](#7-情侣情感调节-agent)
- [8. 电子宠物](#8-电子宠物)
- [9. 文件上传](#9-文件上传)
- [10. 安全与鉴权](#10-安全与鉴权)
- [11. 通用约定](#11-通用约定)

---

## 总体架构

```
┌────────────────────┐         ┌─────────────────────┐         ┌──────────────┐
│  Vue 3 + Element   │  HTTPS  │  Spring Boot 3.3.5  │   JDBC  │  MySQL 8.x   │
│  Plus + Pinia      │ ──────> │  Security + JWT     │ ──────> │  couple_life │
│  (Vite dev server) │         │  MyBatis-Plus       │         └──────────────┘
└────────────────────┘         │                     │
         │                     │  情感顾问 Agent      │ HTTPS   ┌──────────────┐
         │ localStorage         │ (HttpClient)        │ ──────> │  LLM 服务    │
         │ 模型自定义配置       └─────────────────────┘         │ (OpenAI 兼容) │
                                                                └──────────────┘
```

- **前端**：Vue 3 + Vite 6 + Element Plus + Pinia + Vue Router 4 + Axios + ECharts。
- **后端**：Spring Boot 3.3.5 / Java 17，Spring Security + JWT 鉴权，MyBatis-Plus 持久化，Java 内置 `HttpClient` 调 LLM。
- **存储**：MySQL（关系数据） + 本地文件系统（图片）。
- **LLM**：OpenAI 兼容协议，默认 Pollinations.ai 免费匿名 tier，可被前端运行时配置覆盖。

---

## 1. 账号体系

### 1.1 注册

- **入口**：`POST /api/auth/register`
- **必填**：`username` `nickname` `gender` `password` `confirmPassword` `captcha`
- **二选一**：`phone` 或 `email`（验证码发往该账号）
- **校验**：手机号 `^1[3-9]\d{9}$`、邮箱 `RFC 5322` 简化版；前后端使用同一套规则（前端 `utils/validators.js`，后端 `@Pattern`/`@Email`）
- **流程**：
  1. 前端先调 `POST /api/auth/captcha`，type=`REGISTER`，account 填手机号或邮箱
  2. 后端写入 `login_captcha` 表（带 60s 冷却），dev 环境可在响应里回显明文
     - 邮箱场景：配置 `MAIL_USERNAME`/`MAIL_PASSWORD` 后通过 QQ SMTP 真实发送（详见 `EMAIL_CAPTCHA.md`），未配置则仅记日志
     - 手机号场景：尚未对接短信网关，仅写表 + 记日志
  3. 提交注册时校验验证码，密码 `BCryptPasswordEncoder` 加盐
  4. 默认 `profile_completed=1`

### 1.2 登录（双模式）

| 模式 | loginType | account | 凭证 | 备注 |
|---|---|---|---|---|
| 账号密码 | `PASSWORD` | 手机号或邮箱 | `password` + 图形验证码 | 调 `GET /api/auth/image-captcha` 拿 SVG dataURI |
| 短信登录 | `SMS_CODE` | 手机号 | `captcha` | 未注册时**自动建号**，返回 `user.profileCompleted=0` |

- 登录成功返回 `LoginVO { token, user }`，token 默认有效期 168h（`app.jwt.expire-hours`）。
- 前端 `userStore` 把 token 写入 `localStorage`，axios 拦截器自动加 `Authorization` 头。

### 1.3 强制完善资料

- 短信登录新建号场景下 `profileCompleted=0`，前端 `MainLayout.vue` 在路由守卫和 `onMounted` 都会兜底弹 `ProfileSetupDialog`，必须填写 nickname / gender / password 后才能继续。
- **接口**：`POST /api/user/complete-profile`。

### 1.4 个人信息

- `Profile.vue` + `UserController` 支持修改昵称、头像、性别、生日；头像走 `UploadController` 上传后回写 `avatar`。

---

## 2. 情侣关系

- **绑定方式**：邀请码。每个用户都有唯一 `invite_code`，对方在 `Profile` / 绑定页输入即可。
- **核心接口**（`CoupleController`）：
  - `GET /api/couple/info`：当前情侣关系详情
  - `POST /api/couple/bind`：用对方邀请码建立关系
  - `POST /api/couple/unbind`：解除关系（双向）
- **数据**：`couple_relation(user_id, partner_id, couple_id, love_start_date, status)`，双向各存一行，`couple_id` 共享。
- **关键约束**：业务接口（打卡、相册等）会基于当前用户的有效 `couple_id` 取数，未绑定的用户只能看到自己的内容。

---

## 3. 每日打卡

- **页面**：`Checkin.vue`，左侧日历视图，点击 `今日打卡` 弹窗写心情。
- **数据模型**：`daily_checkin(user_id, checkin_date, mood_emoji, mood_text, content)`，`(user_id, checkin_date)` 唯一，每人每天一次。
- **接口**：
  - `POST /api/checkins`：新增/覆盖当日打卡
  - `GET /api/checkins/calendar?month=YYYY-MM`：按月拉取本人 + 伴侣的打卡，用于日历表情渲染
  - `GET /api/checkins/couple?date=YYYY-MM-DD`：拉取某天双方的打卡详情
- **首页联动**：`HomeController` 取最近 6 条混合打卡用于看板。

---

## 4. 旅行记录

- **页面**：`Travel.vue`，列表 + 详情卡片，可上传多图与坐标。
- **数据模型**：
  - `travel_record(couple_id, location, latitude, longitude, travel_date, my_feeling, partner_feeling, ...)`
  - `travel_image(travel_id, image_url, sort)`
- **接口**（`TravelController`）：
  - `GET /api/travel/list`：按 `couple_id` 取列表
  - `POST /api/travel`：新建（含图片 URL 数组）
  - `PUT /api/travel/{id}` / `DELETE /api/travel/{id}`
- **图片**：先调 `POST /api/upload/image` 拿到 URL，再随 travel 记录一起提交。

---

## 5. 共享相册

- **页面**：`Album.vue`，按日期分组瀑布流，支持点击放大与评论；右上角「批量选择」进入多选模式后底部浮现操作条，可全选 / 全不选 / 一键导出 ZIP。
- **数据模型**：
  - `album_image(couple_id, image_url, title, description, taken_at, uploader_id)`
  - `photo_comment(image_id, user_id, content)`
- **接口**（`AlbumController`）：
  - `GET /api/album/list`：按日期分组的 `AlbumGroupVO[]`
  - `POST /api/album/upload`：批量入库
  - `DELETE /api/album/{id}`：软删（`deleted=1`）
  - `GET /api/album/{id}/comments` / `POST /api/album/{id}/comments`：评论 CRUD
  - `POST /api/album/photos/export`：批量导出照片为 ZIP
    - 请求体：`[1, 2, 3]`（照片 id 数组，至少 1 个）
    - 成功响应：`Content-Type: application/zip` + `Content-Disposition: attachment; filename="album_<YYYY-MM-DD>.zip"`，body 为流式 ZIP
    - 失败响应：HTTP 200 + `application/json` 的 `Result.fail(message)`（前端据 content-type 区分降级提示）
- **批量导出实现要点**：
  - 后端用 JDK 自带 `java.util.zip.ZipOutputStream` 流式输出，无需额外依赖；
    ZIP 内文件名格式 `<拍摄日期>_<照片id>.<原扩展名>`，重名追加 `_1/_2…` 序号。
  - 校验（id 非空、归属当前情侣、未软删）通过后才设置响应头并写流；磁盘文件丢失会跳过单张并 warn 日志，
    流式写入中途的 IOException 仅记日志、不再抛 BizException（避免与已发出的 zip 字节混合产生损坏响应）。
  - 前端 `api/album.js#exportPhotos` 直接用裸 `axios`、`responseType: 'blob'`、120s 超时，绕过 `request.js`
    的统一响应拦截器（拦截器期望 `{code, message, data}` 包装，会把 Blob 当成业务失败）。
  - 前端用临时 `<a download>` + `URL.createObjectURL` 触发浏览器下载，文件名优先解析响应头
    `Content-Disposition` 的 RFC5987 `filename*=UTF-8''…` 形式。
- **逻辑删除**：MyBatis-Plus 全局 `logic-delete-field: deleted`。

---

## 6. 首页数据看板

- **页面**：`Home.vue`，使用 ECharts 渲染恋爱天数、打卡分布、最近旅行/相册片段。
- **接口**：`GET /api/home/summary`，单接口聚合返回 `HomeSummaryVO`：
  - `loveDays`：根据 `love_start_date` 计算
  - `recentCheckins`：最近 6 条
  - `recentTravels`：最近 3 条
  - `recentPhotos`：最近 8 张
- **未绑定情侣**：自动降级，仅返回个人打卡，旅行/相册返回空数组。

---

## 7. 情侣情感调节 Agent

> 心理咨询师 + 情感导师 + 中立调解人 三位一体的对话 Agent，专为情侣化解矛盾、增进理解设计。

### 7.1 角色与原则（写死在 `CounselorServiceImpl.SYSTEM_PROMPT`）

- **核心能力**：矛盾调解、情感解惑、沟通指导、关系评估、情绪疏导
- **工作原则**：中立公正、共情优先、非评判性、保护隐私、专业边界（家暴/出轨/自伤等场景导出官方求助渠道）
- **交互流程**：倾听 → 澄清 → 分析 → 建议（含话术、可做的事、雷区） → 跟进
- **输出风格**：温暖克制、避免命令式表达、适度引用心理学概念并通俗化

### 7.2 默认 LLM（开箱即用）

| 项 | 值 |
|---|---|
| Provider | Pollinations.ai（开源，OpenAI 兼容协议） |
| Base URL | `https://text.pollinations.ai/openai` |
| Model | `openai`（实际路由到 `gpt-oss-20b`） |
| 是否需 Key | 否（匿名 tier 直接可用） |

> 已实测匿名调用返回 200 OK。

### 7.3 用户自定义模型（前端运行时覆盖）

- 入口：`Counselor.vue` 顶部「模型设置」按钮
- 配置三项：`Base URL` / `API Key` / `Model`，存于 `localStorage(counselor_llm_config)`，**不上传服务端持久化**
- 优先级：**请求级覆盖 > yaml 默认**。任一字段为空时自动回退默认。
- 兼容服务示例：
  - OpenAI：`https://api.openai.com` + `gpt-4o-mini`
  - DeepSeek：`https://api.deepseek.com` + `deepseek-chat`
  - 硅基流动：`https://api.siliconflow.cn` + `Qwen/Qwen2.5-7B-Instruct`
  - 本地 Ollama：`http://localhost:11434` + `qwen2.5`

### 7.4 接口

- **`POST /api/counselor/chat`**（需 JWT）
- 请求体：
  ```json
  {
    "messages": [
      { "role": "user", "content": "我们今早因为家务又吵了一架..." },
      { "role": "assistant", "content": "听到这里我能感受到你的疲惫..." }
    ],
    "baseUrl": "可选，覆盖默认",
    "apiKey":  "可选，覆盖默认",
    "model":   "可选，覆盖默认"
  }
  ```
- 响应：`{ code: 200, data: { reply: "..." } }`
- 空 `messages`：直接返回开场白，不调 LLM。
- 异常映射：
  - 上游非 2xx → `BizException("情感顾问暂时无法回应，请稍后再试")`
  - 网络/解析异常 → `BizException("情感顾问连接异常：" + 原因)`
  - `BizException` 走 `GlobalExceptionHandler` 统一转 `code=500` + 消息

### 7.5 前端实现要点（`Counselor.vue`）

- 仅发送最近 20 条历史，控制 token 成本
- 单接口超时延长至 90s（默认 axios 是 15s，LLM 易超时）
- 错误提示统一在 `request.js` 拦截器，业务层不重复 toast
- Ctrl/⌘ + Enter 发送，普通 Enter 在 textarea 内换行

### 7.6 安全护栏

- system prompt 中已硬编码：检测家暴/严重心理危机时强制输出官方求助渠道（心理援助热线 400-161-9995 等）
- 对话内容仅在请求生命周期内存在，**不落库**
- API Key 仅存于浏览器 `localStorage`，每次请求随 body 发送，请在公共设备上慎用并退出后清除

---

## 8. 电子宠物

> 情侣共养的电子宠物模块（MVP）：宠物常驻悬浮挂件，可拖拽、可互动，由双方共同养育，
> 记录亲密度与陪伴天数。选择 / 更换宠物需要双方共同同意。

### 8.1 业务能力（MVP）

- **宠物种类**：内置 5 种（CAT/DOG/RABBIT/DRAGON/SLIME），由 `pet_type` 表配置，可扩展。
- **共同同意**：发起方提交 `pet_selection_request`（PENDING）→ 伴侣同意/拒绝；超过 24h 自动 `EXPIRED`。
  同一情侣同一时刻仅允许一个 PENDING 请求；新请求会把旧的 PENDING 置为 EXPIRED 让位。
- **悬浮挂件**：`FloatingPet.vue` 全局注入；长按（>200ms）拖拽，松手落位，坐标持久化到 `localStorage(floating_pet_position)`。
  单击展开互动 popover（喂食 / 抚摸 / 玩耍 + 关键属性 + 跳转详情）。
- **互动收益**（MVP 写死的简化数值）：
  - `FEED`：+intimacy 5 / +fullness 25（饱食度上限 100）
  - `PET` ：+intimacy 3 / +mood 15（每日同一用户最多 5 次）
  - `PLAY`：+intimacy 4 / +mood 10 / -fullness 5
- **属性衰减**：定时任务 `PetScheduledTask`（每日 00:05）扣减饱食度 -10、心情值 -8（不低于 0）。
  以 `pet.last_decay_date` 做幂等；用户进入互动时也会按"上次衰减日 → 今天"补齐衰减。
- **等级与阶段**：`level = 1 + intimacy / 100`；`stage`：`<500 BABY / <2000 TEEN / 否则 ADULT`。
- **陪伴天数**：`今日 - bound_date + 1`，由 VO 即时计算返回。
- **个人侧开关**：`sys_user.pet_display_enabled`（默认 1），关闭后该用户不再渲染悬浮挂件，
  但宠物属性仍正常衰减、双方互动数据仍计入。

### 8.2 数据模型

| 表 | 关键字段 | 说明 |
|---|---|---|
| `pet_type` | `code` `name` `description` `avatar` `sprite_url` `enabled` `sort_order` | 上架的宠物种类配置 |
| `pet` | `couple_id` `pet_type_id` `intimacy` `fullness` `mood` `level` `stage` `bound_date` `last_decay_date` `status` | 情侣宠物实例；`(couple_id, status)` 唯一保证一只活跃 |
| `pet_interaction_log` | `pet_id` `couple_id` `user_id` `action` `*_delta` | 每次互动一条；用于次数限制 / 回放 / 统计 |
| `pet_selection_request` | `couple_id` `requester_id` `partner_id` `pet_type_id` `nickname` `status` `expire_time` | 共同同意机制 |
| `sys_user` 新增列 | `pet_display_enabled` TINYINT NOT NULL DEFAULT 1 | 个人侧悬浮宠物开关 |

DDL 详见 [`sql/init.sql`](sql/init.sql) 中的「电子宠物模块」段落。
**升级旧库**需要手动执行：

```sql
ALTER TABLE sys_user ADD COLUMN pet_display_enabled TINYINT NOT NULL DEFAULT 1
  COMMENT '个人侧是否展示悬浮宠物：1 是，0 否';
```

### 8.3 接口（`PetController`，需 JWT）

| 方法 | 路径 | 说明 |
|---|---|---|
| GET  | `/api/pet/current` | 当前情侣的活跃宠物详情；未绑定情侣 / 未选择时 `data=null` |
| GET  | `/api/pet/types` | 上架的宠物种类列表 |
| GET  | `/api/pet/selection/list` | 与我相关（我发起 / 待我处理）的选择请求，按时间倒序 |
| POST | `/api/pet/selection/request` | 发起选择 / 更换请求，body `{ petTypeId, nickname? }` |
| POST | `/api/pet/selection/{id}/agree` | 同意请求（必须是 partnerId 才能调用），返回新生成的宠物 |
| POST | `/api/pet/selection/{id}/reject` | 拒绝请求 |
| POST | `/api/pet/interact` | 互动；body `{ action: "FEED" | "PET" | "PLAY" }`，返回最新宠物状态 |
| PUT  | `/api/user/pet-display` | 个人侧开关；body `{ enabled: true | false }` |

错误码均通过 `Result` 包装返回（参见 §11）；常见业务错误：
- 「请先绑定情侣关系」—— 未绑定情侣却调用互动 / 创建请求接口
- 「请求当前状态不可处理」—— 重复处理已经 AGREED/REJECTED/EXPIRED 的请求
- 「今天已经摸够 5 次啦」—— `PET` 行为命中每日次数上限

### 8.4 前端实现

- 全局组件：`couple-life-frontend/src/components/FloatingPet.vue`，由 `MainLayout.vue` 注入到 `z-index: 9999`，
  内部依据 `userStore.user.petDisplayEnabled` 与 `petStore.pet` 决定是否渲染。
- 页面：`couple-life-frontend/src/views/Pet.vue`（路由 `/pet`，菜单"电子宠物"）。覆盖：
  - 当前宠物详情卡片（亲密度 / 饱食度 / 心情进度条 + 互动按钮）
  - 与我相关的选择请求（同意 / 拒绝）
  - 宠物种类网格 + 昵称输入 + 发起请求
- Store：`stores/petStore.js` 缓存当前宠物，互动后由调用方直接覆写 `pet`。
- API：`api/pet.js` 集中封装。
- 个人信息页（`views/Profile.vue`）新增「显示悬浮电子宠物」开关，调用 `PUT /api/user/pet-display`。
- 拖拽细节：长按 200ms 或位移 >8px 触发拖拽；落位坐标持久化到 `localStorage(floating_pet_position)`；
  浏览器尺寸变化时自动夹回视口；移动端 `touchstart/move/end` 事件兼容。

### 8.5 资源接入规范（新增宠物种类）

> 适合在 MVP 之后扩充宠物图鉴时使用。

1. 美术准备：图标（48×48 PNG/WebP）、悬浮主体动画（序列帧 GIF / Lottie / WebM 任选其一）。
2. 资源上传：放到对象存储或 `app.upload-dir`，得到稳定 URL。
3. 数据库登记：

   ```sql
   INSERT INTO pet_type(code, name, description, avatar, sprite_url, sort_order, enabled)
   VALUES ('FOX', '小狐狸', '机灵的小狐狸', 'https://.../fox-avatar.png', 'https://.../fox.gif', 6, 1);
   ```

4. 前端兜底 emoji：`FloatingPet.vue` / `Pet.vue` 的 `emojiOf()` 中添加新 code 的 fallback emoji（避免后端资源未上传时空白）。
5. 上线灰度：可先把 `enabled=0` 内测，确认无问题再切 1 上架。

### 8.6 后续可扩展点

- 宠物商店（积分 / 签到奖励兑换食物、装扮）
- 宠物日记（升级、纪念日自动记录）
- 多宠物收藏（解锁过的宠物可随时切换）
- 与签到 / 纪念日联动：连续签到额外加亲密度
- WebSocket / SSE：实时推送选择请求与互动结果，替代当前的"按需轮询"

---

## 9. 文件上传

- **接口**：`POST /api/upload/image`（multipart/form-data，字段名 `file`）
- 限制：单文件 ≤ 10MB，整请求 ≤ 50MB（`spring.servlet.multipart`）
- 存储：`app.upload-dir` 指向的本地目录，文件名按日期 + UUID 重命名
- 静态映射：`WebMvcConfig` 把 `app.upload-dir` 暴露为 `/uploads/**`

> 生产环境建议替换为对象存储（OSS / S3），同时收紧 `SecurityConfig` 的 `permitAll("/uploads/**")` 范围。

---

## 10. 安全与鉴权

### 10.1 Spring Security 配置（`SecurityConfig`）

- 全局 `STATELESS`，CSRF 关闭，CORS 白名单 `*`（dev）
- `permitAll`：`/api/auth/**`、`/uploads/**`、`OPTIONS /**`
- 其他全部 `authenticated()`
- `JwtAuthFilter` 在 `UsernamePasswordAuthenticationFilter` 之前解析 `Authorization: Bearer xxx`

### 10.2 JWT

- 算法：HS256（jjwt 0.12.6）
- 载荷：`sub=userId`，`exp=now + app.jwt.expire-hours`
- secret 至少 32 字节，**生产必须替换** `app.jwt.secret`

### 10.3 异常处理（`GlobalExceptionHandler`）

| 异常 | 响应 |
|---|---|
| `BizException` | `{ code: 500, message: <原因> }` |
| `MethodArgumentNotValidException` / `BindException` | 取首个字段错误 message |
| 其他 `Exception` | `{ code: 500, message: "系统繁忙，请稍后再试" }`，原始堆栈写入日志 |

---

## 11. 通用约定

- **响应包装**：`Result<T> { code, message, data }`，业务成功 `code=200`；前端 `request.js` 拦截器以此判定是否 reject。
- **未登录**：`code=401` 或 HTTP 401/403 → 拦截器自动清 token + 跳转 `/login`。
- **逻辑删除**：MyBatis-Plus 全局 `deleted` 字段，删除即更新 `deleted=1`。
- **时间字段**：实体使用 `LocalDate` / `LocalDateTime`，序列化为 ISO 字符串。
- **日志**：`logback-spring.xml` 输出到 `app.log.path` 目录（默认 `logs/`），可通过 `-Dapp.log.path=...` 覆盖。
- **前端代理**：`vite.config.js` 转发 `/api` 与 `/uploads` 到 `http://localhost:8080`。

---

如发现文档与代码不一致，以代码为准，并欢迎提交 PR 修复本文档。
