import { useCallback, useEffect, useMemo, useState } from 'react'
import {
  Alert,
  Box,
  Button,
  MenuItem,
  Paper,
  Stack,
  TextField,
  Typography,
} from '@mui/material'
import {
  Background,
  Controls,
  MiniMap,
  ReactFlow,
  addEdge,
  useEdgesState,
  useNodesState,
  type Connection,
  type Edge,
  type Node,
} from '@xyflow/react'
import '@xyflow/react/dist/style.css'
import { Link as RouterLink, Navigate, useNavigate, useParams } from 'react-router-dom'
import { getRule, runRule, updateRule } from '../../shared/api/client'
import { useAuth } from '../../shared/auth/AuthContext'
import { pageTitle } from '../../shared/brand'
import { useDocumentTitle } from '../../shared/useDocumentTitle'
import { NODE_CATALOG, fromFlow, newNodeId, toFlow } from './graph'
import { NodeInspector } from './NodeInspector'
import { ruleNodeTypes } from './nodes'

export function RuleEditorPage() {
  const { id } = useParams()
  const ruleId = Number(id)
  useDocumentTitle(pageTitle('Редактор правил'))
  const { token, logout } = useAuth()
  const navigate = useNavigate()

  const [name, setName] = useState('')
  const [mode, setMode] = useState('single')
  const [cooldownMs, setCooldownMs] = useState(0)
  const [enabled, setEnabled] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [info, setInfo] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)
  const [selectedId, setSelectedId] = useState<string | null>(null)

  const [nodes, setNodes, onNodesChange] = useNodesState<Node>([])
  const [edges, setEdges, onEdgesChange] = useEdgesState<Edge>([])

  useEffect(() => {
    if (!token || !Number.isFinite(ruleId)) return
    getRule(ruleId)
      .then((res) => {
        setName(res.rule.name)
        setMode(res.rule.mode || 'single')
        setCooldownMs(res.rule.cooldownMs ?? 0)
        setEnabled(res.rule.enabled)
        const flow = toFlow(res.rule.graph ?? { nodes: [], edges: [] })
        setNodes(flow.nodes)
        setEdges(flow.edges)
      })
      .catch((err: Error) => setError(err.message))
  }, [token, ruleId, setNodes, setEdges])

  const selected = useMemo(
    () => nodes.find((n) => n.id === selectedId) ?? null,
    [nodes, selectedId],
  )

  const onConnect = useCallback(
    (connection: Connection) =>
      setEdges((eds: Edge[]) =>
        addEdge({ ...connection, id: `e_${crypto.randomUUID().slice(0, 8)}` }, eds),
      ),
    [setEdges],
  )

  function addNode(type: string, defaults: Record<string, unknown>) {
    const id = newNodeId(type)
    setNodes((ns: Node[]) => [
      ...ns,
      {
        id,
        type,
        position: { x: 120 + ns.length * 24, y: 80 + (ns.length % 5) * 40 },
        data: { ...defaults },
      },
    ])
    setSelectedId(id)
  }

  async function onSave() {
    setBusy(true)
    setError(null)
    setInfo(null)
    try {
      await updateRule(ruleId, {
        name: name.trim() || 'rule',
        enabled,
        mode,
        cooldownMs,
        graph: fromFlow(nodes, edges),
      })
      setInfo('Сохранено')
    } catch (err) {
      setError(err instanceof Error ? err.message : 'ошибка')
    } finally {
      setBusy(false)
    }
  }

  async function onRun() {
    setBusy(true)
    setError(null)
    try {
      await runRule(ruleId)
      setInfo('Запущено')
    } catch (err) {
      setError(err instanceof Error ? err.message : 'ошибка')
    } finally {
      setBusy(false)
    }
  }

  if (!token) return <Navigate to="/login" replace />
  if (!Number.isFinite(ruleId)) return <Navigate to="/rules" replace />

  return (
    <Box sx={{ height: '100dvh', display: 'flex', flexDirection: 'column', bgcolor: 'background.default' }}>
      <Stack
        direction="row"
        spacing={1}
        sx={{
          px: 1.5,
          py: 1,
          alignItems: 'center',
          borderBottom: 1,
          borderColor: 'divider',
          flexWrap: 'wrap',
          gap: 1,
        }}
      >
        <Button component={RouterLink} to="/rules" size="small" color="inherit">
          ← Правила
        </Button>
        <TextField
          size="small"
          label="Имя"
          value={name}
          onChange={(e) => setName(e.target.value)}
          sx={{ minWidth: 180 }}
        />
        <TextField
          size="small"
          select
          label="mode"
          value={mode}
          onChange={(e) => setMode(e.target.value)}
          sx={{ width: 120 }}
        >
          <MenuItem value="single">single</MenuItem>
          <MenuItem value="restart">restart</MenuItem>
          <MenuItem value="queued">queued</MenuItem>
        </TextField>
        <TextField
          size="small"
          type="number"
          label="cooldown ms"
          value={cooldownMs}
          onChange={(e) => setCooldownMs(Number(e.target.value))}
          sx={{ width: 120 }}
        />
        <Button variant="contained" size="small" onClick={onSave} disabled={busy}>
          Сохранить
        </Button>
        <Button variant="outlined" size="small" onClick={onRun} disabled={busy}>
          Run
        </Button>
        <Button size="small" color="inherit" onClick={logout}>
          Выйти
        </Button>
      </Stack>

      {error ? (
        <Alert severity="error" onClose={() => setError(null)}>
          {error}
        </Alert>
      ) : null}
      {info ? (
        <Alert severity="success" onClose={() => setInfo(null)}>
          {info}
        </Alert>
      ) : null}

      <Box sx={{ flex: 1, display: 'grid', gridTemplateColumns: { xs: '1fr', md: '160px 1fr 280px' }, minHeight: 0 }}>
        <Paper
          square
          variant="outlined"
          sx={{ p: 1, overflow: 'auto', borderTop: 0, display: { xs: 'none', md: 'block' } }}
        >
          <Typography sx={{ fontWeight: 700, fontSize: 12, mb: 1 }}>Узлы</Typography>
          <Stack spacing={0.5}>
            {NODE_CATALOG.map((item) => (
              <Button
                key={item.type}
                size="small"
                variant="outlined"
                onClick={() => addNode(item.type, item.defaults)}
                sx={{ justifyContent: 'flex-start', textTransform: 'none' }}
              >
                {item.label}
              </Button>
            ))}
          </Stack>
        </Paper>

        <Box sx={{ minHeight: 360, height: '100%' }}>
          <ReactFlow
            nodes={nodes}
            edges={edges}
            onNodesChange={onNodesChange}
            onEdgesChange={onEdgesChange}
            onConnect={onConnect}
            nodeTypes={ruleNodeTypes}
            fitView
            onSelectionChange={({ nodes: sel }: { nodes: Node[] }) =>
              setSelectedId(sel[0]?.id ?? null)
            }
            deleteKeyCode={['Backspace', 'Delete']}
            proOptions={{ hideAttribution: true }}
          >
            <Background gap={18} size={1} />
            <Controls />
            <MiniMap pannable zoomable />
          </ReactFlow>
        </Box>

        <Paper square variant="outlined" sx={{ p: 1.5, overflow: 'auto', borderTop: 0 }}>
          <Typography sx={{ fontWeight: 700, fontSize: 12, mb: 1 }}>Свойства</Typography>
          <NodeInspector
            node={selected}
            onChange={(nid, data) => {
              setNodes((ns: Node[]) => ns.map((n: Node) => (n.id === nid ? { ...n, data } : n)))
            }}
          />
          <Stack direction="row" spacing={1} sx={{ mt: 2, display: { xs: 'flex', md: 'none' }, flexWrap: 'wrap' }}>
            {NODE_CATALOG.slice(0, 4).map((item) => (
              <Button key={item.type} size="small" onClick={() => addNode(item.type, item.defaults)}>
                + {item.label}
              </Button>
            ))}
          </Stack>
          <Button size="small" sx={{ mt: 2 }} onClick={() => navigate('/rules')}>
            К списку
          </Button>
        </Paper>
      </Box>
    </Box>
  )
}
