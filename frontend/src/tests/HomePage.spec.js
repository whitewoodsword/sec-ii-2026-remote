import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import HomePage from '../components/HomePage.vue'
import { useAuthStore } from '../stores/auth'

// Mock 路由
const mockRouterPush = vi.fn()
vi.mock('vue-router', () => ({
    useRouter: () => ({
        push: mockRouterPush
    })
}))

describe('HomePage', () => {
    let wrapper

    beforeEach(() => {
        // 为每个测试创建独立的 Pinia 实例
        setActivePinia(createPinia())

        wrapper = mount(HomePage, {
            global: {
                stubs: {
                    // 如果 AlertBox 组件有复杂逻辑，可以 stubbing 掉
                    AlertBox: true
                }
            }
        })
    })

    it('renders the header headline correctly', () => {
        expect(wrapper.text()).toContain('校园互助服务平台')
    })

    it('renders category filter buttons', () => {
        const categories = ['全部', '快递代取', '学习辅导', '二手交易', '活动组队', '其他']
        categories.forEach(cat => {
            expect(wrapper.text()).toContain(cat)
        })
    })

    it('shows login button when user is not logged in', () => {
        const authStore = useAuthStore()
        authStore.logout()

        // 重新挂载以反映状态变化
        wrapper = mount(HomePage, {
            global: { stubs: { AlertBox: true } }
        })

        expect(wrapper.text()).toContain('登录 / 注册')
        expect(wrapper.find('.avatar').exists()).toBe(false)
    })
})

describe('HomePage - User Interactions', () => {
    let wrapper

    beforeEach(() => {
        setActivePinia(createPinia())
        wrapper = mount(HomePage, {
            global: { stubs: { AlertBox: true } }
        })
    })

    it('navigates to publish page when publish button is clicked', async () => {
        const publishBtn = wrapper.find('.publish-btn')
        await publishBtn.trigger('click')

        expect(mockRouterPush).toHaveBeenCalledWith('/create/demand')
    })

    it('filters demands when a category is selected', async () => {
        // 初始状态，"全部" 应该是 active
        const allCategoryBtn = wrapper.findAll('.category-btn')[0]
        expect(allCategoryBtn.classes()).toContain('active')

        // 点击 "学习辅导" 分类
        const studyBtn = wrapper.findAll('.category-btn').find(
            btn => btn.text() === '学习辅导'
        )
        await studyBtn.trigger('click')

        // 验证 active 类转移到了学习辅导按钮
        expect(studyBtn.classes()).toContain('active')
    })

    it('clears search when clear button is clicked', async () => {
        const searchInput = wrapper.find('.search-box input')
        await searchInput.setValue('test keyword')

        // 验证清除按钮出现
        const clearBtn = wrapper.find('.search-clear')
        expect(clearBtn.exists()).toBe(true)

        await clearBtn.trigger('click')
        expect(searchInput.element.value).toBe('')
    })
})


describe('HomePage - Logged In User', () => {
    let wrapper
    let authStore

    beforeEach(() => {
        setActivePinia(createPinia())
        authStore = useAuthStore()

        const user = {
            id: 1,
            name: '测试用户',
            avatarPath: '/avatars/test.png',
            isAdmin: false
        }

        authStore.setAuth({"user": user, "token": 111111})

        wrapper = mount(HomePage, {
            global: { stubs: { AlertBox: true } }
        })
    })

    it('shows user info and avatar when logged in', () => {
        expect(wrapper.text()).toContain('测试用户')
        expect(wrapper.find('.avatar').exists()).toBe(true)
        expect(wrapper.find('.header-btn').exists()).toBe(false)
    })

    it('toggles dropdown menu when avatar is clicked', async () => {
        const avatar = wrapper.find('.avatar')

        // 初始下拉菜单不可见
        expect(wrapper.find('.dropdown-menu').exists()).toBe(false)

        await avatar.trigger('click')

        // 点击后下拉菜单出现
        expect(wrapper.find('.dropdown-menu').exists()).toBe(true)
        expect(wrapper.text()).toContain('个人主页')
        expect(wrapper.text()).toContain('消息')
        expect(wrapper.text()).toContain('退出登录')
    })

    it('navigates to profile when dropdown item is clicked', async () => {
        // 先打开下拉菜单
        await wrapper.find('.avatar').trigger('click')

        // 点击个人主页
        const profileBtn = wrapper.findAll('.dropdown-item').find(
            btn => btn.text() === '个人主页'
        )
        await profileBtn.trigger('click')

        expect(mockRouterPush).toHaveBeenCalledWith('/my/profile')
    })
})
