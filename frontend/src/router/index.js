import { createRouter, createWebHistory } from 'vue-router'
import HelloWorld from "../components/HelloWorld.vue";
import WelcomePage from "../components/WelcomePage.vue";
import HomePage from '../components/HomePage.vue';
import LogInPage from '../components/LogInPage.vue';
import RegisterPage from '../components/RegisterPage.vue';
import UserProfilePage from '../components/UserProfilePage.vue';
import CreateDemandPage from '../components/CreateDemandPage.vue';
import EditDemandPage from '../components/EditDemandPage.vue';
import DemandDetailPage from '../components/DemandDetailPage.vue';
import MyDemandsPage from '../components/MyDemandsPage.vue';
import OrderWorkbenchPage from '../components/OrderWorkbenchPage.vue';
import ReviewPage from '../views/ReviewPage.vue'
import OrderDetailPage from '../components/OrderDetailPage.vue'
import MyOrdersPage from '../components/MyOrdersPage.vue'
import MyConversationsPage from '../components/MyConversationsPage.vue'

// 定义路由（路径 -> 组件的映射）
const routes = [
    {
        path: '/',
        name: 'homepage',
        component: HomePage
    },
    {
        path: '/demand/:id',
        name: 'demandDetail',
        component: DemandDetailPage
    },
    {
        path: '/my/demands',
        name: 'myDemands',
        component: MyDemandsPage
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
        path: '/edit/demand/:id',
        name: 'editDemand',
        component: EditDemandPage
    },
    {
        path: '/create/demand',
        name: 'createDemand',
        component: CreateDemandPage
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
        path: '/order/:id',
        name: 'orderDetail',
        component: OrderDetailPage
    },
    {
        path: '/review',
        name: 'reviewPage',
        component: ReviewPage
    },
    {
        path: '/my/orders',
        name: 'myOrders',
        component: MyOrdersPage
    },
    {
        path: '/my/conversations',
        name: 'myConversations',
        component: MyConversationsPage
    },
    {
        // 404 路由 - 匹配所有未定义的路径
        path: '/:pathMatch(.*)*',
        name: 'notfound',
        component: WelcomePage
    }
    
]

// 创建路由实例
/*const router = createRouter({
    // 使用 history 模式（需要服务器配置）
    // 如果想用 hash 模式，改用 createWebHashHistory()
   
    routes
})*/

const router = createRouter({
    history: createWebHistory(import.meta.env.BASE_URL),
    routes,
    scrollBehavior() {
        // 每次路由切换都回到顶部
        return { top: 0, behavior: 'auto' };
    }
});

export default router
