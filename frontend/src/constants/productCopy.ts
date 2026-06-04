export const PRODUCT_PAGE_NAMES = {
  dashboard: '首页',
  question: '题库训练',
  knowledge: '知识库',
  chat: '问答',
  favorites: '我的收藏',
  interview: '模拟面试',
  wrong: '错题本',
  review: '复习巩固',
  studyPlan: '学习计划',
  applications: '投递管理',
  resume: '简历助手',
  analytics: '数据分析',
  settings: '账户设置'
} as const

export const CHAT_MODE_NAMES = {
  direct: '直接提问',
  withKnowledge: '带资料提问',
  withResume: '结合简历提问'
} as const

export const INTERVIEW_MODE_NAMES = {
  plain: '不带简历',
  withResume: '结合简历',
  withProject: '结合项目'
} as const

export const KNOWLEDGE_STATUS_NAMES = {
  draft: '处理中',
  parsed: '待提问',
  indexed: '可以提问'
} as const

export const EMPTY_STATE_COPY = {
  analyticsTrend: {
    title: '还没有趋势数据',
    description: '完成计划、简历或投递相关操作后，这里会显示准备趋势。'
  },
  analyticsReviewIntensity: {
    title: '还没有复盘强度数据',
    description: '完成今天的错题复盘后，这里会显示复盘强度变化。'
  },
  analyticsReviewStability: {
    title: '还没有复盘稳定性数据',
    description: '连续完成几次复盘后，这里会显示稳定性变化。'
  },
  analyticsCategoryMastery: {
    title: '还没有分类掌握度',
    description: '完成题目复盘后，这里会显示各分类的掌握进度。'
  },
  analyticsMasteryDistribution: {
    title: '还没有错题分布数据',
    description: '完成一轮错题复盘后，这里会显示当前状态分布。'
  },
  analyticsInterviewTrend: {
    title: '还没有面试趋势数据',
    description: '完成几次模拟面试后，这里会显示分类趋势变化。'
  },
  leaderboard: {
    title: '还没有贡献排名',
    description: '参与一次社区回答后，这里会显示你的贡献排名。'
  },
  communityBoard: {
    title: '这个版块还没有帖子',
    description: '换个版块看看，或发起一个新问题。'
  },
  communityReplies: {
    title: '还没有回复',
    description: '写下第一条回复，参与这次讨论。'
  },
  knowledgeRecommended: {
    title: '还没有可用推荐资料',
    description: '你可以切到“我的文档”上传资料，或调整筛选范围。'
  },
  knowledgePersonal: {
    title: '上传你的第一份学习资料',
    description: '上传资料后，处理完成即可提问。'
  },
  favorites: {
    title: '还没有收藏内容',
    description: '去知识库、题库或社区添加几条收藏。'
  },
  favoriteGroups: {
    title: '还没有收藏分组',
    description: '新建一个分组，把相关内容整理到一起。'
  },
  wrongBook: {
    title: '还没有错题记录',
    description: '完成题库训练或模拟面试后，这里会显示低分题和易错题。'
  },
  interviewHistory: {
    title: '还没有已完成的模拟面试',
    description: '开始一轮模拟面试后，这里会显示分数、表现和时间线。'
  },
  dashboardRecentInterview: {
    title: '还没有最近面试摘要',
    description: '完成一轮模拟面试后，这里会显示最近一次训练的摘要和薄弱点。'
  },
  dashboardInterviews: {
    title: '还没有最近面试记录',
    description: '完成模拟面试后，这里会显示最近几次训练的分数和完成时间。'
  },
  dashboardInterviewTrend: {
    title: '还没有可回看的面试分数',
    description: '完成几次模拟面试后，这里会显示分数变化。'
  },
  dashboardWeakPoints: {
    title: '还没有分类掌握度',
    description: '完成做题和复习后，这里会显示需要补齐的分类。'
  },
  dashboardApplicationDonut: {
    title: '还没有投递记录',
    description: '记录目标岗位后，这里会显示各阶段的分布变化。'
  },
  resumeLibrary: {
    title: '还没有简历',
    description: '上传一份简历后，就可以开始整理项目、提纲和面试表达。'
  },
  resumeWorkspace: {
    title: '上传简历后，这里会显示整理内容',
    description: '你可以检查简历内容、整理项目追问，并确认开场和面试提纲。'
  },
  wrongDetailSelection: {
    title: '选择一道错题查看详情',
    description: '点击左侧题目后，这里会显示标准答案、错误原因和下一次复习安排。'
  },
  applicationDetailKeywords: {
    title: '还没有明显命中词',
    description: '补充 JD 关键词或刷新分析结果后，这里会显示岗位匹配点。'
  },
  applicationDetailTimeline: {
    title: '还没有过程记录',
    description: '补充当前状态或面试反馈后，这里会显示完整时间线。'
  },
  applicationBoardFocus: {
    title: '还没有可推进的岗位',
    description: '记录目标岗位后，这里会显示下一步建议。'
  },
  deviceManage: {
    title: '还没有其他登录设备',
    description: '目前只看到这台设备；后续如有新设备登录，这里会保留记录，方便排查陌生登录。'
  },
  adminOverviewTrend: {
    title: '还没有趋势数据',
    description: '积累一段时间的数据后，这里会显示新增和活跃走势。'
  },
  adminQuestionList: {
    title: '还没有题目',
    description: '新增题目或导入题库后，这里会显示筛选结果。'
  },
  adminSystemConfigHistory: {
    title: '还没有变更记录',
    description: '更新配置后，这里会显示修改历史。'
  },
  adminKnowledgeDocs: {
    title: '还没有文档',
    description: '导入资料后，这里可以处理解析、索引和检索验证。'
  }
} as const

export const ERROR_COPY = {
  applicationStatusUpdateFailed: '当前阶段更新失败，请检查填写内容后重试。',
  applicationEventCreateFailed: '反馈记录失败，请补全信息后重试。',
  applicationAnalysisRefreshFailed: 'JD 分析刷新失败，请稍后重试。',
  chatAnswerFailed: '这次回答生成失败，请换个问法后重试。',
  chatSessionLoadFailed: '无法加载这段会话，请重试或切换到其他会话。',
  chatSessionDeleteFailed: '会话删除失败，请稍后重试。',
  chatSubmitFailed: '这次提问发送失败，请检查网络后重试。',
  communityQuestionLoadFailed: '帖子内容加载失败，请稍后重试；如果仍有问题，请回到社区列表重新进入。',
  communityQuestionEditLoadFailed: '无法加载要编辑的帖子内容，请回到社区列表后重试。',
  communityQuestionSaveFailed: '发帖失败，请检查标题和内容后重试。',
  dashboardCoreLoadFailed: '无法加载首页数据，请刷新页面后重试。',
  interviewHistoryLoadFailed: '无法加载最近的面试记录，请稍后重试。',
  interviewDetailLoadFailed: '无法加载这次面试记录，请稍后重试或切换到其他记录。',
  interviewNextQuestionLoadFailed: '无法加载下一题，请稍后重试。',
  interviewSummaryLoadFailed: '无法加载这次面试总结，请稍后重试。',
  notificationLoadFailed: '无法加载通知列表，请稍后重试。',
  notificationMarkAllReadFailed: '全部已读设置失败，请稍后重试。',
  favoritesLoadFailed: '无法加载收藏列表，请刷新或调整筛选后重试。',
  favoriteRemoveFailed: '取消收藏失败，请稍后重试。',
  favoriteBatchRemoveFailed: '批量取消收藏失败，请稍后重试。',
  favoriteTagCreateFailed: '收藏分组创建失败，请换个名字后重试。',
  favoriteTagDeleteFailed: '分组删除失败，请稍后重试。',
  knowledgeListLoadFailed: '资料列表暂时没加载出来，请切换范围或稍后重试。',
  knowledgeUploadFailed: '资料上传失败，请检查文件格式和大小后重试。',
  knowledgeDeleteFailed: '文档删除失败，请稍后重试。',
  questionListLoadFailed: '无法加载题库列表，请调整筛选条件后重试。',
  questionFavoriteToggleFailed: '收藏状态更新失败，请稍后重试。',
  reviewTodayLoadFailed: '无法加载今天的复习任务，请刷新页面或稍后重试。',
  reviewSubmitFailed: '这次复习结果保存失败，请重新选择评级。',
  resumeWorkspaceLoadFailed: '无法加载简历助手，请刷新页面后重试。',
  resumeSaveFailed: '简历修改保存失败，请检查内容后重试。',
  resumeRetryParseFailed: '简历重新识别失败，请稍后重试。',
  resumeUploadFailed: '简历上传失败，请检查文件格式后重试。',
  resumeRestoreVersionFailed: '版本回滚失败，请稍后重试。',
  resumeOutlineCopyFailed: '面试提纲复制失败，请稍后重试。',
  wrongListLoadFailed: '无法加载错题列表，请刷新页面或稍后重试。',
  wrongDetailLoadFailed: '无法加载这道错题的详情，请切换题目或稍后重试。',
  wrongMasteryUpdateFailed: '掌握状态更新失败，请重新选择目标状态。',
  wrongDeleteFailed: '错题删除失败，请稍后重试。',
  wrongExportFailed: '错题本导出失败，请稍后重试。',
  deviceLoadFailed: '无法加载登录设备，请刷新后重试。',
  deviceRevokeFailed: '设备撤销失败，请稍后重试。',
  deviceRevokeAllFailed: '批量撤销设备失败，请稍后重试。',
  adminContentLoadFailed: '无法加载待审核内容，请刷新列表后重试。',
  adminContentApproveFailed: '审核通过失败，请稍后重试。',
  adminContentRejectFailed: '拒绝操作失败，请稍后重试。',
  adminOverviewLoadFailed: '无法加载后台概览数据，请刷新页面后重试。',
  adminUsersLoadFailed: '无法加载用户列表，请刷新或调整筛选后重试。',
  adminUserSaveFailed: '用户信息更新失败，请稍后重试。',
  adminUserBanFailed: '封禁用户失败，请稍后重试。',
  adminUserUnbanFailed: '解封用户失败，请稍后重试。',
  adminUserDetailLoadFailed: '无法加载用户详情，请稍后重试。',
  adminCategorySaveFailed: '分类保存失败，请检查名称和类型后重试。',
  adminCategoryDeleteFailed: '分类删除失败，请稍后重试。',
  adminQuestionSaveFailed: '题目保存失败，请检查标题和分类后重试。',
  adminQuestionDeleteFailed: '题目删除失败，请稍后重试。',
  adminQuestionImportFailed: '题库导入失败，请检查文件格式后重试。',
  adminRuntimeGovernanceLoadFailed: '无法加载运行时治理数据，请刷新页面后重试。',
  adminSystemConfigLoadFailed: '无法加载系统配置，请刷新后重试。',
  adminSystemConfigSaveFailed: '配置保存失败，请检查改动后重试。',
  adminInterviewGovernanceLoadFailed: '无法加载面试治理数据，请刷新列表后重试。',
  adminLoginLogLoadFailed: '无法加载登录日志，请刷新或调整筛选后重试。',
  adminQuestionLoadFailed: '无法加载题库列表，请刷新或调整筛选后重试。',
  adminKnowledgeLoadFailed: '无法加载文档列表，请刷新或调整筛选后重试。',
  adminKnowledgeSearchFailed: '检索失败，请换个问题或稍后重试。',
  adminKnowledgeImportFailed: '资料导入失败，请稍后重试。',
  adminKnowledgeRechunkFailed: '文档重新切分失败，请稍后重试。',
  adminKnowledgeReindexFailed: '索引重建失败，请稍后重试。',
  adminKnowledgeBatchRechunkFailed: '批量重新切分失败，请稍后重试。',
  adminKnowledgeBatchReindexFailed: '批量重建索引失败，请稍后重试。',
  adminQuestionExportFailed: '题库导出失败，请稍后重试。',
  adminUserExportFailed: '用户列表导出失败，请稍后重试。',
  adminAiLogLoadFailed: '无法加载 AI 调用日志，请刷新或调整筛选后重试。'
} as const
