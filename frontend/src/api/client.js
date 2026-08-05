import axios from 'axios'

export const jobServiceClient = axios.create({
  baseURL: import.meta.env.VITE_JOB_SERVICE_URL || 'http://localhost:8081',
})

export const applicationServiceClient = axios.create({
  baseURL: import.meta.env.VITE_APPLICATION_SERVICE_URL || 'http://localhost:8082',
})
