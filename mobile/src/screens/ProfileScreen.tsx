import { useQuery } from '@tanstack/react-query';
import { router } from 'expo-router';
import React from 'react';
import { Alert, Pressable, StyleSheet, Text, View } from 'react-native';
import { api } from '../api/client';
import { clearTokens, getTokens } from '../auth/tokenStore';
import { AppScreen, EmployeeAvatar, ErrorState, LabelValue, LoadingState, PaperCard, SectionBanner, SecondaryButton, StatusBadge } from '../components/designSystem';
import { positionLabelKey, storeLabelKey } from '../i18n/domainLabels';
import { useI18n } from '../i18n/I18nProvider';
import { colors, spacing, typography } from '../theme/theme';
import { EmployeePrivate } from '../types/domain';

export function ProfileScreen() {
  const { t, locale, setLocale } = useI18n();
  const profile = useQuery({ queryKey: ['me'], queryFn: () => api<EmployeePrivate>('/api/me') });

  async function logoutAll() {
    Alert.alert(t('confirm'), t('logoutAll'), [
      { text: t('cancel') },
      { text: t('confirm'), onPress: async () => { await api('/api/auth/logout-all', { method: 'POST' }); await clearTokens(); router.replace('/'); } }
    ]);
  }

  async function logoutCurrent() {
    const { refreshToken } = await getTokens();
    if (refreshToken) await api('/api/auth/logout', { method: 'POST', body: JSON.stringify({ refreshToken }) });
    await clearTokens();
    router.replace('/');
  }

  if (profile.isLoading) return <LoadingState message={t('loading')} />;
  if (!profile.data) return <ErrorState message={t('networkError')} />;
  const isBusinessPartner = profile.data.positions.some((position) => ['OWNER', 'MANAGER', 'FINANCIAL_MANAGER'].includes(position));
  const nextLocale = locale === 'en' ? 'es' : locale === 'es' ? 'zh' : 'en';
  const languageLabel = locale === 'en' ? 'languageEnglish' : locale === 'es' ? 'languageSpanish' : 'languageChinese';

  return (
    <AppScreen title={t('profile')} activeMainTab="profile">
      <PaperCard>
        <View style={styles.profileHeader}>
          <EmployeeAvatar name={profile.data.displayName} />
          <View style={styles.profileHeaderText}>
            <Text style={styles.displayName}>{profile.data.displayName}</Text>
            <StatusBadge label={profile.data.positions.map((position) => t(positionLabelKey(position))).join(', ')} tone="green" />
          </View>
        </View>
        <LabelValue label={t('homeStore')} value={t(storeLabelKey(profile.data.homeStore))} />
      </PaperCard>
      <SectionBanner label="MY ACCOUNT" tone="green" />
      <PaperCard>
        <NavRow label={t('editProfile')} onPress={() => router.push('/edit-profile')} />
        <NavRow label={t('stores')} onPress={() => router.push('/stores')} />
        <NavRow label={t('changeToastPin')} onPress={() => router.push('/toast-pin')} />
        <NavRow label={t('changeEmail')} onPress={() => router.push('/email-change')} />
        <NavRow label={t('changePhone')} onPress={() => router.push('/phone-change')} />
        <NavRow label={t('profilePhoto')} onPress={() => router.push('/profile-photo')} />
      </PaperCard>
      <SectionBanner label={t('language')} tone="burgundy" />
      <PaperCard>
        <SecondaryButton label={t(languageLabel)} onPress={() => setLocale(nextLocale)} />
      </PaperCard>
      <SectionBanner label={t('security')} tone="burgundy" />
      <PaperCard>
        <SecondaryButton label={t('logout')} onPress={logoutCurrent} />
        <SecondaryButton label={t('logoutAll')} accessibilityLabel={t('logoutAll')} onPress={logoutAll} />
      </PaperCard>
      {isBusinessPartner ? (
        <>
          <SectionBanner label="MANAGEMENT" tone="green" />
          <PaperCard>
            <NavRow label={t('peopleManagement')} onPress={() => router.push('/manager')} />
          </PaperCard>
        </>
      ) : null}
    </AppScreen>
  );
}

function NavRow({ label, onPress }: { label: string; onPress: () => void }) {
  return (
    <Pressable accessibilityRole="button" accessibilityLabel={label} onPress={onPress} style={({ pressed }) => [styles.navRow, pressed ? styles.navRowPressed : null]}>
      <Text style={styles.navRowText}>{label}</Text>
      <Text style={styles.navArrow}>›</Text>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  profileHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.md
  },
  profileHeaderText: {
    flex: 1,
    gap: spacing.xs
  },
  displayName: {
    color: colors.ink,
    fontFamily: typography.display,
    fontSize: 24,
    fontWeight: '800'
  },
  navRow: {
    minHeight: 46,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    borderBottomWidth: 1,
    borderBottomColor: colors.border
  },
  navRowPressed: {
    opacity: 0.7
  },
  navRowText: {
    color: colors.ink,
    fontFamily: typography.interface,
    fontWeight: '800'
  },
  navArrow: {
    color: colors.burgundy,
    fontFamily: typography.display,
    fontSize: 24,
    fontWeight: '800'
  }
});
