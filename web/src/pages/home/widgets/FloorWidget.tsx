import AddIcon from '@mui/icons-material/Add'
import PowerSettingsNewOutlinedIcon from '@mui/icons-material/PowerSettingsNewOutlined'
import RemoveIcon from '@mui/icons-material/Remove'
import { Box, Typography } from '@mui/material'
import { useRef } from 'react'
import type { Device } from '../../../shared/api/types'
import {
  FLOOR_SETPOINT_MAX,
  FLOOR_SETPOINT_MIN,
  clampFloorSetpoint,
  formatFloorTemp,
  readFloorState,
  type FloorMode,
} from './floorTopics'
import {
  HoldRound,
  RoundBtn,
  stopBubble,
  TEMP_CIRCLE,
  TempCircle,
  WIDGET_BTN,
} from './widgetChrome'

type Props = {
  device: Device
  values: Record<string, string>
  sendCmd: (topic: string, payload: string) => void
}

const MODES: { value: FloorMode; label: string }[] = [
  { value: 'off', label: 'выкл' },
  { value: 'heat', label: 'нагрев' },
  { value: 'auto', label: 'авто' },
]

export function FloorWidget({ device, values, sendCmd }: Props) {
  const s = readFloorState(device.id, device.mqttTopic, values, device.powerTopic)
  const heating = s.mode !== 'off' && s.on
  const setpointRef = useRef(s.setpoint)
  setpointRef.current = s.setpoint

  function setSetpoint(next: number) {
    sendCmd(s.setpointTopic, String(clampFloorSetpoint(next)))
  }

  function setMode(mode: FloorMode) {
    sendCmd(s.modeTopic, mode)
  }

  return (
    <Box
      sx={{
        width: '100%',
        minHeight: 0,
        display: 'grid',
        gap: 1,
        alignContent: 'start',
        opacity: s.mode === 'off' ? 0.78 : 1,
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
        <RoundBtn
          ariaLabel={s.mode === 'off' ? `${device.name}: авто` : `${device.name}: выключить`}
          active={heating}
          onClick={() => setMode(s.mode === 'off' ? 'auto' : 'off')}
        >
          <PowerSettingsNewOutlinedIcon sx={{ fontSize: 22 }} />
        </RoundBtn>
        <Box />
        <TempCircle
          value={formatFloorTemp(s.temp)}
          size={TEMP_CIRCLE}
          accent={heating}
          align="end"
          ariaLabel={`${device.name}: сейчас ${formatFloorTemp(s.temp)}°`}
        />
      </Box>

      <Box
        sx={{
          display: 'grid',
          gridTemplateColumns: `${WIDGET_BTN}px 1fr ${WIDGET_BTN}px`,
          alignItems: 'center',
          gap: 0.75,
        }}
      >
        <HoldRound
          ariaLabel={`${device.name}: уставка −`}
          disabled={s.setpoint <= FLOOR_SETPOINT_MIN}
          onStep={() => setSetpoint(setpointRef.current - 0.5)}
        >
          <RemoveIcon sx={{ fontSize: 22 }} />
        </HoldRound>
        <Typography
          aria-label={`${device.name}: уставка ${formatFloorTemp(s.setpoint)}°`}
          sx={{
            textAlign: 'center',
            fontWeight: 700,
            fontSize: '1.85rem',
            lineHeight: 1,
            fontVariantNumeric: 'tabular-nums',
            letterSpacing: '-0.03em',
            color: s.mode === 'auto' || heating ? 'primary.light' : 'text.primary',
          }}
        >
          {formatFloorTemp(s.setpoint)}
          <Typography component="span" sx={{ fontSize: '0.95rem', fontWeight: 600, ml: 0.2 }}>
            °
          </Typography>
        </Typography>
        <HoldRound
          ariaLabel={`${device.name}: уставка +`}
          disabled={s.setpoint >= FLOOR_SETPOINT_MAX}
          onStep={() => setSetpoint(setpointRef.current + 0.5)}
        >
          <AddIcon sx={{ fontSize: 22 }} />
        </HoldRound>
      </Box>

      <Box sx={{ display: 'flex', gap: 0.6 }}>
        {MODES.map((m) => {
          const selected = s.mode === m.value
          return (
            <Box
              key={m.value}
              component="button"
              type="button"
              aria-label={m.label}
              aria-pressed={selected}
              title={m.label}
              onClick={(e) => {
                stopBubble(e)
                setMode(m.value)
              }}
              onPointerDown={stopBubble}
              sx={{
                all: 'unset',
                boxSizing: 'border-box',
                flex: 1,
                minWidth: 0,
                minHeight: WIDGET_BTN,
                borderRadius: 1.5,
                display: 'grid',
                placeItems: 'center',
                cursor: 'pointer',
                fontSize: '0.8rem',
                fontWeight: 700,
                letterSpacing: '0.02em',
                bgcolor: selected ? 'rgba(94, 200, 216, 0.32)' : 'rgba(232, 238, 246, 0.08)',
                color: selected ? 'primary.light' : 'text.secondary',
                border: 1,
                borderColor: selected ? 'rgba(94, 200, 216, 0.55)' : 'transparent',
                WebkitTapHighlightColor: 'transparent',
                '&:active': { transform: 'scale(0.97)' },
              }}
            >
              {m.label}
            </Box>
          )
        })}
      </Box>
    </Box>
  )
}
