/**
 * FleetFlow Design Tokens — Border Radius
 */
export const FfRadius = {
  none: '0',
  sm: '0.25rem',   // 4px
  md: '0.5rem',    // 8px
  lg: '0.75rem',   // 12px
  xl: '1rem',      // 16px
  '2xl': '1.25rem', // 20px
  full: '9999px'
} as const;

/**
 * FleetFlow Design Tokens — Elevation / Shadows
 */
export const FfElevation = {
  none: 'none',
  sm: '0 1px 2px 0 rgba(15, 23, 42, 0.05)',
  md: '0 4px 6px -1px rgba(15, 23, 42, 0.08), 0 2px 4px -2px rgba(15, 23, 42, 0.06)',
  lg: '0 10px 15px -3px rgba(15, 23, 42, 0.1), 0 4px 6px -4px rgba(15, 23, 42, 0.08)',
  xl: '0 20px 25px -5px rgba(15, 23, 42, 0.1), 0 8px 10px -6px rgba(15, 23, 42, 0.08)'
} as const;

/**
 * FleetFlow Design Tokens — Breakpoints
 */
export const FfBreakpoints = {
  sm: 640,
  md: 768,
  lg: 1024,
  xl: 1280,
  '2xl': 1536
} as const;

/**
 * FleetFlow Design Tokens — Animation
 */
export const FfAnimation = {
  duration: {
    fast: '150ms',
    normal: '250ms',
    slow: '400ms'
  },
  easing: {
    standard: 'cubic-bezier(0.4, 0, 0.2, 1)',
    decelerate: 'cubic-bezier(0, 0, 0.2, 1)',
    accelerate: 'cubic-bezier(0.4, 0, 1, 1)'
  }
} as const;
