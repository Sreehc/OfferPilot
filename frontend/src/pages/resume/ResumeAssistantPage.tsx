import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { App as AntApp, Button, Card, List, Popconfirm, Space, Upload } from 'antd'
import { fetchResumeDetailApi, fetchResumeListApi, fetchResumeProjectQuestionsApi, fetchResumeScoreApi, fetchResumeVersionsApi, restoreResumeVersionApi, retryResumeParseApi, uploadResumeApi } from '@/api/modules/resume'
import { getErrorMessage } from '@/api/client'
import { GeneratedArtifactCard } from '@/components/agent/AgentComponents'
import { DataTableCard, EntitySummary, formatDateTime, normalizeRecords, pickArray, pickText, StatusTag } from '@/modules/common'
import { ModulePage } from '@/modules/common'

export function ResumeAssistantPage() {
  const { message } = AntApp.useApp()
  const queryClient = useQueryClient()
  const [detailId, setDetailId] = useState<string | null>(null)
  const list = useQuery({ queryKey: ['resume'], queryFn: () => fetchResumeListApi().then((response) => response.data) })
  const detail = useQuery({ queryKey: ['resume', detailId], queryFn: () => detailId ? fetchResumeDetailApi(detailId).then((response) => response.data) : Promise.resolve(null), enabled: Boolean(detailId) })
  const questions = useQuery({ queryKey: ['resume', detailId, 'questions'], queryFn: () => detailId ? fetchResumeProjectQuestionsApi(detailId).then((response) => response.data) : Promise.resolve(null), enabled: Boolean(detailId) })
  const score = useQuery({ queryKey: ['resume', detailId, 'score'], queryFn: () => detailId ? fetchResumeScoreApi(detailId).then((response) => response.data) : Promise.resolve(null), enabled: Boolean(detailId) })
  const versions = useQuery({ queryKey: ['resume', detailId, 'versions'], queryFn: () => detailId ? fetchResumeVersionsApi(detailId).then((response) => response.data) : Promise.resolve(null), enabled: Boolean(detailId) })
  const invalidate = () => queryClient.invalidateQueries({ queryKey: ['resume'] })
  const upload = useMutation({ mutationFn: (file: File) => uploadResumeApi(file), onSuccess: () => { message.success('简历已上传'); invalidate() }, onError: (error) => message.error(getErrorMessage(error, '上传失败')) })
  const retry = useMutation({ mutationFn: (id: string) => retryResumeParseApi(id), onSuccess: () => { message.success('已重新解析'); invalidate() }, onError: (error) => message.error(getErrorMessage(error, '重试失败')) })
  const restore = useMutation({ mutationFn: (versionId: string) => restoreResumeVersionApi(versionId), onSuccess: () => { message.success('已恢复到该版本'); invalidate() }, onError: (error) => message.error(getErrorMessage(error, '恢复失败')) })
  const rows = normalizeRecords(list.data)
  const active = detail.data as any
  const versionRows = normalizeRecords(versions.data)
  const suggestions = [
    ...pickArray<Record<string, unknown>>(active, ['suggestions', 'improvements', 'nextActions']),
    ...pickArray<Record<string, unknown>>(score.data as any, ['suggestions', 'items'])
  ]
  return (
    <ModulePage
      title="简历助手"
      description="上传、解析、评分、优化建议和版本恢复统一放在这里。"
      metrics={[
        { label: '简历数', value: rows.length, hint: '当前列表' },
        { label: '评分', value: (score.data as any)?.score ?? '-', hint: '系统评分' },
        { label: '版本', value: versionRows.length, hint: '可恢复历史' },
        { label: '状态', value: list.isFetching ? '刷新中' : '已同步', hint: '简历数据' }
      ]}
      actions={<Upload showUploadList={false} beforeUpload={(file) => { upload.mutate(file); return Upload.LIST_IGNORE }}><Button type="primary" loading={upload.isPending}>上传简历</Button></Upload>}
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
            { title: '操作', render: (_, row) => <Space><Button size="small" loading={retry.isPending} onClick={() => retry.mutate(String(row.id || row.resumeId))}>重新解析</Button><Button size="small" onClick={() => setDetailId(String(row.id || row.resumeId))}>查看</Button></Space> }
          ]}
        />
        <Space orientation="vertical" style={{ width: '100%' }}>
          <Card title="简历详情" className="surface-card" loading={Boolean(detailId) && detail.isLoading}>
            {active ? <EntitySummary record={active} fields={[{ label: '文件名', keys: ['fileName', 'resumeTitle'] }, { label: '评分', keys: ['score'] }, { label: '更新时间', keys: ['updateTime'] }, { label: '状态', keys: ['status'], tag: true }]} /> : '请选择一份简历查看详情'}
          </Card>
          <GeneratedArtifactCard title="优化建议" items={suggestions.map((item) => pickText(item, ['title', 'name', 'summary', 'content']))} />
          <Card title="追问问题" className="surface-card" loading={Boolean(detailId) && questions.isLoading}>
            <div className="workspace-grid">
              {normalizeRecords(questions.data).map((item) => <Card key={item.id} size="small">{pickText(item, ['title', 'question'])}</Card>)}
              {Boolean(detailId) && !questions.isLoading && normalizeRecords(questions.data).length === 0 && <div className="muted-text">暂无追问问题</div>}
            </div>
          </Card>
          {detailId && (
            <Card title="版本历史" className="surface-card" loading={versions.isLoading}>
              <List
                dataSource={versionRows}
                locale={{ emptyText: '暂无历史版本' }}
                renderItem={(item) => (
                  <List.Item actions={[
                    <Popconfirm key="restore" title="恢复到该版本？" description="当前内容将被该版本覆盖。" okText="恢复" cancelText="取消" onConfirm={() => restore.mutate(String(item.id || item.versionId))}>
                      <Button size="small" loading={restore.isPending}>恢复</Button>
                    </Popconfirm>
                  ]}>
                    <List.Item.Meta title={pickText(item, ['versionName', 'name', 'title'], '版本')} description={formatDateTime(item.updateTime || item.createTime || item.createdAt)} />
                  </List.Item>
                )}
              />
            </Card>
          )}
        </Space>
      </div>
    </ModulePage>
  )
}
