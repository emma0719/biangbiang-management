import { zodResolver } from '@hookform/resolvers/zod';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import React, { useEffect, useState } from 'react';
import { Controller, useForm } from 'react-hook-form';
import { StyleSheet, Text } from 'react-native';
import { z } from 'zod';
import { api } from '../api/client';
import { PrimaryButton } from '../components/PrimaryButton';
import { AppScreen, FormSection, InlineMessage, LabelValue, LoadingState, SecondaryButton } from '../components/designSystem';
import { storeLabelKey } from '../i18n/domainLabels';
import { useI18n } from '../i18n/I18nProvider';
import { colors, spacing, typography } from '../theme/theme';
import { EmployeePrivate, StoreCode } from '../types/domain';

const schema = z.object({ eligibleStores: z.array(z.enum(['SEATTLE', 'REDMOND'])).min(1) });
type Values = z.infer<typeof schema>;

export function StorePreferencesScreen() {
  const { t } = useI18n();
  const queryClient = useQueryClient();
  const [message, setMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const profile = useQuery({ queryKey: ['me'], queryFn: () => api<EmployeePrivate>('/api/me') });
  const { control, handleSubmit, reset, formState } = useForm<Values>({ resolver: zodResolver(schema), defaultValues: { eligibleStores: ['SEATTLE'] } });
  useEffect(() => { if (profile.data) reset({ eligibleStores: profile.data.eligibleStores }); }, [profile.data, reset]);
  async function submit(values: Values) {
    setError(null); setMessage(null);
    try {
      await api('/api/me/stores', { method: 'PATCH', body: JSON.stringify(values) });
      await queryClient.invalidateQueries({ queryKey: ['me'] });
      setMessage(t('storesSuccess'));
    } catch { setError(t('apiError')); }
  }
  if (profile.isLoading) return <LoadingState message={t('loading')} />;
  return (
    <AppScreen title={t('stores')}>
      <FormSection>
      <LabelValue label={t('homeStore')} value={profile.data ? t(storeLabelKey(profile.data.homeStore)) : ''} />
      <Text style={styles.readOnlyHint}>{t('readOnlyHomeStore')}</Text>
      <Controller control={control} name="eligibleStores" render={({ field }) => <SecondaryButton label={field.value.map((store: StoreCode) => t(storeLabelKey(store))).join(', ')} onPress={() => field.onChange(field.value.length === 2 ? ['SEATTLE'] : ['SEATTLE', 'REDMOND'])} accessibilityLabel={t('eligibleStores')} />} />
      {error ? <InlineMessage type="error" message={error} /> : null}
      {message ? <InlineMessage type="success" message={message} /> : null}
      <PrimaryButton label={t('save')} onPress={handleSubmit(submit)} disabled={formState.isSubmitting} />
      </FormSection>
    </AppScreen>
  );
}

const styles = StyleSheet.create({
  readOnlyHint: {
    color: colors.mutedInk,
    fontFamily: typography.interface,
    marginBottom: spacing.sm
  }
});
