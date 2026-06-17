import { useQuery } from '@tanstack/react-query'
import { Button, Card, List, Progress, Space, Typography } from 'antd'
import { Link } from 'react-router-dom'
import { fetchDashboardOverviewApi } from '@/api/modules/dashboard'
import { EChartCard } from '@/components/charts/EChartCard'
import { buildCategorySeries, DataListCard, normalizeRecords, pickArray, pickText } from '@/modules/common'
import { ModulePage } from '@/modules/common'

export function DashboardPage() {
  const query = useQuery({ queryKey: ['dashboard', 'overview'], queryFn: () => fetchDashboardOverviewApi().then((response) => response.data) })
  const overview = query.data as any
  const activities = normalizeRecords(overview?.recentActivities || overview?.activities || overview?.records)
  const trendRecords = normalizeRecords(overview?.trend || overview?.trainingTrend || overview?.reviewTrend)
  const trend = buildCategorySeries(trendRecords, ['label', 'date', 'day', 'name'], ['value', 'count', 'score', 'completed'])
  const weakPoints = pickArray<string | Record<string, unknown>>(overview, ['weakPoints', 'weaknesses', 'riskItems'])
  const agentOutputs = pickArray<string | Record<string, unknown>>(overview, ['agentOutputs', 'agentSuggestions', 'nextActions'])
  const chartOption = {
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: trend.labels },
    yAxis: { type: 'value' },
    series: [{ type: 'line', smooth: true, data: trend.values, areaStyle: {} }]
  }
  const metrics = [
    { label: '今日训练', value: overview?.todayTasks ?? overview?.todayTaskCount ?? '-', hint: '待完成动作' },
    { label: '能力画像', value: overview?.profileScore ?? overview?.abilityScore ?? '-', hint: '综合画像得分' },
    { label: '待复盘', value: overview?.reviewCount ?? overview?.pendingReviewCount ?? '-', hint: '需要回顾的条目' },
    { label: 'Agent 建议', value: overview?.agentHints ?? overview?.agentSuggestionCount ?? agentOutputs.length, hint: '可直接执行' }
  ]
  return (
    <ModulePage
      title="Dashboard"
      description="把训练、复盘、投递、面试和 Agent 建议收拢到统一工作台。"
      metrics={metrics}
      actions={<Space><Link to="/interview"><Button type="primary">开始面试</Button></Link><Link to="/study-plan"><Button>查看计划</Button></Link></Space>}
    >
      <div className="workspace-grid two">
        <div className="workspace-grid">
          <EChartCard title="训练趋势" option={chartOption} />
          <DataListCard
            title="最近行动"
            data={activities}
            renderItem={(item) => <List.Item.Meta title={pickText(item, ['name', 'title'], '行动')} description={pickText(item, ['time', 'description'], '')} />}
          />
        </div>
        <div className="workspace-grid">
          <Card title="下一步行动" className="surface-card">
            <Space direction="vertical" style={{ width: '100%' }}>
              <Progress percent={Number(overview?.progress ?? overview?.completionRate ?? 0)} />
              <Typography.Paragraph className="muted-text">系统会根据答题、面试、简历和投递行为持续刷新建议。</Typography.Paragraph>
            </Space>
          </Card>
          <DataListCard
            title="弱点与提醒"
            data={weakPoints.map((item, index) => typeof item === 'string' ? { id: index, name: item } : item)}
            renderItem={(item) => <List.Item>{pickText(item, ['name'])}</List.Item>}
          />
          <DataListCard
            title="Agent 输出"
            data={agentOutputs.map((item, index) => typeof item === 'string' ? { id: index, name: item } : item)}
            emptyTitle="暂无 Agent 输出"
            renderItem={(item) => <List.Item>{pickText(item, ['title', 'name', 'summary'])}</List.Item>}
          />
        </div>
      </div>
    </ModulePage>
  )
}
