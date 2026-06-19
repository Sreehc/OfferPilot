import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { App as AntApp, Button, Popconfirm, Select, Space, Tag } from 'antd'
import { Link } from 'react-router-dom'
import { deleteWrongApi, exportWrongMarkdownApi, fetchWrongListApi, updateMasteryApi } from '@/api/modules/wrong'
import { getErrorMessage } from '@/api/client'
import { DataTableCard, downloadBlob, formatDateTime, masteryLabel, normalizeRecords, pickText } from '@/modules/common'
import { ModulePage } from '@/modules/common'

export function WrongBookPage() {
  const { message } = AntApp.useApp()
  const queryClient = useQueryClient()
  const list = useQuery({ queryKey: ['wrong', 'list'], queryFn: () => fetchWrongListApi(1, 20).then((response) => response.data) })
  const invalidate = () => queryClient.invalidateQueries({ queryKey: ['wrong'] })
  const mastery = useMutation({ mutationFn: ({ id, status }: { id: number; status: string }) => updateMasteryApi(id, { status }), onSuccess: () => { message.success('掌握度已更新'); invalidate() }, onError: (error) => message.error(getErrorMessage(error, '更新失败')) })
  const remove = useMutation({ mutationFn: (id: number) => deleteWrongApi(id), onSuccess: () => { message.success('错题已删除'); invalidate() }, onError: (error) => message.error(getErrorMessage(error, '删除失败')) })
  const exportWrong = useMutation({ mutationFn: exportWrongMarkdownApi, onSuccess: (response) => { downloadBlob(response.data, 'wrong-questions.md'); message.success('错题已导出') }, onError: (error) => message.error(getErrorMessage(error, '导出失败')) })
  const rows = normalizeRecords(list.data)
  return (
    <ModulePage
      title="错题本"
      description="重做错题、查看解析并管理掌握度。"
      metrics={[
        { label: '错题总数', value: rows.length, hint: '当前筛选' },
        { label: '复习中', value: rows.filter((row) => pickText(row, ['masteryLevel', 'status']).toLowerCase().includes('review')).length, hint: '需要继续练习' },
        { label: '已掌握', value: rows.filter((row) => pickText(row, ['masteryLevel', 'status']).toLowerCase().includes('master')).length, hint: '可降低频率' },
        { label: '导出', value: exportWrong.isPending ? '进行中' : '可用', hint: 'Markdown' }
      ]}
      actions={<Button type="primary" loading={exportWrong.isPending} onClick={() => exportWrong.mutate()}>导出 Markdown</Button>}
    >
      <DataTableCard
        title="错题列表"
        data={list.data}
        loading={list.isLoading}
        error={list.error}
        onRetry={() => list.refetch()}
        emptyTitle="暂无错题，去题库练习后这里会自动收集"
        columns={[
          { title: '题目', render: (_, row) => <Link to={`/question/${row.questionId || row.id}`}>{pickText(row, ['title', 'questionTitle', 'name'], '错题')}</Link> },
          { title: '分类', render: (_, row) => pickText(row, ['categoryName', 'category']) },
          { title: '掌握度', render: (_, row) => <Tag>{masteryLabel(pickText(row, ['masteryLevel', 'status']))}</Tag> },
          { title: '更新时间', render: (_, row) => formatDateTime(row.updateTime || row.createTime) },
          { title: '操作', render: (_, row) => <Space wrap><Link to={`/question/${row.questionId || row.id}`}><Button size="small" type="primary">重新作答</Button></Link><Select size="small" style={{ width: 110 }} defaultValue={pickText(row, ['masteryLevel'], 'reviewing')} options={[{ value: 'not_started', label: '未开始' }, { value: 'reviewing', label: '复习中' }, { value: 'mastered', label: '已掌握' }]} onChange={(status) => mastery.mutate({ id: Number(row.id), status })} /><Popconfirm title="删除这道错题？" description="此操作不可恢复。" okText="删除" okButtonProps={{ danger: true }} cancelText="取消" onConfirm={() => remove.mutate(Number(row.id))}><Button danger size="small" loading={remove.isPending}>删除</Button></Popconfirm></Space> }
        ]}
      />
    </ModulePage>
  )
}
