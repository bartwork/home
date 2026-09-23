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
}

export type LoginResponse = {
  token: string
  user: User
}
