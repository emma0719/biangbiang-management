import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { Stack } from 'expo-router';
import React from 'react';
import { SafeAreaProvider } from 'react-native-safe-area-context';
import { I18nProvider } from '../src/i18n/I18nProvider';

const queryClient = new QueryClient();

export default function Layout() {
  return (
    <QueryClientProvider client={queryClient}>
      <SafeAreaProvider>
        <I18nProvider>
          <Stack screenOptions={{ headerShown: false }} />
        </I18nProvider>
      </SafeAreaProvider>
    </QueryClientProvider>
  );
}
