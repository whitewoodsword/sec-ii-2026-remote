# Frontend 项目细节

##  项目概述

本项目是一个基于 **Vue 3** 和 **Vite** 构建的现代化前端应用，采用最新的 Composition API 和 `<script setup>` 语法糖，提供了快速、高效的开发体验。

### 技术栈

- **核心框架**: Vue 3.5.32+
- **构建工具**: Vite 8.0.4+
- **代码检查**: ESLint 10.2.0+
- **CSS**: 原生 CSS（支持深色模式）
- **包管理器**: npm

---

##  项目结构
````
frontend/ 前端根目录
├── src/ 
│ ├── assets/ # 静态资源目录 
│ │ ├── vite.svg # Vite Logo 
│ │ ├── vue.svg # Vue Logo 
│ │ └── hero.png # 主视觉图片 
│ ├── components/ # Vue 组件目录 
│ │ └── HelloWorld.vue # 示例组件 
│ ├── App.vue # 根组件 
│ ├── main.js # 应用入口文件 
│ └── style.css # 全局样式文件 
├── public/ # 公共资源目录 
│ ├── favicon.svg # 网站图标 
│ └── icons.svg # SVG 图标集 
├── index.html # HTML 入口文件 
├── package.json # 项目配置和依赖 
├── vite.config.js # Vite 配置文件 
├── eslint.config.js # ESLint 配置文件 
└── README.md # 项目说明文档
````


---

##  核心技术详解

### 1. Vue 3 Composition API

项目采用 Vue 3 的 **Composition API** 和 **`<script setup>`** 语法糖，提供更简洁的代码组织和更好的 TypeScript 支持。

**示例 - HelloWorld.vue:**
```vue
<script setup>
import { ref } from 'vue'
import viteLogo from '../assets/vite.svg'
import heroImg from '../assets/hero.png'
import vueLogo from '../assets/vue.svg'

const count = ref(0)
</script>
````

### 2. Vite 构建系统
使用 Vite 作为构建工具，提供极速的开发体验和优化的生产构建。

vite.config.js 配置:


````
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
})
````
----

### 开发指南

**环境要求**:
- Node.js: >= 18.0.0 
- npm: >= 9.0.0 
- 浏览器: 支持 ES Modules 的现代浏览器

**安装依赖**
````bash
cd frontend
npm install
````
**开发命令**

````bash
npm run dev
````
默认端口: http://localhost:5173


**构建生产版本**
````bash
npm run build
````
**检查代码**
````bash
npm run lint
````

**自动修复问题**
````bash
npm run lint:fix
````


### 生产依赖

vue@^3.5.32: Vue 3 核心框架
### 开发依赖:

- vite@^8.0.4: 构建工具 
- @vitejs/plugin-vue@^6.0.5: Vue 单文件组件支持 
- eslint@^10.2.0: 代码检查工具 
- eslint-plugin-vue@^10.8.0: Vue ESLint 插件 
- @eslint/js@^10.0.1: JavaScript ESLint 规则 
- @eslint/json@^1.2.0: JSON 文件检查 
- globals@^17.5.0: 全局变量定义



> 最后更新: 2026-04-21
