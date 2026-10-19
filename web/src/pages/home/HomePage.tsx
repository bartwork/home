import { useCallback, useEffect, useMemo, useState } from 'react'
import { Alert, Badge, Box, Button, Chip, Paper, Typography } from '@mui/material'
import { Link as RouterLink, Navigate } from 'react-router-dom'
import {
  createRoom,
  countDiscovered,
  deleteDevice,
  deleteRoom,
  listDevices,
  listRooms,
  updateDevice,
  updateRoom,
  type Device,
  type Room,
} from '../../shared/api/client'
import { useAuth } from '../../shared/auth/AuthContext'
import { pageTitle } from '../../shared/brand'
import { AppShell, ShellLogoutButton } from '../../shared/layout/AppShell'
import { useRealtime } from '../../shared/realtime/useRealtime'
import { useDocumentTitle } from '../../shared/useDocumentTitle'
import { DeviceTile } from './DeviceTile'
import { deviceGridSx, wideDeviceGridItemSx } from './deviceGrid'
import { EditDeviceDialog } from './EditDeviceDialog'
import { EditRoomDialog, type RoomDraft } from './EditRoomDialog'
import { RoomPanel } from './RoomPanel'

function nextRoomDraft(rooms: Room[]): RoomDraft {
  const n = rooms.length
  return {
    name: '',
    x: 2 + (n % 3) * 4,
    y: 2 + (n % 4) * 4,
    w: 32,
    h: 28,
    sortOrder: 0,
  }
}

export function HomePage() {
  useDocumentTitle(pageTitle('Дом'))
  const { token, user, logout } = useAuth()
  const { connected, mqttConnected, values, sendCmd, lastError, clearError } = useRealtime()
  const [devices, setDevices] = useState<Device[]>([])
  const [rooms, setRooms] = useState<Room[]>([])
  const [discoveredCount, setDiscoveredCount] = useState(0)
  const [error, setError] = useState<string | null>(null)
  const [editMode, setEditMode] = useState(false)
  const [roomDraft, setRoomDraft] = useState<RoomDraft | null>(null)
  const [editDevice, setEditDevice] = useState<Device | null>(null)

  const liveLabel = !connected ? 'offline' : mqttConnected ? 'live' : 'mqtt off'
  const liveColor = !connected ? 'default' : mqttConnected ? 'secondary' : 'warning'

  const reload = useCallback(() => {
    Promise.all([listDevices(), listRooms()])
      .then(([devs, rms]) => {
        setDevices(devs)
        setRooms(rms)
      })
      .catch((err: Error) => setError(err.message))
  }, [])

  useEffect(() => {
    if (!token) return
    let cancelled = false
    reload()
    countDiscovered()
      .then((n) => {
        if (!cancelled) setDiscoveredCount(n)
      })
      .catch(() => undefined)
    const t = window.setInterval(() => {
      countDiscovered()
        .then(setDiscoveredCount)
        .catch(() => undefined)
    }, 5000)
    return () => {
      cancelled = true
      window.clearInterval(t)
    }
  }, [token, reload])

  const byRoom = useMemo(() => {
    const map = new Map<number, Device[]>()
    const unassigned: Device[] = []
    const hidden: Device[] = []
    for (const d of devices) {
      if (!d.active) {
        hidden.push(d)
        continue
      }
      if (d.roomId) {
        const list = map.get(d.roomId) ?? []
        list.push(d)
        map.set(d.roomId, list)
      } else {
        unassigned.push(d)
      }
    }
    return { map, unassigned, hidden }
  }, [devices])

  if (!token) {
    return <Navigate to="/login" replace />
  }

  const subtitle = user
    ? `${user.login}${user.firstName ? ` · ${user.firstName}` : ''}`
    : undefined

  return (
    <AppShell
      title="Дом"
      subtitle={subtitle}
      maxWidth={false}
      actions={
        <>
          <Chip
            size="small"
            label={liveLabel}
            color={liveColor}
            variant="outlined"
            sx={{ textTransform: 'uppercase', letterSpacing: '0.08em', fontWeight: 600 }}
          />
          <Button
            size="small"
            variant={editMode ? 'contained' : 'outlined'}
            color={editMode ? 'secondary' : 'inherit'}
            onClick={() => setEditMode((v) => !v)}
          >
            {editMode ? 'Готово' : 'Раскладка'}
          </Button>
          {editMode ? (
            <Button size="small" color="inherit" onClick={() => setRoomDraft(nextRoomDraft(rooms))}>
              + Комната
            </Button>
          ) : null}
          <Badge badgeContent={discoveredCount} color="primary" max={99}>
            <Button component={RouterLink} to="/discovered" size="small" color="inherit">
              Обнаружено
            </Button>
          </Badge>
          <ShellLogoutButton onClick={logout} />
        </>
      }
    >
      {error ? (
        <Alert severity="error" variant="outlined" sx={{ mb: 2 }}>
          {error}
        </Alert>
      ) : null}
      {lastError ? (
        <Alert severity="warning" variant="outlined" sx={{ mb: 2 }} onClose={clearError}>
          {lastError}
        </Alert>
      ) : null}
      {connected && !mqttConnected ? (
        <Alert severity="warning" variant="outlined" sx={{ mb: 2 }}>
          MQTT к брокеру не подключён — переключатели не управляют железом. Проверьте сеть и `MQTT_BROKER`.
        </Alert>
      ) : null}
      {editMode ? (
        <Alert severity="info" variant="outlined" sx={{ mb: 2 }}>
          Раскладка: тяните комнату за заголовок, угол — размер; плитки — DnD для порядка. Клик без
          движения — имя/настройки.
        </Alert>
      ) : null}

      {rooms.length === 0 && devices.length === 0 ? (
        <Paper
          elevation={0}
          sx={{ p: 3, border: 1, borderColor: 'divider', bgcolor: 'rgba(18, 26, 36, 0.72)' }}
        >
          <Typography color="text.secondary" sx={{ mb: 1.5 }}>
            Пока пусто. Включите «Раскладка» → «+ Комната» или примите топики из MQTT.
          </Typography>
          <Button
            variant="contained"
            size="small"
            sx={{ mr: 1 }}
            onClick={() => {
              setEditMode(true)
              setRoomDraft(nextRoomDraft([]))
            }}
          >
            Добавить комнату
          </Button>
          <Button component={RouterLink} to="/discovered" size="small">
            К обнаруженным
          </Button>
        </Paper>
      ) : (
        <Box
          sx={{
            position: 'relative',
            width: '100%',
            height: 'min(78dvh, 900px)',
            minHeight: 480,
            borderRadius: 3,
            border: 1,
            borderColor: 'divider',
            bgcolor: 'rgba(8, 12, 18, 0.55)',
            overflow: 'hidden',
          }}
        >
          {[...rooms]
            .sort((a, b) => a.sortOrder - b.sortOrder || a.id - b.id)
            .map((room) => (
              <RoomPanel
                key={room.id}
                room={room}
                devices={byRoom.map.get(room.id) ?? []}
                values={values}
                editMode={editMode}
                sendCmd={sendCmd}
                onEditRoom={(r) => setRoomDraft(r)}
                onEditDevice={setEditDevice}
                onRoomLayout={(next, commit) => {
                  setRooms((prev) => prev.map((r) => (r.id === next.id ? next : r)))
                  if (commit) {
                    updateRoom(next).catch((err: Error) => setError(err.message))
                  }
                }}
                onDevicesReorder={(ordered) => {
                  const prevById = new Map(devices.map((d) => [d.id, d]))
                  const changed = ordered.filter((d) => {
                    const old = prevById.get(d.id)
                    return !old || old.x !== d.x || old.y !== d.y
                  })
                  setDevices((prev) => {
                    const byId = new Map(ordered.map((d) => [d.id, d]))
                    return prev.map((d) => byId.get(d.id) ?? d)
                  })
                  if (changed.length === 0) return
                  void Promise.all(changed.map((d) => updateDevice(d))).catch((err: Error) =>
                    setError(err.message),
                  )
                }}
              />
            ))}
        </Box>
      )}

      {byRoom.unassigned.length > 0 ? (
        <Paper
          elevation={0}
          sx={{ mt: 2, p: 2, border: 1, borderColor: 'divider', bgcolor: 'rgba(18, 26, 36, 0.72)' }}
        >
          <Typography variant="overline" color="text.secondary" sx={{ letterSpacing: '0.1em' }}>
            Без комнаты
          </Typography>
          <Box
            sx={{
              ...deviceGridSx,
              mt: 1.25,
              maxWidth: 640,
            }}
          >
            {byRoom.unassigned.map((d) => (
              <Box key={d.id} sx={wideDeviceGridItemSx(d.deviceType)}>
                <DeviceTile
                  device={d}
                  value={values[d.mqttTopic] ?? '—'}
                  powerValue={d.powerTopic ? values[d.powerTopic] : undefined}
                  values={values}
                  editMode={editMode}
                  sendCmd={sendCmd}
                  onEdit={setEditDevice}
                />
              </Box>
            ))}
          </Box>
        </Paper>
      ) : null}

      {editMode && byRoom.hidden.length > 0 ? (
        <Paper
          elevation={0}
          sx={{ mt: 2, p: 2, border: 1, borderColor: 'divider', bgcolor: 'rgba(18, 26, 36, 0.72)' }}
        >
          <Typography variant="overline" color="text.secondary" sx={{ letterSpacing: '0.1em' }}>
            Скрытые с плана
          </Typography>
          <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 1, mt: 1 }}>
            {byRoom.hidden.map((d) => (
              <Chip
                key={d.id}
                label={d.name}
                onClick={() => setEditDevice(d)}
                variant="outlined"
                sx={{ cursor: 'pointer' }}
              />
            ))}
          </Box>
        </Paper>
      ) : null}

      <EditRoomDialog
        room={roomDraft}
        open={Boolean(roomDraft)}
        onClose={() => setRoomDraft(null)}
        onSave={async (draft) => {
          if (draft.id) {
            const res = await updateRoom(draft as Room)
            setRooms((prev) => prev.map((r) => (r.id === res.room.id ? res.room : r)))
            return
          }
          const res = await createRoom({
            name: draft.name,
            x: draft.x,
            y: draft.y,
            w: draft.w,
            h: draft.h,
            sortOrder: draft.sortOrder,
          })
          setRooms((prev) => [...prev, res.room])
        }}
        onDelete={async (id) => {
          await deleteRoom(id)
          setRooms((prev) => prev.filter((r) => r.id !== id))
          setDevices((prev) =>
            prev.map((d) => (d.roomId === id ? { ...d, roomId: undefined, roomName: '' } : d)),
          )
        }}
      />
      <EditDeviceDialog
        device={editDevice}
        rooms={rooms}
        open={Boolean(editDevice)}
        onClose={() => setEditDevice(null)}
        onSave={async (device) => {
          const res = await updateDevice(device)
          setDevices((prev) => prev.map((d) => (d.id === res.device.id ? res.device : d)))
        }}
        onDelete={async (id) => {
          await deleteDevice(id)
          setDevices((prev) => prev.filter((d) => d.id !== id))
        }}
      />
    </AppShell>
  )
}
