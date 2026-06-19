import { useQuery } from '@tanstack/react-query'
import { Button, Card, Descriptions, Timeline } from 'antd'
import { ArrowLeftOutlined } from '@ant-design/icons'
import { useNavigate, useParams } from 'react-router-dom'
import { interviewDetailApi } from '@/api/modules/interview'
import { DataListCard, formatDateTime, labelOf, normalizeRecords, pickText } from '@/modules/common'
import { ModulePage } from '@/modules/common'

export function InterviewDetailPage() {
  const params = useParams()
  const navigate = useNavigate()
  const detail = useQuery({ queryKey: ['interview', 'detail', params.id], queryFn: () => interviewDetailApi(String(params.id)).then((response) => response.data), enabled: Boolean(params.id) })
  const current = detail.data as any
  const qaRows = normalizeRecords(current?.qaList || current?.questions || current?.records)
  const retros = normalizeRecords(current?.retrospective || current?.analysis || current?.insights)
  return (
    <ModulePage
      title="面试详情"
      description="查看单次面试的题目、回答和复盘建议。"
      actions={<Button icon={<ArrowLeftOutlined />} onClick={() => navigate('/interview')}>返回面试列表</Button>}
      metrics={[
        { label: '状态', value: labelOf(current?.status, '-'), hint: '当前会话' },
        { label: '题目数', value: qaRows.length, hint: '问答记录' },
        { label: '复盘数', value: retros.length, hint: '分析输出' },
        { label: '更新时间', value: formatDateTime(current?.updateTime || current?.createdAt), hint: '最近记录' }
      ]}
    >
      <Card className="surface-card" loading={detail.isLoading}>
        <Descriptions column={1} items={[
          { label: '会话 ID', children: params.id },
          { label: '状态', children: labelOf(current?.status, '-') },
          { label: '结论', children: current?.summary || current?.resultSummary || '-' }
        ]} />
      </Card>
      <Card title="复盘时间线" className="surface-card">
        {retros.length === 0 ? (
          <div className="muted-text">{detail.isLoading ? '加载中…' : '本次面试暂无复盘记录。'}</div>
        ) : (
          <Timeline items={retros.map((item) => ({
            children: (
              <div>
                <strong>{pickText(item, ['title', 'stage', 'dimension', 'name'], '复盘')}</strong>
                <div>{pickText(item, ['content', 'comment', 'suggestion', 'description', 'summary'], '-')}</div>
              </div>
            )
          }))} />
        )}
      </Card>
      <DataListCard
        title="问答记录"
        data={qaRows}
        loading={detail.isLoading}
        error={detail.error}
        onRetry={() => detail.refetch()}
        emptyTitle="暂无问答记录"
        renderItem={(item) => <Descriptions column={1} items={[
          { label: '问题', children: pickText(item, ['question', 'title']) },
          { label: '回答', children: pickText(item, ['answer', 'content', 'response']) }
        ]} />}
      />
    </ModulePage>
  )
}
