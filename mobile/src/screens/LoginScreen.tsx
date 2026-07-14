import { zodResolver } from '@hookform/resolvers/zod';
import React, { useState } from 'react';
import { Controller, useForm } from 'react-hook-form';
import { Pressable, StyleSheet, Text, View } from 'react-native';
import { z } from 'zod';
import { api, ApiError } from '../api/client';
import { saveTokens } from '../auth/tokenStore';
import { Field } from '../components/Field';
import { PrimaryButton } from '../components/PrimaryButton';
import { AppScreen, BrandHeader, FormSection, InlineMessage, SecondaryButton } from '../components/designSystem';
import { useI18n } from '../i18n/I18nProvider';
import { colors, spacing, typography } from '../theme/theme';

const schema = z.object({
  identifier: z.string().min(1),
  password: z.string().min(1)
});

type Values = z.infer<typeof schema>;

export function LoginScreen({ onSuccess, onDeveloperPreview }: { onSuccess: () => void; onDeveloperPreview?: () => void }) {
  const { t } = useI18n();
  const [error, setError] = useState<string | null>(null);
  const { control, handleSubmit, formState } = useForm<Values>({ resolver: zodResolver(schema), defaultValues: { identifier: '', password: '' } });

  async function submit(values: Values) {
    setError(null);
    try {
      const tokens = await api<{ accessToken: string; refreshToken: string }>('/api/auth/login', { method: 'POST', body: JSON.stringify(values) });
      await saveTokens(tokens.accessToken, tokens.refreshToken);
      onSuccess();
    } catch (err) {
      setError(err instanceof ApiError && err.code === 'AUTH_INVALID_CREDENTIALS' ? t('invalidCredentials') : t('networkError'));
    }
  }

  return (
    <AppScreen>
      <View style={styles.decorativeSeal}>
        <Text style={styles.decorativeSealText}>面</Text>
      </View>
      <BrandHeader title="Restaurant Ops" subtitle={t('loginTitle')} />
      <FormSection>
        <Controller control={control} name="identifier" render={({ field, fieldState }) => <Field label={t('identifier')} value={field.value} onChangeText={field.onChange} autoCapitalize="none" error={fieldState.error ? t('validationRequired') : undefined} />} />
        <Controller control={control} name="password" render={({ field, fieldState }) => <Field label={t('password')} value={field.value} onChangeText={field.onChange} secureTextEntry error={fieldState.error ? t('validationRequired') : undefined} />} />
        {error ? <InlineMessage type="error" message={error} /> : null}
        <PrimaryButton label={t('submit')} onPress={handleSubmit(submit)} disabled={formState.isSubmitting} />
        <Pressable accessibilityRole="button" accessibilityLabel={t('forgotPassword')} style={styles.secondaryLink}>
          <Text style={styles.secondaryLinkText}>{t('forgotPassword')}</Text>
        </Pressable>
      </FormSection>
      {__DEV__ && onDeveloperPreview ? (
        <SecondaryButton label={t('developerPreview')} onPress={onDeveloperPreview} accessibilityLabel={t('developerPreview')} />
      ) : null}
    </AppScreen>
  );
}

const styles = StyleSheet.create({
  decorativeSeal: {
    alignSelf: 'center',
    width: 54,
    height: 54,
    borderRadius: 27,
    borderWidth: 2,
    borderColor: colors.orangeAccent,
    backgroundColor: colors.burgundy,
    alignItems: 'center',
    justifyContent: 'center'
  },
  decorativeSealText: {
    color: colors.paperLight,
    fontFamily: typography.display,
    fontSize: 26,
    fontWeight: '800'
  },
  secondaryLink: {
    minHeight: 44,
    alignItems: 'center',
    justifyContent: 'center',
    paddingVertical: spacing.sm
  },
  secondaryLinkText: {
    color: colors.burgundy,
    fontFamily: typography.interface,
    fontWeight: '800'
  }
});
