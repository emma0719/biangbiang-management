import { fireEvent, render, screen, waitFor } from '@testing-library/react-native';
import React from 'react';
import { ApiError, api } from '../src/api/client';
import { ActivationScreen } from '../src/screens/ActivationScreen';
import { TestProviders } from '../testUtils';

jest.mock('../src/api/client', () => ({
  api: jest.fn(),
  ApiError: class ApiError extends Error {
    code: string;

    constructor(code: string) {
      super(code);
      this.code = code;
    }
  }
}));

jest.mock('../src/auth/tokenStore', () => ({ saveTokens: jest.fn() }));

describe('ActivationScreen', () => {
  it('renders expired invitation state', async () => {
    (api as jest.Mock).mockResolvedValueOnce({ valid: false, positions: [], code: 'INVITATION_INVALID_OR_EXPIRED' });

    render(<ActivationScreen token="expired" onSuccess={jest.fn()} />, { wrapper: TestProviders });

    expect(await screen.findByText('This invitation is invalid or expired.')).toBeTruthy();
  });

  it('renders duplicate-field API errors during activation', async () => {
    (api as jest.Mock)
      .mockResolvedValueOnce({ valid: true, positions: ['HOST'], code: 'OK' })
      .mockRejectedValueOnce(new ApiError('EMAIL_ALREADY_IN_USE'));

    render(<ActivationScreen token="valid" onSuccess={jest.fn()} />, { wrapper: TestProviders });

    await screen.findByText('Activate account');
    fireEvent.changeText(screen.getByLabelText('English name'), 'Taylor');
    fireEvent.changeText(screen.getByLabelText('Preferred name'), 'Taylor');
    fireEvent.changeText(screen.getByLabelText('Email'), 'taylor@example.com');
    fireEvent.changeText(screen.getByLabelText('Phone'), '2065550100');
    fireEvent.changeText(screen.getByLabelText('Password'), 'password123');
    fireEvent.changeText(screen.getByLabelText('Toast PIN'), '1234');
    fireEvent.press(screen.getByLabelText('Submit'));

    await waitFor(() => expect(screen.getByText('That value is already in use')).toBeTruthy());
  });
});
