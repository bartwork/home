import { Box, Typography } from '@mui/material'
import type { Device, Room } from '../../shared/api/types'
import { DeviceTile } from './DeviceTile'
import { deviceGridSx } from './deviceGrid'

type Props = {
  room: Room
  devices: Device[]
  values: Record<string, string>
  editMode: boolean
  sendCmd: (topic: string, payload: string) => void
  onEditRoom: (room: Room) => void
  onEditDevice: (device: Device) => void
}

function sortDevices(devices: Device[]): Device[] {
  return [...devices].sort((a, b) => a.y - b.y || a.x - b.x || a.id - b.id)
}

export function RoomPanel({
  room,
  devices,
  values,
  editMode,
  sendCmd,
  onEditRoom,
  onEditDevice,
}: Props) {
  const list = sortDevices(devices)

  return (
    <Box
      sx={{
        position: 'absolute',
        left: `${room.x}%`,
        top: `${room.y}%`,
        width: `${room.w}%`,
        height: `${room.h}%`,
        borderRadius: 3,
        border: 1,
        borderColor: editMode ? 'primary.main' : 'divider',
        bgcolor: 'rgba(12, 18, 28, 0.72)',
        display: 'flex',
        flexDirection: 'column',
        overflow: 'hidden',
      }}
    >
      <Typography
        onClick={editMode ? () => onEditRoom(room) : undefined}
        sx={{
          px: 1.5,
          py: 0.85,
          fontWeight: 700,
          letterSpacing: '0.08em',
          textTransform: 'uppercase',
          fontSize: '0.72rem',
          color: 'text.secondary',
          borderBottom: 1,
          borderColor: 'divider',
          cursor: editMode ? 'pointer' : 'default',
          flexShrink: 0,
        }}
      >
        {room.name}
      </Typography>
      <Box
        sx={{
          flex: 1,
          minHeight: 0,
          overflow: 'auto',
          p: 1.1,
          ...deviceGridSx,
        }}
      >
        {list.map((d) => (
          <DeviceTile
            key={d.id}
            device={d}
            value={values[d.mqttTopic] ?? '—'}
            powerValue={d.powerTopic ? values[d.powerTopic] : undefined}
            editMode={editMode}
            sendCmd={sendCmd}
            onEdit={onEditDevice}
          />
        ))}
      </Box>
    </Box>
  )
}
