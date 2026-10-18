import { useEffect, useRef, useState, type PointerEvent as ReactPointerEvent } from 'react'
import { Box, Typography } from '@mui/material'
import type { Device, Room } from '../../shared/api/types'
import { clampRect } from './clampRect'
import { DeviceTile } from './DeviceTile'
import { deviceGridSx, sortDevicesByLayout, withLinearOrder } from './deviceGrid'

type Props = {
  room: Room
  devices: Device[]
  values: Record<string, string>
  editMode: boolean
  sendCmd: (topic: string, payload: string) => void
  onEditRoom: (room: Room) => void
  onEditDevice: (device: Device) => void
  onRoomLayout?: (room: Room, commit: boolean) => void
  onDevicesReorder?: (ordered: Device[]) => void
}

export function RoomPanel({
  room,
  devices,
  values,
  editMode,
  sendCmd,
  onEditRoom,
  onEditDevice,
  onRoomLayout,
  onDevicesReorder,
}: Props) {
  const list = sortDevicesByLayout(devices)
  const panelRef = useRef<HTMLDivElement | null>(null)
  const [dragOverId, setDragOverId] = useState<number | null>(null)
  const dragDeviceId = useRef<number | null>(null)
  const didDrag = useRef(false)
  const suppressClickTimer = useRef<number | undefined>(undefined)
  const unbindPointer = useRef<(() => void) | null>(null)

  useEffect(() => {
    return () => {
      unbindPointer.current?.()
      if (suppressClickTimer.current !== undefined) {
        window.clearTimeout(suppressClickTimer.current)
      }
    }
  }, [])

  function planRect() {
    const el = panelRef.current?.offsetParent as HTMLElement | null
    return el?.getBoundingClientRect()
  }

  function bindPointerDrag(
    e: ReactPointerEvent,
    apply: (dx: number, dy: number, orig: Room) => Room,
    onTap: () => void,
  ) {
    if (!editMode || !onRoomLayout) return
    e.preventDefault()
    e.stopPropagation()
    const plan = planRect()
    if (!plan) return

    unbindPointer.current?.()

    const startX = e.clientX
    const startY = e.clientY
    const orig = { ...room }
    let moved = false
    let last = orig

    const move = (ev: PointerEvent) => {
      const dx = ((ev.clientX - startX) / plan.width) * 100
      const dy = ((ev.clientY - startY) / plan.height) * 100
      if (Math.abs(dx) + Math.abs(dy) > 0.4) moved = true
      last = apply(dx, dy, orig)
      onRoomLayout(last, false)
    }
    const cleanup = () => {
      window.removeEventListener('pointermove', move)
      window.removeEventListener('pointerup', up)
      window.removeEventListener('pointercancel', up)
      if (unbindPointer.current === cleanup) unbindPointer.current = null
    }
    const up = () => {
      cleanup()
      if (moved) onRoomLayout(last, true)
      else onTap()
    }
    unbindPointer.current = cleanup
    window.addEventListener('pointermove', move)
    window.addEventListener('pointerup', up)
    window.addEventListener('pointercancel', up)
  }

  function onHeaderPointerDown(e: ReactPointerEvent) {
    if ((e.target as HTMLElement).closest('[data-resize]')) return
    bindPointerDrag(
      e,
      (dx, dy, orig) => ({ ...orig, ...clampRect(orig.x + dx, orig.y + dy, orig.w, orig.h) }),
      () => onEditRoom(room),
    )
  }

  function onResizePointerDown(e: ReactPointerEvent) {
    bindPointerDrag(
      e,
      (dw, dh, orig) => ({ ...orig, ...clampRect(orig.x, orig.y, orig.w + dw, orig.h + dh) }),
      () => undefined,
    )
  }

  function reorder(fromId: number, toId: number) {
    if (!onDevicesReorder || fromId === toId) return
    const ordered = sortDevicesByLayout(devices)
    const from = ordered.findIndex((d) => d.id === fromId)
    const to = ordered.findIndex((d) => d.id === toId)
    if (from < 0 || to < 0) return
    const next = [...ordered]
    const [item] = next.splice(from, 1)
    next.splice(to, 0, item)
    onDevicesReorder(withLinearOrder(next))
  }

  function markDragged() {
    didDrag.current = true
    if (suppressClickTimer.current !== undefined) {
      window.clearTimeout(suppressClickTimer.current)
    }
    suppressClickTimer.current = window.setTimeout(() => {
      didDrag.current = false
      suppressClickTimer.current = undefined
    }, 150)
  }

  return (
    <Box
      ref={panelRef}
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
        touchAction: editMode ? 'none' : 'auto',
      }}
    >
      <Typography
        onPointerDown={onHeaderPointerDown}
        sx={{
          px: 1.5,
          py: 0.85,
          fontWeight: 700,
          letterSpacing: '0.08em',
          textTransform: 'uppercase',
          fontSize: '0.72rem',
          textAlign: 'center',
          color: 'text.secondary',
          borderBottom: 1,
          borderColor: 'divider',
          cursor: editMode ? 'grab' : 'default',
          flexShrink: 0,
          userSelect: 'none',
        }}
        title={editMode ? 'Тяните за заголовок · клик — имя' : undefined}
      >
        {room.name}
        {editMode ? ' · ⋮⋮' : ''}
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
          <Box
            key={d.id}
            draggable={editMode}
            onDragStart={(e) => {
              didDrag.current = false
              dragDeviceId.current = d.id
              e.dataTransfer.effectAllowed = 'move'
              e.dataTransfer.setData('text/plain', String(d.id))
            }}
            onDrag={(e) => {
              if (e.clientX !== 0 || e.clientY !== 0) didDrag.current = true
            }}
            onDragOver={(e) => {
              if (!editMode) return
              e.preventDefault()
              setDragOverId(d.id)
            }}
            onDragLeave={() => setDragOverId((id) => (id === d.id ? null : id))}
            onDrop={(e) => {
              e.preventDefault()
              markDragged()
              const from = Number(e.dataTransfer.getData('text/plain') || dragDeviceId.current)
              setDragOverId(null)
              dragDeviceId.current = null
              if (Number.isFinite(from)) reorder(from, d.id)
            }}
            onDragEnd={() => {
              setDragOverId(null)
              dragDeviceId.current = null
              markDragged()
            }}
            sx={{
              outline: dragOverId === d.id ? '2px solid rgba(94, 200, 216, 0.7)' : 'none',
              outlineOffset: 2,
              borderRadius: 2,
              opacity: editMode ? 0.95 : 1,
              cursor: editMode ? 'grab' : 'default',
            }}
          >
            <DeviceTile
              device={d}
              value={values[d.mqttTopic] ?? '—'}
              powerValue={d.powerTopic ? values[d.powerTopic] : undefined}
              editMode={editMode}
              sendCmd={sendCmd}
              onEdit={(dev) => {
                if (didDrag.current) return
                onEditDevice(dev)
              }}
            />
          </Box>
        ))}
      </Box>
      {editMode ? (
        <Box
          data-resize
          onPointerDown={onResizePointerDown}
          sx={{
            position: 'absolute',
            right: 4,
            bottom: 4,
            width: 16,
            height: 16,
            borderRadius: '3px 0 8px 0',
            borderRight: '2px solid',
            borderBottom: '2px solid',
            borderColor: 'primary.main',
            cursor: 'nwse-resize',
            opacity: 0.85,
          }}
          title="Размер"
        />
      ) : null}
    </Box>
  )
}
