# Changelog

## Sprint 4 — 部署与收尾 (2026-05-03)

### Day 1: 生产部署方案

- 后端 Dockerfile（eclipse-temurin:17-jre-alpine）
- 前端 Dockerfile（多阶段构建：Node 20 构建 + Nginx 部署）
- `deploy/nginx.conf` + `frontend/nginx.conf`：gzip 压缩、静态资源缓存、API 反向代理
- `frontend/.env.production`：`VITE_API_BASE_URL=/api`
- `deploy/docker-compose.prod.yml`：MySQL 8、Redis 7、backend、frontend，健康检查、数据卷持久化
- `spring-boot-starter-actuator`：`/actuator/health` 端点，安全白名单放行
- `application-prod.yml`：Actuator 暴露 health/info 端点

### Day 2: 前端生产优化

- `vite.config.ts`：`build.target: 'es2020'`，`manualChunks` 分离 element-plus、echarts、vendor
- 路由懒加载：所有页面组件改为 `() => import(...)` 动态导入

### Day 3: 文档更新

- README.md：更新实现状态、添加生产部署说明和测试运行说明
- 04-数据库设计.md：补充 `wrong_question` 唯一约束和 knowledge_chunk 检索说明
- CHANGELOG.md：本文件

---

## Sprint 3 — 体验优化与性能 (2026-05-02 ~ 2026-05-03)

### Day 1: 前端体验优化

- Vite 代理配置：`server.proxy` 将 `/api` 代理到 `localhost:8080`
- Interview 页面：5 分钟倒计时、评分数字动画（ease-out cubic）、Ctrl+Enter 快捷提交
- Chat 页面：消息自动滚动到底部（nextTick）、Enter 发送提示
- Dashboard：ECharts 雷达图展示薄弱点

### Day 2: 后端性能优化

- `ChatServiceImpl.send()`：事务范围缩小，LLM 调用移到事务外
- `KnowledgeRetrievalServiceImpl`：DB 级 LIKE 预过滤替代全量加载
- `DashboardServiceImpl`：Redis 缓存（5 分钟 TTL），数据变更时主动清除
- `OpenAiCompatibleLlmGateway`：connectTimeout=10s、readTimeout=可配置、5xx 自动重试 1 次

### Day 3: 后端代码质量

- 所有 DTO 补全 Jakarta Validation 注解
- `wrong_question` 唯一约束 + 迁移脚本
- 结构化日志：DefaultAiOrchestratorService、JwtAuthenticationFilter
- `logback-spring.xml`：dev=console，prod=console+文件+错误文件，50MB 轮转，30 天保留

### Day 4: 前端代码质量

- TypeScript strict 模式（strict、noUncheckedIndexedAccess、noUnusedLocals/Params）
- `storage.ts` 类型修复：`getUser()` 返回 `UserInfo | null`
- AdminPage 拆分为 AdminCategoryTab、AdminQuestionTab、AdminKnowledgeTab
- DashboardPage 拆分为 DashboardGuideCard、DashboardMetrics、DashboardInterviews、DashboardWeakPoints
- ErrorBoundary.vue + `app.config.errorHandler` 全局错误捕获

### Day 5: 联调与回归测试

- 修复 ChatServiceImpl @Transactional 自调用不生效（@Lazy self-injection）
- 修复 InterviewServiceImpl.answer() LLM 调用在事务内（拆分为 3 阶段）
- 所有 13 个 Controller 添加 OpenAPI 注解（@Tag、@Operation、@Parameter）

---

## Sprint 2 — 安全加固与质量基线 (2026-05-02)

### Day 1: 安全修复

- JWT 登出：Redis Token 黑名单
- CORS 配置收紧：环境变量配置允许域名
- JWT Secret 启动校验：非 dev profile 下检查是否为默认值
- Chat XSS 防护：DOMPurify sanitize Markdown 渲染

### Day 2: 代码规范

- `.gitignore` 修复：不再排除 docs/ 和 deploy/
- ESLint + Prettier 配置
- Checkstyle 插件 + checkstyle.xml

### Day 3-4: 核心模块测试

- 后端：Auth、Chat、Dashboard、Interview、Security 单元测试
- 前端：Vitest + auth store、http 工具单元测试

### Day 5: 接口分页

- 后端：统一分页返回 `PageResult<T>`
- 前端：el-pagination 组件适配

---

## Sprint 1 — 补齐核心闭环 (2026-05-02)

### 核心功能

- Interview 模块：AI 评分、自动入错题、session 状态流转
- Wrong 模块：错题列表、掌握状态切换、删除
- Plan 模块：AI 生成学习计划、任务状态追踪
- Dashboard：数据聚合、首次引导、薄弱点分析

### 前端对接

- 所有模块 API 对接 + 页面重写
- 空状态引导、Loading 状态、错误处理
