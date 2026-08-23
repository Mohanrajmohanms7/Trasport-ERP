/**
 * FleetFlow Design Tokens — Colors
 * CSS variables: --ff-color-{name}-{shade}
 */
export const FfColors = {
  primary: {
    50: '#eff6ff',
    100: '#dbeafe',
    500: '#3b82f6',
    600: '#2563eb',
    700: '#1d4ed8',
    900: '#1e3a8a'
  },
  success: {
    50: '#ecfdf5',
    500: '#10b981',
    700: '#047857'
  },
  danger: {
    50: '#fef2f2',
    500: '#ef4444',
    700: '#b91c1c'
  },
  warning: {
    50: '#fffbeb',
    500: '#f59e0b',
    700: '#b45309'
  },
  info: {
    50: '#ecfeff',
    500: '#06b6d4',
    700: '#0e7490'
  },
  neutral: {
    50: '#f8fafc',
    100: '#f1f5f9',
    200: '#e2e8f0',
    300: '#cbd5e1',
    400: '#94a3b8',
    500: '#64748b',
    600: '#475569',
    700: '#334155',
    800: '#1e293b',
    900: '#0f172a',
    950: '#020617'
  }
} as const;

export type FfColorPalette = typeof FfColors;
