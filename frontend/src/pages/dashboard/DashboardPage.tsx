import { useQuery } from '@tanstack/react-query'
import { Button, Card, List, Progress, Space, Tag, Typography } from 'antd'
import { Link } from 'react-router-dom'
import { fetchDashboardOverviewApi } from '@/api/modules/dashboard'
import { AdaptiveRecommendationsPanel } from '@/components/adaptive/AdaptiveRecommendationsPanel'
import { EChartCard } from '@/components/charts/EChartCard'
import { buildCategorySeries, DataListCard, normalizeRecords, pickArray, pickText } from '@/modules/common'
import { ModulePage } from '@/modules/common'

function readNumber(value: unknown, fallback = 0) {
  const next = Number(value)
  return Number.isFinite(next) ? next : fallback
}

function clampPercent(value: number) {
  return Math.max(0, Math.min(100, Math.round(value)))
}

function actionPath(value: unknown, fallback: string) {
  const text = String(value || '').trim()
  return text || fallback
}

function normalizeWeakPoint(item: string | Record<string, unknown>, index: number) {
  if (typeof item === 'string') return { id: index, title: item, score: undefined, wrongCount: undefined }
  return {
    id: item.id || item.categoryId || index,
    title: pickText(item, ['categoryName', 'name', 'title', 'label'], '薄弱项'),
    score: item.score,
    wrongCount: item.wrongCount
  }
}

function normalizeActionItem(item: string | Record<string, unknown>, index: number) {
  if (typeof item === 'string') return { id: index, title: item, description: '', path: '/agent', status: '' }
  return {
    id: item.key || item.id || index,
    title: pickText(item, ['label', 'title', 'name', 'summary'], '继续推进'),
    description: pickText(item, ['description', 'summary', 'reason', 'content'], ''),
    path: actionPath(item.path || item.actionPath || item.url, '/agent'),
    status: pickText(item, ['status', 'priority', 'tone'], '')
  }
}

export function DashboardPage() {
  const query = useQuery({ queryKey: ['dashboard', 'overview'], queryFn: () => fetchDashboardOverviewApi().then((response) => response.data) })
  const overview = query.data as any
  const activities = normalizeRecords(overview?.recentActivities || overview?.activities || overview?.records)
  const trendRecords = normalizeRecords(overview?.trend || overview?.trainingTrend || overview?.reviewTrend)
  const trend = buildCategorySeries(trendRecords, ['label', 'date', 'day', 'name'], ['value', 'count', 'score', 'completed'])
  const weakPoints = pickArray<string | Record<string, unknown>>(overview, ['weakPoints', 'weaknesses', 'riskItems'])
  const agentOutputs = pickArray<string | Record<string, unknown>>(overview, ['agentOutputs', 'agentSuggestions', 'nextActions'])
  const workflowContinuations = pickArray<string | Record<string, unknown>>(overview, ['workflowContinuations', 'continuations', 'pendingWorkflows'])
  const nextAction = (overview?.mainTask && typeof overview.mainTask === 'object' ? overview.mainTask : overview?.nextAction) || {}
  const todayTotal = readNumber(overview?.todayTaskCount ?? overview?.todayTasks ?? overview?.totalTaskCount)
  const todayCompleted = readNumber(overview?.completedTaskCount ?? overview?.todayCompletedTaskCount ?? overview?.completedTasks)
  const explicitProgress = overview?.progressPercent ?? overview?.completionRate ?? (overview?.progress && typeof overview.progress === 'object' ? overview.progress.percent : overview?.progress)
  const progressPercent = clampPercent(explicitProgress !== undefined ? readNumber(explicitProgress) : todayTotal ? (todayCompleted / todayTotal) * 100 : 0)
  const hasDashboardData = Boolean(
    overview?.nextAction ||
    activities.length ||
    weakPoints.length ||
    agentOutputs.length ||
    workflowContinuations.length ||
    readNumber(overview?.learningCount) ||
    readNumber(overview?.wrongCount) ||
    readNumber(overview?.reviewDebtCount) ||
    todayTotal
  )
  const mainTaskTitle = pickText(nextAction, ['title', 'name'], hasDashboardData ? '完成一轮训练闭环' : '今天先建立第一条训练记录')
  const mainTaskDescription = pickText(nextAction, ['description', 'summary'], hasDashboardData ? '先选择一个最能推动结果的训练动作，把今天的学习推进到可复盘状态。' : '从面试、刷题或简历里任选一个动作开始，系统会据此生成薄弱项和下一步建议。')
  const mainTaskPath = actionPath(nextAction.path || nextAction.actionPath, hasDashboardData ? '/study-plan' : '/interview')
  const aiSuggestion = pickText(overview, ['suggestedFocus', 'aiSuggestion', 'nextSuggestion'], pickText(nextAction, ['reason'], '系统会根据答题、面试、简历和投递行为持续刷新建议。'))
  const weakPointRows = weakPoints.map(normalizeWeakPoint)
  const continuationRows = workflowContinuations.map(normalizeActionItem)
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
    { label: '智能建议', value: overview?.agentHints ?? overview?.agentSuggestionCount ?? agentOutputs.length, hint: '可直接执行' }
  ]
  return (
    <ModulePage
      title="今日工作台"
      description="把训练、复盘、投递、面试和智能建议收拢到统一工作台。"
      metrics={metrics}
      actions={<Space wrap><Link to="/chat"><Button>进入 AI 问答</Button></Link><Link to="/agent"><Button>创建 Agent 任务</Button></Link></Space>}
    >
      <section className="dashboard-cockpit" role="region" aria-label="今日 AI 训练驾驶舱">
        <div className="dashboard-cockpit-main">
          <div className="dashboard-cockpit-eyebrow">今日 AI 训练驾驶舱</div>
          <div className="dashboard-main-task">
            <Space size={8} wrap>
              <Tag color={pickText(nextAction, ['priority'], '') === 'P0' ? 'red' : 'blue'}>今日主任务</Tag>
              {pickText(nextAction, ['priority'], '') ? <Tag>{pickText(nextAction, ['priority'])}</Tag> : null}
            </Space>
            <Typography.Title level={3}>{mainTaskTitle}</Typography.Title>
            <Typography.Paragraph className="muted-text">{mainTaskDescription}</Typography.Paragraph>
            <Space wrap>
              <Link to={mainTaskPath}><Button type="primary">执行主任务</Button></Link>
              <Link to="/interview"><Button>开始面试</Button></Link>
              <Link to="/question"><Button>刷题训练</Button></Link>
              <Link to="/resume"><Button>上传简历</Button></Link>
            </Space>
          </div>
          <div className="dashboard-cockpit-footer">
            <div>
              <Typography.Text strong>AI 下一步建议</Typography.Text>
              <Typography.Paragraph className="muted-text">{aiSuggestion}</Typography.Paragraph>
            </div>
          </div>
        </div>
        <div className="dashboard-cockpit-side">
          <Card title="训练进度" className="surface-card dashboard-progress-card">
            <Progress percent={progressPercent} />
            <Typography.Text className="muted-text">{todayTotal ? `完成 ${todayCompleted} / ${todayTotal}` : '继续积累训练数据'}</Typography.Text>
          </Card>
          <Card title="薄弱项提醒" className="surface-card dashboard-weak-card">
            {weakPointRows.length ? (
              <Space orientation="vertical" style={{ width: '100%' }} size={8}>
                {weakPointRows.slice(0, 3).map((item) => (
                  <Link key={String(item.id)} to="/question" className="dashboard-weak-item">
                    <span>{item.title}</span>
                    <Space size={4}>
                      {item.score !== undefined ? <Tag>{String(item.score)} 分</Tag> : null}
                      {item.wrongCount !== undefined ? <Tag color="warning">错 {String(item.wrongCount)}</Tag> : null}
                    </Space>
                  </Link>
                ))}
              </Space>
            ) : (
              <Typography.Text className="muted-text">暂无弱点提醒，完成一次练习后会生成画像。</Typography.Text>
            )}
          </Card>
        </div>
      </section>

      <AdaptiveRecommendationsPanel />

      <div className="workspace-grid two">
        <div className="workspace-grid">
          <EChartCard title="训练趋势" option={chartOption} />
          <DataListCard
            title="最近行动"
            data={activities}
            loading={query.isLoading}
            error={query.error}
            onRetry={() => query.refetch()}
            emptyTitle="还没有训练记录，先开始一次模拟面试或练习"
            renderItem={(item) => <List.Item.Meta title={pickText(item, ['name', 'title'], '行动')} description={pickText(item, ['time', 'description'], '')} />}
          />
        </div>
        <div className="workspace-grid">
          <Card title="下一步行动" className="surface-card">
            <Space orientation="vertical" style={{ width: '100%' }}>
              <Progress percent={Number(overview?.progress ?? overview?.completionRate ?? 0)} />
              <Typography.Paragraph className="muted-text">系统会根据答题、面试、简历和投递行为持续刷新建议。</Typography.Paragraph>
            </Space>
          </Card>
          <DataListCard
            title="弱点与提醒"
            data={weakPointRows}
            loading={query.isLoading}
            error={query.error}
            onRetry={() => query.refetch()}
            emptyTitle="暂无弱点提醒，继续练习以生成画像"
            renderItem={(item) => <Link to="/question">{pickText(item, ['title', 'name'])}</Link>}
          />
          <DataListCard
            title="智能建议与待继续工作流"
            data={[...agentOutputs.map(normalizeActionItem), ...continuationRows]}
            loading={query.isLoading}
            error={query.error}
            onRetry={() => query.refetch()}
            emptyTitle="暂无智能建议"
            renderItem={(item) => (
              <List.Item.Meta
                title={<Link to={actionPath(item.path, '/agent')}>{pickText(item, ['title', 'name', 'summary'])}</Link>}
                description={pickText(item, ['description', 'status'])}
              />
            )}
          />
        </div>
      </div>
    </ModulePage>
  )
}
