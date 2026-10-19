import { useEffect, useRef, useState } from 'react'
import AddIcon from '@mui/icons-material/Add'
import CloseIcon from '@mui/icons-material/Close'
import {
  Box,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  IconButton,
  Typography,
} from '@mui/material'
import type { ColorFavorite } from '../../../shared/api/client'
import { MAX_COLOR_FAVORITES } from './colorFavorites'
import {
  clampLed,
  hsvToCss,
  LED_BRIGHTNESS_MAX,
  LED_HUE_MAX,
  LED_SATURATION_MAX,
} from './ledTopics'
import { usePointerDrag } from './pointerDrag'
import { stopBubble } from './widgetChrome'

type Props = {
  open: boolean
  title: string
  hue: number
  saturation: number
  brightness: number
  favorites: ColorFavorite[]
  favoritesPersisted: boolean
  favoritesBusy: boolean
  onAddFavorite: () => void
  onRemoveFavorite: (id: number) => void
  onResetFavorites: () => void
  onClose: () => void
  onChange: (next: { hue: number; saturation: number; brightness: number }) => void
}

const HUE_RAINBOW =
  'linear-gradient(90deg, #f00 0%, #ff0 17%, #0f0 33%, #0ff 50%, #00f 67%, #f0f 83%, #f00 100%)'

function Thumb({
  leftPct,
  topPct,
  color,
  size,
  dragging,
}: {
  leftPct: number
  topPct?: number
  color: string
  size: number
  dragging: boolean
}) {
  return (
    <Box
      sx={{
        position: 'absolute',
        left: `${leftPct}%`,
        top: topPct === undefined ? '50%' : `${topPct}%`,
        width: size,
        height: size,
        borderRadius: '50%',
        transform: 'translate(-50%, -50%)',
        bgcolor: color,
        border: '2.5px solid #fff',
        boxShadow: '0 1px 4px rgba(0,0,0,0.35)',
        pointerEvents: 'none',
        transition: dragging ? 'none' : 'left 100ms ease, top 100ms ease, background 100ms ease',
      }}
    />
  )
}

function BrightnessBar({
  value,
  hue,
  sat,
  onCommit,
}: {
  value: number
  hue: number
  sat: number
  onCommit: (n: number) => void
}) {
  const [local, setLocal] = useState(value)
  const localRef = useRef(local)
  localRef.current = local
  const endColor = hsvToCss(hue, sat, 100)

  const { elRef, dragging, onPointerDown } = usePointerDrag(
    (clientX, _y, rect) => {
      if (rect.width <= 0) return
      const t = Math.min(1, Math.max(0, (clientX - rect.left) / rect.width))
      setLocal(clampLed(t * LED_BRIGHTNESS_MAX, 0, LED_BRIGHTNESS_MAX))
    },
    { onEnd: () => onCommit(localRef.current) },
  )

  useEffect(() => {
    if (!dragging) setLocal(value)
  }, [value, dragging])

  const pct = (local / LED_BRIGHTNESS_MAX) * 100

  return (
    <Box sx={{ display: 'grid', gap: 0.75 }}>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'baseline' }}>
        <Typography sx={{ fontSize: '0.8rem', fontWeight: 600, color: 'text.secondary' }}>
          Яркость
        </Typography>
        <Typography sx={{ fontSize: '0.8rem', fontVariantNumeric: 'tabular-nums', color: 'text.secondary' }}>
          {local}%
        </Typography>
      </Box>
      <Box
        ref={elRef}
        role="slider"
        aria-label="Яркость"
        aria-valuemin={0}
        aria-valuemax={LED_BRIGHTNESS_MAX}
        aria-valuenow={local}
        onPointerDown={onPointerDown}
        sx={{
          position: 'relative',
          height: 40,
          borderRadius: 999,
          cursor: 'ew-resize',
          touchAction: 'none',
          userSelect: 'none',
          // без overflow/border: иначе на скруглении слева видна тёмная «полоска»
          background: `linear-gradient(90deg, #1a222e 0%, ${endColor} 100%)`,
        }}
      >
        <Thumb
          leftPct={Math.min(96, Math.max(4, pct))}
          color="#fff"
          size={22}
          dragging={dragging}
        />
      </Box>
    </Box>
  )
}

function HueSatPad({
  hue,
  saturation,
  onChange,
}: {
  hue: number
  saturation: number
  onChange: (hue: number, saturation: number) => void
}) {
  const [localH, setLocalH] = useState(hue)
  const [localS, setLocalS] = useState(saturation)
  const localRef = useRef({ h: hue, s: saturation })
  localRef.current = { h: localH, s: localS }

  const { elRef, dragging, onPointerDown } = usePointerDrag(
    (clientX, clientY, rect) => {
      const x = Math.min(1, Math.max(0, (clientX - rect.left) / Math.max(rect.width, 1)))
      const y = Math.min(1, Math.max(0, (clientY - rect.top) / Math.max(rect.height, 1)))
      const h = clampLed(x * LED_HUE_MAX, 0, LED_HUE_MAX)
      const s = clampLed((1 - y) * LED_SATURATION_MAX, 0, LED_SATURATION_MAX)
      setLocalH(h)
      setLocalS(s)
    },
    {
      axis: 'xy',
      onEnd: () => onChange(localRef.current.h, localRef.current.s),
    },
  )

  useEffect(() => {
    if (!dragging) {
      setLocalH(hue)
      setLocalS(saturation)
    }
  }, [hue, saturation, dragging])

  const left = (localH / LED_HUE_MAX) * 100
  const top = (1 - localS / LED_SATURATION_MAX) * 100

  return (
    <Box
      ref={elRef}
      role="slider"
      aria-label="Цвет: оттенок и насыщенность"
      onPointerDown={onPointerDown}
      sx={{
        position: 'relative',
        width: '100%',
        aspectRatio: '1 / 1',
        maxHeight: 'min(48dvh, 380px)',
        mx: 'auto',
        cursor: 'crosshair',
        touchAction: 'none',
        userSelect: 'none',
      }}
    >
      {/* фон отдельно: скругление через border-radius без overflow:hidden */}
      <Box
        aria-hidden
        sx={{
          position: 'absolute',
          inset: 0,
          borderRadius: 2.5,
          // без белого backgroundColor — иначе на скруглении сверху светлая 1px-полоска
          backgroundImage: [
            'linear-gradient(180deg, rgba(255,255,255,0) 0%, #fff 100%)',
            HUE_RAINBOW,
          ].join(', '),
          transform: 'translateZ(0)',
          pointerEvents: 'none',
        }}
      />
      <Thumb
        leftPct={Math.min(96, Math.max(4, left))}
        topPct={Math.min(96, Math.max(4, top))}
        color={hsvToCss(localH, localS, 100)}
        size={28}
        dragging={dragging}
      />
    </Box>
  )
}

function FavoritesRow({
  items,
  persisted,
  busy,
  onPick,
  onAdd,
  onRemove,
  onReset,
}: {
  items: ColorFavorite[]
  persisted: boolean
  busy: boolean
  onPick: (c: { hue: number; saturation: number; brightness: number }) => void
  onAdd: () => void
  onRemove: (id: number) => void
  onReset: () => void
}) {
  const full = persisted && items.length >= MAX_COLOR_FAVORITES

  return (
    <Box sx={{ display: 'grid', gap: 0.75 }}>
      <Box sx={{ display: 'flex', alignItems: 'baseline', justifyContent: 'space-between', gap: 1 }}>
        <Typography sx={{ fontSize: '0.8rem', fontWeight: 600, color: 'text.secondary' }}>
          {persisted ? 'Избранное' : 'Стандартные'}
        </Typography>
        {persisted ? (
          <Box
            component="button"
            type="button"
            disabled={busy}
            onClick={(e) => {
              stopBubble(e)
              onReset()
            }}
            onPointerDown={stopBubble}
            sx={{
              appearance: 'none',
              border: 'none',
              margin: 0,
              padding: '2px 4px',
              bgcolor: 'transparent',
              color: 'text.secondary',
              fontSize: '0.72rem',
              fontWeight: 600,
              cursor: busy ? 'default' : 'pointer',
              opacity: busy ? 0.45 : 0.85,
              textDecoration: 'underline',
              textUnderlineOffset: 3,
              '&:hover': { color: 'text.primary' },
            }}
          >
            Сбросить
          </Box>
        ) : null}
      </Box>
      <Box
        sx={{
          display: 'flex',
          flexWrap: 'wrap',
          gap: 1,
          alignItems: 'center',
        }}
      >
        {items.map((f) => {
          const swatch = hsvToCss(f.hue, f.saturation, Math.max(f.brightness, 28))
          return (
            <Box key={f.id} sx={{ position: 'relative' }}>
              <Box
                component="button"
                type="button"
                aria-label={`Цвет H${f.hue} S${f.saturation} V${f.brightness}`}
                onClick={(e) => {
                  stopBubble(e)
                  onPick({ hue: f.hue, saturation: f.saturation, brightness: f.brightness })
                }}
                onPointerDown={stopBubble}
                sx={{
                  appearance: 'none',
                  border: '2px solid rgba(255,255,255,0.35)',
                  margin: 0,
                  padding: 0,
                  width: 40,
                  height: 40,
                  borderRadius: '50%',
                  bgcolor: swatch,
                  cursor: 'pointer',
                  boxShadow: 'none',
                  '&:active': { transform: 'scale(0.94)' },
                }}
              />
              {persisted ? (
                <Box
                  component="button"
                  type="button"
                  aria-label="Удалить цвет"
                  onClick={(e) => {
                    stopBubble(e)
                    onRemove(f.id)
                  }}
                  onPointerDown={stopBubble}
                  sx={{
                    appearance: 'none',
                    border: 'none',
                    margin: 0,
                    padding: 0,
                    position: 'absolute',
                    top: -4,
                    right: -4,
                    width: 20,
                    height: 20,
                    borderRadius: '50%',
                    bgcolor: 'rgba(12, 18, 28, 0.92)',
                    color: 'text.secondary',
                    fontSize: 14,
                    lineHeight: '20px',
                    cursor: 'pointer',
                    display: 'grid',
                    placeItems: 'center',
                  }}
                >
                  ×
                </Box>
              ) : null}
            </Box>
          )
        })}
        <Box
          component="button"
          type="button"
          aria-label={full ? 'Заменить самый старый цвет' : 'Сохранить цвет'}
          disabled={busy}
          onClick={(e) => {
            stopBubble(e)
            onAdd()
          }}
          onPointerDown={stopBubble}
          sx={{
            appearance: 'none',
            border: '1.5px dashed rgba(232, 238, 246, 0.28)',
            margin: 0,
            padding: 0,
            width: 40,
            height: 40,
            borderRadius: '50%',
            bgcolor: 'transparent',
            color: 'text.secondary',
            cursor: busy ? 'default' : 'pointer',
            opacity: busy ? 0.35 : 1,
            display: 'grid',
            placeItems: 'center',
            '&:active': { transform: busy ? undefined : 'scale(0.94)' },
          }}
        >
          <AddIcon sx={{ fontSize: 22 }} />
        </Box>
      </Box>
      <Typography sx={{ fontSize: '0.7rem', color: 'text.secondary', opacity: 0.8 }}>
        {!persisted
          ? '«+» сохранит текущий цвет в избранное'
          : full
            ? 'Набор полный — «+» заменит самый старый'
            : '× удаляет · «Сбросить» вернёт стандартные'}
      </Typography>
    </Box>
  )
}

export function LedColorPicker({
  open,
  title,
  hue,
  saturation,
  brightness,
  favorites,
  favoritesPersisted,
  favoritesBusy,
  onAddFavorite,
  onRemoveFavorite,
  onResetFavorites,
  onClose,
  onChange,
}: Props) {
  const preview = hsvToCss(hue, saturation, Math.max(brightness, 22))

  return (
    <Dialog
      open={open}
      onClose={onClose}
      fullWidth
      maxWidth="sm"
      slotProps={{
        backdrop: {
          sx: { backgroundColor: 'rgba(4, 8, 14, 0.72)' },
        },
        paper: {
          sx: {
            m: { xs: 1.25, sm: 2 },
            width: '100%',
            maxHeight: '92dvh',
            borderRadius: 3,
            bgcolor: '#121820',
            backgroundImage: 'none',
            border: 'none',
            boxShadow: '0 16px 48px rgba(0,0,0,0.55)',
            overflow: 'hidden',
          },
        },
      }}
    >
      <DialogTitle
        sx={{
          display: 'flex',
          alignItems: 'center',
          gap: 1.25,
          px: 2,
          py: 1.75,
          fontSize: '1.05rem',
          fontWeight: 700,
          borderBottom: 'none',
          boxShadow: 'none',
          '&::after': { display: 'none' },
        }}
      >
        <Box
          aria-hidden
          sx={{
            width: 26,
            height: 26,
            borderRadius: '50%',
            bgcolor: preview,
            flexShrink: 0,
          }}
        />
        <Box
          sx={{
            flex: 1,
            minWidth: 0,
            overflow: 'hidden',
            textOverflow: 'ellipsis',
            whiteSpace: 'nowrap',
          }}
        >
          {title}
        </Box>
        <IconButton
          aria-label="Закрыть"
          onClick={onClose}
          size="large"
          sx={{
            width: 48,
            height: 48,
            color: 'text.secondary',
            bgcolor: 'rgba(232, 238, 246, 0.06)',
            '&:hover': { bgcolor: 'rgba(232, 238, 246, 0.1)' },
            '&:active': { bgcolor: 'rgba(232, 238, 246, 0.14)' },
          }}
        >
          <CloseIcon />
        </IconButton>
      </DialogTitle>

      <DialogContent
        sx={{
          px: 2,
          pt: 0,
          pb: 1.5,
          display: 'grid',
          gap: 2,
          '&.MuiDialogContent-root': { paddingTop: 0 },
        }}
      >
        <HueSatPad
          hue={hue}
          saturation={saturation}
          onChange={(h, s) => onChange({ hue: h, saturation: s, brightness })}
        />
        <BrightnessBar
          value={brightness}
          hue={hue}
          sat={saturation}
          onCommit={(b) => onChange({ hue, saturation, brightness: b })}
        />
        <FavoritesRow
          items={favorites}
          persisted={favoritesPersisted}
          busy={favoritesBusy}
          onPick={onChange}
          onAdd={onAddFavorite}
          onRemove={onRemoveFavorite}
          onReset={onResetFavorites}
        />
      </DialogContent>

      <DialogActions sx={{ px: 2, pb: 2, pt: 0.5 }}>
        <Button
          fullWidth
          variant="contained"
          color="primary"
          size="large"
          disableElevation
          onClick={onClose}
          sx={{
            minHeight: 52,
            fontSize: '1rem',
            fontWeight: 700,
            borderRadius: 999,
            bgcolor: '#5ec8d8',
            backgroundImage: 'linear-gradient(135deg, #7ad4e2 0%, #4fb8ca 55%, #3aa8ba 100%)',
            color: '#061018',
            border: 'none',
            outline: 'none',
            boxShadow: 'none',
            '&:hover': {
              bgcolor: '#6fd0de',
              backgroundImage: 'linear-gradient(135deg, #8adceb 0%, #5ec8d8 55%, #3aa8ba 100%)',
              boxShadow: 'none',
            },
            '&:focus': { boxShadow: 'none' },
            '&:active': { boxShadow: 'none' },
          }}
        >
          Готово
        </Button>
      </DialogActions>
    </Dialog>
  )
}
