import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { App as AntApp, Button, Form, Input, Modal, Popconfirm, Select, Space, Tag } from 'antd'
import { Link } from 'react-router-dom'
import { addQuestionApi, deleteQuestionApi, fetchQuestionsApi } from '@/api/modules/question'
import { fetchCategoriesApi } from '@/api/modules/category'
import { getErrorMessage } from '@/api/client'
import { useAuthStore } from '@/features/auth/authStore'
import { difficultyLabel, DataTableCard, normalizeRecords, pickText } from '@/modules/common'
import { ModulePage } from '@/modules/common'

export function QuestionBankPage() {
  const { message } = AntApp.useApp()
  const queryClient = useQueryClient()
  const isAdmin = useAuthStore((state) => state.user?.role) === 'ADMIN'
  const [open, setOpen] = useState(false)
  const [filters, setFilters] = useState({ keyword: '', categoryId: undefined as number | undefined, difficulty: undefined as string | undefined })
  const query = useQuery({ queryKey: ['questions', filters], queryFn: () => fetchQuestionsApi({ pageNum: 1, pageSize: 20, ...filters }).then((response) => response.data) })
  const categories = useQuery({ queryKey: ['categories'], queryFn: () => fetchCategoriesApi().then((response) => response.data) })
  const invalidate = () => queryClient.invalidateQueries({ queryKey: ['questions'] })
  const add = useMutation({ mutationFn: addQuestionApi, onSuccess: () => { message.success('题目已新增'); setOpen(false); invalidate() }, onError: (error) => message.error(getErrorMessage(error, '新增题目失败')) })
  const remove = useMutation({ mutationFn: (id: number) => deleteQuestionApi(id), onSuccess: () => { message.success('题目已删除'); invalidate() }, onError: (error) => message.error(getErrorMessage(error, '删除失败')) })
  const rows = normalizeRecords(query.data)
  const categoryRows = normalizeRecords(categories.data)
  const columns = [
    { title: '题目', render: (_: unknown, row: Record<string, any>) => <Link to={`/question/${row.id || row.questionId}`}>{pickText(row, ['title', 'name'], '未命名题目')}</Link> },
    { title: '分类', render: (_: unknown, row: Record<string, any>) => pickText(row, ['categoryName', 'category']) },
    { title: '难度', render: (_: unknown, row: Record<string, any>) => <Tag>{difficultyLabel(pickText(row, ['difficulty']))}</Tag> },
    { title: '标签', render: (_: unknown, row: Record<string, any>) => <Tag>{pickText(row, ['tags', 'type'], '-')}</Tag> },
    ...(isAdmin ? [{ title: '操作', render: (_: unknown, row: Record<string, any>) => <Popconfirm title="删除题目" description="此操作不可恢复。" onConfirm={() => remove.mutate(Number(row.id))}><Button danger size="small" loading={remove.isPending}>删除</Button></Popconfirm> }] : [])
  ]
  return (
    <ModulePage
      title="题库"
      description="点击题目进入详情，可练习并查看解析。"
      metrics={[
        { label: '题目数', value: rows.length, hint: '当前页' },
        { label: '分类', value: categoryRows.length, hint: '可筛选方向' },
        { label: '高频题', value: rows.filter((row) => Number(row.frequency || 0) >= 4).length, hint: '重点复习' },
        { label: '状态', value: query.isFetching ? '刷新中' : '已同步', hint: '题库列表' }
      ]}
      actions={<Space wrap><Input.Search allowClear placeholder="搜索题目" onSearch={(keyword) => setFilters((current) => ({ ...current, keyword }))} /><Select allowClear placeholder="分类" style={{ width: 140 }} options={categoryRows.map((item) => ({ value: item.id, label: pickText(item, ['name']) }))} onChange={(categoryId) => setFilters((current) => ({ ...current, categoryId }))} /><Select allowClear placeholder="难度" style={{ width: 120 }} options={[{ value: 'easy', label: '简单' }, { value: 'medium', label: '中等' }, { value: 'hard', label: '困难' }]} onChange={(difficulty) => setFilters((current) => ({ ...current, difficulty }))} />{isAdmin && <Button type="primary" onClick={() => setOpen(true)}>新增题目</Button>}</Space>}
    >
      <DataTableCard
        title="题目列表"
        data={query.data}
        loading={query.isLoading}
        error={query.error}
        onRetry={() => query.refetch()}
        emptyTitle="暂无题目，调整筛选或稍后再来练习"
        columns={columns}
      />
      <Modal open={open} title="新增题目" footer={null} onCancel={() => setOpen(false)}>
        <Form layout="vertical" onFinish={(values) => add.mutate(values)}>
          <Form.Item label="标题" name="title" rules={[{ required: true }]}><Input /></Form.Item>
          <Form.Item label="分类" name="categoryId"><Select allowClear options={categoryRows.map((item) => ({ value: item.id, label: pickText(item, ['name']) }))} /></Form.Item>
          <Form.Item label="难度" name="difficulty" initialValue="medium"><Select options={[{ value: 'easy', label: '简单' }, { value: 'medium', label: '中等' }, { value: 'hard', label: '困难' }]} /></Form.Item>
          <Form.Item label="标准答案" name="standardAnswer"><Input.TextArea rows={5} /></Form.Item>
          <Button type="primary" htmlType="submit" loading={add.isPending}>保存</Button>
        </Form>
      </Modal>
    </ModulePage>
  )
}
