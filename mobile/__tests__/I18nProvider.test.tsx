import { fireEvent, render, screen, waitFor } from '@testing-library/react-native';
import * as SecureStore from 'expo-secure-store';
import React from 'react';
import { Text } from 'react-native';
import { I18nProvider, useI18n } from '../src/i18n/I18nProvider';
import { TranslationKey } from '../src/i18n/translations';

function Probe({ translationKey = 'loginTitle' }: { translationKey?: TranslationKey }) {
  const { locale, setLocale, t } = useI18n();
  return (
    <>
      <Text>{locale}</Text>
      <Text>{t(translationKey)}</Text>
      <Text onPress={() => setLocale('zh')}>set zh</Text>
    </>
  );
}

describe('I18nProvider', () => {
  beforeEach(() => {
    (SecureStore.getItemAsync as jest.Mock).mockResolvedValue(null);
    (SecureStore.setItemAsync as jest.Mock).mockResolvedValue(undefined);
  });

  it('defaults first-time users to English', async () => {
    render(<I18nProvider><Probe /></I18nProvider>);

    expect(await screen.findByText('en')).toBeTruthy();
    expect(screen.getByText('Sign in')).toBeTruthy();
  });

  it('falls back to English when the active locale is missing a key', async () => {
    render(<I18nProvider><Probe /></I18nProvider>);

    fireEvent.press(screen.getByText('set zh'));

    await waitFor(() => expect(screen.getByText('zh')).toBeTruthy());
    expect(screen.getByText('Sign in')).toBeTruthy();
  });

  it('restores an explicitly saved language preference', async () => {
    (SecureStore.getItemAsync as jest.Mock).mockResolvedValue('es');

    render(<I18nProvider><Probe /></I18nProvider>);

    await waitFor(() => expect(screen.getByText('es')).toBeTruthy());
    expect(screen.getByText('Iniciar sesion')).toBeTruthy();
  });
});
