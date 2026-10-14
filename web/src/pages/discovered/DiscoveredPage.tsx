import { useCallback, useEffect, useState } from 'react'
import {
  Alert,
  Box,
  Button,
  Chip,
  List,
  ListItem,
  Paper,
  Stack,
  Typography,
} from '@mui/material'
import { Link as RouterLink, Navigate } from 'react-router-dom'
import {
  acceptAllDiscovered,
  acceptDiscovered,
  ignoreDiscovered,
  listDiscovered,
  listRooms,
  type Discovered,
  type Room,
} from '../../shared/api/client'
import { useAuth } from '../../shared/auth/AuthContext'
import { pageTitle } from '../../shared/brand'
import { AppShell, ShellLogoutButton } from '../../shared/layout/AppShell'
import { useDocumentTitle } from '../../shared/useDocumentTitle'
import { AcceptDialog } from './AcceptDialog'

export function DiscoveredPage() {
  useDocumentTitle(pageTitle('Обнаружено'))
  const { token, logout } = useAuth()
  const [items, setItems] = useState<Discovered[]>([])
  const [rooms, setRooms] = useState<Room[]>([])
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)
  const [acceptItem, setAcceptItem] = useState<Discovered | null>(null)

  const reload = useCallback(() => {
    listDiscovered()
      .then(setItems)
      .catch((err: Error) => setError(err.message))
  }, [])

  useEffect(() => {
    if (!token) return
    reload()
    listRooms()
      .then(setRooms)
      .catch(() => undefined)
    const t = window.setInterval(reload, 4000)
    return () => window.clearInterval(t)
  }, [token, reload])

  if (!token) {
    return <Navigate to="/login" replace />
  }

  async function onIgnore(id: number) {
    setBusy(true)
    setError(null)
    try {
      await ignoreDiscovered(id)
      reload()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Ошибка')
    } finally {
      setBusy(false)
    }
  }

  async function onAcceptUseful() {
    setBusy(true)
    setError(null)
    try {
      await acceptAllDiscovered()
      reload()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Ошибка')
    } finally {
      setBusy(false)
    }
  }

  return (
    <AppShell
      title="Обнаружено"
      subtitle="Кандидаты из MQTT. Примите с понятным названием и комнатой — появятся на плане дома."
      maxWidth={false}
      actions={
        <>
          <Button component={RouterLink} to="/" size="small" color="inherit">
            Дом
          </Button>
          <ShellLogoutButton onClick={logout} />
        </>
      }
    >
      <Stack direction="row" sx={{ justifyContent: 'flex-end', mb: 2 }}>
        <Button
          variant="contained"
          size="small"
          disabled={busy || items.length === 0}
          onClick={onAcceptUseful}
        >
          Принять полезные
        </Button>
      </Stack>

      {error ? (
        <Alert severity="error" variant="outlined" sx={{ mb: 2 }}>
          {error}
        </Alert>
      ) : null}

      {items.length === 0 ? (
        <Paper
          elevation={0}
          sx={{ p: 3, border: 1, borderColor: 'divider', bgcolor: 'rgba(18, 26, 36, 0.72)' }}
        >
          <Typography color="text.secondary">
            Пока пусто. Подключите брокер Wirenboard — retained-топики появятся здесь автоматически.
          </Typography>
        </Paper>
      ) : (
        <List disablePadding sx={{ display: 'grid', gap: 1.25 }}>
          {items.map((d) => (
            <ListItem
              key={d.id}
              component={Paper}
              elevation={0}
              sx={{
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'stretch',
                gap: 1.25,
                px: 2,
                py: 1.75,
                border: 1,
                borderColor: 'divider',
                bgcolor: 'rgba(18, 26, 36, 0.72)',
              }}
            >
              <Box sx={{ display: 'flex', justifyContent: 'space-between', gap: 1, flexWrap: 'wrap' }}>
                <Box sx={{ minWidth: 0 }}>
                  <Typography sx={{ fontWeight: 600 }}>{d.title}</Typography>
                  <Typography variant="body2" color="text.secondary" sx={{ wordBreak: 'break-all' }}>
                    {d.mqttTopic}
                  </Typography>
                </Box>
                <Stack direction="row" spacing={0.75} sx={{ alignItems: 'center' }}>
                  <Chip
                    size="small"
                    label={d.suggestedType || 'raw'}
                    color={
                      d.suggestedType === 'switch' ||
                      d.suggestedType === 'range' ||
                      d.suggestedType === 'sensor'
                        ? 'primary'
                        : 'default'
                    }
                    variant="outlined"
                  />
                  {d.lastPayload ? (
                    <Chip size="small" label={d.lastPayload} variant="outlined" />
                  ) : null}
                </Stack>
              </Box>
              <Stack direction="row" spacing={1} sx={{ justifyContent: 'flex-end' }}>
                <Button size="small" disabled={busy} onClick={() => onIgnore(d.id)}>
                  Скрыть
                </Button>
                <Button
                  size="small"
                  variant="contained"
                  disabled={busy}
                  onClick={() => setAcceptItem(d)}
                >
                  Принять
                </Button>
              </Stack>
            </ListItem>
          ))}
        </List>
      )}

      <AcceptDialog
        item={acceptItem}
        rooms={rooms}
        open={Boolean(acceptItem)}
        busy={busy}
        onClose={() => setAcceptItem(null)}
        onAccept={async (opts) => {
          if (!acceptItem) return
          setBusy(true)
          setError(null)
          try {
            await acceptDiscovered(acceptItem.id, opts)
            reload()
          } finally {
            setBusy(false)
          }
        }}
      />
    </AppShell>
  )
}
