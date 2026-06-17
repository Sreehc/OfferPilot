import { useMutation } from '@tanstack/react-query'
import { App as AntApp, Button, Card, Form, Input } from 'antd'
import { useNavigate } from 'react-router-dom'
import { createCommunityQuestionApi } from '@/api/modules/community'
import { getErrorMessage } from '@/api/client'
import { ModulePage } from '@/modules/common'

export function CommunitySubmitPage() {
  const navigate = useNavigate()
  const { message } = AntApp.useApp()
  const create = useMutation({
    mutationFn: createCommunityQuestionApi,
    onSuccess: (response) => {
      message.success('问题已提交')
      const id = (response.data as any)?.id
      navigate(id ? `/community/question/${id}` : '/community')
    },
    onError: (error) => message.error(getErrorMessage(error, '提交失败'))
  })
  return <ModulePage title="发起提问" description="提交新问题并等待社区回复。"><Card className="surface-card"><Form layout="vertical" onFinish={(values) => create.mutate(values)}><Form.Item label="标题" name="title" rules={[{ required: true }]}><Input /></Form.Item><Form.Item label="内容" name="content" rules={[{ required: true }]}><Input.TextArea rows={6} /></Form.Item><Button type="primary" htmlType="submit" loading={create.isPending}>提交</Button></Form></Card></ModulePage>
}
