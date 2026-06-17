import ReactECharts from 'echarts-for-react'
import { Card } from 'antd'

export function EChartCard({ title, option, height = 280 }: { title: string; option: Record<string, any>; height?: number }) {
  return <Card title={title} className="surface-card"><ReactECharts option={option} style={{ height }} notMerge lazyUpdate /></Card>
}
