import { Box, Typography } from '@mui/material'
import type { SvgIconComponent } from '@mui/icons-material'
import type { Device } from '../../shared/api/types'
import { iconFor } from './icons'
import { DimmerWidget, isDimmerOn, parseDimmerLevel } from './widgets/DimmerWidget'
import { SensorWidget } from './widgets/SensorWidget'
import { RawWidget } from './widgets/RawWidget'

const SWITCH_HERO = 56

type Props = {
  device: Device
  value: string
  powerValue?: string
  editMode: boolean
  sendCmd: (topic: string, payload: string) => void
  onEdit: (device: Device) => void
}

function SwitchHero({
  name,
  on,
  Icon,
  onToggle,
}: {
  name: string
  on: boolean
  Icon: SvgIconComponent
  onToggle: () => void
}) {
  return (
    <Box
      component="button"
      type="button"
      aria-label={on ? `${name}: выключить` : `${name}: включить`}
      onClick={(e) => {
        e.stopPropagation()
        onToggle()
      }}
      onPointerDown={(e) => e.stopPropagation()}
      sx={{
        all: 'unset',
        boxSizing: 'border-box',
        width: SWITCH_HERO,
        height: SWITCH_HERO,
        borderRadius: '50%',
        display: 'grid',
        placeItems: 'center',
        cursor: 'pointer',
        bgcolor: on ? 'primary.main' : 'rgba(232, 238, 246, 0.08)',
        color: on ? 'primary.contrastText' : 'text.secondary',
        boxShadow: on ? '0 2px 10px rgba(94, 200, 216, 0.4)' : 'none',
        transition: 'background 140ms ease, transform 120ms ease, box-shadow 140ms ease',
        '&:hover': { filter: 'brightness(1.06)' },
        '&:active': { transform: 'scale(0.96)' },
      }}
    >
      <Icon sx={{ fontSize: 28 }} />
    </Box>
  )
}

export function DeviceTile({ device, value, powerValue, editMode, sendCmd, onEdit }: Props) {
  const Icon = iconFor(device)
  const isSwitch = device.deviceType === 'switch'
  const isDimmer = device.deviceType === 'range'
  const level = parseDimmerLevel(value)
  const on = isSwitch ? value === '1' : isDimmer ? isDimmerOn(powerValue, level) : false

  return (
    <Box
      onClick={editMode ? () => onEdit(device) : undefined}
      sx={{
        aspectRatio: '1 / 1',
        width: '100%',
        height: 'auto',
        maxWidth: '100%',
        boxSizing: 'border-box',
        display: 'grid',
        gridTemplateRows: '20px 1fr',
        gap: 0.5,
        p: 1,
        borderRadius: 2.5,
        border: 1,
        borderColor: editMode ? 'secondary.main' : on ? 'rgba(94, 200, 216, 0.5)' : 'divider',
        background: on
          ? 'linear-gradient(165deg, rgba(94, 200, 216, 0.18) 0%, rgba(16, 24, 34, 0.96) 55%)'
          : 'linear-gradient(165deg, rgba(30, 40, 54, 0.96) 0%, rgba(12, 18, 28, 0.96) 100%)',
        boxShadow: on ? '0 0 20px rgba(94, 200, 216, 0.1)' : 'none',
        cursor: editMode ? 'pointer' : 'default',
        userSelect: 'none',
        overflow: 'hidden',
        transition: 'border-color 160ms ease, box-shadow 160ms ease',
      }}
    >
      <Typography
        sx={{
          fontWeight: 600,
          fontSize: '0.72rem',
          lineHeight: '20px',
          textAlign: 'center',
          color: 'text.primary',
          overflow: 'hidden',
          textOverflow: 'ellipsis',
          whiteSpace: 'nowrap',
          minWidth: 0,
        }}
      >
        {device.name}
      </Typography>

      <Box
        sx={{
          minHeight: 0,
          minWidth: 0,
          display: 'grid',
          placeItems: 'center',
          overflow: 'hidden',
        }}
      >
        {editMode ? (
          <Typography variant="caption" color="text.secondary">
            правка
          </Typography>
        ) : isDimmer ? (
          <Box
            sx={{ width: '100%', height: '100%', minHeight: 0 }}
            onClick={(e) => e.stopPropagation()}
            onPointerDown={(e) => e.stopPropagation()}
          >
            <DimmerWidget device={device} value={value} powerValue={powerValue} sendCmd={sendCmd} />
          </Box>
        ) : isSwitch ? (
          <SwitchHero
            name={device.name}
            on={on}
            Icon={Icon}
            onToggle={() => sendCmd(device.mqttTopic, on ? '0' : '1')}
          />
        ) : device.deviceType === 'sensor' ? (
          <Box onClick={(e) => e.stopPropagation()} onPointerDown={(e) => e.stopPropagation()}>
            <SensorWidget device={device} value={value} sendCmd={sendCmd} />
          </Box>
        ) : (
          <Box onClick={(e) => e.stopPropagation()} onPointerDown={(e) => e.stopPropagation()}>
            <RawWidget device={device} value={value} sendCmd={sendCmd} />
          </Box>
        )}
      </Box>
    </Box>
  )
}
