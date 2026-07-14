import { fireEvent, render, screen, waitFor } from '@testing-library/react-native';
import React from 'react';
import { api } from '../src/api/client';
import { ChangeToastPinScreen } from '../src/screens/ChangeToastPinScreen';
import { ContactChangeScreen } from '../src/screens/ContactChangeScreen';
import { EditProfileScreen } from '../src/screens/EditProfileScreen';
import { StorePreferencesScreen } from '../src/screens/StorePreferencesScreen';
import { TestProviders } from '../testUtils';

jest.mock('../src/api/client', () => ({ api: jest.fn() }));

describe('profile edit screens', () => {
  beforeEach(() => jest.resetAllMocks());

  it('edits English and preferred name', async () => {
    (api as jest.Mock).mockResolvedValueOnce(profile()).mockResolvedValueOnce({}).mockResolvedValueOnce(profile());
    render(<EditProfileScreen />, { wrapper: TestProviders });
    await screen.findByText('Edit profile');
    fireEvent.changeText(screen.getByLabelText('English name'), 'Jordan');
    fireEvent.changeText(screen.getByLabelText('Preferred name'), 'Jo');
    fireEvent.press(screen.getByLabelText('Save'));
    await waitFor(() => expect(api).toHaveBeenCalledWith('/api/me/profile', expect.objectContaining({ method: 'PATCH', body: JSON.stringify({ englishName: 'Jordan', preferredName: 'Jo' }) })));
    expect(await screen.findByText('Saved')).toBeTruthy();
  });

  it('edits eligible stores without sending homeStore', async () => {
    (api as jest.Mock).mockResolvedValueOnce(profile()).mockResolvedValueOnce({}).mockResolvedValueOnce(profile());
    render(<StorePreferencesScreen />, { wrapper: TestProviders });
    await screen.findByText('Home store');
    expect(screen.getAllByText('Seattle').length).toBeGreaterThan(0);
    expect(screen.getByText('Home store is managed by a manager')).toBeTruthy();
    fireEvent.press(screen.getByLabelText('Eligible stores'));
    fireEvent.press(screen.getByLabelText('Save'));
    await waitFor(() => expect(api).toHaveBeenCalledWith('/api/me/stores', expect.objectContaining({ method: 'PATCH', body: JSON.stringify({ eligibleStores: ['SEATTLE', 'REDMOND'] }) })));
  });

  it('updates Toast PIN and shows API error state', async () => {
    (api as jest.Mock).mockRejectedValueOnce(new Error('fail')).mockResolvedValueOnce({});
    render(<ChangeToastPinScreen />, { wrapper: TestProviders });
    fireEvent.changeText(screen.getByLabelText('Toast PIN'), '9999');
    fireEvent.press(screen.getByLabelText('Save'));
    expect(await screen.findByText('Unable to save changes')).toBeTruthy();
    fireEvent.press(screen.getByLabelText('Save'));
    expect(await screen.findByText('Toast PIN changed')).toBeTruthy();
  });

  it('requests and verifies email changes', async () => {
    (api as jest.Mock).mockResolvedValue({});
    render(<ContactChangeScreen kind="email" />, { wrapper: TestProviders });
    fireEvent.changeText(screen.getByLabelText('Email'), 'new@example.com');
    fireEvent.press(screen.getByLabelText('Request change'));
    expect(await screen.findByText('Email verification requested')).toBeTruthy();
    fireEvent.changeText(screen.getByLabelText('Verification token'), 'token');
    fireEvent.press(screen.getByLabelText('Verify'));
    expect(await screen.findByText('Email changed')).toBeTruthy();
  });

  it('requests and verifies phone changes', async () => {
    (api as jest.Mock).mockResolvedValue({});
    render(<ContactChangeScreen kind="phone" />, { wrapper: TestProviders });
    fireEvent.changeText(screen.getByLabelText('Phone'), '2065550199');
    fireEvent.press(screen.getByLabelText('Request change'));
    expect(await screen.findByText('Phone verification requested')).toBeTruthy();
    fireEvent.changeText(screen.getByLabelText('Verification token'), 'token');
    fireEvent.press(screen.getByLabelText('Verify'));
    expect(await screen.findByText('Phone changed')).toBeTruthy();
  });

  function profile() {
    return {
      id: 1,
      englishName: 'Taylor',
      preferredName: 'Taylor',
      displayName: 'Taylor',
      email: 'taylor@example.com',
      phone: '+12065550100',
      homeStore: 'SEATTLE',
      eligibleStores: ['SEATTLE'],
      positions: ['HOST'],
      status: 'ACTIVE',
      createdAt: '2026-06-15T00:00:00Z'
    };
  }
});
