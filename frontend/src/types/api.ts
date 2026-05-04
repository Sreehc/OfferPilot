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
  role: string
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

export interface TwoFactorEnable {
  recoveryCodes: string[]
}

export interface RecentInterviewItem {
  sessionId: number
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

export interface DashboardOverview {
  learningCount: number
  averageScore: number
  wrongCount: number
  planCompletionRate: number
  planHealthScore: number
  recentInterviews: RecentInterviewItem[]
  weakPoints: WeakPointItem[]
  firstVisit: boolean
  overallAbility?: number
  recommendedDifficulty?: string
  weakCategories?: string[]
  suggestedFocus?: string | null
  categoryAbilities?: CategoryAbility[]

  // Analytics insights
  thisWeekAvgScore?: number
  lastWeekAvgScore?: number
  thisWeekInterviewCount?: number
  categoryChanges?: CategoryChange[]
  bestStudyHours?: HourDistribution[]
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
  tags?: string
  standardAnswer?: string
  scoreStandard?: string
  source?: string
  createTime?: string
  updateTime?: string
}

export interface KnowledgeDocItem {
  id: number
  title: string
  categoryId?: number
  categoryName?: string
  sourceType?: string
  fileUrl?: string
  status: 'draft' | 'parsed' | 'indexed'
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
}

export interface KnowledgeSearchResult {
  query: string
  references: KnowledgeReferenceItem[]
}

export interface ChatSessionItem {
  id: number
  title: string
  mode: 'chat' | 'rag'
  lastMessageTime?: string
  updateTime?: string
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
  references: KnowledgeReferenceItem[]
}

export interface InterviewCurrentQuestion {
  sessionId: number
  currentIndex: number
  questionCount: number
  questionId: number
  questionTitle: string
}

export interface InterviewAnswerResult {
  score: number
  comment: string
  standardAnswer: string
  followUp: string
  addedToWrongBook: boolean
  hasNextQuestion: boolean
}

export interface InterviewRecordItem {
  questionId: number
  questionTitle: string
  userAnswer: string
  score: number
  comment: string
  standardAnswer: string
  followUp: string
  voiceTranscript?: string
  voiceConfidence?: number
}

export interface InterviewDetail {
  sessionId: number
  direction: string
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
  addedToWrongBook: boolean
  hasNextQuestion: boolean
}

export interface InterviewHistoryItem {
  sessionId: number
  direction: string
  status: string
  mode?: string
  totalScore: number
  questionCount: number
  startTime?: string
  endTime?: string
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

export interface ReviewStats {
  totalReviews: number
  currentStreak: number
  todayPending: number
  todayCompleted?: number
  heatmap?: Record<string, number>
}

export interface StudyPlanItem {
  id: number
  title: string
  goal: string
  status: string
  version?: number
  parentPlanId?: number
  startDate?: string
  endDate?: string
  totalTasks?: number
  completedTasks?: number
  healthScore?: number
  tasks: StudyPlanTaskItem[]
}

export interface StudyPlanTaskItem {
  id: number
  taskDate: string
  taskType: string
  content: string
  status: 'todo' | 'done'
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
  wrongCount: number
  isWeak: boolean
  recommendedDifficulty: string
}

export interface AbilityProfile {
  overallAbility: number
  recommendedDifficulty: string
  categoryAbilities: CategoryAbility[]
  weakCategories: string[]
  suggestedFocus: string | null
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

export interface CategoryTrend {
  categoryId: number
  categoryName: string
  points: WeeklyPoint[]
}

export interface AbilityTrend {
  weeks: string[]
  overallTrend: WeeklyPoint[]
  categoryTrends: CategoryTrend[]
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
  masteryDistribution: Record<string, number>
  totalReviews: number
  currentStreak: number
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
