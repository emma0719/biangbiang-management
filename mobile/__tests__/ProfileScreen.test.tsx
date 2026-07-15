import { fireEvent, render, screen, waitFor } from '@testing-library/react-native';
import { Alert } from 'react-native';
import React from 'react';
import Shift from '../app/shift';
import { api } from '../src/api/client';
import { ProfileScreen } from '../src/screens/ProfileScreen';
import { TestProviders } from '../testUtils';
import { router } from 'expo-router';

jest.mock('../src/api/client', () => ({ api: jest.fn() }));
jest.mock('../src/auth/tokenStore', () => ({ clearTokens: jest.fn(), getTokens: jest.fn().mockResolvedValue({ refreshToken: 'refresh' }) }));
jest.mock('expo-router', () => ({ router: { replace: jest.fn(), push: jest.fn() }, usePathname: () => '/profile' }));

describe('ProfileScreen', () => {
  beforeEach(() => {
    jest.spyOn(Alert, 'alert').mockImplementation(jest.fn());
  });

  it('hides manager navigation for regular employees', async () => {
    (api as jest.Mock).mockResolvedValueOnce(profile(['HOST']));

    render(<ProfileScreen />, { wrapper: TestProviders });

    expect(await screen.findByText('My profile')).toBeTruthy();
    expect(screen.queryByText('People Management')).toBeNull();
  });

  it('shows bottom navigation for regular employees', async () => {
    (api as jest.Mock).mockResolvedValueOnce(profile(['HOST']));

    render(<ProfileScreen />, { wrapper: TestProviders });

    await screen.findByText('My profile');
    expect(screen.getByText('Profile')).toBeTruthy();
    expect(screen.getByText('Shift')).toBeTruthy();
    expect(screen.getByText('Order')).toBeTruthy();
    expect(screen.getByText('Inventory')).toBeTruthy();
    fireEvent.press(screen.getByLabelText('Order'));
    expect(router.replace).toHaveBeenCalledWith('/order');
  });

  it('shows people management navigation for managers', async () => {
    (api as jest.Mock).mockResolvedValueOnce(profile(['MANAGER']));

    render(<ProfileScreen />, { wrapper: TestProviders });

    await screen.findByText('My profile');
    fireEvent.press(screen.getByLabelText('People Management'));
    expect(router.push).toHaveBeenCalledWith('/manager');
  });

  it('shows logout-all confirmation', async () => {
    (api as jest.Mock).mockResolvedValueOnce(profile(['HOST']));

    render(<ProfileScreen />, { wrapper: TestProviders });

    fireEvent.press(await screen.findByLabelText('Log out all devices'));
    await waitFor(() => expect(Alert.alert).toHaveBeenCalledWith('Confirm', 'Log out all devices', expect.any(Array)));
  });

  it('can switch language to Spanish', async () => {
    (api as jest.Mock).mockResolvedValueOnce(profile(['HOST']));

    render(<ProfileScreen />, { wrapper: TestProviders });

    fireEvent.press(await screen.findByText('English'));
    expect(await screen.findByText('Mi perfil')).toBeTruthy();
  });

  it('shows Simplified Chinese bottom navigation labels', async () => {
    (api as jest.Mock).mockResolvedValueOnce(profile(['HOST']));

    render(<ProfileScreen />, { wrapper: TestProviders });

    fireEvent.press(await screen.findByText('English'));
    fireEvent.press(await screen.findByText('Espanol'));
    expect(await screen.findByText('个人')).toBeTruthy();
    expect(screen.getByText('换班')).toBeTruthy();
    expect(screen.getByText('订货')).toBeTruthy();
    expect(screen.getByText('库存')).toBeTruthy();
  });

  it('shows the shift coverage employee tabs', async () => {
    (api as jest.Mock).mockImplementation((path: string) => {
      if (path === '/api/me') return Promise.resolve(profile(['HOST']));
      if (path === '/api/employees') return Promise.resolve([]);
      if (path === '/api/coverage-requests/pool') return Promise.resolve([]);
      if (path === '/api/coverage-requests/mine') return Promise.resolve([]);
      if (path === '/api/coverage-requests/involving-me') return Promise.resolve([]);
      return Promise.resolve([]);
    });

    render(<Shift />, { wrapper: TestProviders });

    expect(await screen.findByText('New Request')).toBeTruthy();
    expect(screen.getByText('Shift Pool')).toBeTruthy();
    expect(screen.getByText('My Requests')).toBeTruthy();
  });

  function profile(positions: string[]) {
    return {
      id: 1,
      englishName: 'Taylor',
      preferredName: 'Taylor',
      displayName: 'Taylor',
      email: 'taylor@example.com',
      phone: '+12065550100',
      homeStore: 'SEATTLE',
      eligibleStores: ['SEATTLE'],
      positions,
      status: 'ACTIVE',
      createdAt: '2026-06-15T00:00:00Z'
    };
  }
});
