import { Typography } from '@mui/material'
import type { WidgetProps } from './types'

export function SensorWidget({ device, value }: WidgetProps) {
  return (
    <Typography
      sx={{
        fontWeight: 700,
        fontSize: '1.15rem',
        lineHeight: 1.1,
        textAlign: 'center',
        fontVariantNumeric: 'tabular-nums',
      }}
    >
      {value === '—' || !value ? '—' : value}
      {device.unit && value && value !== '—' ? (
        <Typography component="span" sx={{ display: 'block', fontSize: '0.65rem', color: 'text.secondary', mt: 0.15 }}>
          {device.unit}
        </Typography>
      ) : null}
    </Typography>
  )
}
