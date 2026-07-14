import { zodResolver } from '@hookform/resolvers/zod';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import React, { useEffect, useState } from 'react';
import { Controller, useForm } from 'react-hook-form';
import { z } from 'zod';
import { api } from '../api/client';
import { Field } from '../components/Field';
import { PrimaryButton } from '../components/PrimaryButton';
import { AppScreen, FormSection, InlineMessage, LoadingState } from '../components/designSystem';
import { useI18n } from '../i18n/I18nProvider';
import { EmployeePrivate } from '../types/domain';

const schema = z.object({ englishName: z.string().min(1), preferredName: z.string().min(1) });
type Values = z.infer<typeof schema>;

export function EditProfileScreen() {
  const { t } = useI18n();
  const queryClient = useQueryClient();
  const [message, setMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const profile = useQuery({ queryKey: ['me'], queryFn: () => api<EmployeePrivate>('/api/me') });
  const { control, handleSubmit, reset, formState } = useForm<Values>({ resolver: zodResolver(schema), defaultValues: { englishName: '', preferredName: '' } });

  useEffect(() => {
    if (profile.data) reset({ englishName: profile.data.englishName, preferredName: profile.data.preferredName });
  }, [profile.data, reset]);

  async function submit(values: Values) {
    setError(null); setMessage(null);
    try {
      await api('/api/me/profile', { method: 'PATCH', body: JSON.stringify(values) });
      await queryClient.invalidateQueries({ queryKey: ['me'] });
      setMessage(t('success'));
    } catch {
      setError(t('apiError'));
    }
  }

  if (profile.isLoading) return <LoadingState message={t('loading')} />;
  return (
    <AppScreen title={t('editProfile')}>
      <FormSection>
      <Controller control={control} name="englishName" render={({ field, fieldState }) => <Field label={t('englishName')} value={field.value} onChangeText={field.onChange} error={fieldState.error ? t('validationRequired') : undefined} />} />
      <Controller control={control} name="preferredName" render={({ field, fieldState }) => <Field label={t('preferredName')} value={field.value} onChangeText={field.onChange} error={fieldState.error ? t('validationRequired') : undefined} />} />
      {error ? <InlineMessage type="error" message={error} /> : null}
      {message ? <InlineMessage type="success" message={message} /> : null}
      <PrimaryButton label={t('save')} onPress={handleSubmit(submit)} disabled={formState.isSubmitting} />
      </FormSection>
    </AppScreen>
  );
}
