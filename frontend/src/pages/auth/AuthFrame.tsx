import { Card, Typography } from 'antd'
import type { ReactNode } from 'react'
import { BrandGlyph } from '@/components/brand/BrandGlyph'

const HIGHLIGHTS = [
  '题库刷题 · 模拟面试 · 错题复习闭环',
  '简历优化与投递跟进一站管理',
  'AI 助手陪练，定位能力薄弱项'
]

export function AuthFrame({ title, description, children }: { title: string; description: string; children: ReactNode }) {
  return (
    <div className="auth-shell">
      <section className="auth-brand">
        <div className="auth-brand__mark">
          <BrandGlyph size={40} />
          <div>
            <div className="auth-brand__name">OfferPilot</div>
            <div className="auth-brand__meta">AI 求职训练平台</div>
          </div>
        </div>
        <div className="auth-brand__kicker"><span className="auth-pulse" aria-hidden="true" />求职训练工作流</div>
        <h1 className="auth-brand__title">把每一次练习<br />变成下一个 Offer</h1>
        <p className="auth-brand__desc">题库、模拟面试、简历投递与复盘复习，串成一条能坚持下来的求职训练闭环。</p>
        <ul className="auth-brand__list">
          {HIGHLIGHTS.map((item) => <li key={item}>{item}</li>)}
        </ul>
      </section>
      <section className="auth-form">
        <Card className="surface-card" style={{ width: 'min(440px, 100%)' }}>
          <div className="auth-card__head">
            <BrandGlyph size={32} />
            <div>
              <Typography.Title level={3} style={{ margin: 0 }}>{title}</Typography.Title>
              <Typography.Text type="secondary">{description}</Typography.Text>
            </div>
          </div>
          {children}
        </Card>
      </section>
    </div>
  )
}
