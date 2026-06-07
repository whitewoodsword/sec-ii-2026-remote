<template>
  <div class="welcome-page">
    <div class="content">
      <h1>欢迎使用校园互助服务平台</h1>
    </div>

    <div class="button-wrapper">
      <button class="start-button" @click="goToHomePage">
        开始使用 →
      </button>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()

const goToHomePage = () => {
  router.push('/')
}
</script>

<style scoped>
.welcome-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  position: relative;
}

.content {
  text-align: center;
  color: white;
  margin-bottom: 80px;
  padding: 0 20px;
}

.content h1 {
  font-size: 52px;
  font-weight: 600;
  margin-bottom: 16px;
  letter-spacing: -0.5px;
  line-height: 1.2;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.content p {
  font-size: 18px;
  opacity: 0.95;
  font-weight: 300;
  letter-spacing: 0.3px;
}

.button-wrapper {
  position: absolute;
  bottom: 20%;
}

.start-button {
  padding: 16px 48px;
  font-size: 16px;
  font-weight: 500;
  color: #667eea;
  background: white;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1), 0 1px 3px rgba(0, 0, 0, 0.08);
  letter-spacing: 0.5px;
}

.start-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 10px 20px rgba(0, 0, 0, 0.15), 0 3px 6px rgba(0, 0, 0, 0.1);
}

.start-button:active {
  transform: translateY(0);
}

@media (max-width: 768px) {
  .content h1 {
    font-size: 36px;
  }
  
  .content p {
    font-size: 16px;
  }
  
  .start-button {
    padding: 14px 36px;
    font-size: 15px;
  }
}
</style>