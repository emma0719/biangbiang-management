import React from 'react';
import { Modal, StyleSheet, Text, View } from 'react-native';
import { colors, radii, shadows, spacing, typography } from '../theme/theme';
import { DangerButton, PrimaryButton, SecondaryButton } from './designSystem';

type ConfirmDialogAction = {
  label: string;
  onPress: () => void;
  variant?: 'primary' | 'secondary' | 'danger';
  disabled?: boolean;
  accessibilityLabel?: string;
};

export function ConfirmDialog({ visible, title, actions, message, errorMessage, onCancel }: { visible: boolean; title: string; actions: ConfirmDialogAction[]; message?: string; errorMessage?: string; onCancel: () => void }) {
  if (!visible) return null;
  return (
    <Modal visible={visible} transparent animationType="fade" onRequestClose={onCancel}>
      <View style={styles.backdrop}>
        <View accessibilityRole="alert" style={styles.dialog}>
          <Text style={styles.title}>{title}</Text>
          {message ? <Text style={styles.message}>{message}</Text> : null}
          {errorMessage ? <Text accessibilityRole="alert" style={styles.error}>{errorMessage}</Text> : null}
          <View style={styles.actions}>
            {actions.map((action) => {
              if (action.variant === 'danger') {
                return <DangerButton key={action.label} label={action.label} onPress={action.onPress} disabled={action.disabled} accessibilityLabel={action.accessibilityLabel} />;
              }
              if (action.variant === 'secondary') {
                return <SecondaryButton key={action.label} label={action.label} onPress={action.onPress} disabled={action.disabled} accessibilityLabel={action.accessibilityLabel} />;
              }
              return <PrimaryButton key={action.label} label={action.label} onPress={action.onPress} disabled={action.disabled} accessibilityLabel={action.accessibilityLabel} />;
            })}
          </View>
        </View>
      </View>
    </Modal>
  );
}

const styles = StyleSheet.create({
  backdrop: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: 'rgba(45, 30, 22, 0.42)',
    padding: spacing.lg
  },
  dialog: {
    width: '100%',
    maxWidth: 420,
    borderRadius: radii.md,
    borderWidth: 1,
    borderColor: colors.border,
    backgroundColor: colors.paperLight,
    padding: spacing.lg,
    gap: spacing.md,
    ...shadows.card
  },
  title: {
    color: colors.ink,
    fontFamily: typography.display,
    fontSize: 22,
    fontWeight: '800',
    letterSpacing: 0
  },
  message: {
    color: colors.mutedInk,
    fontFamily: typography.interface,
    fontSize: 15
  },
  error: {
    color: colors.error,
    fontFamily: typography.interface,
    fontWeight: '700'
  },
  actions: {
    gap: spacing.sm
  }
});
