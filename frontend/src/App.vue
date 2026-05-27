<script setup>
import { watch } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()

watch(() => route.path, (newPath) => {
  console.log('route changed:', newPath)
})
</script>

<template>
  <div id="app">
    <router-view />

    <footer class="app-footer">
      <p>校园互助服务平台 CampusHub</p>
      <p>Group 67 for SEII 2026</p>
    </footer>
  </div>
</template>

<style scoped>
#app {
  min-height: 100vh;
}

.app-footer {
  text-align: center;
  padding: 32px 20px;
  background: #62055f;
  color: #d4d4d8;
  line-height: 1.7;
  font-size: 14px;
}

.app-footer p {
  margin: 0;
}
</style>
