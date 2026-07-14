import * as SecureStore from 'expo-secure-store';
import { clearTokens, getTokens, saveTokens } from '../src/auth/tokenStore';

describe('tokenStore', () => {
  it('stores tokens in Expo SecureStore', async () => {
    (SecureStore.getItemAsync as jest.Mock).mockResolvedValueOnce('access').mockResolvedValueOnce('refresh');

    await saveTokens('access', 'refresh');
    await expect(getTokens()).resolves.toEqual({ accessToken: 'access', refreshToken: 'refresh' });
    await clearTokens();

    expect(SecureStore.setItemAsync).toHaveBeenCalledWith('restaurantOps.accessToken', 'access');
    expect(SecureStore.setItemAsync).toHaveBeenCalledWith('restaurantOps.refreshToken', 'refresh');
    expect(SecureStore.deleteItemAsync).toHaveBeenCalledWith('restaurantOps.accessToken');
    expect(SecureStore.deleteItemAsync).toHaveBeenCalledWith('restaurantOps.refreshToken');
  });
});
