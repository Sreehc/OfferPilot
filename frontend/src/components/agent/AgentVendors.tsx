import { Bubble, Prompts, Sender, Think, ThoughtChain } from '@ant-design/x'
import { AssistantRuntimeProvider } from '@assistant-ui/react'
import { CopilotKit } from '@copilotkit/react-core'
import { Card, Space, Tag, Typography } from 'antd'

export function AgentVendorsPanel() {
  const assistantSurface = Boolean(AssistantRuntimeProvider)
  const copilotSurface = Boolean(CopilotKit)

  return (
    <Card title="Agent 交互底座" className="surface-card">
      <Space direction="vertical" style={{ width: '100%' }} size={12}>
        <Bubble role="assistant" content="这里承载对话气泡和生成式内容。" />
        <Prompts items={[{ key: 'resume', label: '优化简历' }, { key: 'interview', label: '准备面试' }, { key: 'plan', label: '生成学习计划' }]} />
        <Sender placeholder="输入 Agent 指令" />
        <ThoughtChain items={[{ key: 'plan', title: 'Plan', description: '拆解目标并选择工具' }, { key: 'tool', title: 'Tool', description: '调用后端 agent run 或知识检索' }]} />
        <Think title="当前思考" defaultExpanded>
          <Typography.Text>等待用户确认或后端结果。</Typography.Text>
        </Think>
        <Space wrap>
          <Tag color={assistantSurface ? 'blue' : 'default'}>assistant-ui</Tag>
          <Tag color="green">Ant Design X</Tag>
          <Tag color={copilotSurface ? 'purple' : 'default'}>CopilotKit pilot</Tag>
        </Space>
      </Space>
    </Card>
  )
}
