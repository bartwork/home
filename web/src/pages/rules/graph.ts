import type { Edge, Node } from '@xyflow/react'
import type { RuleEdge, RuleGraph, RuleNode } from '../../shared/api/types'

export const NODE_CATALOG: { type: string; label: string; defaults: Record<string, unknown> }[] = [
  { type: 'trigger.mqtt', label: 'MQTT', defaults: { topic: '', on: 'change' } },
  { type: 'trigger.cron', label: 'Cron', defaults: { expr: '0 * * * *', timezone: 'Europe/Moscow' } },
  { type: 'trigger.manual', label: 'Manual', defaults: {} },
  {
    type: 'condition.compare',
    label: 'Compare',
    defaults: { left: { const: '' }, op: 'eq', right: { const: '' } },
  },
  { type: 'condition.and', label: 'AND', defaults: {} },
  { type: 'condition.or', label: 'OR', defaults: {} },
  {
    type: 'action.mqtt',
    label: 'MQTT out',
    defaults: { topic: '', payload: '1', command: true },
  },
  { type: 'action.delay', label: 'Delay', defaults: { ms: 1000 } },
  { type: 'action.set_var', label: 'Set var', defaults: { name: 'x', value: { const: '' } } },
  {
    type: 'logic.thermostat',
    label: 'Thermostat',
    defaults: {
      temp: { topic: '' },
      setpoint: { topic: '' },
      mode: { topic: '' },
      hysteresis: 0.5,
      relay: '',
      command: true,
    },
  },
]

export function emptyGraph(): RuleGraph {
  return {
    nodes: [
      {
        id: 't_manual',
        type: 'trigger.manual',
        position: { x: 80, y: 120 },
        data: {},
      },
    ],
    edges: [],
  }
}

export function toFlow(graph: RuleGraph): { nodes: Node[]; edges: Edge[] } {
  const nodes: Node[] = (graph.nodes ?? []).map((n) => ({
    id: n.id,
    type: n.type,
    position: n.position ?? { x: 0, y: 0 },
    data: { ...(n.data ?? {}) },
  }))
  const edges: Edge[] = (graph.edges ?? []).map((e) => ({
    id: e.id,
    source: e.source,
    target: e.target,
    sourceHandle: e.sourceHandle || undefined,
    targetHandle: e.targetHandle || undefined,
  }))
  return { nodes, edges }
}

export function fromFlow(nodes: Node[], edges: Edge[]): RuleGraph {
  const outNodes: RuleNode[] = nodes.map((n) => ({
    id: n.id,
    type: n.type ?? 'trigger.manual',
    position: n.position,
    data: (n.data ?? {}) as Record<string, unknown>,
  }))
  const outEdges: RuleEdge[] = edges.map((e) => ({
    id: e.id,
    source: e.source,
    target: e.target,
    ...(e.sourceHandle ? { sourceHandle: e.sourceHandle } : {}),
    ...(e.targetHandle ? { targetHandle: e.targetHandle } : {}),
  }))
  return { nodes: outNodes, edges: outEdges }
}

export function newNodeId(type: string): string {
  return `${type.replace(/\./g, '_')}_${Math.random().toString(36).slice(2, 8)}`
}
