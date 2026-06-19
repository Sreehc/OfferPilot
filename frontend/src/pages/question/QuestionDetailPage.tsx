import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { useNavigate, useParams } from 'react-router-dom'
import { Button, Card, Result, Space, Tag, Typography } from 'antd'
import { ArrowLeftOutlined } from '@ant-design/icons'
import { fetchQuestionDetailApi } from '@/api/modules/question'
import { getErrorMessage } from '@/api/client'
import { difficultyLabel, pickText } from '@/modules/common'
import { ModulePage } from '@/modules/common'

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
  const tags = pickText(record, ['tags', 'type'], '')

  return (
    <ModulePage
      title="题目详情"
      description="阅读题目，自己作答后对照参考解析。"
      actions={<Button icon={<ArrowLeftOutlined />} onClick={() => navigate('/question')}>返回题库</Button>}
    >
      {detail.error ? (
        <Result status="warning" title="题目加载失败" subTitle={getErrorMessage(detail.error)} extra={<Space><Button onClick={() => detail.refetch()}>重试</Button><Button type="primary" onClick={() => navigate('/question')}>返回题库</Button></Space>} />
      ) : (
        <div className="workspace-grid two">
          <Card className="surface-card" loading={detail.isLoading} title={pickText(record, ['title', 'name'], '题目')}>
            <Space wrap style={{ marginBottom: 12 }}>
              <Tag color="blue">{difficultyLabel(pickText(record, ['difficulty']))}</Tag>
              <Tag>{pickText(record, ['categoryName', 'category'], '未分类')}</Tag>
              {tags !== '-' && tags !== '' && <Tag>{tags}</Tag>}
            </Space>
            <Typography.Paragraph style={{ whiteSpace: 'pre-wrap' }}>
              {pickText(record, ['content', 'stem', 'description', 'questionContent'], record ? '本题暂无题干内容' : '')}
            </Typography.Paragraph>
          </Card>
          <Space orientation="vertical" style={{ width: '100%' }}>
            <Card className="surface-card" title="我的作答">
              <Typography.Paragraph type="secondary" style={{ marginBottom: 8 }}>先尝试自己作答，再对照参考解析，效果更好。</Typography.Paragraph>
              <Typography.Paragraph>
                <Typography.Text editable={{ onChange: setDraft, text: draft }}>{draft || '点击此处开始作答…'}</Typography.Text>
              </Typography.Paragraph>
              <Button type="primary" onClick={() => setRevealed(true)}>对照参考解析</Button>
            </Card>
            {revealed && (
              <Card className="surface-card" title="参考解析">
                {answer ? (
                  <Typography.Paragraph style={{ whiteSpace: 'pre-wrap' }}>{answer}</Typography.Paragraph>
                ) : (
                  <Typography.Text type="secondary">该题暂无参考解析。</Typography.Text>
                )}
              </Card>
            )}
          </Space>
        </div>
      )}
    </ModulePage>
  )
}
