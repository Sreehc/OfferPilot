<template>
  <div class="space-y-4">
    <!-- Waveform Visualization -->
    <div class="relative h-24 overflow-hidden rounded-xl bg-[var(--panel-muted)]">
      <canvas ref="canvasRef" class="h-full w-full"></canvas>
      <div v-if="!isRecording && !audioUrl" class="absolute inset-0 flex items-center justify-center">
        <span class="text-sm text-tertiary">点击下方按钮开始录音</span>
      </div>
      <div v-if="isRecording" class="absolute top-3 right-3 flex items-center gap-2">
        <span class="h-2 w-2 animate-pulse rounded-full bg-red-500"></span>
        <span class="text-xs font-semibold tabular-nums text-red-500">{{ formatDuration(duration) }}</span>
      </div>
    </div>

    <!-- Controls -->
    <div class="flex items-center justify-center gap-4">
      <!-- Record / Stop -->
      <button
        v-if="!isRecording && !audioUrl"
        type="button"
        class="flex h-14 w-14 items-center justify-center rounded-full bg-red-500 text-white shadow-lg transition hover:bg-red-600 active:scale-95"
        @click="startRecording"
      >
        <UiIcon name="microphone" class="h-6 w-6" />
      </button>

      <button
        v-if="isRecording"
        type="button"
        class="flex h-14 w-14 items-center justify-center rounded-full bg-[var(--interactive-bg)] text-primary shadow-lg transition hover:bg-[var(--interactive-hover)] active:scale-95"
        @click="stopRecording"
      >
        <UiIcon name="stop" class="h-6 w-6" />
      </button>

      <!-- Playback (after recording) -->
      <template v-if="audioUrl && !isRecording">
        <button
          type="button"
          class="flex h-12 w-12 items-center justify-center rounded-full bg-accent text-white shadow transition hover:opacity-90 active:scale-95"
          @click="togglePlayback"
        >
          <UiIcon :name="isPlaying ? 'stop' : 'videoPlay'" class="h-5 w-5" />
        </button>

        <button
          type="button"
          class="flex h-10 w-10 items-center justify-center rounded-full bg-[var(--interactive-bg)] text-secondary transition hover:bg-[var(--interactive-hover)]"
          title="重新录制"
          @click="resetRecording"
        >
          <UiIcon name="refreshRight" class="h-4 w-4" />
        </button>
      </template>
    </div>

    <!-- Status -->
    <p v-if="statusText" class="text-center text-xs text-secondary">
      {{ statusText }}
    </p>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { UiIcon } from '@/components/ui'

const props = defineProps<{
  disabled?: boolean
}>()

const emit = defineEmits<{
  recorded: [blob: Blob]
  cleared: []
}>()

const canvasRef = ref<HTMLCanvasElement | null>(null)
const isRecording = ref(false)
const isPlaying = ref(false)
const audioUrl = ref<string | null>(null)
const duration = ref(0)

let mediaRecorder: MediaRecorder | null = null
let audioContext: AudioContext | null = null
let analyser: AnalyserNode | null = null
let animationFrame: number | null = null
let durationTimer: ReturnType<typeof setInterval> | null = null
let audioElement: HTMLAudioElement | null = null
let recordedChunks: Blob[] = []

const readThemeToken = (name: string) => getComputedStyle(document.documentElement).getPropertyValue(name).trim()

const statusText = computed(() => {
  if (isRecording.value) return '正在录音...'
  if (audioUrl.value && !isPlaying.value) return '录音完成，可播放试听或重新录制'
  if (isPlaying.value) return '播放中...'
  return ''
})

const formatDuration = (seconds: number) => {
  const m = Math.floor(seconds / 60)
  const s = seconds % 60
  return `${m}:${String(s).padStart(2, '0')}`
}

const drawWaveform = () => {
  if (!canvasRef.value || !analyser) return

  const canvas = canvasRef.value
  const ctx = canvas.getContext('2d')
  if (!ctx) return

  // Set canvas size
  const rect = canvas.getBoundingClientRect()
  canvas.width = rect.width * window.devicePixelRatio
  canvas.height = rect.height * window.devicePixelRatio
  ctx.scale(window.devicePixelRatio, window.devicePixelRatio)

  const bufferLength = analyser.frequencyBinCount
  const dataArray = new Uint8Array(bufferLength)

  const draw = () => {
    if (!isRecording.value) return
    animationFrame = requestAnimationFrame(draw)

    analyser!.getByteTimeDomainData(dataArray)

    ctx.clearRect(0, 0, rect.width, rect.height)

    // Draw waveform
    ctx.lineWidth = 2
    ctx.strokeStyle = readThemeToken('--bc-cyan') || '#2f7f77'
      ctx.beginPath()

      const sliceWidth = rect.width / bufferLength
      let x = 0

      for (let i = 0; i < bufferLength; i++) {
      const v = (dataArray[i] ?? 0) / 128.0
        const y = (v * rect.height) / 2

      if (i === 0) {
        ctx.moveTo(x, y)
      } else {
        ctx.lineTo(x, y)
      }
      x += sliceWidth
    }

    ctx.lineTo(rect.width, rect.height / 2)
    ctx.stroke()

    // Draw center line
    const cyanRgb = readThemeToken('--bc-cyan-rgb') || '47 127 119'
    ctx.strokeStyle = `rgb(${cyanRgb} / 0.15)`
    ctx.lineWidth = 1
    ctx.beginPath()
    ctx.moveTo(0, rect.height / 2)
    ctx.lineTo(rect.width, rect.height / 2)
    ctx.stroke()
  }

  draw()
}

const startRecording = async () => {
  if (props.disabled) return

  try {
    const stream = await navigator.mediaDevices.getUserMedia({ audio: true })

    audioContext = new AudioContext()
    const source = audioContext.createMediaStreamSource(stream)
    analyser = audioContext.createAnalyser()
    analyser.fftSize = 2048
    source.connect(analyser)

    mediaRecorder = new MediaRecorder(stream, {
      mimeType: MediaRecorder.isTypeSupported('audio/webm;codecs=opus')
        ? 'audio/webm;codecs=opus'
        : 'audio/webm'
    })

    recordedChunks = []
    mediaRecorder.ondataavailable = (e) => {
      if (e.data.size > 0) recordedChunks.push(e.data)
    }

    mediaRecorder.onstop = () => {
      const blob = new Blob(recordedChunks, { type: mediaRecorder?.mimeType || 'audio/webm' })
      audioUrl.value = URL.createObjectURL(blob)
      emit('recorded', blob)

      // Stop all tracks
      stream.getTracks().forEach(t => t.stop())
    }

    mediaRecorder.start(100) // Collect data every 100ms
    isRecording.value = true
    duration.value = 0
    drawWaveform()

    durationTimer = setInterval(() => {
      duration.value++
    }, 1000)

  } catch (err) {
    console.error('Failed to start recording:', err)
  }
}

const stopRecording = () => {
  if (mediaRecorder && mediaRecorder.state !== 'inactive') {
    mediaRecorder.stop()
  }
  isRecording.value = false
  if (animationFrame) cancelAnimationFrame(animationFrame)
  if (durationTimer) clearInterval(durationTimer)
  if (audioContext) {
    audioContext.close()
    audioContext = null
  }
}

const togglePlayback = () => {
  if (!audioUrl.value) return

  if (isPlaying.value) {
    audioElement?.pause()
    isPlaying.value = false
    return
  }

  if (!audioElement) {
    audioElement = new Audio(audioUrl.value)
    audioElement.onended = () => { isPlaying.value = false }
  }

  audioElement.play()
  isPlaying.value = true
}

const resetRecording = () => {
  if (audioElement) {
    audioElement.pause()
    audioElement = null
  }
  if (audioUrl.value) URL.revokeObjectURL(audioUrl.value)
  audioUrl.value = null
  isPlaying.value = false
  duration.value = 0
  recordedChunks = []

  // Clear canvas
  if (canvasRef.value) {
    const ctx = canvasRef.value.getContext('2d')
    if (ctx) ctx.clearRect(0, 0, canvasRef.value.width, canvasRef.value.height)
  }

  emit('cleared')
}

onBeforeUnmount(() => {
  stopRecording()
  if (audioElement) {
    audioElement.pause()
    audioElement = null
  }
  if (audioUrl.value) URL.revokeObjectURL(audioUrl.value)
})

watch(() => props.disabled, (val) => {
  if (val && isRecording.value) stopRecording()
})
</script>
