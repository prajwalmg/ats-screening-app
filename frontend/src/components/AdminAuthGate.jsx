import { useState } from 'react'
import { apiClient, ADMIN_TOKEN_STORAGE_KEY, extractErrorMessage } from '../api/client'

function AdminAuthGate({ children }) {
  const [token, setToken] = useState(() => localStorage.getItem(ADMIN_TOKEN_STORAGE_KEY))
  const [username, setUsername] = useState('admin')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  async function handleLogin(e) {
    e.preventDefault()
    setLoading(true)
    setError('')
    try {
      const { data } = await apiClient.post('/auth/login', { username, password })
      localStorage.setItem(ADMIN_TOKEN_STORAGE_KEY, data.token)
      setToken(data.token)
    } catch (err) {
      setError(extractErrorMessage(err, 'Login failed'))
    } finally {
      setLoading(false)
    }
  }

  function handleLogout() {
    localStorage.removeItem(ADMIN_TOKEN_STORAGE_KEY)
    setToken(null)
  }

  if (!token) {
    return (
      <div className="max-w-sm mx-auto mt-16 p-6 border border-gray-200 rounded-lg shadow-sm bg-white">
        <h1 className="text-lg font-semibold mb-1 text-gray-900">Admin sign in</h1>
        <p className="text-sm text-gray-500 mb-4">Default demo credentials: admin / admin123</p>
        <form onSubmit={handleLogin} className="space-y-3">
          <div>
            <label className="block text-sm font-medium text-gray-700">Username</label>
            <input
              className="mt-1 w-full rounded border border-gray-300 px-3 py-2 text-sm"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700">Password</label>
            <input
              type="password"
              className="mt-1 w-full rounded border border-gray-300 px-3 py-2 text-sm"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
            />
          </div>
          {error && <p className="text-sm text-red-600">{error}</p>}
          <button
            type="submit"
            disabled={loading}
            className="w-full bg-blue-600 text-white rounded py-2 text-sm font-medium disabled:opacity-50"
          >
            {loading ? 'Signing in…' : 'Sign in'}
          </button>
        </form>
      </div>
    )
  }

  return (
    <div>
      <div className="flex justify-end px-6 pt-4">
        <button onClick={handleLogout} className="text-sm text-gray-500 hover:text-gray-800">
          Sign out
        </button>
      </div>
      {children}
    </div>
  )
}

export default AdminAuthGate
