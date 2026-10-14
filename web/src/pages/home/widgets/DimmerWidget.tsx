import { useEffect, useRef, useState } from 'react'
import { Box, Slider } from '@mui/material'
import type { Device } from '../../../shared/api/types'
import type { WidgetProps } from './types'

const MAX = 100

function parseLevel(value: string): number | null {
  const trimmed = value.trim()
  if (!trimmed || trimmed === '—') return null
  const n = Number.parseFloat(trimmed)
  if (!Number.isFinite(n)) return null
  return Math.min(MAX, Math.max(0, Math.round(n)))
}

function sliderValue(v: number | number[]): number {
  return Array.isArray(v) ? (v[0] ?? 0) : v
}

export function isDimmerOn(powerValue: string | undefined, level: number | null): boolean {
  if (powerValue === '1') return true
  if (powerValue === '0') return false
  return (level ?? 0) > 0
}

export function parseDimmerLevel(value: string): number | null {
  return parseLevel(value)
}

const lastLevels = new Map<number, number>()

export function toggleDimmerPower(
  device: Device,
  value: string,
  powerValue: string | undefined,
  sendCmd: (topic: string, payload: string) => void,
) {
  const remote = parseLevel(value)
  const on = isDimmerOn(powerValue, remote)
  const powerTopic = device.powerTopic?.trim()
  const remembered = lastLevels.get(device.id) ?? 80

  if (on) {
    if (remote !== null && remote > 0) lastLevels.set(device.id, remote)
    if (powerTopic) sendCmd(powerTopic, '0')
    else sendCmd(device.mqttTopic, '0')
    return
  }

  const level = remote !== null && remote > 0 ? remote : remembered
  if (powerTopic) {
    sendCmd(powerTopic, '1')
    if ((remote ?? 0) <= 0) sendCmd(device.mqttTopic, String(level))
  } else {
    sendCmd(device.mqttTopic, String(level))
  }
}

/** Компактный слайдер в фиксированной полосе 26px. */
export function DimmerWidget({ device, value, powerValue, sendCmd, onPreview }: WidgetProps) {
  const remote = parseLevel(value)
  const [local, setLocal] = useState(remote ?? 0)
  const [dragging, setDragging] = useState(false)
  const lastOn = useRef(remote && remote > 0 ? remote : 80)

  useEffect(() => {
    if (!dragging && remote !== null) {
      setLocal(remote)
      onPreview?.(null)
      if (remote > 0) {
        lastOn.current = remote
        lastLevels.set(device.id, remote)
      }
    }
  }, [remote, dragging, device.id, onPreview])

  const powerTopic = device.powerTopic?.trim()
  const on = isDimmerOn(powerValue, remote)

  function commitLevel(next: number) {
    setLocal(next)
    onPreview?.(null)
    if (next > 0) {
      lastOn.current = next
      lastLevels.set(device.id, next)
    }
    sendCmd(device.mqttTopic, String(next))
    if (!powerTopic) return
    if (next > 0 && powerValue !== '1') sendCmd(powerTopic, '1')
    if (next === 0 && powerValue !== '0') sendCmd(powerTopic, '0')
  }

  return (
    <Box
      sx={{
        display: 'flex',
        alignItems: 'center',
        width: '100%',
        height: 26,
        minWidth: 0,
        px: 0.25,
      }}
    >
      <Slider
        size="small"
        value={local}
        min={0}
        max={MAX}
        step={1}
        aria-label={device.name}
        onChange={(_, v) => {
          const next = sliderValue(v)
          setDragging(true)
          setLocal(next)
          onPreview?.(next)
        }}
        onChangeCommitted={(_, v) => {
          setDragging(false)
          commitLevel(sliderValue(v))
        }}
        sx={{
          width: '100%',
          minWidth: 0,
          height: 4,
          padding: '11px 0 !important',
          margin: 0,
          opacity: on || !powerTopic ? 1 : 0.4,
          '& .MuiSlider-rail': { opacity: 0.3, height: 3 },
          '& .MuiSlider-track': { height: 3, border: 0 },
          '& .MuiSlider-thumb': {
            width: 12,
            height: 12,
            '&::before': { boxShadow: 'none' },
          },
        }}
      />
    </Box>
  )
}
