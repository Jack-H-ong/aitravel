# AI 旅游攻略生成与问答助手

> 基于 AI 大模型的智能旅游攻略生成与问答平台，用户只需输入目的地、天数、预算和兴趣偏好，即可一键生成个性化行程；同时内置 AI 旅游问答助手，随时解答旅行疑问。

**仓库地址**：[https://gitee.com/novemb/aitravel](https://gitee.com/novemb/aitravel)

---

## ✨ 功能特性

### 面向用户
| 功能 | 说明 |
|------|------|
| 🏞️ **景点浏览** | 按城市、分类筛选景点，查看详情、收藏、浏览历史 |
| 🤖 **AI 攻略生成** | 输入目的地、天数、预算、偏好，大模型自动生成含每日安排、景点、美食推荐的完整行程 |
| 🗺️ **行程管理** | 我的行程列表、详情查看、手动编辑调整、删除 |
| 💬 **智能问答** | 基于大模型的旅游问答助手，结合行程上下文提供精准建议 |
| 👤 **个人中心** | 我的行程、我的收藏、浏览历史聚合展示 |

### 面向管理员
| 功能 | 说明 |
|------|------|
| 🏛️ **景点管理** | 景点信息增删改查 |
| 👥 **用户管理** | 用户列表查看、启用/禁用账号 |
| 📋 **行程管理** | 全站用户行程浏览 |
| 💬 **对话记录** | AI 对话记录查看（内容审核） |

---

## 🛠️ 技术栈

### 后端 (travel-backend)
| 技术 | 版本 | 用途 |
|------|------|------|
| Java | 21 | 运行环境 |
| Spring Boot | 3.4.1 | 应用框架 |
| MyBatis-Plus | 3.5.7 | ORM（分页、逻辑删除、条件构造器） |
| MySQL | 8.0 | 关系型数据库 |
| JWT (jjwt) | 0.12.6 | 无状态身份认证 |
| BCrypt | - | 密码加密 |
| Knife4j | 4.5.0 | API 文档（Swagger UI） |
| Hutool | 5.8.38 | 工具类库 |
| Lombok | - | 代码简化 |
| Spring 6 RestClient | - | AI 服务 HTTP 调用 |

### 前端 (travel-frontend)
| 技术 | 版本 | 用途 |
|------|------|------|
| Vue | 3.5 | UI 框架 |
| Vite | 8.3 | 构建工具 |
| Vue Router | 5.4 | 路由管理（懒加载、守卫） |
| Pinia | 4.0 | 状态管理 |
| Element Plus | 2.14 | UI 组件库 |
| Axios | 1.20 | HTTP 请求封装 |

### AI 服务
| 技术 | 说明 |
|------|------|
| 通义千问 (DashScope) | 默认 Provider，可通过配置切换 |
| 扩展设计 | `AiGateway` 接口抽象，新增 Provider 只需实现接口 + 配置切换 |

---

## 📁 项目结构

```
aitravel/
├── travel-backend/              # Spring Boot 后端
│   ├── sql/init.sql             # 数据库初始化脚本
│   ├── src/main/java/com/aitravel/
│   │   ├── controller/          # REST 控制器（6 个）
│   │   ├── service/             # 业务接口
│   │   │   └── impl/            # 业务实现
│   │   │   └── ai/              # AI 网关（PromptBuilder、AiResponseParser、TongyiAiClient）
│   │   ├── mapper/              # MyBatis-Plus Mapper
│   │   ├── entity/              # 数据库实体
│   │   ├── dto/                 # 入参 DTO
│   │   ├── vo/                  # 出参 VO
│   │   ├── common/              # Result、异常、错误码
│   │   ├── security/            # JWT 过滤器、拦截器、注解
│   │   └── config/              # WebMvc、MyBatis-Plus、AI 装配
│   └── src/main/resources/application.yml
│
└── travel-frontend/              # Vue3 前端
    ├── src/
    │   ├── views/               # 页面组件
    │   │   ├── admin/           # 后台管理页面
    │   │   └── ...              # 用户页面
    │   ├── components/          # 公共组件（卡片、时间轴、对话气泡）
    │   ├── api/                 # 按业务域划分的接口模块
    │   ├── store/               # Pinia 状态（user/scenic/trip/chat）
    │   ├── router/              # 路由表 + 全局守卫
    │   └── utils/               # axios 封装、Token 读写、常量
    └── vite.config.js
```

---

## 🚀 快速开始

### 环境要求

| 环境 | 版本 |
|------|------|
| JDK | 21+ |
| Node.js | 18+ |
| MySQL | 8.0+ |
| Maven | 3.8+（或使用自带的 `mvnw`） |

### 1. 数据库初始化

```bash
# 连接 MySQL 后执行
mysql -u root -p < travel-backend/sql/init.sql
```

脚本会：
- 创建 `ai_travel` 数据库
- 创建 8 张业务表（user、scenic_spot、trip、trip_day、trip_spot、favorite、browse_history、ai_chat）
- 插入种子数据（2 个用户、14 个景点）

**默认账号**：
| 用户名 | 密码 | 角色 |
|--------|------|------|
| admin | admin123 | 管理员 |
| demo | user123 | 普通用户 |

### 2. 后端配置与启动

**2.1 环境变量（必须配置）**

```bash
# 数据库密码（如与默认不同）
export DB_PASSWORD=your_db_password

# JWT 密钥（至少 32 字符，生产环境必须修改）
export JWT_SECRET=your-production-jwt-secret-key-32chars-minimum

# AI 服务 API Key（通义千问）
export API_KEY=your-dashscope-api-key
```

**2.2 修改配置文件**（如不使用环境变量）

编辑 `travel-backend/src/main/resources/application.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/ai_travel?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: root
    password: Aa112211   # ← 修改为你的数据库密码

jwt:
  secret: change-me-in-production-0123456789abcdef0123456789abcdef   # ← 生产环境必须修改

ai:
  api:
    provider: tongyi
    api-key: ${API_KEY}   # ← 从环境变量 API_KEY 读取
    model: qwen-turbo
    base-url: https://dashscope.aliyuncs.com
    timeout: 30000
    temperature: 0.7
    max-tokens: 3000
```

**2.3 启动后端**

```bash
cd travel-backend

# 使用 Maven Wrapper（推荐）
./mvnw spring-boot:run    # Windows: mvnw.cmd spring-boot:run

# 或使用全局 Maven
mvn spring-boot:run
```

后端启动在 `http://localhost:8080`

**API 文档**：启动后访问 `http://localhost:8080/doc.html`（Knife4j / Swagger UI）

### 3. 前端启动

```bash
cd travel-frontend

# 安装依赖
npm install

# 开发模式（Vite dev server，自动代理 /api → http://localhost:8080）
npm run dev
```

前端启动在 `http://localhost:5173`

### 4. 构建与部署

**前端构建**：
```bash
cd travel-frontend
npm run build
# 产物在 dist/，由 Nginx 托管
```

**后端打包**：
```bash
cd travel-backend
./mvnw clean package -DskipTests    # Windows: mvnw.cmd clean package -DskipTests
# 产物在 target/travel-backend-0.0.1-SNAPSHOT.jar

# 运行
java -jar target/travel-backend-0.0.1-SNAPSHOT.jar
```

---

## 🔌 API 接口概览

### 统一约定
- **Base URL**: `/api`
- **鉴权头**: `Authorization: Bearer {token}`
- **统一响应**: `{ "code": 200, "message": "success", "data": {...} }`
- **错误码**: `200` 成功 / `400` 参数错误 / `401` 未认证 / `403` 无权限 / `404` 不存在 / `500` 服务异常 / `5001` AI 服务异常

### 接口列表

| 模块 | 接口 | 方法 | 权限 |
|------|------|------|------|
| 认证 | `/api/auth/register` | POST | 公开 |
| 认证 | `/api/auth/login` | POST | 公开 |
| 认证 | `/api/auth/me` | GET | 登录 |
| 认证 | `/api/auth/update` | PUT | 登录 |
| 景点 | `/api/scenic/list` | GET | 公开（可选鉴权） |
| 景点 | `/api/scenic/{id}` | GET | 公开（可选鉴权） |
| 景点 | `/api/scenic/favorite` | POST | 登录 |
| 景点 | `/api/scenic/my-favorites` | GET | 登录 |
| 行程 | `/api/trip/generate` | POST | 登录 |
| 行程 | `/api/trip/list` | GET | 登录 |
| 行程 | `/api/trip/{id}` | GET/PUT/DELETE | 登录 |
| 问答 | `/api/chat` | POST | 登录 |
| 问答 | `/api/chat/history` | GET | 登录 |
| 个人中心 | `/api/profile/history` | GET | 登录 |
| 后台 | `/api/admin/scenic` | GET/POST | 管理员 |
| 后台 | `/api/admin/scenic/{id}` | PUT/DELETE | 管理员 |
| 后台 | `/api/admin/users` | GET | 管理员 |
| 后台 | `/api/admin/users/{id}/status` | PUT | 管理员 |
| 后台 | `/api/admin/trips` | GET | 管理员 |
| 后台 | `/api/admin/chats` | GET | 管理员 |

**详细接口定义**：启动后端后访问 `http://localhost:8080/doc.html`（Knife4j 自动生成）

---

## 🏗️ 系统架构

### 分层架构

```
┌─────────────────────────────────────────────────────────┐
│                     前端 Vue3 SPA                        │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐    │
│  │  Router   │ │   Pinia   │ │  Axios   │ │  Element+ │    │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘    │
└────────────────────────┬────────────────────────────────┘
                         │ HTTP /api/*
                         ▼
┌─────────────────────────────────────────────────────────┐
│              Spring Boot 3.4 单体后端                    │
│  ┌──────────────────────────────────────────────────┐   │
│  │  JwtAuthFilter → AuthInterceptor → Controller    │   │
│  └──────────────────────────────────────────────────┘   │
│  ┌──────────────────────────────────────────────────┐   │
│  │              Service 业务层（6 模块）              │   │
│  │  UserService / ScenicService / TripService ...   │   │
│  └──────────────────────┬───────────────────────────┘   │
│  ┌──────────────────────┼───────────────────────────┐   │
│  │              Mapper (MyBatis-Plus)               │   │
│  └──────────────────────┬───────────────────────────┘   │
└─────────────────────────┼───────────────────────────────┘
                          │
              ┌───────────┼───────────┐
              ▼           ▼           ▼
          MySQL 8.0    DashScope    (可扩展)
```

### 模块划分

| 模块 | 职责 |
|------|------|
| M-USER | 用户注册、登录、JWT 核发、信息更新 |
| M-SCENIC | 景点 CRUD、收藏、浏览记录、列表查询 |
| M-TRIP | AI 攻略生成、行程 CRUD、归属校验 |
| M-CHAT | AI 智能问答、对话历史、多轮上下文 |
| M-PROFILE | 个人中心聚合查询 |
| M-ADMIN | 后台管理（复用各业务模块能力） |
| M-SECURITY | JWT 过滤器、拦截器、权限裁决 |
| M-AI | 大模型网关（接口抽象 + Provider 实现） |
| M-COMMON | 统一响应、全局异常、错误码 |
| M-PERSIST | 实体、Mapper、MyBatis-Plus 配置 |

### 关键设计决策

| 决策 | 说明 |
|------|------|
| **AI 调用在事务外** | 避免长事务占用数据库连接；先查数据 → 事务外调 AI → 最后开事务落库 |
| **行程删除策略** | `trip` 逻辑删除（`deleted=1`）；`trip_day`、`trip_spot` 物理删除（子表无 `deleted` 字段） |
| **收藏计数维护** | `favorite_count` 在收藏/取消收藏的**同一事务**内原子 ±1，由唯一键兜底并发 |
| **可选鉴权** | 公开接口（景点列表/详情）携带合法 Token 时注入身份，返回 `isFavorite`；无效 Token 不报错 |
| **AI Provider 抽象** | `AiGateway` 接口 + `@ConditionalOnProperty` 配置切换，新增 Provider 零调用方改动 |
| **响应解析容错** | 剥离 Markdown 代码围栏、容忍前后缀文字、字段缺失给默认值 |
| **ThreadLocal 身份传递** | 过滤器设置 → 请求结束 finally 清理，防止线程池复用串号 |

### JWT 认证链路

```
请求 → JwtAuthFilter（只解析 UserContext）→ AuthInterceptor（按注解裁决）→ Controller（UserContext.getUserId()）
```

---

## 🗄️ 数据库设计

### ER 关系

```
user ──1:N── trip
user ──1:N── favorite ──N:1── scenic_spot
user ──1:N── browse_history ──N:1── scenic_spot
user ──1:N── ai_chat
trip ──1:N── trip_day
trip ──1:N── trip_spot ──N:1── scenic_spot
```

### 表清单（8 张）

| 表 | 说明 | 逻辑删除 |
|----|------|----------|
| `user` | 用户表（BCrypt 密码、角色） | ✅ |
| `scenic_spot` | 景点信息（分类、价格、评分、浏览量） | ✅ |
| `trip` | 行程主表（标题、城市、天数、预算） | ✅ |
| `trip_day` | 每日行程（主题、午餐、晚餐、提示） | ❌ |
| `trip_spot` | 行程景点关联（scenic_id 可空） | ❌ |
| `favorite` | 收藏（唯一键 user_id + scenic_id） | ❌ |
| `browse_history` | 浏览记录（UPSERT 维护最新时间） | ❌ |
| `ai_chat` | AI 对话记录（只追加，每条问答产生 user + assistant 两条） | ❌ |

---

## ⚠️ 安全提示

1. **生产环境必须修改默认密码**：
   - 数据库密码（`application.yml` 的 `spring.datasource.password`）
   - JWT 密钥（`jwt.secret`，至少 32 字符）
   - 默认账号密码（`admin`/`admin123`、`demo`/`user123`）

2. **API Key 不要硬编码**：通过环境变量 `API_KEY` 注入

3. **密码已加密存储**：使用 BCrypt（Spring Security Crypto 工具包），禁止明文

4. **SQL 注入防护**：全部使用 MyBatis-Plus 参数化查询

5. **越权防护**：所有按 ID 访问用户私有资源的接口（行程详情/修改/删除）先校验归属当前 userId，不满足返回 403

---

## 📄 License

MIT License
