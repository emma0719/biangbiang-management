import { useMutation, useQuery } from '@tanstack/react-query';
import React from 'react';
import { Alert, StyleSheet, Text, View } from 'react-native';
import { api } from '../api/client';
import { PrimaryButton } from '../components/PrimaryButton';
import { AppScreen, DangerButton, EmptyState, EmployeeAvatar, LabelValue, PaperCard, SectionBanner, SecondaryButton, StatusBadge } from '../components/designSystem';
import { invitationStatusLabelKey, positionLabelKey, storeLabelKey } from '../i18n/domainLabels';
import { useI18n } from '../i18n/I18nProvider';
import { colors, layout, spacing, typography } from '../theme/theme';
import { EmployeePrivate, Invitation } from '../types/domain';

export function ManagerScreen() {
  const { t } = useI18n();
  const invitations = useQuery({ queryKey: ['invitations'], queryFn: () => api<Invitation[]>('/api/manager/invitations') });
  const employees = useQuery({ queryKey: ['employees'], queryFn: () => api<EmployeePrivate[]>('/api/manager/employees') });
  const createInvitation = useMutation({
    mutationFn: () => api<Invitation>('/api/manager/invitations', { method: 'POST', body: JSON.stringify({ positions: ['HOST'] }) }),
    onSuccess: (invitation) => Alert.alert(t('copyLink'), invitation.activationLink ?? '')
  });

  return (
    <AppScreen title={t('manager')} maxWidth={layout.managerMaxWidth}>
      <SectionBanner label={t('createInvitation')} tone="green" />
      <PaperCard>
        <PrimaryButton label={t('createInvitation')} onPress={() => createInvitation.mutate()} disabled={createInvitation.isPending} />
      </PaperCard>
      <SectionBanner label={t('invitationHistory')} tone="burgundy" />
      {invitations.data?.length ? invitations.data.map((invitation) => (
        <PaperCard key={invitation.id}>
          <StatusBadge label={t(invitationStatusLabelKey(invitation.status))} tone={invitation.status === 'ACTIVE' ? 'green' : invitation.status === 'REVOKED' ? 'danger' : 'paper'} />
          <LabelValue label={t('positions')} value={invitation.positions.map((position) => t(positionLabelKey(position))).join(', ')} />
          <View style={styles.actionRow}>
            <SecondaryButton label={t('regenerate')} onPress={() => api(`/api/manager/invitations/${invitation.id}/regenerate`, { method: 'POST' })} />
            <DangerButton label={t('revoke')} onPress={() => Alert.alert(t('confirm'), t('revoke'), [{ text: t('cancel') }, { text: t('confirm'), onPress: () => api(`/api/manager/invitations/${invitation.id}/revoke`, { method: 'POST' }) }])} />
          </View>
        </PaperCard>
      )) : <EmptyState message={t(invitations.isLoading ? 'loading' : 'empty')} />}
      <SectionBanner label={t('employees')} tone="green" />
      {employees.data?.map((employee) => (
        <PaperCard key={employee.id} tone={employee.status === 'DEACTIVATED' ? 'danger' : 'paper'}>
          <View style={styles.employeeHeader}>
            <EmployeeAvatar name={employee.displayName} />
            <View style={styles.employeeTitle}>
              <Text style={styles.employeeName}>{employee.displayName}</Text>
              <StatusBadge label={t(employee.status === 'ACTIVE' ? 'statusActive' : 'statusDeactivated')} tone={employee.status === 'ACTIVE' ? 'green' : 'danger'} />
            </View>
          </View>
          <LabelValue label={t('homeStore')} value={t(storeLabelKey(employee.homeStore))} />
          <LabelValue label={t('positions')} value={employee.positions.map((position) => t(positionLabelKey(position))).join(', ')} />
          <View style={styles.actionRow}>
            <SecondaryButton label={t('forceLogout')} onPress={() => api(`/api/manager/employees/${employee.id}/logout-all`, { method: 'POST' })} />
            <DangerButton label={t('deactivate')} onPress={() => Alert.alert(t('confirm'), t('deactivate'), [{ text: t('cancel') }, { text: t('confirm'), onPress: () => api(`/api/manager/employees/${employee.id}/deactivate`, { method: 'POST' }) }])} />
          </View>
        </PaperCard>
      ))}
      {!employees.data?.length ? <EmptyState message={t(employees.isLoading ? 'loading' : 'empty')} /> : null}
    </AppScreen>
  );
}

const styles = StyleSheet.create({
  employeeHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.md
  },
  employeeTitle: {
    flex: 1,
    gap: spacing.xs
  },
  employeeName: {
    color: colors.ink,
    fontFamily: typography.display,
    fontSize: 20,
    fontWeight: '800'
  },
  actionRow: {
    gap: spacing.sm
  }
});
