import { router } from 'expo-router';
import React from 'react';
import { LoginScreen } from '../src/screens/LoginScreen';

export default function Index() {
  return <LoginScreen onSuccess={() => router.replace('/profile')} onDeveloperPreview={() => router.push('/developer-preview')} />;
}
