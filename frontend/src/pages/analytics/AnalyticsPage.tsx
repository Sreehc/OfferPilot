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
  const chartOption = {
    tooltip: { trigger: 'axis' },
    radar: { indicator: profileSeries.labels.map((name) => ({ name, max: 100 })) },
    series: [{ type: 'radar', data: [{ value: profileSeries.values, name: '能力画像' }] }]
  }
  return (
    <ModulePage
      title="数据分析"
      description="能力趋势、效率、画像和 topic retrospective。"
      actions={<Space><Button type="primary">刷新画像</Button></Space>}
      metrics={[
        { label: '能力画像', value: profileRecord?.overallScore ?? profileRecord?.score ?? '-', hint: '综合得分' },
        { label: '效率', value: (efficiency.data as any)?.efficiency ?? '-', hint: '学习效率' },
        { label: '趋势点', value: normalizeRecords(trend.data).length, hint: '时间序列' },
        { label: '洞察', value: normalizeRecords(insights.data).length, hint: '分析结果' }
      ]}
    >
      <div className="workspace-grid two">
        <EChartCard title="能力画像" option={chartOption} />
        <Card title="画像详情" className="surface-card">
          <Typography.Paragraph className="muted-text">{pickText(profileRecord, ['summary', 'description'], '暂无画像摘要')}</Typography.Paragraph>
          <DataListCard title="学习洞察" data={insights.data} renderItem={(item) => <div><strong>{pickText(item, ['title', 'name'])}</strong><div className="muted-text">{pickText(item, ['summary', 'description'])}</div></div>} />
        </Card>
      </div>
    </ModulePage>
  )
}
