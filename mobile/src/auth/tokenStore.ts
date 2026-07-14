import * as SecureStore from 'expo-secure-store';
import { Platform } from 'react-native';

const accessKey = 'restaurantOps.accessToken';
const refreshKey = 'restaurantOps.refreshToken';
let webMemoryTokens = { accessToken: null as string | null, refreshToken: null as string | null };

function webStorage() {
  if (Platform.OS !== 'web' || typeof window === 'undefined') return null;
  return window.localStorage ?? null;
}

export async function saveTokens(accessToken: string, refreshToken: string) {
  const storage = webStorage();
  if (storage) {
    storage.setItem(accessKey, accessToken);
    storage.setItem(refreshKey, refreshToken);
    return;
  }
  if (Platform.OS === 'web') {
    webMemoryTokens = { accessToken, refreshToken };
    return;
  }
  await SecureStore.setItemAsync(accessKey, accessToken);
  await SecureStore.setItemAsync(refreshKey, refreshToken);
}

export async function getTokens() {
  const storage = webStorage();
  if (storage) {
    return {
      accessToken: storage.getItem(accessKey),
      refreshToken: storage.getItem(refreshKey)
    };
  }
  if (Platform.OS === 'web') return webMemoryTokens;
  const [accessToken, refreshToken] = await Promise.all([
    SecureStore.getItemAsync(accessKey),
    SecureStore.getItemAsync(refreshKey)
  ]);
  return { accessToken, refreshToken };
}

export async function clearTokens() {
  const storage = webStorage();
  if (storage) {
    storage.removeItem(accessKey);
    storage.removeItem(refreshKey);
    return;
  }
  if (Platform.OS === 'web') {
    webMemoryTokens = { accessToken: null, refreshToken: null };
    return;
  }
  await Promise.all([SecureStore.deleteItemAsync(accessKey), SecureStore.deleteItemAsync(refreshKey)]);
}
