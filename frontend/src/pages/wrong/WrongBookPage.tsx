import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { App as AntApp, Button, Card, Empty, Popconfirm, Select, Space, Tag, Typography } from 'antd'
import { Link } from 'react-router-dom'
import { deleteWrongApi, exportWrongMarkdownApi, fetchWrongListApi, updateMasteryApi } from '@/api/modules/wrong'
import { getErrorMessage } from '@/api/client'
import { DataTableCard, downloadBlob, formatDateTime, getTotal, masteryLabel, normalizeRecords, pickText } from '@/modules/common'
import { ModulePage } from '@/modules/common'

function wrongItemId(row: Record<string, any>) {
  return Number(row.id || row.wrongQuestionId)
}

function wrongQuestionHref(row: Record<string, any>) {
  return `/question/${row.questionId || row.id}`
}

function nextWrongIndex(current: number, total: number) {
  if (!total) return 0
  return (current + 1) % total
}

export function WrongBookPage() {
  const { message } = AntApp.useApp()
  const queryClient = useQueryClient()
  const [activeIndex, setActiveIndex] = useState(0)
  const [showExplanation, setShowExplanation] = useState(false)
  const list = useQuery({ queryKey: ['wrong', 'list'], queryFn: () => fetchWrongListApi(1, 20).then((response) => response.data) })
  const invalidate = () => queryClient.invalidateQueries({ queryKey: ['wrong'] })
  const mastery = useMutation({ mutationFn: ({ id, masteryLevel }: { id: number; masteryLevel: string }) => updateMasteryApi(id, { masteryLevel }), onSuccess: () => { message.success('掌握度已更新'); invalidate() }, onError: (error) => message.error(getErrorMessage(error, '更新失败')) })
  const remove = useMutation({ mutationFn: (id: number) => deleteWrongApi(id), onSuccess: () => { message.success('错题已删除'); invalidate() }, onError: (error) => message.error(getErrorMessage(error, '删除失败')) })
  const exportWrong = useMutation({ mutationFn: exportWrongMarkdownApi, onSuccess: (response) => { downloadBlob(response.data, 'wrong-questions.md'); message.success('错题已导出') }, onError: (error) => message.error(getErrorMessage(error, '导出失败')) })
  const rows = normalizeRecords(list.data)
  const currentIndex = rows.length ? Math.min(activeIndex, rows.length - 1) : 0
  const active = rows[currentIndex]
  const title = pickText(active, ['title', 'questionTitle', 'name'], '错题')
  const errorReason = pickText(active, ['errorReason', 'wrongReason', 'mistakeReason', 'reason'], '暂无错误原因记录。')
  const answer = pickText(active, ['standardAnswer', 'answer', 'referenceAnswer'], '')
  const explanation = pickText(active, ['aiExplanation', 'explanation', 'reviewSummary', 'analysis'], '')
  const moveNext = () => {
    setActiveIndex((index) => nextWrongIndex(index, rows.length))
    setShowExplanation(false)
  }

  return (
    <ModulePage
      title="错题本"
      description="先用卡片流重做错题，再在管理列表里处理删除、导出和批量整理。"
      metrics={[
        { label: '错题总数', value: getTotal(list.data, rows.length), hint: '全部错题' },
        { label: '复习中', value: rows.filter((row) => pickText(row, ['masteryLevel', 'status']).toLowerCase().includes('review')).length, hint: '当前页' },
        { label: '已掌握', value: rows.filter((row) => pickText(row, ['masteryLevel', 'status']).toLowerCase().includes('master')).length, hint: '当前页' },
        { label: '导出', value: exportWrong.isPending ? '进行中' : '可用', hint: 'Markdown' }
      ]}
      actions={<Button type="primary" loading={exportWrong.isPending} onClick={() => exportWrong.mutate()}>导出 Markdown</Button>}
    >
      <section
        role={list.isLoading ? undefined : 'region'}
        aria-label={list.isLoading ? undefined : '错题卡片复习流'}
        className="review-flow wrong-review-flow"
      >
        <Card className="surface-card review-flow-card wrong-review-card" loading={list.isLoading}>
          {!list.isLoading && rows.length && active ? (
            <div className="review-card">
              <div className="review-card-head">
                <Space wrap>
                  <Tag color="blue">第 {currentIndex + 1} / {rows.length} 题</Tag>
                  <Tag>{masteryLabel(pickText(active, ['masteryLevel', 'status']))}</Tag>
                  {active.nextReviewDate ? <Tag color="processing">下次复习 {String(active.nextReviewDate)}</Tag> : null}
                </Space>
                <Typography.Title level={3}>{title}</Typography.Title>
                <Typography.Paragraph className="muted-text">
                  先重新作答，再查看 AI 解释。错题管理动作保留在下方列表，练习时不打断主流程。
                </Typography.Paragraph>
              </div>

              <div className="wrong-reason-panel">
                <Typography.Text strong>上次失分原因</Typography.Text>
                <Typography.Paragraph>{errorReason}</Typography.Paragraph>
              </div>

              {showExplanation ? (
                <div className="review-card-answer">
                  <div>
                    <Typography.Text strong>参考答案</Typography.Text>
                    <Typography.Paragraph>{answer || '暂无参考答案。'}</Typography.Paragraph>
                  </div>
                  <div>
                    <Typography.Text strong>AI 解释</Typography.Text>
                    <Typography.Paragraph>{explanation || errorReason}</Typography.Paragraph>
                  </div>
                </div>
              ) : (
                <div className="review-card-hidden">
                  <Typography.Text strong>解析暂时隐藏</Typography.Text>
                  <Typography.Text className="muted-text">先重新组织答案，再展开 AI 解释和参考答案。</Typography.Text>
                  <Button type="primary" onClick={() => setShowExplanation(true)}>查看 AI 解释</Button>
                </div>
              )}

              <div className="review-score-actions">
                <Typography.Text strong>复习动作</Typography.Text>
                <Space wrap>
                  <Link to={wrongQuestionHref(active)}>
                    <Button type="primary">重新作答</Button>
                  </Link>
                  <Button
                    loading={mastery.isPending}
                    onClick={() => mastery.mutate({ id: wrongItemId(active), masteryLevel: 'mastered' })}
                  >
                    标记已掌握
                  </Button>
                  <Button onClick={moveNext}>下一题</Button>
                </Space>
              </div>

              <div className="review-card-foot">
                <Typography.Text className="muted-text">
                  已复习 {String(active.reviewCount ?? 0)} 次，连续正确 {String(active.streak ?? 0)} 次。
                </Typography.Text>
              </div>
            </div>
          ) : !list.isLoading ? (
            <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无错题">
              <Button type="primary" href="/question">去题库练习</Button>
            </Empty>
          ) : null}
        </Card>
      </section>

      <section role="region" aria-label="错题管理列表" className="wrong-management-list">
        <DataTableCard
          title="错题管理"
          data={list.data}
          loading={list.isLoading}
          error={list.error}
          onRetry={() => list.refetch()}
          emptyTitle="暂无错题，去题库练习后这里会自动收集"
          columns={[
            { title: '题目', render: (_, row) => <Link to={wrongQuestionHref(row)}>{pickText(row, ['title', 'questionTitle', 'name'], '错题')}</Link> },
            { title: '分类', render: (_, row) => pickText(row, ['categoryName', 'category']) },
            { title: '掌握度', render: (_, row) => <Tag>{masteryLabel(pickText(row, ['masteryLevel', 'status']))}</Tag> },
            { title: '更新时间', render: (_, row) => formatDateTime(row.updateTime || row.createTime) },
            { title: '操作', render: (_, row) => <Space wrap><Link to={wrongQuestionHref(row)}><Button size="small" type="primary">重新作答</Button></Link><Select size="small" style={{ width: 110 }} defaultValue={pickText(row, ['masteryLevel'], 'reviewing')} options={[{ value: 'not_started', label: '未开始' }, { value: 'reviewing', label: '复习中' }, { value: 'mastered', label: '已掌握' }]} onChange={(masteryLevel) => mastery.mutate({ id: wrongItemId(row), masteryLevel })} /><Popconfirm title="删除这道错题？" description="此操作不可恢复。" okText="删除" okButtonProps={{ danger: true }} cancelText="取消" onConfirm={() => remove.mutate(wrongItemId(row))}><Button danger size="small" loading={remove.isPending}>删除</Button></Popconfirm></Space> }
          ]}
        />
      </section>
    </ModulePage>
  )
}
