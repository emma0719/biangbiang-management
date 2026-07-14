import { fireEvent, render, screen, waitFor } from '@testing-library/react-native';
import React from 'react';
import { api } from '../src/api/client';
import { ProfilePhotoScreen } from '../src/screens/ProfilePhotoScreen';
import { TestProviders } from '../testUtils';

jest.mock('../src/api/client', () => ({ api: jest.fn() }));
jest.mock('expo-image-picker', () => ({
  MediaTypeOptions: { Images: 'Images' },
  launchImageLibraryAsync: jest.fn().mockResolvedValue({
    canceled: false,
    assets: [{ uri: 'file:///photo.png', fileName: 'photo.png', mimeType: 'image/png' }]
  })
}));

describe('ProfilePhotoScreen', () => {
  it('uploads and removes a profile photo', async () => {
    (api as jest.Mock).mockResolvedValue({});
    render(<ProfilePhotoScreen />, { wrapper: TestProviders });

    fireEvent.press(screen.getByLabelText('Select photo'));
    await waitFor(() => expect(screen.getByText('Profile photo')).toBeTruthy());
    fireEvent.press(screen.getByLabelText('Upload photo'));
    expect(await screen.findByText('Profile photo uploaded')).toBeTruthy();
    fireEvent.press(screen.getByLabelText('Remove photo'));
    expect(await screen.findByText('Profile photo removed')).toBeTruthy();
  });
});
