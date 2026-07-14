import * as SecureStore from 'expo-secure-store';
import React, { createContext, useContext, useEffect, useMemo, useState } from 'react';
import { Platform } from 'react-native';
import { Locale, TranslationKey, translations } from './translations';

type I18nValue = {
  locale: Locale;
  setLocale: (locale: Locale) => void;
  t: (key: TranslationKey) => string;
};

const I18nContext = createContext<I18nValue | undefined>(undefined);
const languageKey = 'restaurantOps.language';

function isLocale(value: string | null): value is Locale {
  return value === 'en' || value === 'es' || value === 'zh';
}

function webStorage() {
  if (Platform.OS !== 'web' || typeof window === 'undefined') return null;
  return window.localStorage ?? null;
}

export async function getSavedLocale(): Promise<Locale | null> {
  const storage = webStorage();
  if (storage) {
    const value = storage.getItem(languageKey);
    return isLocale(value) ? value : null;
  }
  if (Platform.OS === 'web') return null;
  const value = await SecureStore.getItemAsync(languageKey);
  return isLocale(value) ? value : null;
}

export async function saveLocale(locale: Locale) {
  const storage = webStorage();
  if (storage) {
    storage.setItem(languageKey, locale);
    return;
  }
  if (Platform.OS === 'web') return;
  await SecureStore.setItemAsync(languageKey, locale);
}

export function I18nProvider({ children }: { children: React.ReactNode }) {
  const [locale, setLocaleState] = useState<Locale>('en');
  useEffect(() => {
    let mounted = true;
    getSavedLocale().then((saved) => {
      if (mounted && saved) setLocaleState(saved);
    });
    return () => { mounted = false; };
  }, []);
  const setLocale = (nextLocale: Locale) => {
    setLocaleState(nextLocale);
    void saveLocale(nextLocale);
  };
  const value = useMemo(() => ({ locale, setLocale, t: (key: TranslationKey) => (translations[locale] as Partial<Record<TranslationKey, string>>)[key] ?? translations.en[key] }), [locale]);
  return <I18nContext.Provider value={value}>{children}</I18nContext.Provider>;
}

export function useI18n() {
  const value = useContext(I18nContext);
  if (!value) throw new Error('I18nProvider missing');
  return value;
}
