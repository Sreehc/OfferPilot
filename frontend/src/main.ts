import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import { createPinia } from 'pinia'
import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import './styles/main.css'

const app = createApp(App)
const pinia = createPinia()

app.config.errorHandler = (err, _vm, info) => {
  console.error('[Global Error]', err, info)
}

app.use(pinia)
app.use(router)
app.use(ElementPlus)
app.mount('#app')

