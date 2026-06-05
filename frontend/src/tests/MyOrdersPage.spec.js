import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import MyOrdersPage from '../components/MyOrdersPage.vue'
import { useAuthStore } from '../stores/auth'

const mockRouterPush = vi.fn()

vi.mock('vue-router', () => ({
  useRouter: () => ({
    push: mockRouterPush,
  }),
}))

describe('MyOrdersPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    localStorage.clear()
    setActivePinia(createPinia())
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  function buildFetchMock() {
    return vi.fn((url, options = {}) => {
      if (url.includes('/orders/user/1')) {
        return Promise.resolve({
          json: () => Promise.resolve({
            code: 200,
            data: {
              content: [
                {
                  id: 11,
                  demandId: 101,
                  publisherId: 1,
                  acceptorId: 2,
                  status: 'IN_PROGRESS',
                  createdAt: '2026-06-03T09:00:00',
                  updatedAt: '2026-06-03T10:00:00',
                  partnerName: 'Acceptor User',
                },
                {
                  id: 12,
                  demandId: 102,
                  publisherId: 3,
                  acceptorId: 1,
                  status: 'COMPLETED',
                  createdAt: '2026-06-01T09:00:00',
                  updatedAt: '2026-06-01T11:00:00',
                  completedAt: '2026-06-01T11:00:00',
                  partnerName: 'Publisher User',
                },
              ],
              totalPages: 1,
              totalElements: 2,
            },
          }),
        })
      }

      if (url.includes('/reviews/user-order?orderId=11&userId=1')) {
        return Promise.resolve({
          json: () => Promise.resolve({ code: 200, data: { id: 9001 } }),
        })
      }

      if (url.includes('/reviews/user-order?orderId=12&userId=1')) {
        return Promise.resolve({
          json: () => Promise.resolve({ code: 404, message: 'not reviewed' }),
        })
      }

      if (url.includes('/orders/11/status')) {
        expect(options.method).toBe('PATCH')
        return Promise.resolve({
          json: () => Promise.resolve({ code: 200, data: { id: 11, status: 'IN_PROGRESS' } }),
        })
      }

      if (url.includes('/reviews/create?')) {
        expect(options.method).toBe('POST')
        return Promise.resolve({
          json: () => Promise.resolve({ code: 201, data: { id: 7001 } }),
        })
      }

      throw new Error(`Unexpected fetch: ${url}`)
    })
  }

  it('renders order list and review list after loading', async () => {
    const authStore = useAuthStore()
    authStore.setAuth({
      token: 'token-123',
      user: { id: 1, name: 'Current User' },
    })

    vi.stubGlobal('fetch', buildFetchMock())

    const wrapper = mount(MyOrdersPage, {
      global: {
        stubs: {
          AlertBox: true,
          PaginationComponent: true,
          Pagination: true,
        },
      },
    })

    await flushPromises()

    expect(wrapper.findAll('.order-card').length).toBe(2)
    expect(wrapper.find('.review-card').exists()).toBe(true)
    expect(wrapper.text()).toContain('Acceptor User')
  })

  it('navigates to order detail when an order card is clicked', async () => {
    const authStore = useAuthStore()
    authStore.setAuth({
      token: 'token-123',
      user: { id: 1, name: 'Current User' },
    })

    vi.stubGlobal('fetch', buildFetchMock())

    const wrapper = mount(MyOrdersPage, {
      global: {
        stubs: {
          AlertBox: true,
          PaginationComponent: true,
          Pagination: true,
        },
      },
    })

    await flushPromises()
    await wrapper.find('.order-card').trigger('click')

    expect(mockRouterPush).toHaveBeenCalledWith('/order/11')
  })

  it('submits a review from the pending review panel', async () => {
    const authStore = useAuthStore()
    authStore.setAuth({
      token: 'token-123',
      user: { id: 1, name: 'Current User' },
    })

    const fetchMock = buildFetchMock()
    vi.stubGlobal('fetch', fetchMock)

    const wrapper = mount(MyOrdersPage, {
      global: {
        stubs: {
          AlertBox: true,
          PaginationComponent: true,
          Pagination: true,
        },
      },
    })

    await flushPromises()
    await wrapper.find('.review-action-btn').trigger('click')
    await flushPromises()

    const textarea = wrapper.find('.comment-input')
    await textarea.setValue('Great cooperation')
    await wrapper.find('.submit-btn').trigger('click')
    await flushPromises()

    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining('/reviews/create?'),
      expect.objectContaining({ method: 'POST' }),
    )
  })
})
