// 品牌图标：还原重构前 OfferPilot 的标记（圆形徽章 + 旋转圆角方块描边 + 右上强调点）。
// 颜色走主题变量，自动适配深浅色。
export function BrandGlyph({ size = 34 }: { size?: number }) {
  return (
    <svg width={size} height={size} viewBox="0 0 32 32" className="brand-glyph" role="img" aria-label="OfferPilot">
      <circle className="brand-glyph__badge" cx="16" cy="16" r="15" />
      <rect className="brand-glyph__core" x="10.5" y="10.5" width="11" height="11" rx="3.4" transform="rotate(45 16 16)" />
      <circle className="brand-glyph__dot" cx="23" cy="10" r="2.8" />
    </svg>
  )
}
