import type { MouseEvent, PointerEvent, ReactNode } from 'react'
import { Box, type SxProps, type Theme } from '@mui/material'

export const WIDGET_BTN = 28

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
          bgcolor: active ? 'primary.main' : 'rgba(232, 238, 246, 0.1)',
          color: active ? 'primary.contrastText' : 'text.secondary',
          boxShadow: active ? '0 1px 8px rgba(94, 200, 216, 0.35)' : 'none',
          '&:active': disabled ? undefined : { transform: 'scale(0.94)' },
        },
        ...(Array.isArray(sx) ? sx : sx ? [sx] : []),
      ]}
    >
      {children}
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
        px: 0.5,
        py: 0.4,
        borderRadius: 1,
        textAlign: 'center',
        fontSize: '0.6rem',
        fontWeight: 600,
        lineHeight: 1.2,
        display: 'inline-flex',
        alignItems: 'center',
        justifyContent: 'center',
        gap: 0.3,
        cursor: disabled ? 'default' : 'pointer',
        opacity: disabled ? 0.45 : 1,
        bgcolor: active ? 'rgba(94, 200, 216, 0.28)' : 'rgba(232, 238, 246, 0.08)',
        color: active ? 'primary.light' : 'text.secondary',
        border: 1,
        borderColor: active ? 'rgba(94, 200, 216, 0.45)' : 'transparent',
        transition: 'background 120ms ease, border-color 120ms ease',
        '&:hover': disabled ? undefined : { filter: 'brightness(1.08)' },
        '&:active': disabled ? undefined : { transform: 'scale(0.97)' },
      }}
    >
      {children}
    </Box>
  )
}
