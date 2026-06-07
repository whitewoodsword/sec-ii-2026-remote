import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import MyDemandsPage from '../components/MyDemandsPage.vue'
import { useAuthStore } from '../stores/auth'

const mockRouterPush = vi.fn()

vi.mock('vue-router', () => ({
  useRouter: () => ({
    push: mockRouterPush,
  }),
}))

describe('MyDemandsPage', () => {
  let authStore

  beforeEach(() => {
    vi.clearAllMocks()
    localStorage.clear()
    setActivePinia(createPinia())
    authStore = useAuthStore()
    authStore.setAuth({
      token: 'token-123',
      user: { id: 1, name: 'Test User' },
    })
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  describe('Page Rendering', () => {
    it('renders page title correctly', () => {
      const wrapper = mount(MyDemandsPage, {
        global: {
          stubs: { AlertBox: true, PaginationComponent: true },
        },
      })

      expect(wrapper.text()).toContain('我的需求')
      expect(wrapper.text()).toContain('查看和管理您发布的所有需求')
    })

    it('shows create demand button', () => {
      const wrapper = mount(MyDemandsPage, {
        global: {
          stubs: { AlertBox: true, PaginationComponent: true },
        },
      })

      expect(wrapper.text()).toContain('发布新需求')
    })

    it('displays status filter options', () => {
      const wrapper = mount(MyDemandsPage, {
        global: {
          stubs: { AlertBox: true, PaginationComponent: true },
        },
      })

      const statuses = ['全部状态', '待接取', '已接取', '已拒绝', '已完成', '已取消', '已过期']
      statuses.forEach(status => {
        expect(wrapper.text()).toContain(status)
      })
    })

    it('displays category filter options', () => {
      const wrapper = mount(MyDemandsPage, {
        global: {
          stubs: { AlertBox: true, PaginationComponent: true },
        },
      })

      const categories = ['全部分类', '快递代取', '学习辅导', '二手交易', '活动组队', '其他']
      categories.forEach(cat => {
        expect(wrapper.text()).toContain(cat)
      })
    })
  })

  describe('Demand List', () => {
    const mockDemands = {
      code: 200,
      data: {
        content: [
          {
            id: 1,
            title: 'Test Demand 1',
            description: 'This is a test demand',
            category: '快递代取',
            status: 'PENDING',
            location: 'Test Location',
            reward: 50,
            createdAt: '2026-06-01T10:00:00',
            pictureUrls: '',
          },
          {
            id: 2,
            title: 'Test Demand 2',
            description: 'Another test demand',
            category: '学习辅导',
            status: 'ACCEPTED',
            location: 'Another Location',
            reward: 100,
            createdAt: '2026-06-02T10:00:00',
            pictureUrls: '',
          },
        ],
        totalElements: 2,
        totalPages: 1,
      },
    }

    it('fetches and displays demands on mount', async () => {
      const fetchMock = vi.fn(() =>
        Promise.resolve({
          json: () => Promise.resolve(mockDemands),
        })
      )
      vi.stubGlobal('fetch', fetchMock)

      const wrapper = mount(MyDemandsPage, {
        global: {
          stubs: { AlertBox: true, PaginationComponent: true },
        },
      })

      await flushPromises()

      expect(wrapper.text()).toContain('Test Demand 1')
      expect(wrapper.text()).toContain('Test Demand 2')
    })

    it('shows empty state when no demands', async () => {
      const fetchMock = vi.fn(() =>
        Promise.resolve({
          json: () => Promise.resolve({
            code: 200,
            data: {
              content: [],
              totalElements: 0,
              totalPages: 0,
            },
          }),
        })
      )
      vi.stubGlobal('fetch', fetchMock)

      const wrapper = mount(MyDemandsPage, {
        global: {
          stubs: { AlertBox: true, PaginationComponent: true },
        },
      })

      await flushPromises()

      expect(wrapper.text()).toContain('暂无发布的需求')
    })

    it('shows loading state while fetching', async () => {
      let resolveFetch
      const fetchPromise = new Promise((resolve) => {
        resolveFetch = resolve
      })

      const fetchMock = vi.fn(() => fetchPromise)
      vi.stubGlobal('fetch', fetchMock)

      const wrapper = mount(MyDemandsPage, {
        global: {
          stubs: { AlertBox: true, PaginationComponent: true },
        },
      })

      expect(wrapper.text()).toContain('我的需求')

      resolveFetch({
        json: () => Promise.resolve(mockDemands),
      })
      await flushPromises()
    })
  })

  describe('Status Display', () => {
    it('shows correct status text for PENDING', () => {
      const wrapper = mount(MyDemandsPage, {
        global: {
          stubs: { AlertBox: true, PaginationComponent: true },
        },
      })

      const statusSpan = wrapper.find('.status-pending')
      expect(statusSpan.exists()).toBe(false)
    })

    it('applies correct status class for ACCEPTED', () => {
      const wrapper = mount(MyDemandsPage, {
        global: {
          stubs: { AlertBox: true, PaginationComponent: true },
        },
      })

      const statusSpan = wrapper.find('.status-accepted')
      expect(statusSpan.exists()).toBe(false)
    })
  })

  describe('Actions', () => {
    const mockDemands = {
      code: 200,
      data: {
        content: [
          {
            id: 1,
            title: 'Test Demand',
            description: 'Test description',
            category: '快递代取',
            status: 'PENDING',
            location: 'Test Location',
            reward: 50,
            createdAt: '2026-06-01T10:00:00',
            pictureUrls: '',
          },
        ],
        totalElements: 1,
        totalPages: 1,
      },
    }

    it('navigates to detail when demand card is clicked', async () => {
      const fetchMock = vi.fn(() =>
        Promise.resolve({
          json: () => Promise.resolve(mockDemands),
        })
      )
      vi.stubGlobal('fetch', fetchMock)

      const wrapper = mount(MyDemandsPage, {
        global: {
          stubs: { AlertBox: true, PaginationComponent: true },
        },
      })

      await flushPromises()

      const demandCard = wrapper.find('.demand-card')
      await demandCard.trigger('click')

      expect(mockRouterPush).toHaveBeenCalledWith('/demand/1')
    })

    it('navigates to edit page when edit button is clicked', async () => {
      const fetchMock = vi.fn(() =>
        Promise.resolve({
          json: () => Promise.resolve(mockDemands),
        })
      )
      vi.stubGlobal('fetch', fetchMock)

      const wrapper = mount(MyDemandsPage, {
        global: {
          stubs: { AlertBox: true, PaginationComponent: true },
        },
      })

      await flushPromises()

      const editBtn = wrapper.find('.edit-btn')
      await editBtn.trigger('click')

      expect(mockRouterPush).toHaveBeenCalledWith('/edit/demand/1')
    })

    it('navigates to create page when create button is clicked', async () => {
      const fetchMock = vi.fn(() =>
        Promise.resolve({
          json: () => Promise.resolve({ code: 200, data: { content: [] } }),
        })
      )
      vi.stubGlobal('fetch', fetchMock)

      const wrapper = mount(MyDemandsPage, {
        global: {
          stubs: { AlertBox: true, PaginationComponent: true },
        },
      })

      const createBtn = wrapper.find('.create-btn')
      await createBtn.trigger('click')

      expect(mockRouterPush).toHaveBeenCalledWith('/create/demand')
    })
  })

  describe('Filtering', () => {
    it('handles search input', async () => {
      const fetchMock = vi.fn(() =>
        Promise.resolve({
          json: () => Promise.resolve({ code: 200, data: { content: [], totalElements: 0, totalPages: 0 } }),
        })
      )
      vi.stubGlobal('fetch', fetchMock)

      const wrapper = mount(MyDemandsPage, {
        global: {
          stubs: { AlertBox: true, PaginationComponent: true },
        },
      })

      const searchInput = wrapper.find('.search-box input')
      await searchInput.setValue('test keyword')
      await searchInput.trigger('keyup.enter')

      expect(fetchMock).toHaveBeenCalled()
    })

    it('resets filters when reset button is clicked', async () => {
      const fetchMock = vi.fn(() =>
        Promise.resolve({
          json: () => Promise.resolve({ code: 200, data: { content: [], totalElements: 0, totalPages: 0 } }),
        })
      )
      vi.stubGlobal('fetch', fetchMock)

      const wrapper = mount(MyDemandsPage, {
        global: {
          stubs: { AlertBox: true, PaginationComponent: true },
        },
      })

      const resetBtn = wrapper.find('.reset-btn')
      await resetBtn.trigger('click')

      expect(fetchMock).toHaveBeenCalled()
    })
  })

  describe('Authentication', () => {
    it('redirects to login if not authenticated', async () => {
      const authStore = useAuthStore()
      authStore.logout()

      mount(MyDemandsPage, {
        global: {
          stubs: { AlertBox: true, PaginationComponent: true },
        },
      })

      await flushPromises()

      expect(mockRouterPush).toHaveBeenCalledWith('/login')
    })
  })
})