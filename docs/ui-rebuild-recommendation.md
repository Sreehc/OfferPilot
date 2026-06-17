# OfferPilot 前端 UI 重构执行计划

## 1. 目标

本文件把原先的 UI 重构建议，整理成可执行的前端开发计划和任务拆分，作为 OfferPilot 当前前端重构的主文档。

本轮重构的结论已经固定：

- 前端采用 `React + TypeScript + Vite + Ant Design`
- Agent 体验层单独设计，不再沿用普通后台表单/表格范式
- `Ant Design` 负责产品骨架、表单、表格、设置页和后台治理
- `Ant Design X`、`assistant-ui` 负责 Agent 聊天与消息流
- `CopilotKit` 只做单点试点，不做全站铺开
- 第一阶段不改后端接口协议，只要求前端兼容现有 API

历史上的 Vue / Element Plus 方案文档不再作为本轮执行基线，遇到冲突时以本文件为准。

## 2. 当前代码状态

### 2.1 已完成的基线

当前 `frontend/` 已经完成 React 化重建，已经有这些结果：

- 入口改为 `frontend/src/main.tsx`
- 应用根组件改为 `frontend/src/app/App.tsx`
- 路由、守卫、应用壳层、主题、错误边界、离线提示都已接入
- 请求层已经拆成 `frontend/src/api/client.ts` 和 `frontend/src/api/modules/*`
- 鉴权状态已经迁移到 `frontend/src/features/auth/authStore.ts`
- 主题状态已经迁移到 `frontend/src/features/theme/themeStore.ts`
- 共享页面组件已经集中到 `frontend/src/modules/common/*`
- Agent 组件已经集中到 `frontend/src/components/agent/*`
- 大部分主路由页面已经改为 React 页面
- `npm run lint`、`npm run test`、`npm run build` 已可通过

### 2.2 当前仍需收口的部分

现阶段不是“是否能跑”的问题，而是“是否足够产品化”的问题。剩余工作主要集中在：

- 主线业务页的深度交互和写操作收口
- Agent 的流式对话、工具调用、审批链路和产物展示
- 设置页和管理后台的完整表单、筛选、分页、导出和权限控制
- 测试、可访问性、包体积和旧实现退役

## 3. 选型原则

### 3.1 主 UI 和 Agent UI 的分工

| 场景 | 选型 | 说明 |
|---|---|---|
| 产品骨架、表格、表单、后台、设置 | `Ant Design` | 负责全局一致性、效率和治理类页面 |
| Agent 聊天输入、气泡、消息流 | `Ant Design X` + `assistant-ui` | 负责对话体验、流式消息和工具调用表现 |
| Agent 工作流试点 | `CopilotKit` | 只在一个真实业务流程里验证，不全站依赖 |
| 设计参考而非主依赖 | `shadcn/ui`、`Material`、`stitch` | 可参考交互语言和视觉组织，但不作为当前主栈 |

### 3.2 采用和不采用的边界

- `shadcn/ui` 更适合作为可组合原语和局部交互参考，不适合作为当前项目主 UI 底座
- `Material` 更像另一套完整设计语言，切换成本太高，不适合本轮重构
- `stitch` 可用于设计表达参考，但不是前端运行时依赖
- 当前项目已经明确选择 `Ant Design` 作为主产品 UI，Agent 只在局部引入专门组件栈

## 4. 代码基线对照

| 路径 | 责任 | 当前状态 |
|---|---|---|
| `frontend/src/app/*` | 应用入口、Provider、路由、壳层 | 已完成基础搭建 |
| `frontend/src/api/client.ts` | 请求封装、错误处理、设备信息 | 已完成基础封装 |
| `frontend/src/api/modules/*` | 各业务域 API 模块 | 已拆分完成 |
| `frontend/src/components/agent/*` | Agent 体验组件 | 已存在基础组件 |
| `frontend/src/components/feedback/*` | 错误、离线、通知等全局反馈 | 已接入 |
| `frontend/src/modules/common/*` | 页面级通用容器和列表/表格块 | 已接入 |
| `frontend/src/pages/*` | 业务页面 | 大部分已迁移为 React 页面 |
| `frontend/src/features/auth/*` | 登录态、恢复、登出 | 已接入 |
| `frontend/src/features/theme/*` | 深浅色主题 | 已接入 |
| `frontend/src/styles/*` | 全局样式和主题 token | 已接入 |
| `frontend/src/test/*` | 测试初始化 | 已接入 |

### 4.1 当前路由覆盖

当前前端已经覆盖这些主要页面：

- 登录、注册、找回密码、2FA
- Dashboard
- Chat
- Agent Workbench
- Question
- Knowledge
- Interview
- Study Plan
- Resume
- Applications
- Analytics
- Favorites
- Wrong Book
- Review
- Community
- Settings
- Admin

结论：这次工作不是“修补旧页面”，而是已经进入“重建后的产品化收口”阶段。

## 5. 详细开发计划

### 阶段 0：范围冻结与基线确认

#### 目标

把本轮工作明确为 React 重建，冻结继续用 Vue / Element Plus 修补的思路。

#### 任务拆分

- 以 `frontend/` 的 React 结构作为唯一执行基线
- 旧 Vue 页面只保留为迁移参考
- 第一阶段不引入后端接口重构
- 明确 Agent 层和主产品层分离

#### 验收

- 文档、代码和路由都对齐 React 方案
- 不再出现“继续在 Vue 上局部翻修”的执行路径

### 阶段 1：工程骨架和运行时

#### 目标

保证前端具备稳定运行、稳定构建和稳定鉴权的基础。

#### 任务拆分

- React 入口和根组件
- `QueryClientProvider`、`ConfigProvider`、全局 `AntApp`
- 路由守卫
- 登录态恢复
- 全局错误边界
- 离线提示
- 页面壳层和移动端导航

#### 当前状态

这部分已经落地，主要工作变成收口和稳定化。

#### 验收

- `npm run build` 通过
- `npm run test` 通过
- `npm run lint` 通过
- 未登录访问受保护页会跳转登录
- admin 页面具备权限控制

### 阶段 2：主线业务页面

#### 目标

把用户最常用的训练、面试、简历、投递和分析链路补成可用主线。

#### 任务拆分

| 模块 | 当前状态 | 下一步任务 | 验收 |
|---|---|---|---|
| Dashboard | 已有数据驱动页面 | 补真实指标、跳转入口、行动建议和复习联动 | 能作为主入口串起后续流程 |
| Question / Knowledge | 已有列表和基础交互 | 补筛选、分页、详情、上传、重切分、重索引 | 可以完成题库和知识库管理动作 |
| Study Plan / Review | 已有页面基础 | 补计划生成、任务状态更新、今日复习和评分 | 可以形成训练-复习闭环 |
| Interview | 已有模拟面试和详情框架 | 补 JD 备面、录音复盘、历史追踪和实时辅助位 | 可以完整查看一次面试过程 |
| Resume | 已有助手页 | 补上传、解析、版本、评分和建议输出 | 可以得到可执行简历建议 |
| Applications | 已有看板和详情页 | 补状态流转、事件记录、分析视图 | 可以跟踪投递全流程 |
| Analytics | 已有统计页 | 补能力趋势、效率、画像和洞察 | 可以反映训练和投递结果 |

#### 验收

- 每个模块都具备 `loading`、`empty`、`error` 状态
- 写操作有明确反馈
- 页面之间的跳转可以串成主流程

### 阶段 3：Agent UI 层

#### 目标

把 Agent 体验从普通后台页面中独立出来，形成专门的消息流和任务流 UI。

#### 组件拆分

- `AgentComposer`
- `ToolCallCard`
- `ThoughtTimeline`
- `HumanApprovalBar`
- `GeneratedArtifactCard`
- `AgentVendorsPanel`

#### 页面拆分

- `/chat` 负责多轮对话和消息流
- `/agent` 负责 run、step、审批、结果消费

#### 集成任务

- 接入 `Ant Design X` 的输入、气泡和 prompt 能力
- 接入或改造 `assistant-ui` 的消息流能力
- 统一 `/api/chat` 和 `/api/agent/runs` 的状态模型
- 保留 SSE / streaming 接口位
- 让工具调用、思考链和审批从纯文本变成结构化 UI

#### 验收

- 用户可以发起对话
- 用户可以查看 run 和 step
- 用户可以批准、拒绝或取消
- 工具调用和产物展示不是纯文本拼接

### 阶段 4：学习辅助与社区

#### 目标

把收藏、错题、复习和社区能力补齐，形成训练侧的支撑闭环。

#### 任务拆分

- 收藏列表和删除
- 错题本和复习巩固
- 复习任务状态更新
- 社区列表、提问、详情和排行榜

#### 验收

- 列表、详情、空态和删除逻辑统一
- 社区互动链路可用
- 训练侧数据可以回流到主线页面

### 阶段 5：设置与后台治理

#### 目标

恢复账号安全、Provider 配置和管理后台能力。

#### 任务拆分

| 模块 | 当前状态 | 下一步任务 | 验收 |
|---|---|---|---|
| 账号资料 | 已有基础页 | 补头像上传、资料展示和编辑收口 | 可以查看和维护个人资料 |
| 安全能力 | 已有基础页 | 补邮箱验证、2FA、恢复码和登出逻辑 | 安全流程可独立完成 |
| 设备与历史 | 已有基础页 | 补设备撤销和登录历史浏览 | 可以管理活跃设备 |
| Provider 配置 | 已有基础页 | 补 readiness、校验和保存反馈 | 可以查看外部服务状态 |
| 数据导出 | 已有基础页 | 补导出状态和下载处理 | 可以导出个人数据 |
| Admin 概览 | 已有基础页 | 补筛选、分页、状态标签和操作反馈 | 非 admin 无法进入后台 |
| 内容与知识治理 | 已有基础页 | 补审核、分类、题库和知识库管理 | 后台治理动作可闭环 |
| AI 日志与系统配置 | 已有基础页 | 补日志浏览、配置保存和运行态查看 | 可以定位系统行为 |

#### 验收

- 非 admin 用户无法进入管理后台
- 所有管理页都有筛选和分页
- 操作都有成功、失败和加载反馈

### 阶段 6：质量收口和旧实现退役

#### 目标

完成测试、性能收敛、可访问性和旧 Vue 代码退役。

#### 任务拆分

- 单元测试
  - auth store
  - route guards
  - API client
  - provider readiness
  - Agent 状态转换
- 页面 smoke test
  - login
  - dashboard
  - chat
  - agent
  - interview
  - resume
  - applications
  - admin
- 性能收口
  - 路由级 lazy load
  - 大包体积分片
  - Agent 依赖分包
- 可访问性
  - 键盘可达
  - 对比度
  - 语义化按钮和提示
- 旧实现退役
  - 清理残留 Vue / Pinia / Element Plus 引用
  - 清理不再需要的旧测试和旧入口

#### 验收命令

```bash
cd frontend
npm run lint
npm run test
npm run build
```

## 6. 并行顺序

- `API 模块` 和 `页面` 可以在接口稳定后并行推进
- `Agent UI` 可以和主线页面并行，但最终要等 auth 和 API client 稳定
- `设置 / 后台治理` 适合放在主线完成后推进
- `质量收口` 需要在功能基本稳定后集中完成

## 7. 风险与控制

| 风险 | 影响 | 控制方式 |
|---|---|---|
| 重构范围过大 | 周期失控 | 按阶段验收，不跨阶段扩张 |
| 后端同步改动 | 计划叠加 | 第一阶段保持接口兼容 |
| Agent 组件接入不稳 | 返工 | 先在 `/chat` 和 `/agent` 小范围落地 |
| CopilotKit 过早铺开 | 依赖过重 | 只做单点试点 |
| 旧逻辑遗漏 | 功能回归 | 用路由、API 和页面清单逐项对照 |
| 大包体积 | 首屏变慢 | 路由级懒加载和按需分包 |
| 设计系统分裂 | 风格发散 | 统一由 Ant Design 约束产品骨架 |

## 8. 里程碑

### Milestone 1：骨架稳定

范围：

- React + Vite
- Provider
- 路由
- API client
- auth

验收：

- 可以登录
- 可以访问受保护路由
- 可以构建

### Milestone 2：主线闭环

范围：

- Dashboard
- Question
- Knowledge
- Study Plan
- Interview
- Resume
- Applications
- Analytics

验收：

- 用户可以走完训练、面试、简历、投递的主线流程

### Milestone 3：Agent 可用

范围：

- Chat
- Agent Workbench
- ToolCallCard
- ThoughtTimeline
- HumanApprovalBar

验收：

- 用户可以发起对话、查看 run、审批或拒绝、消费结果

### Milestone 4：治理和收口

范围：

- 设置
- 后台治理
- 测试
- 旧 Vue 退役

验收：

- 正式路由全部由 React 覆盖
- 前端不再依赖 Vue / Pinia / Element Plus

## 9. 后续建议拆分文档

如果继续细拆，建议再拆出两份执行文档：

- `docs/frontend-react-route-map.md`：逐路由迁移清单
- `docs/frontend-agent-ui-spec.md`：Agent UI 交互、状态和异常规格

## 10. 完成审计

本轮收口已经把上一版“未 100% 完成”的部分推进到可验收状态。

### 10.1 已完成项

| 范围 | 完成证据 |
|---|---|
| Agent 状态模型 | 已新增 `frontend/src/components/agent/agentModel.ts`，统一 `run / step / tool_call / approval / artifact / message` 映射 |
| Agent streaming | 已新增 `frontend/src/components/agent/useAgentStreaming.ts`，`/chat` 具备 `/api/chat/stream` 优先、普通发送兜底的链路 |
| 工具调用 UI | `ToolCallCard` 已展示状态、参数、耗时和错误信息 |
| 人工审批 UI | `HumanApprovalBar` 已接入审批可见性、loading、批准、拒绝、取消 |
| Agent 产物 UI | `GeneratedArtifactCard` 已支持复制、打开链接、类型标签和长内容展开 |
| Chat 页面 | 已统一使用 Agent 状态模型和 streaming hook |
| Agent Workbench | 已统一使用 Agent 状态模型，run、step、tool、artifact、approval 展示不再各自硬编码 |
| 题库 | 已补关键词、分类、难度筛选和删除确认 |
| 知识库 | 已补关键词、分类、索引状态筛选、上传分类、重索引确认和删除确认 |
| 投递看板 | 已补关键词、状态筛选、刷新分析确认，API 支持查询参数 |
| 管理后台 | 已补用户封禁/解封确认、内容审核确认、AI 日志 provider/model/error 字段 |
| 权限门禁 | 已补 `RequireAuth` admin / anonymous route guard 测试 |
| API helper | 已补错误消息提取测试 |
| 页面 smoke | 已补 dashboard、chat、agent、interview、resume、applications、admin 渲染 smoke |
| 测试环境 | 已补 `matchMedia` 和 `ResizeObserver` mock，保证 Ant Design 组件能在 jsdom 下稳定测试 |

### 10.2 验收命令

本轮完成后以下命令均已通过：

```bash
cd frontend
npm run lint
npm run test
npm run build
```

### 10.3 非阻断说明

`npm run build` 仍会输出两类依赖层面的警告：

- `node-fetch` 触发的浏览器外部化提示，来源是 Agent 相关依赖链。
- `antd`、`agent-ui`、`echarts` 等 chunk 大小提示。

当前已经使用路由级 lazy load 和 `manualChunks` 做了基础分包；这些警告不阻断构建，也不影响本轮 UI 重构完成验收。后续如果要继续压首屏包体积，应单独做依赖替换、按页面动态引入或移除未使用 Agent 依赖的专项优化。
