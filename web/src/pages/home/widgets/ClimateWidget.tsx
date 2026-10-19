import type { SvgIconComponent } from '@mui/icons-material'
import AcUnitOutlinedIcon from '@mui/icons-material/AcUnitOutlined'
import AddIcon from '@mui/icons-material/Add'
import AirOutlinedIcon from '@mui/icons-material/AirOutlined'
import BedtimeOutlinedIcon from '@mui/icons-material/BedtimeOutlined'
import EnergySavingsLeafOutlinedIcon from '@mui/icons-material/EnergySavingsLeafOutlined'
import PowerSettingsNewOutlinedIcon from '@mui/icons-material/PowerSettingsNewOutlined'
import RemoveIcon from '@mui/icons-material/Remove'
import SpeedOutlinedIcon from '@mui/icons-material/SpeedOutlined'
import SyncOutlinedIcon from '@mui/icons-material/SyncOutlined'
import WaterDropOutlinedIcon from '@mui/icons-material/WaterDropOutlined'
import WhatshotOutlinedIcon from '@mui/icons-material/WhatshotOutlined'
import { Box, Typography } from '@mui/material'
import type { ReactNode } from 'react'
import type { Device } from '../../../shared/api/types'
import {
  CLIMATE_FANS,
  CLIMATE_HORIZONTAL,
  CLIMATE_MODES,
  CLIMATE_SWINGS,
  CLIMATE_VERTICAL,
  clampTarget,
  FAN_QUIET,
  FAN_TURBO,
  formatTemp,
  labelByValue,
  type LabeledValue,
  nextInCycle,
  readClimateState,
  TARGET_TEMP_MAX,
  TARGET_TEMP_MIN,
  toggleFanPreset,
} from './climateTopics'
import { ChipBtn, RoundBtn, stopBubble, WIDGET_BTN } from './widgetChrome'

type Props = {
  device: Device
  values: Record<string, string>
  sendCmd: (topic: string, payload: string) => void
}

const MODE_ICONS: Record<number, SvgIconComponent> = {
  1: WhatshotOutlinedIcon,
  2: AcUnitOutlinedIcon,
  3: SyncOutlinedIcon,
  4: WaterDropOutlinedIcon,
  5: AirOutlinedIcon,
}

function ChipRow({ children }: { children: ReactNode }) {
  return <Box sx={{ display: 'flex', gap: 0.35 }}>{children}</Box>
}

/** Чип, крутящий enum по sibling-топику. */
function CycleChip({
  topic,
  list,
  value,
  fallback,
  sendCmd,
  active,
  formatLabel,
}: {
  topic: string
  list: LabeledValue[]
  value: number | null
  fallback: string
  sendCmd: (topic: string, payload: string) => void
  active?: boolean
  formatLabel?: (label: string) => string
}) {
  const raw = labelByValue(list, value)
  const label = raw ? (formatLabel ? formatLabel(raw) : raw) : fallback
  return (
    <ChipBtn
      ariaLabel={`${fallback}: ${raw ?? 'неизвестно'}, сменить`}
      active={active ?? (value !== null && value > 0)}
      disabled={!topic}
      onClick={() => sendCmd(topic, String(nextInCycle(list, value)))}
    >
      {label}
    </ChipBtn>
  )
}

export function ClimateWidget({ device, values, sendCmd }: Props) {
  const s = readClimateState(device.mqttTopic, values)
  const { topics } = s
  const activeTopic = topics.active || device.mqttTopic
  const modeLabel = labelByValue(CLIMATE_MODES, s.mode)
  const indoorDigits = s.indoor !== null && !Number.isInteger(s.indoor) ? 1 : 0

  function pub(topic: string, payload: string) {
    if (!topic) return
    sendCmd(topic, payload)
  }

  function setTarget(next: number) {
    pub(topics.target, String(clampTarget(next)))
  }

  return (
    <Box
      sx={{
        width: '100%',
        minHeight: 0,
        display: 'grid',
        gap: 0.5,
        alignContent: 'start',
        opacity: s.on ? 1 : 0.78,
      }}
    >
      <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 0.5 }}>
        <RoundBtn
          ariaLabel={s.on ? `${device.name}: выключить` : `${device.name}: включить`}
          active={s.on}
          onClick={() => pub(activeTopic, s.on ? '0' : '1')}
        >
          <PowerSettingsNewOutlinedIcon sx={{ fontSize: 16 }} />
        </RoundBtn>
        <Typography
          sx={{
            fontSize: '0.65rem',
            color: 'text.secondary',
            fontVariantNumeric: 'tabular-nums',
            display: 'flex',
            alignItems: 'center',
            gap: 0.25,
          }}
        >
          <AcUnitOutlinedIcon sx={{ fontSize: 12, opacity: 0.7 }} />
          {formatTemp(s.indoor, indoorDigits)}°
        </Typography>
      </Box>

      <Box
        sx={{
          display: 'grid',
          gridTemplateColumns: `${WIDGET_BTN}px 1fr ${WIDGET_BTN}px`,
          alignItems: 'center',
          gap: 0.35,
        }}
      >
        <RoundBtn
          ariaLabel="Температура −"
          disabled={s.target === null || s.target <= TARGET_TEMP_MIN}
          onClick={() => {
            if (s.target !== null) setTarget(s.target - 1)
          }}
        >
          <RemoveIcon sx={{ fontSize: 16 }} />
        </RoundBtn>
        <Box sx={{ textAlign: 'center', minWidth: 0 }}>
          <Typography
            sx={{
              fontWeight: 700,
              fontSize: '1.35rem',
              lineHeight: 1,
              fontVariantNumeric: 'tabular-nums',
              letterSpacing: '-0.03em',
            }}
          >
            {formatTemp(s.target)}
            <Typography component="span" sx={{ fontSize: '0.7rem', fontWeight: 600, ml: 0.15 }}>
              °
            </Typography>
          </Typography>
          <Typography sx={{ fontSize: '0.55rem', color: 'text.secondary', mt: 0.2 }}>
            {modeLabel ?? 'режим'}
          </Typography>
        </Box>
        <RoundBtn
          ariaLabel="Температура +"
          disabled={s.target !== null && s.target >= TARGET_TEMP_MAX}
          onClick={() => setTarget(s.target !== null ? s.target + 1 : 24)}
        >
          <AddIcon sx={{ fontSize: 16 }} />
        </RoundBtn>
      </Box>

      <Box sx={{ display: 'flex', gap: 0.3 }}>
        {CLIMATE_MODES.map((m) => {
          const Icon = MODE_ICONS[m.value] ?? SyncOutlinedIcon
          const selected = s.mode === m.value
          return (
            <Box
              key={m.value}
              component="button"
              type="button"
              aria-label={m.label}
              aria-pressed={selected}
              disabled={!topics.mode}
              title={m.label}
              onClick={(e) => {
                stopBubble(e)
                pub(topics.mode, String(m.value))
              }}
              onPointerDown={stopBubble}
              sx={{
                all: 'unset',
                boxSizing: 'border-box',
                flex: 1,
                minWidth: 0,
                height: 30,
                borderRadius: 1,
                display: 'grid',
                placeItems: 'center',
                cursor: 'pointer',
                bgcolor: selected ? 'rgba(94, 200, 216, 0.3)' : 'rgba(232, 238, 246, 0.06)',
                color: selected ? 'primary.light' : 'text.secondary',
                border: 1,
                borderColor: selected ? 'rgba(94, 200, 216, 0.5)' : 'transparent',
                '&:active': { transform: 'scale(0.96)' },
              }}
            >
              <Icon sx={{ fontSize: 15 }} />
            </Box>
          )
        })}
      </Box>

      <ChipRow>
        <CycleChip
          topic={topics.fan}
          list={CLIMATE_FANS}
          value={s.fan}
          fallback="Вентилятор"
          sendCmd={sendCmd}
          active={s.fan !== null && s.fan > 0 && !s.quietOn && !s.turboOn}
          formatLabel={(label) => `Вент. ${label}`}
        />
        <ChipBtn
          ariaLabel={s.quietOn ? 'Тихий: выключить' : 'Тихий: включить'}
          active={s.quietOn}
          disabled={!topics.fan}
          onClick={() => pub(topics.fan, String(toggleFanPreset(s.fan, FAN_QUIET)))}
        >
          <AirOutlinedIcon sx={{ fontSize: 12 }} />
          Тихий
        </ChipBtn>
        <ChipBtn
          ariaLabel={s.turboOn ? 'Турбо: выключить' : 'Турбо: включить'}
          active={s.turboOn}
          disabled={!topics.fan}
          onClick={() => pub(topics.fan, String(toggleFanPreset(s.fan, FAN_TURBO)))}
        >
          <SpeedOutlinedIcon sx={{ fontSize: 12 }} />
          Турбо
        </ChipBtn>
      </ChipRow>

      <ChipRow>
        <CycleChip
          topic={topics.horizontal}
          list={CLIMATE_HORIZONTAL}
          value={s.horizontal}
          fallback="Гориз. жалюзи"
          sendCmd={sendCmd}
        />
        <CycleChip
          topic={topics.vertical}
          list={CLIMATE_VERTICAL}
          value={s.vertical}
          fallback="Верт. жалюзи"
          sendCmd={sendCmd}
        />
      </ChipRow>

      <ChipRow>
        <CycleChip
          topic={topics.swing}
          list={CLIMATE_SWINGS}
          value={s.swing}
          fallback="Качание"
          sendCmd={sendCmd}
        />
        <ChipBtn
          ariaLabel={s.ecoOn ? 'Эко: выключить' : 'Эко: включить'}
          active={s.ecoOn}
          disabled={!topics.eco}
          onClick={() => pub(topics.eco, s.ecoOn ? '0' : '1')}
        >
          <EnergySavingsLeafOutlinedIcon sx={{ fontSize: 12 }} />
          Эко
        </ChipBtn>
        <ChipBtn
          ariaLabel={s.sleepOn ? 'Сон: выключить' : 'Сон: включить'}
          active={s.sleepOn}
          disabled={!topics.sleep}
          onClick={() => pub(topics.sleep, s.sleepOn ? '0' : '1')}
        >
          <BedtimeOutlinedIcon sx={{ fontSize: 12 }} />
          Сон
        </ChipBtn>
      </ChipRow>
    </Box>
  )
}
