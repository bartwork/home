import {
  Box,
  FormControlLabel,
  MenuItem,
  Stack,
  Switch,
  TextField,
  Typography,
} from '@mui/material'
import type { Node } from '@xyflow/react'

type Props = {
  node: Node | null
  onChange: (id: string, data: Record<string, unknown>) => void
}

function asRecord(v: unknown): Record<string, unknown> {
  return v && typeof v === 'object' ? (v as Record<string, unknown>) : {}
}

export function NodeInspector({ node, onChange }: Props) {
  if (!node) {
    return (
      <Typography color="text.secondary" sx={{ fontSize: 13 }}>
        Выберите узел на холсте
      </Typography>
    )
  }

  const data = asRecord(node.data)
  const patch = (next: Record<string, unknown>) => onChange(node.id, { ...data, ...next })

  return (
    <Stack spacing={1.25}>
      <Typography sx={{ fontWeight: 700, fontSize: 13 }}>{node.type}</Typography>
      <Typography color="text.secondary" sx={{ fontSize: 11 }}>
        id: {node.id}
      </Typography>

      {node.type === 'trigger.mqtt' ? (
        <>
          <TextField
            size="small"
            label="topic"
            value={String(data.topic ?? '')}
            onChange={(e) => patch({ topic: e.target.value })}
            fullWidth
          />
          <TextField
            size="small"
            select
            label="on"
            value={String(data.on ?? 'change')}
            onChange={(e) => patch({ on: e.target.value })}
            fullWidth
          >
            <MenuItem value="change">change</MenuItem>
            <MenuItem value="any">any</MenuItem>
          </TextField>
        </>
      ) : null}

      {node.type === 'trigger.cron' ? (
        <>
          <TextField
            size="small"
            label="expr"
            value={String(data.expr ?? '')}
            onChange={(e) => patch({ expr: e.target.value })}
            fullWidth
          />
          <TextField
            size="small"
            label="timezone"
            value={String(data.timezone ?? 'Europe/Moscow')}
            onChange={(e) => patch({ timezone: e.target.value })}
            fullWidth
          />
        </>
      ) : null}

      {node.type === 'condition.compare' ? (
        <>
          <ValueRefFields
            label="left"
            value={asRecord(data.left)}
            onChange={(left) => patch({ left })}
          />
          <TextField
            size="small"
            select
            label="op"
            value={String(data.op ?? 'eq')}
            onChange={(e) => patch({ op: e.target.value })}
            fullWidth
          >
            {['eq', 'ne', 'lt', 'lte', 'gt', 'gte'].map((op) => (
              <MenuItem key={op} value={op}>
                {op}
              </MenuItem>
            ))}
          </TextField>
          <ValueRefFields
            label="right"
            value={asRecord(data.right)}
            onChange={(right) => patch({ right })}
          />
        </>
      ) : null}

      {node.type === 'action.mqtt' ? (
        <>
          <TextField
            size="small"
            label="topic"
            value={String(data.topic ?? '')}
            onChange={(e) => patch({ topic: e.target.value })}
            fullWidth
          />
          <TextField
            size="small"
            label="payload"
            value={String(data.payload ?? '')}
            onChange={(e) => patch({ payload: e.target.value })}
            fullWidth
          />
          <FormControlLabel
            control={
              <Switch
                checked={Boolean(data.command)}
                onChange={(_, v) => patch({ command: v })}
              />
            }
            label="command (/on)"
          />
        </>
      ) : null}

      {node.type === 'action.delay' ? (
        <TextField
          size="small"
          type="number"
          label="ms"
          value={Number(data.ms ?? 0)}
          onChange={(e) => patch({ ms: Number(e.target.value) })}
          fullWidth
        />
      ) : null}

      {node.type === 'action.set_var' ? (
        <>
          <TextField
            size="small"
            label="name"
            value={String(data.name ?? '')}
            onChange={(e) => patch({ name: e.target.value })}
            fullWidth
          />
          <ValueRefFields
            label="value"
            value={asRecord(data.value)}
            onChange={(value) => patch({ value })}
          />
        </>
      ) : null}

      {node.type === 'logic.thermostat' ? (
        <>
          <ValueRefFields label="temp" value={asRecord(data.temp)} onChange={(temp) => patch({ temp })} />
          <ValueRefFields
            label="setpoint"
            value={asRecord(data.setpoint)}
            onChange={(setpoint) => patch({ setpoint })}
          />
          <ValueRefFields label="mode" value={asRecord(data.mode)} onChange={(mode) => patch({ mode })} />
          <TextField
            size="small"
            type="number"
            label="hysteresis"
            value={Number(data.hysteresis ?? 0.5)}
            onChange={(e) => patch({ hysteresis: Number(e.target.value) })}
            fullWidth
          />
          <TextField
            size="small"
            label="relay"
            value={String(data.relay ?? '')}
            onChange={(e) => patch({ relay: e.target.value })}
            fullWidth
          />
          <FormControlLabel
            control={
              <Switch
                checked={Boolean(data.command)}
                onChange={(_, v) => patch({ command: v })}
              />
            }
            label="command (/on)"
          />
        </>
      ) : null}

      {node.type === 'trigger.manual' ||
      node.type === 'condition.and' ||
      node.type === 'condition.or' ? (
        <Box />
      ) : null}
    </Stack>
  )
}

function ValueRefFields({
  label,
  value,
  onChange,
}: {
  label: string
  value: Record<string, unknown>
  onChange: (v: Record<string, unknown>) => void
}) {
  const mode = value.topic ? 'topic' : value.var ? 'var' : 'const'
  return (
    <Stack spacing={0.75} sx={{ border: 1, borderColor: 'divider', borderRadius: 1, p: 1 }}>
      <Typography sx={{ fontSize: 11, fontWeight: 600 }}>{label}</Typography>
      <TextField
        size="small"
        select
        label="источник"
        value={mode}
        onChange={(e) => {
          const m = e.target.value
          if (m === 'topic') onChange({ topic: String(value.topic ?? value.const ?? '') })
          else if (m === 'var') onChange({ var: String(value.var ?? '') })
          else onChange({ const: String(value.const ?? value.topic ?? '') })
        }}
        fullWidth
      >
        <MenuItem value="topic">topic</MenuItem>
        <MenuItem value="const">const</MenuItem>
        <MenuItem value="var">var</MenuItem>
      </TextField>
      {mode === 'topic' ? (
        <TextField
          size="small"
          label="topic"
          value={String(value.topic ?? '')}
          onChange={(e) => onChange({ topic: e.target.value })}
          fullWidth
        />
      ) : null}
      {mode === 'const' ? (
        <TextField
          size="small"
          label="const"
          value={String(value.const ?? '')}
          onChange={(e) => onChange({ const: e.target.value })}
          fullWidth
        />
      ) : null}
      {mode === 'var' ? (
        <TextField
          size="small"
          label="var"
          value={String(value.var ?? '')}
          onChange={(e) => onChange({ var: e.target.value })}
          fullWidth
        />
      ) : null}
    </Stack>
  )
}
