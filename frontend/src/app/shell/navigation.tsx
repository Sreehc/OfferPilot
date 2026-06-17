import { BarChartOutlined, BookOutlined, CheckSquareOutlined, DashboardOutlined, DatabaseOutlined, FileDoneOutlined, HeartOutlined, MessageOutlined, ReadOutlined, RobotOutlined, SafetyCertificateOutlined, ScheduleOutlined, SettingOutlined, TeamOutlined, TrophyOutlined } from '@ant-design/icons'
import type { ReactNode } from 'react'

export interface NavItem { path: string; label: string; icon: ReactNode; adminOnly?: boolean }

export const navItems: NavItem[] = [
  { path: '/dashboard', label: '首页', icon: <DashboardOutlined /> },
  { path: '/chat', label: 'AI 问答', icon: <MessageOutlined /> },
  { path: '/agent', label: 'Agent 工作台', icon: <RobotOutlined /> },
  { path: '/question', label: '题库', icon: <BookOutlined /> },
  { path: '/knowledge', label: '知识库', icon: <DatabaseOutlined /> },
  { path: '/interview', label: '面试', icon: <FileDoneOutlined /> },
  { path: '/study-plan', label: '学习计划', icon: <ScheduleOutlined /> },
  { path: '/resume', label: '简历', icon: <ReadOutlined /> },
  { path: '/applications', label: '投递', icon: <CheckSquareOutlined /> },
  { path: '/analytics', label: '分析', icon: <BarChartOutlined /> },
  { path: '/favorites', label: '收藏', icon: <HeartOutlined /> },
  { path: '/wrong', label: '错题本', icon: <SafetyCertificateOutlined /> },
  { path: '/review', label: '复习', icon: <TrophyOutlined /> },
  { path: '/community', label: '社区', icon: <TeamOutlined /> },
  { path: '/settings', label: '设置', icon: <SettingOutlined /> },
  { path: '/admin', label: '管理后台', icon: <SafetyCertificateOutlined />, adminOnly: true }
]
