import { zodResolver } from '@hookform/resolvers/zod';
import React, { useState } from 'react';
import { Controller, useForm } from 'react-hook-form';
import { z } from 'zod';
import { api } from '../api/client';
import { Field } from '../components/Field';
import { PrimaryButton } from '../components/PrimaryButton';
import { AppScreen, FormSection, InlineMessage } from '../components/designSystem';
import { useI18n } from '../i18n/I18nProvider';

const schema = z.object({ toastPin: z.string().min(1) });
type Values = z.infer<typeof schema>;

export function ChangeToastPinScreen() {
  const { t } = useI18n();
  const [message, setMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const { control, handleSubmit, formState } = useForm<Values>({ resolver: zodResolver(schema), defaultValues: { toastPin: '' } });
  async function submit(values: Values) {
    setError(null); setMessage(null);
    try {
      await api('/api/me/toast-pin', { method: 'PATCH', body: JSON.stringify(values) });
      setMessage(t('toastPinSuccess'));
    } catch { setError(t('apiError')); }
  }
  return (
    <AppScreen title={t('changeToastPin')}>
      <FormSection>
      <Controller control={control} name="toastPin" render={({ field, fieldState }) => <Field label={t('toastPin')} value={field.value} onChangeText={field.onChange} error={fieldState.error ? t('validationRequired') : undefined} />} />
      {error ? <InlineMessage type="error" message={error} /> : null}
      {message ? <InlineMessage type="success" message={message} /> : null}
      <PrimaryButton label={t('save')} onPress={handleSubmit(submit)} disabled={formState.isSubmitting} />
      </FormSection>
    </AppScreen>
  );
}
