/** Sibling-топики ONOKOM: якорь — …/controls/Active. */

import { parseControlNum, siblingControlTopic } from './wbControls'

export const TARGET_TEMP_MIN = 16
export const TARGET_TEMP_MAX = 30

export type LabeledValue = { value: number; label: string }

/** Mode: 1 Heat … 5 Fan. */
export const CLIMATE_MODES: LabeledValue[] = [
  { value: 1, label: 'Нагрев' },
  { value: 2, label: 'Охл.' },
  { value: 3, label: 'Авто' },
  { value: 4, label: 'Сушка' },
  { value: 5, label: 'Вент.' },
]

/** Smart fan speed: 0 Auto … 5 Turbo. */
export const CLIMATE_FANS: LabeledValue[] = [
  { value: 0, label: 'Авто' },
  { value: 1, label: 'Тихий' },
  { value: 2, label: '1' },
  { value: 3, label: '2' },
  { value: 4, label: '3' },
  { value: 5, label: 'Турбо' },
]

export const FAN_QUIET = 1
export const FAN_TURBO = 5

/** Vanes swing. */
export const CLIMATE_SWINGS: LabeledValue[] = [
  { value: 0, label: 'Качание стоп' },
  { value: 1, label: 'Качание оба' },
  { value: 2, label: 'Качание верт.' },
  { value: 3, label: 'Качание гориз.' },
]

/** Horizontal vanes (верх/низ). */
export const CLIMATE_HORIZONTAL: LabeledValue[] = [
  { value: 0, label: 'Гориз. стоп' },
  { value: 1, label: 'Гориз. кач.' },
  { value: 2, label: 'Гориз. низ' },
  { value: 3, label: 'Гориз. ниже' },
  { value: 4, label: 'Гориз. ср−' },
  { value: 5, label: 'Гориз. сер.' },
  { value: 6, label: 'Гориз. ср+' },
  { value: 7, label: 'Гориз. выше' },
  { value: 8, label: 'Гориз. верх' },
]

/** Vertical vanes (лево/право). */
export const CLIMATE_VERTICAL: LabeledValue[] = [
  { value: 0, label: 'Верт. стоп' },
  { value: 1, label: 'Верт. кач.' },
  { value: 2, label: 'Верт. лево' },
  { value: 3, label: 'Верт. левее' },
  { value: 4, label: 'Верт. сер.' },
  { value: 5, label: 'Верт. правее' },
  { value: 6, label: 'Верт. право' },
]

const CONTROL_KEYS = [
  'mode',
  'target',
  'indoor',
  'fan',
  'swing',
  'horizontal',
  'vertical',
  'eco',
  'sleep',
] as const

type ControlKey = (typeof CONTROL_KEYS)[number]

const CONTROL_NAMES: Record<ControlKey, string> = {
  mode: 'Mode',
  target: 'Target temperature',
  indoor: 'Indoor air temperature',
  fan: 'Smart fan speed',
  swing: 'Vanes swing',
  horizontal: 'Horizontal vanes',
  vertical: 'Vertical vanes',
  eco: 'Eco mode',
  sleep: 'Sleep mode',
}

export type ClimateTopics = { active: string } & Record<ControlKey, string>

export type ClimateState = {
  topics: ClimateTopics
  on: boolean
  target: number | null
  indoor: number | null
  mode: number | null
  fan: number | null
  swing: number | null
  horizontal: number | null
  vertical: number | null
  ecoOn: boolean
  sleepOn: boolean
  quietOn: boolean
  turboOn: boolean
}

export function climateTopics(mqttTopic: string): ClimateTopics {
  const active = mqttTopic.trim()
  const out = { active } as ClimateTopics
  for (const key of CONTROL_KEYS) {
    out[key] = siblingControlTopic(active, CONTROL_NAMES[key])
  }
  return out
}

export function formatTemp(n: number | null, digits = 0): string {
  if (n === null) return '—'
  return digits > 0 ? n.toFixed(digits) : String(Math.round(n))
}

export function clampTarget(n: number): number {
  return Math.min(TARGET_TEMP_MAX, Math.max(TARGET_TEMP_MIN, Math.round(n)))
}

export function labelByValue(list: LabeledValue[], v: number | null): string | undefined {
  if (v === null) return undefined
  return list.find((item) => item.value === v)?.label
}

export function nextInCycle(list: LabeledValue[], current: number | null): number {
  if (list.length === 0) return 0
  if (current === null) return list[0].value
  const i = list.findIndex((item) => item.value === current)
  if (i < 0) return list[0].value
  return list[(i + 1) % list.length].value
}

/** Тихий/Турбо: повторный тап → Авто. */
export function toggleFanPreset(current: number | null, preset: number): number {
  return current === preset ? 0 : preset
}

export function isClimateOn(values: Record<string, string>, mqttTopic: string): boolean {
  const topics = climateTopics(mqttTopic)
  return (values[topics.active] ?? values[mqttTopic]) === '1'
}

export function readClimateState(
  mqttTopic: string,
  values: Record<string, string>,
): ClimateState {
  const topics = climateTopics(mqttTopic)
  const fan = parseControlNum(values[topics.fan])
  return {
    topics,
    on: (values[topics.active] ?? values[mqttTopic]) === '1',
    target: parseControlNum(values[topics.target]),
    indoor: parseControlNum(values[topics.indoor]),
    mode: parseControlNum(values[topics.mode]),
    fan,
    swing: parseControlNum(values[topics.swing]),
    horizontal: parseControlNum(values[topics.horizontal]),
    vertical: parseControlNum(values[topics.vertical]),
    ecoOn: values[topics.eco] === '1',
    sleepOn: values[topics.sleep] === '1',
    quietOn: fan === FAN_QUIET,
    turboOn: fan === FAN_TURBO,
  }
}
