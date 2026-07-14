import { zodResolver } from '@hookform/resolvers/zod';
import React, { useState } from 'react';
import { Controller, useForm } from 'react-hook-form';
import { z } from 'zod';
import { api } from '../api/client';
import { Field } from '../components/Field';
import { PrimaryButton } from '../components/PrimaryButton';
import { AppScreen, Divider, FormSection, InlineMessage } from '../components/designSystem';
import { useI18n } from '../i18n/I18nProvider';

type Kind = 'email' | 'phone';

export function ContactChangeScreen({ kind }: { kind: Kind }) {
  const { t } = useI18n();
  const [message, setMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const requestSchema = z.object({ value: kind === 'email' ? z.string().email() : z.string().min(1) });
  const verifySchema = z.object({ token: z.string().min(1) });
  const requestForm = useForm<{ value: string }>({ resolver: zodResolver(requestSchema), defaultValues: { value: '' } });
  const verifyForm = useForm<{ token: string }>({ resolver: zodResolver(verifySchema), defaultValues: { token: '' } });

  async function request(values: { value: string }) {
    setError(null); setMessage(null);
    try {
      await api(`/api/me/${kind}-change/request`, { method: 'POST', body: JSON.stringify({ [kind]: values.value }) });
      setMessage(t(kind === 'email' ? 'emailRequestSuccess' : 'phoneRequestSuccess'));
    } catch { setError(t('apiError')); }
  }

  async function verify(values: { token: string }) {
    setError(null); setMessage(null);
    try {
      await api(`/api/me/${kind}-change/verify`, { method: 'POST', body: JSON.stringify(values) });
      setMessage(t(kind === 'email' ? 'emailVerifySuccess' : 'phoneVerifySuccess'));
    } catch { setError(t('apiError')); }
  }

  return (
    <AppScreen title={t(kind === 'email' ? 'changeEmail' : 'changePhone')}>
      <FormSection>
      <Controller control={requestForm.control} name="value" render={({ field, fieldState }) => <Field label={t(kind)} value={field.value} onChangeText={field.onChange} autoCapitalize="none" error={fieldState.error ? t(kind === 'email' ? 'validationEmail' : 'validationRequired') : undefined} />} />
      <PrimaryButton label={t('requestChange')} onPress={requestForm.handleSubmit(request)} disabled={requestForm.formState.isSubmitting} />
      <Divider />
      <Controller control={verifyForm.control} name="token" render={({ field, fieldState }) => <Field label={t('verificationToken')} value={field.value} onChangeText={field.onChange} autoCapitalize="none" error={fieldState.error ? t('validationRequired') : undefined} />} />
      {error ? <InlineMessage type="error" message={error} /> : null}
      {message ? <InlineMessage type="success" message={message} /> : null}
      <PrimaryButton label={t('verify')} onPress={verifyForm.handleSubmit(verify)} disabled={verifyForm.formState.isSubmitting} />
      </FormSection>
    </AppScreen>
  );
}
