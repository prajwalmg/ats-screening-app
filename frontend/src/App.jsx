import { BrowserRouter, Routes, Route, Link } from 'react-router-dom'
import Apply from './pages/Apply'
import AdminJobs from './pages/AdminJobs'
import AdminApplications from './pages/AdminApplications'
import AdminAuthGate from './components/AdminAuthGate'

function App() {
  return (
    <BrowserRouter>
      <nav className="border-b border-gray-200 bg-white">
        <div className="max-w-5xl mx-auto px-4 py-3 flex gap-6 text-sm font-medium">
          <span className="text-gray-900 font-semibold">ATS</span>
          <Link to="/apply" className="text-gray-600 hover:text-gray-900">
            Apply
          </Link>
          <Link to="/admin/jobs" className="text-gray-600 hover:text-gray-900">
            Admin · Jobs
          </Link>
          <Link to="/admin/applications" className="text-gray-600 hover:text-gray-900">
            Admin · Applications
          </Link>
        </div>
      </nav>
      <Routes>
        <Route path="/apply" element={<Apply />} />
        <Route
          path="/admin/jobs"
          element={
            <AdminAuthGate>
              <AdminJobs />
            </AdminAuthGate>
          }
        />
        <Route
          path="/admin/applications"
          element={
            <AdminAuthGate>
              <AdminApplications />
            </AdminAuthGate>
          }
        />
      </Routes>
    </BrowserRouter>
  )
}

export default App
