/** Тёплый пол: реле (mqttTopic), датчик (powerTopic), уставка/режим — /devices/home/... */

import { parseControlNum } from './wbControls'

export type FloorMode = 'off' | 'heat' | 'auto'

export type FloorState = {
  relay: string
  sensor: string
  setpointTopic: string
  modeTopic: string
  on: boolean
  temp: number | null
  setpoint: number
  mode: FloorMode
}

export const FLOOR_SETPOINT_MIN = 15
export const FLOOR_SETPOINT_MAX = 35
export const FLOOR_SETPOINT_DEFAULT = 26

export function floorSetpointTopic(deviceId: number): string {
  return `/devices/home/controls/floor_${deviceId}_setpoint`
}

export function floorModeTopic(deviceId: number): string {
  return `/devices/home/controls/floor_${deviceId}_mode`
}

export function clampFloorSetpoint(n: number): number {
  return Math.min(FLOOR_SETPOINT_MAX, Math.max(FLOOR_SETPOINT_MIN, Math.round(n * 2) / 2))
}

export function parseFloorMode(raw: string | undefined): FloorMode {
  switch ((raw ?? '').trim().toLowerCase()) {
    case 'off':
      return 'off'
    case 'heat':
      return 'heat'
    default:
      return 'auto'
  }
}

export function nextFloorMode(mode: FloorMode): FloorMode {
  if (mode === 'off') return 'heat'
  if (mode === 'heat') return 'auto'
  return 'off'
}

export function readFloorState(
  deviceId: number,
  mqttTopic: string,
  values: Record<string, string>,
  powerTopic?: string,
): FloorState {
  const relay = mqttTopic.trim()
  const sensor = (powerTopic ?? '').trim()
  const setpointTopic = floorSetpointTopic(deviceId)
  const modeTopic = floorModeTopic(deviceId)
  const sp = parseControlNum(values[setpointTopic])
  return {
    relay,
    sensor,
    setpointTopic,
    modeTopic,
    on: (values[relay] ?? '') === '1',
    temp: sensor ? parseControlNum(values[sensor]) : null,
    setpoint: sp ?? FLOOR_SETPOINT_DEFAULT,
    mode: parseFloorMode(values[modeTopic]),
  }
}

export function formatFloorTemp(n: number | null): string {
  if (n === null) return '—'
  return Number.isInteger(n) ? String(n) : n.toFixed(1)
}
