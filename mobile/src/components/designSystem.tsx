import React from 'react';
import { ActivityIndicator, Pressable, ScrollView, StyleSheet, Text, TextInput, TextInputProps, View } from 'react-native';
import { router, usePathname } from 'expo-router';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useI18n } from '../i18n/I18nProvider';
import { TranslationKey } from '../i18n/translations';
import { colors, layout, radii, shadows, spacing, typography } from '../theme/theme';

type Tone = 'green' | 'burgundy' | 'paper' | 'danger';
export type MainTab = 'profile' | 'shift' | 'order' | 'inventory';

export function AppScreen({ children, title, subtitle, maxWidth = layout.maxWidth, activeMainTab, onBeforeMainTabChange }: { children: React.ReactNode; title?: string; subtitle?: string; maxWidth?: number; activeMainTab?: MainTab; onBeforeMainTabChange?: (path: '/profile' | '/shift' | '/order' | '/inventory') => boolean }) {
  return (
    <SafeAreaView style={styles.safeArea}>
      <ScrollView contentContainerStyle={[styles.scrollShell, activeMainTab ? styles.scrollShellWithTabs : null]} keyboardShouldPersistTaps="handled">
        <View style={[styles.content, { maxWidth }]}>
          {title ? <BrandHeader title={title} subtitle={subtitle} /> : null}
          {children}
        </View>
      </ScrollView>
      {activeMainTab ? <MainTabBar active={activeMainTab} onBeforeChange={onBeforeMainTabChange} /> : null}
    </SafeAreaView>
  );
}

function MainTabBar({ active, onBeforeChange }: { active: MainTab; onBeforeChange?: (path: '/profile' | '/shift' | '/order' | '/inventory') => boolean }) {
  const { t } = useI18n();
  const pathname = usePathname();
  const tabs: Array<{ key: MainTab; label: TranslationKey; path: '/profile' | '/shift' | '/order' | '/inventory' }> = [
    { key: 'profile', label: 'mainNavProfile', path: '/profile' },
    { key: 'shift', label: 'mainNavShift', path: '/shift' },
    { key: 'order', label: 'mainNavOrder', path: '/order' },
    { key: 'inventory', label: 'mainNavInventory', path: '/inventory' }
  ];
  return (
    <View style={styles.mainTabBar}>
      {tabs.map((tab) => {
        const selected = active === tab.key;
        return (
          <Pressable
            key={tab.key}
            accessibilityRole="button"
            accessibilityLabel={t(tab.label)}
            onPress={() => {
              if (pathname === tab.path) return;
              if (onBeforeChange && !onBeforeChange(tab.path)) return;
              router.replace(tab.path);
            }}
            style={({ pressed }) => [styles.mainTab, selected ? styles.mainTabActive : null, pressed ? styles.pressedButton : null]}
          >
            <Text style={[styles.mainTabText, selected ? styles.mainTabTextActive : null]}>{t(tab.label)}</Text>
          </Pressable>
        );
      })}
    </View>
  );
}

export function BrandHeader({ title, subtitle }: { title: string; subtitle?: string }) {
  return (
    <View style={styles.brandHeader}>
      <View style={styles.headerRule} />
      <Text style={styles.brandTitle}>{title}</Text>
      {subtitle ? <Text style={styles.brandSubtitle}>{subtitle}</Text> : null}
      <View style={styles.headerRule} />
    </View>
  );
}

export function SectionBanner({ label, tone = 'burgundy' }: { label: string; tone?: Extract<Tone, 'green' | 'burgundy'> }) {
  return (
    <View style={[styles.sectionBanner, tone === 'green' ? styles.sectionBannerGreen : styles.sectionBannerBurgundy]}>
      <View style={styles.bannerAccent} />
      <Text style={styles.sectionBannerText}>{label}</Text>
      <View style={styles.bannerAccent} />
    </View>
  );
}

export function PaperCard({ children, tone = 'paper' }: { children: React.ReactNode; tone?: Tone }) {
  return <View style={[styles.paperCard, tone === 'danger' ? styles.dangerCard : null]}>{children}</View>;
}

export function FormSection({ title, children }: { title?: string; children: React.ReactNode }) {
  return (
    <PaperCard>
      {title ? <Text style={styles.formSectionTitle}>{title}</Text> : null}
      <View style={styles.stack}>{children}</View>
    </PaperCard>
  );
}

export function Divider() {
  return <View style={styles.divider} />;
}

function Button({ label, onPress, disabled, accessibilityLabel, variant }: { label: string; onPress: () => void; disabled?: boolean; accessibilityLabel?: string; variant: 'primary' | 'secondary' | 'danger' }) {
  const buttonStyle = variant === 'primary' ? styles.primaryButton : variant === 'danger' ? styles.dangerButton : styles.secondaryButton;
  const textStyle = variant === 'secondary' ? styles.secondaryButtonText : styles.solidButtonText;
  return (
    <Pressable
      accessibilityRole="button"
      accessibilityLabel={accessibilityLabel ?? label}
      disabled={disabled}
      onPress={onPress}
      style={({ pressed }) => [styles.button, buttonStyle, disabled ? styles.disabledButton : null, pressed ? styles.pressedButton : null]}
    >
      <Text style={[styles.buttonText, textStyle]}>{label}</Text>
    </Pressable>
  );
}

export function PrimaryButton(props: { label: string; onPress: () => void; disabled?: boolean; accessibilityLabel?: string }) {
  return <Button {...props} variant="primary" />;
}

export function SecondaryButton(props: { label: string; onPress: () => void; disabled?: boolean; accessibilityLabel?: string }) {
  return <Button {...props} variant="secondary" />;
}

export function DangerButton(props: { label: string; onPress: () => void; disabled?: boolean; accessibilityLabel?: string }) {
  return <Button {...props} variant="danger" />;
}

export function BrandTextInput({ label, error, readOnly, ...props }: TextInputProps & { label: string; error?: string; readOnly?: boolean }) {
  return (
    <View style={styles.fieldWrap}>
      <Text accessibilityRole="text" style={styles.fieldLabel}>{label}</Text>
      <TextInput
        accessibilityLabel={label}
        placeholderTextColor={colors.mutedInk}
        style={[styles.input, error ? styles.inputError : null, readOnly || props.editable === false ? styles.inputReadOnly : null]}
        {...props}
      />
      {error ? <Text accessibilityRole="alert" style={styles.fieldError}>{error}</Text> : null}
    </View>
  );
}

export function StatusBadge({ label, tone = 'green' }: { label: string; tone?: Tone }) {
  const toneStyle = tone === 'danger' ? styles.badgeDanger : tone === 'burgundy' ? styles.badgeBurgundy : tone === 'paper' ? styles.badgePaper : styles.badgeGreen;
  return (
    <View style={[styles.badge, toneStyle]}>
      <View style={styles.badgeDot} />
      <Text style={styles.badgeText}>{label}</Text>
    </View>
  );
}

export function EmployeeAvatar({ name }: { name: string }) {
  const initials = name.split(' ').map((part) => part[0]).join('').slice(0, 2).toUpperCase();
  return (
    <View style={styles.avatar}>
      <Text style={styles.avatarText}>{initials}</Text>
    </View>
  );
}

export function EmptyState({ message }: { message: string }) {
  return (
    <PaperCard>
      <Text style={styles.emptyText}>{message}</Text>
    </PaperCard>
  );
}

export function LoadingState({ message }: { message: string }) {
  return (
    <AppScreen>
      <PaperCard>
        <ActivityIndicator color={colors.forestGreen} />
        <Text style={styles.emptyText}>{message}</Text>
      </PaperCard>
    </AppScreen>
  );
}

export function ErrorState({ message }: { message: string }) {
  return (
    <AppScreen>
      <PaperCard tone="danger">
        <Text style={styles.errorText}>{message}</Text>
      </PaperCard>
    </AppScreen>
  );
}

export function InlineMessage({ message, type }: { message: string; type: 'error' | 'success' }) {
  return <Text accessibilityRole={type === 'error' ? 'alert' : 'text'} style={type === 'error' ? styles.inlineError : styles.inlineSuccess}>{message}</Text>;
}

export function LabelValue({ label, value }: { label: string; value: string }) {
  return (
    <View style={styles.labelValue}>
      <Text style={styles.labelValueLabel}>{label}</Text>
      <Text style={styles.labelValueText}>{value}</Text>
    </View>
  );
}

export const styles = StyleSheet.create({
  safeArea: {
    flex: 1,
    backgroundColor: colors.paper
  },
  scrollShell: {
    flexGrow: 1,
    alignItems: 'center',
    padding: spacing.lg,
    backgroundColor: colors.paper
  },
  scrollShellWithTabs: {
    paddingBottom: spacing.xl * 3
  },
  content: {
    width: '100%',
    gap: spacing.lg
  },
  brandHeader: {
    backgroundColor: colors.burgundy,
    borderColor: colors.burgundyDark,
    borderWidth: 1,
    borderRadius: radii.md,
    padding: spacing.lg,
    gap: spacing.sm
  },
  brandTitle: {
    color: colors.white,
    fontFamily: typography.display,
    fontSize: 30,
    fontWeight: '800',
    letterSpacing: 0,
    textAlign: 'center'
  },
  brandSubtitle: {
    color: colors.paperLight,
    fontFamily: typography.interface,
    fontSize: 14,
    textAlign: 'center'
  },
  headerRule: {
    height: 1,
    backgroundColor: colors.orangeAccent,
    opacity: 0.75
  },
  sectionBanner: {
    borderRadius: radii.sm,
    paddingVertical: spacing.sm,
    paddingHorizontal: spacing.md,
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.sm
  },
  sectionBannerBurgundy: {
    backgroundColor: colors.burgundy
  },
  sectionBannerGreen: {
    backgroundColor: colors.forestGreen
  },
  sectionBannerText: {
    color: colors.white,
    fontFamily: typography.display,
    fontWeight: '800',
    fontSize: 16,
    letterSpacing: 0,
    textTransform: 'uppercase'
  },
  bannerAccent: {
    flex: 1,
    height: 1,
    backgroundColor: colors.orangeAccent
  },
  paperCard: {
    backgroundColor: colors.paperLight,
    borderColor: colors.border,
    borderWidth: 1,
    borderRadius: radii.md,
    padding: spacing.lg,
    gap: spacing.md,
    ...shadows.card
  },
  dangerCard: {
    borderColor: colors.error,
    backgroundColor: '#F7E4D8'
  },
  formSectionTitle: {
    color: colors.ink,
    fontFamily: typography.display,
    fontSize: 20,
    fontWeight: '800'
  },
  stack: {
    gap: spacing.md
  },
  divider: {
    height: 1,
    backgroundColor: colors.border
  },
  button: {
    minHeight: layout.buttonHeight,
    borderRadius: radii.sm,
    paddingHorizontal: spacing.lg,
    paddingVertical: spacing.md,
    alignItems: 'center',
    justifyContent: 'center',
    borderWidth: 1
  },
  primaryButton: {
    backgroundColor: colors.forestGreen,
    borderColor: colors.forestGreenDark
  },
  secondaryButton: {
    backgroundColor: colors.white,
    borderColor: colors.burgundy
  },
  dangerButton: {
    backgroundColor: colors.burgundy,
    borderColor: colors.burgundyDark
  },
  disabledButton: {
    backgroundColor: colors.disabled,
    borderColor: colors.disabled
  },
  pressedButton: {
    opacity: 0.82
  },
  buttonText: {
    fontFamily: typography.interface,
    fontSize: 15,
    fontWeight: '800',
    textAlign: 'center'
  },
  solidButtonText: {
    color: colors.white
  },
  secondaryButtonText: {
    color: colors.burgundy
  },
  fieldWrap: {
    gap: spacing.xs
  },
  fieldLabel: {
    color: colors.ink,
    fontFamily: typography.interface,
    fontWeight: '700'
  },
  input: {
    minHeight: layout.inputHeight,
    borderWidth: 1,
    borderColor: colors.border,
    borderRadius: radii.sm,
    backgroundColor: colors.white,
    color: colors.ink,
    paddingHorizontal: spacing.md,
    paddingVertical: spacing.sm,
    fontFamily: typography.interface,
    fontSize: 16
  },
  inputError: {
    borderColor: colors.error
  },
  inputReadOnly: {
    backgroundColor: '#EFE1C4',
    color: colors.mutedInk
  },
  fieldError: {
    color: colors.error,
    fontFamily: typography.interface,
    fontWeight: '700'
  },
  badge: {
    alignSelf: 'flex-start',
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.xs,
    borderRadius: radii.sm,
    borderWidth: 1,
    paddingHorizontal: spacing.sm,
    paddingVertical: spacing.xs
  },
  badgeGreen: {
    backgroundColor: '#E5F0E9',
    borderColor: colors.success
  },
  badgeBurgundy: {
    backgroundColor: '#F2D9D1',
    borderColor: colors.burgundy
  },
  badgePaper: {
    backgroundColor: colors.white,
    borderColor: colors.border
  },
  badgeDanger: {
    backgroundColor: '#F7E4D8',
    borderColor: colors.error
  },
  badgeDot: {
    width: 7,
    height: 7,
    borderRadius: 4,
    backgroundColor: colors.orangeAccent
  },
  badgeText: {
    color: colors.ink,
    fontFamily: typography.interface,
    fontSize: 12,
    fontWeight: '800'
  },
  avatar: {
    width: 48,
    height: 48,
    borderRadius: 24,
    backgroundColor: colors.forestGreen,
    alignItems: 'center',
    justifyContent: 'center',
    borderWidth: 1,
    borderColor: colors.orangeAccent
  },
  avatarText: {
    color: colors.white,
    fontFamily: typography.display,
    fontSize: 18,
    fontWeight: '800'
  },
  emptyText: {
    color: colors.mutedInk,
    fontFamily: typography.interface,
    fontSize: 15
  },
  errorText: {
    color: colors.error,
    fontFamily: typography.interface,
    fontWeight: '800'
  },
  inlineError: {
    color: colors.error,
    fontFamily: typography.interface,
    fontWeight: '800'
  },
  inlineSuccess: {
    color: colors.success,
    fontFamily: typography.interface,
    fontWeight: '800'
  },
  labelValue: {
    gap: 2
  },
  labelValueLabel: {
    color: colors.mutedInk,
    fontFamily: typography.interface,
    fontSize: 12,
    fontWeight: '800',
    textTransform: 'uppercase'
  },
  labelValueText: {
    color: colors.ink,
    fontFamily: typography.interface,
    fontSize: 15
  },
  mainTabBar: {
    flexDirection: 'row',
    gap: spacing.xs,
    borderTopWidth: 1,
    borderTopColor: colors.border,
    backgroundColor: colors.paperLight,
    paddingHorizontal: spacing.sm,
    paddingTop: spacing.sm,
    paddingBottom: spacing.sm
  },
  mainTab: {
    flex: 1,
    minHeight: 44,
    alignItems: 'center',
    justifyContent: 'center',
    borderRadius: radii.sm,
    borderWidth: 1,
    borderColor: 'transparent'
  },
  mainTabActive: {
    backgroundColor: colors.forestGreen,
    borderColor: colors.forestGreen
  },
  mainTabText: {
    color: colors.ink,
    fontFamily: typography.interface,
    fontWeight: '800',
    fontSize: 13
  },
  mainTabTextActive: {
    color: colors.white
  }
});
