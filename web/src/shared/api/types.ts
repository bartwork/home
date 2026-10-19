export type User = {
  id: number
  login: string
  lastName: string
  firstName: string
  secondName: string
  email: string
  phone: string
  lastAuthAt?: string
  active: boolean
}

export type DeviceType = 'switch' | 'range' | 'sensor' | 'raw' | 'climate' | 'led'

export type DeviceIcon =
  | ''
  | 'light'
  | 'dimmer'
  | 'switch'
  | 'fan'
  | 'sensor'
  | 'socket'
  | 'thermo'
  | 'ac'

export type Device = {
  id: number
  name: string
  mqttTopic: string
  deviceType: DeviceType | string
  unit: string
  powerTopic: string
  roomId?: number
  roomName: string
  icon: string
  x: number
  y: number
  w: number
  h: number
  active: boolean
  createdAt: string
  wbDevice: string
  wbControl: string
  source: string
}

export type Room = {
  id: number
  name: string
  x: number
  y: number
  w: number
  h: number
  sortOrder: number
}

export type Discovered = {
  id: number
  mqttTopic: string
  wbDevice: string
  wbControl: string
  title: string
  suggestedType: string
  unit: string
  lastPayload: string
  lastSeenAt: string
  status: string
}

export type LoginResponse = {
  token: string
  user: User
}

export type ColorFavorite = {
  id: number
  hue: number
  saturation: number
  brightness: number
  sortOrder: number
  createdAt: string
}
