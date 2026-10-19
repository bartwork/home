import { useEffect, useRef, useState, type PointerEvent as ReactPointerEvent } from 'react'
import AutorenewOutlinedIcon from '@mui/icons-material/AutorenewOutlined'
import PowerSettingsNewOutlinedIcon from '@mui/icons-material/PowerSettingsNewOutlined'
import { Box, Typography } from '@mui/material'
import type { Device } from '../../../shared/api/types'
import { useColorFavorites } from './colorFavorites'
import { LedColorPicker } from './LedColorPicker'
import {
  clampLed,
  hsvToCss,
  LED_BRIGHTNESS_MAX,
  LED_HUE_MAX,
  LED_SATURATION_MAX,
  readLedState,
} from './ledTopics'
import { RoundBtn, stopBubble } from './widgetChrome'

type Props = {
  device: Device
  values: Record<string, string>
  sendCmd: (topic: string, payload: string) => void
}

function SliderRow({
  label,
  value,
  max,
  ariaLabel,
  trackBg,
  onCommit,
}: {
  label: string
  value: number
  max: number
  ariaLabel: string
  trackBg: string
  onCommit: (n: number) => void
}) {
  const trackRef = useRef<HTMLDivElement | null>(null)
  const [local, setLocal] = useState(value)
  const [dragging, setDragging] = useState(false)
  const unbind = useRef<(() => void) | null>(null)

  useEffect(() => () => unbind.current?.(), [])

  useEffect(() => {
    if (!dragging) setLocal(value)
  }, [value, dragging])

  function levelFromX(clientX: number, rect: DOMRect): number {
    if (rect.width <= 0) return 0
    const t = Math.min(1, Math.max(0, (clientX - rect.left) / rect.width))
    return clampLed(t * max, 0, max)
  }

  function onPointerDown(e: ReactPointerEvent) {
    e.preventDefault()
    stopBubble(e)
    const track = trackRef.current
    if (!track) return
    unbind.current?.()

    const apply = (clientX: number) => {
      setDragging(true)
      setLocal(levelFromX(clientX, track.getBoundingClientRect()))
    }
    apply(e.clientX)

    const move = (ev: PointerEvent) => apply(ev.clientX)
    const up = (ev: PointerEvent) => {
      cleanup()
      setDragging(false)
      const next = levelFromX(ev.clientX, track.getBoundingClientRect())
      setLocal(next)
      onCommit(next)
    }
    const cleanup = () => {
      window.removeEventListener('pointermove', move)
      window.removeEventListener('pointerup', up)
      window.removeEventListener('pointercancel', up)
      if (unbind.current === cleanup) unbind.current = null
    }
    unbind.current = cleanup
    window.addEventListener('pointermove', move)
    window.addEventListener('pointerup', up)
    window.addEventListener('pointercancel', up)
  }

  const pct = max > 0 ? (local / max) * 100 : 0

  return (
    <Box sx={{ display: 'grid', gap: 0.25, minWidth: 0 }}>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'baseline', gap: 0.5 }}>
        <Typography sx={{ fontSize: '0.55rem', color: 'text.secondary', fontWeight: 600 }}>{label}</Typography>
        <Typography sx={{ fontSize: '0.55rem', color: 'text.secondary', fontVariantNumeric: 'tabular-nums' }}>
          {local}
        </Typography>
      </Box>
      <Box
        ref={trackRef}
        role="slider"
        aria-label={ariaLabel}
        aria-valuemin={0}
        aria-valuemax={max}
        aria-valuenow={local}
        onPointerDown={onPointerDown}
        sx={{
          position: 'relative',
          height: 14,
          cursor: 'ew-resize',
          touchAction: 'none',
          userSelect: 'none',
          // без border/overflow — иначе тёмные «полоски» на скруглении
        }}
      >
        <Box
          aria-hidden
          sx={{
            position: 'absolute',
            inset: 0,
            borderRadius: 1,
            backgroundImage: trackBg,
            transform: 'translateZ(0)',
            pointerEvents: 'none',
          }}
        />
        <Box
          sx={{
            position: 'absolute',
            top: '50%',
            left: `${Math.min(96, Math.max(4, pct))}%`,
            width: 12,
            height: 12,
            borderRadius: '50%',
            transform: 'translate(-50%, -50%)',
            bgcolor: '#fff',
            boxShadow: '0 0 0 2px rgba(12, 18, 28, 0.9), 0 1px 6px rgba(0,0,0,0.45)',
            pointerEvents: 'none',
            transition: dragging ? 'none' : 'left 120ms ease',
          }}
        />
      </Box>
    </Box>
  )
}

export function LedWidget({ device, values, sendCmd }: Props) {
  const [pickerOpen, setPickerOpen] = useState(false)
  /** Игнор закрытия сразу после открытия — иначе тот же click попадает в backdrop. */
  const ignoreCloseUntil = useRef(0)
  const favorites = useColorFavorites()
  const s = readLedState(device.mqttTopic, values)
  const { topics } = s
  const hue = s.hue ?? 0
  const sat = s.saturation ?? 100
  const bri = s.brightness ?? 0
  const preview = hsvToCss(hue, sat, s.on ? Math.max(bri, 18) : 12)
  const whitePct = s.whiteBrightness ?? 0
  const lit = s.on || s.whiteOn

  function ensureStripOn() {
    if (!s.on) sendCmd(topics.strip, '1')
  }

  function stopHueChanging() {
    if (s.hueChanging) sendCmd(topics.hueChanging, '0')
  }

  function setBrightness(n: number) {
    const next = clampLed(n, 0, LED_BRIGHTNESS_MAX)
    sendCmd(topics.brightness, String(next))
    if (next > 0) {
      stopHueChanging()
      ensureStripOn()
    } else if (s.on) sendCmd(topics.strip, '0')
  }

  function setHue(n: number) {
    stopHueChanging()
    sendCmd(topics.hue, String(clampLed(n, 0, LED_HUE_MAX)))
    ensureStripOn()
  }

  function setSat(n: number) {
    stopHueChanging()
    sendCmd(topics.saturation, String(clampLed(n, 0, LED_SATURATION_MAX)))
    ensureStripOn()
  }

  function setWhiteBrightness(n: number) {
    const next = clampLed(n, 0, LED_BRIGHTNESS_MAX)
    sendCmd(topics.whiteBrightness, String(next))
    if (next > 0 && !s.whiteOn) sendCmd(topics.white, '1')
    else if (next === 0 && s.whiteOn) sendCmd(topics.white, '0')
  }

  function toggleWhite() {
    if (s.whiteOn) {
      sendCmd(topics.white, '0')
      return
    }
    sendCmd(topics.white, '1')
    if ((s.whiteBrightness ?? 0) <= 0) {
      sendCmd(topics.whiteBrightness, String(LED_BRIGHTNESS_MAX))
    }
  }

  function applyPicker(next: { hue: number; saturation: number; brightness: number }) {
    stopHueChanging()
    if (next.hue !== hue) setHue(next.hue)
    if (next.saturation !== sat) setSat(next.saturation)
    if (next.brightness !== bri) setBrightness(next.brightness)
    else ensureStripOn()
  }

  function openPicker() {
    ignoreCloseUntil.current = Date.now() + 350
    setPickerOpen(true)
  }

  function closePicker() {
    if (Date.now() < ignoreCloseUntil.current) return
    setPickerOpen(false)
  }

  return (
    <Box
      sx={{
        width: '100%',
        minHeight: 0,
        boxSizing: 'border-box',
        display: 'grid',
        gap: 0.55,
        alignContent: 'start',
        px: 0.35,
        pb: 0.35,
        opacity: lit ? 1 : 0.78,
      }}
    >
      <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
        <RoundBtn
          ariaLabel={s.on ? `${device.name}: выключить` : `${device.name}: включить`}
          active={s.on}
          onClick={() => sendCmd(topics.strip, s.on ? '0' : '1')}
          sx={{ boxShadow: 'none' }}
        >
          <PowerSettingsNewOutlinedIcon sx={{ fontSize: 16 }} />
        </RoundBtn>
        <RoundBtn
          ariaLabel={s.hueChanging ? 'Авто-оттенок: выключить' : 'Авто-оттенок: включить'}
          active={s.hueChanging}
          onClick={() => {
            sendCmd(topics.hueChanging, s.hueChanging ? '0' : '1')
            if (!s.hueChanging) ensureStripOn()
          }}
          sx={{ boxShadow: 'none' }}
        >
          <AutorenewOutlinedIcon sx={{ fontSize: 16 }} />
        </RoundBtn>
        <RoundBtn
          ariaLabel={s.whiteOn ? 'Белый канал: выключить' : 'Белый канал: включить'}
          active={s.whiteOn}
          onClick={toggleWhite}
          sx={{
            bgcolor: s.whiteOn ? 'rgba(245, 248, 255, 0.92)' : undefined,
            color: s.whiteOn ? '#1a2030' : undefined,
            boxShadow: 'none',
            fontSize: '0.65rem',
            fontWeight: 700,
          }}
        >
          W
        </RoundBtn>
        <Box
          component="button"
          type="button"
          aria-label={`${device.name}: выбрать цвет`}
          onPointerDown={stopBubble}
          onClick={(e) => {
            stopBubble(e)
            openPicker()
          }}
          sx={{
            appearance: 'none',
            border: 'none',
            outline: 'none',
            margin: 0,
            padding: 0,
            font: 'inherit',
            color: 'inherit',
            boxSizing: 'border-box',
            display: 'block',
            flex: 1,
            minWidth: 0,
            height: 28,
            borderRadius: 1.5,
            bgcolor: preview,
            cursor: 'pointer',
            WebkitTapHighlightColor: 'transparent',
            boxShadow: 'none !important',
            filter: 'none',
            opacity: s.on ? 1 : 0.55,
            transition: 'background 140ms ease, opacity 140ms ease',
            '&:active': { transform: 'scale(0.98)', boxShadow: 'none' },
            '&:focus': { boxShadow: 'none', outline: 'none' },
            '&:hover': { boxShadow: 'none', filter: 'none' },
          }}
        />
      </Box>

      <LedColorPicker
        open={pickerOpen}
        title={device.name}
        hue={hue}
        saturation={sat}
        brightness={bri}
        favorites={favorites.items}
        favoritesPersisted={favorites.persisted}
        favoritesBusy={favorites.busy}
        onAddFavorite={() => void favorites.add({ hue, saturation: sat, brightness: bri })}
        onRemoveFavorite={(id) => void favorites.remove(id)}
        onResetFavorites={() => void favorites.reset()}
        onClose={closePicker}
        onChange={applyPicker}
      />

      {favorites.items.length > 0 ? (
        <Box
          role="group"
          aria-label="Избранные цвета"
          sx={{
            display: 'grid',
            gridTemplateColumns: `repeat(${favorites.items.length}, minmax(0, 1fr))`,
            gap: 0.5,
            width: '100%',
            minWidth: 0,
          }}
        >
          {favorites.items.map((f) => {
            const selected =
              Math.round(hue) === f.hue &&
              Math.round(sat) === f.saturation &&
              Math.round(bri) === f.brightness
            return (
              <Box
                key={f.id}
                component="button"
                type="button"
                aria-label={`Избранный цвет H${f.hue}`}
                aria-pressed={selected}
                onPointerDown={stopBubble}
                onClick={(e) => {
                  stopBubble(e)
                  applyPicker({ hue: f.hue, saturation: f.saturation, brightness: f.brightness })
                }}
                sx={{
                  appearance: 'none',
                  margin: 0,
                  padding: 0,
                  width: '100%',
                  minWidth: 0,
                  height: 28,
                  borderRadius: 1.5,
                  border: selected
                    ? '2px solid rgba(255,255,255,0.92)'
                    : '1px solid rgba(255,255,255,0.18)',
                  bgcolor: hsvToCss(f.hue, f.saturation, Math.max(f.brightness, 32)),
                  cursor: 'pointer',
                  boxShadow: selected ? '0 0 0 1px rgba(12,18,28,0.55)' : 'none',
                  WebkitTapHighlightColor: 'transparent',
                  transition: 'border-color 120ms ease, box-shadow 120ms ease, transform 80ms ease',
                  '&:active': { transform: 'scale(0.97)' },
                }}
              />
            )
          })}
        </Box>
      ) : null}

      <SliderRow
        label="Оттенок"
        value={hue}
        max={LED_HUE_MAX}
        ariaLabel="Оттенок"
        trackBg="linear-gradient(90deg, #ff0000, #ffff00, #00ff00, #00ffff, #0000ff, #ff00ff, #ff0000)"
        onCommit={setHue}
      />
      <SliderRow
        label="Насыщенность"
        value={sat}
        max={LED_SATURATION_MAX}
        ariaLabel="Насыщенность"
        trackBg={`linear-gradient(90deg, ${hsvToCss(hue, 0, 80)}, ${hsvToCss(hue, 100, 80)})`}
        onCommit={setSat}
      />
      <SliderRow
        label="Яркость"
        value={bri}
        max={LED_BRIGHTNESS_MAX}
        ariaLabel="Яркость"
        trackBg={`linear-gradient(90deg, #111, ${hsvToCss(hue, sat, 100)})`}
        onCommit={setBrightness}
      />
      <SliderRow
        label="Белый"
        value={whitePct}
        max={LED_BRIGHTNESS_MAX}
        ariaLabel="Яркость белого канала"
        trackBg="linear-gradient(90deg, #222, #f5f7ff)"
        onCommit={setWhiteBrightness}
      />
    </Box>
  )
}
