import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import { createPinia } from 'pinia'
import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import { vLazy } from './directives/lazy'
import './styles/main.css'

const app = createApp(App)
const pinia = createPinia()

app.config.errorHandler = (err, _vm, info) => {
  console.error('[Global Error]', err, info)
}

// Register global directives
app.directive('lazy', vLazy)

app.use(pinia)
app.use(router)
app.use(ElementPlus)
app.mount('#app')

