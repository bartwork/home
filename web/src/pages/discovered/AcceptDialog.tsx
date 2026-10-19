import { useEffect, useState } from 'react'
import {
  Alert,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  FormControl,
  InputLabel,
  MenuItem,
  Select,
  Stack,
  TextField,
  Typography,
} from '@mui/material'
import type { Discovered, Room } from '../../shared/api/types'
import { DEVICE_TYPE_OPTIONS } from '../../shared/api/deviceTypes'
import { ICON_OPTIONS } from '../home/icons'

type Props = {
  item: Discovered | null
  rooms: Room[]
  open: boolean
  busy: boolean
  onClose: () => void
  onAccept: (opts: {
    name: string
    roomId: number
    icon: string
    deviceType: string
  }) => Promise<void>
}

function defaultIcon(suggestedType: string): string {
  if (suggestedType === 'climate') return 'ac'
  if (suggestedType === 'led') return 'light'
  if (suggestedType === 'floor') return 'thermo'
  if (suggestedType === 'range') return 'dimmer'
  if (suggestedType === 'switch') return 'light'
  if (suggestedType === 'sensor') return 'sensor'
  return ''
}

export function AcceptDialog({ item, rooms, open, busy, onClose, onAccept }: Props) {
  const [name, setName] = useState('')
  const [roomId, setRoomId] = useState(0)
  const [icon, setIcon] = useState('')
  const [deviceType, setDeviceType] = useState('raw')
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!item) return
    setName(item.title || item.wbControl || '')
    setRoomId(0)
    const typ = item.suggestedType || 'raw'
    setDeviceType(typ)
    setIcon(defaultIcon(typ))
    setError(null)
  }, [item, rooms])

  async function submit() {
    setError(null)
    try {
      await onAccept({ name: name.trim(), roomId, icon, deviceType })
      onClose()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Ошибка')
    }
  }

  return (
    <Dialog open={open} onClose={onClose} fullWidth maxWidth="xs">
      <DialogTitle>Принять устройство</DialogTitle>
      <DialogContent>
        <Stack spacing={2} sx={{ pt: 1 }}>
          {item ? (
            <Typography variant="body2" color="text.secondary" sx={{ wordBreak: 'break-all' }}>
              {item.mqttTopic}
            </Typography>
          ) : null}
          <TextField
            label="Название"
            value={name}
            onChange={(e) => setName(e.target.value)}
            fullWidth
            autoFocus
          />
          <FormControl fullWidth>
            <InputLabel id="accept-type">Тип</InputLabel>
            <Select
              labelId="accept-type"
              label="Тип"
              value={deviceType}
              onChange={(e) => {
                const next = String(e.target.value)
                setDeviceType(next)
                if (!icon || icon === defaultIcon(deviceType)) {
                  setIcon(defaultIcon(next))
                }
              }}
            >
              {DEVICE_TYPE_OPTIONS.map((o) => (
                <MenuItem key={o.value} value={o.value}>
                  {o.label}
                </MenuItem>
              ))}
            </Select>
          </FormControl>
          <FormControl fullWidth>
            <InputLabel id="accept-room">Комната</InputLabel>
            <Select
              labelId="accept-room"
              label="Комната"
              value={roomId}
              onChange={(e) => setRoomId(Number(e.target.value))}
            >
              <MenuItem value={0}>без комнаты</MenuItem>
              {rooms.map((r) => (
                <MenuItem key={r.id} value={r.id}>
                  {r.name}
                </MenuItem>
              ))}
            </Select>
          </FormControl>
          <FormControl fullWidth>
            <InputLabel id="accept-icon">Иконка</InputLabel>
            <Select
              labelId="accept-icon"
              label="Иконка"
              value={icon}
              onChange={(e) => setIcon(String(e.target.value))}
            >
              {ICON_OPTIONS.map((o) => (
                <MenuItem key={o.value || 'auto'} value={o.value}>
                  {o.label}
                </MenuItem>
              ))}
            </Select>
          </FormControl>
          {error ? (
            <Alert severity="error" variant="outlined">
              {error}
            </Alert>
          ) : null}
        </Stack>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose} disabled={busy}>
          Отмена
        </Button>
        <Button variant="contained" onClick={submit} disabled={busy || !name.trim()}>
          Принять
        </Button>
      </DialogActions>
    </Dialog>
  )
}
