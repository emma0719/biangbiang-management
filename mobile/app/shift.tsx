import React from 'react';
import { Text } from 'react-native';
import { AppScreen, PaperCard, SectionBanner } from '../src/components/designSystem';
import { useI18n } from '../src/i18n/I18nProvider';
import { colors, typography } from '../src/theme/theme';

export default function Shift() {
  const { t } = useI18n();
  return (
    <AppScreen title={t('mainNavShift')} activeMainTab="shift">
      <SectionBanner label={t('mainNavShift')} tone="green" />
      <PaperCard>
        <Text style={{ color: colors.ink, fontFamily: typography.display, fontSize: 22, fontWeight: '800' }}>{t('shiftUnavailable')}</Text>
        <Text style={{ color: colors.mutedInk, fontFamily: typography.interface, fontSize: 15 }}>{t('shiftComingSoon')}</Text>
      </PaperCard>
      <PaperCard>
        <Text style={{ color: colors.ink, fontFamily: typography.interface, fontWeight: '800' }}>{t('shiftPostCoverage')}</Text>
        <Text style={{ color: colors.ink, fontFamily: typography.interface, fontWeight: '800' }}>{t('shiftViewAvailable')}</Text>
        <Text style={{ color: colors.ink, fontFamily: typography.interface, fontWeight: '800' }}>{t('shiftRequestCoverage')}</Text>
        <Text style={{ color: colors.ink, fontFamily: typography.interface, fontWeight: '800' }}>{t('shiftManagerApproval')}</Text>
      </PaperCard>
    </AppScreen>
  );
}
