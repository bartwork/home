import { Typography } from '@mui/material'
import type { WidgetProps } from './types'

export function RawWidget({ value }: WidgetProps) {
  return (
    <Typography
      variant="body2"
      color="text.secondary"
      sx={{ textAlign: 'center', wordBreak: 'break-all', fontSize: '0.75rem', px: 0.5 }}
    >
      {value}
    </Typography>
  )
}
