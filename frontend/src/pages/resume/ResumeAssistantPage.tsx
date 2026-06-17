import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { App as AntApp, Button, Card, Form, Input, Modal, Rate, Space, Upload } from 'antd'
import { uploadResumeApi, fetchResumeDetailApi, fetchResumeListApi, fetchResumeProjectQuestionsApi, fetchResumeScoreApi, fetchResumeVersionsApi, retryResumeParseApi } from '@/api/modules/resume'
import { getErrorMessage } from '@/api/client'
import { GeneratedArtifactCard } from '@/components/agent/AgentComponents'
import { DataTableCard, EntitySummary, normalizeRecords, pickArray, pickText, StatusTag } from '@/modules/common'
import { ModulePage } from '@/modules/common'

export function ResumeAssistantPage() {
  const { message } = AntApp.useApp()
  const queryClient = useQueryClient()
  const [detailId, setDetailId] = useState<string | null>(null)
  const [open, setOpen] = useState(false)
  const list = useQuery({ queryKey: ['resume'], queryFn: () => fetchResumeListApi().then((response) => response.data) })
  const detail = useQuery({ queryKey: ['resume', detailId], queryFn: () => detailId ? fetchResumeDetailApi(detailId).then((response) => response.data) : Promise.resolve(null), enabled: Boolean(detailId) })
  const questions = useQuery({ queryKey: ['resume', detailId, 'questions'], queryFn: () => detailId ? fetchResumeProjectQuestionsApi(detailId).then((response) => response.data) : Promise.resolve(null), enabled: Boolean(detailId) })
  const score = useQuery({ queryKey: ['resume', detailId, 'score'], queryFn: () => detailId ? fetchResumeScoreApi(detailId).then((response) => response.data) : Promise.resolve(null), enabled: Boolean(detailId) })
  const versions = useQuery({ queryKey: ['resume', detailId, 'versions'], queryFn: () => detailId ? fetchResumeVersionsApi(detailId).then((response) => response.data) : Promise.resolve(null), enabled: Boolean(detailId) })
  const invalidate = () => queryClient.invalidateQueries({ queryKey: ['resume'] })
  const upload = useMutation({ mutationFn: (file: File) => uploadResumeApi(file), onSuccess: () => { message.success('简历已上传'); invalidate() }, onError: (error) => message.error(getErrorMessage(error, '上传失败')) })
  const retry = useMutation({ mutationFn: (id: string) => retryResumeParseApi(id), onSuccess: () => { message.success('已重新解析'); invalidate() }, onError: (error) => message.error(getErrorMessage(error, '重试失败')) })
  const rows = normalizeRecords(list.data)
  const active = detail.data as any
  const suggestions = [
    ...pickArray<Record<string, unknown>>(active, ['suggestions', 'improvements', 'nextActions']),
    ...pickArray<Record<string, unknown>>(score.data as any, ['suggestions', 'items'])
  ]
  return (
    <ModulePage
      title="简历助手"
      description="上传、解析、评分、版本和追问统一放在这里。"
      metrics={[
        { label: '简历数', value: rows.length, hint: '当前列表' },
        { label: '评分', value: (score.data as any)?.score ?? '-', hint: '系统评分' },
        { label: '版本', value: normalizeRecords(versions.data).length, hint: '可恢复历史' },
        { label: '状态', value: list.isFetching ? '刷新中' : '已同步', hint: '简历数据' }
      ]}
      actions={<Space><Upload showUploadList={false} beforeUpload={(file) => { upload.mutate(file); return Upload.LIST_IGNORE }}><Button type="primary">上传简历</Button></Upload><Button onClick={() => setOpen(true)}>生成优化建议</Button></Space>}
    >
      <div className="workspace-grid two">
        <DataTableCard
          title="简历列表"
          data={list.data}
          loading={list.isLoading}
          error={list.error}
          onRetry={() => list.refetch()}
          columns={[
            { title: '简历', render: (_, row) => <Button type="link" onClick={() => setDetailId(String(row.id || row.resumeId))}>{pickText(row, ['fileName', 'resumeTitle'], '简历')}</Button> },
            { title: '评分', render: (_, row) => pickText(row, ['score'], '-') },
            { title: '状态', render: (_, row) => <StatusTag value={pickText(row, ['status'], 'READY')} /> },
            { title: '操作', render: (_, row) => <Space><Button size="small" onClick={() => retry.mutate(String(row.id || row.resumeId))}>重新解析</Button><Button size="small" onClick={() => setDetailId(String(row.id || row.resumeId))}>查看</Button></Space> }
          ]}
        />
        <Space direction="vertical" style={{ width: '100%' }}>
          <Card title="简历详情" className="surface-card">
            {active ? <EntitySummary record={active} fields={[{ label: '文件名', keys: ['fileName', 'resumeTitle'] }, { label: '评分', keys: ['score'] }, { label: '更新时间', keys: ['updateTime'] }, { label: '状态', keys: ['status'], tag: true }]} /> : '请选择一份简历'}
            <Button style={{ marginTop: 16 }} onClick={() => setOpen(true)}>查看优化建议</Button>
          </Card>
          <GeneratedArtifactCard title="建议输出" items={suggestions.map((item) => pickText(item, ['title', 'name', 'summary', 'content']))} />
          <Card title="追问问题" className="surface-card">
            <div className="workspace-grid">
              {normalizeRecords(questions.data).map((item) => <Card key={item.id} size="small">{pickText(item, ['title', 'question'])}</Card>)}
            </div>
          </Card>
        </Space>
      </div>
      <Modal open={open} title="优化建议" footer={null} onCancel={() => setOpen(false)}>
        <Form layout="vertical">
          <Form.Item label="可优化项"><Rate defaultValue={4} /></Form.Item>
          <Form.Item label="说明"><Input.TextArea rows={5} /></Form.Item>
          <Button type="primary">生成建议</Button>
        </Form>
      </Modal>
    </ModulePage>
  )
}
