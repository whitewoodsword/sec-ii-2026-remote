import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import UserInformationPage from '../components/UserInformationPage.vue'
import { useAuthStore } from '../stores/auth'

const mockRouterPush = vi.fn()
const mockRouteParams = { params: { id: '1' } }

vi.mock('vue-router', () => ({
  useRouter: () => ({
    push: mockRouterPush,
  }),
  useRoute: () => mockRouteParams,
}))

// Mock axios
vi.mock('axios', () => ({
  default: {
    get: vi.fn(),
  },
}))

import axios from 'axios'

describe('UserInformationPage', () => {
  let authStore

  const mockUser = {
    id: 1,
    name: 'Test User',
    phone: '13800138000',
    avatarPath: '/avatars/test.png',
    isAdmin: false,
    averageScore: 4.5,
    scoreNum: 10,
  }

  const mockReviews = {
    code: 200,
    data: {
      content: [
        {
          id: 1,
          reviewerId: 2,
          reviewerName: 'Reviewer User',
          score: 5,
          content: 'Great user!',
          createdAt: '2026-06-01T10:00:00',
        },
        {
          id: 2,
          reviewerId: 3,
          reviewerName: 'Another User',
          score: 4,
          content: 'Good experience',
          createdAt: '2026-06-02T10:00:00',
        },
      ],
      totalPages: 1,
      totalElements: 2,
    },
  }

  beforeEach(() => {
    vi.clearAllMocks()
    localStorage.clear()
    setActivePinia(createPinia())
    authStore = useAuthStore()
    authStore.setAuth({
      token: 'token-123',
      user: { id: 10, name: 'Logged In User' },
    })

    mockRouteParams.params = { id: '1' }
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  describe('Page Rendering', () => {
    it('renders user information page correctly', async () => {
      axios.get.mockImplementation((url) => {
        if (url.includes('/users/1')) {
          return Promise.resolve({ data: { code: 200, data: mockUser } })
        }
        if (url.includes('/reviews/received/1')) {
          return Promise.resolve({ data: mockReviews })
        }
        return Promise.reject(new Error('Not found'))
      })

      const wrapper = mount(UserInformationPage, {
        global: {
          stubs: { AlertBox: true },
        },
      })

      await flushPromises()

      expect(wrapper.text()).toContain('Test User')
      expect(wrapper.text()).toContain('收到的评价')
    })

    it('displays user stats', async () => {
      axios.get.mockImplementation((url) => {
        if (url.includes('/users/1')) {
          return Promise.resolve({ data: { code: 200, data: mockUser } })
        }
        if (url.includes('/reviews/received/1')) {
          return Promise.resolve({ data: mockReviews })
        }
        return Promise.reject(new Error('Not found'))
      })

      const wrapper = mount(UserInformationPage, {
        global: {
          stubs: { AlertBox: true },
        },
      })

      await flushPromises()

      expect(wrapper.text()).toContain('平均评分')
      expect(wrapper.text()).toContain('评价次数')
    })

    it('displays user basic info', async () => {
      axios.get.mockImplementation((url) => {
        if (url.includes('/users/1')) {
          return Promise.resolve({ data: { code: 200, data: mockUser } })
        }
        if (url.includes('/reviews/received/1')) {
          return Promise.resolve({ data: mockReviews })
        }
        return Promise.reject(new Error('Not found'))
      })

      const wrapper = mount(UserInformationPage, {
        global: {
          stubs: { AlertBox: true },
        },
      })

      await flushPromises()

      expect(wrapper.text()).toContain('手机号')
      expect(wrapper.text()).toContain('13800138000')
      expect(wrapper.text()).toContain('身份')
      expect(wrapper.text()).toContain('普通用户')
    })
  })

  describe('Reviews Display', () => {
    it('displays list of reviews', async () => {
      axios.get.mockImplementation((url) => {
        if (url.includes('/users/1')) {
          return Promise.resolve({ data: { code: 200, data: mockUser } })
        }
        if (url.includes('/reviews/received/1')) {
          return Promise.resolve({ data: mockReviews })
        }
        return Promise.reject(new Error('Not found'))
      })

      const wrapper = mount(UserInformationPage, {
        global: {
          stubs: { AlertBox: true },
        },
      })

      await flushPromises()

      expect(wrapper.text()).toContain('Great user!')
      expect(wrapper.text()).toContain('Good experience')
    })

    it('shows loading state while fetching reviews', async () => {
      let resolveReviews
      const reviewsPromise = new Promise((resolve) => {
        resolveReviews = resolve
      })

      axios.get.mockImplementation((url) => {
        if (url.includes('/users/1')) {
          return Promise.resolve({ data: { code: 200, data: mockUser } })
        }
        if (url.includes('/reviews/received/1')) {
          return reviewsPromise
        }
        return Promise.reject(new Error('Not found'))
      })

      const wrapper = mount(UserInformationPage, {
        global: {
          stubs: { AlertBox: true },
        },
      })

      await flushPromises()

      expect(wrapper.text()).toContain('加载评价中...')

      resolveReviews({ data: mockReviews })
      await flushPromises()
    })

    it('shows empty state when no reviews', async () => {
      axios.get.mockImplementation((url) => {
        if (url.includes('/users/1')) {
          return Promise.resolve({ data: { code: 200, data: mockUser } })
        }
        if (url.includes('/reviews/received/1')) {
          return Promise.resolve({
            data: {
              code: 200,
              data: {
                content: [],
                totalPages: 0,
                totalElements: 0,
              },
            },
          })
        }
        return Promise.reject(new Error('Not found'))
      })

      const wrapper = mount(UserInformationPage, {
        global: {
          stubs: { AlertBox: true },
        },
      })

      await flushPromises()

      expect(wrapper.text()).toContain('暂无评价')
    })

    it('displays star ratings correctly', async () => {
      axios.get.mockImplementation((url) => {
        if (url.includes('/users/1')) {
          return Promise.resolve({ data: { code: 200, data: mockUser } })
        }
        if (url.includes('/reviews/received/1')) {
          return Promise.resolve({ data: mockReviews })
        }
        return Promise.reject(new Error('Not found'))
      })

      const wrapper = mount(UserInformationPage, {
        global: {
          stubs: { AlertBox: true },
        },
      })

      await flushPromises()

      const stars = wrapper.findAll('.star.filled')
      expect(stars.length).toBeGreaterThan(0)
    })
  })

  describe('Score Filtering', () => {
    it('filters reviews by score', async () => {
      const fetchSpy = vi.fn()
      axios.get.mockImplementation((url) => {
        fetchSpy(url)
        if (url.includes('/users/1')) {
          return Promise.resolve({ data: { code: 200, data: mockUser } })
        }
        if (url.includes('/reviews/received/1')) {
          return Promise.resolve({ data: mockReviews })
        }
        return Promise.reject(new Error('Not found'))
      })

      const wrapper = mount(UserInformationPage, {
        global: {
          stubs: { AlertBox: true },
        },
      })

      await flushPromises()

      const filterChips = wrapper.findAll('.filter-chip')
      const fiveStarChip = filterChips.find(chip => chip.text() === '5星')
      await fiveStarChip.trigger('click')

      expect(axios.get).toHaveBeenCalledWith(expect.stringContaining('score=5'))
    })

    it('shows all reviews when "全部" filter is clicked', async () => {
      axios.get.mockImplementation((url) => {
        if (url.includes('/users/1')) {
          return Promise.resolve({ data: { code: 200, data: mockUser } })
        }
        if (url.includes('/reviews/received/1')) {
          return Promise.resolve({ data: mockReviews })
        }
        return Promise.reject(new Error('Not found'))
      })

      const wrapper = mount(UserInformationPage, {
        global: {
          stubs: { AlertBox: true },
        },
      })

      await flushPromises()

      const filterChips = wrapper.findAll('.filter-chip')
      const allChip = filterChips.find(chip => chip.text() === '全部')
      await allChip.trigger('click')

      expect(axios.get).toHaveBeenCalledWith(expect.not.stringContaining('score='))
    })
  })

  describe('Pagination', () => {
    const mockPaginatedReviews = {
      code: 200,
      data: {
        content: [
          {
            id: 1,
            reviewerId: 2,
            reviewerName: 'Reviewer User',
            score: 5,
            content: 'Review 1',
            createdAt: '2026-06-01T10:00:00',
          },
        ],
        totalPages: 3,
        totalElements: 15,
      },
    }

    it('shows pagination when multiple pages exist', async () => {
      axios.get.mockImplementation((url) => {
        if (url.includes('/users/1')) {
          return Promise.resolve({ data: { code: 200, data: mockUser } })
        }
        if (url.includes('/reviews/received/1')) {
          return Promise.resolve({ data: mockPaginatedReviews })
        }
        return Promise.reject(new Error('Not found'))
      })

      const wrapper = mount(UserInformationPage, {
        global: {
          stubs: { AlertBox: true },
        },
      })

      await flushPromises()

      expect(wrapper.find('.pagination').exists()).toBe(true)
      expect(wrapper.text()).toContain('1 / 3')
    })

    it('changes page when next button is clicked', async () => {
      axios.get.mockImplementation((url) => {
        if (url.includes('/users/1')) {
          return Promise.resolve({ data: { code: 200, data: mockUser } })
        }
        if (url.includes('/reviews/received/1')) {
          return Promise.resolve({ data: mockPaginatedReviews })
        }
        return Promise.reject(new Error('Not found'))
      })

      const wrapper = mount(UserInformationPage, {
        global: {
          stubs: { AlertBox: true },
        },
      })

      await flushPromises()

      const nextBtn = wrapper.find('.page-btn:last-child')
      await nextBtn.trigger('click')

      expect(axios.get).toHaveBeenCalledWith(expect.stringContaining('page=1'))
    })
  })

  describe('Error Handling', () => {
    it('shows error when user not found', async () => {
      axios.get.mockImplementation((url) => {
        if (url.includes('/users/1')) {
          return Promise.resolve({ data: { code: 404, message: 'User not found' } })
        }
        return Promise.reject(new Error('Not found'))
      })

        mount(UserInformationPage, {
        global: {
          stubs: { AlertBox: true },
        },
      })

      await flushPromises()
    })

    it('shows error when fetching reviews fails', async () => {
      axios.get.mockImplementation((url) => {
        if (url.includes('/users/1')) {
          return Promise.resolve({ data: { code: 200, data: mockUser } })
        }
        if (url.includes('/reviews/received/1')) {
          return Promise.reject(new Error('Network error'))
        }
        return Promise.reject(new Error('Not found'))
      })

      const wrapper = mount(UserInformationPage, {
        global: {
          stubs: { AlertBox: true },
        },
      })

      await flushPromises()

      expect(wrapper.text()).toContain('收到的评价')
    })
  })

  describe('Avatar Display', () => {
    it('displays user avatar', async () => {
      axios.get.mockImplementation((url) => {
        if (url.includes('/users/1')) {
          return Promise.resolve({ data: { code: 200, data: mockUser } })
        }
        if (url.includes('/reviews/received/1')) {
          return Promise.resolve({ data: mockReviews })
        }
        return Promise.reject(new Error('Not found'))
      })

      const wrapper = mount(UserInformationPage, {
        global: {
          stubs: { AlertBox: true },
        },
      })

      await flushPromises()

      const avatar = wrapper.find('.avatar-large')
      expect(avatar.exists()).toBe(true)
    })

    it('uses default avatar when no avatar path', async () => {
      const userWithoutAvatar = { ...mockUser, avatarPath: null }
      axios.get.mockImplementation((url) => {
        if (url.includes('/users/1')) {
          return Promise.resolve({ data: { code: 200, data: userWithoutAvatar } })
        }
        if (url.includes('/reviews/received/1')) {
          return Promise.resolve({ data: mockReviews })
        }
        return Promise.reject(new Error('Not found'))
      })

      const wrapper = mount(UserInformationPage, {
        global: {
          stubs: { AlertBox: true },
        },
      })

      await flushPromises()

      const avatar = wrapper.find('.avatar-large')
      expect(avatar.attributes('src')).toContain('default-avatar.png')
    })
  })
})