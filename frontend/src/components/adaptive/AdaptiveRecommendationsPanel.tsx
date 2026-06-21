import { useMemo } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Alert, Button, Card, Empty, Space, Tag, Typography } from 'antd'
import { Link } from 'react-router-dom'
import { fetchAbilityProfileApi, fetchAdaptiveRecommendationsApi, fetchRecommendInterviewApi, fetchRecommendQuestionsApi } from '@/api/modules/adaptive'
import type { AnyRecord } from '@/api/types'
import { normalizeRecords, pickArray, pickText } from '@/modules/common'

type RecommendationAction = {
  id: string
  title: string
  reason: string
  weakPoint: string
  targetPath: string
  actionLabel: string
  tone?: string
  priority?: number
  rank?: number
  sourceIds?: string[]
}

function firstWeakPoint(profile: AnyRecord | undefined) {
  const weakCategories = pickArray<string | AnyRecord>(profile, ['weakCategories', 'weaknesses', 'weakPoints'])
  const weakCategory = weakCategories.find(Boolean)
  if (typeof weakCategory === 'string') return weakCategory
  const namedWeakCategory = pickText(weakCategory, ['categoryName', 'name', 'title', 'label'], '')
  if (namedWeakCategory && namedWeakCategory !== '-') return namedWeakCategory

  const categoryAbilities = pickArray<AnyRecord>(profile, ['categoryAbilities', 'abilities', 'dimensions'])
  const weakAbility = categoryAbilities.find((item) => item.isWeak === true || Number(item.abilityScore ?? item.score ?? 100) < 60)
  return pickText(weakAbility, ['categoryName', 'name', 'title', 'label'], '')
}

function difficultyText(value: unknown) {
  const difficulty = String(value || '').trim().toLowerCase()
  if (!difficulty) return ''
  const map: Record<string, string> = { easy: '简单', medium: '中等', hard: '困难' }
  return map[difficulty] || String(value)
}

function questionTarget(row: AnyRecord) {
  const id = row.questionId || row.id
  return id ? `/question/${id}` : '/question'
}

function buildActions(profile: AnyRecord | undefined, questionsData: unknown, interview: AnyRecord | undefined): RecommendationAction[] {
  const weakPoint = firstWeakPoint(profile)
  const suggestedFocus = pickText(profile, ['suggestedFocus', 'summary', 'evidenceSummary'], '')
  const recommendedDifficulty = difficultyText(profile?.recommendedDifficulty)
  const questionActions = normalizeRecords<AnyRecord>(questionsData)
    .slice(0, 2)
    .map((row, index) => {
      const rowWeakPoint = pickText(row, ['categoryName', 'category', 'weakPoint'], weakPoint || '待补能力')
      const difficulty = difficultyText(row.difficulty)
      return {
        id: `question-${row.questionId || row.id || index}`,
        title: pickText(row, ['title', 'name'], '推荐题目'),
        reason: pickText(row, ['reason', 'summary', 'description'], '基于能力画像和近期错题推荐这道题。'),
        weakPoint: rowWeakPoint,
        targetPath: questionTarget(row),
        actionLabel: '去刷题',
        tone: difficulty
      }
    })

  const interviewDirection = pickText(interview, ['direction', 'title', 'name'], '')
  const interviewAction = interviewDirection && interviewDirection !== '-'
    ? [{
        id: 'interview',
        title: interviewDirection,
        reason: pickText(interview, ['reason', 'summary', 'description'], '基于当前薄弱项推荐一次专项面试。'),
        weakPoint: weakPoint || interviewDirection,
        targetPath: '/interview',
        actionLabel: '启动面试',
        tone: difficultyText(interview?.difficulty)
      }]
    : []

  const profileHasSignal = Boolean(weakPoint || suggestedFocus || recommendedDifficulty)
  const planAndProfileActions = profileHasSignal
    ? [
        {
          id: 'study-plan',
          title: '生成专项学习计划',
          reason: weakPoint ? `围绕 ${weakPoint} 更新今日训练任务。` : '根据能力画像刷新下一轮学习计划。',
          weakPoint: weakPoint || '能力画像',
          targetPath: '/study-plan',
          actionLabel: '查看计划',
          tone: recommendedDifficulty
        },
        {
          id: 'profile',
          title: '查看能力画像',
          reason: suggestedFocus && suggestedFocus !== '-' ? suggestedFocus : '查看长期能力画像，确认推荐依据和薄弱项变化。',
          weakPoint: weakPoint || '能力画像',
          targetPath: '/analytics',
          actionLabel: '查看画像',
          tone: recommendedDifficulty
        }
      ]
    : []

  return [...questionActions, ...interviewAction, ...planAndProfileActions]
}

function buildBackendActions(data: unknown): RecommendationAction[] {
  return normalizeRecords<AnyRecord>(data).map((row, index) => {
    const targetPath = pickText(row, ['targetPath', 'actionPath', 'path', 'url'], '/question')
    const type = pickText(row, ['type'], 'recommendation')
    return {
      id: pickText(row, ['id'], `${type}-${index}`),
      title: pickText(row, ['title', 'name'], '推荐动作'),
      reason: pickText(row, ['reason', 'summary', 'description'], '基于后端能力画像推荐。'),
      weakPoint: pickText(row, ['weakPoint', 'categoryName', 'focus'], '能力画像'),
      targetPath,
      actionLabel: pickText(row, ['actionLabel', 'buttonText', 'cta'], '去处理'),
      tone: difficultyText(row.tone || row.difficulty || row.scoreLevel),
      priority: Number.isFinite(Number(row.priority)) ? Number(row.priority) : undefined,
      rank: Number.isFinite(Number(row.rank)) ? Number(row.rank) : index + 1,
      sourceIds: pickArray<string>(row, ['sourceIds'])
    }
  })
}

export function AdaptiveRecommendationsPanel({ compact = false }: { compact?: boolean }) {
  const recommendations = useQuery({ queryKey: ['adaptive', 'recommendations', 6], queryFn: () => fetchAdaptiveRecommendationsApi(6).then((response) => response.data) })
  const profile = useQuery({ queryKey: ['adaptive', 'profile'], queryFn: () => fetchAbilityProfileApi().then((response) => response.data) })
  const questions = useQuery({ queryKey: ['adaptive', 'questions', 3], queryFn: () => fetchRecommendQuestionsApi(3).then((response) => response.data) })
  const interview = useQuery({ queryKey: ['adaptive', 'interview'], queryFn: () => fetchRecommendInterviewApi().then((response) => response.data) })
  const backendActions = useMemo(() => buildBackendActions(recommendations.data), [recommendations.data])
  const fallbackActions = useMemo(
    () => buildActions(profile.data as AnyRecord | undefined, questions.data, interview.data as AnyRecord | undefined),
    [profile.data, questions.data, interview.data]
  )
  const actions = backendActions.length ? backendActions : fallbackActions
  const loading = recommendations.isLoading && !fallbackActions.length
    ? true
    : profile.isLoading || questions.isLoading || interview.isLoading
  const hasError = Boolean(recommendations.error && profile.error && questions.error && interview.error)

  return (
    <section role="region" aria-label="自适应推荐" className="adaptive-recommendation-panel">
      <Card
        title="自适应推荐"
        className="surface-card"
        loading={loading}
        extra={<Link to="/analytics">画像详情</Link>}
      >
        <Space orientation="vertical" size={12} style={{ width: '100%' }}>
          {hasError && !actions.length ? (
            <Alert
              type="warning"
              showIcon
              message="自适应推荐暂时不可用"
              description="可以刷新后重试，或先进入题库、学习计划继续训练。"
              action={<Button size="small" onClick={() => { recommendations.refetch(); profile.refetch(); questions.refetch(); interview.refetch() }}>重试</Button>}
            />
          ) : null}
          {!loading && !actions.length && !hasError ? (
            <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无自适应推荐">
              <Space wrap>
                <Link to="/question"><Button>去题库积累数据</Button></Link>
                <Link to="/study-plan"><Button type="primary">生成学习计划</Button></Link>
              </Space>
            </Empty>
          ) : null}
          {actions.length ? (
            <div className={compact ? 'adaptive-recommendation-list compact' : 'adaptive-recommendation-list'}>
              {actions.map((item) => (
                <article key={item.id} className="adaptive-recommendation-item">
                  <div className="adaptive-recommendation-copy">
                    <Space size={8} wrap>
                      <Typography.Text strong>{item.title}</Typography.Text>
                      {item.tone ? <Tag>{item.tone}</Tag> : null}
                    </Space>
                    <Typography.Paragraph className="muted-text">{item.reason}</Typography.Paragraph>
                    <Tag color="blue">关联薄弱项：{item.weakPoint}</Tag>
                  </div>
                  <Link to={item.targetPath}>
                    <Button type={item.rank === 1 || item.id === 'question-0' ? 'primary' : 'default'}>{item.actionLabel}</Button>
                  </Link>
                </article>
              ))}
            </div>
          ) : null}
        </Space>
      </Card>
    </section>
  )
}
