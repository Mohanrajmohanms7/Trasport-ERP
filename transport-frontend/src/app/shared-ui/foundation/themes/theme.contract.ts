/**
 * Theme contract — maps semantic roles to CSS custom properties.
 * Applied via data-ff-theme="light|dark" on <html>.
 */
export type FfThemeMode = 'light' | 'dark';

export interface FfThemeContract {
  mode: FfThemeMode;
  /** Surface colors */
  surfacePage: string;
  surfaceCard: string;
  surfaceSidebar: string;
  surfaceInput: string;
  surfaceHover: string;
  /** Text */
  textPrimary: string;
  textSecondary: string;
  textMuted: string;
  textInverse: string;
  /** Borders */
  borderDefault: string;
  borderStrong: string;
  borderFocus: string;
}

export const FfLightTheme: FfThemeContract = {
  mode: 'light',
  surfacePage: '#f8fafc',
  surfaceCard: '#ffffff',
  surfaceSidebar: '#ffffff',
  surfaceInput: '#ffffff',
  surfaceHover: '#f1f5f9',
  textPrimary: '#0f172a',
  textSecondary: '#475569',
  textMuted: '#64748b',
  textInverse: '#ffffff',
  borderDefault: '#e2e8f0',
  borderStrong: '#cbd5e1',
  borderFocus: '#2563eb'
};

export const FfDarkTheme: FfThemeContract = {
  mode: 'dark',
  surfacePage: '#0f172a',
  surfaceCard: '#1e293b',
  surfaceSidebar: '#1e293b',
  surfaceInput: '#0f172a',
  surfaceHover: '#334155',
  textPrimary: '#f8fafc',
  textSecondary: '#cbd5e1',
  textMuted: '#94a3b8',
  textInverse: '#0f172a',
  borderDefault: '#334155',
  borderStrong: '#475569',
  borderFocus: '#3b82f6'
};
