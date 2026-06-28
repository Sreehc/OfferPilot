# OfferPilot

OfferPilot 是一个面向 Java 求职者的 AI 面试训练与求职管理平台。当前项目采用 Spring Boot 单体后端 + React SPA 前端，把题库训练、知识问答、模拟面试、学习计划、简历整理、投递管理、复习收藏和管理治理串成一条连续工作流。

## 当前能力

- **求职训练工作台**：首页展示训练概览、弱项、最近面试、计划进度、简历状态和投递推进情况
- **题库训练**：按关键字、分类、题型、难度、岗位方向和标签筛题，查看题目详情、追问建议和常见错误
- **知识库与 RAG 问答**：上传 Markdown / TXT / PDF / Word 文档，解析切片后支持检索、引用回答和流式问答
- **模拟面试**：支持文字面试、语音面试、岗位准备、Copilot 准备、实时 Copilot 会话和录屏复盘
- **学习计划**：生成 7 / 14 / 30 天计划，执行每日任务并刷新后续节奏
- **简历助手**：上传并解析简历，维护版本，查看项目拆分、项目追问、自我介绍、面试简历提纲和简历评分
- **投递管理**：记录岗位、分析 JD、更新状态、维护事件时间线
- **复习与收藏**：管理错题、SM-2 复习队列、收藏内容和收藏标签
- **社区与通知**：支持提问、回答、投票、排行榜和站内通知
- **自适应推荐**：基于训练信号输出能力画像、推荐题目和推荐面试方向
- **Agent 工作台**：维护用户模型提供商配置，创建、审批、拒绝和取消 Agent 运行
- **数据分析与导出**：查看趋势、效率、学习洞察、能力主题回顾，导出个人数据
- **管理后台**：管理用户、题库、分类、知识文档、社区内容、AI 日志、系统配置、登录日志和面试治理

## 主线流程

```text
进入工作台
  -> 题库训练定位薄弱点
  -> 知识库与问答补齐理解
  -> 模拟面试验证表达
  -> 学习计划安排接下来几天的训练
  -> 简历助手整理项目与自我介绍
  -> 投递管理记录和推进岗位
  -> 复习、收藏、推荐和数据分析辅助复盘
  -> 后台治理内容、AI 调用和系统配置
```

## 技术栈

| 层级 | 技术 |
|------|------|
| 后端 | Spring Boot 3.3.5、Java 17、MyBatis-Plus 3.5.7、MySQL 8、Redis 8 |
| 安全 | Spring Security、JWT 无状态认证、Redis Token 黑名单、CORS 白名单、2FA、设备管理 |
| AI | Spring AI 兼容层、`LlmGateway` / `EmbeddingGateway` 抽象、用户 Provider 配置、AI 调用日志与配额 |
| 文档解析 | Apache Tika（Markdown / TXT / PDF / Word） |
| 前端 | React、Vite、TypeScript、Ant Design、Ant Design X、React Router、Zustand、TanStack Query、ECharts |
| 部署 | Docker Compose、Nginx、GitHub Actions、GHCR |

## 目录结构

```text
OfferPilot
├── backend/       Spring Boot 后端
├── frontend/      React 前端应用
├── sql/           数据库初始化与增量迁移脚本
├── deploy/        生产部署 Compose、Nginx 与重部署脚本
├── docs/          当前产品、技术、API 与迁移文档
└── docker-compose.yml
```

## 环境要求

- JDK 17+
- Maven 3.9+
- Node.js 20+
- MySQL 8.x
- Redis 7.x+

## 本地启动

### 1. 准备共享基础设施

当前工作区已经统一使用外部共享基础设施，不再建议由 `OfferPilot` 自己拉起 MySQL / Redis。

默认对接：

- MySQL: `127.0.0.1:3306`
- Redis: `127.0.0.1:6379`

如果你已经按工作区统一方案启动了 `/Users/cheers/Desktop/workspace/infrastructure`，这里不需要再额外启动数据库或缓存。

`deploy/docker-compose.yml` 和 `deploy/docker-compose.prod.yml` 现在只负责启动 `OfferPilot` 前后端，并连接到现有共享基础设施。

### 2. 初始化数据库

第一次创建数据库时执行：

```bash
mysql -h 127.0.0.1 -P 3306 -u root -p offerpilot < sql/init.sql
mysql -h 127.0.0.1 -P 3306 -u root -p offerpilot < sql/initdata.sql
```

如果数据库来自旧版本，再按需要执行 `sql/migrate_*.sql` 中的增量脚本，例如 Agent、Provider、录屏复盘、Copilot 会话和用户 Provider 配置相关迁移。

### 3. 启动后端

```bash
cd backend
mvn spring-boot:run
```

默认端口：`8080`。

常用入口：

- Knife4j：`http://localhost:8080/doc.html`
- Swagger UI：`http://localhost:8080/swagger-ui.html`
- 健康检查：`http://localhost:8080/actuator/health`

### 4. 启动前端

```bash
cd frontend
npm install
npm run dev
```

默认端口：`5173`。Vite 已配置 `/api` 代理到 `http://localhost:8080`。

## 默认测试账号

初始化数据包含演示账号，常用本地登录信息：

- 用户名：`demo`
- 密码：`123456`

## 环境变量

### 后端

| 变量 | 说明 | dev 默认值 | prod 默认值 |
|------|------|------------|-------------|
| `OFFERPILOT_DB_HOST` | MySQL 主机 | `127.0.0.1` | `mysql` 或部署文件指定值 |
| `OFFERPILOT_DB_PORT` | MySQL 端口 | `3306` | `3306` |
| `OFFERPILOT_DB_NAME` | 数据库名 | `offerpilot` | `offerpilot` |
| `OFFERPILOT_DB_USERNAME` | 数据库用户 | `root` | `offerpilot` |
| `OFFERPILOT_DB_PASSWORD` | 数据库密码 | `root` | 必填 |
| `OFFERPILOT_REDIS_HOST` | Redis 主机 | `127.0.0.1` | `127.0.0.1` |
| `OFFERPILOT_REDIS_PORT` | Redis 端口 | `6379` | `6379` |
| `OFFERPILOT_REDIS_PASSWORD` | Redis 密码 | 空 | 空 |
| `OFFERPILOT_JWT_SECRET` | JWT 密钥，至少 32 字符 | 开发默认值 | 必填 |
| `OFFERPILOT_JWT_EXPIRE_SECONDS` | JWT 有效期秒数 | `86400` | `86400` |
| `OFFERPILOT_CORS_ORIGINS` | CORS 允许域名 | `http://localhost:5173` | 必填 |
| `OFFERPILOT_LLM_ENABLED` | LLM 开关 | `false` | `true` |
| `OFFERPILOT_LLM_BASE_URL` | LLM API 地址 | `https://api.openai.com/v1` | `https://api.openai.com/v1` |
| `OFFERPILOT_LLM_API_KEY` | LLM API Key | 空 | 启用 LLM 时必填 |
| `OFFERPILOT_LLM_MODEL` | LLM 模型 | `gpt-4.1-mini` | `gpt-4.1-mini` |
| `OFFERPILOT_EMBEDDING_ENABLED` | Embedding 开关 | `false` | `false` |
| `OFFERPILOT_EMBEDDING_BASE_URL` | Embedding API 地址 | 同 LLM 地址 | 同 LLM 地址 |
| `OFFERPILOT_EMBEDDING_API_KEY` | Embedding API Key | 同 LLM Key | 同 LLM Key |
| `OFFERPILOT_EMBEDDING_MODEL` | Embedding 模型 | `text-embedding-3-small` | `text-embedding-3-small` |
| `OFFERPILOT_EMBEDDING_DIMENSIONS` | 向量维度 | `1536` | `1536` |
| `OFFERPILOT_VECTOR_THRESHOLD` | 向量相似度阈值 | `0.3` | `0.3` |
| `OFFERPILOT_RATE_LIMIT` | 限流窗口内最大请求数 | `60` | `60` |
| `OFFERPILOT_RATE_WINDOW` | 限流窗口秒数 | `60` | `60` |
| `OFFERPILOT_AI_QUOTA` | 每用户每日 AI 调用配额 | `100` | `100` |
| `OFFERPILOT_AUTH_EXPOSE_DEBUG_CODES` | 是否返回调试验证码 | `true` | `false` |
| `OFFERPILOT_STORAGE_LOCAL_ROOT` | 本地文件存储根目录 | `./data/storage` | `/var/lib/offerpilot/storage` |
| `OFFERPILOT_HTTP_BIND_HOST` | 前端监听地址 | - | `127.0.0.1` |
| `OFFERPILOT_HTTP_PORT` | 前端 HTTP 端口 | - | `8092` |
| `OFFERPILOT_BACKEND_PORT` | 后端 HTTP 端口 | `8080` | `8080` |

完整生产模板见 `.env.example` 和 `deploy/.env.example`。

### 前端

| 变量 | 说明 | 默认值 |
|------|------|--------|
| `VITE_API_BASE_URL` | API 基础路径 | `/api` |
| `VITE_APP_TITLE` | 应用标题 | `OfferPilot` |

## 生产部署

### 仓库根目录构建部署

```bash
cp .env.example .env
# 编辑 .env，至少填写 OFFERPILOT_DB_PASSWORD、OFFERPILOT_JWT_SECRET、OFFERPILOT_CORS_ORIGINS

docker compose --env-file .env up -d --build
```

这套 Compose 现在只启动 `OfferPilot` 前后端，并直接连接共享 MySQL / Redis。

### deploy 目录生产部署

```bash
cp deploy/.env.example deploy/.env
# 编辑 deploy/.env

cd deploy
docker compose --env-file .env -f docker-compose.prod.yml up -d --build
```

`deploy/docker-compose.prod.yml` 和 `deploy/docker-compose.release.yml` 默认假设 MySQL / Redis 都由宿主机上的共享基础设施提供。线上自动发布相关文件为 `.github/workflows/deploy.yml`、`deploy/docker-compose.release.yml` 和 `deploy/redeploy.sh`。

## 运行测试与质量检查

```bash
# 后端测试
cd backend
mvn test

# 前端单元测试
cd frontend
npm run test

# 前端类型检查与构建
npm run build

# 前端 lint
npm run lint
```

## 技术亮点

- **主线工作台**：首页围绕当前主任务、计划进度、简历状态和投递推进组织
- **AI 运行时抽象**：`LlmGateway` / `EmbeddingGateway` 接口 + Spring AI 兼容层，逐步承接 chat、embedding 和 agent runtime 能力
- **用户 Provider 配置**：支持按用户维护模型服务配置，并在 Agent 工作台中检查可用性
- **Agent Runtime 演进**：已具备 run、审批、provider gating、结果消费以及初始 definition/runtime skeleton
- **RAG 检索**：Redis Stack 向量检索 + 关键词混合检索，Apache Tika 解析多格式文档后自动切分向量化
- **面试链路扩展**：文字、语音、岗位准备、Copilot 准备、实时 Copilot 和录屏复盘共用面试域模型
- **安全设计**：JWT 无状态认证 + Redis Token 黑名单 + CORS 白名单 + 2FA + 设备撤销 + 登录日志
- **速率与配额**：Redis 滑动窗口限流 + 每用户每日 LLM 调用配额
- **可观测与治理**：请求日志、Actuator 健康检查、AI 调用日志、运行治理和后台配置覆盖
- **前端质量**：TypeScript strict、路由懒加载、ErrorBoundary、主题切换、Vitest 测试和 Lighthouse 配置

## 文档

- 当前文档入口：[docs/README.md](./docs/README.md)
- 产品需求：[docs/prd.md](./docs/prd.md)
- 技术方案：[docs/tech-design.md](./docs/tech-design.md)
- API 规范：[docs/api-spec.md](./docs/api-spec.md)
- 数据库设计：[docs/database-design.md](./docs/database-design.md)
- UX 规格：[docs/ux-spec.md](./docs/ux-spec.md)
- 迁移历史：[docs/history.md](./docs/history.md)

## License

MIT
