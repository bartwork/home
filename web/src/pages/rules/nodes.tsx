import type { ReactNode } from 'react'
import { Handle, Position, type NodeProps } from '@xyflow/react'
import { Box, Typography } from '@mui/material'

const shell = {
  minWidth: 140,
  maxWidth: 220,
  px: 1.25,
  py: 1,
  borderRadius: 1.5,
  border: '1px solid',
  borderColor: 'divider',
  bgcolor: 'background.paper',
  boxShadow: '0 4px 18px rgba(0,0,0,0.25)',
  fontSize: 12,
} as const

function NodeShell({
  title,
  accent,
  children,
  handles,
}: {
  title: string
  accent: string
  children?: ReactNode
  handles: 'trigger' | 'condition' | 'action'
}) {
  return (
    <Box sx={{ ...shell, borderTop: `3px solid ${accent}` }}>
      {handles !== 'trigger' ? <Handle type="target" position={Position.Left} /> : null}
      <Typography sx={{ fontWeight: 700, fontSize: 11, letterSpacing: 0.4, mb: 0.35, color: accent }}>
        {title}
      </Typography>
      {children}
      {handles === 'condition' ? (
        <>
          <Handle type="source" id="true" position={Position.Right} style={{ top: '35%' }} />
          <Handle type="source" id="false" position={Position.Right} style={{ top: '70%' }} />
        </>
      ) : (
        <Handle type="source" position={Position.Right} />
      )}
    </Box>
  )
}

function line(text: string) {
  return (
    <Typography sx={{ fontSize: 11, color: 'text.secondary', wordBreak: 'break-all', lineHeight: 1.3 }}>
      {text}
    </Typography>
  )
}

export function TriggerMQTTNode({ data }: NodeProps) {
  const d = data as { topic?: string; on?: string }
  return (
    <NodeShell title="trigger.mqtt" accent="#5ec8d8" handles="trigger">
      {line(d.topic || 'topic…')}
      {line(`on: ${d.on || 'change'}`)}
    </NodeShell>
  )
}

export function TriggerCronNode({ data }: NodeProps) {
  const d = data as { expr?: string }
  return (
    <NodeShell title="trigger.cron" accent="#5ec8d8" handles="trigger">
      {line(d.expr || 'cron…')}
    </NodeShell>
  )
}

export function TriggerManualNode() {
  return (
    <NodeShell title="trigger.manual" accent="#5ec8d8" handles="trigger">
      {line('POST …/run')}
    </NodeShell>
  )
}

export function ConditionCompareNode({ data }: NodeProps) {
  const d = data as { left?: { topic?: string; const?: string }; op?: string; right?: { const?: string; topic?: string } }
  const left = d.left?.topic || d.left?.const || '?'
  const right = d.right?.topic || d.right?.const || '?'
  return (
    <NodeShell title="compare" accent="#f0b35a" handles="condition">
      {line(`${left} ${d.op || 'eq'} ${right}`)}
      <Typography sx={{ fontSize: 10, mt: 0.5, color: 'text.disabled' }}>true / false</Typography>
    </NodeShell>
  )
}

export function ConditionAndNode() {
  return <NodeShell title="AND" accent="#f0b35a" handles="condition" />
}

export function ConditionOrNode() {
  return <NodeShell title="OR" accent="#f0b35a" handles="condition" />
}

export function ActionMQTTNode({ data }: NodeProps) {
  const d = data as { topic?: string; payload?: string; command?: boolean }
  return (
    <NodeShell title="action.mqtt" accent="#7dcea0" handles="action">
      {line(d.topic || 'topic…')}
      {line(`→ ${d.payload ?? ''} ${d.command ? '(cmd)' : ''}`)}
    </NodeShell>
  )
}

export function ActionDelayNode({ data }: NodeProps) {
  const d = data as { ms?: number }
  return (
    <NodeShell title="delay" accent="#7dcea0" handles="action">
      {line(`${d.ms ?? 0} ms`)}
    </NodeShell>
  )
}

export function ActionSetVarNode({ data }: NodeProps) {
  const d = data as { name?: string }
  return (
    <NodeShell title="set_var" accent="#7dcea0" handles="action">
      {line(d.name || 'name')}
    </NodeShell>
  )
}

export function LogicThermostatNode({ data }: NodeProps) {
  const d = data as { relay?: string; hysteresis?: number }
  return (
    <NodeShell title="thermostat" accent="#e07a5f" handles="action">
      {line(d.relay || 'relay…')}
      {line(`hyst ${d.hysteresis ?? 0.5}`)}
    </NodeShell>
  )
}

export const ruleNodeTypes = {
  'trigger.mqtt': TriggerMQTTNode,
  'trigger.cron': TriggerCronNode,
  'trigger.manual': TriggerManualNode,
  'condition.compare': ConditionCompareNode,
  'condition.and': ConditionAndNode,
  'condition.or': ConditionOrNode,
  'action.mqtt': ActionMQTTNode,
  'action.delay': ActionDelayNode,
  'action.set_var': ActionSetVarNode,
  'logic.thermostat': LogicThermostatNode,
}
