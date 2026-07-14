import { fireEvent, render, screen } from '@testing-library/react-native';
import React from 'react';
import { api } from '../src/api/client';
import { DeveloperPreviewScreen } from '../src/screens/DeveloperPreviewScreen';
import { LoginScreen } from '../src/screens/LoginScreen';
import { TestProviders } from '../testUtils';

jest.mock('../src/api/client', () => ({ api: jest.fn() }));

describe('Developer preview', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('shows a development-only entry from the login screen', () => {
    const onDeveloperPreview = jest.fn();

    render(<LoginScreen onSuccess={jest.fn()} onDeveloperPreview={onDeveloperPreview} />, { wrapper: TestProviders });

    fireEvent.press(screen.getByLabelText('Developer Preview'));
    expect(onDeveloperPreview).toHaveBeenCalledTimes(1);
  });

  it('renders every implemented preview target without calling real APIs', () => {
    render(<DeveloperPreviewScreen />, { wrapper: TestProviders });

    const labels = [
      'Login',
      'Invitation activation',
      'Invalid/expired invitation',
      'Activation success',
      'Forgot password',
      'Profile',
      'Edit profile',
      'Eligible stores',
      'Change Toast PIN',
      'Change email request',
      'Change email verification',
      'Change phone request',
      'Change phone verification',
      'Profile photo',
      'Security settings',
      'Language settings',
      'Manager create invitation',
      'Manager invitation history',
      'Manager employee list',
      'Manager employee detail'
    ];

    for (const label of labels) {
      fireEvent.press(screen.getByLabelText(label));
      expect(screen.getAllByText(label).length).toBeGreaterThan(0);
    }

    expect(screen.getByText('Development-only preview using mock data')).toBeTruthy();
    expect(api).not.toHaveBeenCalled();
  });

  it('switches preview labels between English and Spanish', () => {
    render(<DeveloperPreviewScreen />, { wrapper: TestProviders });

    fireEvent.press(screen.getByLabelText('Switch to Spanish'));

    expect(screen.getByText('Vista previa de desarrollo')).toBeTruthy();
    expect(screen.getByLabelText('Cambiar a ingles')).toBeTruthy();
    expect(api).not.toHaveBeenCalled();
  });
});
