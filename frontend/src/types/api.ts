export interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

export interface PageResult<T> {
  records: T[]
  total: number
  pageNum: number
  pageSize: number
  totalPages: number
}

export interface UserInfo {
  id: number
  username: string
  nickname: string
  avatar?: string
  email?: string
  emailVerified?: boolean
  emailVerifiedAt?: string
  role: string
  status?: number
  createTime?: string
  lastLoginTime?: string
}

export interface LoginResponse {
  token: string
  userInfo: UserInfo
  deviceId?: number
  requires2fa?: boolean
  tempToken?: string
}

export interface TwoFactorStatus {
  enabled: boolean
  recoveryCodesRemaining?: number
}

export interface TwoFactorSetup {
  otpauthUri: string
  secret: string
}

export interface AuthDelivery {
  message: string
  maskedEmail?: string
  expiresInMinutes?: number
  debugCode?: string
}

export interface TwoFactorEnable {
  recoveryCodes: string[]
}

export type ProviderScope = 'llm' | 'embedding' | 'asr' | 'search' | 'oss' | 'voiceprint'

export interface UserProviderConfigItem {
  scope: ProviderScope
  label: string
  description: string
  enabled: boolean
  configured: boolean
  status: 'missing' | 'incomplete' | 'saved' | 'ready' | string
  statusMessage: string
  providerName?: string
  baseUrl?: string
  model?: string
  apiKeyMasked?: string
  accessKeyMasked?: string
  secretKeyMasked?: string
  endpoint?: string
  bucket?: string
  regionName?: string
  dimensions?: number
  lastCheckedAt?: string
  lastCheckStatus?: string
  lastCheckMessage?: string
  updateTime?: string
}

export interface RecentInterviewItem {
  sessionId: string
  direction: string
  totalScore: number
  status: string
  finishedAt: string
}

export interface WeakPointItem {
  categoryName: string
  wrongCount: number
  score: number
}

export interface CategoryMasteryItem {
  categoryId?: number | null
  categoryName: string
  totalCards: number
  masteredCards: number
  dueCount: number
  masteryRate: number
}

export interface PlanSummary {
  planId?: string
  title: string
  currentDay?: number
  totalDays?: number
  todayTaskCount?: number
  completedTaskCount?: number
  totalTaskCount?: number
  progressRate?: number
  actionPath?: string
}

export interface ResumePreparationSummary {
  resumeId?: string
  resumeCount: number
  latestResumeTitle: string
  parseStatus?: string
  projectCount?: number
  actionPath?: string
}

export interface ApplicationSummary {
  totalCount: number
  activeCount: number
  offerCount: number
  averageMatchScore?: number
  topCompany?: string
  actionPath?: string
}

export interface NextStepSummary {
  title: string
  description: string
  actionPath: string
}

export interface NextAction {
  key: 'upload_resume' | 'generate_plan' | 'complete_today_plan' | 'job_prep' | 'follow_application' | 'start_interview'
  title: string
  description: string
  path: string
  reason: string
  priority: 'P0' | 'P1' | 'P2'
}

export interface DashboardWorkflowContinuation {
  key: string
  label: string
  status: string
  description: string
  path: string
  tone: 'blue' | 'teal' | 'violet' | 'amber' | string
}

export interface DashboardOverview {
  learningCount: number
  averageScore: number
  wrongCount: number
  recentInterviews: RecentInterviewItem[]
  weakPoints: WeakPointItem[]
  firstVisit: boolean
  reviewDebtCount?: number
  studyStreak?: number
  weakCategories?: string[]
  suggestedFocus?: string | null
  categoryAbilities?: CategoryAbility[]
  nextAction?: NextAction
  applicationSummary?: ApplicationSummary
  workflowContinuations?: DashboardWorkflowContinuation[]
}

export interface CategoryItem {
  id: number
  name: string
  type: 'question' | 'knowledge' | 'interview'
  sortOrder?: number
  status?: number
}

export interface QuestionItem {
  id: number
  title: string
  categoryId: number
  categoryName?: string
  type?: string
  difficulty: 'easy' | 'medium' | 'hard'
  frequency?: number
  jobDirection?: string
  applicableScope?: string
  tags?: string
  standardAnswer?: string
  interviewAnswer?: string
  followUpSuggestions?: string
  commonMistakes?: string
  scoreStandard?: string
  source?: string
  createTime?: string
  updateTime?: string
}

export type KnowledgeLibraryScope = 'system' | 'personal' | string
export type KnowledgeBusinessType = 'system_knowledge' | 'user_note' | 'resume' | 'jd' | 'project_doc' | string
export type KnowledgeParseStatus = 'pending' | 'parsed' | 'failed' | string
export type KnowledgeIndexStatus = 'pending' | 'indexed' | 'failed' | string
export type KnowledgeDocStatus = 'draft' | 'parsed' | 'indexed'

export interface KnowledgeDocItem {
  id: number
  title: string
  categoryId?: number
  categoryName?: string
  libraryScope?: KnowledgeLibraryScope
  businessType?: KnowledgeBusinessType
  sourceType?: string
  fileType?: string
  fileUrl?: string
  parseStatus?: KnowledgeParseStatus
  indexStatus?: KnowledgeIndexStatus
  status: KnowledgeDocStatus
  summary?: string
  chunkCount?: number
  updateTime?: string
}

export interface KnowledgeReferenceItem {
  docId: number
  chunkId: number
  docTitle: string
  snippet: string
  score?: number
  libraryScope?: 'system' | 'personal' | string
  businessType?: string
  fileType?: string
}

export interface KnowledgeSearchResult {
  query: string
  references: KnowledgeReferenceItem[]
}

export interface ChatSessionItem {
  id: number
  title: string
  mode: 'chat' | 'rag'
  contextType?: ContextType
  knowledgeScope?: ChatKnowledgeScope
  contextSource?: ContextSource
  lastMessageTime?: string
  updateTime?: string
}

export type ChatAnswerMode = 'learning' | 'interview' | 'concise' | 'project'
export type ChatKnowledgeScope = 'all' | 'system' | 'personal'
export type ContextType = 'general' | 'knowledge' | 'resume' | 'project'

export interface ContextSource {
  type: ContextType
  label: string
  summary?: string
  knowledgeScope?: ChatKnowledgeScope
  sourceDocId?: string
  sourceDocTitle?: string
  resumeId?: string
  resumeTitle?: string
  projectId?: string
  projectName?: string
}

export interface ChatMessageItem {
  id: number
  role: 'user' | 'assistant'
  messageType: string
  content: string
  createTime: string
  references: KnowledgeReferenceItem[]
}

export interface ChatSendResult {
  sessionId: number
  sessionTitle: string
  answer: string
  answerMode?: ChatAnswerMode
  knowledgeScope?: ChatKnowledgeScope
  contextType?: ContextType
  contextSource?: ContextSource
  references: KnowledgeReferenceItem[]
  suggestedQuestions?: string[]
}

export interface InterviewCurrentQuestion {
  sessionId: string
  currentIndex: number
  questionCount: number
  questionId: string
  questionTitle: string
  direction: string
  jobRole?: string
  experienceLevel?: string
  techStack?: string
  durationMinutes?: number
  includeResumeProject?: boolean
  contextType?: ContextType
  contextSource?: ContextSource
}

export interface InterviewScoreDimension {
  dimension: string
  score: number
  summary: string
}

export interface InterviewAnswerResult {
  score: number
  comment: string
  standardAnswer: string
  followUp: string
  scoreBreakdown?: InterviewScoreDimension[]
  weakPointTags?: string[]
  reviewSummary?: string
  addedToWrongBook: boolean
  hasNextQuestion: boolean
}

export interface InterviewRecordItem {
  questionId: string
  questionTitle: string
  userAnswer: string
  score: number
  comment: string
  standardAnswer: string
  followUp: string
  scoreBreakdown?: InterviewScoreDimension[]
  weakPointTags?: string[]
  reviewSummary?: string
  isLowScore?: boolean
  voiceTranscript?: string
  voiceConfidence?: number
}

export interface InterviewDetail {
  sessionId: string
  direction: string
  jobRole?: string
  experienceLevel?: string
  techStack?: string
  durationMinutes?: number
  includeResumeProject?: boolean
  contextType?: ContextType
  contextSource?: ContextSource
  status: string
  mode?: string
  totalScore: number
  questionCount: number
  startTime?: string
  endTime?: string
  records: InterviewRecordItem[]
}

export interface VoiceSubmitResult {
  transcript: string
  transcriptConfidence?: number
  transcriptTimeMs?: number
  score: number
  comment: string
  standardAnswer: string
  followUp: string
  scoreBreakdown?: InterviewScoreDimension[]
  weakPointTags?: string[]
  reviewSummary?: string
  addedToWrongBook: boolean
  hasNextQuestion: boolean
}

export interface InterviewHistoryItem {
  sessionId: string
  direction: string
  jobRole?: string
  experienceLevel?: string
  techStack?: string
  durationMinutes?: number
  includeResumeProject?: boolean
  contextType?: ContextType
  contextSource?: ContextSource
  status: string
  mode?: string
  totalScore: number
  questionCount: number
  startTime?: string
  endTime?: string
}

export interface JobPrepSession {
  id: string
  applicationId?: string
  resumeFileId?: string
  resumeTitle?: string
  company?: string
  jobTitle?: string
  jdText: string
  status: string
  matchScore?: number
  matchedKeywords: string[]
  missingKeywords: string[]
  focusAreas: string[]
  resumeTalkingPoints: string[]
  mockQuestions: string[]
  nextActions: string[]
  providerStatus?: string
  providerStatusMessage?: string
  suggestedAgentType?: string
  suggestedTriggerSource?: string
  nextActionLabel?: string
  nextActionPath?: string
  providerReadiness: CopilotPrepProviderReadiness[]
  summary: string
  updateTime?: string
}

export interface CopilotPrepProviderReadiness {
  scope: string
  label: string
  status: string
  statusMessage: string
}

export interface CopilotPrepSession {
  id: string
  applicationId?: string
  resumeFileId?: string
  jobPrepSessionId?: string
  resumeTitle?: string
  company?: string
  jobTitle?: string
  jdText?: string
  notes?: string
  status: string
  summary: string
  openingBrief: string[]
  keyRisks: string[]
  liveCues: string[]
  followUpQuestions: string[]
  nextActions: string[]
  providerStatus?: string
  providerStatusMessage?: string
  suggestedAgentType?: string
  suggestedTriggerSource?: string
  nextActionLabel?: string
  nextActionPath?: string
  providerReadiness: CopilotPrepProviderReadiness[]
  updateTime?: string
}

export interface CopilotRealtimeEvent {
  id: string
  sessionId: string
  eventType: string
  source: string
  summary: string
  payload?: Record<string, unknown>
  createTime?: string
}

export interface CopilotRealtimePostInterviewReview {
  summary: string
  strengths: string[]
  weakPoints: string[]
  recommendedActions: string[]
  suggestedAgentType?: string
  suggestedTriggerSource?: string
  nextActionLabel?: string
  nextActionPath?: string
}

export interface CopilotRealtimeSession {
  id: string
  copilotPrepSessionId: string
  applicationId?: string
  resumeFileId?: string
  jobPrepSessionId?: string
  resumeTitle?: string
  company?: string
  jobTitle?: string
  status: string
  providerStatus: string
  prepSummary: string
  liveChecklist: string[]
  providerReadiness: CopilotPrepProviderReadiness[]
  latestEventSummary?: string
  connectedAt?: string
  disconnectedAt?: string
  endedAt?: string
  postInterviewReview?: CopilotRealtimePostInterviewReview
  events: CopilotRealtimeEvent[]
  updateTime?: string
}

export interface RecordingReviewSegment {
  id: string
  segmentIndex: number
  transcriptText: string
  startOffsetMs?: number
  endOffsetMs?: number
  signalType?: string
}

export interface RecordingReviewSession {
  id: string
  direction?: string
  jobRole?: string
  notes?: string
  status: string
  statusMessage?: string
  transcript?: string
  transcriptConfidence?: number
  transcriptTimeMs?: number
  overallScore?: number
  summary: string
  strengths: string[]
  weakPoints: string[]
  suggestedActions: string[]
  providerStatus?: string
  providerStatusMessage?: string
  suggestedAgentType?: string
  suggestedTriggerSource?: string
  nextActionLabel?: string
  nextActionPath?: string
  providerReadiness: CopilotPrepProviderReadiness[]
  segments: RecordingReviewSegment[]
  updateTime?: string
}

export interface AgentRun {
  id: string
  agentType: string
  triggerSource: string
  status: string
  title: string
  summary: string
  userPrompt?: string
  contextRefs: string[]
  streamMode?: string
  recommendations: string[]
  checkpoints: string[]
  nextActionPath?: string
  nextActionLabel?: string
  requiresApproval: boolean
  approvalActionType?: string
  approvalSummary?: string
  approvalStage?: 'not_required' | 'waiting' | 'approved' | 'rejected' | 'canceled' | 'completed' | string
  decisionNote?: string
  executionSummary?: string
  providerGateStatus?: 'not_applicable' | 'ready' | 'degraded' | 'blocked' | string
  providerGateSummary?: string
  timeline: AgentRunTimelineItem[]
  providerGates: AgentRunProviderGate[]
  updateTime?: string
}

export interface AgentRunTimelineItem {
  key: string
  stepType?: 'analyze' | 'retrieve' | 'score' | 'update_profile' | 'schedule_review' | 'prepare_realtime' | 'wait_transcription' | 'wait_approval' | string
  title: string
  description: string
  status: 'completed' | 'waiting' | 'ready' | 'rejected' | 'canceled' | string
  timestamp?: string
}

export interface AgentRunProviderGate {
  scope: ProviderScope
  label: string
  status: 'missing' | 'incomplete' | 'saved' | 'ready' | string
  statusMessage: string
  required: boolean
}

export interface StudyPlanTaskItem {
  id: string
  dayIndex: number
  taskDate: string
  module: 'question' | 'chat' | 'review' | 'interview' | string
  title: string
  description: string
  actionPath: string
  estimatedMinutes: number
  priority: 'high' | 'medium' | 'low' | string
  status: 'pending' | 'completed' | string
  completedAt?: string
}

export interface PlanReasonSummary {
  title: string
  summary: string
  signals: string[]
}

export interface TodayFocusSummary {
  state: 'idle' | 'active' | 'completed' | string
  title: string
  reason: string
  expectedOutcome: string
}

export interface StudyPlanTrendSummary {
  status: 'on_track' | 'in_progress' | 'not_started' | string
  title: string
  summary: string
  highlights: string[]
}

export interface StudyPlan {
  id: string
  title: string
  durationDays: number
  focusDirection: string
  targetRole: string
  techStack: string
  weakPoints: string[]
  reviewSuggestion: string
  status: 'active' | 'completed' | 'archived' | string
  startDate: string
  endDate: string
  currentDay: number
  progressRate: number
  totalTaskCount: number
  completedTaskCount: number
  todayTaskCount: number
  dailyTargetMinutes: number
  planReasonSummary?: PlanReasonSummary
  todayFocusSummary?: TodayFocusSummary
  trendSummary?: StudyPlanTrendSummary
  tasks: StudyPlanTaskItem[]
}

export interface ResumeProjectQuestionItem {
  question: string
  intent: string
}

export interface ResumeProjectItem {
  id: string
  projectName: string
  roleName: string
  techStack: string
  responsibility: string
  achievement: string
  projectSummary: string
  followUpQuestions: ResumeProjectQuestionItem[]
  riskHints: string[]
  manualEdited?: boolean
  sortOrder?: number
}

export interface ResumeSummaryItem {
  id: string
  title: string
  fileType: string
  parseStatus: string
  summary: string
  skills: string[]
  education: string
  selfIntro: string
  interviewResumeText: string
  parseError?: string
  userFixStatus?: 'none' | 'pending' | 'updated' | string
  lastParsedAt?: string
  updateTime?: string
}

export interface ResumeFileDetail extends ResumeSummaryItem {
  projects: ResumeProjectItem[]
}

export interface EditableInterviewResumeProject {
  projectId?: string
  projectName: string
  roleName: string
  talkingPoint: string
  result: string
}

export interface EditableInterviewResume {
  headline: string
  positioning: string
  summary: string
  skillKeywords: string[]
  projectHighlights: EditableInterviewResumeProject[]
  speakingChecklist: string[]
  exportText: string
  editable: boolean
}

export type JobApplicationStatus = 'saved' | 'applied' | 'written' | 'interview' | 'offer' | 'rejected'

export interface JobApplicationEventItem {
  id: string
  eventType: string
  title: string
  content?: string
  eventTime: string
  result?: string
  interviewRound?: number
  interviewer?: string
  feedbackTags?: string[]
}

export interface JobApplicationItem {
  id: string
  resumeFileId?: string
  resumeTitle?: string
  company: string
  jobTitle: string
  city?: string
  source?: string
  jdText?: string
  status: JobApplicationStatus | string
  matchScore: number
  jdKeywords: string[]
  missingKeywords: string[]
  analysisSummary: string
  reviewSuggestion?: string
  nextStepSuggestion?: string
  applyDate?: string
  nextStepDate?: string
  updateTime?: string
}

export interface JobApplicationDetail extends JobApplicationItem {
  events: JobApplicationEventItem[]
}

export interface WrongQuestionItem {
  id: number
  questionId: number
  title: string
  masteryLevel: 'not_started' | 'reviewing' | 'mastered'
  standardAnswer?: string
  errorReason?: string
  easeFactor?: number
  intervalDays?: number
  nextReviewDate?: string
  streak?: number
  reviewCount?: number
}

export interface ReviewTodayItem {
  wrongQuestionId: number
  questionId: number
  title: string
  standardAnswer?: string
  errorReason?: string
  easeFactor?: number
  intervalDays?: number
  streak?: number
  nextReviewDate?: string
  overdueDays: number
  masteryLevel: string
}

export type ReviewContentType = 'all' | 'wrong_card'

export interface UnifiedReviewItem {
  reviewItemId: string
  contentType: ReviewContentType | string
  sourceType?: string
  title: string
  answer?: string
  explanation?: string
  easeFactor?: number
  intervalDays?: number
  streak?: number
  nextReviewDate?: string
  nextReviewAt?: string
  overdueDays: number
  masteryLevel: string
  wrongQuestionId?: string
}

export interface ReviewTodayData {
  selectedContentType: ReviewContentType | string
  totalPending: number
  overdueCount: number
  todayCompleted: number
  currentStreak: number
  countsByContentType: Record<string, number>
  items: UnifiedReviewItem[]
}

export interface ReviewStats {
  totalReviews: number
  currentStreak: number
  todayPending: number
  todayCompleted?: number
  overdueCount?: number
  contentTypeDistribution?: Record<string, number>
  heatmap?: Record<string, number>
}

export interface CommunityQuestion {
  id: number
  userId: number
  authorName?: string
  authorRank?: string
  title: string
  content: string
  categoryId?: number
  categoryName?: string
  status: string
  upvoteCount: number
  answerCount: number
  accepted?: boolean
  hasVoted?: boolean
  createdAt?: string
}

export interface CommunityAnswer {
  id: number
  questionId: number
  userId: number
  authorName?: string
  authorRank?: string
  content: string
  isAccepted: boolean
  upvoteCount: number
  hasVoted?: boolean
  createdAt?: string
}

export interface CommunityQuestionDetail extends CommunityQuestion {
  answers: CommunityAnswer[]
}

export interface LeaderboardEntry {
  userId: number
  username?: string
  rankTitle: string
  communityScore: number
  communityQuestions: number
  communityAnswers: number
  communityAccepted: number
  position: number
}

export interface CategoryAbility {
  categoryId: number
  categoryName: string
  abilityScore: number
  interviewCount: number
  recordingReviewCount: number
  wrongCount: number
  isWeak: boolean
  recommendedDifficulty: string
}

export interface AbilityProfile {
  overallAbility: number
  recommendedDifficulty: string
  recordingReviewCount: number
  categoryAbilities: CategoryAbility[]
  weakCategories: string[]
  suggestedFocus: string | null
  evidenceStatus?: 'insufficient' | 'forming' | 'ready' | string
  evidenceSummary?: string
}

export interface ProfileTopicWeeklyScore {
  week: string
  score: number
}

export interface ProfileTopicDetail {
  categoryId: string
  categoryName: string
  abilityScore: number
  interviewCount: number
  recordingReviewCount: number
  jobPrepCount?: number
  copilotPrepCount?: number
  applicationFeedbackCount?: number
  resumeEvidenceCount?: number
  wrongCount: number
  weak: boolean
  recommendedDifficulty: string
  totalCards: number
  masteredCards: number
  dueCount: number
  masteryRate: number
  evidenceStatus?: 'insufficient' | 'forming' | 'ready' | string
  evidenceSummary?: string
  retrospectiveReady?: boolean
  summary: string
  focusRecommendations: string[]
  recentScores: ProfileTopicWeeklyScore[]
}

export interface ProfileTopicRetrospective {
  categoryId: string
  categoryName: string
  title: string
  stage: string
  evidenceStatus?: 'insufficient' | 'forming' | 'ready' | string
  evidenceSummary?: string
  summary: string
  keySignals: string[]
  riskSignals: string[]
  nextActions: string[]
}

export interface RecommendInterview {
  direction: string
  questionCount: number
  reason: string
  difficulty: string
}

export interface RecommendQuestion {
  questionId: number
  title: string
  categoryId: number
  categoryName?: string
  difficulty: string
  reason: string
}

export interface NotificationItem {
  id: number
  type: string
  title: string
  content: string
  link?: string
  isRead: boolean
  createTime: string
}

export interface WeeklyPoint {
  week: string
  score: number
  count: number
}

export interface MemoryTrendPoint {
  week: string
  value: number
  count: number
}

export interface CategoryTrend {
  categoryId: number
  categoryName: string
  points: WeeklyPoint[]
}

export interface AbilityTrend {
  weeks: string[]
  reviewActivityTrend: MemoryTrendPoint[]
  reviewDebtTrend: MemoryTrendPoint[]
  masteredGrowthTrend: MemoryTrendPoint[]
  overallTrend: WeeklyPoint[]
  categoryTrends: CategoryTrend[]
  planProgressTrend?: Array<{
    week: string
    progressRate: number
    completedTaskCount: number
    totalTaskCount: number
  }>
  applicationActivityTrend?: Array<{
    week: string
    totalCount: number
    activeCount: number
    interviewCount: number
    offerCount: number
  }>
  resumeActivityTrend?: Array<{
    week: string
    uploadCount: number
    parsedCount: number
  }>
}

export interface WeeklyEF {
  week: string
  avgEF: number
  reviewCount: number
}

export interface WeeklyForgettingRate {
  week: string
  forgettingRate: number
  totalRatings: number
  againCount: number
}

export interface EfficiencyData {
  avgEaseFactor: number
  efTrend: WeeklyEF[]
  ratingDistribution: Record<number, number>
  forgettingRateTrend: WeeklyForgettingRate[]
  reviewDebtTrend: Array<{
    label: string
    reviewDebtCount: number
  }>
  masteredGrowthTrend: Array<{
    label: string
    masteredCardCount: number
  }>
  masteryDistribution: Record<string, number>
  contentTypeDistribution?: Record<string, number>
  categoryMastery?: CategoryMasteryItem[]
  totalReviews: number
  currentStreak: number
  forgettingRate?: number
}

export interface CategoryChange {
  categoryId: number
  categoryName: string
  thisWeekScore: number
  lastWeekScore: number
  change: number
}

export interface HourDistribution {
  timeSlot: string
  sessionCount: number
  avgScore: number
}

export interface LearningInsights {
  thisWeekAvgScore: number
  lastWeekAvgScore: number
  thisWeekInterviewCount: number
  lastWeekInterviewCount: number
  todayCompletionStatus?: string
  reviewDebtStatus?: string
  masteryGrowthStatus?: string
  planExecutionStatus?: string
  todayPlanCompletedTaskCount?: number
  todayPlanTaskCount?: number
  activePlanProgressRate?: number
  activePlanTitle?: string
  applicationActiveCount?: number
  applicationOfferCount?: number
  applicationStatus?: string
  resumeCount?: number
  latestResumeTitle?: string
  resumeReadinessStatus?: string
  interviewConversionStatus?: string
  nextAction?: NextAction
  categoryChanges: CategoryChange[]
  bestStudyHours: HourDistribution[]
}

export interface LoginDeviceItem {
  id: number
  deviceFingerprint: string
  deviceName?: string
  ip?: string
  city?: string
  lastActiveTime: string
  createTime: string
  current: boolean
}

export interface LoginLogItem {
  id: number
  userId: number
  username?: string
  ip?: string
  city?: string
  device?: string
  status: number
  failReason?: string
  createTime: string
}

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
  createTime: string
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
