import { useEffect, useState } from 'react'
import {
  Alert,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Stack,
  TextField,
} from '@mui/material'
import type { Room } from '../../shared/api/types'
import { parseNum } from './parseNum'

export type RoomDraft = Omit<Room, 'id'> & { id?: number }

type Props = {
  room: RoomDraft | null
  open: boolean
  onClose: () => void
  onSave: (room: RoomDraft) => Promise<void>
  onDelete?: (id: number) => Promise<void>
}

export function EditRoomDialog({ room, open, onClose, onSave, onDelete }: Props) {
  const isNew = !room?.id
  const [name, setName] = useState('')
  const [x, setX] = useState('0')
  const [y, setY] = useState('0')
  const [w, setW] = useState('32')
  const [h, setH] = useState('28')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!room) return
    setName(room.name)
    setX(String(room.x))
    setY(String(room.y))
    setW(String(room.w))
    setH(String(room.h))
    setError(null)
  }, [room])

  async function save() {
    if (!room) return
    const trimmed = name.trim()
    if (!trimmed) {
      setError('Укажите название')
      return
    }
    setBusy(true)
    setError(null)
    try {
      await onSave({
        ...room,
        name: trimmed,
        x: parseNum(x, room.x),
        y: parseNum(y, room.y),
        w: parseNum(w, room.w),
        h: parseNum(h, room.h),
      })
      onClose()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Ошибка')
    } finally {
      setBusy(false)
    }
  }

  async function remove() {
    if (!room?.id || !onDelete) return
    if (!window.confirm(`Удалить комнату «${room.name}»? Устройства останутся без комнаты.`)) return
    setBusy(true)
    setError(null)
    try {
      await onDelete(room.id)
      onClose()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Ошибка')
    } finally {
      setBusy(false)
    }
  }

  return (
    <Dialog open={open} onClose={onClose} fullWidth maxWidth="xs">
      <DialogTitle>{isNew ? 'Новая комната' : 'Комната'}</DialogTitle>
      <DialogContent>
        <Stack spacing={2} sx={{ pt: 1 }}>
          <TextField
            label="Название"
            value={name}
            onChange={(e) => setName(e.target.value)}
            fullWidth
            autoFocus
          />
          <Stack direction="row" spacing={1}>
            <TextField label="X %" value={x} onChange={(e) => setX(e.target.value)} fullWidth />
            <TextField label="Y %" value={y} onChange={(e) => setY(e.target.value)} fullWidth />
          </Stack>
          <Stack direction="row" spacing={1}>
            <TextField label="Ширина %" value={w} onChange={(e) => setW(e.target.value)} fullWidth />
            <TextField label="Высота %" value={h} onChange={(e) => setH(e.target.value)} fullWidth />
          </Stack>
          {error ? (
            <Alert severity="error" variant="outlined">
              {error}
            </Alert>
          ) : null}
        </Stack>
      </DialogContent>
      <DialogActions sx={{ justifyContent: 'space-between' }}>
        {!isNew && onDelete ? (
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
            {isNew ? 'Создать' : 'Сохранить'}
          </Button>
        </Stack>
      </DialogActions>
    </Dialog>
  )
}
