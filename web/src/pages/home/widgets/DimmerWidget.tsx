import { useEffect, useRef, useState, type PointerEvent as ReactPointerEvent } from 'react'
import LightbulbOutlinedIcon from '@mui/icons-material/LightbulbOutlined'
import { Box, Typography } from '@mui/material'
import type { WidgetProps } from './types'
import {
  DIMMER_MAX,
  isDimmerOn,
  levelFromPointerY,
  parseDimmerLevel,
  rememberDimmerLevel,
  toggleDimmerPower,
} from './dimmerLogic'

export {
  isDimmerOn,
  parseDimmerLevel,
  toggleDimmerPower,
} from './dimmerLogic'

/**
 * Вертикальный диммер (Apple Home–like):
 * заливка снизу вверх, жест по треку = яркость, тап по лампе = вкл/выкл.
 */
export function DimmerWidget({ device, value, powerValue, sendCmd }: WidgetProps) {
  const remote = parseDimmerLevel(value)
  const [local, setLocal] = useState(remote ?? 0)
  const [dragging, setDragging] = useState(false)
  const trackRef = useRef<HTMLDivElement | null>(null)
  const unbind = useRef<(() => void) | null>(null)

  useEffect(() => {
    return () => unbind.current?.()
  }, [])

  useEffect(() => {
    if (!dragging && remote !== null) {
      setLocal(remote)
      rememberDimmerLevel(device.id, remote)
    }
  }, [remote, dragging, device.id])

  const powerTopic = device.powerTopic?.trim()
  const powered = isDimmerOn(powerValue, remote)
  // Во время драга заливка следует за пальцем, даже если power ещё off.
  const display = dragging ? local : powered ? (remote ?? local) : 0
  const fill = dragging || powered ? (dragging ? local : display) : 0

  function commitLevel(next: number) {
    setLocal(next)
    rememberDimmerLevel(device.id, next)
    sendCmd(device.mqttTopic, String(next))
    if (!powerTopic) return
    if (next > 0 && powerValue !== '1') sendCmd(powerTopic, '1')
    if (next === 0 && powerValue !== '0') sendCmd(powerTopic, '0')
  }

  function onTrackPointerDown(e: ReactPointerEvent) {
    if ((e.target as HTMLElement).closest('[data-power]')) return
    e.preventDefault()
    e.stopPropagation()
    const track = trackRef.current
    if (!track) return

    unbind.current?.()

    const apply = (clientY: number) => {
      const next = levelFromPointerY(clientY, track.getBoundingClientRect())
      setDragging(true)
      setLocal(next)
    }
    apply(e.clientY)

    const move = (ev: PointerEvent) => apply(ev.clientY)
    const up = (ev: PointerEvent) => {
      cleanup()
      setDragging(false)
      commitLevel(levelFromPointerY(ev.clientY, track.getBoundingClientRect()))
    }
    const cleanup = () => {
      window.removeEventListener('pointermove', move)
      window.removeEventListener('pointerup', up)
      window.removeEventListener('pointercancel', up)
      if (unbind.current === cleanup) unbind.current = null
    }
    unbind.current = cleanup
    window.addEventListener('pointermove', move)
    window.addEventListener('pointerup', up)
    window.addEventListener('pointercancel', up)
  }

  return (
    <Box
      ref={trackRef}
      onPointerDown={onTrackPointerDown}
      role="slider"
      aria-label={device.name}
      aria-valuemin={0}
      aria-valuemax={DIMMER_MAX}
      aria-valuenow={display}
      sx={{
        position: 'relative',
        width: '100%',
        height: '100%',
        minHeight: 0,
        borderRadius: 2,
        overflow: 'hidden',
        cursor: 'ns-resize',
        touchAction: 'none',
        bgcolor: 'rgba(8, 12, 18, 0.55)',
        border: 1,
        borderColor: powered || dragging ? 'rgba(94, 200, 216, 0.35)' : 'divider',
        userSelect: 'none',
      }}
    >
      <Box
        sx={{
          position: 'absolute',
          left: 0,
          right: 0,
          bottom: 0,
          height: `${fill}%`,
          background:
            fill > 0
              ? 'linear-gradient(180deg, rgba(138, 220, 235, 0.95) 0%, rgba(62, 168, 186, 0.9) 100%)'
              : 'transparent',
          transition: dragging ? 'none' : 'height 160ms ease, background 160ms ease',
          boxShadow: fill > 8 ? '0 0 24px rgba(94, 200, 216, 0.35)' : 'none',
        }}
      />

      <Typography
        sx={{
          position: 'absolute',
          top: 6,
          left: 0,
          right: 0,
          zIndex: 1,
          textAlign: 'center',
          fontWeight: 700,
          fontSize: '0.78rem',
          fontVariantNumeric: 'tabular-nums',
          letterSpacing: '-0.02em',
          color: fill > 72 ? 'primary.contrastText' : 'text.primary',
          textShadow: fill > 72 ? 'none' : '0 1px 2px rgba(0,0,0,0.45)',
          pointerEvents: 'none',
        }}
      >
        {display}%
      </Typography>

      <Box
        component="button"
        type="button"
        data-power
        aria-label={powered ? `${device.name}: выключить` : `${device.name}: включить`}
        onClick={(e) => {
          e.stopPropagation()
          toggleDimmerPower(device, value, powerValue, sendCmd)
        }}
        onPointerDown={(e) => e.stopPropagation()}
        sx={{
          all: 'unset',
          boxSizing: 'border-box',
          position: 'absolute',
          left: '50%',
          top: '50%',
          transform: 'translate(-50%, -50%)',
          zIndex: 2,
          width: 44,
          height: 44,
          borderRadius: '50%',
          display: 'grid',
          placeItems: 'center',
          cursor: 'pointer',
          bgcolor: powered || dragging ? 'rgba(6, 16, 24, 0.35)' : 'rgba(232, 238, 246, 0.1)',
          color: powered || dragging ? '#fff' : 'text.secondary',
          backdropFilter: 'blur(6px)',
          transition: 'background 140ms ease, color 140ms ease',
          '&:hover': { filter: 'brightness(1.08)' },
          '&:active': { transform: 'translate(-50%, -50%) scale(0.96)' },
        }}
      >
        <LightbulbOutlinedIcon sx={{ fontSize: 26 }} />
      </Box>
    </Box>
  )
}
