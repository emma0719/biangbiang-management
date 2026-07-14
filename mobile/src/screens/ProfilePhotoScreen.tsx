import * as ImagePicker from 'expo-image-picker';
import React, { useState } from 'react';
import { api } from '../api/client';
import { PrimaryButton } from '../components/PrimaryButton';
import { AppScreen, DangerButton, EmployeeAvatar, FormSection, InlineMessage } from '../components/designSystem';
import { useI18n } from '../i18n/I18nProvider';

export function ProfilePhotoScreen() {
  const { t } = useI18n();
  const [asset, setAsset] = useState<ImagePicker.ImagePickerAsset | null>(null);
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  async function pick() {
    setError(null); setMessage(null);
    const result = await ImagePicker.launchImageLibraryAsync({ mediaTypes: ImagePicker.MediaTypeOptions.Images, allowsEditing: true, quality: 0.9 });
    if (!result.canceled) setAsset(result.assets[0]);
  }

  async function upload() {
    if (!asset) { setError(t('validationRequired')); return; }
    setLoading(true); setError(null); setMessage(null);
    try {
      const formData = new FormData();
      formData.append('file', { uri: asset.uri, name: asset.fileName ?? 'profile.jpg', type: asset.mimeType ?? 'image/jpeg' } as unknown as Blob);
      await api('/api/me/profile-photo', { method: 'POST', body: formData });
      setMessage(t('photoUploadSuccess'));
    } catch { setError(t('apiError')); } finally { setLoading(false); }
  }

  async function remove() {
    setLoading(true); setError(null); setMessage(null);
    try {
      await api('/api/me/profile-photo', { method: 'DELETE' });
      setAsset(null);
      setMessage(t('photoRemoveSuccess'));
    } catch { setError(t('apiError')); } finally { setLoading(false); }
  }

  return (
    <AppScreen title={t('profilePhoto')}>
      <FormSection>
      <EmployeeAvatar name="Maya Chen" />
      <PrimaryButton label={t('selectPhoto')} onPress={pick} disabled={loading} />
      <PrimaryButton label={t('uploadPhoto')} onPress={upload} disabled={loading} />
      <DangerButton label={t('removePhoto')} onPress={remove} disabled={loading} />
      {error ? <InlineMessage type="error" message={error} /> : null}
      {message ? <InlineMessage type="success" message={message} /> : null}
      </FormSection>
    </AppScreen>
  );
}
