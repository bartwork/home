/** Sibling-топики WB-MSW: якорь — …/controls/Temperature. Тип UI: air. */

import { parseControlNum, siblingControlTopic } from './wbControls'

/** Пороги как в internal/air/level.go (+ гистерезис). */
export const CO2_GOOD_MAX = 800
export const CO2_HIGH_MIN = 1200
export const CO2_GOOD_LEAVE = 900
export const CO2_HIGH_LEAVE = 1100

const CONTROL_KEYS = [
  'temperature',
  'humidity',
  'co2',
  'voc',
  'illuminance',
  'sound',
  'motion',
  'maxMotion',
] as const

type ControlKey = (typeof CONTROL_KEYS)[number]

const CONTROL_NAMES: Record<ControlKey, string> = {
  temperature: 'Temperature',
  humidity: 'Humidity',
  co2: 'CO2',
  voc: 'Air Quality (VOC)',
  illuminance: 'Illuminance',
  sound: 'Sound Level',
  motion: 'Current Motion',
  maxMotion: 'Max Motion',
}

export type AirTopics = Record<ControlKey, string>
export type Co2Level = 'good' | 'ok' | 'high'

export type AirState = {
  topics: AirTopics
  temperature: number | null
  humidity: number | null
  co2: number | null
  voc: number | null
  illuminance: number | null
  sound: number | null
  motion: number | null
  maxMotion: number | null
  motionOn: boolean
  co2Level: Co2Level
}

/** Последний уровень по якорю — как гистерезис LED на бэкенде. */
const co2Prev = new Map<string, Co2Level>()

export function airTopics(mqttTopic: string): AirTopics {
  const anchor = mqttTopic.trim()
  const out = {} as AirTopics
  for (const key of CONTROL_KEYS) {
    out[key] = siblingControlTopic(anchor, CONTROL_NAMES[key])
  }
  return out
}

export function nextCo2Level(prev: Co2Level | '', ppm: number): Co2Level {
  if (prev === 'high' && ppm >= CO2_HIGH_LEAVE) return 'high'
  if (prev === 'good' && ppm < CO2_GOOD_LEAVE) return 'good'
  if (ppm >= CO2_HIGH_MIN) return 'high'
  if (ppm < CO2_GOOD_MAX) return 'good'
  return 'ok'
}

export function co2LevelFor(mqttTopic: string, ppm: number | null): Co2Level {
  if (ppm === null) return 'ok'
  const prev = co2Prev.get(mqttTopic) ?? ''
  const next = nextCo2Level(prev, ppm)
  co2Prev.set(mqttTopic, next)
  return next
}

export function co2Label(level: Co2Level): string {
  switch (level) {
    case 'good':
      return 'норма'
    case 'ok':
      return 'душновато'
    case 'high':
      return 'проветри'
  }
}

export function formatAirNum(n: number | null, digits = 0): string {
  if (n === null) return '—'
  if (digits <= 0) return String(Math.round(n))
  return n.toFixed(digits)
}

export function isMotionOn(motion: number | null, maxMotion: number | null): boolean {
  return (motion !== null && motion > 0) || (maxMotion !== null && maxMotion > 0)
}

export function isAirAttention(values: Record<string, string>, mqttTopic: string): boolean {
  return readAirState(mqttTopic, values).co2Level === 'high'
}

export function readAirState(mqttTopic: string, values: Record<string, string>): AirState {
  const topics = airTopics(mqttTopic)
  const temperature =
    parseControlNum(values[topics.temperature]) ?? parseControlNum(values[mqttTopic])
  const humidity = parseControlNum(values[topics.humidity])
  const co2 = parseControlNum(values[topics.co2])
  const voc = parseControlNum(values[topics.voc])
  const illuminance = parseControlNum(values[topics.illuminance])
  const sound = parseControlNum(values[topics.sound])
  const motion = parseControlNum(values[topics.motion])
  const maxMotion = parseControlNum(values[topics.maxMotion])
  return {
    topics,
    temperature,
    humidity,
    co2,
    voc,
    illuminance,
    sound,
    motion,
    maxMotion,
    motionOn: isMotionOn(motion, maxMotion),
    co2Level: co2LevelFor(mqttTopic, co2),
  }
}
