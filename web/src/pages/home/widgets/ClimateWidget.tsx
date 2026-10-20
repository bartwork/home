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
import { useRef, type ReactNode } from 'react'
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
import {
  ChipBtn,
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

const MODE_ICONS: Record<number, SvgIconComponent> = {
  1: WhatshotOutlinedIcon,
  2: AcUnitOutlinedIcon,
  3: SyncOutlinedIcon,
  4: WaterDropOutlinedIcon,
  5: AirOutlinedIcon,
}

function ChipRow({ children }: { children: ReactNode }) {
  return <Box sx={{ display: 'flex', gap: 0.5 }}>{children}</Box>
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
  const indoorDigits = s.indoor !== null && !Number.isInteger(s.indoor) ? 1 : 0
  const targetRef = useRef(s.target)
  targetRef.current = s.target

  function pub(topic: string, payload: string) {
    if (!topic) return
    sendCmd(topic, payload)
  }

  function setTarget(next: number) {
    pub(topics.target, String(clampTarget(next)))
  }

  const indoorLabel = formatTemp(s.indoor, indoorDigits)
  const targetLabel = formatTemp(s.target)

  return (
    <Box
      sx={{
        width: '100%',
        minHeight: 0,
        display: 'grid',
        gap: 1,
        alignContent: 'start',
        opacity: s.on ? 1 : 0.78,
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
          ariaLabel={s.on ? `${device.name}: выключить` : `${device.name}: включить`}
          active={s.on}
          onClick={() => pub(activeTopic, s.on ? '0' : '1')}
        >
          <PowerSettingsNewOutlinedIcon sx={{ fontSize: 22 }} />
        </RoundBtn>
        <Box />
        <TempCircle
          value={indoorLabel}
          size={TEMP_CIRCLE}
          accent={s.on}
          ariaLabel={`${device.name}: в комнате ${indoorLabel}°`}
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
          ariaLabel={`${device.name}: температура −`}
          disabled={s.target === null || s.target <= TARGET_TEMP_MIN}
          onStep={() => {
            const cur = targetRef.current
            if (cur !== null) setTarget(cur - 1)
          }}
        >
          <RemoveIcon sx={{ fontSize: 22 }} />
        </HoldRound>
        <Typography
          aria-label={`${device.name}: уставка ${targetLabel}°`}
          sx={{
            textAlign: 'center',
            fontWeight: 700,
            fontSize: '1.85rem',
            lineHeight: 1,
            fontVariantNumeric: 'tabular-nums',
            letterSpacing: '-0.03em',
            color: s.on ? 'primary.light' : 'text.primary',
          }}
        >
          {targetLabel}
          <Typography component="span" sx={{ fontSize: '0.95rem', fontWeight: 600, ml: 0.2 }}>
            °
          </Typography>
        </Typography>
        <HoldRound
          ariaLabel={`${device.name}: температура +`}
          disabled={s.target !== null && s.target >= TARGET_TEMP_MAX}
          onStep={() => setTarget(targetRef.current !== null ? targetRef.current + 1 : 24)}
        >
          <AddIcon sx={{ fontSize: 22 }} />
        </HoldRound>
      </Box>

      <Box sx={{ display: 'flex', gap: 0.5 }}>
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
                minHeight: WIDGET_BTN,
                borderRadius: 1.5,
                display: 'grid',
                placeItems: 'center',
                cursor: 'pointer',
                bgcolor: selected ? 'rgba(94, 200, 216, 0.32)' : 'rgba(232, 238, 246, 0.08)',
                color: selected ? 'primary.light' : 'text.secondary',
                border: 1,
                borderColor: selected ? 'rgba(94, 200, 216, 0.55)' : 'transparent',
                WebkitTapHighlightColor: 'transparent',
                opacity: topics.mode ? 1 : 0.45,
                '&:active': { transform: 'scale(0.97)' },
              }}
            >
              <Icon sx={{ fontSize: 22 }} />
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
          <AirOutlinedIcon sx={{ fontSize: 14 }} />
          Тихий
        </ChipBtn>
        <ChipBtn
          ariaLabel={s.turboOn ? 'Турбо: выключить' : 'Турбо: включить'}
          active={s.turboOn}
          disabled={!topics.fan}
          onClick={() => pub(topics.fan, String(toggleFanPreset(s.fan, FAN_TURBO)))}
        >
          <SpeedOutlinedIcon sx={{ fontSize: 14 }} />
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
          <EnergySavingsLeafOutlinedIcon sx={{ fontSize: 14 }} />
          Эко
        </ChipBtn>
        <ChipBtn
          ariaLabel={s.sleepOn ? 'Сон: выключить' : 'Сон: включить'}
          active={s.sleepOn}
          disabled={!topics.sleep}
          onClick={() => pub(topics.sleep, s.sleepOn ? '0' : '1')}
        >
          <BedtimeOutlinedIcon sx={{ fontSize: 14 }} />
          Сон
        </ChipBtn>
      </ChipRow>
    </Box>
  )
}
