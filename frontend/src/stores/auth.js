
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useAuthStore = defineStore('auth', () => {
  // 状态
  const token = ref(localStorage.getItem('token') || null)
  const user = ref(JSON.parse(localStorage.getItem('user') || 'null'))
  
  // 计算属性
  const isLoggedIn = computed(() => !!token.value)
  
  // 方法
  function setAuth(data) {
    token.value = data.token
    user.value = data.user || null
    
    // 持久化存储
    localStorage.setItem('token', data.token)
    if (data.user) {
      localStorage.setItem('user', JSON.stringify(data.user))
    }
  }
  

  function clearAuth() {
    token.value = null
    user.value = null
    
    // 清除存储
    localStorage.removeItem('token')
    localStorage.removeItem('user')
  }
  
  function logout() {
    clearAuth()
    // 可以在这里调用后端登出接口
  }
  
  return {
    token,
    user,
    isLoggedIn,
    setAuth,
    clearAuth,
    logout
  }
})