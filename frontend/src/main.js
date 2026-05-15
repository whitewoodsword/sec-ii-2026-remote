import './style.css'
import { createApp } from 'vue'
import App from './App.vue'
import { createPinia } from 'pinia'
import router from './router'  // 导入路由配置

const app = createApp(App)

app.use(createPinia()) 
app.use(router)  // 注册路由插件
app.mount('#app')
