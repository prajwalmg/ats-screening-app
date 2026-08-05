import { useEffect, useState } from 'react'
import { apiClient, extractErrorMessage } from '../api/client'

const initialForm = {
  title: '',
  description: '',
  department: '',
  requiredSkills: '',
  minYearsExperience: '',
}

function AdminJobs() {
  const [jobs, setJobs] = useState([])
  const [loading, setLoading] = useState(true)
  const [listError, setListError] = useState('')
  const [form, setForm] = useState(initialForm)
  const [formError, setFormError] = useState('')
  const [submitting, setSubmitting] = useState(false)

  function loadJobs() {
    setLoading(true)
    apiClient
      .get('/api/admin/jobs')
      .then(({ data }) => setListError('') || setJobs(data))
      .catch((err) => setListError(extractErrorMessage(err, 'Failed to load jobs')))
      .finally(() => setLoading(false))
  }

  useEffect(loadJobs, [])

  function updateField(field, value) {
    setForm((prev) => ({ ...prev, [field]: value }))
  }

  async function handleSubmit(e) {
    e.preventDefault()
    setFormError('')
    if (!form.title.trim() || !form.description.trim()) {
      setFormError('Title and description are required')
      return
    }

    setSubmitting(true)
    try {
      await apiClient.post('/api/admin/jobs', {
        title: form.title,
        description: form.description,
        department: form.department || null,
        requiredSkills: form.requiredSkills
          ? form.requiredSkills.split(',').map((s) => s.trim()).filter(Boolean)
          : [],
        minYearsExperience: form.minYearsExperience ? Number(form.minYearsExperience) : null,
      })
      setForm(initialForm)
      loadJobs()
    } catch (err) {
      setFormError(extractErrorMessage(err, 'Failed to create job'))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="max-w-4xl mx-auto mt-8 px-4 space-y-8">
      <div>
        <h1 className="text-lg font-semibold text-gray-900 mb-4">Create a job</h1>
        <form onSubmit={handleSubmit} className="grid grid-cols-2 gap-4 bg-white border border-gray-200 rounded-lg p-6">
          <div className="col-span-2">
            <label className="block text-sm font-medium text-gray-700">Title</label>
            <input
              className="mt-1 w-full rounded border border-gray-300 px-3 py-2 text-sm"
              value={form.title}
              onChange={(e) => updateField('title', e.target.value)}
            />
          </div>
          <div className="col-span-2">
            <label className="block text-sm font-medium text-gray-700">Description</label>
            <textarea
              rows={2}
              className="mt-1 w-full rounded border border-gray-300 px-3 py-2 text-sm"
              value={form.description}
              onChange={(e) => updateField('description', e.target.value)}
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700">Department</label>
            <input
              className="mt-1 w-full rounded border border-gray-300 px-3 py-2 text-sm"
              value={form.department}
              onChange={(e) => updateField('department', e.target.value)}
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700">Min. years experience</label>
            <input
              type="number"
              min="0"
              className="mt-1 w-full rounded border border-gray-300 px-3 py-2 text-sm"
              value={form.minYearsExperience}
              onChange={(e) => updateField('minYearsExperience', e.target.value)}
            />
          </div>
          <div className="col-span-2">
            <label className="block text-sm font-medium text-gray-700">Required skills (comma-separated)</label>
            <input
              className="mt-1 w-full rounded border border-gray-300 px-3 py-2 text-sm"
              placeholder="Java, Spring Boot, PostgreSQL"
              value={form.requiredSkills}
              onChange={(e) => updateField('requiredSkills', e.target.value)}
            />
          </div>
          {formError && <p className="col-span-2 text-sm text-red-600">{formError}</p>}
          <div className="col-span-2">
            <button
              type="submit"
              disabled={submitting}
              className="bg-blue-600 text-white rounded px-4 py-2 text-sm font-medium disabled:opacity-50"
            >
              {submitting ? 'Creating…' : 'Create job'}
            </button>
          </div>
        </form>
      </div>

      <div>
        <h2 className="text-lg font-semibold text-gray-900 mb-4">Jobs</h2>
        {listError && <p className="text-sm text-red-600 mb-3">{listError}</p>}
        {loading ? (
          <p className="text-sm text-gray-500">Loading…</p>
        ) : (
          <div className="overflow-x-auto border border-gray-200 rounded-lg">
            <table className="min-w-full text-sm">
              <thead className="bg-gray-50 text-left text-gray-500">
                <tr>
                  <th className="px-4 py-2">Title</th>
                  <th className="px-4 py-2">Department</th>
                  <th className="px-4 py-2">Required skills</th>
                  <th className="px-4 py-2">Min. years</th>
                  <th className="px-4 py-2">Status</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {jobs.map((job) => (
                  <tr key={job.id}>
                    <td className="px-4 py-2 font-medium text-gray-900">{job.title}</td>
                    <td className="px-4 py-2 text-gray-600">{job.department || '—'}</td>
                    <td className="px-4 py-2 text-gray-600">{(job.requiredSkills || []).join(', ') || '—'}</td>
                    <td className="px-4 py-2 text-gray-600">{job.minYearsExperience ?? '—'}</td>
                    <td className="px-4 py-2 text-gray-600">{job.status}</td>
                  </tr>
                ))}
                {jobs.length === 0 && (
                  <tr>
                    <td colSpan={5} className="px-4 py-6 text-center text-gray-400">
                      No jobs yet
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  )
}

export default AdminJobs
