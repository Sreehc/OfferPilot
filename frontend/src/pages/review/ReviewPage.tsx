import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { App as AntApp, Rate, Tag } from 'antd'
import { fetchReviewStatsApi, fetchReviewTodayApi, submitReviewRateApi } from '@/api/modules/review'
import { getErrorMessage } from '@/api/client'
import { contentTypeLabel, DataTableCard, normalizeRecords, pickText, StatusTag } from '@/modules/common'
import { ModulePage } from '@/modules/common'

export function ReviewPage() {
  const { message, modal } = AntApp.useApp()
  const queryClient = useQueryClient()
  const today = useQuery({ queryKey: ['review', 'today'], queryFn: () => fetchReviewTodayApi().then((response) => response.data) })
  const stats = useQuery({ queryKey: ['review', 'stats'], queryFn: () => fetchReviewStatsApi().then((response) => response.data) })
  const rate = useMutation({ mutationFn: ({ id, score }: { id: string; score: number }) => submitReviewRateApi(id, { score }), onSuccess: () => { message.success('复习评分已提交'); queryClient.invalidateQueries({ queryKey: ['review'] }) }, onError: (error) => message.error(getErrorMessage(error, '评分失败')) })
  const confirmRate = (id: string, score: number) => modal.confirm({ title: '确认提交本次评分？', content: `你的掌握评分：${score} 星，提交后将更新复习计划。`, okText: '提交', cancelText: '取消', onOk: () => rate.mutate({ id, score }) })
  const rows = normalizeRecords(today.data)
  return (
    <ModulePage
      title="复习巩固"
      description="今日复习、评分和统计。"
      metrics={[
        { label: '今日复习', value: rows.length, hint: '待处理条目' },
        { label: '完成率', value: (stats.data as any)?.completionRate ?? '-', hint: '近期统计' },
        { label: '平均评分', value: (stats.data as any)?.avgScore ?? '-', hint: '自评结果' },
        { label: '状态', value: today.isFetching ? '刷新中' : '已同步', hint: '数据状态' }
      ]}
    >
      <DataTableCard
        title="今日复习"
        data={today.data}
        loading={today.isLoading}
        error={today.error}
        onRetry={() => today.refetch()}
        emptyTitle="今日暂无待复习内容，去题库练习生成更多复习项"
        columns={[
          { title: '内容', render: (_, row) => pickText(row, ['title', 'contentTitle', 'name'], '复习项') },
          { title: '类型', render: (_, row) => <Tag>{contentTypeLabel(pickText(row, ['contentType', 'type']))}</Tag> },
          { title: '状态', render: (_, row) => <StatusTag value={pickText(row, ['status'])} /> },
          { title: '评分', render: (_, row) => <Rate allowHalf defaultValue={Number(row.score || 0)} onChange={(score) => confirmRate(String(row.id || row.reviewItemId), score)} /> },
          { title: '说明', render: (_, row) => pickText(row, ['summary', 'description'], '-') }
        ]}
      />
    </ModulePage>
  )
}
