import { useEffect, useState } from 'react'
import {
  Alert,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  FormControl,
  FormControlLabel,
  InputLabel,
  MenuItem,
  Select,
  Stack,
  Switch,
  TextField,
} from '@mui/material'
import type { Device, Room } from '../../shared/api/types'
import { ICON_OPTIONS } from './icons'
import { parseNum } from './parseNum'

type Props = {
  device: Device | null
  rooms: Room[]
  open: boolean
  onClose: () => void
  onSave: (device: Device) => Promise<void>
  onDelete?: (id: number) => Promise<void>
}

export function EditDeviceDialog({ device, rooms, open, onClose, onSave, onDelete }: Props) {
  const [name, setName] = useState('')
  const [powerTopic, setPowerTopic] = useState('')
  const [roomId, setRoomId] = useState(0)
  const [icon, setIcon] = useState('')
  const [x, setX] = useState('4')
  const [y, setY] = useState('4')
  const [active, setActive] = useState(true)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!device) return
    setName(device.name)
    setPowerTopic(device.powerTopic || '')
    setRoomId(device.roomId ?? 0)
    setIcon(device.icon || '')
    setX(String(device.x))
    setY(String(device.y))
    setActive(device.active)
    setError(null)
  }, [device])

  async function save() {
    if (!device) return
    setBusy(true)
    setError(null)
    try {
      const room = rooms.find((r) => r.id === roomId)
      await onSave({
        ...device,
        name: name.trim() || device.name,
        powerTopic: powerTopic.trim(),
        roomId: roomId || undefined,
        roomName: room?.name ?? '',
        icon,
        x: parseNum(x, device.x),
        y: parseNum(y, device.y),
        active,
      })
      onClose()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Ошибка')
    } finally {
      setBusy(false)
    }
  }

  async function remove() {
    if (!device || !onDelete) return
    if (!window.confirm(`Удалить «${device.name}»? Топик снова появится в «Обнаружено».`)) return
    setBusy(true)
    setError(null)
    try {
      await onDelete(device.id)
      onClose()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Ошибка')
    } finally {
      setBusy(false)
    }
  }

  return (
    <Dialog open={open} onClose={onClose} fullWidth maxWidth="xs">
      <DialogTitle>Устройство</DialogTitle>
      <DialogContent>
        <Stack spacing={2} sx={{ pt: 1 }}>
          <TextField label="Название" value={name} onChange={(e) => setName(e.target.value)} fullWidth />
          {device?.deviceType === 'range' ? (
            <TextField
              label="Топик вкл/выкл"
              value={powerTopic}
              onChange={(e) => setPowerTopic(e.target.value)}
              fullWidth
              helperText="Пусто = без кнопки питания. MDM3: Channel N → KN; LED: … Brightness → без суффикса"
            />
          ) : null}
          <FormControl fullWidth>
            <InputLabel id="dev-room">Комната</InputLabel>
            <Select
              labelId="dev-room"
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
            <InputLabel id="dev-icon">Иконка</InputLabel>
            <Select
              labelId="dev-icon"
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
          <Stack direction="row" spacing={1}>
            <TextField
              label="Порядок X"
              value={x}
              onChange={(e) => setX(e.target.value)}
              fullWidth
              helperText="сортировка в сетке"
            />
            <TextField label="Порядок Y" value={y} onChange={(e) => setY(e.target.value)} fullWidth />
          </Stack>
          <FormControlLabel
            control={<Switch checked={active} onChange={(e) => setActive(e.target.checked)} />}
            label="Показывать на плане"
          />
          {error ? (
            <Alert severity="error" variant="outlined">
              {error}
            </Alert>
          ) : null}
        </Stack>
      </DialogContent>
      <DialogActions sx={{ justifyContent: 'space-between' }}>
        {onDelete ? (
          <Button color="error" onClick={remove} disabled={busy}>
            Удалить
          </Button>
        ) : (
          <span />
        )}
        <Stack direction="row" spacing={1}>
          <Button onClick={onClose} disabled={busy}>
            Отмена
          </Button>
          <Button variant="contained" onClick={save} disabled={busy}>
            Сохранить
          </Button>
        </Stack>
      </DialogActions>
    </Dialog>
  )
}
