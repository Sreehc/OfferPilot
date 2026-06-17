import { useQuery } from '@tanstack/react-query'
import { Card, Descriptions, Timeline } from 'antd'
import { useParams } from 'react-router-dom'
import { interviewDetailApi } from '@/api/modules/interview'
import { DataListCard, normalizeRecords, pickText } from '@/modules/common'
import { ModulePage } from '@/modules/common'

export function InterviewDetailPage() {
  const params = useParams()
  const detail = useQuery({ queryKey: ['interview', 'detail', params.id], queryFn: () => interviewDetailApi(String(params.id)).then((response) => response.data), enabled: Boolean(params.id) })
  const current = detail.data as any
  const qaRows = normalizeRecords(current?.qaList || current?.questions || current?.records)
  const retros = normalizeRecords(current?.retrospective || current?.analysis || current?.insights)
  return (
    <ModulePage
      title="面试详情"
      description="查看单次面试的题目、回答和 Agent 复盘。"
      metrics={[
        { label: '状态', value: current?.status || '-', hint: '当前会话' },
        { label: '题目数', value: qaRows.length, hint: '问答记录' },
        { label: '复盘数', value: retros.length, hint: '分析输出' },
        { label: '更新时间', value: current?.updateTime || current?.createdAt || '-', hint: '最近记录' }
      ]}
    >
      <Card className="surface-card" loading={detail.isLoading}>
        <Descriptions column={1} items={[
          { label: 'Session ID', children: params.id },
          { label: '状态', children: current?.status || '-' },
          { label: '结论', children: current?.summary || current?.resultSummary || '-' }
        ]} />
      </Card>
      <Card title="复盘时间线" className="surface-card">
        <Timeline items={[
          { children: pickText(current, ['opening', 'intro'], '等待后端返回复盘步骤') }
        ]} />
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
