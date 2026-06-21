import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { App as AntApp, Button, Card, Empty, Rate, Space, Tag, Typography } from 'antd'
import { fetchReviewStatsApi, fetchReviewTodayApi, submitReviewRateApi } from '@/api/modules/review'
import { getErrorMessage } from '@/api/client'
import { contentTypeLabel, DataTableCard, normalizeRecords, pickText, StatusTag } from '@/modules/common'
import { ModulePage } from '@/modules/common'

function reviewItemId(row: Record<string, any>) {
  return String(row.reviewItemId || row.id || row.wrongQuestionId)
}

function nextIndex(current: number, total: number) {
  if (!total) return 0
  return (current + 1) % total
}

export function ReviewPage() {
  const { message, modal } = AntApp.useApp()
  const queryClient = useQueryClient()
  const [activeIndex, setActiveIndex] = useState(0)
  const [revealed, setRevealed] = useState(false)
  const today = useQuery({ queryKey: ['review', 'today'], queryFn: () => fetchReviewTodayApi().then((response) => response.data) })
  const stats = useQuery({ queryKey: ['review', 'stats'], queryFn: () => fetchReviewStatsApi().then((response) => response.data) })
  const rate = useMutation({ mutationFn: ({ id, score }: { id: string; score: number }) => submitReviewRateApi(id, { score }), onSuccess: () => { message.success('复习评分已提交'); queryClient.invalidateQueries({ queryKey: ['review'] }) }, onError: (error) => message.error(getErrorMessage(error, '评分失败')) })
  const confirmRate = (id: string, score: number) => modal.confirm({
    title: null,
    content: (
      <div className="review-rate-confirm">
        <Typography.Text strong>确认提交本次评分？</Typography.Text>
        <Typography.Paragraph className="muted-text">你的掌握评分：{score} 星，提交后将更新复习计划。</Typography.Paragraph>
      </div>
    ),
    okText: '提交',
    okButtonProps: { 'aria-label': '提交' },
    cancelText: '取消',
    onOk: () => rate.mutate({ id, score })
  })
  const rows = normalizeRecords(today.data)
  const currentIndex = rows.length ? Math.min(activeIndex, rows.length - 1) : 0
  const active = rows[currentIndex]
  const answer = pickText(active, ['answer', 'standardAnswer', 'referenceAnswer'], '')
  const explanation = pickText(active, ['explanation', 'aiExplanation', 'errorReason', 'summary'], '')
  const moveNext = () => {
    setActiveIndex((index) => nextIndex(index, rows.length))
    setRevealed(false)
  }
  return (
    <ModulePage
      title="复习巩固"
      description="用卡片流完成今日复习，先回忆，再查看答案、AI 解释并评分。"
      metrics={[
        { label: '今日复习', value: rows.length, hint: '待处理条目' },
        { label: '完成率', value: (stats.data as any)?.completionRate ?? '-', hint: '近期统计' },
        { label: '平均评分', value: (stats.data as any)?.avgScore ?? '-', hint: '自评结果' },
        { label: '状态', value: today.isFetching ? '刷新中' : '已同步', hint: '数据状态' }
      ]}
    >
      <section
        role={today.isLoading ? undefined : 'region'}
        aria-label={today.isLoading ? undefined : '卡片式复习流'}
        className="review-flow"
      >
        <Card className="surface-card review-flow-card" loading={today.isLoading}>
          {!today.isLoading && rows.length && active ? (
            <div className="review-card">
              <div className="review-card-head">
                <Space wrap>
                  <Tag color="blue">第 {currentIndex + 1} / {rows.length} 题</Tag>
                  <Tag>{contentTypeLabel(pickText(active, ['contentType', 'type']))}</Tag>
                  <StatusTag value={pickText(active, ['masteryLevel', 'status'])} />
                  {Number(active.overdueDays || 0) > 0 ? <Tag color="warning">逾期 {String(active.overdueDays)} 天</Tag> : null}
                </Space>
                <Typography.Title level={3}>{pickText(active, ['title', 'contentTitle', 'name'], '复习项')}</Typography.Title>
                <Typography.Paragraph className="muted-text">
                  先在脑中复述答案，再展开解释。评分会影响下一次复习间隔。
                </Typography.Paragraph>
              </div>

              {!revealed ? (
                <div className="review-card-hidden">
                  <Typography.Text strong>答案暂时隐藏</Typography.Text>
                  <Typography.Text className="muted-text">先完成主动回忆，再查看答案和 AI 解释。</Typography.Text>
                  <Button type="primary" onClick={() => setRevealed(true)}>查看答案和 AI 解释</Button>
                </div>
              ) : (
                <div className="review-card-answer">
                  <div>
                    <Typography.Text strong>参考答案</Typography.Text>
                    <Typography.Paragraph>{answer || '暂无参考答案。'}</Typography.Paragraph>
                  </div>
                  <div>
                    <Typography.Text strong>AI 解释</Typography.Text>
                    <Typography.Paragraph>{explanation || '暂无 AI 解释。'}</Typography.Paragraph>
                  </div>
                </div>
              )}

              <div className="review-score-actions">
                <Typography.Text strong>本轮掌握度</Typography.Text>
                <Space wrap>
                  <Button onClick={() => confirmRate(reviewItemId(active), 1)}>很吃力</Button>
                  <Button onClick={() => confirmRate(reviewItemId(active), 2)}>有印象</Button>
                  <Button type="primary" onClick={() => confirmRate(reviewItemId(active), 3)}>掌握一般</Button>
                  <Button onClick={() => confirmRate(reviewItemId(active), 4)}>比较熟</Button>
                  <Button onClick={() => confirmRate(reviewItemId(active), 5)}>完全掌握</Button>
                </Space>
              </div>

              <div className="review-card-foot">
                <Typography.Text className="muted-text">
                  连续正确 {String(active.streak ?? 0)} 次，间隔 {String(active.intervalDays ?? '-')} 天。
                </Typography.Text>
                <Button onClick={moveNext}>下一题</Button>
              </div>
            </div>
          ) : !today.isLoading ? (
            <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="今日暂无待复习内容">
              <Button type="primary" href="/question">去题库练习</Button>
            </Empty>
          ) : null}
        </Card>
      </section>

      <DataTableCard
        title="复习明细"
        data={today.data}
        loading={today.isLoading}
        error={today.error}
        onRetry={() => today.refetch()}
        emptyTitle="今日暂无待复习内容，去题库练习生成更多复习项"
        columns={[
          { title: '内容', render: (_, row) => pickText(row, ['title', 'contentTitle', 'name'], '复习项') },
          { title: '类型', render: (_, row) => <Tag>{contentTypeLabel(pickText(row, ['contentType', 'type']))}</Tag> },
          { title: '状态', render: (_, row) => <StatusTag value={pickText(row, ['status'])} /> },
          { title: '评分', render: (_, row) => <Rate allowHalf defaultValue={Number(row.score || 0)} onChange={(score) => confirmRate(reviewItemId(row), score)} /> },
          { title: '说明', render: (_, row) => pickText(row, ['summary', 'description'], '-') }
        ]}
      />
    </ModulePage>
  )
}
