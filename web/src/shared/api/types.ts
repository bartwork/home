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

export type Device = {
  id: number
  name: string
  mqttTopic: string
  deviceType: string
  unit?: string
  active: boolean
  wbDevice?: string
  wbControl?: string
  source?: string
}

export type Discovered = {
  id: number
  mqttTopic: string
  wbDevice: string
  wbControl: string
  title: string
  suggestedType: string
  unit?: string
  lastPayload?: string
  lastSeenAt?: string
  status: string
}

export type LoginResponse = {
  token: string
  user: User
}
