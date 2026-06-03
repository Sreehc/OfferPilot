# OfferPilot 全栈实施计划

> 涉及三大块：收藏功能、简历解析增强、简历制作占位页
> 覆盖数据库 DDL、后端 Java 模块、前端 Vue 页面
> 后端遵循现有模式：Spring Boot 3 + MyBatis-Plus + 原始 SQL DDL

---

## 一、收藏功能（全栈新建）

### 1.1 数据库（`sql/init.sql` 追加）

新增两张表：

```sql
-- ============================================================
-- 收藏表
-- ============================================================
CREATE TABLE IF NOT EXISTS favorite (
    id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    target_type VARCHAR(32) NOT NULL COMMENT '收藏目标类型: knowledge / question / community',
    target_id BIGINT NOT NULL COMMENT '收藏目标 ID',
    tag_id BIGINT DEFAULT NULL COMMENT '自定义分组 ID，可为空',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_favorite_user (user_id),
    INDEX idx_favorite_user_type (user_id, target_type),
    UNIQUE KEY uk_favorite_user_target (user_id, target_type, target_id),
    CONSTRAINT fk_favorite_user FOREIGN KEY (user_id) REFERENCES user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 收藏分组表
-- ============================================================
CREATE TABLE IF NOT EXISTS favorite_tag (
    id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    name VARCHAR(64) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_favorite_tag_user (user_id),
    CONSTRAINT fk_favorite_tag_user FOREIGN KEY (user_id) REFERENCES user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

### 1.2 后端 — Entity

**`backend/src/main/java/com/offerpilot/favorite/entity/Favorite.java`**

```java
@TableName("favorite")
@Data
@EqualsAndHashCode(callSuper = true)
public class Favorite extends BaseEntity {
    private Long userId;
    private String targetType;   // knowledge / question / community
    private Long targetId;
    private Long tagId;
}
```

**`backend/src/main/java/com/offerpilot/favorite/entity/FavoriteTag.java`**

```java
@TableName("favorite_tag")
@Data
@EqualsAndHashCode(callSuper = true)
public class FavoriteTag extends BaseEntity {
    private Long userId;
    private String name;
    private Integer sortOrder;
}
```

### 1.3 后端 — Mapper

**`backend/.../favorite/mapper/FavoriteMapper.java`** — extends `BaseMapper<Favorite>`
**`backend/.../favorite/mapper/FavoriteTagMapper.java`** — extends `BaseMapper<FavoriteTag>`

### 1.4 后端 — DTO / VO

**DTO（请求）：**

```java
// FavoriteUpsertRequest.java
@Data
public class FavoriteUpsertRequest {
    @NotBlank
    private String targetType;   // knowledge / question / community
    @NotNull
    private Long targetId;
    private Long tagId;
}

// FavoriteBatchDeleteRequest.java
@Data
public class FavoriteBatchDeleteRequest {
    @NotEmpty
    private List<Long> ids;
}

// FavoriteTagUpsertRequest.java
@Data
public class FavoriteTagUpsertRequest {
    @NotBlank
    private String name;
    private Integer sortOrder;
}

// FavoriteQuery.java
@Data
public class FavoriteQuery extends PageQuery {
    private String targetType;   // 可选筛选
    private Long tagId;          // 可选筛选
    private String keyword;      // 搜索标题
}
```

**VO（响应）：**

```java
// FavoriteVO.java
@Data @Builder
public class FavoriteVO {
    private Long id;
    private String targetType;
    private Long targetId;
    private String title;        // 从关联表拼装
    private String summary;      // 从关联表拼装
    private String categoryName;
    private Long tagId;
    private String tagName;
    private LocalDateTime createTime;
}

// FavoriteTagVO.java
@Data @Builder
public class FavoriteTagVO {
    private Long id;
    private String name;
    private Integer count;       // 该分组下的收藏数量
    private Integer sortOrder;
}

// FavoriteStatsVO.java
@Data @Builder
public class FavoriteStatsVO {
    private Integer total;
    private Integer knowledgeCount;
    private Integer questionCount;
    private Integer communityCount;
    private Integer todayCount;
}
```

### 1.5 后端 — Service

**`FavoriteService.java`（接口）**

```java
public interface FavoriteService {
    PageResult<FavoriteVO> list(Long userId, FavoriteQuery query);
    FavoriteStatsVO stats(Long userId);
    FavoriteVO add(Long userId, FavoriteUpsertRequest request);
    void remove(Long userId, Long favoriteId);
    void batchRemove(Long userId, List<Long> ids);
    boolean isFavorited(Long userId, String targetType, Long targetId);
    List<FavoriteTagVO> listTags(Long userId);
    FavoriteTagVO createTag(Long userId, FavoriteTagUpsertRequest request);
    void deleteTag(Long userId, Long tagId);
    void updateFavoriteTag(Long userId, Long favoriteId, Long tagId);
}
```

**`FavoriteServiceImpl.java`（实现）要点：**

- `add()`: 先查 `uk_favorite_user_target` 唯一索引防重复，已存在则直接返回
- `remove()` / `batchRemove()`: 执行前做 ownership 校验
- `list()`: 关联查询拼装 title / summary — 根据 targetType 分别查 `knowledge_doc`、`question`、`community_question`，用 `selectBatchIds` 批量加载后 Map 拼装
- `stats()`: 用 `selectCount` + `LambdaQueryWrapper` 按 type 分组统计
- `isFavorited()`: 提供给其他模块快速判断当前用户是否已收藏某项
- `listTags()` / `createTag()` / `deleteTag()`: 标准 CRUD，带 ownership 校验
- `updateFavoriteTag()`: 修改 favorite 记录的 `tagId`

### 1.6 后端 — Controller

**`FavoriteController.java`**

```
@RestController
@RequestMapping("/api/favorites")
@Tag(name = "收藏", description = "收藏管理与分组")

GET    /                    → list(userId, query)          → Result<PageResult<FavoriteVO>>
GET    /stats               → stats(userId)                → Result<FavoriteStatsVO>
POST   /                    → add(userId, request)         → Result<FavoriteVO>
DELETE /{id}                → remove(userId, id)           → Result<Void>
POST   /batch-delete        → batchRemove(userId, ids)     → Result<Void>
GET    /check               → isFavorited(userId, type, targetId) → Result<Boolean>
GET    /tags                → listTags(userId)              → Result<List<FavoriteTagVO>>
POST   /tags                → createTag(userId, request)   → Result<FavoriteTagVO>
DELETE /tags/{tagId}         → deleteTag(userId, tagId)     → Result<Void>
PUT    /{id}/tag            → updateFavoriteTag(userId, id, tagId) → Result<Void>
```

### 1.7 前端 — API 模块（`frontend/src/api/favorites.ts` 新建）

对接上述后端接口，使用 `request` 工具。

### 1.8 前端 — 类型定义（`frontend/src/types/api.ts` 补充）

```ts
export type FavoriteTargetType = 'knowledge' | 'question' | 'community'

export interface FavoriteItem {
  id: number
  targetType: FavoriteTargetType
  targetId: number
  title: string
  summary?: string
  categoryName?: string
  tagId?: number
  tagName?: string
  createdAt: string
}

export interface FavoriteTagItem {
  id: number
  name: string
  count: number
  sortOrder: number
}

export interface FavoriteStats {
  total: number
  knowledgeCount: number
  questionCount: number
  communityCount: number
  todayCount: number
}
```

### 1.9 前端 — 路由

`src/router/index.ts` 在 children 中新增：

```ts
{
  path: 'favorites',
  name: 'favorites',
  component: () => import('@/pages/favorites/FavoritesPage.vue'),
  meta: { title: '我的收藏' }
}
```

### 1.10 前端 — 导航（`NavRail.vue`）

- 现有 `/knowledge` → label 改为 `"知识库"`，hint 改为 `"查看推荐资料和个人文档"`
- 新增 `/favorites` → label `"收藏"`，hint `"收藏的资料、题目和社区回答"`，group `'辅助强化'`

### 1.11 前端 — 页面（`src/pages/favorites/FavoritesPage.vue`）

```
FavoritesPage（桌面端双栏布局，移动端单列）
├── 左侧主区域
│   ├── 顶部统计栏（shell-section-card，4 个 stat 卡片：总数/资料/题目/社区）
│   ├── 收藏列表
│   │   ├── 每项卡片包含：
│   │   │   ├── 类型标签（hard-chip：资料 / 题目 / 社区）
│   │   │   ├── 标题 + 摘要
│   │   │   ├── 分组标签（detail-pill）
│   │   │   ├── 收藏时间
│   │   │   ├── "去查看" RouterLink（按 targetType 跳 /knowledge、/question、/community/question/:id）
│   │   │   └── 取消收藏按钮（el-popconfirm）
│   │   └── 批量操作栏（全选、批量取消收藏）
│   ├── 空状态（EmptyState icon="inbox"）
│   └── 分页（el-pagination）
└── 右侧侧边栏（desktop sticky）
    ├── 搜索框（el-input keyword）
    ├── 类型筛选（el-select: 全部/资料/题目/社区）
    ├── 分组筛选（el-select: 全部/自定义分组列表）
    ├── 排序选择（el-select: 时间倒序/正序）
    └── 分组管理（新增分组按钮 + 分组列表，支持删除）
```

### 1.12 前端 — 其他页面联动

| 页面 | 改动 |
|------|------|
| `KnowledgePage.vue` | 文档卡片增加收藏 icon 按钮，调用 `POST /api/favorites`，加载时查询收藏状态 |
| `QuestionBankPage.vue` | 题目卡片增加收藏 icon 按钮，调用 `POST /api/favorites` |
| `CommunityQuestionDetail.vue` | 增加收藏按钮 |
| `AppLayout.vue` | 全局搜索项 `{ label: '我的收藏', path: '/favorites' }`（替换原有 `/knowledge?section=favorites`） |

---

## 二、简历解析增强（全栈增量）

### 2.1 数据库（`sql/init.sql` 追加）

新增简历版本表：

```sql
-- ============================================================
-- 简历版本历史表
-- ============================================================
CREATE TABLE IF NOT EXISTS resume_version (
    id BIGINT PRIMARY KEY,
    resume_file_id BIGINT NOT NULL COMMENT '关联 resume_file.id',
    user_id BIGINT NOT NULL,
    version INT NOT NULL DEFAULT 1,
    snapshot_json TEXT COMMENT '该版本快照 JSON（保存 resume_file + resume_project 全量数据）',
    change_summary VARCHAR(512) DEFAULT NULL COMMENT '变更摘要',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_resume_version_file (resume_file_id),
    INDEX idx_resume_version_user (user_id),
    CONSTRAINT fk_resume_version_file FOREIGN KEY (resume_file_id) REFERENCES resume_file(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

### 2.2 后端 — 简历评分

**新增 Entity: 无（纯计算逻辑，不落库）**

**Service 层补充 `ResumeService` 接口新增方法：**

```java
ResumeScoreVO score(Long userId, Long resumeId);
```

**`ResumeScoreVO.java`（新增 VO）**

```java
@Data @Builder
public class ResumeScoreVO {
    private Integer overallScore;        // 0-100
    private Integer completenessScore;   // 完整度
    private Integer keywordCoverage;     // 关键词覆盖率
    private Integer atsCompatibility;    // ATS 兼容性
    private List<ResumeSuggestionVO> suggestions;
}
```

**`ResumeSuggestionVO.java`（新增 VO）**

```java
@Data @Builder
public class ResumeSuggestionVO {
    private String field;        // summary / skills / projects / education
    private String severity;     // info / warn / critical
    private String message;
}
```

**`ResumeServiceImpl.score()` 实现逻辑（纯规则引擎，不调 AI）：**

1. 加载 `ResumeFile` + 关联 `ResumeProject` 列表
2. **完整度评分**（40 分权重）：
   - summary 非空 +8，长度 > 50 字 +4
   - skills 非空且 >= 3 项 +8
   - education 非空 +6
   - selfIntro 非空 +8，长度 > 100 字 +6
   - projects >= 1 个 +6，>= 2 个 +4
3. **关键词覆盖**（30 分权重）：
   - skills 中匹配 projects 里的 techStack 关键词，匹配率 * 30
4. **ATS 兼容性**（30 分权重）：
   - title 非空 +10
   - projects 每个都有 achievement（量化结果）+10
   - projects 每个都有 responsibility +10
5. 生成 suggestions 列表，每个缺失项对应一条 suggestion

**Controller 层补充：**

```java
@Operation(summary = "简历评分")
@GetMapping("/{resumeId}/score")
public Result<ResumeScoreVO> score(
    @Parameter(description = "简历 ID") @PathVariable Long resumeId) {
    return Result.success(resumeService.score(currentUserId(), resumeId));
}
```

### 2.3 后端 — 简历版本管理

**新增 Entity：`ResumeVersion.java`**

```java
@TableName("resume_version")
@Data
@EqualsAndHashCode(callSuper = true)
public class ResumeVersion extends BaseEntity {
    private Long resumeFileId;
    private Long userId;
    private Integer version;
    private String snapshotJson;
    private String changeSummary;
}
```

**新增 Mapper：`ResumeVersionMapper.java`** — extends `BaseMapper<ResumeVersion>`

**新增 VO：`ResumeVersionVO.java`**

```java
@Data @Builder
public class ResumeVersionVO {
    private Long id;
    private Long resumeFileId;
    private Integer version;
    private String changeSummary;
    private LocalDateTime createTime;
}
```

**Service 层补充：**

```java
List<ResumeVersionVO> listVersions(Long userId, Long resumeId);
ResumeFileVO restoreVersion(Long userId, Long versionId);
```

**实现逻辑：**

- `listVersions()`: 查询 `resume_version` 表，按 version 倒序
- `restoreVersion()`: 加载 version 快照 JSON，反序列化后写回 `resume_file` + `resume_project`，并自动创建新版本记录
- 在 `update()` 方法中，每次保存修改前自动创建版本快照（记录变更摘要如 "修正摘要"、"新增项目：xxx"）

**Controller 层补充：**

```java
@Operation(summary = "简历版本历史")
@GetMapping("/{resumeId}/versions")
public Result<List<ResumeVersionVO>> versions(
    @Parameter(description = "简历 ID") @PathVariable Long resumeId) {
    return Result.success(resumeService.listVersions(currentUserId(), resumeId));
}

@Operation(summary = "回滚到指定版本")
@PostMapping("/versions/{versionId}/restore")
public Result<ResumeFileVO> restoreVersion(
    @Parameter(description = "版本 ID") @PathVariable Long versionId) {
    return Result.success(resumeService.restoreVersion(currentUserId(), versionId));
}
```

### 2.4 前端 — API 补充（`src/api/resume.ts`）

```ts
export const fetchResumeScoreApi = (resumeId: string) => {
  return request<ResumeScoreVO>({ url: `/resume/${resumeId}/score`, method: 'get' })
}

export const fetchResumeVersionsApi = (resumeId: string) => {
  return request<ResumeVersionVO[]>({ url: `/resume/${resumeId}/versions`, method: 'get' })
}

export const restoreResumeVersionApi = (versionId: string) => {
  return request<ResumeFileDetail>({ url: `/resume/versions/${versionId}/restore`, method: 'post' })
}
```

### 2.5 前端 — 类型补充（`src/types/api.ts`）

```ts
export interface ResumeScoreVO {
  overallScore: number
  completenessScore: number
  keywordCoverage: number
  atsCompatibility: number
  suggestions: ResumeSuggestionVO[]
}

export interface ResumeSuggestionVO {
  field: string
  severity: 'info' | 'warn' | 'critical'
  message: string
}

export interface ResumeVersionVO {
  id: string
  resumeFileId: string
  version: number
  changeSummary: string
  createTime: string
}
```

### 2.6 前端 — 页面改动（`ResumeAssistantPage.vue`）

**新增功能一：简历评分卡片**

插入位置：步骤 2（检查简历内容）和步骤 3（检查项目追问）之间。

```
<article class="shell-section-card p-5 sm:p-6">
  <h3>简历评分</h3>
  ├── 评分概览区（三列布局）
  │   ├── 综合分数环形进度（CSS conic-gradient + 数字叠加）
  │   ├── 完整度 / 关键词覆盖 / ATS 兼容性 三个小指标条
  │   └── 整体评价文字
  └── 优化建议列表
      ├── severity 图标（info=蓝、warn=黄、critical=红）
      ├── field 标签（detail-pill）
      └── message 说明
</article>
```

**新增功能二：版本历史卡片**

插入位置：左侧边栏"当前简历"卡片下方。

```
<article class="shell-section-card p-5 sm:p-6">
  <h3>版本历史</h3>
  ├── 版本列表（时间线样式）
  │   每项：版本号 · 修改时间 · 变更摘要
  │   点击 "回滚" → el-popconfirm 确认 → 调 restoreResumeVersionApi
  └── 空状态："暂无版本记录"
</article>
```

**新增功能三：面试联动增强**

- 步骤 3 项目追问，每个追问问题旁增加 `<RouterLink :to="/chat">单独练习</RouterLink>` 按钮
- 步骤 5 增加文案提示："模拟面试时会自动加载你的简历上下文"

### 2.7 后端 — 改动文件清单

| 文件 | 操作 |
|------|------|
| `sql/init.sql` | 追加 `resume_version` 表 DDL |
| `resume/entity/ResumeVersion.java` | 新建 |
| `resume/mapper/ResumeVersionMapper.java` | 新建 |
| `resume/vo/ResumeScoreVO.java` | 新建 |
| `resume/vo/ResumeSuggestionVO.java` | 新建 |
| `resume/vo/ResumeVersionVO.java` | 新建 |
| `resume/service/ResumeService.java` | 新增 `score()`、`listVersions()`、`restoreVersion()` 方法签名 |
| `resume/service/impl/ResumeServiceImpl.java` | 实现评分逻辑、版本快照、版本回滚；`update()` 中增加自动保存版本 |
| `resume/controller/ResumeController.java` | 新增 `GET /{resumeId}/score`、`GET /{resumeId}/versions`、`POST /versions/{versionId}/restore` |

---

## 三、简历制作占位页 `/resume-craft`（纯前端）

### 3.1 后端

无需改动。

### 3.2 前端 — 路由（`src/router/index.ts`）

```ts
{
  path: 'resume-craft',
  name: 'resume-craft',
  component: () => import('@/pages/resume-craft/ResumeCraftPage.vue'),
  meta: { title: '简历制作' }
}
```

### 3.3 前端 — 导航（`NavRail.vue`）

将现有 `/resume` 那条 nav 拆分为两条：

| 路由 | label | hint | group |
|------|-------|------|-------|
| `/resume-craft` | 简历制作 | 从零开始制作一份专业简历 | 主线训练 |
| `/resume` | 简历解析 | 上传已有简历，AI 整理成面试提纲 | 主线训练 |

### 3.4 前端 — 页面（`src/pages/resume-craft/ResumeCraftPage.vue`）

```vue
<template>
  <div class="space-y-6">
    <section class="shell-section-card p-6 sm:p-8">
      <EmptyState
        icon="document"
        title="简历制作即将上线"
        description="我们正在开发从零制作简历的功能。在此之前，你可以先使用简历解析来整理已有的简历。"
      >
        <template #action>
          <RouterLink to="/resume" class="hard-button-primary">
            去简历解析
          </RouterLink>
        </template>
      </EmptyState>
    </section>
  </div>
</template>
```

### 3.5 前端 — 全局搜索（`AppLayout.vue`）

```ts
{ label: '简历制作', path: '/resume-craft' }
```

---

## 四、改动总览

### 数据库（`sql/init.sql`）

新增 3 张表：`favorite`、`favorite_tag`、`resume_version`

### 后端（`backend/src/main/java/com/offerpilot/`）

| 模块 | 新建文件 | 修改文件 |
|------|----------|----------|
| favorite | `entity/Favorite.java`、`entity/FavoriteTag.java`、`mapper/FavoriteMapper.java`、`mapper/FavoriteTagMapper.java`、`dto/FavoriteUpsertRequest.java`、`dto/FavoriteBatchDeleteRequest.java`、`dto/FavoriteTagUpsertRequest.java`、`dto/FavoriteQuery.java`、`vo/FavoriteVO.java`、`vo/FavoriteTagVO.java`、`vo/FavoriteStatsVO.java`、`service/FavoriteService.java`、`service/impl/FavoriteServiceImpl.java`、`controller/FavoriteController.java` | — |
| resume | `entity/ResumeVersion.java`、`mapper/ResumeVersionMapper.java`、`vo/ResumeScoreVO.java`、`vo/ResumeSuggestionVO.java`、`vo/ResumeVersionVO.java` | `service/ResumeService.java`、`service/impl/ResumeServiceImpl.java`、`controller/ResumeController.java` |

### 前端（`frontend/src/`）

| 模块 | 新建文件 | 修改文件 |
|------|----------|----------|
| 收藏 | `api/favorites.ts`、`pages/favorites/FavoritesPage.vue` | `types/api.ts`、`router/index.ts`、`components/NavRail.vue`、`layouts/AppLayout.vue`、`pages/knowledge/KnowledgePage.vue`、`pages/question/QuestionBankPage.vue` |
| 简历增强 | — | `api/resume.ts`、`types/api.ts`、`pages/resume/ResumeAssistantPage.vue` |
| 简历制作 | `pages/resume-craft/ResumeCraftPage.vue` | `router/index.ts`、`components/NavRail.vue`、`layouts/AppLayout.vue` |

---

## 五、实施顺序

1. **简历制作占位页** — 最小改动，前端路由 + 导航拆分 + 占位页
2. **收藏功能（全栈）** — DDL → Entity → Mapper → DTO/VO → Service → Controller → 前端 API → 页面骨架 → 其他页面联动
3. **简历评分** — 后端规则引擎 + 前端评分卡片
4. **简历版本管理** — DDL → Entity → Mapper → VO → Service → Controller → 前端版本卡片
5. **简历面试联动增强** — 纯前端页面改动
