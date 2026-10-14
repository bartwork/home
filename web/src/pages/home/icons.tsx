import type { SvgIconComponent } from '@mui/icons-material'
import AirOutlinedIcon from '@mui/icons-material/AirOutlined'
import LightbulbOutlinedIcon from '@mui/icons-material/LightbulbOutlined'
import PowerSettingsNewOutlinedIcon from '@mui/icons-material/PowerSettingsNewOutlined'
import SensorsOutlinedIcon from '@mui/icons-material/SensorsOutlined'
import ThermostatOutlinedIcon from '@mui/icons-material/ThermostatOutlined'
import TuneOutlinedIcon from '@mui/icons-material/TuneOutlined'
import type { Device } from '../../shared/api/types'

const byIcon: Record<string, SvgIconComponent> = {
  light: LightbulbOutlinedIcon,
  dimmer: TuneOutlinedIcon,
  switch: PowerSettingsNewOutlinedIcon,
  fan: AirOutlinedIcon,
  sensor: SensorsOutlinedIcon,
  socket: PowerSettingsNewOutlinedIcon,
  thermo: ThermostatOutlinedIcon,
}

const byType: Record<string, SvgIconComponent> = {
  switch: PowerSettingsNewOutlinedIcon,
  range: LightbulbOutlinedIcon,
  sensor: SensorsOutlinedIcon,
  raw: SensorsOutlinedIcon,
}

export function iconFor(device: Device): SvgIconComponent {
  if (device.icon && byIcon[device.icon]) return byIcon[device.icon]
  return byType[device.deviceType] ?? SensorsOutlinedIcon
}

export const ICON_OPTIONS = [
  { value: '', label: 'по типу' },
  { value: 'light', label: 'свет' },
  { value: 'dimmer', label: 'диммер' },
  { value: 'switch', label: 'выключатель' },
  { value: 'fan', label: 'вытяжка' },
  { value: 'socket', label: 'розетка' },
  { value: 'sensor', label: 'сенсор' },
  { value: 'thermo', label: 'тепло' },
]
