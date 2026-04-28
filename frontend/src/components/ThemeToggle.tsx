import { SunIcon, MoonIcon, MonitorIcon } from 'lucide-react'
import { cn } from '@/lib/utils'
import { useTheme, type Theme } from '@/contexts/ThemeContext'

const OPTIONS: { value: Theme; Icon: typeof SunIcon; label: string }[] = [
  { value: 'light', Icon: SunIcon, label: 'Light' },
  { value: 'dark', Icon: MoonIcon, label: 'Dark' },
  { value: 'auto', Icon: MonitorIcon, label: 'System' },
]

export function ThemeToggle() {
  const { theme, setTheme } = useTheme()

  return (
    <div className="flex items-center rounded-md border p-0.5 gap-0.5">
      {OPTIONS.map(({ value, Icon, label }) => (
        <button
          key={value}
          onClick={() => setTheme(value)}
          title={label}
          aria-label={label}
          aria-pressed={theme === value}
          className={cn(
            'flex items-center justify-center size-6 rounded transition-colors',
            theme === value
              ? 'bg-primary text-primary-foreground'
              : 'text-muted-foreground hover:text-foreground'
          )}
        >
          <Icon className="size-3.5" />
        </button>
      ))}
    </div>
  )
}
