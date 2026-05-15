<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import AlertBox from './SmallComponents/AlertBox.vue'



const authStore = useAuthStore()
const router = useRouter()

// 表单数据
const formData = ref({
  phone: '',
  password: ''
})

// 是否加载中
const loading = ref(false)
const showPassword = ref(false)


const showAlert = ref(false)
const alertConfig = ref({
  title: '系统通知',
  content: '',
  htmlContent: '',
  confirmText: '确定'
})


const showNotification = (title, content, isHtml = false) => {
  alertConfig.value = {
    title: title,
    content: isHtml ? '' : content,
    htmlContent: isHtml ? content : '',
    confirmText: '确定'
  }
  showAlert.value = true
}

const handleAlertConfirm = () => {
  router.push('/')
}

// 处理登录
const handleLogin = () => {
  // 你的登录逻辑
  fetch('http://localhost:8080/users/login', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify(formData.value)
  })
  .then(response => response.json())
  .then(data => {
    console.log(data)
    authStore.setAuth({user: data.user, token: data.token})
    showNotification('登录成功', '欢迎回来，'+data.user.name+'！祝你使用愉快。', true)
  })
  .catch(error => {
    alert('登录失败'+error)
})

  //alert('登录信息:'+formData.value.phone+formData.value.password)
}

// 跳转到注册页面
const goToRegister = () => {
  router.push('/register')
}

// 处理忘记密码
const handleForgotPassword = () => {
  // 你的忘记密码逻辑
  console.log('忘记密码')
}

</script>

<template>
  <div class="login-page">
    <div class="login-container">
      <!-- 返回按钮 -->
      <div class="back-btn" @click="router.push('/')">
        <span class="back-icon">返回主页</span>
      </div>

      <!-- Logo/标题区域 -->
      <div class="login-header">
        <h1 class="login-title">欢迎回来</h1>
        <p class="login-subtitle">登录你的校园互助服务平台账号</p>
      </div>

      <!-- 登录表单 -->
      <form @submit.prevent="handleLogin" class="login-form">
        <!-- 手机号输入框 -->
        <div class="form-group">
          <div class="input-icon">手机号</div>
          <input
            type="tel"
            v-model="formData.phone"
            placeholder="请输入手机号"
            class="form-input"
            maxlength="11"
          />
        </div>

        <!-- 密码输入框 -->
        <div class="form-group">
          <div class="input-icon">密码</div>
          <input
            :type="showPassword ? 'text' : 'password'"
            v-model="formData.password"
            placeholder="请输入密码"
            class="form-input"
          />
          <div class="password-toggle" @click="showPassword = !showPassword">
            {{ showPassword ? '👁️' : '👁️‍🗨️' }}
          </div>
        </div>

        <AlertBox
        v-model:visible="showAlert"
        :title="alertConfig.title"
        :content="alertConfig.content"
        :html-content="alertConfig.htmlContent"
        :confirm-text="alertConfig.confirmText"
        @confirm="handleAlertConfirm"
      />
      

        <!-- 忘记密码 -->
        <div class="form-options">
          <button type="button" class="forgot-btn" @click="handleForgotPassword">
            忘记密码？
          </button>
        </div>

        <!-- 登录按钮 -->
        <button type="submit" class="login-btn" :disabled="loading">
          {{ loading ? '登录中...' : '登录' }}
        </button>

        <!-- 注册入口 -->
        <div class="register-link">
          <span>还没有账号？</span>
          <button type="button" class="register-btn" @click="goToRegister">
            立即注册
          </button>
        </div>
      </form>
    </div>
  </div>
</template>

<style scoped>
.login-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #62055f 0%, #8b1a86 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}

.login-container {
  width: 100%;
  max-width: 420px;
  background: white;
  border-radius: 24px;
  padding: 32px 28px 40px;
  position: relative;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.15);
  animation: fadeInUp 0.5s ease-out;
}

/* 返回按钮 */
.back-btn {
  position: absolute;
  top: 20px;
  left: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  border-radius: 50%;
  transition: all 0.2s ease;
}



.back-icon {
  font-size: 12px;
  border: none;
  background-color: transparent;
  color: #62055f;
}

/* 头部区域 */
.login-header {
  text-align: center;
  margin-bottom: 40px;
}

.logo-wrapper {
  display: flex;
  justify-content: center;
  margin-bottom: 20px;
}

.logo {
  width: 70px;
  height: 70px;
  background: linear-gradient(135deg, #62055f 0%, #8b1a86 100%);
  border-radius: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 36px;
  box-shadow: 0 8px 16px rgba(98, 5, 95, 0.2);
}

.login-title {
  font-size: 28px;
  font-weight: 600;
  color: #62055f;
  margin: 0 0 8px 0;
}

.login-subtitle {
  font-size: 14px;
  color: #888;
  margin: 0;
}

/* 表单样式 */
.login-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.form-group {
  position: relative;
  display: flex;
  align-items: center;
  border: 1px solid #e5e4e7;
  border-radius: 12px;
  transition: all 0.2s ease;
  background: white;
}

.form-group:focus-within {
  border-color: #62055f;
  box-shadow: 0 0 0 3px rgba(98, 5, 95, 0.1);
}

.input-icon {
  padding: 14px 0 14px 16px;
  font-size: 18px;
  color: #999;
}

.form-input {
  flex: 1;
  padding: 14px 12px 14px 8px;
  border: none;
  outline: none;
  color: #000;
  font-size: 16px;
  background: transparent;
}

.form-input::placeholder {
  color: #bbb;
}

.password-toggle {
  padding: 14px 16px;
  cursor: pointer;
  font-size: 18px;
  color: #999;
  user-select: none;
}

/* 表单选项 */
.form-options {
  text-align: right;
  margin-top: -8px;
}

.forgot-btn {
  background: none;
  border: none;
  color: #62055f;
  font-size: 13px;
  cursor: pointer;
  text-decoration: none;
}

.forgot-btn:hover {
  text-decoration: underline;
}

/* 登录按钮 */
.login-btn {
  background: #62055f;
  color: white;
  border: none;
  border-radius: 12px;
  padding: 14px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
  margin-top: 8px;
}

.login-btn:hover:not(:disabled) {
  background: #7a0e76;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(98, 5, 95, 0.3);
}

.login-btn:active:not(:disabled) {
  transform: translateY(0);
}

.login-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* 注册入口 */
.register-link {
  text-align: center;
  font-size: 14px;
  color: #888;
  margin-top: 8px;
}

.register-btn {
  background: none;
  border: none;
  color: #62055f;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  margin-left: 6px;
}

.register-btn:hover {
  text-decoration: underline;
}

/* 动画 */
@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 响应式 */
@media (max-width: 480px) {
  .login-container {
    padding: 28px 20px 36px;
  }

  .login-title {
    font-size: 24px;
  }

  .form-input {
    font-size: 14px;
  }
}
</style>