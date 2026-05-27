import { createRouter, createWebHistory } from 'vue-router'
import ReviewPage from '../views/ReviewPage.vue'

const routes = [
  { path: '/', redirect: '/review' },
  { path: '/review', component: ReviewPage },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

export default router
