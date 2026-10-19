import PowerSettingsNewOutlinedIcon from '@mui/icons-material/PowerSettingsNewOutlined'
import { Box, Typography } from '@mui/material'
import type { Device } from '../../../shared/api/types'
import { formatFloorTemp, readFloorState } from './floorTopics'
import { RoundBtn, WIDGET_BTN } from './widgetChrome'

type Props = {
  device: Device
  values: Record<string, string>
  sendCmd: (topic: string, payload: string) => void
}

export function FloorWidget({ device, values, sendCmd }: Props) {
  const { relay, on, temp } = readFloorState(device.mqttTopic, values, device.powerTopic)
  const label = formatFloorTemp(temp)

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
        gap: 0.75,
      }}
    >
      <Typography
        sx={{
          fontWeight: 700,
          fontSize: '1.35rem',
          lineHeight: 1,
          fontVariantNumeric: 'tabular-nums',
          color: on ? 'primary.light' : 'text.primary',
        }}
      >
        {label}
        {temp !== null ? (
          <Typography
            component="span"
            sx={{ ml: 0.25, fontSize: '0.7rem', fontWeight: 600, color: 'text.secondary' }}
          >
            °C
          </Typography>
        ) : null}
      </Typography>
      <RoundBtn
        ariaLabel={on ? `${device.name}: выключить` : `${device.name}: включить`}
        active={on}
        onClick={() => sendCmd(relay, on ? '0' : '1')}
        sx={{ width: WIDGET_BTN + 8, height: WIDGET_BTN + 8 }}
      >
        <PowerSettingsNewOutlinedIcon sx={{ fontSize: 20 }} />
      </RoundBtn>
    </Box>
  )
}
