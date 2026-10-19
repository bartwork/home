/** Тёплый пол: якорь — реле (mqttTopic), датчик — powerTopic. */

import { parseControlNum } from './wbControls'

export type FloorState = {
  relay: string
  sensor: string
  on: boolean
  temp: number | null
}

export function readFloorState(
  mqttTopic: string,
  values: Record<string, string>,
  powerTopic?: string,
): FloorState {
  const relay = mqttTopic.trim()
  const sensor = (powerTopic ?? '').trim()
  return {
    relay,
    sensor,
    on: (values[relay] ?? '') === '1',
    temp: sensor ? parseControlNum(values[sensor]) : null,
  }
}

export function formatFloorTemp(n: number | null): string {
  if (n === null) return '—'
  return Number.isInteger(n) ? String(n) : n.toFixed(1)
}
