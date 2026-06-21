import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { Button, Card, Empty, Input, Result, Space, Tag, Typography } from 'antd'
import { ArrowLeftOutlined } from '@ant-design/icons'
import { fetchQuestionDetailApi } from '@/api/modules/question'
import { getErrorMessage } from '@/api/client'
import { AdaptiveRecommendationsPanel } from '@/components/adaptive/AdaptiveRecommendationsPanel'
import { difficultyLabel, pickArray, pickText } from '@/modules/common'
import { ModulePage } from '@/modules/common'

function normalizeTextItems(input: unknown): string[] {
  return pickArray<string | Record<string, unknown>>({ input }, ['input'])
    .map((item) => typeof item === 'string' ? item : pickText(item, ['title', 'content', 'question', 'prompt', 'text'], ''))
    .filter((item) => item && item !== '-')
}

export function QuestionDetailPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const [draft, setDraft] = useState('')
  const [revealed, setRevealed] = useState(false)
  const detail = useQuery({
    queryKey: ['question', id],
    queryFn: () => fetchQuestionDetailApi(Number(id)).then((response) => response.data),
    enabled: Boolean(id)
  })
  const record = detail.data as any
  const answer = pickText(record, ['standardAnswer', 'answer', 'analysis', 'explanation', 'referenceAnswer'], '')
  const aiAnalysis = pickText(record, ['aiAnalysis', 'aiExplanation', 'assistantAnalysis', 'analysisSuggestion'], '')
  const followUps = normalizeTextItems(record?.followUpPrompts || record?.followUps || record?.aiFollowUps || record?.prompts)
  const relatedQuestions = pickArray<Record<string, unknown>>(record, ['relatedQuestions', 'similarQuestions', 'sameCategoryQuestions'])
  const tags = pickText(record, ['tags', 'type'], '')
  const questionId = Number(id || record?.id || record?.questionId)
  const canReveal = draft.trim().length > 0
  const favoritePath = Number.isFinite(questionId) ? `/favorites?targetType=question&targetId=${questionId}` : '/favorites'
  const wrongPath = Number.isFinite(questionId) ? `/wrong?questionId=${questionId}` : '/wrong'

  return (
    <ModulePage
      title="题目详情"
      description="先独立作答，再对照 AI 解析、参考答案和追问，形成更接近真实面试的训练闭环。"
      actions={<Button icon={<ArrowLeftOutlined />} onClick={() => navigate('/question')}>返回题库</Button>}
    >
      {detail.error ? (
        <Result status="warning" title="题目加载失败" subTitle={getErrorMessage(detail.error)} extra={<Space><Button onClick={() => detail.refetch()}>重试</Button><Button type="primary" onClick={() => navigate('/question')}>返回题库</Button></Space>} />
      ) : (
        <div className="question-training-layout">
          <div className="question-training-main">
            <Card className="surface-card question-stem-card" loading={detail.isLoading} title={pickText(record, ['title', 'name'], '题目')}>
              <Space wrap style={{ marginBottom: 12 }}>
                <Tag color="blue">{difficultyLabel(pickText(record, ['difficulty']))}</Tag>
                <Tag>{pickText(record, ['categoryName', 'category'], '未分类')}</Tag>
                {tags !== '-' && tags !== '' && <Tag>{tags}</Tag>}
              </Space>
              <Typography.Paragraph style={{ whiteSpace: 'pre-wrap' }}>
                {pickText(record, ['content', 'stem', 'description', 'questionContent'], record ? '本题暂无题干内容' : '')}
              </Typography.Paragraph>
            </Card>

            <Card className="surface-card" title="训练作答区">
              <section role="region" aria-label="训练作答区" className="question-answer-panel">
                <Typography.Paragraph className="muted-text">
                  先用自己的语言写出结论、原因、边界和项目例子，再查看解析。
                </Typography.Paragraph>
                <Input.TextArea
                  aria-label="我的作答"
                  value={draft}
                  rows={7}
                  maxLength={3000}
                  showCount
                  placeholder="写下你的面试回答。建议先给结论，再补充原理、风险和替代方案。"
                  onChange={(event) => setDraft(event.target.value)}
                />
                <Space wrap>
                  <Button type="primary" disabled={!canReveal} onClick={() => setRevealed(true)}>
                    提交作答并查看解析
                  </Button>
                  <Button onClick={() => { setDraft(''); setRevealed(false) }}>重新作答</Button>
                </Space>
                {!revealed ? (
                  <div className="question-analysis-locked">
                    <Typography.Text strong>未作答前先隐藏参考解析</Typography.Text>
                    <Typography.Text className="muted-text">这样可以避免直接背答案，先暴露自己的表达缺口。</Typography.Text>
                  </div>
                ) : null}
              </section>
            </Card>

            {revealed && (
              <div className="question-analysis-grid">
                <Card className="surface-card" title="参考解析">
                  {answer ? (
                    <Typography.Paragraph style={{ whiteSpace: 'pre-wrap' }}>{answer}</Typography.Paragraph>
                  ) : (
                    <Typography.Text type="secondary">该题暂无参考解析。</Typography.Text>
                  )}
                </Card>
                <Card className="surface-card" title="AI 解析">
                  {aiAnalysis ? (
                    <Typography.Paragraph style={{ whiteSpace: 'pre-wrap' }}>{aiAnalysis}</Typography.Paragraph>
                  ) : (
                    <Typography.Text type="secondary">暂无 AI 解析建议。</Typography.Text>
                  )}
                </Card>
              </div>
            )}
          </div>

          <aside className="question-training-side">
            <AdaptiveRecommendationsPanel compact />
            <Card className="surface-card" title="AI 追问">
              {revealed && followUps.length ? (
                <ul className="question-side-list">
                  {followUps.map((item) => (
                    <li key={item}>{item}</li>
                  ))}
                </ul>
              ) : (
                <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description={revealed ? '暂无 AI 追问' : '提交作答后展示追问'} />
              )}
            </Card>
            <Card className="surface-card" title="同类题">
              {relatedQuestions.length ? (
                <ul className="question-side-list question-related-list">
                  {relatedQuestions.map((item, index) => {
                    const targetId = item.id || item.questionId
                    const title = pickText(item, ['title', 'name', 'questionTitle'], '同类题')
                    return (
                      <li key={String(targetId || title || index)}>
                        <Link className="question-related-link" to={targetId ? `/question/${targetId}` : '/question'}>{title}</Link>
                        <div className="question-related-meta">
                          <Tag>{difficultyLabel(pickText(item, ['difficulty']))}</Tag>
                          {pickText(item, ['categoryName', 'category'], '') ? <Tag>{pickText(item, ['categoryName', 'category'])}</Tag> : null}
                        </div>
                      </li>
                    )
                  })}
                </ul>
              ) : (
                <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无同类题" />
              )}
            </Card>
            <Card className="surface-card" title="训练沉淀">
              <section role="region" aria-label="训练沉淀入口">
                <div className="question-action-list">
                  <Link to={favoritePath}><Button block>收藏本题</Button></Link>
                  <Link to={wrongPath}><Button block>加入错题复习</Button></Link>
                  <Link to={`/chat?questionId=${Number.isFinite(questionId) ? questionId : ''}`}><Button block>带题目追问 AI</Button></Link>
                </div>
              </section>
            </Card>
          </aside>
        </div>
      )}
    </ModulePage>
  )
}
