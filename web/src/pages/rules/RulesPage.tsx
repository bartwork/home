import { useCallback, useEffect, useState } from 'react'
import {
  Alert,
  Box,
  Button,
  Chip,
  List,
  ListItem,
  ListItemButton,
  ListItemText,
  Paper,
  Stack,
  Switch,
} from '@mui/material'
import { Link as RouterLink, Navigate, useNavigate } from 'react-router-dom'
import {
  createRule,
  deleteRule,
  ensureFloorRules,
  listRules,
  setRuleEnabled,
  type Rule,
} from '../../shared/api/client'
import { useAuth } from '../../shared/auth/AuthContext'
import { pageTitle } from '../../shared/brand'
import { AppShell, ShellLogoutButton } from '../../shared/layout/AppShell'
import { useDocumentTitle } from '../../shared/useDocumentTitle'
import { emptyGraph } from './graph'

export function RulesPage() {
  useDocumentTitle(pageTitle('Правила'))
  const { token, logout } = useAuth()
  const navigate = useNavigate()
  const [rules, setRules] = useState<Rule[]>([])
  const [error, setError] = useState<string | null>(null)
  const [info, setInfo] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  const reload = useCallback(() => {
    listRules()
      .then(setRules)
      .catch((err: Error) => setError(err.message))
  }, [])

  useEffect(() => {
    if (!token) return
    reload()
  }, [token, reload])

  if (!token) return <Navigate to="/login" replace />

  async function onCreate() {
    setBusy(true)
    setError(null)
    try {
      const res = await createRule({ name: 'Новое правило', enabled: true, graph: emptyGraph() })
      navigate(`/rules/${res.rule.id}`)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'ошибка')
    } finally {
      setBusy(false)
    }
  }

  async function onEnsureFloors() {
    setBusy(true)
    setError(null)
    setInfo(null)
    try {
      const res = await ensureFloorRules()
      await reload()
      setInfo(res.created > 0 ? `Создано правил: ${res.created}` : 'Все тёплые полы уже покрыты')
    } catch (err) {
      setError(err instanceof Error ? err.message : 'ошибка')
    } finally {
      setBusy(false)
    }
  }

  return (
    <AppShell
      title="Правила"
      subtitle="Graph-движок: MQTT, cron, термостат. Редактор — React Flow."
      maxWidth="md"
      actions={
        <>
          <Button component={RouterLink} to="/" size="small" color="inherit">
            Дом
          </Button>
          <ShellLogoutButton onClick={logout} />
        </>
      }
    >
      {error ? (
        <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError(null)}>
          {error}
        </Alert>
      ) : null}
      {info ? (
        <Alert severity="success" sx={{ mb: 2 }} onClose={() => setInfo(null)}>
          {info}
        </Alert>
      ) : null}

      <Stack direction="row" spacing={1} sx={{ mb: 2 }}>
        <Button variant="contained" onClick={onCreate} disabled={busy}>
          Новое правило
        </Button>
        <Button variant="outlined" onClick={onEnsureFloors} disabled={busy}>
          Правила тёплых полов
        </Button>
      </Stack>

      <Paper variant="outlined" sx={{ overflow: 'hidden' }}>
        <List disablePadding>
          {rules.length === 0 ? (
            <ListItem>
              <ListItemText primary="Пока нет правил" secondary="Создайте или нажмите «Правила тёплых полов»" />
            </ListItem>
          ) : (
            rules.map((r) => (
              <ListItem
                key={r.id}
                divider
                secondaryAction={
                  <Stack direction="row" spacing={1} sx={{ alignItems: 'center' }}>
                    <Chip size="small" label={r.mode} variant="outlined" />
                    <Switch
                      size="small"
                      checked={r.enabled}
                      onChange={(_, checked) => {
                        setRuleEnabled(r.id, checked)
                          .then(reload)
                          .catch((err: Error) => setError(err.message))
                      }}
                    />
                    <Button
                      size="small"
                      color="error"
                      onClick={() => {
                        if (!confirm(`Удалить «${r.name}»?`)) return
                        deleteRule(r.id)
                          .then(reload)
                          .catch((err: Error) => setError(err.message))
                      }}
                    >
                      Удал.
                    </Button>
                  </Stack>
                }
                disablePadding
              >
                <ListItemButton component={RouterLink} to={`/rules/${r.id}`}>
                  <ListItemText
                    primary={r.name}
                    secondary={`${r.graph?.nodes?.length ?? 0} узлов · id ${r.id}`}
                  />
                </ListItemButton>
              </ListItem>
            ))
          )}
        </List>
      </Paper>
      <Box sx={{ height: 8 }} />
    </AppShell>
  )
}
