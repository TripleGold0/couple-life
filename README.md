# Couple Life · 情侣生活记录平台

Couple Life 是一个面向情侣的轻量级生活记录 Web 应用。项目采用前后端分离架构，支持两位用户通过邀请码绑定关系后，共同记录每日心情、旅行足迹、相册回忆，并提供情感顾问 Agent 与情侣共养电子宠物等扩展功能。

![Java](https://img.shields.io/badge/Java-17-orange.svg)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.5-brightgreen.svg)
![Vue](https://img.shields.io/badge/Vue-3.5-42b883.svg)
![Vite](https://img.shields.io/badge/Vite-6.0-646cff.svg)
![MySQL](https://img.shields.io/badge/MySQL-8.0%2B-4479A1.svg)

## 功能概览

- 用户注册：用户名、昵称、性别、手机号或邮箱，以及对应验证码。
- 登录鉴权：账号密码登录、图形验证码、手机号验证码登录即注册、JWT 无状态鉴权。
- 情侣绑定：通过邀请码建立情侣关系，可维护恋爱开始日期。
- 首页看板：展示恋爱天数、打卡统计、旅行和相册概览等数据。
- 每日打卡：记录每日心情 emoji、心情文字和打卡内容。
- 旅行日志：基于高德地图标记旅行地点，支持经纬度、城市、日期、双方感受和配图。
- 共享相册：上传照片、按日期分组、评论照片、批量导出 ZIP。
- 情感顾问 Agent：后端通过 OpenAI 兼容接口调用 LLM，默认配置 Pollinations.ai，可切换 OpenAI、DeepSeek、Groq、Ollama 等兼容服务。
- 电子宠物：情侣共同选择宠物，支持喂食、抚摸、玩耍、亲密度成长、每日属性衰减和个人侧悬浮显示开关。
- 文件上传：图片上传到后端本地目录，并通过 `/uploads/**` 暴露访问。

## 技术栈

### 后端

- Java 17
- Spring Boot 3.3.5
- Spring Web / Validation / Security / Mail
- JWT：jjwt 0.12.6
- MyBatis-Plus 3.5.16
- MySQL 8.x
- Lombok

### 前端

- Vue 3.5
- Vite 6
- Vue Router 4
- Pinia
- Element Plus
- Axios
- ECharts
- 高德地图 JS API

## 项目结构

```text
couple_life/
├── couple-life-backend/                 # Spring Boot 后端服务
│   ├── src/main/java/com/love/couplelife/
│   │   ├── common/                      # 统一返回、业务异常、全局异常处理
│   │   ├── config/                      # Security、JWT、MyBatis、Web、日志配置
│   │   ├── controller/                  # REST API 控制器
│   │   ├── dto/                         # 请求 DTO
│   │   ├── entity/                      # 数据库实体
│   │   ├── mapper/                      # MyBatis-Plus Mapper
│   │   ├── service/                     # 业务接口与实现
│   │   ├── util/                        # JWT、安全上下文、文件上传工具
│   │   └── vo/                          # 响应 VO
│   ├── src/main/resources/
│   │   ├── application.yml              # 后端配置
│   │   └── logback-spring.xml           # 日志配置
│   └── uploads/                         # 本地上传文件目录
├── couple-life-frontend/                # Vue 3 前端应用
│   ├── src/
│   │   ├── api/                         # 前端 API 封装
│   │   ├── assets/styles/               # 全局样式
│   │   ├── components/                  # 通用组件
│   │   ├── layouts/                     # 主布局
│   │   ├── router/                      # 路由配置
│   │   ├── stores/                      # Pinia 状态
│   │   ├── utils/                       # 请求、校验、日志等工具
│   │   └── views/                       # 页面视图
│   ├── .env.example                     # 前端环境变量示例
│   └── vite.config.js                   # Vite 配置与开发代理
├── sql/init.sql                         # 数据库初始化脚本
├── EMAIL_CAPTCHA.md                     # 邮箱验证码说明
├── FEATURES.md                          # 功能细节说明
└── README.md
```

## 环境要求

- JDK 17+
- Maven 3.8+
- Node.js 18+
- MySQL 8.0+

## 快速开始

### 1. 初始化数据库

```bash
mysql -uroot -p < sql/init.sql
```

该脚本会创建 `couple_life` 数据库，并初始化用户、情侣关系、验证码、每日打卡、旅行、相册、评论、电子宠物等业务表。

### 2. 配置后端

编辑 `couple-life-backend/src/main/resources/application.yml`，至少确认数据库连接信息：

```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/couple_life?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
    username: root
    password: your_mysql_password

app:
  jwt:
    secret: replace-with-a-random-secret-at-least-32-bytes
    expire-hours: 168
  upload-dir: uploads
  captcha:
    expose-code: true
    sms-cooldown-seconds: 60
  openai:
    api-key: ${OPENAI_API_KEY:}
    base-url: ${OPENAI_BASE_URL:https://text.pollinations.ai/openai}
    model: ${OPENAI_MODEL:openai}
    temperature: 0.7
```

邮箱验证码使用 Spring Mail。未配置 `MAIL_USERNAME` 时，系统只记录日志，不真实发送邮件。

```bash
export MAIL_USERNAME=your_mail@qq.com
export MAIL_PASSWORD=your_mail_auth_code
```

Windows PowerShell 可使用：

```powershell
$env:MAIL_USERNAME="your_mail@qq.com"
$env:MAIL_PASSWORD="your_mail_auth_code"
```

### 3. 启动后端

```bash
cd couple-life-backend
mvn spring-boot:run
```

后端默认监听 `http://localhost:8080`。

### 4. 配置并启动前端

前端可复制 `.env.example` 为 `.env.local` 后按需修改：

```env
VITE_BACKEND_TARGET=http://localhost:8080
VITE_DEV_PORT=5173
```

启动开发服务：

```bash
cd couple-life-frontend
npm install
npm run dev
```

前端默认监听 `http://localhost:5173`。Vite 开发代理会将 `/api` 和 `/uploads` 转发到 `VITE_BACKEND_TARGET`。

### 5. 构建前端

```bash
cd couple-life-frontend
npm run build
```

构建产物位于 `couple-life-frontend/dist`，可由 Nginx 等静态服务托管，并通过反向代理将 `/api` 和 `/uploads` 转发到后端。

## 前端路由

```text
/                  Landing Page
/login             登录页
/register          注册页
/app/home          首页看板
/app/checkin       每日打卡
/app/travel        旅行地图日志
/app/album         情侣相册
/app/counselor     情感顾问 Agent
/app/pet           电子宠物
/app/profile       个人信息与情侣绑定
```

除 `/`、`/login`、`/register` 外，其余页面需要本地存在登录 token。

## 后端接口概览

后端统一返回结构为：

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

公开接口：

- `POST /api/auth/captcha`：发送短信或邮箱验证码
- `GET /api/auth/image-captcha`：获取图形验证码
- `POST /api/auth/register`：注册
- `POST /api/auth/login`：登录
- `/uploads/**`：访问已上传图片

需要 JWT 的接口：

- `GET /api/user/me`：当前用户信息
- `PUT /api/user/profile`：更新个人资料
- `POST /api/user/complete-profile`：短信登录后完善资料
- `PUT /api/user/pet-display`：设置个人侧宠物悬浮展示
- `POST /api/couple/bind`：绑定情侣关系
- `POST /api/couple/unbind`：解除情侣关系
- `GET /api/home/summary`：首页汇总数据
- `POST /api/checkins`：创建或更新每日打卡
- `GET /api/checkins/calendar`：我的打卡日历
- `GET /api/checkins/couple`：情侣打卡记录
- `GET|POST|PUT|DELETE /api/travels`：旅行记录管理
- `POST /api/album/photos`：上传相册照片
- `GET /api/album/photos`：相册列表
- `POST /api/album/photos/{photoId}/comments`：添加照片评论
- `GET /api/album/photos/{photoId}/comments`：照片评论列表
- `POST /api/album/photos/export`：批量导出照片 ZIP
- `POST /api/upload/image`：上传图片
- `POST /api/counselor/chat`：情感顾问对话
- `GET /api/pet/current`：当前宠物
- `GET /api/pet/types`：可选宠物类型
- `GET /api/pet/selection/list`：宠物选择请求列表
- `POST /api/pet/selection/request`：发起选择或更换宠物请求
- `POST /api/pet/selection/{id}/agree`：同意宠物选择请求
- `POST /api/pet/selection/{id}/reject`：拒绝宠物选择请求
- `POST /api/pet/interact`：宠物互动

## 关键配置说明

### JWT

登录成功后，后端返回 JWT。前端会将 token 存入 `localStorage`，并在后续请求中自动携带 `Authorization` 请求头。

生产环境必须替换 `app.jwt.secret`，建议使用不少于 32 字节的随机字符串。

### 验证码

- 邮箱验证码可通过 QQ 邮箱 SMTP 发送，详见 `EMAIL_CAPTCHA.md`。
- `app.captcha.expose-code=true` 时，接口可能回显验证码，便于本地调试。
- 生产环境必须将 `app.captcha.expose-code` 设置为 `false`。
- 手机短信验证码当前为业务占位，未接入真实短信网关时只适合本地联调。

### 文件上传

默认上传目录为后端工作目录下的 `uploads/`，后端通过 `/uploads/**` 暴露为静态资源。

### 地图

旅行页面使用高德地图 JS API。当前地图脚本配置位于 `couple-life-frontend/index.html`，上线前应替换为自己的高德地图 Key 与安全密钥，并按高德控制台要求配置域名白名单。

### 情感顾问 Agent

后端默认使用 OpenAI 兼容协议：

```yaml
app:
  openai:
    api-key: ${OPENAI_API_KEY:}
    base-url: ${OPENAI_BASE_URL:https://text.pollinations.ai/openai}
    model: ${OPENAI_MODEL:openai}
```

如需切换供应商，可通过环境变量或前端请求中的 `baseUrl`、`apiKey`、`model` 覆盖。

## 生产部署注意事项

- 替换数据库账号密码，避免使用 root 账号直连业务库。
- 替换 `app.jwt.secret`。
- 设置 `app.captcha.expose-code=false`。
- 配置真实邮件账号或短信网关。
- 替换高德地图 Key 与安全密钥。
- 收敛 CORS 来源，不建议生产环境继续允许任意来源。
- 上传目录建议放到独立持久化目录，并配置备份策略。
- 前端 History 模式部署时，需要服务端 fallback 到 `index.html`。

## 相关文档

- `FEATURES.md`：功能流程、数据结构和模块说明。
- `EMAIL_CAPTCHA.md`：邮箱验证码配置、联调与升级说明。
- `sql/init.sql`：数据库表结构与初始化数据。

