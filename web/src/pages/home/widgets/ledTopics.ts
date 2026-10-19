/** Sibling-топики WB-LED: якорь — …/controls/RGB Strip. */

import { parseControlNum, siblingControlTopic } from './wbControls'

export const LED_BRIGHTNESS_MAX = 100
export const LED_SATURATION_MAX = 100
export const LED_HUE_MAX = 360

const CONTROLS = {
  brightness: 'RGB Strip Brightness',
  hue: 'RGB Strip Hue',
  saturation: 'RGB Strip Saturation',
  hueChanging: 'Hue Changing',
  white: 'Channel 4',
  whiteBrightness: 'Channel 4 Brightness',
} as const

export type LedTopics = {
  strip: string
  brightness: string
  hue: string
  saturation: string
  hueChanging: string
  white: string
  whiteBrightness: string
}

export type LedState = {
  topics: LedTopics
  on: boolean
  brightness: number | null
  hue: number | null
  saturation: number | null
  hueChanging: boolean
  whiteOn: boolean
  whiteBrightness: number | null
}

function ledTopics(mqttTopic: string): LedTopics {
  const strip = mqttTopic.trim()
  return {
    strip,
    brightness: siblingControlTopic(strip, CONTROLS.brightness),
    hue: siblingControlTopic(strip, CONTROLS.hue),
    saturation: siblingControlTopic(strip, CONTROLS.saturation),
    hueChanging: siblingControlTopic(strip, CONTROLS.hueChanging),
    white: siblingControlTopic(strip, CONTROLS.white),
    whiteBrightness: siblingControlTopic(strip, CONTROLS.whiteBrightness),
  }
}

export function clampLed(n: number, min: number, max: number): number {
  return Math.min(max, Math.max(min, Math.round(n)))
}

/** Плитка «вкл», если горит RGB или белый канал. */
export function isLedOn(values: Record<string, string>, mqttTopic: string): boolean {
  const s = readLedState(mqttTopic, values)
  return s.on || s.whiteOn
}

/** HSV → CSS rgb (H 0–360, S/V 0–100). */
export function hsvToCss(h: number, s: number, v: number): string {
  const hh = ((h % 360) + 360) % 360
  const ss = Math.min(100, Math.max(0, s)) / 100
  const vv = Math.min(100, Math.max(0, v)) / 100
  const c = vv * ss
  const x = c * (1 - Math.abs(((hh / 60) % 2) - 1))
  const m = vv - c
  let r = 0
  let g = 0
  let b = 0
  if (hh < 60) [r, g, b] = [c, x, 0]
  else if (hh < 120) [r, g, b] = [x, c, 0]
  else if (hh < 180) [r, g, b] = [0, c, x]
  else if (hh < 240) [r, g, b] = [0, x, c]
  else if (hh < 300) [r, g, b] = [x, 0, c]
  else [r, g, b] = [c, 0, x]
  const R = Math.round((r + m) * 255)
  const G = Math.round((g + m) * 255)
  const B = Math.round((b + m) * 255)
  return `rgb(${R}, ${G}, ${B})`
}

export function readLedState(mqttTopic: string, values: Record<string, string>): LedState {
  const topics = ledTopics(mqttTopic)
  return {
    topics,
    on: values[topics.strip] === '1',
    brightness: parseControlNum(values[topics.brightness]),
    hue: parseControlNum(values[topics.hue]),
    saturation: parseControlNum(values[topics.saturation]),
    hueChanging: values[topics.hueChanging] === '1',
    whiteOn: values[topics.white] === '1',
    whiteBrightness: parseControlNum(values[topics.whiteBrightness]),
  }
}
