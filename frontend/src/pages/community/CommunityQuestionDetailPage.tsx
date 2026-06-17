import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { App as AntApp, Button, Card, Form, Input, List, Space, Typography } from 'antd'
import { useParams } from 'react-router-dom'
import { acceptCommunityAnswerApi, fetchCommunityQuestionDetailApi, submitCommunityAnswerApi, voteCommunityApi } from '@/api/modules/community'
import { getErrorMessage } from '@/api/client'
import { normalizeRecords, pickText } from '@/modules/common'
import { ModulePage } from '@/modules/common'

export function CommunityQuestionDetailPage() {
  const { id } = useParams()
  const questionId = Number(id)
  const { message } = AntApp.useApp()
  const queryClient = useQueryClient()
  const detail = useQuery({ queryKey: ['community', 'question', id], queryFn: () => fetchCommunityQuestionDetailApi(questionId).then((response) => response.data), enabled: Boolean(id) })
  const answer = useMutation({ mutationFn: (content: string) => submitCommunityAnswerApi({ questionId, content }), onSuccess: () => { message.success('回答已提交'); queryClient.invalidateQueries({ queryKey: ['community', 'question', id] }) }, onError: (error) => message.error(getErrorMessage(error, '提交回答失败')) })
  const accept = useMutation({ mutationFn: (answerId: number) => acceptCommunityAnswerApi(questionId, answerId), onSuccess: () => { message.success('已采纳回答'); queryClient.invalidateQueries({ queryKey: ['community', 'question', id] }) }, onError: (error) => message.error(getErrorMessage(error, '采纳失败')) })
  const vote = useMutation({ mutationFn: (targetId: number) => voteCommunityApi({ targetType: 'question', targetId, voteType: 'up' }), onSuccess: () => { message.success('已投票') }, onError: (error) => message.error(getErrorMessage(error, '投票失败')) })
  const record = detail.data as any
  const answers = normalizeRecords(record?.answers || record?.answerList)
  return (
    <ModulePage title="问题详情" description="查看提问、回答和采纳状态。">
      <div className="workspace-grid two">
        <Card title={pickText(record, ['title'], '问题详情')} loading={detail.isLoading} className="surface-card">
          <Typography.Paragraph>{pickText(record, ['content', 'description'], '暂无内容')}</Typography.Paragraph>
          <Space><Button onClick={() => vote.mutate(questionId)}>赞同问题</Button></Space>
        </Card>
        <Card title="提交回答" className="surface-card">
          <Form layout="vertical" onFinish={({ content }) => answer.mutate(content)}>
            <Form.Item label="回答内容" name="content" rules={[{ required: true }]}><Input.TextArea rows={6} /></Form.Item>
            <Button type="primary" htmlType="submit" loading={answer.isPending}>提交回答</Button>
          </Form>
        </Card>
      </div>
      <Card title="回答列表" className="surface-card">
        <List dataSource={answers} locale={{ emptyText: '暂无回答' }} renderItem={(item) => <List.Item actions={[<Button key="accept" size="small" onClick={() => accept.mutate(Number(item.id || item.answerId))}>采纳</Button>]}><List.Item.Meta title={pickText(item, ['authorName', 'username'], '社区用户')} description={pickText(item, ['content'], '回答内容')} /></List.Item>} />
      </Card>
    </ModulePage>
  )
}
