/**
 * 应用入口（f-foundation）。
 *
 * <p>注册顺序：Element Plus（全局引入 + zh-cn locale）→ Pinia → Router。
 * Pinia 必须先于 Router 安装，确保路由守卫中可安全使用 store。
 */
import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import 'element-plus/dist/index.css'

import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import './style.css'

const app = createApp(App)

app.use(createPinia())
app.use(router)
app.use(ElementPlus, { locale: zhCn })

app.mount('#app')
