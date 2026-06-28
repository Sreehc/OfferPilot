# OfferPilot UX Spec

## 设计目标

- 从通用后台升级为 AI 求职训练工作台。
- 统一浅色模式体验，深色模式保留但不作为第一阶段重点。
- 让 Chat、Agent、Dashboard、Interview、Resume、Applications 更像业务工具。

## 全局规则

- 统一 Design Token、颜色、圆角、阴影和动效。
- 所有交互元素具备 hover、active、focus-visible、disabled、loading。
- 关键页面支持键盘访问和清晰标签。
- 移动端不能依赖横向滚动完成主要操作。

## 页面结构

### Dashboard

- 今日主任务
- 薄弱项
- AI 建议
- 下一步行动

### Chat

- 会话列表
- 消息流
- 来源、工具和产物侧栏
- 附件、重命名、删除确认、重新生成、反馈

### Agent

- Run 列表
- 执行时间线
- 工具调用
- 审批卡
- 输出产物

### Interview / Resume / Applications

- 保留主工作区
- 第二阶段再加页面内 Copilot
- 允许更业务化的布局替代纯表格页

### Knowledge / Question / Wrong / Review

- 以检索、作答、复盘和下一步动作作为主线

### Admin

- 用户、内容、AI、系统、治理分区
- AI 日志和运行治理按面板方式呈现

## 阶段口径

- 第一阶段：全站基础视觉和交互统一。
- 第二阶段：简历、投递、面试页的页面内 Copilot。

## 验收关注点

- 主要任务是否一眼可见
- 关键信息是否首屏可读
- 移动端是否可用
- AI 内容是否有明确来源、状态和产物
