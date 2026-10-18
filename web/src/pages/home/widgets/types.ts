import type { Device } from '../../../shared/api/types'

export type WidgetProps = {
  device: Device
  value: string
  /** Состояние топика вкл/выкл (powerTopic), если есть */
  powerValue?: string
  sendCmd: (topic: string, payload: string) => void
}
