import { Box, keyframes } from '@mui/material'

const driftSlow = keyframes`
  0%, 100% { transform: translate3d(0, 0, 0) scale(1); }
  40% { transform: translate3d(4%, -3%, 0) scale(1.08); }
  70% { transform: translate3d(-2%, 4%, 0) scale(0.96); }
`

const driftAlt = keyframes`
  0%, 100% { transform: translate3d(0, 0, 0) scale(1); }
  50% { transform: translate3d(-5%, 3%, 0) scale(1.1); }
`

const breathe = keyframes`
  0%, 100% { opacity: 0.35; }
  50% { opacity: 0.85; }
`

const shimmer = keyframes`
  0%, 100% { opacity: 0.4; transform: translateX(-2%); }
  50% { opacity: 0.9; transform: translateX(2%); }
`

const floatUp = keyframes`
  0% { transform: translateY(0); opacity: 0.15; }
  50% { opacity: 0.7; }
  100% { transform: translateY(-28vh); opacity: 0; }
`

function rgba(r: number, g: number, b: number, a: number) {
  return `rgba(${r}, ${g}, ${b}, ${a})`
}

const windows = [
  { left: '8%', top: '18%', w: 28, h: 38, delay: '0s', rgb: [110, 210, 225] as const, a: 0.55 },
  { left: '14%', top: '28%', w: 22, h: 30, delay: '1.2s', rgb: [240, 190, 110] as const, a: 0.45 },
  { left: '9%', top: '48%', w: 24, h: 34, delay: '2.4s', rgb: [130, 180, 230] as const, a: 0.4 },
  { left: '78%', top: '16%', w: 30, h: 40, delay: '0.6s', rgb: [240, 185, 100] as const, a: 0.5 },
  { left: '84%', top: '32%', w: 22, h: 28, delay: '1.8s', rgb: [100, 205, 220] as const, a: 0.45 },
  { left: '80%', top: '52%', w: 26, h: 32, delay: '3s', rgb: [160, 200, 240] as const, a: 0.35 },
  { left: '22%', top: '62%', w: 18, h: 24, delay: '0.9s', rgb: [94, 200, 216] as const, a: 0.35 },
  { left: '68%', top: '66%', w: 20, h: 26, delay: '2.1s', rgb: [240, 179, 90] as const, a: 0.3 },
] as const

const sparks = [
  { left: '18%', bottom: '12%', delay: '0s', dur: '9s', cool: true },
  { left: '35%', bottom: '8%', delay: '2s', dur: '11s', cool: false },
  { left: '52%', bottom: '14%', delay: '4s', dur: '10s', cool: true },
  { left: '70%', bottom: '10%', delay: '1s', dur: '12s', cool: false },
  { left: '85%', bottom: '16%', delay: '3.5s', dur: '9.5s', cool: true },
] as const

export function LoginBackdrop() {
  return (
    <Box
      aria-hidden
      sx={{
        position: 'absolute',
        inset: 0,
        zIndex: 0,
        overflow: 'hidden',
        pointerEvents: 'none',
        background: [
          'radial-gradient(120% 80% at 50% 120%, #152033 0%, transparent 55%)',
          'linear-gradient(165deg, #05080f 0%, #0a1220 38%, #101a2a 72%, #0c1520 100%)',
        ].join(', '),
      }}
    >
      <Box
        sx={{
          position: 'absolute',
          inset: 0,
          background: [
            'radial-gradient(ellipse 90% 60% at 50% -10%, rgba(70, 120, 180, 0.28), transparent 55%)',
            'radial-gradient(ellipse 50% 40% at 0% 40%, rgba(40, 90, 120, 0.2), transparent 50%)',
            'radial-gradient(ellipse 45% 35% at 100% 35%, rgba(90, 70, 40, 0.18), transparent 50%)',
          ].join(', '),
        }}
      />

      <Box
        sx={{
          position: 'absolute',
          width: '70vmax',
          height: '70vmax',
          left: '-18%',
          top: '-20%',
          borderRadius: '50%',
          background:
            'radial-gradient(circle, rgba(94, 200, 216, 0.32) 0%, rgba(60, 140, 180, 0.12) 35%, transparent 68%)',
          filter: 'blur(40px)',
          animation: `${driftSlow} 18s ease-in-out infinite`,
        }}
      />
      <Box
        sx={{
          position: 'absolute',
          width: '55vmax',
          height: '55vmax',
          right: '-12%',
          top: '-8%',
          borderRadius: '50%',
          background:
            'radial-gradient(circle, rgba(240, 179, 90, 0.26) 0%, rgba(180, 120, 50, 0.1) 40%, transparent 70%)',
          filter: 'blur(48px)',
          animation: `${driftAlt} 22s ease-in-out infinite`,
        }}
      />
      <Box
        sx={{
          position: 'absolute',
          width: '50vmax',
          height: '40vmax',
          left: '25%',
          bottom: '-18%',
          borderRadius: '50%',
          background: 'radial-gradient(circle, rgba(90, 140, 200, 0.22) 0%, transparent 70%)',
          filter: 'blur(36px)',
          animation: `${driftSlow} 20s ease-in-out infinite reverse`,
        }}
      />

      <Box
        sx={{
          position: 'absolute',
          left: 0,
          right: 0,
          bottom: 0,
          height: { xs: '42%', md: '48%' },
          background: [
            'linear-gradient(180deg, transparent 0%, rgba(6, 10, 18, 0.55) 28%, rgba(5, 8, 14, 0.92) 100%)',
            'linear-gradient(90deg, rgba(8, 14, 24, 0.9) 0%, transparent 18%, transparent 82%, rgba(8, 14, 24, 0.9) 100%)',
          ].join(', '),
        }}
      />

      <Box
        sx={{
          position: 'absolute',
          left: { xs: '2%', md: '6%' },
          bottom: 0,
          width: { xs: '28%', md: '22%' },
          height: { xs: '58%', md: '68%' },
          background:
            'linear-gradient(180deg, rgba(18, 28, 42, 0.55) 0%, rgba(10, 16, 26, 0.85) 100%)',
          clipPath: 'polygon(8% 0, 100% 6%, 92% 100%, 0 100%)',
          borderRight: '1px solid rgba(140, 180, 210, 0.08)',
        }}
      />
      <Box
        sx={{
          position: 'absolute',
          right: { xs: '2%', md: '6%' },
          bottom: 0,
          width: { xs: '30%', md: '24%' },
          height: { xs: '62%', md: '72%' },
          background:
            'linear-gradient(180deg, rgba(22, 30, 44, 0.5) 0%, rgba(10, 16, 26, 0.88) 100%)',
          clipPath: 'polygon(0 8%, 94% 0, 100% 100%, 10% 100%)',
          borderLeft: '1px solid rgba(140, 180, 210, 0.08)',
        }}
      />

      {windows.map((w, i) => {
        const [r, g, b] = w.rgb
        const color = rgba(r, g, b, w.a)
        const soft = rgba(r, g, b, 0.15)
        const glow = rgba(r, g, b, 0.25)
        return (
          <Box
            key={i}
            sx={{
              position: 'absolute',
              left: w.left,
              top: w.top,
              width: w.w,
              height: w.h,
              borderRadius: '3px',
              background: `linear-gradient(180deg, ${color}, ${soft})`,
              boxShadow: `0 0 18px ${color}, 0 0 40px ${glow}`,
              animation: `${breathe} ${5 + (i % 3)}s ease-in-out ${w.delay} infinite`,
              opacity: 0.55,
            }}
          />
        )
      })}

      <Box
        sx={{
          position: 'absolute',
          left: '10%',
          right: '10%',
          bottom: { xs: '28%', md: '32%' },
          height: 2,
          borderRadius: 2,
          background:
            'linear-gradient(90deg, transparent, rgba(94, 200, 216, 0.45), rgba(240, 179, 90, 0.35), rgba(94, 200, 216, 0.4), transparent)',
          filter: 'blur(1px)',
          animation: `${shimmer} 10s ease-in-out infinite`,
        }}
      />
      <Box
        sx={{
          position: 'absolute',
          left: '15%',
          right: '15%',
          bottom: { xs: '26%', md: '30%' },
          height: 48,
          background: 'linear-gradient(180deg, rgba(94, 200, 216, 0.12), transparent)',
          filter: 'blur(12px)',
        }}
      />

      <Box
        sx={{
          position: 'absolute',
          left: '12%',
          right: '12%',
          bottom: 0,
          height: '26%',
          background: [
            'linear-gradient(180deg, rgba(94, 200, 216, 0.06), transparent 70%)',
            'repeating-linear-gradient(90deg, transparent, transparent 48px, rgba(255,255,255,0.015) 48px, rgba(255,255,255,0.015) 49px)',
          ].join(', '),
          maskImage: 'linear-gradient(180deg, black, transparent)',
          opacity: 0.7,
        }}
      />

      {sparks.map((s, i) => (
        <Box
          key={i}
          sx={{
            position: 'absolute',
            left: s.left,
            bottom: s.bottom,
            width: 4,
            height: 4,
            borderRadius: '50%',
            background: s.cool ? '#8adceb' : '#f0c274',
            boxShadow: s.cool ? '0 0 12px #5ec8d8' : '0 0 12px #f0b35a',
            animation: `${floatUp} ${s.dur} linear ${s.delay} infinite`,
          }}
        />
      ))}

      <Box
        sx={{
          position: 'absolute',
          inset: 0,
          background:
            'radial-gradient(ellipse 70% 65% at 50% 45%, transparent 35%, rgba(3, 6, 12, 0.72) 100%)',
        }}
      />
    </Box>
  )
}
