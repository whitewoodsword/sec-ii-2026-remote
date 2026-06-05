import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import DemandDetailPage from '../components/DemandDetailPage.vue'
import { useAuthStore } from '../stores/auth'

const mockRouterPush = vi.fn()
const mockRouterBack = vi.fn()
const mockRouterGo = vi.fn()
const routeState = { params: { id: '101' } }

vi.mock('vue-router', () => ({
  useRouter: () => ({
    push: mockRouterPush,
    back: mockRouterBack,
    go: mockRouterGo,
  }),
  useRoute: () => routeState,
}))

describe('DemandDetailPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    localStorage.clear()
    setActivePinia(createPinia())
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('renders demand detail and shows accept actions for a logged-in non-publisher', async () => {
    const authStore = useAuthStore()
    authStore.setAuth({
      token: 'token-123',
      user: { id: 2, name: 'Acceptor User' },
    })

    const fetchMock = vi.fn((url) => {
      if (url.includes('/demands/101')) {
        return Promise.resolve({
          json: () => Promise.resolve({
            code: 200,
            data: {
              id: 101,
              title: 'Pickup package',
              description: 'Need help collecting a parcel',
              category: '快递代取',
              publisherId: 1,
              status: 'PENDING',
              location: 'Cainiao Station',
              deadline: '2026-06-03T12:00:00',
              reward: 8,
              createdAt: '2026-06-03T09:00:00',
              updatedAt: '2026-06-03T09:30:00',
              pictureUrls: '',
            },
          }),
        })
      }

      if (url.includes('/users/1')) {
        return Promise.resolve({
          json: () => Promise.resolve({
            code: 200,
            data: {
              id: 1,
              name: 'Publisher User',
              averageScore: 4.8,
              scoreNum: 12,
              avatarPath: null,
              isAdmin: false,
              isSuperAdmin: false,
            },
          }),
        })
      }

      throw new Error(`Unexpected fetch: ${url}`)
    })
    vi.stubGlobal('fetch', fetchMock)

    const wrapper = mount(DemandDetailPage, {
      global: {
        stubs: { AlertBox: true },
      },
    })

    await flushPromises()

    expect(wrapper.find('.demand-title').text()).toContain('Pickup package')
    expect(wrapper.find('.publisher-card').exists()).toBe(true)
    expect(wrapper.find('.contact-btn').exists()).toBe(true)
    expect(wrapper.find('.accept-btn').exists()).toBe(true)
  })

  it('creates an order when accept button is clicked', async () => {
    const authStore = useAuthStore()
    authStore.setAuth({
      token: 'token-456',
      user: { id: 2, name: 'Acceptor User' },
    })

    const fetchMock = vi.fn((url, options = {}) => {
      if (url.includes('/demands/101')) {
        return Promise.resolve({
          json: () => Promise.resolve({
            code: 200,
            data: {
              id: 101,
              title: 'Pickup package',
              description: 'Need help collecting a parcel',
              category: '快递代取',
              publisherId: 1,
              status: 'PENDING',
              location: 'Cainiao Station',
              deadline: '2026-06-03T12:00:00',
              reward: 8,
              createdAt: '2026-06-03T09:00:00',
              updatedAt: '2026-06-03T09:30:00',
              pictureUrls: '',
            },
          }),
        })
      }

      if (url.includes('/users/1')) {
        return Promise.resolve({
          json: () => Promise.resolve({
            code: 200,
            data: {
              id: 1,
              name: 'Publisher User',
              averageScore: 4.8,
              scoreNum: 12,
              avatarPath: null,
              isAdmin: false,
              isSuperAdmin: false,
            },
          }),
        })
      }

      if (url.includes('/orders/create')) {
        expect(options.method).toBe('POST')
        return Promise.resolve({
          json: () => Promise.resolve({
            code: 200,
            data: { id: 501, demandId: 101, acceptorId: 2 },
          }),
        })
      }

      throw new Error(`Unexpected fetch: ${url}`)
    })
    vi.stubGlobal('fetch', fetchMock)

    const wrapper = mount(DemandDetailPage, {
      global: {
        stubs: { AlertBox: true },
      },
    })

    await flushPromises()
    await wrapper.find('.accept-btn').trigger('click')
    await flushPromises()

    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining('/orders/create?demandId=101&userId=2'),
      expect.objectContaining({ method: 'POST' }),
    )
  })
})
