import axios from 'axios'

export const ADMIN_TOKEN_STORAGE_KEY = 'ats_admin_token'

export const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_GATEWAY_URL || 'http://localhost:8080',
})

apiClient.interceptors.request.use((config) => {
  if (config.url?.startsWith('/api/admin')) {
    const token = localStorage.getItem(ADMIN_TOKEN_STORAGE_KEY)
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
  }
  return config
})

export function extractErrorMessage(error, fallback) {
  return error?.response?.data?.message || error?.message || fallback
}
