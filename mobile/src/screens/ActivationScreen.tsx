import { zodResolver } from '@hookform/resolvers/zod';
import { useQuery } from '@tanstack/react-query';
import React, { useState } from 'react';
import { Controller, useForm } from 'react-hook-form';
import { StyleSheet, Text, View } from 'react-native';
import { z } from 'zod';
import { api, ApiError } from '../api/client';
import { saveTokens } from '../auth/tokenStore';
import { Field } from '../components/Field';
import { PrimaryButton } from '../components/PrimaryButton';
import { AppScreen, FormSection, InlineMessage, LoadingState, SecondaryButton } from '../components/designSystem';
import { positionLabelKey, storeLabelKey } from '../i18n/domainLabels';
import { useI18n } from '../i18n/I18nProvider';
import { colors, spacing, typography } from '../theme/theme';
import { Position, StoreCode } from '../types/domain';

const schema = z.object({
  englishName: z.string().min(1),
  preferredName: z.string().min(1),
  email: z.string().email(),
  phone: z.string().min(1),
  password: z.string().min(8),
  toastPin: z.string().min(1),
  homeStore: z.enum(['SEATTLE', 'REDMOND']),
  eligibleStores: z.array(z.enum(['SEATTLE', 'REDMOND'])).min(1)
});

type Values = z.infer<typeof schema>;

export function ActivationScreen({ token, onSuccess }: { token: string; onSuccess: () => void }) {
  const { t } = useI18n();
  const [error, setError] = useState<string | null>(null);
  const validation = useQuery({
    queryKey: ['invitation', token],
    queryFn: () => api<{ valid: boolean; positions: Position[]; code: string }>(`/api/public/invitations/validate?token=${encodeURIComponent(token)}`),
    enabled: token.length > 0
  });
  const { control, handleSubmit, formState } = useForm<Values>({
    resolver: zodResolver(schema),
    defaultValues: { englishName: '', preferredName: '', email: '', phone: '', password: '', toastPin: '', homeStore: 'SEATTLE', eligibleStores: ['SEATTLE'] }
  });

  async function submit(values: Values) {
    try {
      const response = await api<{ tokens: { accessToken: string; refreshToken: string } }>('/api/public/invitations/activate', {
        method: 'POST',
        body: JSON.stringify({ ...values, token })
      });
      await saveTokens(response.tokens.accessToken, response.tokens.refreshToken);
      onSuccess();
    } catch (err) {
      setError(err instanceof ApiError && ['EMAIL_ALREADY_IN_USE', 'PHONE_ALREADY_IN_USE', 'TOAST_PIN_ALREADY_IN_USE'].includes(err.code) ? t('duplicateField') : t('serverError'));
    }
  }

  if (validation.isLoading) return <LoadingState message={t('loading')} />;
  if (!validation.data?.valid) return <AppScreen title={t('activationTitle')}><FormSection><InlineMessage type="error" message={t('expiredInvitation')} /></FormSection></AppScreen>;

  return (
    <AppScreen title={t('activationTitle')}>
      <FormSection>
      <Text style={styles.description}>{t('positions')}: {validation.data.positions.map((position) => t(positionLabelKey(position))).join(', ')}</Text>
      {(['englishName', 'preferredName', 'email', 'phone', 'password', 'toastPin'] as const).map((name) => (
        <Controller
          key={name}
          control={control}
          name={name}
          render={({ field, fieldState }) => <Field label={t(name)} value={field.value} onChangeText={field.onChange} secureTextEntry={name === 'password'} error={fieldState.error ? t(name === 'email' ? 'validationEmail' : 'validationRequired') : undefined} />}
        />
      ))}
      <View style={styles.fieldGroup}>
        <Text style={styles.fieldLabel}>{t('homeStore')}</Text>
        <Controller control={control} name="homeStore" render={({ field }) => <SecondaryButton label={t(storeLabelKey(field.value as StoreCode))} onPress={() => field.onChange(field.value === 'SEATTLE' ? 'REDMOND' : 'SEATTLE')} accessibilityLabel={t('homeStore')} />} />
      </View>
      <View style={styles.fieldGroup}>
        <Text style={styles.fieldLabel}>{t('eligibleStores')}</Text>
        <Controller control={control} name="eligibleStores" render={({ field }) => <SecondaryButton label={field.value.map((store) => t(storeLabelKey(store))).join(', ')} onPress={() => field.onChange(field.value.length === 2 ? ['SEATTLE'] : ['SEATTLE', 'REDMOND'])} accessibilityLabel={t('eligibleStores')} />} />
      </View>
      {error ? <InlineMessage type="error" message={error} /> : null}
      <PrimaryButton label={t('submit')} onPress={handleSubmit(submit)} disabled={formState.isSubmitting} />
      </FormSection>
    </AppScreen>
  );
}

const styles = StyleSheet.create({
  description: {
    color: colors.ink,
    fontFamily: typography.interface,
    fontWeight: '700'
  },
  fieldGroup: {
    gap: spacing.xs
  },
  fieldLabel: {
    color: colors.ink,
    fontFamily: typography.interface,
    fontWeight: '800'
  }
});
