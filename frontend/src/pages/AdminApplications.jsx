import { useEffect, useState } from 'react'
import { apiClient, extractErrorMessage } from '../api/client'

const STATUS_OPTIONS = ['SUBMITTED', 'UNDER_REVIEW', 'REJECTED', 'ADVANCED']
const PAGE_SIZE = 10

function AdminApplications() {
  const [jobs, setJobs] = useState([])
  const [jobFilter, setJobFilter] = useState('')
  const [statusFilter, setStatusFilter] = useState('')
  const [page, setPage] = useState(0)
  const [applications, setApplications] = useState([])
  const [pageInfo, setPageInfo] = useState({ totalElements: 0, totalPages: 0 })
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    apiClient
      .get('/api/admin/jobs')
      .then(({ data }) => setJobs(data))
      .catch(() => {})
  }, [])

  useEffect(() => {
    setLoading(true)
    setError('')
    const params = { page, size: PAGE_SIZE }
    if (jobFilter) params.jobId = jobFilter
    if (statusFilter) params.status = statusFilter

    apiClient
      .get('/api/admin/applications', { params })
      .then(({ data }) => {
        setApplications(data.content)
        setPageInfo(data.page)
      })
      .catch((err) => setError(extractErrorMessage(err, 'Failed to load applications')))
      .finally(() => setLoading(false))
  }, [jobFilter, statusFilter, page])

  function handleJobFilterChange(value) {
    setJobFilter(value)
    setPage(0)
  }

  function handleStatusFilterChange(value) {
    setStatusFilter(value)
    setPage(0)
  }

  const totalPages = pageInfo.totalPages ?? 0

  return (
    <div className="max-w-5xl mx-auto mt-8 px-4 space-y-4">
      <h1 className="text-lg font-semibold text-gray-900">Applications</h1>

      <div className="flex gap-4">
        <select
          className="rounded border border-gray-300 px-3 py-2 text-sm"
          value={jobFilter}
          onChange={(e) => handleJobFilterChange(e.target.value)}
        >
          <option value="">All jobs</option>
          {jobs.map((job) => (
            <option key={job.id} value={job.id}>
              {job.title}
            </option>
          ))}
        </select>
        <select
          className="rounded border border-gray-300 px-3 py-2 text-sm"
          value={statusFilter}
          onChange={(e) => handleStatusFilterChange(e.target.value)}
        >
          <option value="">All statuses</option>
          {STATUS_OPTIONS.map((status) => (
            <option key={status} value={status}>
              {status}
            </option>
          ))}
        </select>
      </div>

      {error && <p className="text-sm text-red-600">{error}</p>}

      <div className="overflow-x-auto border border-gray-200 rounded-lg">
        <table className="min-w-full text-sm">
          <thead className="bg-gray-50 text-left text-gray-500">
            <tr>
              <th className="px-4 py-2">Candidate</th>
              <th className="px-4 py-2">Job</th>
              <th className="px-4 py-2">Status</th>
              <th className="px-4 py-2">Score</th>
              <th className="px-4 py-2">Submitted</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-100">
            {loading ? (
              <tr>
                <td colSpan={5} className="px-4 py-6 text-center text-gray-400">
                  Loading…
                </td>
              </tr>
            ) : applications.length === 0 ? (
              <tr>
                <td colSpan={5} className="px-4 py-6 text-center text-gray-400">
                  No applications found
                </td>
              </tr>
            ) : (
              applications.map((app) => (
                <tr key={app.id}>
                  <td className="px-4 py-2 font-medium text-gray-900">{app.candidateName}</td>
                  <td className="px-4 py-2 text-gray-600">{app.jobTitle}</td>
                  <td className="px-4 py-2 text-gray-600">{app.status}</td>
                  <td className="px-4 py-2 text-gray-600">{app.matchScore ?? '—'}</td>
                  <td className="px-4 py-2 text-gray-600">
                    {new Date(app.submittedAt).toLocaleString()}
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      <div className="flex items-center justify-between text-sm text-gray-600">
        <span>
          {pageInfo.totalElements ?? 0} total application{pageInfo.totalElements === 1 ? '' : 's'}
        </span>
        <div className="flex items-center gap-3">
          <button
            className="px-3 py-1 rounded border border-gray-300 disabled:opacity-40"
            disabled={page === 0}
            onClick={() => setPage((p) => Math.max(0, p - 1))}
          >
            Previous
          </button>
          <span>
            Page {totalPages === 0 ? 0 : page + 1} of {totalPages}
          </span>
          <button
            className="px-3 py-1 rounded border border-gray-300 disabled:opacity-40"
            disabled={page + 1 >= totalPages}
            onClick={() => setPage((p) => p + 1)}
          >
            Next
          </button>
        </div>
      </div>
    </div>
  )
}

export default AdminApplications
