import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import OrderDetailPage from '../components/OrderDetailPage.vue'
import { useAuthStore } from '../stores/auth'

const mockRouterPush = vi.fn()
const mockRouterBack = vi.fn()
const mockRouterGo = vi.fn()
const routeState = { params: { id: '501' } }

vi.mock('vue-router', () => ({
  useRouter: () => ({
    push: mockRouterPush,
    back: mockRouterBack,
    go: mockRouterGo,
  }),
  useRoute: () => routeState,
}))

describe('OrderDetailPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    localStorage.clear()
    setActivePinia(createPinia())
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  function stubOrderDetailFetch(status = 'ACCEPTED') {
    return vi.fn((url, options = {}) => {
      if (url.includes('/orders/501') && (!options.method || options.method === 'GET')) {
        return Promise.resolve({
          json: () => Promise.resolve({
            code: 200,
            data: {
              id: 501,
              demandId: 101,
              publisherId: 1,
              acceptorId: 2,
              status,
              createdAt: '2026-06-03T09:00:00',
              updatedAt: '2026-06-03T10:00:00',
              latestRequesterNote: '',
              completedAt: null,
            },
          }),
        })
      }

      if (url.includes('/users/1')) {
        return Promise.resolve({
          json: () => Promise.resolve({ code: 200, data: { id: 1, name: 'Publisher User', phone: '13800000001' } }),
        })
      }

      if (url.includes('/users/2')) {
        return Promise.resolve({
          json: () => Promise.resolve({ code: 200, data: { id: 2, name: 'Acceptor User', phone: '13800000002' } }),
        })
      }

      if (url.includes('/demands/101')) {
        return Promise.resolve({
          json: () => Promise.resolve({ code: 200, data: { id: 101, title: 'Pickup package' } }),
        })
      }

      if (url.includes('/orders/501/status') && options.method === 'PATCH') {
        return Promise.resolve({
          json: () => Promise.resolve({ code: 200, data: { id: 501, status: 'IN_PROGRESS' } }),
        })
      }

      if (url.includes('/orders/501/cancel') && options.method === 'POST') {
        return Promise.resolve({
          json: () => Promise.resolve({ code: 200, data: { id: 501, status: 'CANCELLED' } }),
        })
      }

      throw new Error(`Unexpected fetch: ${url}`)
    })
  }

  it('renders order detail for the acceptor and shows start button', async () => {
    const authStore = useAuthStore()
    authStore.setAuth({
      token: 'token-123',
      user: { id: 2, name: 'Acceptor User' },
    })

    vi.stubGlobal('fetch', stubOrderDetailFetch('ACCEPTED'))

    const wrapper = mount(OrderDetailPage, {
      global: {
        stubs: { AlertBox: true },
      },
    })

    await flushPromises()

    expect(wrapper.find('.order-id').text()).toContain('501')
    expect(wrapper.find('.primary-btn').exists()).toBe(true)
    expect(wrapper.text()).toContain('Pickup package')
  })

  it('starts the order after confirming the dialog', async () => {
    const authStore = useAuthStore()
    authStore.setAuth({
      token: 'token-123',
      user: { id: 2, name: 'Acceptor User' },
    })

    const fetchMock = stubOrderDetailFetch('ACCEPTED')
    vi.stubGlobal('fetch', fetchMock)

    const wrapper = mount(OrderDetailPage, {
      global: {
        stubs: { AlertBox: true },
      },
    })

    await flushPromises()
    await wrapper.find('.primary-btn').trigger('click')
    await flushPromises()
    await wrapper.find('.dialog-btn.confirm').trigger('click')
    await flushPromises()

    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining('/orders/501/status?userId=2&status=IN_PROGRESS'),
      expect.objectContaining({ method: 'PATCH' }),
    )
  })

  it('redirects to login when user is not authenticated', async () => {
    const authStore = useAuthStore()
    authStore.logout()
    vi.stubGlobal('fetch', vi.fn())

    mount(OrderDetailPage, {
      global: {
        stubs: { AlertBox: true },
      },
    })

    await flushPromises()

    expect(mockRouterPush).toHaveBeenCalledWith('/login')
  })
})
