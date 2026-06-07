<template>
  <component
    :is="resolvedIcon"
    class="ui-icon"
    aria-hidden="true"
  />
</template>

<script setup lang="ts">
import { computed } from 'vue'
import {
  Bell,
  Briefcase,
  Calendar,
  ChatDotRound,
  Check,
  CircleCheck,
  CircleClose,
  ArrowLeft,
  ArrowRight,
  Box,
  Connection,
  Download,
  Expand,
  Fold,
  CollectionTag,
  DataAnalysis,
  Document,
  Histogram,
  Monitor,
  Iphone,
  Filter,
  FolderOpened,
  HomeFilled,
  List,
  Loading,
  Lock,
  MagicStick,
  Management,
  Menu,
  More,
  Microphone,
  Moon,
  Notebook,
  Operation,
  Pointer,
  QuestionFilled,
  Reading,
  Refresh,
  RefreshRight,
  Search,
  Setting,
  Sunny,
  Star,
  SwitchButton,
  Top,
  TrendCharts,
  Trophy,
  Upload,
  User,
  VideoPause,
  VideoPlay,
  Warning
} from '@element-plus/icons-vue'

const props = withDefaults(defineProps<{
  name?: string
}>(), {
  name: 'operation'
})

const iconMap = {
  agent: MagicStick,
  applications: Briefcase,
  back: ArrowLeft,
  arrowRight: ArrowRight,
  bell: Bell,
  calendar: Calendar,
  chart: Histogram,
  chat: ChatDotRound,
  check: Check,
  circleCheck: CircleCheck,
  circleClose: CircleClose,
  collapse: Fold,
  connection: Connection,
  community: CollectionTag,
  dashboard: HomeFilled,
  data: DataAnalysis,
  document: Document,
  download: Download,
  computer: Monitor,
  expand: Expand,
  favorites: Star,
  filter: Filter,
  interview: Microphone,
  inbox: Box,
  phone: Iphone,
  knowledge: FolderOpened,
  list: List,
  loading: Loading,
  lock: Lock,
  management: Management,
  menu: Menu,
  microphone: Microphone,
  more: More,
  moon: Moon,
  notebook: Notebook,
  operation: Operation,
  pointer: Pointer,
  question: QuestionFilled,
  reading: Reading,
  refresh: Refresh,
  refreshRight: RefreshRight,
  review: Reading,
  resume: Document,
  search: Search,
  settings: Setting,
  stop: VideoPause,
  study: Notebook,
  sunny: Sunny,
  switch: SwitchButton,
  tablet: Iphone,
  top: Top,
  trend: TrendCharts,
  trophy: Trophy,
  upload: Upload,
  user: User,
  videoPlay: VideoPlay,
  warning: Warning,
  wrong: Reading
} as const

const resolvedIcon = computed(() => iconMap[props.name as keyof typeof iconMap] ?? Operation)
</script>
