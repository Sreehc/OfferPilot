import { useMemo, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Button, Card, Empty, Progress, Space, Tag, Typography } from 'antd'
import { ArrowLeftOutlined } from '@ant-design/icons'
import { useNavigate, useParams } from 'react-router-dom'
import { interviewDetailApi } from '@/api/modules/interview'
import type { AnyRecord } from '@/api/types'
import { formatDateTime, labelOf, normalizeRecords, pickNumber, pickText } from '@/modules/common'
import { ModulePage } from '@/modules/common'

const { Paragraph, Text, Title } = Typography

const LONG_ANSWER_LIMIT = 80
const ANSWER_PREVIEW_LIMIT = 28

interface AbilityItem {
  dimension: string
  score: number
  summary: string
}

interface WeakRecord {
  id: string
  title: string
  score: number
  comment: string
  summary: string
  tags: string[]
  actionPath?: string
}

function toFiniteNumber(value: unknown, fallback = 0) {
  const number = Number(value)
  return Number.isFinite(number) ? number : fallback
}

function clampScore(value: unknown) {
  return Math.max(0, Math.min(100, Math.round(toFiniteNumber(value, 0))))
}

function uniqueTexts(values: unknown[]) {
  return Array.from(
    new Set(
      values
        .flatMap((value) => Array.isArray(value) ? value : [value])
        .map((value) => String(value ?? '').trim())
        .filter(Boolean)
    )
  )
}

function deriveAbilityItems(records: AnyRecord[]): AbilityItem[] {
  const groups = new Map<string, { total: number; count: number; summaries: string[] }>()

  records.forEach((record) => {
    normalizeRecords<AnyRecord>(record.scoreBreakdown || record.breakdown || record.dimensions).forEach((item) => {
      const dimension = pickText(item, ['dimension', 'name', 'title'], '')
      if (!dimension) return
      const current = groups.get(dimension) || { total: 0, count: 0, summaries: [] }
      current.total += clampScore(item.score ?? item.value)
      current.count += 1
      const summary = pickText(item, ['summary', 'comment', 'description'], '')
      if (summary) current.summaries.push(summary)
      groups.set(dimension, current)
    })
  })

  if (groups.size === 0) {
    const scoredRecords = records.filter((record) => Number.isFinite(Number(record.score)))
    if (scoredRecords.length === 0) return []
    const average = Math.round(scoredRecords.reduce((sum, record) => sum + clampScore(record.score), 0) / scoredRecords.length)
    return [{ dimension: '综合表现', score: average, summary: '基于问答得分自动汇总。' }]
  }

  return Array.from(groups.entries()).map(([dimension, value]) => ({
    dimension,
    score: Math.round(value.total / Math.max(1, value.count)),
    summary: value.summaries[0] || '暂无维度说明'
  }))
}

function deriveWeakRecords(records: AnyRecord[]): WeakRecord[] {
  return records
    .filter((record) => Boolean(record.isLowScore) || clampScore(record.score) < 60 || uniqueTexts([record.weakPointTags]).length > 0)
    .map((record, index) => ({
      id: String(record.questionId ?? record.wrongQuestionId ?? record.id ?? index),
      title: pickText(record, ['questionTitle', 'question', 'title'], `第 ${index + 1} 题`),
      score: clampScore(record.score),
      comment: pickText(record, ['comment', 'feedback'], '暂无点评'),
      summary: pickText(record, ['reviewSummary', 'summary', 'analysis'], ''),
      tags: uniqueTexts([record.weakPointTags, record.tags])
    }))
}

function deriveNextTasks(records: AnyRecord[]) {
  return uniqueTexts(records.map((record) => record.followUp || record.nextTask || record.task)).slice(0, 5)
}

function mapBackendAbilityItems(data: unknown): AbilityItem[] {
  return normalizeRecords<AnyRecord>(data).map((item, index) => ({
    dimension: pickText(item, ['dimension', 'name', 'title'], `能力维度 ${index + 1}`),
    score: clampScore(item.score ?? item.value),
    summary: pickText(item, ['summary', 'comment', 'description'], '暂无维度说明')
  }))
}

function mapBackendWeakRecords(data: unknown): WeakRecord[] {
  return normalizeRecords<AnyRecord>(data).map((item, index) => ({
    id: String(item.questionId ?? item.wrongQuestionId ?? item.id ?? index),
    title: pickText(item, ['title', 'questionTitle', 'question'], `薄弱项 ${index + 1}`),
    score: clampScore(item.score),
    comment: pickText(item, ['comment', 'feedback'], '暂无点评'),
    summary: pickText(item, ['summary', 'reviewSummary', 'analysis'], ''),
    tags: uniqueTexts([item.tags, item.weakPointTags]),
    actionPath: pickText(item, ['actionPath', 'targetPath'], '')
  }))
}

function readBackendNextTasks(data: unknown) {
  if (Array.isArray(data)) return uniqueTexts(data)
  return uniqueTexts(normalizeRecords<AnyRecord>(data).map((item) => pickText(item, ['title', 'task', 'summary', 'content'], '')))
}

function collapsedAnswer(answer: string) {
  if (answer.length <= LONG_ANSWER_LIMIT) return answer
  return `${answer.slice(0, ANSWER_PREVIEW_LIMIT)}...`
}

export function InterviewDetailPage() {
  const params = useParams()
  const navigate = useNavigate()
  const [expandedAnswers, setExpandedAnswers] = useState<Record<string, boolean>>({})
  const detail = useQuery({
    queryKey: ['interview', 'detail', params.id],
    queryFn: () => interviewDetailApi(String(params.id)).then((response) => response.data),
    enabled: Boolean(params.id)
  })
  const current = detail.data as AnyRecord | undefined
  const qaRows = useMemo(() => normalizeRecords<AnyRecord>(current?.qaList || current?.questions || current?.records), [current])
  const abilityItems = useMemo(() => {
    const backendItems = mapBackendAbilityItems(current?.abilityItems)
    return backendItems.length ? backendItems : deriveAbilityItems(qaRows)
  }, [current, qaRows])
  const weakRecords = useMemo(() => {
    const backendRecords = mapBackendWeakRecords(current?.weakRecords)
    return backendRecords.length ? backendRecords : deriveWeakRecords(qaRows)
  }, [current, qaRows])
  const nextTasks = useMemo(() => {
    const backendTasks = readBackendNextTasks(current?.nextTasks)
    return backendTasks.length ? backendTasks : deriveNextTasks(qaRows)
  }, [current, qaRows])
  const totalScore = current?.totalScore ?? current?.score ?? (
    qaRows.length ? Math.round(qaRows.reduce((sum, item) => sum + pickNumber(item, ['score'], 0), 0) / qaRows.length) : undefined
  )
  const scoreText = totalScore === undefined || totalScore === null ? '-' : String(clampScore(totalScore))
  const contextTitle = [current?.direction, current?.jobRole].map((value) => String(value ?? '').trim()).filter(Boolean).join(' / ') || '面试会话'
  const reviewSummary = pickText(
    current,
    ['summary', 'resultSummary', 'reviewSummary'],
    weakRecords[0]?.summary || weakRecords[0]?.comment || '暂无结构化复盘建议。'
  )

  return (
    <ModulePage
      title="面试详情"
      description="查看单次面试的题目、回答和复盘建议。"
      actions={<Button icon={<ArrowLeftOutlined />} onClick={() => navigate('/interview')}>返回面试列表</Button>}
      metrics={[
        { label: '状态', value: labelOf(current?.status as string | undefined, '-'), hint: '当前会话' },
        { label: '综合评分', value: scoreText, hint: 'AI 复盘' },
        { label: '题目数', value: qaRows.length, hint: '问答记录' },
        { label: '结束时间', value: formatDateTime(current?.endTime || current?.updateTime || current?.createdAt), hint: '最近记录' }
      ]}
      state={{
        loading: detail.isLoading,
        error: detail.error,
        onRetry: () => { void detail.refetch() },
        errorTitle: '面试详情加载失败',
        errorDescription: '暂时无法读取本次面试复盘，请稍后重试。'
      }}
    >
      <div className="interview-review-layout">
        <section className="interview-review-hero surface-card" role="region" aria-label="AI 复盘报告">
          <div className="interview-review-summary">
            <Text className="interview-review-eyebrow">AI 复盘报告</Text>
            <Title level={2}>{contextTitle}</Title>
            <Paragraph type="secondary">{reviewSummary}</Paragraph>
            <Space wrap size={[8, 8]}>
              <Tag color="blue">{labelOf(current?.mode as string | undefined, '标准面试')}</Tag>
              <Tag>{labelOf(current?.status as string | undefined, '-')}</Tag>
              {current?.durationMinutes && <Tag>{current.durationMinutes} 分钟</Tag>}
              {current?.questionCount && <Tag>{current.questionCount} 题</Tag>}
            </Space>
          </div>
          <div className="interview-review-score" aria-label="综合评分">
            <Text type="secondary">综合评分</Text>
            <strong>{scoreText}</strong>
            <Progress percent={scoreText === '-' ? 0 : Number(scoreText)} showInfo={false} status={Number(scoreText) < 60 ? 'exception' : 'normal'} />
            <Text type="secondary">低分项会在下方优先展示，便于安排下一轮训练。</Text>
          </div>
        </section>

        <div className="interview-review-grid">
          <section role="region" aria-label="能力图">
            <Card title="能力图" className="surface-card interview-ability-card">
              {abilityItems.length === 0 ? (
                <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无能力评分" />
              ) : (
                <div className="interview-ability-list">
                  {abilityItems.map((item) => (
                    <div key={item.dimension} className={item.score < 60 ? 'interview-ability-item is-low-score' : 'interview-ability-item'}>
                      <div className="interview-ability-head">
                        <Text strong>{item.dimension}</Text>
                        <Space size={6}>
                          {item.score < 60 && <Tag color="red">薄弱</Tag>}
                          <Text strong>{item.score}</Text>
                        </Space>
                      </div>
                      <Progress percent={item.score} showInfo={false} status={item.score < 60 ? 'exception' : 'normal'} />
                      <Text type="secondary">{item.summary}</Text>
                    </div>
                  ))}
                </div>
              )}
            </Card>
          </section>

          <section role="region" aria-label="薄弱项和改进任务">
            <Card title="薄弱项和改进任务" className="surface-card interview-weak-card">
              {weakRecords.length === 0 && nextTasks.length === 0 ? (
                <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无薄弱项，建议继续保持训练节奏。" />
              ) : (
                <div className="interview-weak-panel">
                  {weakRecords.slice(0, 4).map((record) => (
                    <div key={record.id} className="interview-weak-item">
                      <div className="interview-weak-head">
                        <Text strong>{record.title}</Text>
                        {record.score < 60 && <Tag color="red">低分重点</Tag>}
                      </div>
                      <Paragraph type="secondary">{record.summary || record.comment}</Paragraph>
                      {record.tags.length > 0 && (
                        <Space wrap size={[6, 6]}>
                          {record.tags.map((tag) => <Tag key={tag}>{tag}</Tag>)}
                        </Space>
                      )}
                    </div>
                  ))}
                  {nextTasks.length > 0 && (
                    <ul className="interview-task-list" aria-label="后续任务">
                      {nextTasks.map((task) => <li key={task}>{task}</li>)}
                    </ul>
                  )}
                </div>
              )}
            </Card>
          </section>
        </div>

        <section role="region" aria-label="问答复盘">
          <Card title="问答复盘" className="surface-card">
            {qaRows.length === 0 ? (
              <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无问答记录" />
            ) : (
              <div className="interview-qa-list">
                {qaRows.map((record, index) => {
                  const id = String(record.questionId ?? record.id ?? index)
                  const answer = pickText(record, ['userAnswer', 'answer', 'content', 'response'], '暂无回答')
                  const isLong = answer.length > LONG_ANSWER_LIMIT
                  const expanded = Boolean(expandedAnswers[id])
                  const score = clampScore(record.score)
                  const tags = uniqueTexts([record.weakPointTags, record.tags])
                  return (
                    <article key={id} className={score < 60 || record.isLowScore ? 'interview-qa-card is-low-score' : 'interview-qa-card'}>
                      <div className="interview-qa-head">
                        <div>
                          <Text type="secondary">第 {index + 1} 题</Text>
                          <Title level={4}>{pickText(record, ['questionTitle', 'question', 'title'], `第 ${index + 1} 题`)}</Title>
                        </div>
                        <Space size={6} wrap>
                          {score < 60 && <Tag color="red">低分重点</Tag>}
                          <Tag color={score < 60 ? 'red' : 'green'}>{score} 分</Tag>
                        </Space>
                      </div>
                      <div className="interview-answer-block">
                        <Text strong>我的回答</Text>
                        <Paragraph>{isLong && !expanded ? collapsedAnswer(answer) : answer}</Paragraph>
                        {isLong && (
                          <Button
                            type="link"
                            className="interview-answer-toggle"
                            onClick={() => setExpandedAnswers((previous) => ({ ...previous, [id]: !expanded }))}
                          >
                            {expanded ? '收起问答' : '展开完整问答'}
                          </Button>
                        )}
                      </div>
                      <div className="interview-review-note">
                        <Text strong>复盘建议</Text>
                        <Paragraph type="secondary">{pickText(record, ['reviewSummary', 'comment', 'analysis'], '暂无复盘建议')}</Paragraph>
                      </div>
                      {tags.length > 0 && (
                        <Space wrap size={[6, 6]}>
                          {tags.map((tag) => <Tag key={tag}>{tag}</Tag>)}
                        </Space>
                      )}
                    </article>
                  )
                })}
              </div>
            )}
          </Card>
        </section>
      </div>
    </ModulePage>
  )
}
