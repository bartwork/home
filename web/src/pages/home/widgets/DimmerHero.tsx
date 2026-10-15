import LightbulbOutlinedIcon from '@mui/icons-material/LightbulbOutlined'
import { Box, Typography } from '@mui/material'

/** Единый размер hero-кнопки у switch и dimmer — плитки визуально ровные. */
export const HERO_SIZE = 60

type Props = {
  level: number | null
  on: boolean
}

/** Лампочка с уровнем внутри колбы. */
export function DimmerHero({ level, on }: Props) {
  const label = level === null ? '—' : String(level)
  const compact = level !== null && level >= 100

  return (
    <Box
      sx={{
        position: 'relative',
        width: 48,
        height: 48,
        display: 'grid',
        placeItems: 'center',
      }}
    >
      <LightbulbOutlinedIcon
        sx={{
          position: 'absolute',
          fontSize: 52,
          opacity: on ? 0.98 : 0.5,
        }}
      />
      <Typography
        sx={{
          position: 'relative',
          zIndex: 1,
          mt: '-7px',
          fontWeight: 700,
          fontSize: compact ? '0.52rem' : '0.58rem',
          lineHeight: 1,
          textAlign: 'center',
          fontVariantNumeric: 'tabular-nums',
          letterSpacing: '-0.04em',
        }}
      >
        {label}
      </Typography>
    </Box>
  )
}
