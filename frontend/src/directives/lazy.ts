import type { Directive } from 'vue'

/**
 * v-lazy directive for lazy loading images.
 * Usage: <img v-lazy="imageUrl" alt="..." />
 *
 * Uses IntersectionObserver to load images when they enter the viewport.
 */
export const vLazy: Directive<HTMLImageElement, string> = {
  mounted(el, binding) {
    const imageEl = el as HTMLImageElement & { _lazyObserver?: IntersectionObserver }
    const defaultSrc = el.src || ''
    el.dataset.src = binding.value

    // Set a transparent placeholder
    el.src = 'data:image/svg+xml,%3Csvg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1 1"%3E%3C/svg%3E'
    el.style.backgroundColor = 'var(--bc-surface-muted)'

    const observer = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (entry.isIntersecting) {
            const src = el.dataset.src
            if (src) {
              el.src = src
              el.onload = () => {
                el.classList.add('loaded')
                el.style.backgroundColor = ''
              }
              el.onerror = () => {
                if (defaultSrc) el.src = defaultSrc
                el.style.backgroundColor = ''
              }
            }
            observer.unobserve(el)
          }
        })
      },
      { rootMargin: '200px' }
    )

    observer.observe(el)
    imageEl._lazyObserver = observer
  },

  updated(el, binding) {
    if (binding.value !== binding.oldValue) {
      el.dataset.src = binding.value
    }
  },

  unmounted(el) {
    const imageEl = el as HTMLImageElement & { _lazyObserver?: IntersectionObserver }
    imageEl._lazyObserver?.disconnect()
  },
}
