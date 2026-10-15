import type { Device, Discovered, LoginResponse, Room, User } from './types'

export type { Device, Discovered, LoginResponse, Room, User } from './types'

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

export async function listDevices(): Promise<Device[]> {
  const res = await api<{ devices?: Device[] }>('/api/v1/devices')
  return res.devices ?? []
}

export function updateDevice(device: Device) {
  return api<{ device: Device }>(`/api/v1/devices/${device.id}`, {
    method: 'PUT',
    body: JSON.stringify({
      id: device.id,
      name: device.name,
      mqttTopic: device.mqttTopic,
      deviceType: device.deviceType,
      unit: device.unit,
      powerTopic: device.powerTopic ?? '',
      roomId: device.roomId ?? 0,
      icon: device.icon,
      x: device.x,
      y: device.y,
      w: device.w,
      h: device.h,
      active: device.active,
    }),
  })
}

export function deleteDevice(id: number) {
  return api<void>(`/api/v1/devices/${id}`, { method: 'DELETE' })
}

export async function listRooms(): Promise<Room[]> {
  const res = await api<{ rooms?: Room[] }>('/api/v1/rooms')
  return res.rooms ?? []
}

export function createRoom(room: Omit<Room, 'id'>) {
  return api<{ room: Room }>('/api/v1/rooms', {
    method: 'POST',
    body: JSON.stringify(room),
  })
}

export function updateRoom(room: Room) {
  return api<{ room: Room }>(`/api/v1/rooms/${room.id}`, {
    method: 'PUT',
    body: JSON.stringify(room),
  })
}

export function deleteRoom(id: number) {
  return api<void>(`/api/v1/rooms/${id}`, { method: 'DELETE' })
}

export async function listDiscovered(status: 'new' | 'ignored' = 'new'): Promise<Discovered[]> {
  const q = status === 'ignored' ? '?status=ignored' : ''
  const res = await api<{ discovered?: Discovered[] }>(`/api/v1/discovered${q}`)
  return res.discovered ?? []
}

export async function countDiscovered(): Promise<number> {
  const res = await api<{ count?: number }>('/api/v1/discovered/count')
  return res.count ?? 0
}

export function acceptDiscovered(
  id: number,
  opts?: { name?: string; roomId?: number; icon?: string },
) {
  return api<{ device: Device }>(`/api/v1/discovered/${id}/accept`, {
    method: 'POST',
    body: JSON.stringify(opts ?? {}),
  })
}

export function ignoreDiscovered(id: number) {
  return api<void>(`/api/v1/discovered/${id}/ignore`, { method: 'POST' })
}

export function restoreDiscovered(id: number) {
  return api<void>(`/api/v1/discovered/${id}/restore`, { method: 'POST' })
}

/** Принимает switch/range/sensor; raw и прочее скрывает (ignoreRaw по умолчанию). */
export function acceptAllDiscovered(opts?: { type?: string; ignoreRaw?: boolean }) {
  const params = new URLSearchParams()
  if (opts?.type) params.set('type', opts.type)
  if (opts?.ignoreRaw === false) params.set('ignoreRaw', '0')
  const q = params.toString()
  return api<{ accepted: number; ignored: number; failed: number }>(
    `/api/v1/discovered/accept-all${q ? `?${q}` : ''}`,
    { method: 'POST' },
  )
}
