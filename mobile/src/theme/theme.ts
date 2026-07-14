import { Platform } from 'react-native';

export const colors = {
  burgundy: '#8C2424',
  burgundyDark: '#6F1919',
  forestGreen: '#18583F',
  forestGreenDark: '#103E2D',
  paper: '#E8D7B5',
  paperLight: '#F5EEDF',
  ink: '#2B2118',
  mutedInk: '#6C5A49',
  orangeAccent: '#D36A24',
  border: '#BDA985',
  white: '#FFFDF8',
  error: '#A62929',
  success: '#2F6B4F',
  disabled: '#A99B86'
} as const;

export const typography = {
  display: Platform.select({ ios: 'Georgia', android: 'serif', web: 'Georgia', default: 'serif' }),
  interface: Platform.select({ ios: 'System', android: 'sans-serif', web: 'Arial', default: 'System' })
} as const;

export const spacing = {
  xs: 4,
  sm: 8,
  md: 12,
  lg: 16,
  xl: 20,
  xxl: 28
} as const;

export const radii = {
  sm: 4,
  md: 6,
  lg: 8
} as const;

export const borders = {
  hairline: 1,
  strong: 2
} as const;

export const shadows = {
  card: Platform.select({
    web: { boxShadow: '0 8px 18px rgba(43, 33, 24, 0.10)' },
    default: {
      shadowColor: colors.ink,
      shadowOpacity: 0.10,
      shadowRadius: 10,
      shadowOffset: { width: 0, height: 4 },
      elevation: 2
    }
  })
} as const;

export const layout = {
  maxWidth: 640,
  managerMaxWidth: 860,
  buttonHeight: 48,
  inputHeight: 48
} as const;

export const theme = { colors, typography, spacing, radii, borders, shadows, layout } as const;
