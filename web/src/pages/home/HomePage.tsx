import { useEffect, useMemo, useState } from 'react'
import {
  Alert,
  AppBar,
  Badge,
  Box,
  Button,
  Chip,
  Container,
  Paper,
  Stack,
  Switch,
  Toolbar,
  Typography,
} from '@mui/material'
import { Link as RouterLink, Navigate } from 'react-router-dom'
import {
  countDiscovered,
  listDevices,
  type Device,
} from '../../shared/api/client'
import { useAuth } from '../../shared/auth/AuthContext'
import { useRealtime } from '../../shared/realtime/useRealtime'
import { APP_NAME, pageTitle } from '../../shared/brand'
import { useDocumentTitle } from '../../shared/useDocumentTitle'

function switchOn(value: string) {
  return value === '1' || value === 'true' || value === 'on'
}

function groupKey(d: Device) {
  return d.wbDevice || 'Прочее'
}

export function HomePage() {
  useDocumentTitle(pageTitle('Устройства'))
  const { token, user, logout } = useAuth()
  const { connected, values, sendCmd } = useRealtime()
  const [devices, setDevices] = useState<Device[]>([])
  const [discoveredCount, setDiscoveredCount] = useState(0)
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
    countDiscovered()
      .then((res) => {
        if (!cancelled) setDiscoveredCount(res.count ?? 0)
      })
      .catch(() => {
        /* ignore */
      })
    const t = window.setInterval(() => {
      countDiscovered()
        .then((res) => setDiscoveredCount(res.count ?? 0))
        .catch(() => undefined)
    }, 5000)
    return () => {
      cancelled = true
      window.clearInterval(t)
    }
  }, [token])

  const groups = useMemo(() => {
    const map = new Map<string, Device[]>()
    for (const d of devices) {
      const key = groupKey(d)
      const list = map.get(key) ?? []
      list.push(d)
      map.set(key, list)
    }
    return [...map.entries()]
  }, [devices])

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
            <Badge badgeContent={discoveredCount} color="primary" max={99}>
              <Button component={RouterLink} to="/discovered" size="small" color="inherit">
                Обнаружено
              </Button>
            </Badge>
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
            ? `${user.login}${user.firstName ? ` · ${user.firstName}` : ''}`
            : 'Виджеты по типу'}
        </Typography>

        {error ? (
          <Alert severity="error" variant="outlined" sx={{ mb: 2 }}>
            {error}
          </Alert>
        ) : null}

        {devices.length === 0 ? (
          <Paper
            elevation={0}
            sx={{ p: 3, border: 1, borderColor: 'divider', bgcolor: 'rgba(18, 26, 36, 0.72)' }}
          >
            <Typography color="text.secondary" sx={{ mb: 1.5 }}>
              Пока нет устройств. Дождитесь MQTT или примите найденные control’ы.
            </Typography>
            <Button component={RouterLink} to="/discovered" variant="contained" size="small">
              К обнаруженным
            </Button>
          </Paper>
        ) : (
          <Stack spacing={3}>
            {groups.map(([group, list]) => (
              <Box key={group}>
                <Typography
                  variant="overline"
                  color="text.secondary"
                  sx={{ letterSpacing: '0.12em', mb: 1, display: 'block' }}
                >
                  {group}
                </Typography>
                <Stack spacing={1.25}>
                  {list.map((d) => {
                    const value = values[d.mqttTopic] ?? '—'
                    const on = switchOn(value)
                    return (
                      <Paper
                        key={d.id}
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
                              onChange={(_, checked) =>
                                sendCmd(d.mqttTopic, checked ? '1' : '0')
                              }
                            />
                          ) : d.deviceType === 'sensor' || d.deviceType === 'range' ? (
                            <Typography
                              variant="h3"
                              sx={{ fontSize: '1.25rem', fontFamily: '"Syne", sans-serif' }}
                            >
                              {value}
                              {d.unit ? ` ${d.unit}` : ''}
                            </Typography>
                          ) : (
                            <Typography variant="body2" color="text.secondary">
                              {value}
                            </Typography>
                          )}
                        </Box>
                      </Paper>
                    )
                  })}
                </Stack>
              </Box>
            ))}
          </Stack>
        )}
      </Container>
    </Box>
  )
}
