import { BarChartOutlined, BookOutlined, CheckSquareOutlined, DashboardOutlined, DatabaseOutlined, FileDoneOutlined, HeartOutlined, MessageOutlined, ReadOutlined, RobotOutlined, SafetyCertificateOutlined, ScheduleOutlined, SettingOutlined, TeamOutlined, TrophyOutlined } from '@ant-design/icons'
import type { ReactNode } from 'react'

export interface NavItem { path: string; label: string; icon: ReactNode; adminOnly?: boolean }
export interface NavGroup { key: string; label: string; items: NavItem[] }

export const navGroups: NavGroup[] = [
  { key: 'today', label: '今日', items: [
    { path: '/dashboard', label: '今日工作台', icon: <DashboardOutlined /> }
  ]},
  { key: 'train', label: '训练', items: [
    { path: '/question', label: '题库', icon: <BookOutlined /> },
    { path: '/interview', label: '模拟面试', icon: <FileDoneOutlined /> },
    { path: '/study-plan', label: '学习计划', icon: <ScheduleOutlined /> },
    { path: '/review', label: '复习', icon: <TrophyOutlined /> },
    { path: '/wrong', label: '错题本', icon: <SafetyCertificateOutlined /> }
  ]},
  { key: 'job', label: '求职资产', items: [
    { path: '/resume', label: '简历', icon: <ReadOutlined /> },
    { path: '/applications', label: '投递', icon: <CheckSquareOutlined /> }
  ]},
  { key: 'ai', label: '知识与 AI', items: [
    { path: '/chat', label: 'AI 问答', icon: <MessageOutlined /> },
    { path: '/agent', label: 'Agent 工作台', icon: <RobotOutlined /> },
    { path: '/knowledge', label: '知识库', icon: <DatabaseOutlined /> }
  ]},
  { key: 'community', label: '社区', items: [
    { path: '/community', label: '社区', icon: <TeamOutlined /> }
  ]},
  { key: 'system', label: '系统', items: [
    { path: '/analytics', label: '数据分析', icon: <BarChartOutlined /> },
    { path: '/favorites', label: '收藏', icon: <HeartOutlined /> },
    { path: '/settings', label: '设置', icon: <SettingOutlined /> },
    { path: '/admin', label: '管理后台', icon: <SafetyCertificateOutlined />, adminOnly: true }
  ]}
]

// 扁平列表（兼容搜索与旧用法）
export const navItems: NavItem[] = navGroups.flatMap((group) => group.items)

// 移动端底栏核心入口（含“更多”由 AppShell 处理）
export const mobileNavPaths = ['/dashboard', '/question', '/applications', '/community']
