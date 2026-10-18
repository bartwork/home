import type { Device } from '../../../shared/api/types'

export const DIMMER_MAX = 100

const lastLevels = new Map<number, number>()

export function parseDimmerLevel(value: string): number | null {
  const trimmed = value.trim()
  if (!trimmed || trimmed === '—') return null
  const n = Number.parseFloat(trimmed)
  if (!Number.isFinite(n)) return null
  return Math.min(DIMMER_MAX, Math.max(0, Math.round(n)))
}

export function isDimmerOn(powerValue: string | undefined, level: number | null): boolean {
  if (powerValue === '1') return true
  if (powerValue === '0') return false
  return (level ?? 0) > 0
}

export function rememberDimmerLevel(deviceId: number, level: number) {
  if (level > 0) lastLevels.set(deviceId, level)
}

export function recalledDimmerLevel(deviceId: number): number {
  return lastLevels.get(deviceId) ?? 80
}

export function toggleDimmerPower(
  device: Device,
  value: string,
  powerValue: string | undefined,
  sendCmd: (topic: string, payload: string) => void,
) {
  const remote = parseDimmerLevel(value)
  const on = isDimmerOn(powerValue, remote)
  const powerTopic = device.powerTopic?.trim()

  if (on) {
    if (remote !== null && remote > 0) rememberDimmerLevel(device.id, remote)
    if (powerTopic) sendCmd(powerTopic, '0')
    else sendCmd(device.mqttTopic, '0')
    return
  }

  const level = remote !== null && remote > 0 ? remote : recalledDimmerLevel(device.id)
  if (powerTopic) {
    sendCmd(powerTopic, '1')
    if ((remote ?? 0) <= 0) sendCmd(device.mqttTopic, String(level))
  } else {
    sendCmd(device.mqttTopic, String(level))
  }
}

/** Яркость по Y: низ трека = 0%, верх = 100%. */
export function levelFromPointerY(clientY: number, rect: DOMRect): number {
  const t = (rect.bottom - clientY) / rect.height
  return Math.min(DIMMER_MAX, Math.max(0, Math.round(t * DIMMER_MAX)))
}
