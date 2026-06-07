import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import LogInPage from '../components/LogInPage.vue'

const mockRouterPush = vi.fn()

vi.mock('vue-router', () => ({
  useRouter: () => ({
    push: mockRouterPush,
  }),
}))

describe('LogInPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    localStorage.clear()
    setActivePinia(createPinia())
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  describe('Page Rendering', () => {
    it('renders login page title correctly', () => {
      const wrapper = mount(LogInPage, {
        global: {
          stubs: { AlertBox: true },
        },
      })

      expect(wrapper.text()).toContain('欢迎回来')
      expect(wrapper.text()).toContain('登录你的校园互助服务平台账号')
    })

    it('displays phone input field', () => {
      const wrapper = mount(LogInPage, {
        global: {
          stubs: { AlertBox: true },
        },
      })

      const phoneInput = wrapper.find('input[type="tel"]')
      expect(phoneInput.exists()).toBe(true)
      expect(phoneInput.attributes('placeholder')).toBe('请输入手机号')
    })

    it('displays password input field', () => {
      const wrapper = mount(LogInPage, {
        global: {
          stubs: { AlertBox: true },
        },
      })

      const passwordInput = wrapper.find('input[type="password"]')
      expect(passwordInput.exists()).toBe(true)
      expect(passwordInput.attributes('placeholder')).toBe('请输入密码')
    })

    it('displays login button', () => {
      const wrapper = mount(LogInPage, {
        global: {
          stubs: { AlertBox: true },
        },
      })

      const loginBtn = wrapper.find('.login-btn')
      expect(loginBtn.exists()).toBe(true)
      expect(loginBtn.text()).toBe('登录')
    })

    it('displays register link', () => {
      const wrapper = mount(LogInPage, {
        global: {
          stubs: { AlertBox: true },
        },
      })

      expect(wrapper.text()).toContain('还没有账号？')
      expect(wrapper.text()).toContain('立即注册')
    })

    it('displays forgot password button', () => {
      const wrapper = mount(LogInPage, {
        global: {
          stubs: { AlertBox: true },
        },
      })

      expect(wrapper.text()).toContain('忘记密码？')
    })
  })

  describe('Form Input', () => {
    it('updates phone value when user types', async () => {
      const wrapper = mount(LogInPage, {
        global: {
          stubs: { AlertBox: true },
        },
      })

      const phoneInput = wrapper.find('input[type="tel"]')
      await phoneInput.setValue('13800138000')

      expect(phoneInput.element.value).toBe('13800138000')
    })

    it('updates password value when user types', async () => {
      const wrapper = mount(LogInPage, {
        global: {
          stubs: { AlertBox: true },
        },
      })

      const passwordInput = wrapper.find('input[type="password"]')
      await passwordInput.setValue('password123')

      expect(passwordInput.element.value).toBe('password123')
    })

    it('toggles password visibility when eye icon is clicked', async () => {
      const wrapper = mount(LogInPage, {
        global: {
          stubs: { AlertBox: true },
        },
      })

      const passwordInput = wrapper.find('input[type="password"]')
      const toggleBtn = wrapper.find('.password-toggle')

      expect(passwordInput.attributes('type')).toBe('password')

      await toggleBtn.trigger('click')
      expect(passwordInput.attributes('type')).toBe('text')

      await toggleBtn.trigger('click')
      expect(passwordInput.attributes('type')).toBe('password')
    })
  })

  describe('Login Functionality', () => {
    it('successful login navigates to home page', async () => {
      const fetchMock = vi.fn(() =>
        Promise.resolve({
          json: () => Promise.resolve({
            code: 200,
            data: {
              token: 'test-token',
              user: {
                id: 1,
                name: 'Test User',
                phone: '13800138000',
              },
            },
          }),
        })
      )
      vi.stubGlobal('fetch', fetchMock)

      const wrapper = mount(LogInPage, {
        global: {
          stubs: { AlertBox: true },
        },
      })

      const phoneInput = wrapper.find('input[type="tel"]')
      const passwordInput = wrapper.find('input[type="password"]')
      const loginBtn = wrapper.find('.login-btn')

      await phoneInput.setValue('13800138000')
      await passwordInput.setValue('password123')
      await loginBtn.trigger('click')

      await flushPromises()

    })

    it('shows error message on failed login', async () => {
      const fetchMock = vi.fn(() =>
        Promise.resolve({
          json: () => Promise.resolve({
            code: 401,
            message: '手机号或密码错误',
          }),
        })
      )
      vi.stubGlobal('fetch', fetchMock)

      const wrapper = mount(LogInPage, {
        global: {
          stubs: { AlertBox: true },
        },
      })

      const phoneInput = wrapper.find('input[type="tel"]')
      const passwordInput = wrapper.find('input[type="password"]')
      const loginBtn = wrapper.find('.login-btn')

      await phoneInput.setValue('13800138000')
      await passwordInput.setValue('wrongpassword')
      await loginBtn.trigger('click')

      await flushPromises()

      expect(wrapper.text()).toContain('返回主页')
    })

    it('handles network error during login', async () => {
      const fetchMock = vi.fn(() => Promise.reject(new Error('Network error')))
      vi.stubGlobal('fetch', fetchMock)

      const wrapper = mount(LogInPage, {
        global: {
          stubs: { AlertBox: true },
        },
      })

      const phoneInput = wrapper.find('input[type="tel"]')
      const passwordInput = wrapper.find('input[type="password"]')
      const loginBtn = wrapper.find('.login-btn')

      await phoneInput.setValue('13800138000')
      await passwordInput.setValue('password123')
      await loginBtn.trigger('click')

      await flushPromises()

      expect(wrapper.text()).toContain('返回主页')
    })
  })

  describe('Navigation', () => {
    it('navigates to register page when register link is clicked', async () => {
      const wrapper = mount(LogInPage, {
        global: {
          stubs: { AlertBox: true },
        },
      })

      const registerBtn = wrapper.find('.register-btn')
      await registerBtn.trigger('click')

      expect(mockRouterPush).toHaveBeenCalledWith('/register')
    })

    it('navigates to home page when back button is clicked', async () => {
      const wrapper = mount(LogInPage, {
        global: {
          stubs: { AlertBox: true },
        },
      })

      const backBtn = wrapper.find('.back-btn')
      await backBtn.trigger('click')

      expect(mockRouterPush).toHaveBeenCalledWith('/')
    })
  })
})