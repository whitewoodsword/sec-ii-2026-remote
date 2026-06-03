import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import MyConversationsPage from '../components/MyConversationsPage.vue'
import { useAuthStore } from '../stores/auth'

// Mock useRouter
const mockRouterPush = vi.fn()
const mockRouterBack = vi.fn()

vi.mock('vue-router', () => ({
    useRouter: () => ({
        push: mockRouterPush,
        back: mockRouterBack
    })
}))

// Mock 全局 fetch
const mockFetch = vi.fn()
// 使用 vi.stubGlobal 或者 globalThis
globalThis.fetch = mockFetch

// 辅助函数：创建已登录的 Pinia
function setupLoggedInStore() {
    const pinia = createPinia()
    setActivePinia(pinia)
    const authStore = useAuthStore()
    authStore.setAuth({
        user: { id: 1, name: '测试用户', avatarPath: '/avatars/test.png', isAdmin: false },
        token: 'test-token-123'
    })
    return { pinia, authStore }
}

// 辅助函数：模拟对话列表接口返回
function mockConversationsResponse(conversations = []) {
    return {
        code: 200,
        data: {
            content: conversations,
            totalElements: conversations.length,
            totalPages: 1,
            currentPage: 0,
            pageSize: 20
        }
    }
}

// 辅助函数：模拟对话详情接口返回
function mockConversationDetailResponse(messages = []) {
    return {
        code: 200,
        data: {
            conversationId: 1,
            otherUser: {
                id: 2,
                name: '测试对方',
                avatarPath: null
            },
            messages: messages,
            unreadCount: 0
        }
    }
}

describe('MyConversationsPage - 渲染测试', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        setupLoggedInStore()

        // 默认 fetch 返回空对话列表
        mockFetch.mockResolvedValue({
            ok: true,
            json: () => Promise.resolve(mockConversationsResponse([]))
        })
    })

    it('页面标题渲染正确', async () => {
        const wrapper = mount(MyConversationsPage, {
            global: {
                stubs: { AlertBox: true }
            }
        })
        
        await new Promise(r => setTimeout(r, 50))
        await wrapper.vm.$nextTick()
        
        expect(wrapper.text()).toContain('消息')
    })

    it('返回按钮存在', async () => {
        const wrapper = mount(MyConversationsPage, {
            global: {
                stubs: { AlertBox: true }
            }
        })
        
        await new Promise(r => setTimeout(r, 50))
        await wrapper.vm.$nextTick()
        
        expect(wrapper.find('.back-btn').exists()).toBe(true)
        expect(wrapper.text()).toContain('返回')
    })

    it('对话列表为空时显示空状态提示', async () => {
        const wrapper = mount(MyConversationsPage, {
            global: {
                stubs: { AlertBox: true }
            }
        })

        await new Promise(r => setTimeout(r, 100))
        await wrapper.vm.$nextTick()

        expect(wrapper.text()).toContain('暂无对话')
    })

    it('未选择对话时右侧显示空聊天区域', async () => {
        const wrapper = mount(MyConversationsPage, {
            global: {
                stubs: { AlertBox: true }
            }
        })
        
        await new Promise(r => setTimeout(r, 50))
        await wrapper.vm.$nextTick()
        
        expect(wrapper.text()).toContain('选择对话开始聊天')
    })

    it('未选择对话时输入区域不渲染', async () => {
        const wrapper = mount(MyConversationsPage, {
            global: {
                stubs: { AlertBox: true }
            }
        })

        await new Promise(r => setTimeout(r, 50))
        await wrapper.vm.$nextTick()
        
        expect(wrapper.find('.message-input').exists()).toBe(false)
        expect(wrapper.find('.send-btn').exists()).toBe(false)
    })
})

describe('MyConversationsPage - 对话列表', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        setupLoggedInStore()
    })

    it('有对话时渲染对话列表项', async () => {
        const conversations = [
            {
                id: 1,
                conversationId: 1,
                otherUser: {
                    id: 2,
                    name: '张三',
                    avatarPath: '/avatars/zhang.jpg'
                },
                lastMessage: {
                    id: 100,
                    content: '你好，请问这个需求还有效吗？',
                    createdAt: new Date().toISOString(),
                    senderId: 2,
                    receiverId: 1
                },
                unreadCount: 2
            }
        ]

        mockFetch.mockResolvedValue({
            ok: true,
            json: () => Promise.resolve(mockConversationsResponse(conversations))
        })

        const wrapper = mount(MyConversationsPage, {
            global: {
                stubs: { AlertBox: true }
            }
        })

        await new Promise(r => setTimeout(r, 100))
        await wrapper.vm.$nextTick()

        expect(wrapper.text()).toContain('张三')
        expect(wrapper.text()).toContain('你好，请问这个需求还有效吗？')
    })

    it('未读消息数 > 0 时显示未读红点', async () => {
        const conversations = [
            {
                id: 1,
                conversationId: 1,
                otherUser: { id: 2, name: '李四', avatarPath: null },
                lastMessage: { 
                    id: 100,
                    content: '好的', 
                    createdAt: new Date().toISOString(),
                    senderId: 2,
                    receiverId: 1
                },
                unreadCount: 99
            }
        ]

        mockFetch.mockResolvedValue({
            ok: true,
            json: () => Promise.resolve(mockConversationsResponse(conversations))
        })

        const wrapper = mount(MyConversationsPage, {
            global: {
                stubs: { AlertBox: true }
            }
        })

        await new Promise(r => setTimeout(r, 100))
        await wrapper.vm.$nextTick()

        const badge = wrapper.find('.unread-badge')
        expect(badge.exists()).toBe(true)
        expect(badge.text()).toBe('99')
    })

    it('未读数为 0 时不显示未读红点', async () => {
        const conversations = [
            {
                id: 1,
                conversationId: 1,
                otherUser: { id: 2, name: '王五', avatarPath: null },
                lastMessage: { 
                    id: 100,
                    content: '谢谢！', 
                    createdAt: new Date().toISOString(),
                    senderId: 2,
                    receiverId: 1
                },
                unreadCount: 0
            }
        ]

        mockFetch.mockResolvedValue({
            ok: true,
            json: () => Promise.resolve(mockConversationsResponse(conversations))
        })

        const wrapper = mount(MyConversationsPage, {
            global: {
                stubs: { AlertBox: true }
            }
        })

        await new Promise(r => setTimeout(r, 100))
        await wrapper.vm.$nextTick()

        expect(wrapper.find('.unread-badge').exists()).toBe(false)
    })
})

describe('MyConversationsPage - 交互测试', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        setupLoggedInStore()
    })

    it('点击返回按钮调用 router.back()', async () => {
        mockFetch.mockResolvedValue({
            ok: true,
            json: () => Promise.resolve(mockConversationsResponse([]))
        })

        const wrapper = mount(MyConversationsPage, {
            global: {
                stubs: { AlertBox: true }
            }
        })

        await new Promise(r => setTimeout(r, 50))
        await wrapper.vm.$nextTick()

        const backBtn = wrapper.find('.back-btn')
        await backBtn.trigger('click')

        expect(mockRouterBack).toHaveBeenCalled()
    })

    it('选择对话后显示聊天头部', async () => {
        const conversations = [
            {
                id: 1,
                conversationId: 1,
                otherUser: { id: 2, name: '赵六', avatarPath: null },
                lastMessage: { 
                    id: 100,
                    content: '嗨', 
                    createdAt: new Date().toISOString(),
                    senderId: 2,
                    receiverId: 1
                },
                unreadCount: 0
            }
        ]

        const messages = [
            { id: 1, senderId: 2, receiverId: 1, content: '嗨', createdAt: new Date().toISOString(), isRead: false }
        ]

        mockFetch
            // 第一次调用：fetchConversations 返回对话列表
            .mockResolvedValueOnce({
                ok: true,
                json: () => Promise.resolve(mockConversationsResponse(conversations))
            })
            // 第二次调用：loadMessages 调用对话详情接口
            .mockResolvedValueOnce({
                ok: true,
                json: () => Promise.resolve(mockConversationDetailResponse(messages))
            })
            // 第三次调用：markMessagesAsRead 中的 fetchConversations
            .mockResolvedValueOnce({
                ok: true,
                json: () => Promise.resolve(mockConversationsResponse(conversations))
            })

        const wrapper = mount(MyConversationsPage, {
            global: {
                stubs: { AlertBox: true }
            }
        })

        await new Promise(r => setTimeout(r, 100))
        await wrapper.vm.$nextTick()

        // 点击对话项
        const convItem = wrapper.find('.conversation-item')
        await convItem.trigger('click')
        await wrapper.vm.$nextTick()
        await new Promise(r => setTimeout(r, 50))

        // 聊天头部应显示对方用户名
        expect(wrapper.text()).toContain('赵六')
    })

    it('发送按钮存在且初始非禁用', async () => {
        const conversations = [
            {
                id: 1,
                conversationId: 1,
                otherUser: { id: 2, name: '钱七', avatarPath: null },
                lastMessage: { 
                    id: 100,
                    content: '测试', 
                    createdAt: new Date().toISOString(),
                    senderId: 2,
                    receiverId: 1
                },
                unreadCount: 0
            }
        ]

        mockFetch
            .mockResolvedValueOnce({
                ok: true,
                json: () => Promise.resolve(mockConversationsResponse(conversations))
            })
            .mockResolvedValueOnce({
                ok: true,
                json: () => Promise.resolve(mockConversationDetailResponse([]))
            })
            .mockResolvedValueOnce({
                ok: true,
                json: () => Promise.resolve(mockConversationsResponse(conversations))
            })

        const wrapper = mount(MyConversationsPage, {
            global: {
                stubs: { AlertBox: true }
            }
        })

        await new Promise(r => setTimeout(r, 100))
        await wrapper.vm.$nextTick()

        const convItem = wrapper.find('.conversation-item')
        await convItem.trigger('click')
        await wrapper.vm.$nextTick()
        await new Promise(r => setTimeout(r, 50))

        const sendBtn = wrapper.find('.send-btn')
        expect(sendBtn.exists()).toBe(true)
        expect(sendBtn.text()).toBe('发送')
    })

    it('选择对话后消息输入框可见', async () => {
        const conversations = [
            {
                id: 1,
                conversationId: 1,
                otherUser: { id: 2, name: '用户A', avatarPath: null },
                lastMessage: { 
                    id: 100,
                    content: '你好', 
                    createdAt: new Date().toISOString(),
                    senderId: 2,
                    receiverId: 1
                },
                unreadCount: 0
            }
        ]

        mockFetch
            .mockResolvedValueOnce({
                ok: true,
                json: () => Promise.resolve(mockConversationsResponse(conversations))
            })
            .mockResolvedValueOnce({
                ok: true,
                json: () => Promise.resolve(mockConversationDetailResponse([]))
            })
            .mockResolvedValueOnce({
                ok: true,
                json: () => Promise.resolve(mockConversationsResponse(conversations))
            })

        const wrapper = mount(MyConversationsPage, {
            global: {
                stubs: { AlertBox: true }
            }
        })

        await new Promise(r => setTimeout(r, 100))
        await wrapper.vm.$nextTick()

        const convItem = wrapper.find('.conversation-item')
        await convItem.trigger('click')
        await wrapper.vm.$nextTick()
        await new Promise(r => setTimeout(r, 50))

        expect(wrapper.find('.message-input').exists()).toBe(true)
        expect(wrapper.find('.send-btn').exists()).toBe(true)
    })
})

describe('MyConversationsPage - 加载状态', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        setupLoggedInStore()
    })

    it('初始加载时显示加载中提示', async () => {
        // fetch 不立即 resolve，让组件保持在 loading 状态
        let resolveFetch
        mockFetch.mockReturnValue(new Promise(resolve => {
            resolveFetch = resolve
        }))

        const wrapper = mount(MyConversationsPage, {
            global: {
                stubs: { AlertBox: true }
            }
        })

        await wrapper.vm.$nextTick()

        // 应该有 loading 状态
        expect(wrapper.text()).toContain('加载对话中')

        // 完成加载
        resolveFetch({
            ok: true,
            json: () => Promise.resolve(mockConversationsResponse([]))
        })
        await new Promise(r => setTimeout(r, 100))
        await wrapper.vm.$nextTick()

        // 加载完成后不应再显示 loading
        expect(wrapper.text()).not.toContain('加载对话中')
    })
})

describe('MyConversationsPage - 发送消息', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        setupLoggedInStore()
    })

    it('输入消息后点击发送按钮能发送消息', async () => {
        const conversations = [
            {
                id: 1,
                conversationId: 1,
                otherUser: { id: 2, name: '测试用户', avatarPath: null },
                lastMessage: { 
                    id: 100,
                    content: '你好', 
                    createdAt: new Date().toISOString(),
                    senderId: 2,
                    receiverId: 1
                },
                unreadCount: 0
            }
        ]

        const initialMessages = [
            { id: 1, senderId: 2, receiverId: 1, content: '你好', createdAt: new Date().toISOString(), isRead: false }
        ]

        let callCount = 0
        mockFetch.mockImplementation(() => {
            callCount++
            if (callCount === 1) {
                return Promise.resolve({
                    ok: true,
                    json: () => Promise.resolve(mockConversationsResponse(conversations))
                })
            } else if (callCount === 2) {
                return Promise.resolve({
                    ok: true,
                    json: () => Promise.resolve(mockConversationDetailResponse(initialMessages))
                })
            } else if (callCount === 3) {
                return Promise.resolve({
                    ok: true,
                    json: () => Promise.resolve(mockConversationsResponse(conversations))
                })
            } else if (callCount === 4) {
                // 发送消息的 POST 请求
                return Promise.resolve({
                    ok: true,
                    json: () => Promise.resolve({
                        code: 200,
                        data: {
                            id: Date.now(),
                            senderId: 1,
                            receiverId: 2,
                            content: '测试消息',
                            createdAt: new Date().toISOString(),
                            isRead: false
                        }
                    })
                })
            } else if (callCount === 5) {
                // 发送后重新加载对话列表
                return Promise.resolve({
                    ok: true,
                    json: () => Promise.resolve(mockConversationsResponse(conversations))
                })
            } else if (callCount === 6) {
                // 发送后重新加载消息
                return Promise.resolve({
                    ok: true,
                    json: () => Promise.resolve(mockConversationDetailResponse([
                        ...initialMessages,
                        { id: 2, senderId: 1, receiverId: 2, content: '测试消息', createdAt: new Date().toISOString(), isRead: false }
                    ]))
                })
            } else {
                return Promise.resolve({
                    ok: true,
                    json: () => Promise.resolve(mockConversationsResponse(conversations))
                })
            }
        })

        const wrapper = mount(MyConversationsPage, {
            global: {
                stubs: { AlertBox: true }
            }
        })

        await new Promise(r => setTimeout(r, 100))
        await wrapper.vm.$nextTick()

        // 选择对话
        const convItem = wrapper.find('.conversation-item')
        await convItem.trigger('click')
        await wrapper.vm.$nextTick()
        await new Promise(r => setTimeout(r, 100))

        // 输入消息
        const textarea = wrapper.find('.message-input')
        await textarea.setValue('测试消息')
        await wrapper.vm.$nextTick()

        // 点击发送
        const sendBtn = wrapper.find('.send-btn')
        await sendBtn.trigger('click')
        await wrapper.vm.$nextTick()
        await new Promise(r => setTimeout(r, 100))

        // 验证发送请求被调用
        expect(mockFetch).toHaveBeenCalledWith(
            expect.stringContaining('/messages/send'),
            expect.objectContaining({
                method: 'POST',
                body: expect.stringContaining('测试消息')
            })
        )
    })
})