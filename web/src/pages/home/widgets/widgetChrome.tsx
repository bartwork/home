import { Box, Typography, type SxProps, type Theme } from '@mui/material'
import { useEffect, useRef, type MouseEvent, type PointerEvent, type ReactNode } from 'react'

/** Зона касания ~48px — комфортно пальцем. */
export const WIDGET_BTN = 48
/** Кружок текущей температуры — тот же размер, что кнопки. */
export const TEMP_CIRCLE = WIDGET_BTN

export function stopBubble(e: MouseEvent | PointerEvent) {
  e.stopPropagation()
}

export function RoundBtn({
  ariaLabel,
  onClick,
  disabled,
  active,
  children,
  sx,
}: {
  ariaLabel: string
  onClick: () => void
  disabled?: boolean
  active?: boolean
  children: ReactNode
  sx?: SxProps<Theme>
}) {
  return (
    <Box
      component="button"
      type="button"
      aria-label={ariaLabel}
      disabled={disabled}
      onClick={(e) => {
        stopBubble(e)
        if (!disabled) onClick()
      }}
      onPointerDown={stopBubble}
      sx={[
        {
          all: 'unset',
          boxSizing: 'border-box',
          width: WIDGET_BTN,
          height: WIDGET_BTN,
          flexShrink: 0,
          borderRadius: '50%',
          display: 'grid',
          placeItems: 'center',
          cursor: disabled ? 'default' : 'pointer',
          opacity: disabled ? 0.35 : 1,
          bgcolor: active ? 'primary.main' : 'rgba(232, 238, 246, 0.12)',
          color: active ? 'primary.contrastText' : 'text.secondary',
          boxShadow: active ? '0 2px 12px rgba(94, 200, 216, 0.4)' : 'none',
          WebkitTapHighlightColor: 'transparent',
          '&:active': disabled ? undefined : { transform: 'scale(0.94)' },
        },
        ...(Array.isArray(sx) ? sx : sx ? [sx] : []),
      ]}
    >
      {children}
    </Box>
  )
}

/** Удержание пальца — повтор шага. */
export function HoldRound({
  ariaLabel,
  onStep,
  disabled,
  children,
}: {
  ariaLabel: string
  onStep: () => void
  disabled?: boolean
  children: ReactNode
}) {
  const timers = useRef<{ delay?: number; repeat?: number }>({})

  function clear() {
    if (timers.current.delay !== undefined) window.clearTimeout(timers.current.delay)
    if (timers.current.repeat !== undefined) window.clearInterval(timers.current.repeat)
    timers.current = {}
  }

  useEffect(() => clear, [])

  return (
    <Box
      component="button"
      type="button"
      aria-label={ariaLabel}
      disabled={disabled}
      onPointerDown={(e) => {
        stopBubble(e)
        if (disabled) return
        e.currentTarget.setPointerCapture(e.pointerId)
        onStep()
        clear()
        timers.current.delay = window.setTimeout(() => {
          timers.current.repeat = window.setInterval(onStep, 120)
        }, 380)
      }}
      onPointerUp={clear}
      onPointerCancel={clear}
      onPointerLeave={clear}
      onClick={(e) => stopBubble(e)}
      sx={{
        all: 'unset',
        boxSizing: 'border-box',
        width: WIDGET_BTN,
        height: WIDGET_BTN,
        flexShrink: 0,
        borderRadius: '50%',
        display: 'grid',
        placeItems: 'center',
        cursor: disabled ? 'default' : 'pointer',
        opacity: disabled ? 0.35 : 1,
        bgcolor: 'rgba(232, 238, 246, 0.12)',
        color: 'text.secondary',
        WebkitTapHighlightColor: 'transparent',
        touchAction: 'none',
        '&:active': disabled ? undefined : { transform: 'scale(0.94)', bgcolor: 'rgba(94, 200, 216, 0.22)' },
      }}
    >
      {children}
    </Box>
  )
}

/** Температура в кружке (текущая / уставка). */
export function TempCircle({
  value,
  size = TEMP_CIRCLE,
  accent,
  ariaLabel,
}: {
  value: string
  size?: number
  accent?: boolean
  ariaLabel?: string
}) {
  return (
    <Box
      role="img"
      aria-label={ariaLabel}
      sx={{
        width: size,
        height: size,
        flexShrink: 0,
        borderRadius: '50%',
        display: 'grid',
        placeItems: 'center',
        justifySelf: 'end',
        bgcolor: accent ? 'rgba(94, 200, 216, 0.22)' : 'rgba(232, 238, 246, 0.1)',
        border: 1,
        borderColor: accent ? 'rgba(94, 200, 216, 0.5)' : 'rgba(232, 238, 246, 0.14)',
        boxShadow: accent ? '0 2px 14px rgba(94, 200, 216, 0.28)' : 'none',
      }}
    >
      <Typography
        sx={{
          fontWeight: 700,
          fontSize: '0.95rem',
          lineHeight: 1,
          fontVariantNumeric: 'tabular-nums',
          letterSpacing: '-0.03em',
          color: accent ? 'primary.light' : 'text.primary',
        }}
      >
        {value}
        <Typography component="span" sx={{ fontSize: '0.7rem', fontWeight: 600, ml: 0.15 }}>
          °
        </Typography>
      </Typography>
    </Box>
  )
}

export function ChipBtn({
  children,
  ariaLabel,
  onClick,
  active,
  disabled,
}: {
  children: ReactNode
  ariaLabel: string
  onClick: () => void
  active?: boolean
  disabled?: boolean
}) {
  return (
    <Box
      component="button"
      type="button"
      aria-label={ariaLabel}
      disabled={disabled}
      onClick={(e) => {
        stopBubble(e)
        if (!disabled) onClick()
      }}
      onPointerDown={stopBubble}
      sx={{
        all: 'unset',
        boxSizing: 'border-box',
        flex: 1,
        minWidth: 0,
        minHeight: 36,
        px: 0.6,
        py: 0.5,
        borderRadius: 1.25,
        textAlign: 'center',
        fontSize: '0.7rem',
        fontWeight: 600,
        lineHeight: 1.2,
        display: 'inline-flex',
        alignItems: 'center',
        justifyContent: 'center',
        gap: 0.35,
        cursor: disabled ? 'default' : 'pointer',
        opacity: disabled ? 0.45 : 1,
        bgcolor: active ? 'rgba(94, 200, 216, 0.28)' : 'rgba(232, 238, 246, 0.08)',
        color: active ? 'primary.light' : 'text.secondary',
        border: 1,
        borderColor: active ? 'rgba(94, 200, 216, 0.45)' : 'transparent',
        WebkitTapHighlightColor: 'transparent',
        transition: 'background 120ms ease, border-color 120ms ease',
        '&:active': disabled ? undefined : { transform: 'scale(0.97)' },
      }}
    >
      {children}
    </Box>
  )
}
