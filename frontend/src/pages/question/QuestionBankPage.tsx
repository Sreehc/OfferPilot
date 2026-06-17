import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { App as AntApp, Button, Form, Input, Modal, Select, Tag } from 'antd'
import { addQuestionApi, deleteQuestionApi, fetchQuestionsApi } from '@/api/modules/question'
import { fetchCategoriesApi } from '@/api/modules/category'
import { getErrorMessage } from '@/api/client'
import { DataTableCard, normalizeRecords, pickText, StatusTag } from '@/modules/common'
import { ModulePage } from '@/modules/common'

export function QuestionBankPage() {
  const { message } = AntApp.useApp()
  const queryClient = useQueryClient()
  const [open, setOpen] = useState(false)
  const query = useQuery({ queryKey: ['questions'], queryFn: () => fetchQuestionsApi({ pageNum: 1, pageSize: 20 }).then((response) => response.data) })
  const categories = useQuery({ queryKey: ['categories'], queryFn: () => fetchCategoriesApi().then((response) => response.data) })
  const invalidate = () => queryClient.invalidateQueries({ queryKey: ['questions'] })
  const add = useMutation({ mutationFn: addQuestionApi, onSuccess: () => { message.success('题目已新增'); setOpen(false); invalidate() }, onError: (error) => message.error(getErrorMessage(error, '新增题目失败')) })
  const remove = useMutation({ mutationFn: (id: number) => deleteQuestionApi(id), onSuccess: () => { message.success('题目已删除'); invalidate() }, onError: (error) => message.error(getErrorMessage(error, '删除失败')) })
  const rows = normalizeRecords(query.data)
  return (
    <ModulePage
      title="题库"
      description="按分类、难度和状态管理题目资源。"
      metrics={[
        { label: '题目数', value: rows.length, hint: '当前页' },
        { label: '分类', value: normalizeRecords(categories.data).length, hint: '可筛选方向' },
        { label: '高频题', value: rows.filter((row) => Number(row.frequency || 0) >= 4).length, hint: '重点复习' },
        { label: '状态', value: query.isFetching ? '刷新中' : '已同步', hint: '题库列表' }
      ]}
      actions={<Button type="primary" onClick={() => setOpen(true)}>新增题目</Button>}
    >
      <DataTableCard
        title="题目列表"
        data={query.data}
        loading={query.isLoading}
        error={query.error}
        onRetry={() => query.refetch()}
        columns={[
          { title: '题目', render: (_, row) => pickText(row, ['title', 'name'], '未命名题目') },
          { title: '分类', render: (_, row) => pickText(row, ['categoryName', 'category']) },
          { title: '难度', render: (_, row) => <StatusTag value={pickText(row, ['difficulty'])} /> },
          { title: '标签', render: (_, row) => <Tag>{pickText(row, ['tags', 'type'], '-')}</Tag> },
          { title: '操作', render: (_, row) => <Button danger size="small" loading={remove.isPending} onClick={() => remove.mutate(Number(row.id))}>删除</Button> }
        ]}
      />
      <Modal open={open} title="新增题目" footer={null} onCancel={() => setOpen(false)}>
        <Form layout="vertical" onFinish={(values) => add.mutate(values)}>
          <Form.Item label="标题" name="title" rules={[{ required: true }]}><Input /></Form.Item>
          <Form.Item label="分类" name="categoryId"><Select allowClear options={normalizeRecords(categories.data).map((item) => ({ value: item.id, label: pickText(item, ['name']) }))} /></Form.Item>
          <Form.Item label="难度" name="difficulty" initialValue="medium"><Select options={[{ value: 'easy', label: '简单' }, { value: 'medium', label: '中等' }, { value: 'hard', label: '困难' }]} /></Form.Item>
          <Form.Item label="标准答案" name="standardAnswer"><Input.TextArea rows={5} /></Form.Item>
          <Button type="primary" htmlType="submit" loading={add.isPending}>保存</Button>
        </Form>
      </Modal>
    </ModulePage>
  )
}
