<template>
  <div class="auth-immersive-shell px-4 py-8 md:px-6 md:py-10">
    <a v-if="skipLabel" href="#auth-main" class="skip-link">
      {{ skipLabel }}
    </a>

    <main
      id="auth-main"
      class="auth-viewport mx-auto grid min-h-[calc(100vh-4rem)] items-stretch gap-4"
      :class="
        wide
          ? 'max-w-[1120px] xl:grid-cols-[minmax(0,0.92fr)_minmax(360px,1.08fr)]'
          : 'max-w-[1080px] xl:grid-cols-[minmax(248px,0.52fr)_minmax(440px,1.48fr)]'
      "
    >
      <section
        class="shell-section-card auth-brand-panel p-5 sm:p-6"
        :class="wide ? 'order-1 flex' : 'order-2 hidden xl:order-1 xl:flex'"
      >
        <RouterLink to="/login" class="auth-brand-mark">
          <AppBrandGlyph :size="38" />
          <div>
            <div class="auth-brand-mark__name">OfferPilot</div>
            <div class="auth-brand-mark__meta">AI 求职训练平台</div>
          </div>
        </RouterLink>

        <div class="flex items-center gap-3">
          <span class="state-pulse" aria-hidden="true" />
          <p class="section-kicker">
            {{ brandKicker }}
          </p>
        </div>

        <div class="mt-4 max-w-md">
          <h1 v-if="brandTitle" class="auth-hero-title">
            {{ brandTitle }}
          </h1>
          <p class="mt-3 text-sm leading-7 text-secondary" :class="{ 'sm:text-base': brandTitle }">
            {{ brandDescription }}
          </p>
        </div>

        <div v-if="sideNote" class="auth-side-note">
          {{ sideNote }}
        </div>
      </section>

      <section class="shell-section-card auth-form-panel order-1 p-6 sm:p-8 md:p-10 xl:order-2">
        <slot />
      </section>
    </main>
  </div>
</template>

<script setup lang="ts">
import AppBrandGlyph from '@/components/AppBrandGlyph.vue'

withDefaults(
  defineProps<{
    skipLabel?: string
    brandKicker: string
    brandDescription: string
    brandTitle?: string
    sideNote?: string
    wide?: boolean
  }>(),
  {
    skipLabel: '',
    brandTitle: '',
    sideNote: '',
    wide: false
  }
)
</script>

<style scoped>
.auth-immersive-shell {
  min-height: 100dvh;
}

.skip-link {
  position: absolute;
  left: 1.25rem;
  top: 0.75rem;
  z-index: 20;
  transform: translateY(-180%);
  border-radius: 999px;
  background: var(--bc-ink);
  color: var(--bc-shell);
  padding: 0.55rem 0.9rem;
  font-size: 0.85rem;
  font-weight: 700;
  transition: transform 160ms ease;
}

.skip-link:focus {
  transform: translateY(0);
}

.auth-brand-panel,
.auth-form-panel {
  min-height: 100%;
}

.auth-brand-panel {
  flex-direction: column;
  justify-content: flex-start;
  gap: 0.9rem;
  background:
    radial-gradient(circle at 16% 18%, rgba(var(--bc-accent-rgb), 0.12), transparent 30%),
    radial-gradient(circle at 82% 14%, rgba(var(--bc-cyan-rgb), 0.08), transparent 24%),
    linear-gradient(145deg, rgba(var(--bc-ink-rgb), 0.04), transparent 42%), var(--panel-bg);
}

.auth-hero-title {
  font-family: theme('fontFamily.display');
  font-size: clamp(2.2rem, 3.3vw, 3.7rem);
  line-height: 0.98;
  letter-spacing: 0;
  color: var(--bc-ink);
  text-wrap: balance;
}

.auth-side-note {
  max-width: 21rem;
  border-radius: calc(var(--radius-md) - 4px);
  border: 1px solid var(--bc-border-subtle);
  background: var(--bc-surface-muted);
  padding: 0.9rem 1rem;
  font-size: 0.88rem;
  line-height: 1.7;
  color: var(--bc-ink-secondary);
}

@media (min-width: 1280px) {
  .auth-form-panel {
    padding-inline: 2.75rem;
  }
}

@media (min-width: 768px) {
  .auth-form-panel {
    padding-inline: 2.5rem;
  }
}
</style>
