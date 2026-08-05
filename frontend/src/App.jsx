import { BrowserRouter, Routes, Route } from 'react-router-dom'
import Apply from './pages/Apply'
import AdminJobs from './pages/AdminJobs'
import AdminApplications from './pages/AdminApplications'

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/apply" element={<Apply />} />
        <Route path="/admin/jobs" element={<AdminJobs />} />
        <Route path="/admin/applications" element={<AdminApplications />} />
      </Routes>
    </BrowserRouter>
  )
}

export default App
