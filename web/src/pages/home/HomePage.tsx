import { useEffect, useState } from 'react'
import {
  Alert,
  AppBar,
  Box,
  Button,
  Chip,
  Container,
  List,
  ListItem,
  Paper,
  Stack,
  Switch,
  Toolbar,
  Typography,
} from '@mui/material'
import { Navigate } from 'react-router-dom'
import { listDevices, type Device } from '../../shared/api/client'
import { useAuth } from '../../shared/auth/AuthContext'
import { useRealtime } from '../../shared/realtime/useRealtime'
import { APP_NAME, pageTitle } from '../../shared/brand'
import { useDocumentTitle } from '../../shared/useDocumentTitle'

function switchOn(value: string) {
  return value === '1' || value === 'true' || value === 'on'
}

export function HomePage() {
  useDocumentTitle(pageTitle('Устройства'))
  const { token, user, logout } = useAuth()
  const { connected, values, sendCmd } = useRealtime()
  const [devices, setDevices] = useState<Device[]>([])
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!token) return
    let cancelled = false
    listDevices()
      .then((res) => {
        if (!cancelled) setDevices(res.devices ?? [])
      })
      .catch((err: Error) => {
        if (!cancelled) setError(err.message)
      })
    return () => {
      cancelled = true
    }
  }, [token])

  if (!token) {
    return <Navigate to="/login" replace />
  }

  return (
    <Box
      component="main"
      sx={{
        minHeight: '100dvh',
        background: [
          'radial-gradient(900px 420px at 85% -10%, rgba(94, 200, 216, 0.12), transparent 55%)',
          'radial-gradient(700px 360px at 10% 100%, rgba(240, 179, 90, 0.08), transparent 50%)',
          'var(--mui-palette-background-default)',
        ].join(', '),
      }}
    >
      <AppBar
        position="sticky"
        elevation={0}
        color="transparent"
        sx={{ borderBottom: 1, borderColor: 'divider', backdropFilter: 'blur(10px)' }}
      >
        <Toolbar sx={{ gap: 2, justifyContent: 'space-between' }}>
          <Typography variant="h1" sx={{ fontSize: '1.8rem' }}>
            {APP_NAME}
          </Typography>
          <Stack direction="row" spacing={1.5} sx={{ alignItems: 'center' }}>
            <Chip
              size="small"
              label={connected ? 'live' : 'offline'}
              color={connected ? 'secondary' : 'default'}
              variant="outlined"
              sx={{ textTransform: 'uppercase', letterSpacing: '0.08em', fontWeight: 600 }}
            />
            <Button variant="outlined" color="inherit" onClick={logout} size="small">
              Выйти
            </Button>
          </Stack>
        </Toolbar>
      </AppBar>

      <Container maxWidth="sm" sx={{ py: { xs: 3, sm: 4 } }}>
        <Typography variant="h2" sx={{ fontSize: { xs: '1.8rem', sm: '2.4rem' }, mb: 0.5 }}>
          Устройства
        </Typography>
        <Typography color="text.secondary" sx={{ mb: 3 }}>
          {user
            ? `${user.login}${user.firstName ? ` · ${user.firstName}` : ''} · MQTT → WebSocket`
            : 'MQTT → WebSocket'}
        </Typography>

        {error ? (
          <Alert severity="error" variant="outlined" sx={{ mb: 2 }}>
            {error}
          </Alert>
        ) : null}

        <List disablePadding sx={{ display: 'grid', gap: 1.5 }}>
          {devices.map((d) => {
            const value = values[d.mqttTopic] ?? '—'
            const on = switchOn(value)
            return (
              <ListItem
                key={d.id}
                component={Paper}
                elevation={0}
                sx={{
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'space-between',
                  gap: 2,
                  px: 2,
                  py: 1.75,
                  border: 1,
                  borderColor: 'divider',
                  bgcolor: 'rgba(18, 26, 36, 0.72)',
                }}
              >
                <Box sx={{ minWidth: 0 }}>
                  <Typography sx={{ fontWeight: 600 }}>{d.name}</Typography>
                  <Typography
                    variant="body2"
                    color="text.secondary"
                    sx={{ wordBreak: 'break-all', mt: 0.25 }}
                  >
                    {d.mqttTopic}
                  </Typography>
                </Box>
                <Box sx={{ flexShrink: 0 }}>
                  {d.deviceType === 'switch' ? (
                    <Switch
                      checked={on}
                      color="primary"
                      slotProps={{ input: { 'aria-label': d.name } }}
                      onChange={(_, checked) => sendCmd(d.mqttTopic, checked ? '1' : '0')}
                    />
                  ) : (
                    <Typography
                      variant="h3"
                      sx={{ fontSize: '1.25rem', fontFamily: '"Syne", sans-serif' }}
                    >
                      {value}
                      {d.unit ? ` ${d.unit}` : ''}
                    </Typography>
                  )}
                </Box>
              </ListItem>
            )
          })}
        </List>
      </Container>
    </Box>
  )
}
