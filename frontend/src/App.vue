<!-- src/App.vue -->
<template>
  <div id="app">

    <router-view />
      
    <!-- 可选：页脚 -->
    <footer>
      <p>校园互助服务平台 (CampusHub)© </p>
      <p>Group 67 For SEII2026版权所有</p>
    </footer>
  </div>
</template>

<script setup>
// 可以监听路由变化
import { watch } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
watch(() => route.path, (newPath) => {
  console.log('路由变化:', newPath)
})
</script>

<style>
/* 全局样式 */
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
   user-select: none;  /* 标准语法 */
  -webkit-user-select: none;  /* Safari */
  -moz-user-select: none;  /* Firefox */
  -ms-user-select: none;  /* IE/Edge */
}

#app {
  font-family: Avenir, Helvetica, Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  color: #2c3e50;
}

nav {
  padding: 20px;
  text-align: center;
  background-color: #f8f9fa;
  margin-bottom: 20px;
}

nav a {
  margin: 0 10px;
  text-decoration: none;
  color: #42b983;
  font-weight: bold;
}

nav a.router-link-exact-active {
  color: #2c3e50;
}

</style>
