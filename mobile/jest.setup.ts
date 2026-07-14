import '@testing-library/react-native/extend-expect';

jest.mock('react-native/Libraries/Components/Switch/Switch', () => {
  const React = require('react');

  const MockSwitch = React.forwardRef(function MockSwitch(props: {
    value?: boolean;
    onValueChange?: (value: boolean) => void;
    disabled?: boolean;
    accessibilityRole?: string;
    accessibilityState?: Record<string, unknown>;
  }, ref: unknown) {
    const { value, onValueChange, disabled, accessibilityRole, accessibilityState, ...rest } = props;
    return React.createElement('RCTSwitch', {
      ...rest,
      ref,
      accessibilityRole: accessibilityRole ?? 'switch',
      accessibilityState: { ...accessibilityState, disabled, checked: value },
      disabled,
      value,
      onValueChange,
      onPress: () => {
        if (!disabled) onValueChange?.(!value);
      }
    });
  });

  return {
    __esModule: true,
    default: MockSwitch
  };
});

jest.mock('expo-secure-store', () => ({
  getItemAsync: jest.fn(),
  setItemAsync: jest.fn(),
  deleteItemAsync: jest.fn()
}));

jest.mock('expo-localization', () => ({
  getLocales: () => [{ languageCode: 'en' }]
}));
