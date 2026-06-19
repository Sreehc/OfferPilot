import { useMutation, useQuery } from '@tanstack/react-query'
import { App as AntApp, Button, Card, Form, Input, Select, Space } from 'antd'
import { useNavigate } from 'react-router-dom'
import { createCommunityQuestionApi } from '@/api/modules/community'
import { fetchCategoriesApi } from '@/api/modules/category'
import { getErrorMessage } from '@/api/client'
import { normalizeRecords, pickText } from '@/modules/common'
import { ModulePage } from '@/modules/common'

export function CommunitySubmitPage() {
  const navigate = useNavigate()
  const { message } = AntApp.useApp()
  const categories = useQuery({ queryKey: ['categories'], queryFn: () => fetchCategoriesApi().then((response) => response.data) })
  const categoryRows = normalizeRecords(categories.data)
  const create = useMutation({
    mutationFn: createCommunityQuestionApi,
    onSuccess: (response) => {
      message.success('问题已提交')
      const id = (response.data as any)?.id
      navigate(id ? `/community/question/${id}` : '/community')
    },
    onError: (error) => message.error(getErrorMessage(error, '提交失败'))
  })
  return (
    <ModulePage title="发起提问" description="清晰描述问题更容易得到优质回答。">
      <Card className="surface-card">
        <Form
          layout="vertical"
          onFinish={(values) => create.mutate({ ...values, tags: typeof values.tags === 'string' ? values.tags.split(/[，,\s]+/).filter(Boolean) : values.tags })}
        >
          <Form.Item label="标题" name="title" rules={[{ required: true, message: '请输入标题' }]} extra="用一句话概括你的问题，例如：Spring 事务在什么情况下会失效？"><Input maxLength={80} showCount /></Form.Item>
          <Form.Item label="分类" name="categoryId"><Select allowClear placeholder="选择分类" options={categoryRows.map((item) => ({ value: item.id, label: pickText(item, ['name']) }))} /></Form.Item>
          <Form.Item label="标签" name="tags" extra="多个标签用逗号分隔，例如：Java, 并发, JVM"><Input placeholder="可选" /></Form.Item>
          <Form.Item label="内容" name="content" rules={[{ required: true, message: '请输入内容' }]} extra="补充背景、你的尝试和报错信息；可粘贴代码片段。"><Input.TextArea rows={8} /></Form.Item>
          <Space>
            <Button type="primary" htmlType="submit" loading={create.isPending}>提交</Button>
            <Button onClick={() => navigate('/community')}>取消</Button>
          </Space>
        </Form>
      </Card>
    </ModulePage>
  )
}
