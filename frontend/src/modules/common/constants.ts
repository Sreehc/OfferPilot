import type { MetricItem } from '@/components/layout/WorkspacePage'

export const fallbackMetrics: MetricItem[] = [
  { label: '今日任务', value: '-', hint: '暂无数据' },
  { label: '能力覆盖', value: '-', hint: '暂无数据' },
  { label: '待处理', value: '-', hint: '暂无数据' },
  { label: 'Agent 建议', value: '-', hint: '暂无数据' }
]
