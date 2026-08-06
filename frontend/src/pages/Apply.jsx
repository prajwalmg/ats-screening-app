import { useEffect, useState } from 'react'
import { apiClient, extractErrorMessage } from '../api/client'

const ALLOWED_RESUME_TYPES = [
  'application/pdf',
  'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
]
const ALLOWED_RESUME_EXTENSIONS = ['.pdf', '.docx']
const MAX_RESUME_BYTES = 5 * 1024 * 1024

function isAllowedResumeFile(file) {
  if (ALLOWED_RESUME_TYPES.includes(file.type)) return true
  // Some browser/OS combos report an empty or generic MIME type for a
  // correctly-typed file (e.g. file.type === '' or 'application/octet-stream'),
  // so fall back to checking the extension rather than false-reject a valid file.
  const name = file.name.toLowerCase()
  return ALLOWED_RESUME_EXTENSIONS.some((ext) => name.endsWith(ext))
}

const initialForm = {
  jobId: '',
  candidateName: '',
  email: '',
  phone: '',
  coverLetterText: '',
}

function Apply() {
  const [jobs, setJobs] = useState([])
  const [jobsError, setJobsError] = useState('')
  const [form, setForm] = useState(initialForm)
  const [resumeFile, setResumeFile] = useState(null)
  const [fieldErrors, setFieldErrors] = useState({})
  const [submitting, setSubmitting] = useState(false)
  const [submitError, setSubmitError] = useState('')
  const [result, setResult] = useState(null)

  useEffect(() => {
    apiClient
      .get('/api/public/jobs')
      .then(({ data }) => setJobs(data.filter((job) => job.status === 'OPEN')))
      .catch((err) => setJobsError(extractErrorMessage(err, 'Failed to load open jobs')))
  }, [])

  function updateField(field, value) {
    setForm((prev) => ({ ...prev, [field]: value }))
  }

  function validate() {
    const errors = {}
    if (!form.jobId) errors.jobId = 'Select a job to apply for'
    if (!form.candidateName.trim()) errors.candidateName = 'Name is required'
    if (!form.email.trim()) {
      errors.email = 'Email is required'
    } else if (!/^\S+@\S+\.\S+$/.test(form.email)) {
      errors.email = 'Enter a valid email address'
    }
    if (!resumeFile) {
      errors.resumeFile = 'Attach a resume (PDF or DOCX)'
    } else if (!isAllowedResumeFile(resumeFile)) {
      errors.resumeFile = 'Only PDF and DOCX files are accepted'
    } else if (resumeFile.size > MAX_RESUME_BYTES) {
      errors.resumeFile = 'Resume must be 5MB or smaller'
    }
    setFieldErrors(errors)
    return Object.keys(errors).length === 0
  }

  async function handleSubmit(e) {
    e.preventDefault()
    setSubmitError('')
    setResult(null)
    if (!validate()) return

    setSubmitting(true)
    try {
      const uploadForm = new FormData()
      uploadForm.append('file', resumeFile)
      const { data: upload } = await apiClient.post('/api/public/resumes', uploadForm, {
        headers: { 'Content-Type': 'multipart/form-data' },
      })

      const { data: application } = await apiClient.post('/api/public/applications', {
        candidateName: form.candidateName,
        email: form.email,
        phone: form.phone || null,
        jobId: Number(form.jobId),
        resumeUrl: upload.resumeUrl,
        coverLetterText: form.coverLetterText || null,
      })

      setResult(application)
      setForm(initialForm)
      setResumeFile(null)
      e.target.reset()
    } catch (err) {
      setSubmitError(extractErrorMessage(err, 'Something went wrong submitting your application'))
    } finally {
      setSubmitting(false)
    }
  }

  if (result) {
    return (
      <div className="max-w-lg mx-auto mt-16 p-6 border border-gray-200 rounded-lg shadow-sm bg-white">
        <h1 className="text-lg font-semibold text-gray-900 mb-2">Application submitted</h1>
        <p className="text-sm text-gray-600 mb-4">
          Thanks, {result.candidateName}. Here's what our automated screening found:
        </p>
        <dl className="space-y-2 text-sm">
          <div className="flex justify-between">
            <dt className="text-gray-500">Status</dt>
            <dd className="font-medium">{result.status}</dd>
          </div>
          {result.matchScore != null && (
            <div className="flex justify-between">
              <dt className="text-gray-500">Skill match score</dt>
              <dd className="font-medium">{result.matchScore}%</dd>
            </div>
          )}
        </dl>
        {result.screeningNotes && <p className="text-sm text-gray-600 mt-4">{result.screeningNotes}</p>}
        <button
          className="mt-6 w-full bg-blue-600 text-white rounded py-2 text-sm font-medium"
          onClick={() => setResult(null)}
        >
          Submit another application
        </button>
      </div>
    )
  }

  return (
    <div className="max-w-lg mx-auto mt-10 p-6 border border-gray-200 rounded-lg shadow-sm bg-white">
      <h1 className="text-lg font-semibold text-gray-900 mb-4">Apply for a role</h1>
      {jobsError && <p className="text-sm text-red-600 mb-3">{jobsError}</p>}
      <form onSubmit={handleSubmit} className="space-y-4" noValidate>
        <div>
          <label className="block text-sm font-medium text-gray-700">Job</label>
          <select
            className="mt-1 w-full rounded border border-gray-300 px-3 py-2 text-sm"
            value={form.jobId}
            onChange={(e) => updateField('jobId', e.target.value)}
          >
            <option value="">Select an open role…</option>
            {jobs.map((job) => (
              <option key={job.id} value={job.id}>
                {job.title}
              </option>
            ))}
          </select>
          {fieldErrors.jobId && <p className="text-sm text-red-600 mt-1">{fieldErrors.jobId}</p>}
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700">Full name</label>
          <input
            className="mt-1 w-full rounded border border-gray-300 px-3 py-2 text-sm"
            value={form.candidateName}
            onChange={(e) => updateField('candidateName', e.target.value)}
          />
          {fieldErrors.candidateName && <p className="text-sm text-red-600 mt-1">{fieldErrors.candidateName}</p>}
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700">Email</label>
          <input
            type="email"
            className="mt-1 w-full rounded border border-gray-300 px-3 py-2 text-sm"
            value={form.email}
            onChange={(e) => updateField('email', e.target.value)}
          />
          {fieldErrors.email && <p className="text-sm text-red-600 mt-1">{fieldErrors.email}</p>}
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700">Phone (optional)</label>
          <input
            className="mt-1 w-full rounded border border-gray-300 px-3 py-2 text-sm"
            value={form.phone}
            onChange={(e) => updateField('phone', e.target.value)}
          />
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700">Cover letter (optional)</label>
          <textarea
            rows={3}
            className="mt-1 w-full rounded border border-gray-300 px-3 py-2 text-sm"
            value={form.coverLetterText}
            onChange={(e) => updateField('coverLetterText', e.target.value)}
          />
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700">Resume (PDF or DOCX, max 5MB)</label>
          <input
            type="file"
            accept=".pdf,.docx"
            className="mt-1 w-full text-sm"
            onChange={(e) => setResumeFile(e.target.files?.[0] ?? null)}
          />
          {fieldErrors.resumeFile && <p className="text-sm text-red-600 mt-1">{fieldErrors.resumeFile}</p>}
        </div>

        {submitError && <p className="text-sm text-red-600">{submitError}</p>}

        <button
          type="submit"
          disabled={submitting}
          className="w-full bg-blue-600 text-white rounded py-2 text-sm font-medium disabled:opacity-50"
        >
          {submitting ? 'Submitting…' : 'Submit application'}
        </button>
      </form>
    </div>
  )
}

export default Apply
