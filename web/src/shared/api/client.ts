import type { Device, LoginResponse, User } from './types'

export type { Device, LoginResponse, User } from './types'

const TOKEN_KEY = 'home.token'
const USER_KEY = 'home.user'

export function getToken(): string | null {
  return sessionStorage.getItem(TOKEN_KEY)
}

export function getStoredUser(): User | null {
  const raw = sessionStorage.getItem(USER_KEY)
  if (!raw) return null
  try {
    return JSON.parse(raw) as User
  } catch {
    sessionStorage.removeItem(USER_KEY)
    return null
  }
}

export function setSession(token: string | null, user: User | null = null) {
  if (!token) {
    sessionStorage.removeItem(TOKEN_KEY)
    sessionStorage.removeItem(USER_KEY)
    return
  }
  sessionStorage.setItem(TOKEN_KEY, token)
  if (user) {
    sessionStorage.setItem(USER_KEY, JSON.stringify(user))
  }
}

export class ApiError extends Error {
  status: number

  constructor(status: number, message: string) {
    super(message)
    this.status = status
  }
}

export async function api<T>(path: string, init: RequestInit = {}): Promise<T> {
  const headers = new Headers(init.headers)
  if (!headers.has('Content-Type') && init.body) {
    headers.set('Content-Type', 'application/json')
  }
  const token = getToken()
  if (token) {
    headers.set('Authorization', `Bearer ${token}`)
  }

  const res = await fetch(path, { ...init, headers })
  if (!res.ok) {
    if (res.status === 401 && !path.includes('/auth/login')) {
      setSession(null)
      if (window.location.pathname !== '/login') {
        window.location.assign('/login')
      }
    }
    let message = res.statusText
    try {
      const body = (await res.json()) as { msg?: string; message?: string }
      message = body.msg || body.message || message
    } catch {
      /* ignore */
    }
    throw new ApiError(res.status, message)
  }
  if (res.status === 204) {
    return undefined as T
  }
  return res.json() as Promise<T>
}

export function login(login: string, password: string) {
  return api<LoginResponse>('/api/v1/auth/login', {
    method: 'POST',
    body: JSON.stringify({ login, password }),
  })
}

export function listDevices() {
  return api<{ devices: Device[] }>('/api/v1/devices')
}
