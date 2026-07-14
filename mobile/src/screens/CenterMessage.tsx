import React from 'react';
import { Text } from 'react-native';
import { AppScreen, PaperCard } from '../components/designSystem';
import { TranslationKey } from '../i18n/translations';
import { useI18n } from '../i18n/I18nProvider';

export function CenterMessage({ translationKey }: { translationKey: TranslationKey }) {
  const { t } = useI18n();
  return (
    <AppScreen title={t(translationKey)}>
      <PaperCard>
        <Text>{t(translationKey)}</Text>
      </PaperCard>
    </AppScreen>
  );
}
