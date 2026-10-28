import AirOutlinedIcon from '@mui/icons-material/AirOutlined'
import Co2OutlinedIcon from '@mui/icons-material/Co2Outlined'
import DirectionsWalkOutlinedIcon from '@mui/icons-material/DirectionsWalkOutlined'
import VolumeUpOutlinedIcon from '@mui/icons-material/VolumeUpOutlined'
import WaterDropOutlinedIcon from '@mui/icons-material/WaterDropOutlined'
import WbSunnyOutlinedIcon from '@mui/icons-material/WbSunnyOutlined'
import { Box, Typography } from '@mui/material'
import type { ReactNode } from 'react'
import type { Device } from '../../../shared/api/types'
import {
  co2Label,
  formatAirNum,
  readAirState,
  type Co2Level,
} from './airTopics'
import { stopBubble, TEMP_CIRCLE, TempCircle, WIDGET_BTN } from './widgetChrome'

type Props = {
  device: Device
  values: Record<string, string>
}

function levelColor(level: Co2Level): string {
  switch (level) {
    case 'good':
      return 'primary.light'
    case 'ok':
      return 'warning.light'
    case 'high':
      return 'error.light'
  }
}

function MetricCell({
  label,
  value,
  unit,
  accent,
  icon,
}: {
  label: string
  value: string
  unit: string
  accent?: boolean
  icon?: ReactNode
}) {
  return (
    <Box
      sx={{
        minWidth: 0,
        px: 0.75,
        py: 0.7,
        borderRadius: 1.5,
        bgcolor: accent ? 'rgba(244, 143, 143, 0.14)' : 'rgba(232, 238, 246, 0.07)',
        border: 1,
        borderColor: accent ? 'rgba(244, 143, 143, 0.35)' : 'transparent',
        display: 'grid',
        gap: 0.25,
        alignContent: 'center',
      }}
    >
      <Typography
        sx={{
          fontSize: '0.65rem',
          color: 'text.secondary',
          fontWeight: 600,
          display: 'flex',
          alignItems: 'center',
          gap: 0.35,
          minWidth: 0,
        }}
      >
        {icon}
        <Box component="span" sx={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
          {label}
        </Box>
      </Typography>
      <Typography
        sx={{
          fontWeight: 700,
          fontSize: '1rem',
          lineHeight: 1.1,
          fontVariantNumeric: 'tabular-nums',
          color: accent ? 'error.light' : 'text.primary',
        }}
      >
        {value}
        {value !== '—' && unit ? (
          <Typography component="span" sx={{ fontSize: '0.7rem', fontWeight: 600, ml: 0.25, color: 'text.secondary' }}>
            {unit}
          </Typography>
        ) : null}
      </Typography>
    </Box>
  )
}

export function AirWidget({ device, values }: Props) {
  const s = readAirState(device.mqttTopic, values)
  const tempDigits = s.temperature !== null && !Number.isInteger(s.temperature) ? 1 : 0
  const humDigits = s.humidity !== null && !Number.isInteger(s.humidity) ? 1 : 0

  return (
    <Box
      sx={{
        width: '100%',
        minHeight: 0,
        display: 'grid',
        gap: 1,
        alignContent: 'start',
        touchAction: 'manipulation',
      }}
      onPointerDown={stopBubble}
    >
      <Box
        sx={{
          display: 'grid',
          gridTemplateColumns: `${WIDGET_BTN}px 1fr ${WIDGET_BTN}px`,
          alignItems: 'center',
          gap: 0.75,
        }}
      >
        <TempCircle
          value={formatAirNum(s.temperature, tempDigits)}
          size={TEMP_CIRCLE}
          accent
          ariaLabel={`${device.name}: температура ${formatAirNum(s.temperature, tempDigits)}°`}
        />
        <Box sx={{ textAlign: 'center', minWidth: 0 }}>
          <Typography
            sx={{
              fontSize: '0.7rem',
              fontWeight: 700,
              color: levelColor(s.co2Level),
              letterSpacing: '0.02em',
            }}
          >
            {co2Label(s.co2Level)}
          </Typography>
          {s.motionOn ? (
            <Typography
              sx={{
                mt: 0.25,
                fontSize: '0.65rem',
                color: 'primary.light',
                fontWeight: 600,
                display: 'inline-flex',
                alignItems: 'center',
                gap: 0.25,
              }}
            >
              <DirectionsWalkOutlinedIcon sx={{ fontSize: 14 }} />
              движение
            </Typography>
          ) : (
            <Typography sx={{ mt: 0.25, fontSize: '0.65rem', color: 'text.secondary', fontWeight: 600 }}>
              воздух
            </Typography>
          )}
        </Box>
        <Box
          aria-label={`${device.name}: влажность ${formatAirNum(s.humidity, humDigits)}%`}
          sx={{
            width: TEMP_CIRCLE,
            height: TEMP_CIRCLE,
            justifySelf: 'end',
            borderRadius: '50%',
            display: 'grid',
            placeItems: 'center',
            bgcolor: 'rgba(232, 238, 246, 0.1)',
            border: 1,
            borderColor: 'rgba(232, 238, 246, 0.14)',
          }}
        >
          <Box sx={{ textAlign: 'center', lineHeight: 1 }}>
            <WaterDropOutlinedIcon sx={{ fontSize: 14, color: 'text.secondary', display: 'block', mx: 'auto', mb: 0.15 }} />
            <Typography
              sx={{
                fontWeight: 700,
                fontSize: '0.85rem',
                fontVariantNumeric: 'tabular-nums',
                letterSpacing: '-0.02em',
              }}
            >
              {formatAirNum(s.humidity, humDigits)}
              <Typography component="span" sx={{ fontSize: '0.65rem', fontWeight: 600 }}>
                %
              </Typography>
            </Typography>
          </Box>
        </Box>
      </Box>

      <Box sx={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 0.6 }}>
        <MetricCell
          label="CO₂"
          value={formatAirNum(s.co2)}
          unit="ppm"
          accent={s.co2Level === 'high'}
          icon={<Co2OutlinedIcon sx={{ fontSize: 14 }} />}
        />
        <MetricCell
          label="VOC"
          value={formatAirNum(s.voc)}
          unit="ppb"
          icon={<AirOutlinedIcon sx={{ fontSize: 14 }} />}
        />
        <MetricCell
          label="свет"
          value={formatAirNum(s.illuminance)}
          unit="лк"
          icon={<WbSunnyOutlinedIcon sx={{ fontSize: 14 }} />}
        />
        <MetricCell
          label="шум"
          value={formatAirNum(s.sound)}
          unit="дБ"
          icon={<VolumeUpOutlinedIcon sx={{ fontSize: 14 }} />}
        />
      </Box>
    </Box>
  )
}
