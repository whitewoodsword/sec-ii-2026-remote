<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import AlertBox from './SmallComponents/AlertBox.vue'

const router = useRouter()

// 表单数据
const formData = ref({
  phone: '',
  verificationCode: '',
  password: '',
  confirmPassword: ''
})

// UI 状态
const loading = ref(false)
const showPassword = ref(false)
const showConfirmPassword = ref(false)
const countdown = ref(0)

// AlertBox 状态
const showAlert = ref(false)
const alertConfig = ref({
  title: '系统通知',
  content: '',
  htmlContent: '',
  confirmText: '确定'
})

// 显示通知的方法
const showNotification = (title, content, isHtml = false) => {
  alertConfig.value = {
    title: title,
    content: isHtml ? '' : content,
    htmlContent: isHtml ? content : '',
    confirmText: '确定'
  }
  showAlert.value = true
}

// 通知确认后的回调
const handleAlertConfirm = () => {
  console.log('通知框已关闭')
  router.push('/')
  // 如果是注册成功的通知，关闭后跳转
}

// 获取验证码
const getVerificationCode = () => {
  showNotification('ERROR:(', '由于通信商条款限制，禁止使用手机号码进行验证码发送，请在验证码栏直接键入任何字符！')
}

// 处理注册
const handleRegister = () => {
  // 你的注册逻辑
  if(formData.value.password !== formData.value.confirmPassword){
    alert('密码和确认密码不一致！')
    formData.value.password = ''
    formData.value.confirmPassword = ''
    return
  }
  fetch('http://localhost:8080/users/register', {
    method: 'POST',
    body: JSON.stringify({
      phone: formData.value.phone,
      password: formData.value.password
    }),
    headers: {
      'Content-type': 'application/json; charset=UTF-8'
    }
  }).then(response => response.json())
  .then(data => {
    console.log(data)
  })
  .catch(error => {
    alert('注册失败')
  })
  showNotification('通知', '亲爱的用户，您已经注册成功！')
}

// 跳转到登录页面
const goToLogin = () => {
  router.push('/login')
}


</script>

<template>
  <div class="register-page">
    <div class="register-container">
      <!-- 返回按钮 -->
      <div class="back-btn" @click="router.push('/')">
        <span class="back-icon">返回主页</span>
      </div>

      <!-- 头部区域 -->
      <div class="register-header">
        <h1 class="register-title">注册账号</h1>
        <p class="register-subtitle">加入校园互助服务平台</p>
      </div>

      <!-- 注册表单 -->
      <form class="register-form" @submit.prevent="handleRegister">
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

        <!-- 验证码输入框 -->
        <div class="form-group code-group">
          <div class="input-icon">验证码</div>
          <input
            type="text"
            v-model="formData.verificationCode"
            placeholder="请输入验证码（乱填就行）"
            class="form-input code-input"
            maxlength="6"
          />
          <button
            type="button"
            class="code-btn"
            :disabled="countdown > 0"
            @click="getVerificationCode"
          >
            获取验证码
          </button>
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

        <!-- 确认密码输入框 -->
        <div class="form-group">
          <div class="input-icon">确认密码</div>
          <input
            :type="showConfirmPassword ? 'text' : 'password'"
            v-model="formData.confirmPassword"
            placeholder="请再次输入密码"
            class="form-input"
          />
          <div class="password-toggle" @click="showConfirmPassword = !showConfirmPassword">
            {{ showConfirmPassword ? '👁️' : '👁️‍🗨️' }}
          </div>
        </div>

        <!-- 注册按钮 -->
        <button type="submit" class="register-btn-submit" :disabled="loading">
          {{ loading ? '注册中...' : '注册' }}
        </button>

        <!-- 登录入口 -->
        <div class="login-link">
          <span>已有账号？</span>
          <button type="button" class="login-btn-link" @click="goToLogin">
            立即登录
          </button>
        </div>
      </form>

      <AlertBox
        v-model:visible="showAlert"
        :title="alertConfig.title"
        :content="alertConfig.content"
        :html-content="alertConfig.htmlContent"
        :confirm-text="alertConfig.confirmText"
        @confirm="handleAlertConfirm"
      />
    </div>
  </div>
</template>

<style scoped>
/* 样式保持不变，你原有的样式代码... */
.register-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #62055f 0%, #8b1a86 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}

.register-container {
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
  color: #62055f;
}

/* 头部区域 */
.register-header {
  text-align: center;
  margin-bottom: 32px;
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

.register-title {
  font-size: 28px;
  font-weight: 600;
  color: #62055f;
  margin: 0 0 8px 0;
}

.register-subtitle {
  font-size: 14px;
  color: #888;
  margin: 0;
}

/* 表单样式 */
.register-form {
  display: flex;
  flex-direction: column;
  gap: 18px;
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
  font-size: 14px;
  color: #999;
  min-width: 70px;
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

/* 验证码组特殊样式 */
.code-group {
  padding-right: 8px;
}

.code-input {
  flex: 1;
}

.code-btn {
  background: none;
  border: none;
  color: #62055f;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  white-space: nowrap;
  transition: all 0.2s ease;
  padding: 0 12px;
}

.code-btn:hover:not(:disabled) {
  color: #7a0e76;
  text-decoration: underline;
}

.code-btn:disabled {
  color: #ccc;
  cursor: not-allowed;
}

/* 密码显示切换 */
.password-toggle {
  padding: 14px 16px;
  cursor: pointer;
  font-size: 18px;
  color: #999;
  user-select: none;
}

/* 注册按钮 */
.register-btn-submit {
  background: #62055f;
  color: white;
  border: none;
  border-radius: 12px;
  padding: 14px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
  margin-top: 12px;
}

.register-btn-submit:hover:not(:disabled) {
  background: #7a0e76;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(98, 5, 95, 0.3);
}

.register-btn-submit:active:not(:disabled) {
  transform: translateY(0);
}

.register-btn-submit:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* 登录入口 */
.login-link {
  text-align: center;
  font-size: 14px;
  color: #888;
  margin-top: 8px;
}

.login-btn-link {
  background: none;
  border: none;
  color: #62055f;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  margin-left: 6px;
}

.login-btn-link:hover {
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
  .register-container {
    padding: 28px 20px 36px;
  }

  .register-title {
    font-size: 24px;
  }

  .form-input {
    font-size: 14px;
  }

  .code-btn {
    font-size: 12px;
    padding: 0 8px;
  }
}
</style>