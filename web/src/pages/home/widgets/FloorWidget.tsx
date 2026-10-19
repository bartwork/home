import AddIcon from '@mui/icons-material/Add'
import PowerSettingsNewOutlinedIcon from '@mui/icons-material/PowerSettingsNewOutlined'
import RemoveIcon from '@mui/icons-material/Remove'
import { Box, Typography } from '@mui/material'
import type { Device } from '../../../shared/api/types'
import {
  clampFloorSetpoint,
  formatFloorTemp,
  nextFloorMode,
  readFloorState,
  type FloorMode,
} from './floorTopics'
import { ChipBtn, RoundBtn, stopBubble, WIDGET_BTN } from './widgetChrome'

type Props = {
  device: Device
  values: Record<string, string>
  sendCmd: (topic: string, payload: string) => void
}

const MODE_LABEL: Record<FloorMode, string> = {
  off: 'выкл',
  heat: 'нагрев',
  auto: 'авто',
}

export function FloorWidget({ device, values, sendCmd }: Props) {
  const s = readFloorState(device.id, device.mqttTopic, values, device.powerTopic)
  const tempLabel = formatFloorTemp(s.temp)
  const heating = s.mode !== 'off' && s.on

  function setSetpoint(next: number) {
    sendCmd(s.setpointTopic, String(clampFloorSetpoint(next)))
  }

  return (
    <Box
      sx={{
        width: '100%',
        height: '100%',
        minHeight: 0,
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        gap: 0.5,
        px: 0.5,
      }}
      onPointerDown={stopBubble}
    >
      <Typography
        sx={{
          fontWeight: 700,
          fontSize: '1.25rem',
          lineHeight: 1,
          fontVariantNumeric: 'tabular-nums',
          color: heating ? 'primary.light' : 'text.primary',
        }}
      >
        {tempLabel}
        {s.temp !== null ? (
          <Typography
            component="span"
            sx={{ ml: 0.25, fontSize: '0.65rem', fontWeight: 600, color: 'text.secondary' }}
          >
            °C
          </Typography>
        ) : null}
      </Typography>

      <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.35 }}>
        <RoundBtn
          ariaLabel={`${device.name}: уставка −`}
          onClick={() => setSetpoint(s.setpoint - 0.5)}
          sx={{ width: WIDGET_BTN - 4, height: WIDGET_BTN - 4 }}
        >
          <RemoveIcon sx={{ fontSize: 16 }} />
        </RoundBtn>
        <Typography
          sx={{
            minWidth: 36,
            textAlign: 'center',
            fontWeight: 700,
            fontSize: '0.95rem',
            fontVariantNumeric: 'tabular-nums',
            color: s.mode === 'auto' ? 'primary.light' : 'text.primary',
          }}
        >
          {formatFloorTemp(s.setpoint)}°
        </Typography>
        <RoundBtn
          ariaLabel={`${device.name}: уставка +`}
          onClick={() => setSetpoint(s.setpoint + 0.5)}
          sx={{ width: WIDGET_BTN - 4, height: WIDGET_BTN - 4 }}
        >
          <AddIcon sx={{ fontSize: 16 }} />
        </RoundBtn>
      </Box>

      <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.4 }}>
        <ChipBtn
          ariaLabel={`${device.name}: режим ${MODE_LABEL[s.mode]}, сменить`}
          active={s.mode !== 'off'}
          onClick={() => sendCmd(s.modeTopic, nextFloorMode(s.mode))}
        >
          {MODE_LABEL[s.mode]}
        </ChipBtn>
        <RoundBtn
          ariaLabel={s.mode === 'off' ? `${device.name}: авто` : `${device.name}: выключить`}
          active={heating}
          onClick={() => sendCmd(s.modeTopic, s.mode === 'off' ? 'auto' : 'off')}
          sx={{ width: WIDGET_BTN, height: WIDGET_BTN }}
        >
          <PowerSettingsNewOutlinedIcon sx={{ fontSize: 18 }} />
        </RoundBtn>
      </Box>
    </Box>
  )
}
