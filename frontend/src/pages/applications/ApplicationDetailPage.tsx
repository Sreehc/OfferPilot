import { useParams } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { Card, Descriptions, List } from 'antd'
import { fetchApplicationDetailApi } from '@/api/modules/applications'
import { DataListCard, formatDateTime, normalizeRecords, pickText, StatusTag } from '@/modules/common'
import { ModulePage } from '@/modules/common'

export function ApplicationDetailPage() {
  const params = useParams()
  const detail = useQuery({
    queryKey: ['applications', params.id],
    queryFn: () => fetchApplicationDetailApi(String(params.id)).then((response) => response.data),
    enabled: Boolean(params.id)
  })
  const record = detail.data as any
  const events = normalizeRecords(record?.events || record?.timeline || record?.records)
  const suggestions = normalizeRecords(record?.suggestions || record?.analysis?.suggestions || record?.nextActions)
  return (
    <ModulePage
      title="投递详情"
      description="查看某一岗位的状态、事件和分析建议。"
      metrics={[
        { label: '状态', value: pickText(record, ['status'], '-'), hint: '当前阶段' },
        { label: '匹配度', value: pickText(record, ['matchScore', 'score'], '-'), hint: '岗位匹配' },
        { label: '事件', value: events.length, hint: '跟进记录' },
        { label: '建议', value: suggestions.length, hint: '分析输出' }
      ]}
    >
      <div className="workspace-grid two">
        <Card className="surface-card" loading={detail.isLoading}>
          <Descriptions column={1} items={[
            { label: 'Application ID', children: params.id },
            { label: '公司', children: pickText(record, ['companyName', 'company']) },
            { label: '岗位', children: pickText(record, ['position', 'jobTitle']) },
            { label: '状态', children: <StatusTag value={pickText(record, ['status'])} /> },
            { label: '更新时间', children: formatDateTime(record?.updateTime || record?.updatedAt) }
          ]} />
        </Card>
        <DataListCard
          title="分析建议"
          data={suggestions}
          loading={detail.isLoading}
          error={detail.error}
          onRetry={() => detail.refetch()}
          emptyTitle="暂无分析建议"
          renderItem={(item) => <List.Item.Meta title={pickText(item, ['title', 'name', 'summary'])} description={pickText(item, ['description', 'content'])} />}
        />
        <DataListCard
          title="事件时间线"
          data={events}
          loading={detail.isLoading}
          error={detail.error}
          onRetry={() => detail.refetch()}
          emptyTitle="暂无事件"
          renderItem={(item) => <List.Item.Meta title={pickText(item, ['title', 'type', 'status'])} description={formatDateTime(item.time || item.createTime || item.createdAt)} />}
        />
      </div>
    </ModulePage>
  )
}
