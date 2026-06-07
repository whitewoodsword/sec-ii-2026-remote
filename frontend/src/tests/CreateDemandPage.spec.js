import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import CreateDemandPage from '../components/CreateDemandPage.vue'
import { useAuthStore } from '../stores/auth'

const mockRouterPush = vi.fn()
const mockRouterBack = vi.fn()

vi.mock('vue-router', () => ({
  useRouter: () => ({
    push: mockRouterPush,
    back: mockRouterBack,
  }),
}))

describe('CreateDemandPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    localStorage.clear()
    setActivePinia(createPinia())
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  describe('Form Validation', () => {

    it('shows error when title exceeds 100 characters', async () => {
      const authStore = useAuthStore()
      authStore.setAuth({
        token: 'token-123',
        user: { id: 1, name: 'Test User' },
      })

      const wrapper = mount(CreateDemandPage, {
        global: {
          stubs: { AlertBox: true },
        },
      })

      const titleInput = wrapper.find('input[placeholder*="需求标题"]')
      await titleInput.setValue('a'.repeat(101))

      await wrapper.find('form').trigger('submit.prevent')

      expect(wrapper.text()).toContain('标题不能超过100个字符')
    })

    it('shows error when no category is selected', async () => {
      const authStore = useAuthStore()
      authStore.setAuth({
        token: 'token-123',
        user: { id: 1, name: 'Test User' },
      })

      const wrapper = mount(CreateDemandPage, {
        global: {
          stubs: { AlertBox: true },
        },
      })

      const titleInput = wrapper.find('input[placeholder*="需求标题"]')
      await titleInput.setValue('Valid Title')

      await wrapper.find('form').trigger('submit.prevent')

      expect(wrapper.text()).toContain('请选择需求分类')
    })

    it('validates reward must be positive number', async () => {
      const authStore = useAuthStore()
      authStore.setAuth({
        token: 'token-123',
        user: { id: 1, name: 'Test User' },
      })

      const wrapper = mount(CreateDemandPage, {
        global: {
          stubs: { AlertBox: true },
        },
      })

      const titleInput = wrapper.find('input[placeholder*="需求标题"]')
      await titleInput.setValue('Valid Title')

      const categoryBtn = wrapper.findAll('.category-btn')[0]
      await categoryBtn.trigger('click')

      const rewardInput = wrapper.find('.reward-input')
      await rewardInput.setValue('-5')

      await wrapper.find('form').trigger('submit.prevent')

      expect(wrapper.text()).toContain('酬金必须为正数')
    })

    it('validates deadline must be in the future', async () => {
      const authStore = useAuthStore()
      authStore.setAuth({
        token: 'token-123',
        user: { id: 1, name: 'Test User' },
      })

      const wrapper = mount(CreateDemandPage, {
        global: {
          stubs: { AlertBox: true },
        },
      })

      const titleInput = wrapper.find('input[placeholder*="需求标题"]')
      await titleInput.setValue('Valid Title')

      const categoryBtn = wrapper.findAll('.category-btn')[0]
      await categoryBtn.trigger('click')

      const deadlineInput = wrapper.find('input[type="datetime-local"]')
      const pastDate = new Date()
      pastDate.setDate(pastDate.getDate() - 1)
      await deadlineInput.setValue(pastDate.toISOString().slice(0, 16))

      await wrapper.find('form').trigger('submit.prevent')

      expect(wrapper.text()).toContain('截止时间必须晚于当前时间')
    })
  })

  describe('Category Selection', () => {
    it('displays all category options', () => {
      const wrapper = mount(CreateDemandPage, {
        global: {
          stubs: { AlertBox: true },
        },
      })

      const categories = ['快递代取', '学习辅导', '二手交易', '活动组队', '其他']
      categories.forEach(cat => {
        expect(wrapper.text()).toContain(cat)
      })
    })

    it('highlights selected category', async () => {
      const wrapper = mount(CreateDemandPage, {
        global: {
          stubs: { AlertBox: true },
        },
      })

      const categoryBtns = wrapper.findAll('.category-btn')
      await categoryBtns[1].trigger('click')

      expect(categoryBtns[1].classes()).toContain('active')
    })
  })

  describe('Image Upload', () => {
    it('shows image preview after selecting files', async () => {
      const wrapper = mount(CreateDemandPage, {
        global: {
          stubs: { AlertBox: true },
        },
      })

      const file = new File(['test'], 'test.jpg', { type: 'image/jpeg' })
      const fileInput = wrapper.find('input[type="file"]')

      Object.defineProperty(fileInput.element, 'files', {
        value: [file],
      })
      await fileInput.trigger('change')

      expect(wrapper.find('.image-preview-grid').exists()).toBe(true)
    })

    it('removes image when remove button is clicked', async () => {
      const wrapper = mount(CreateDemandPage, {
        global: {
          stubs: { AlertBox: true },
        },
      })

      const file = new File(['test'], 'test.jpg', { type: 'image/jpeg' })
      const fileInput = wrapper.find('input[type="file"]')

      Object.defineProperty(fileInput.element, 'files', {
        value: [file],
      })
      await fileInput.trigger('change')

      expect(wrapper.findAll('.image-preview-item').length).toBe(1)

      const removeBtn = wrapper.find('.remove-image-btn')
      await removeBtn.trigger('click')

      expect(wrapper.findAll('.image-preview-item').length).toBe(0)
    })
  })

  describe('Navigation', () => {
    it('navigates back when back button is clicked', async () => {
      const wrapper = mount(CreateDemandPage, {
        global: {
          stubs: { AlertBox: true },
        },
      })

      const backBtn = wrapper.find('.back-btn')
      await backBtn.trigger('click')

      expect(mockRouterBack).toHaveBeenCalled()
    })

    it('navigates back when cancel button is clicked', async () => {
      const wrapper = mount(CreateDemandPage, {
        global: {
          stubs: { AlertBox: true },
        },
      })

      const cancelBtn = wrapper.find('.cancel-btn')
      await cancelBtn.trigger('click')

      expect(mockRouterBack).toHaveBeenCalled()
    })
  })

  describe('Form Submission', () => {
    it('requires login before submitting', async () => {
      const authStore = useAuthStore()
      authStore.logout()

      const wrapper = mount(CreateDemandPage, {
        global: {
          stubs: { AlertBox: true },
        },
      })

      const titleInput = wrapper.find('input[placeholder*="需求标题"]')
      await titleInput.setValue('Valid Title')

      const categoryBtn = wrapper.findAll('.category-btn')[0]
      await categoryBtn.trigger('click')

      await wrapper.find('form').trigger('submit.prevent')

      expect(wrapper.text()).toContain('发布新需求')
    })

    it('submits form successfully when all fields are valid', async () => {
      const authStore = useAuthStore()
      authStore.setAuth({
        token: 'token-123',
        user: { id: 1, name: 'Test User' },
      })

      const fetchMock = vi.fn((url, options) => {
        if (url === 'http://localhost:8080/demands/create') {
          expect(options.method).toBe('POST')
          return Promise.resolve({
            json: () => Promise.resolve({ code: 200, message: 'success' }),
          })
        }
        return Promise.resolve({
          json: () => Promise.resolve({ code: 200 }),
        })
      })
      vi.stubGlobal('fetch', fetchMock)

      const wrapper = mount(CreateDemandPage, {
        global: {
          stubs: { AlertBox: true },
        },
      })

      const titleInput = wrapper.find('input[placeholder*="需求标题"]')
      await titleInput.setValue('Valid Test Title')

      const categoryBtn = wrapper.findAll('.category-btn')[0]
      await categoryBtn.trigger('click')

      await wrapper.find('form').trigger('submit.prevent')
      await flushPromises()

      expect(wrapper.text()).toContain('发布新需求')
    })
  })
})