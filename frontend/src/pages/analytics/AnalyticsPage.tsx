import { useQuery } from '@tanstack/react-query'
import { Button, Card, Space, Typography } from 'antd'
import { fetchAnalyticsProfileApi, fetchAbilityTrendApi, fetchEfficiencyApi, fetchLearningInsightsApi } from '@/api/modules/analytics'
import { EChartCard } from '@/components/charts/EChartCard'
import { buildCategorySeries, DataListCard, normalizeRecords, pickArray, pickText } from '@/modules/common'
import { ModulePage } from '@/modules/common'

export function AnalyticsPage() {
  const profile = useQuery({ queryKey: ['analytics', 'profile'], queryFn: () => fetchAnalyticsProfileApi().then((response) => response.data) })
  const trend = useQuery({ queryKey: ['analytics', 'trend'], queryFn: () => fetchAbilityTrendApi().then((response) => response.data) })
  const efficiency = useQuery({ queryKey: ['analytics', 'efficiency'], queryFn: () => fetchEfficiencyApi().then((response) => response.data) })
  const insights = useQuery({ queryKey: ['analytics', 'insights'], queryFn: () => fetchLearningInsightsApi().then((response) => response.data) })
  const profileRecord = profile.data as any
  const profileDimensions = pickArray<Record<string, unknown>>(profileRecord, ['dimensions', 'abilities', 'scores'])
  const profileSeries = buildCategorySeries(profileDimensions, ['name', 'label', 'category'], ['score', 'value', 'percent'])
  const trendRows = normalizeRecords(trend.data)
  const trendSeries = buildCategorySeries(trendRows, ['date', 'label', 'time', 'name'], ['score', 'value', 'count'])
  const radarOption = {
    tooltip: { trigger: 'item' },
    radar: { indicator: profileSeries.labels.map((name) => ({ name, max: 100 })) },
    series: [{ type: 'radar', data: [{ value: profileSeries.values, name: '能力画像' }] }]
  }
  const trendOption = {
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: trendSeries.labels },
    yAxis: { type: 'value' },
    series: [{ type: 'line', smooth: true, data: trendSeries.values, areaStyle: {} }]
  }
  const refreshing = profile.isFetching || trend.isFetching || efficiency.isFetching || insights.isFetching
  return (
    <ModulePage
      title="数据分析"
      description="能力画像、趋势、学习效率与学习洞察。"
      actions={<Space><Button type="primary" loading={refreshing} onClick={() => { profile.refetch(); trend.refetch(); efficiency.refetch(); insights.refetch() }}>刷新画像</Button></Space>}
      metrics={[
        { label: '能力画像', value: profileRecord?.overallScore ?? profileRecord?.score ?? '-', hint: '综合得分' },
        { label: '效率', value: (efficiency.data as any)?.efficiency ?? '-', hint: '学习效率' },
        { label: '趋势点', value: trendRows.length, hint: '时间序列' },
        { label: '洞察', value: normalizeRecords(insights.data).length, hint: '分析结果' }
      ]}
    >
      <div className="workspace-grid two">
        {profile.isLoading ? <Card title="能力画像" className="surface-card" loading /> : <EChartCard title="能力画像" option={radarOption} />}
        {trend.isLoading ? <Card title="能力趋势" className="surface-card" loading /> : <EChartCard title="能力趋势" option={trendOption} />}
      </div>
      <Card title="画像详情" className="surface-card">
        <Typography.Paragraph className="muted-text">{pickText(profileRecord, ['summary', 'description'], '暂无画像摘要')}</Typography.Paragraph>
        <DataListCard
          title="学习洞察"
          data={insights.data}
          loading={insights.isLoading}
          error={insights.error}
          onRetry={() => insights.refetch()}
          emptyTitle="暂无学习洞察，多积累训练数据后生成"
          renderItem={(item) => <div><strong>{pickText(item, ['title', 'name'])}</strong><div className="muted-text">{pickText(item, ['summary', 'description'])}</div></div>}
        />
      </Card>
    </ModulePage>
  )
}
