import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { App as AntApp, Button, Card, Form, Input, List, Space, Tag, Typography } from 'antd'
import { ArrowLeftOutlined } from '@ant-design/icons'
import { useNavigate, useParams } from 'react-router-dom'
import { acceptCommunityAnswerApi, fetchCommunityQuestionDetailApi, submitCommunityAnswerApi, voteCommunityApi } from '@/api/modules/community'
import { getErrorMessage } from '@/api/client'
import { useAuthStore } from '@/features/auth/authStore'
import { normalizeRecords, pickText } from '@/modules/common'
import { ModulePage } from '@/modules/common'

export function CommunityQuestionDetailPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const questionId = Number(id)
  const { message } = AntApp.useApp()
  const queryClient = useQueryClient()
  const currentUser = useAuthStore((state) => state.user)
  const detail = useQuery({ queryKey: ['community', 'question', id], queryFn: () => fetchCommunityQuestionDetailApi(questionId).then((response) => response.data), enabled: Boolean(id) })
  const answer = useMutation({ mutationFn: (content: string) => submitCommunityAnswerApi({ questionId, content }), onSuccess: () => { message.success('回答已提交'); queryClient.invalidateQueries({ queryKey: ['community', 'question', id] }) }, onError: (error) => message.error(getErrorMessage(error, '提交回答失败')) })
  const accept = useMutation({ mutationFn: (answerId: number) => acceptCommunityAnswerApi(questionId, answerId), onSuccess: () => { message.success('已采纳回答'); queryClient.invalidateQueries({ queryKey: ['community', 'question', id] }) }, onError: (error) => message.error(getErrorMessage(error, '采纳失败')) })
  const vote = useMutation({ mutationFn: ({ targetType, targetId }: { targetType: 'question' | 'answer'; targetId: number }) => voteCommunityApi({ targetType, targetId, voteType: 'up' }), onSuccess: () => { message.success('已投票'); queryClient.invalidateQueries({ queryKey: ['community', 'question', id] }) }, onError: (error) => message.error(getErrorMessage(error, '投票失败')) })
  const record = detail.data as any
  const answers = normalizeRecords(record?.answers || record?.answerList)
  const acceptedAnswerId = record?.acceptedAnswerId != null ? Number(record.acceptedAnswerId) : undefined
  const authorId = record?.authorId != null ? Number(record.authorId) : record?.userId != null ? Number(record.userId) : undefined
  const isAsker = Boolean(currentUser && authorId != null && Number(currentUser.id) === authorId)
  return (
    <ModulePage
      title="问题详情"
      description="查看提问、回答和采纳状态。"
      actions={<Button icon={<ArrowLeftOutlined />} onClick={() => navigate('/community')}>返回社区</Button>}
    >
      <div className="workspace-grid two">
        <Card title={pickText(record, ['title'], '问题详情')} loading={detail.isLoading} className="surface-card">
          <Typography.Paragraph>{pickText(record, ['content', 'description'], '暂无内容')}</Typography.Paragraph>
          <Space><Button onClick={() => vote.mutate({ targetType: 'question', targetId: questionId })}>赞同问题</Button></Space>
        </Card>
        <Card title="提交回答" className="surface-card">
          <Form layout="vertical" onFinish={({ content }) => answer.mutate(content)}>
            <Form.Item label="回答内容" name="content" rules={[{ required: true }]}><Input.TextArea rows={6} /></Form.Item>
            <Button type="primary" htmlType="submit" loading={answer.isPending}>提交回答</Button>
          </Form>
        </Card>
      </div>
      <Card title="回答列表" className="surface-card">
        <List
          dataSource={answers}
          locale={{ emptyText: '暂无回答，成为第一个回答者' }}
          renderItem={(item) => {
            const answerId = Number(item.id || item.answerId)
            const isAccepted = acceptedAnswerId != null && answerId === acceptedAnswerId
            const actions = [
              <Button key="vote" size="small" onClick={() => vote.mutate({ targetType: 'answer', targetId: answerId })}>赞同 {pickText(item, ['voteCount', 'upvotes', 'likes'], '0')}</Button>
            ]
            if (isAsker && !isAccepted && acceptedAnswerId == null) {
              actions.push(<Button key="accept" type="primary" size="small" loading={accept.isPending} onClick={() => accept.mutate(answerId)}>采纳</Button>)
            }
            return (
              <List.Item actions={actions}>
                <List.Item.Meta
                  title={<Space>{pickText(item, ['authorName', 'username'], '社区用户')}{isAccepted && <Tag color="green">已采纳</Tag>}</Space>}
                  description={pickText(item, ['content'], '回答内容')}
                />
              </List.Item>
            )
          }}
        />
      </Card>
    </ModulePage>
  )
}
