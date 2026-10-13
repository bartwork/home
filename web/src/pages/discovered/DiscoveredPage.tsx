import { useCallback, useEffect, useState } from 'react'
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
  Toolbar,
  Typography,
} from '@mui/material'
import { Link as RouterLink, Navigate } from 'react-router-dom'
import {
  acceptAllDiscovered,
  acceptDiscovered,
  ignoreDiscovered,
  listDiscovered,
  type Discovered,
} from '../../shared/api/client'
import { useAuth } from '../../shared/auth/AuthContext'
import { APP_NAME, pageTitle } from '../../shared/brand'
import { useDocumentTitle } from '../../shared/useDocumentTitle'

export function DiscoveredPage() {
  useDocumentTitle(pageTitle('Обнаружено'))
  const { token, logout } = useAuth()
  const [items, setItems] = useState<Discovered[]>([])
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  const reload = useCallback(() => {
    listDiscovered()
      .then((res) => setItems(res.discovered ?? []))
      .catch((err: Error) => setError(err.message))
  }, [])

  useEffect(() => {
    if (!token) return
    reload()
    const t = window.setInterval(reload, 4000)
    return () => window.clearInterval(t)
  }, [token, reload])

  if (!token) {
    return <Navigate to="/login" replace />
  }

  async function onAccept(id: number) {
    setBusy(true)
    setError(null)
    try {
      await acceptDiscovered(id)
      reload()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Ошибка')
    } finally {
      setBusy(false)
    }
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
      await acceptAllDiscovered('switch')
      await acceptAllDiscovered('sensor')
      reload()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Ошибка')
    } finally {
      setBusy(false)
    }
  }

  return (
    <Box
      component="main"
      sx={{
        minHeight: '100dvh',
        background: [
          'radial-gradient(900px 420px at 85% -10%, rgba(94, 200, 216, 0.12), transparent 55%)',
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
          <Typography variant="h1" sx={{ fontSize: '1.5rem' }}>
            {APP_NAME}
          </Typography>
          <Stack direction="row" spacing={1} sx={{ alignItems: 'center' }}>
            <Button component={RouterLink} to="/" size="small" color="inherit">
              Устройства
            </Button>
            <Button variant="outlined" color="inherit" onClick={logout} size="small">
              Выйти
            </Button>
          </Stack>
        </Toolbar>
      </AppBar>

      <Container maxWidth="sm" sx={{ py: { xs: 3, sm: 4 } }}>
        <Stack
          direction="row"
          spacing={1}
          sx={{ alignItems: 'center', justifyContent: 'space-between', mb: 1, flexWrap: 'wrap', gap: 1 }}
        >
          <Typography variant="h2" sx={{ fontSize: { xs: '1.6rem', sm: '2rem' } }}>
            Обнаружено
          </Typography>
          <Button
            variant="contained"
            size="small"
            disabled={busy || items.length === 0}
            onClick={onAcceptUseful}
          >
            Принять switch/sensor
          </Button>
        </Stack>
        <Typography color="text.secondary" sx={{ mb: 3 }}>
          Кандидаты из MQTT `/devices/#`. Примите нужные — появятся на главной как виджеты.
        </Typography>

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
                    <Typography sx={{ fontWeight: 600 }}>{d.title || d.wbControl}</Typography>
                    <Typography variant="body2" color="text.secondary" sx={{ wordBreak: 'break-all' }}>
                      {d.mqttTopic}
                    </Typography>
                  </Box>
                  <Stack direction="row" spacing={0.75} sx={{ alignItems: 'center' }}>
                    <Chip
                      size="small"
                      label={d.suggestedType || 'unknown'}
                      color={d.suggestedType === 'switch' || d.suggestedType === 'sensor' ? 'primary' : 'default'}
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
                    onClick={() => onAccept(d.id)}
                  >
                    Принять
                  </Button>
                </Stack>
              </ListItem>
            ))}
          </List>
        )}
      </Container>
    </Box>
  )
}
