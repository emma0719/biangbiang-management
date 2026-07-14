import { router, useLocalSearchParams } from 'expo-router';
import React from 'react';
import { ActivationScreen } from '../src/screens/ActivationScreen';

export default function Activate() {
  const { token } = useLocalSearchParams<{ token?: string }>();
  return <ActivationScreen token={token ?? ''} onSuccess={() => router.replace('/activation-success')} />;
}
