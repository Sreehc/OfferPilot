import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { AudioOutlined, FileSearchOutlined, PlayCircleOutlined, ThunderboltOutlined, VideoCameraOutlined } from '@ant-design/icons'
import { App as AntApp, Button, Card, Form, Input, Modal, Space, Tag, Typography } from 'antd'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { createCopilotPrepSessionApi, createCopilotRealtimeSessionApi, createJobPrepSessionApi, createRecordingReviewApi, fetchInterviewHistoryApi, fetchInterviewTrendApi, fetchLatestCopilotRealtimeSessionApi, startInterviewApi, startVoiceInterviewApi } from '@/api/modules/interview'
import { getErrorMessage } from '@/api/client'
import { DataTableCard, formatDateTime, normalizeRecords, pickText, StatusTag } from '@/modules/common'
import { EChartCard } from '@/components/charts/EChartCard'
import { ModulePage } from '@/modules/common'

type ApiLike = { data?: Record<string, unknown> } | undefined

export function InterviewPage() {
  const { message } = AntApp.useApp()
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const [open, setOpen] = useState(false)
  const [recordingOpen, setRecordingOpen] = useState(false)
  const history = useQuery({ queryKey: ['interview', 'history'], queryFn: () => fetchInterviewHistoryApi().then((response) => response.data) })
  const trend = useQuery({ queryKey: ['interview', 'trend'], queryFn: () => fetchInterviewTrendApi().then((response) => response.data) })
  const latestRealtime = useQuery({ queryKey: ['interview', 'copilot', 'latest'], queryFn: () => fetchLatestCopilotRealtimeSessionApi().then((response) => response.data) })
  const invalidate = () => queryClient.invalidateQueries({ queryKey: ['interview'] })
  const goToSession = (resp: ApiLike, successText: string) => {
    message.success(successText)
    invalidate()
    const payload = (resp?.data || {}) as Record<string, unknown>
    const sessionId = payload.sessionId || payload.id
    if (sessionId) navigate(`/interview/detail/${sessionId}`)
  }
  const start = useMutation({ mutationFn: startInterviewApi, onSuccess: (resp) => goToSession(resp, '已开始模拟面试'), onError: (error) => message.error(getErrorMessage(error, '开始失败')) })
  const prep = useMutation({ mutationFn: createJobPrepSessionApi, onSuccess: (resp) => goToSession(resp, '已创建 JD 备面会话'), onError: (error) => message.error(getErrorMessage(error, '创建失败')) })
  const copilotPrep = useMutation({ mutationFn: createCopilotPrepSessionApi, onSuccess: (resp) => goToSession(resp, '已创建 Copilot 备面会话'), onError: (error) => message.error(getErrorMessage(error, '创建失败')) })
  const copilotRealtime = useMutation({ mutationFn: createCopilotRealtimeSessionApi, onSuccess: (resp) => goToSession(resp, '已创建实时会话'), onError: (error) => message.error(getErrorMessage(error, '创建失败')) })
  const voice = useMutation({ mutationFn: startVoiceInterviewApi, onSuccess: (resp) => goToSession(resp, '已开始语音面试'), onError: (error) => message.error(getErrorMessage(error, '开始失败')) })
  const recordingReview = useMutation({
    mutationFn: createRecordingReviewApi,
    onSuccess: (resp) => {
      setRecordingOpen(false)
      goToSession(resp, '已生成录屏复盘')
    },
    onError: (error) => message.error(getErrorMessage(error, '生成失败'))
  })
  const rows = normalizeRecords(history.data)
  const trendRows = normalizeRecords(trend.data)
  const trendOption = {
    xAxis: { type: 'category', data: trendRows.map((row, index) => pickText(row, ['date', 'time', 'label'], String(index + 1))) },
    yAxis: { type: 'value' },
    series: [{ type: 'bar', data: trendRows.map((row) => Number(row.score || row.value || row.count || 0)) }]
  }
  const startVoice = () => voice.mutate({ direction: 'Java 后端', durationMinutes: 20, questionCount: 3 })
  const modeCards = [
    {
      key: 'standard',
      icon: <PlayCircleOutlined />,
      tag: '模拟问答',
      title: '标准面试',
      description: '按岗位启动一轮完整模拟面试，适合日常训练和阶段自测。',
      button: '开始标准面试',
      primary: true,
      loading: start.isPending,
      onClick: () => setOpen(true)
    },
    {
      key: 'job-prep',
      icon: <FileSearchOutlined />,
      tag: 'JD 分析',
      title: 'JD 备面',
      description: '围绕岗位描述拆解高频追问、能力风险和准备重点。',
      button: '启动 JD 备面',
      loading: prep.isPending,
      onClick: () => prep.mutate({})
    },
    {
      key: 'realtime',
      icon: <ThunderboltOutlined />,
      tag: '现场辅助',
      title: '实时 Copilot',
      description: '创建实时面试辅助会话，沉淀关键问题和后续复盘线索。',
      button: '打开实时 Copilot',
      loading: copilotRealtime.isPending,
      onClick: () => copilotRealtime.mutate({})
    },
    {
      key: 'voice',
      icon: <AudioOutlined />,
      tag: '语音训练',
      title: '语音面试',
      description: '用默认 Java 后端配置开始 20 分钟语音问答，快速训练表达节奏。',
      button: '开始语音面试',
      loading: voice.isPending,
      onClick: startVoice
    },
    {
      key: 'recording',
      icon: <VideoCameraOutlined />,
      tag: '复盘沉淀',
      title: '录屏复盘',
      description: '上传或粘贴面试转写，生成结构化表现复盘和改进建议。',
      button: '上传录屏复盘',
      loading: recordingReview.isPending,
      onClick: () => setRecordingOpen(true)
    }
  ]
  return (
    <ModulePage
      title="模拟面试"
      description="覆盖模拟面试、JD 备面、实时 Copilot 与录音复盘。"
      actions={<Space><Button type="primary" onClick={() => setOpen(true)}>开始模拟面试</Button><Button onClick={() => prep.mutate({})}>JD 备面</Button><Button onClick={() => copilotPrep.mutate({})}>Copilot 备面</Button><Button onClick={() => copilotRealtime.mutate({})}>实时 Copilot</Button></Space>}
      metrics={[
        { label: '面试会话', value: rows.length, hint: '累计记录' },
        { label: '趋势点', value: trendRows.length, hint: '能力变化' },
        { label: '最新会话', value: rows[0] ? formatDateTime(rows[0].createTime || rows[0].updateTime) : '-', hint: '最近活跃' },
        { label: '状态', value: latestRealtime.data ? '实时会话可用' : (history.isFetching ? '刷新中' : '已同步'), hint: '会话列表' }
      ]}
    >
      <section className="interview-mode-launcher" role="region" aria-label="面试模式启动器">
        <div className="interview-mode-header">
          <div>
            <div className="interview-mode-eyebrow">INTERVIEW STARTER</div>
            <Typography.Title level={2}>选择本次面试训练模式</Typography.Title>
            <Typography.Text type="secondary">把准备、实战辅助、语音表达和复盘入口集中在首屏，先启动训练，再查看历史和趋势。</Typography.Text>
          </div>
          <Tag color="blue">5 种模式</Tag>
        </div>
        <div className="interview-mode-grid">
          {modeCards.map((mode) => (
            <Card key={mode.key} className={`interview-mode-card${mode.primary ? ' is-primary' : ''}`} variant="borderless">
              <div className="interview-mode-icon" aria-hidden="true">{mode.icon}</div>
              <div className="interview-mode-meta">
                <Tag>{mode.tag}</Tag>
                <Typography.Title level={3}>{mode.title}</Typography.Title>
                <Typography.Paragraph type="secondary">{mode.description}</Typography.Paragraph>
              </div>
              <Button type={mode.primary ? 'primary' : 'default'} block loading={mode.loading} onClick={mode.onClick}>
                {mode.button}
              </Button>
            </Card>
          ))}
        </div>
      </section>

      <div className="workspace-grid two">
        <div className="workspace-grid">
          <EChartCard title="面试趋势" option={trendOption} />
          <DataTableCard
            title="历史记录"
            data={history.data}
            loading={history.isLoading}
            error={history.error}
            onRetry={() => history.refetch()}
            columns={[
              { title: '会话', render: (_, row) => <Link to={`/interview/detail/${row.id || row.sessionId}`}>{pickText(row, ['title', 'sessionTitle'], '面试会话')}</Link> },
              { title: '模式', render: (_, row) => pickText(row, ['mode'], 'standard') },
              { title: '状态', render: (_, row) => <StatusTag value={pickText(row, ['status'])} /> },
              { title: '时间', render: (_, row) => formatDateTime(row.time || row.createTime) }
            ]}
          />
        </div>
        <Card title="实时 Copilot" className="surface-card">
          <Space orientation="vertical">
            <div>{latestRealtime.data ? '当前存在实时 Copilot 会话，可继续跟进。' : '当前没有实时 Copilot 会话。'}</div>
            <Button onClick={() => copilotRealtime.mutate({})} loading={copilotRealtime.isPending}>进入 Copilot</Button>
          </Space>
        </Card>
      </div>
      <Modal open={open} title="开始模拟面试" footer={null} onCancel={() => setOpen(false)}>
        <Form layout="vertical" onFinish={(values) => start.mutate(values)}>
          <Form.Item label="岗位" name="jobTitle" rules={[{ required: true }]}><Input /></Form.Item>
          <Form.Item label="公司" name="company"><Input /></Form.Item>
          <Form.Item label="说明" name="description"><Input.TextArea rows={5} /></Form.Item>
          <Button type="primary" htmlType="submit" loading={start.isPending}>开始</Button>
        </Form>
      </Modal>
      <Modal open={recordingOpen} title="上传录屏复盘" footer={null} onCancel={() => setRecordingOpen(false)} destroyOnHidden>
        <Form
          layout="vertical"
          initialValues={{ direction: 'Java 后端' }}
          onFinish={(values) => recordingReview.mutate({
            direction: values.direction || 'Java 后端',
            jobRole: values.jobRole,
            notes: values.notes,
            transcriptText: values.transcriptText
          })}
        >
          <Form.Item label="方向" name="direction"><Input placeholder="Java 后端" /></Form.Item>
          <Form.Item label="岗位" name="jobRole"><Input placeholder="例如：高级 Java 工程师" /></Form.Item>
          <Form.Item label="文字转写" name="transcriptText" rules={[{ required: true, message: '请填写录屏或语音转写内容' }]}>
            <Input.TextArea rows={6} placeholder="粘贴面试问答、语音转写或关键片段" />
          </Form.Item>
          <Form.Item label="补充说明" name="notes"><Input.TextArea rows={3} placeholder="可补充面试岗位、公司背景或希望重点复盘的问题" /></Form.Item>
          <Button type="primary" htmlType="submit" loading={recordingReview.isPending}>生成复盘</Button>
        </Form>
      </Modal>
    </ModulePage>
  )
}
