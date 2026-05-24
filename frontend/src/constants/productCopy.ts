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
  leaderboard: {
    title: '当前还没有贡献排名',
    description: '先参与社区回答，后面这里会继续更新贡献排名。'
  },
  communityBoard: {
    title: '当前版块还没有帖子',
    description: '换个版块看看，或先发起一个问题，把这块讨论带起来。'
  },
  communityReplies: {
    title: '当前还没有回复',
    description: '先写下第一条回复，把这次讨论继续往前推进。'
  },
  knowledgeRecommended: {
    title: '当前还没有可用推荐资料',
    description: '先切到“我的文档”上传一份资料，或调整筛选范围继续找能继续提问的内容。'
  },
  knowledgePersonal: {
    title: '上传你的第一份学习资料',
    description: '上传一份资料，处理完成后就能继续提问。'
  },
  favorites: {
    title: '当前还没有收藏内容',
    description: '先去知识库、题库或社区标记几条重点内容，后面这里会集中回看。'
  },
  favoriteGroups: {
    title: '当前还没有收藏分组',
    description: '先新建一个分组，把同一轮复习要看的内容收在一起。'
  },
  wrongBook: {
    title: '当前还没有错题记录',
    description: '先做一轮题库训练或模拟面试，低分题和易错题会自动进来，后面这里再集中复习。'
  },
  interviewHistory: {
    title: '当前还没有已完成的模拟面试',
    description: '先开始一轮，后面这里会按时间线保留你的分数和表现。'
  },
  dashboardRecentInterview: {
    title: '当前还没有最近面试摘要',
    description: '先完成一轮模拟面试，后面这里会继续展示最近一次训练的摘要和薄弱点。'
  },
  dashboardInterviews: {
    title: '当前还没有最近面试记录',
    description: '先完成一轮模拟面试，后面这里会继续回看最近几次训练的分数、方向和完成时间。'
  },
  dashboardInterviewTrend: {
    title: '当前还没有可回看的面试分数',
    description: '先完成一轮模拟面试，后面这里会继续对比最近几次训练的分数变化。'
  },
  dashboardWeakPoints: {
    title: '当前还没有分类掌握度',
    description: '先做题并完成一轮复习，后面这里会继续提醒哪类内容最该优先补齐。'
  },
  deviceManage: {
    title: '当前还没有其他登录设备',
    description: '目前只看到这台设备。后面有新设备登录时，这里会继续保留记录，方便排查陌生登录。'
  },
  adminKnowledgeDocs: {
    title: '当前还没有文档',
    description: '先导入一份资料，后面这里再继续处理解析、索引和检索验证。'
  }
} as const

export const ERROR_COPY = {
  chatAnswerFailed: '这次回答还没生成成功，请换个问法或稍后再试。',
  chatSessionLoadFailed: '这段会话还没加载出来，请重新点一次，或换一段会话继续查看。',
  chatSessionDeleteFailed: '这段会话还没删除成功，请稍后再试。',
  chatSubmitFailed: '这次提问没有发送成功，请检查网络后再试。',
  communityQuestionLoadFailed: '帖子内容还没加载出来，请稍后再试；如果还是不行，先回到社区列表重新进入。',
  communityQuestionEditLoadFailed: '要编辑的帖子内容还没加载出来，请先回到社区列表重新进入。',
  communityQuestionSaveFailed: '这次发帖还没提交成功，请检查标题和内容后再试。',
  dashboardCoreLoadFailed: '首页核心数据还没加载出来，请刷新页面后再试。',
  interviewHistoryLoadFailed: '最近的面试记录还没加载出来，请稍后再试。',
  interviewNextQuestionLoadFailed: '下一题还没拿到，请稍后再试。',
  interviewSummaryLoadFailed: '这次面试总结还没加载出来，请稍后再试。',
  notificationLoadFailed: '通知列表还没加载出来，请稍后再试。',
  notificationMarkAllReadFailed: '通知还没全部设为已读，请稍后再试。',
  favoritesLoadFailed: '收藏列表还没加载出来，请刷新或调整筛选后再试。',
  favoriteRemoveFailed: '这条收藏还没取消成功，请稍后再试。',
  favoriteBatchRemoveFailed: '这批收藏还没取消成功，请稍后再试。',
  favoriteTagCreateFailed: '收藏分组还没创建成功，请换个名字后再试。',
  favoriteTagDeleteFailed: '这个分组还没删除成功，请稍后再试。',
  knowledgeListLoadFailed: '资料列表暂时没加载出来，请切换范围或稍后重试。',
  knowledgeUploadFailed: '这份资料还没上传成功，请检查文件格式、大小后再试。',
  knowledgeDeleteFailed: '这份文档还没删除成功，请稍后再试。',
  questionListLoadFailed: '题库列表还没加载出来，请调整筛选条件后再试。',
  questionFavoriteToggleFailed: '收藏状态没有更新成功，请稍后再试。',
  reviewTodayLoadFailed: '今天的复习任务还没加载出来，请刷新页面或稍后再试。',
  reviewSubmitFailed: '这次复习结果还没保存成功，请再点一次当前评级。',
  resumeWorkspaceLoadFailed: '简历助手还没加载出来，请刷新页面后再试。',
  resumeSaveFailed: '简历修改还没保存成功，请检查内容后再试。',
  resumeRetryParseFailed: '这份简历还没重新识别成功，请稍后再试。',
  resumeUploadFailed: '简历还没上传成功，请检查文件格式后再试。',
  resumeRestoreVersionFailed: '这个版本还没回滚成功，请稍后再试。',
  resumeOutlineCopyFailed: '面试提纲还没复制成功，请稍后再试。',
  wrongListLoadFailed: '错题列表还没加载出来，请刷新页面或稍后再试。',
  wrongDetailLoadFailed: '这道错题的详情还没加载出来，请换一题，或稍后再试。',
  wrongMasteryUpdateFailed: '掌握状态还没更新成功，请重新点一次当前目标状态。',
  wrongDeleteFailed: '这道错题还没删除成功，请稍后再试。',
  wrongExportFailed: '错题本还没导出成功，请稍后再试。',
  deviceLoadFailed: '登录设备还没加载出来，请刷新后再试。',
  deviceRevokeFailed: '这台设备暂时没有撤销成功，请稍后再试。',
  deviceRevokeAllFailed: '其他设备暂时没有全部撤销成功，请稍后再试。',
  adminContentLoadFailed: '待审核内容还没加载出来，请刷新列表后再试。',
  adminContentApproveFailed: '这条内容还没审核通过，请稍后再试。',
  adminContentRejectFailed: '这条内容还没拒绝成功，请稍后再试。',
  adminUsersLoadFailed: '用户列表还没加载出来，请刷新或调整筛选后再试。',
  adminUserSaveFailed: '这位用户的信息还没更新成功，请稍后再试。',
  adminUserBanFailed: '这位用户还没封禁成功，请稍后再试。',
  adminUserUnbanFailed: '这位用户还没解封成功，请稍后再试。',
  adminUserDetailLoadFailed: '这位用户的详情还没加载出来，请稍后再试。',
  adminSystemConfigLoadFailed: '系统配置还没加载出来，请刷新后再试。',
  adminSystemConfigSaveFailed: '这项配置还没保存成功，请检查改动后再试。',
  adminInterviewGovernanceLoadFailed: '面试治理数据还没加载出来，请刷新列表后再试。',
  adminLoginLogLoadFailed: '登录日志还没加载出来，请刷新或调整筛选后再试。',
  adminQuestionLoadFailed: '题库列表还没加载出来，请刷新或调整筛选后再试。',
  adminKnowledgeLoadFailed: '文档列表还没加载出来，请刷新或调整筛选后再试。',
  adminKnowledgeSearchFailed: '检索结果还没拿到，请换个问题或稍后再试。',
  adminKnowledgeImportFailed: '这份资料还没导入成功，请稍后再试。',
  adminKnowledgeRechunkFailed: '这份文档还没重新切分成功，请稍后再试。',
  adminKnowledgeReindexFailed: '这份文档的索引还没重建成功，请稍后再试。',
  adminKnowledgeBatchRechunkFailed: '这批文档还没重新切分成功，请稍后再试。',
  adminKnowledgeBatchReindexFailed: '这批索引还没重建成功，请稍后再试。',
  adminQuestionExportFailed: '题库还没导出成功，请稍后再试。',
  adminUserExportFailed: '用户列表还没导出成功，请稍后再试。',
  adminAiLogLoadFailed: 'AI 调用日志还没加载出来，请刷新或调整筛选后再试。'
} as const
