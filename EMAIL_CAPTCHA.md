# 邮箱验证码：使用、不足与升级路径

> 适用范围：couple-life-backend 当前的邮箱验证码实现
> 涉及代码：`AuthServiceImpl#sendCaptcha`、`MailCaptchaSender`、`application.yml` 中的 `spring.mail.*`
> 最后更新：2026-05-28（已修复 §3 标记 ✅ 的项）

---

## 一、当前实现是什么

### 1.1 数据流

```
用户在前端填邮箱 → POST /api/auth/captcha
   → AuthServiceImpl.sendCaptcha
       1. 频控（同账号 60s 内不可重复发）
       2. 生成 6 位随机数字码
       3. 入库 login_captcha 表（5 分钟过期、used=0）
       4. 调 MailCaptchaSender.send → JavaMailSender → QQ SMTP (smtp.qq.com:465)
   → 用户邮箱收到 HTML 邮件
   → 用户在前端输入验证码 → POST /api/auth/register 或 /api/auth/login
       → verifyCaptcha 校验后立即置 used=1
```

### 1.2 关键设计决策

| 决策点 | 选择 | 原因 |
|---|---|---|
| 发件人 | 服务端固定一个 QQ 邮箱 | 全站一个出口，配置一次永久使用 |
| 收件人 | 用户自己输入的任意邮箱 | QQ/163/Gmail 都能收 |
| 凭证存放 | 环境变量 `MAIL_USERNAME` / `MAIL_PASSWORD` | 不进 git |
| 装配条件 | `@ConditionalOnExpression("'${spring.mail.username:}'.length() > 0")` | 用 SpEL 表达式做"非空字符串"判断；`@ConditionalOnProperty` 默认行为会把空串视为存在，无法实现真正的"未配置时不装配" |
| 注入方式 | `ObjectProvider<MailCaptchaSender>` 可选注入 | 邮箱未配置 → sender 为 null，仅记日志，不报错 |
| 失败策略 | 发送失败仅记日志、不抛异常 | 验证码已入库，靠前端"重发"兜底 |
| 兜底 | `app.captcha.expose-code=true` 时接口直接回显明文 | dev 联调或 SMTP 未配时可继续工作 |
| 入参校验 | `sendCaptcha` 在入口按 `type` 强校验账号格式 | LOGIN→必须手机号；REGISTER→必须手机号或邮箱；其他类型直接拒 |
| 前后端正则一致 | 同一份 phone/email 正则在前端 `utils/validators.js` 与后端 `AuthServiceImpl` 各维护一份 | 避免"前端通过、后端拒绝"的体验割裂 |

### 1.3 启动前需做的事

1. **拿 QQ 邮箱授权码**
   - 登录 mail.qq.com → 设置 → 账户
   - 开启「IMAP/SMTP 服务」，按提示发短信验证 → 拿到 16 位授权码
2. **注入环境变量**（任选一种，详见 §2）

### 1.4 验证是否真的发出去

```bash
curl -X POST http://localhost:8080/api/auth/captcha \
  -H "Content-Type: application/json" \
  -d '{"account":"测试邮箱@xxx.com","type":"REGISTER"}'
```
- 看后端日志是否打印 `验证码邮件发送成功 to=...`
- 看目标邮箱的收件箱 / 垃圾箱

---

## 二、环境变量怎么设

> 三选一即可。前两种是终端配置，第三种是 IDE 配置。

### 方式 A：终端临时（仅本次启动有效）

```bash
cd couple-life-backend
MAIL_USERNAME=xxx@qq.com MAIL_PASSWORD=授权码 mvn spring-boot:run
```

### 方式 B：终端永久（写入 `~/.zshrc`）

```bash
echo 'export MAIL_USERNAME=xxx@qq.com' >> ~/.zshrc
echo 'export MAIL_PASSWORD=授权码' >> ~/.zshrc
source ~/.zshrc
```
> 注意：只对**终端启动**有效，IDEA 里点绿色三角可能读不到（IDEA 默认不继承 `~/.zshrc`）。

### 方式 C：IDEA 启动配置（推荐，常用 IDE 启动时）

`Edit Configurations...` → 选中 `CoupleLifeApplication` → `Environment variables` 填：

```
MAIL_USERNAME=xxx@qq.com;MAIL_PASSWORD=授权码
```

---

## 三、不足之处（按严重程度排序）

> ✅ 标记为「已修复」的条目仅作历史记录保留，便于回顾设计演进

### 🔴 高风险 — 上线前必须解决

#### 1. ✅ 已修复：sendCaptcha 入参校验缺失

> 修复时间：2026-05-28
> 修复 commit：在 `AuthServiceImpl#sendCaptcha` 入口按 `type` 强校验账号格式

**原问题**：只校验了「短信登录类型必须是手机号」，注册类型可传任意字符串。

**当前实现**：
```java
if (TYPE_LOGIN_SMS.equals(dto.getType())) {
    if (!PHONE_PATTERN.matcher(dto.getAccount()).matches()) throw ...
} else if (TYPE_REGISTER.equals(dto.getType())) {
    if (!isPhone && !isEmail) throw ...
} else {
    throw new BizException("不支持的验证码类型");
}
```
+ 前端 `utils/validators.js` 集中维护正则，与后端对齐。

#### 2. ✅ 已修复：MailCaptchaSender 在 username 为空字符串时仍装配

> 修复时间：2026-05-28

**原问题**：`@ConditionalOnProperty(name = "spring.mail.username")` 配合 YAML `${MAIL_USERNAME:}` 默认空串，会被判定为"存在且非 false" → 装配出无凭证的 sender，每次发邮件都报 `535 Authentication failed`。

**当前实现**：改用 `@ConditionalOnExpression("'${spring.mail.username:}'.length() > 0")`，并在 `send()` 方法入口加 fromAddress 空值兜底。

#### 3. ✅ 已修复：RegisterDTO.password 缺 @NotBlank

> 修复时间：2026-05-28

**原问题**：仅 `@Size(min=6)` 注解，对 null 值放行（Bean Validation 规范），导致 service 层 `dto.getPassword().equals(...)` 理论上 NPE。

**当前实现**：增加 `@NotBlank(message = "密码不能为空")`，由 `@Valid` 在 controller 层拦截。

#### 4. 频控太弱，可被刷邮件

当前只有「同账号 60s 冷却」，缺少：
- **同 IP** 的全局频控（恶意者可以用脚本枚举不同邮箱地址轰炸他人）
- **每日上限**（QQ 个人邮箱发太多会被临时封 SMTP，且会被 Gmail 等收件方判垃圾邮件加剧）
- **图形验证码前置**（高敏感接口理应前置图形验证码，当前注册/短信登录的 `sendCaptcha` 是裸暴露的）

**修复**：见 §4 的 "Bucket4j / Redis 滑窗" 方案。

#### 5. 凭证只支持环境变量，多环境部署不灵活

如果将来有 dev / staging / prod 三套配置，会很乱。

**修复**：拆 `application-dev.yml` / `application-prod.yml`，配合 `SPRING_PROFILES_ACTIVE` 切换。

### 🟡 中风险 — 用户量上来需解决

#### 6. QQ 个人邮箱本身的硬限制

- **频次**：日发送 ~50–500 封不等（取决于账号活跃度），超了会被腾讯封 SMTP 几小时
- **信誉**：没有 SPF / DKIM / DMARC，发到 Gmail / Outlook 大概率进**垃圾箱**，发到 163 / qq 之间相对好
- **品牌**：发件人写着 `xxx@qq.com`，不像正经产品

#### 7. 单机内存里没异步化，发邮件阻塞业务线程

`mailSender.send()` 是同步调用，QQ SMTP 在网络抖动时单次能阻塞几秒。当前设了 5s 超时，但仍然占着 Tomcat 线程。

**修复**：把 `MailCaptchaSender.send` 包成 `@Async` 任务，或丢到 `Executor` 异步执行。

#### 8. 没有失败重试 / 死信记录

发送失败仅打日志，丢了就丢了。用户点"重发"前只能干等。

**修复**：见 §4「队列化」。

#### 9. `expose-code=true` 在生产是定时炸弹

只要忘记改成 `false`，接口会直接返回明文验证码，等于把验证码废了。

**修复**：
- 给生产 profile 强制 `expose-code: false`
- 启动时如果 `expose-code=true && spring.profiles.active=prod` → 直接 fail-fast

#### 10. 邮件模板硬编码在 Java 里

HTML 写在 `MailCaptchaSender.buildHtml`，改文案要重新编译部署。

**修复**：用 Thymeleaf / Freemarker 模板放 `resources/templates/`。

### 🟢 低风险 — 体验优化

#### 11. 没有"未收到邮件？"的产品引导

用户不知道要去看垃圾箱。前端可以加一句提示。

#### 12. 验证码长度固定 6 位

短信场景 6 位是惯例，邮箱其实可以用 8 位字母+数字增强抗暴力（5 分钟有效 + 一次性，6 位数字 100w 组合也够）。

#### 13. 没有埋点观测

发送量、失败率、各邮箱域到达率（@gmail.com / @qq.com 分别多少进垃圾）都没数据。

**修复**：接 Prometheus 埋点 `mail_sent_total{result, domain}`。

---

## 四、用户量上来后的升级路径

按"用户规模阶段"分三档，逐档迁移即可，**业务代码（AuthServiceImpl）几乎不需要改**，只换 `MailCaptchaSender` 的实现或配置。

### 阶段 1 — 个人/小团队（< 10 人/天，当前阶段）

✅ **不用改**，QQ 个人邮箱 + 现有代码足够。

剩余加固项（按优先级）：
- §9 给生产 profile 强制 `expose-code=false` 并 fail-fast
- §7 邮件发送加 `@Async` 异步化
- §4 频控加固（IP 维度 + 图形验证码前置）

### 阶段 2 — 内测/小规模上线（10–500 人/天）

**问题**：QQ 个人邮箱开始触发限频，垃圾邮件投诉增多。

**升级动作**：

1. **换企业邮箱**（最简单，零代码改动）
   - 腾讯企业邮 / 阿里云邮 / 网易企业邮箱（约 ¥300-500/年/账号）
   - 改 `application.yml` 的 SMTP 地址、账号、密码即可
   - 自动获得 SPF/DKIM 资格（厂商代配），到达率明显提升

2. **加图形验证码前置**
   - 当前 `/api/auth/captcha` 是裸接口，加一层 `captchaKey + captcha` 前置（参考已有的 `generateImageCaptcha`）
   - 阻止脚本批量调用

3. **接 Bucket4j 做 IP 频控**

   ```xml
   <dependency>
     <groupId>com.bucket4j</groupId>
     <artifactId>bucket4j-core</artifactId>
   </dependency>
   ```

   单 IP 每分钟最多 5 次、每天最多 50 次。

4. **异步化**

   ```java
   @Async("mailExecutor")
   public CompletableFuture<Boolean> send(String to, String code) { ... }
   ```

   配 `ThreadPoolTaskExecutor`（核心 4、最大 16、队列 200），失败丢进 dead-letter 表。

### 阶段 3 — 正式上线（500+ 人/天，或要求高到达率）

**问题**：企业邮箱也开始限频，且没有送达 / 打开 / 退信回调，运维瞎子。

**升级动作**：换「专业邮件 API 服务」。

| 服务 | 免费额度 | 单价 | 国内可用 | 备注 |
|---|---|---|---|---|
| **腾讯云邮件推送 SES** | 200 封/月 | ¥0.005/封 | ✅ | 国内最便宜、备案域名才能用 |
| **阿里云邮件推送** | 200 封/天 | ¥0.4/千封 | ✅ | 同上 |
| **Resend** | 3000 封/月 | $0.40/千封 | ❌（需翻墙/海外用户） | 开发者体验最好 |
| **SendGrid** | 100 封/天 | $14.95/月起 | ⚠️（部分省份不稳） | 老牌 |
| **AWS SES** | 62000 封/月（EC2 内） | $0.10/千封 | ⚠️ | 海外用户专用 |

**代码改造**：

替换 `MailCaptchaSender` 实现，但**保留接口**：

```java
public interface CaptchaMailSender {
    boolean send(String to, String code);
}

@Component
@ConditionalOnProperty(name = "app.mail.provider", havingValue = "tencent-ses")
public class TencentSesMailSender implements CaptchaMailSender { ... }

@Component
@ConditionalOnProperty(name = "app.mail.provider", havingValue = "smtp", matchIfMissing = true)
public class SmtpMailSender implements CaptchaMailSender { ... }  // 现有实现
```

`AuthServiceImpl` 改注入 `CaptchaMailSender` 接口，业务代码零改动。

**配套基础建设**：

1. **域名 + DNS**
   - 注册一个域名（如 `couple-life.com`，约 ¥55/年）
   - 配 SPF / DKIM / DMARC 三条记录（厂商控制台会给）
   - 发件人改成 `noreply@couple-life.com`，专业感拉满

2. **模板托管**
   - 邮件 HTML 抽到 Thymeleaf 模板：`resources/templates/captcha-email.html`
   - 或上传到邮件服务的模板控制台，仅传变量

3. **可观测**
   - Webhook 接入"送达 / 打开 / 退信 / 投诉" 回调
   - Prometheus 埋点：发送量、到达率（按域分维度）、平均延迟
   - Grafana 告警：失败率 > 5% 时 PageDuty

4. **安全增强**
   - Redis 滑窗替代 Bucket4j 内存频控（多实例部署时必须）
   - 加 hCaptcha / Turnstile 等行为验证码
   - 注册接口启用慢哈希、IP 黑名单库（fail2ban 风格）

---

## 五、迁移检查清单

升级到下一档时，对照这张表：

- [x] §1 入参校验（已修复 2026-05-28）
- [x] §2 ConditionalOnExpression 修复空字符串 bug（已修复 2026-05-28）
- [x] §3 RegisterDTO 密码 @NotBlank（已修复 2026-05-28）
- [ ] §4 频控已加固（IP 维度 / 图形验证码 / 日上限）
- [ ] §5 多环境 profile 拆分
- [ ] §7 邮件发送已异步
- [ ] §8 失败/退信有死信表或回调记录
- [ ] §9 生产 profile 强制 `expose-code=false` 且启动校验
- [ ] §10 邮件模板已抽出
- [ ] §13 接入观测埋点
- [ ] 接入企业邮箱 / 邮件 API
- [ ] 配置域名 SPF/DKIM/DMARC
- [ ] 安全：Redis 滑窗、行为验证码、登录失败计数

---

## 六、相关文件索引

后端：
- `couple-life-backend/src/main/java/com/love/couplelife/service/impl/AuthServiceImpl.java` — `sendCaptcha`（含格式校验、路由）/ `verifyCaptcha` / `login` / `register`
- `couple-life-backend/src/main/java/com/love/couplelife/service/impl/MailCaptchaSender.java` — SMTP 发送实现，使用 `@ConditionalOnExpression` 控制装配
- `couple-life-backend/src/main/java/com/love/couplelife/dto/RegisterDTO.java` — 注册参数 + 校验注解
- `couple-life-backend/src/main/java/com/love/couplelife/dto/LoginDTO.java` — 登录参数（password 校验在 service 层做，因为 SMS_CODE 模式不需要密码）
- `couple-life-backend/src/main/java/com/love/couplelife/dto/CaptchaDTO.java` — 验证码下发参数
- `couple-life-backend/src/main/resources/application.yml` — `spring.mail.*` / `app.captcha.*`
- `couple-life-backend/pom.xml` — `spring-boot-starter-mail` 依赖

前端：
- `couple-life-frontend/src/utils/validators.js` — 集中的手机号/邮箱正则与 el-form 校验器（`phoneValidator` / `emailValidator` / `phoneOrEmailValidator`），与后端正则保持一致
- `couple-life-frontend/src/views/Register.vue` — 注册页，使用 utils 校验器
- `couple-life-frontend/src/views/Login.vue` — 登录页（密码 + 短信两种模式），使用 utils 校验器

数据库：
- `sql/init.sql` — `login_captcha` 表结构
