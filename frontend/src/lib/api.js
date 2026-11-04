const BASE = '/api'

export class ApiError extends Error {
  constructor(message, status) {
    super(message)
    this.status = status
  }
}

export function getToken() {
  return localStorage.getItem('token')
}

export async function api(path, { method = 'GET', body, auth = true } = {}) {
  const headers = {}
  if (body !== undefined) headers['Content-Type'] = 'application/json'
  const token = getToken()
  if (auth && token) headers.Authorization = `Bearer ${token}`
  let res
  try {
    res = await fetch(BASE + path, { method, headers, body: body !== undefined ? JSON.stringify(body) : undefined })
  } catch {
    throw new ApiError('No pudimos conectar con el servidor. Revisa tu conexión.', 0)
  }
  if (res.status === 204) return null
  const data = await res.json().catch(() => null)
  if (!res.ok) {
    const msg = data?.errorDTO?.errorMessage || `Error ${res.status}`
    throw new ApiError(msg, res.status)
  }
  return data
}

export const apiBaseUrl = () => `${window.location.origin}${BASE}`
