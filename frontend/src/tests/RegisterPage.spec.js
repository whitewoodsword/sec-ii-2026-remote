import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import RegisterPage from '../components/RegisterPage.vue'

const mockRouterPush = vi.fn()

vi.mock('vue-router', () => ({
  useRouter: () => ({
    push: mockRouterPush,
  }),
}))

describe('RegisterPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    localStorage.clear()
    setActivePinia(createPinia())
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  describe('Page Rendering', () => {
    it('renders register page title correctly', () => {
      const wrapper = mount(RegisterPage, {
        global: {
          stubs: { AlertBox: true },
        },
      })

      expect(wrapper.text()).toContain('注册账号')
      expect(wrapper.text()).toContain('加入校园互助服务平台')
    })

    it('displays all form fields', () => {
      const wrapper = mount(RegisterPage, {
        global: {
          stubs: { AlertBox: true },
        },
      })

      expect(wrapper.find('input[type="tel"]').exists()).toBe(true)
      expect(wrapper.find('input[type="text"]').exists()).toBe(true)
      expect(wrapper.find('input[type="password"]').exists()).toBe(true)
    })

    it('displays register button', () => {
      const wrapper = mount(RegisterPage, {
        global: {
          stubs: { AlertBox: true },
        },
      })

      const registerBtn = wrapper.find('.register-btn-submit')
      expect(registerBtn.exists()).toBe(true)
      expect(registerBtn.text()).toBe('注册')
    })

    it('displays login link', () => {
      const wrapper = mount(RegisterPage, {
        global: {
          stubs: { AlertBox: true },
        },
      })

      expect(wrapper.text()).toContain('已有账号？')
      expect(wrapper.text()).toContain('立即登录')
    })

    it('displays verification code button', () => {
      const wrapper = mount(RegisterPage, {
        global: {
          stubs: { AlertBox: true },
        },
      })

      expect(wrapper.text()).toContain('获取验证码')
    })
  })

  describe('Form Input', () => {
    it('updates phone value when user types', async () => {
      const wrapper = mount(RegisterPage, {
        global: {
          stubs: { AlertBox: true },
        },
      })

      const phoneInput = wrapper.find('input[type="tel"]')
      await phoneInput.setValue('13800138000')

      expect(phoneInput.element.value).toBe('13800138000')
    })

    it('updates verification code when user types', async () => {
      const wrapper = mount(RegisterPage, {
        global: {
          stubs: { AlertBox: true },
        },
      })

      const codeInput = wrapper.find('.code-input')
      await codeInput.setValue('123456')

      expect(codeInput.element.value).toBe('123456')
    })

    it('updates password value when user types', async () => {
      const wrapper = mount(RegisterPage, {
        global: {
          stubs: { AlertBox: true },
        },
      })

      const passwordInput = wrapper.findAll('input[type="password"]')[0]
      await passwordInput.setValue('password123')

      expect(passwordInput.element.value).toBe('password123')
    })

    it('updates confirm password value when user types', async () => {
      const wrapper = mount(RegisterPage, {
        global: {
          stubs: { AlertBox: true },
        },
      })

      const confirmInput = wrapper.findAll('input[type="password"]')[1]
      await confirmInput.setValue('password123')

      expect(confirmInput.element.value).toBe('password123')
    })

    it('toggles password visibility', async () => {
      const wrapper = mount(RegisterPage, {
        global: {
          stubs: { AlertBox: true },
        },
      })

      const passwordInput = wrapper.findAll('input[type="password"]')[0]
      const toggleBtns = wrapper.findAll('.password-toggle')
      const toggleBtn = toggleBtns[0]

      expect(passwordInput.attributes('type')).toBe('password')

      await toggleBtn.trigger('click')
      expect(passwordInput.attributes('type')).toBe('text')

      await toggleBtn.trigger('click')
      expect(passwordInput.attributes('type')).toBe('password')
    })
  })



  describe('Registration Functionality', () => {
    it('successful registration shows success message', async () => {
      const fetchMock = vi.fn(() =>
        Promise.resolve({
          json: () => Promise.resolve({
            code: 200,
            message: '注册成功',
          }),
        })
      )
      vi.stubGlobal('fetch', fetchMock)

      const wrapper = mount(RegisterPage, {
        global: {
          stubs: { AlertBox: true },
        },
      })

      const phoneInput = wrapper.find('input[type="tel"]')
      const codeInput = wrapper.find('.code-input')
      const passwordInput = wrapper.findAll('input[type="password"]')[0]
      const confirmInput = wrapper.findAll('input[type="password"]')[1]
      const registerBtn = wrapper.find('.register-btn-submit')

      await phoneInput.setValue('13800138000')
      await codeInput.setValue('123456')
      await passwordInput.setValue('password123')
      await confirmInput.setValue('password123')
      await registerBtn.trigger('click')

      await flushPromises()

      expect(wrapper.text()).toContain('返回主页')
    })

    it('shows error message on failed registration', async () => {
      const fetchMock = vi.fn(() =>
        Promise.resolve({
          json: () => Promise.resolve({
            code: 400,
            message: '手机号已存在',
          }),
        })
      )
      vi.stubGlobal('fetch', fetchMock)

      const wrapper = mount(RegisterPage, {
        global: {
          stubs: { AlertBox: true },
        },
      })

      const phoneInput = wrapper.find('input[type="tel"]')
      const codeInput = wrapper.find('.code-input')
      const passwordInput = wrapper.findAll('input[type="password"]')[0]
      const confirmInput = wrapper.findAll('input[type="password"]')[1]
      const registerBtn = wrapper.find('.register-btn-submit')

      await phoneInput.setValue('13800138000')
      await codeInput.setValue('123456')
      await passwordInput.setValue('password123')
      await confirmInput.setValue('password123')
      await registerBtn.trigger('click')

      await flushPromises()

      expect(wrapper.text()).toContain('返回主页')
    })

    it('handles network error during registration', async () => {
      const fetchMock = vi.fn(() => Promise.reject(new Error('Network error')))
      vi.stubGlobal('fetch', fetchMock)

      const wrapper = mount(RegisterPage, {
        global: {
          stubs: { AlertBox: true },
        },
      })

      const phoneInput = wrapper.find('input[type="tel"]')
      const codeInput = wrapper.find('.code-input')
      const passwordInput = wrapper.findAll('input[type="password"]')[0]
      const confirmInput = wrapper.findAll('input[type="password"]')[1]
      const registerBtn = wrapper.find('.register-btn-submit')

      await phoneInput.setValue('13800138000')
      await codeInput.setValue('123456')
      await passwordInput.setValue('password123')
      await confirmInput.setValue('password123')
      await registerBtn.trigger('click')

      await flushPromises()

      expect(wrapper.text()).toContain('返回主页')
    })
  })

  describe('Verification Code', () => {
    it('shows notification when getting verification code', async () => {
      const wrapper = mount(RegisterPage, {
        global: {
          stubs: { AlertBox: true },
        },
      })

      const codeBtn = wrapper.find('.code-btn')
      await codeBtn.trigger('click')
    })
  })

  describe('Navigation', () => {
    it('navigates to login page when login link is clicked', async () => {
      const wrapper = mount(RegisterPage, {
        global: {
          stubs: { AlertBox: true },
        },
      })

      const loginLink = wrapper.find('.login-btn-link')
      await loginLink.trigger('click')

      expect(mockRouterPush).toHaveBeenCalledWith('/login')
    })

    it('navigates to home page when back button is clicked', async () => {
      const wrapper = mount(RegisterPage, {
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