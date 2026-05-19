// src/router/index.js
import { createRouter, createWebHistory } from 'vue-router'
import HelloWorld from "../components/HelloWorld.vue";
import WelcomePage from "../components/WelcomePage.vue";
import HomePage from '../components/HomePage.vue';
import LogInPage from '../components/LogInPage.vue';
import RegisterPage from '../components/RegisterPage.vue';
import UserProfilePage from '../components/UserProfilePage.vue';
import OrderWorkbenchPage from '../components/OrderWorkbenchPage.vue';


// 定义路由（路径 -> 组件的映射）
const routes = [
    {
        path: '/',
        name: 'homepage',
        component: HomePage
    },
    {
        path: '/welcome',
        name: 'welcome',
        component: WelcomePage
    },
    {
        path: '/hello',
        name: 'hello',
        component: HelloWorld
    },
    {
        path: '/my/profile',
        name: 'userProfile',
        component: UserProfilePage
    },
    {
        path: '/register',
        name: 'register',
        component: RegisterPage

    },
    {
        path: '/login',
        name: 'login',
        component: LogInPage
    },
    {
        path: '/orders/workbench',
        name: 'orderWorkbench',
        component: OrderWorkbenchPage
    },
    
    {
        // 404 路由 - 匹配所有未定义的路径
        path: '/:pathMatch(.*)*',
        name: 'notfound',
        component: WelcomePage
    }
    
]

// 创建路由实例
const router = createRouter({
    // 使用 history 模式（需要服务器配置）
    // 如果想用 hash 模式，改用 createWebHashHistory()
    history: createWebHistory(import.meta.env.BASE_URL),
    routes
})

export default router
