import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { router } from 'expo-router';
import React, { useMemo, useState } from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';
import { ApiError, api } from '../api/client';
import {
  approveCoverageRequest,
  cancelCoverageRequest,
  claimCoverageRequest,
  CoverageRequest,
  CoverageStatus,
  CoverageType,
  createCoverageRequest,
  getActiveEmployees,
  getCoverageHistory,
  getCoverageInvolvingMe,
  getCoveragePending,
  getCoveragePool,
  getCoverageUpcoming,
  getMyCoverageRequests,
  rejectCoverageRequest,
  ShiftType
} from '../api/coverage';
import { clearTokens } from '../auth/tokenStore';
import { ConfirmDialog } from '../components/ConfirmDialog';
import { AppScreen, BrandTextInput, EmptyState, InlineMessage, LabelValue, PaperCard, PrimaryButton, SecondaryButton, SectionBanner, StatusBadge } from '../components/designSystem';
import { positionLabelKey, storeLabelKey } from '../i18n/domainLabels';
import { useI18n } from '../i18n/I18nProvider';
import { TranslationKey } from '../i18n/translations';
import { colors, spacing, typography } from '../theme/theme';
import { EmployeePrivate, EmployeePublic, Position, StoreCode } from '../types/domain';

type CoverageTab = 'new' | 'pool' | 'mine' | 'pending' | 'upcoming' | 'history';
type PendingDialog =
  | { type: 'claim'; request: CoverageRequest }
  | { type: 'cancel'; request: CoverageRequest }
  | { type: 'approve'; request: CoverageRequest }
  | { type: 'reject'; request: CoverageRequest };

type FormState = {
  store: StoreCode;
  coverageType: CoverageType;
  shiftDate: string;
  startTime: string;
  endTime: string;
  shiftType: ShiftType;
  position: Position;
  replacementEmployeeId: string;
  reason: string;
};

const stores: StoreCode[] = ['SEATTLE', 'REDMOND'];
const shiftTypes: ShiftType[] = ['LUNCH', 'DINNER', 'DOUBLE'];
const positions: Position[] = ['FOOD_RUNNER', 'HOST', 'BARTENDER', 'SERVER_ONE_STAR', 'SERVER_TWO_STAR', 'SHIFT_LEADER', 'MANAGER'];
const initialForm: FormState = {
  store: 'SEATTLE',
  coverageType: 'PUBLIC',
  shiftDate: today(),
  startTime: '',
  endTime: '',
  shiftType: 'DINNER',
  position: 'HOST',
  replacementEmployeeId: '',
  reason: ''
};

export function ShiftCoverageScreen() {
  const { t } = useI18n();
  const queryClient = useQueryClient();
  const [tab, setTab] = useState<CoverageTab>('new');
  const [form, setForm] = useState<FormState>(initialForm);
  const [message, setMessage] = useState<TranslationKey | undefined>();
  const [error, setError] = useState<TranslationKey | undefined>();
  const [dialog, setDialog] = useState<PendingDialog | undefined>();
  const [managerNotes, setManagerNotes] = useState<Record<number, string>>({});

  const profile = useQuery({ queryKey: ['me'], queryFn: () => api<EmployeePrivate>('/api/me') });
  const isBusinessPartner = isCoverageManager(profile.data);
  const employees = useQuery({ queryKey: ['coverage', 'employees'], queryFn: getActiveEmployees });
  const pool = useQuery({ queryKey: ['coverage', 'pool'], queryFn: getCoveragePool });
  const mine = useQuery({ queryKey: ['coverage', 'mine'], queryFn: getMyCoverageRequests });
  const involvingMe = useQuery({ queryKey: ['coverage', 'involving-me'], queryFn: getCoverageInvolvingMe });
  const pending = useQuery({ queryKey: ['coverage', 'manager', 'pending'], queryFn: getCoveragePending, enabled: isBusinessPartner });
  const upcoming = useQuery({ queryKey: ['coverage', 'manager', 'upcoming'], queryFn: () => getCoverageUpcoming(14), enabled: isBusinessPartner });
  const history = useQuery({ queryKey: ['coverage', 'manager', 'history'], queryFn: getCoverageHistory, enabled: isBusinessPartner });

  const currentEmployeeId = profile.data?.id;
  const replacementOptions = useMemo(() => asArray(employees.data)
    .filter((employee) => employee.status === 'ACTIVE' && employee.id !== currentEmployeeId)
    .sort((left, right) => left.displayName.localeCompare(right.displayName)), [employees.data, currentEmployeeId]);

  React.useEffect(() => {
    if (profile.data?.homeStore && form.store === initialForm.store) {
      setForm((current) => ({ ...current, store: profile.data!.homeStore }));
    }
  }, [profile.data?.homeStore]);

  React.useEffect(() => {
    if (!isBusinessPartner && ['pending', 'upcoming', 'history'].includes(tab)) {
      setTab('new');
    }
  }, [isBusinessPartner, tab]);

  React.useEffect(() => {
    if ([profile.error, employees.error, pool.error, mine.error, involvingMe.error, pending.error, upcoming.error, history.error].some(isAuthInvalidError)) {
      clearTokens().finally(() => router.replace('/'));
    }
  }, [profile.error, employees.error, pool.error, mine.error, involvingMe.error, pending.error, upcoming.error, history.error]);

  const refreshCoverage = () => {
    queryClient.invalidateQueries({ queryKey: ['coverage'] });
    queryClient.refetchQueries({ queryKey: ['coverage'] });
  };

  const refreshManagerCoverage = () => {
    queryClient.invalidateQueries({ queryKey: ['coverage', 'manager'] });
    queryClient.refetchQueries({ queryKey: ['coverage', 'manager'] });
  };

  const createMutation = useMutation({
    mutationFn: () => {
      const validation = validateForm(form);
      if (validation) throw new ApiError(validation);
      return createCoverageRequest({
        store: form.store,
        coverageType: form.coverageType,
        shiftDate: form.shiftDate,
        startTime: form.startTime.trim() || null,
        endTime: form.endTime.trim() || null,
        shiftType: form.shiftType,
        position: form.position,
        reason: form.reason.trim() || null,
        replacementEmployeeId: form.coverageType === 'DIRECT' ? Number(form.replacementEmployeeId) : null
      });
    },
    onSuccess: () => {
      setForm({ ...initialForm, store: profile.data?.homeStore ?? initialForm.store });
      setMessage('coverageCreateSuccess');
      setError(undefined);
      setTab('mine');
      refreshCoverage();
    },
    onError: (failure) => {
      setMessage(undefined);
      setError(errorKey(failure));
    }
  });

  const claimMutation = useMutation({
    mutationFn: (id: number) => claimCoverageRequest(id),
    onSuccess: () => {
      setDialog(undefined);
      setMessage('coverageClaimSuccess');
      setError(undefined);
      refreshCoverage();
    },
    onError: (failure) => {
      setDialog(undefined);
      setMessage(undefined);
      setError(errorKey(failure));
      refreshCoverage();
    }
  });

  const cancelMutation = useMutation({
    mutationFn: (id: number) => cancelCoverageRequest(id),
    onSuccess: () => {
      setDialog(undefined);
      setMessage('coverageCancelSuccess');
      setError(undefined);
      refreshCoverage();
    },
    onError: (failure) => {
      setDialog(undefined);
      setMessage(undefined);
      setError(errorKey(failure));
    }
  });

  const approveMutation = useMutation({
    mutationFn: (id: number) => approveCoverageRequest(id),
    onSuccess: () => {
      setDialog(undefined);
      setMessage('coverageApproveSuccess');
      setError(undefined);
      refreshCoverage();
    },
    onError: (failure) => {
      setDialog(undefined);
      setMessage(undefined);
      setError(errorKey(failure));
      if (isRefreshableManagerError(failure)) refreshManagerCoverage();
    }
  });

  const rejectMutation = useMutation({
    mutationFn: ({ id, managerNote }: { id: number; managerNote: string }) => rejectCoverageRequest(id, { managerNote: managerNote.trim() || null }),
    onSuccess: () => {
      setDialog(undefined);
      setMessage('coverageRejectSuccess');
      setError(undefined);
      refreshCoverage();
    },
    onError: (failure) => {
      setDialog(undefined);
      setMessage(undefined);
      setError(errorKey(failure));
      if (isRefreshableManagerError(failure)) refreshManagerCoverage();
    }
  });

  const myPosted = sortCoverage(asArray(mine.data));
  const covering = sortCoverage(dedupeById(asArray(involvingMe.data).filter((request) => !myPosted.some((posted) => posted.id === request.id))));
  const loading = profile.isLoading || employees.isLoading || pool.isLoading || mine.isLoading || involvingMe.isLoading;
  const loadError = profile.isError || employees.isError || pool.isError || mine.isError || involvingMe.isError;

  return (
    <AppScreen title={t('mainNavShift')} subtitle={t('coverageResponsibilityNotice')} activeMainTab="shift">
      <SectionBanner label={t('coverageTitle')} tone="green" />
      <TabNav active={tab} setActive={setTab} isBusinessPartner={isBusinessPartner} />
      {message ? <InlineMessage type="success" message={t(message)} /> : null}
      {error ? <InlineMessage type="error" message={t(error)} /> : null}
      {loadError ? (
        <PaperCard tone="danger">
          <Text style={styles.errorText}>{t('coverageLoadError')}</Text>
          <SecondaryButton label={t('retry')} onPress={refreshCoverage} />
        </PaperCard>
      ) : null}
      {loading ? <EmptyState message={t('loading')} /> : null}
      {!loading && tab === 'new' ? (
        <NewRequestForm
          form={form}
          setForm={setForm}
          replacementOptions={replacementOptions}
          submitting={createMutation.isPending}
          onSubmit={() => createMutation.mutate()}
        />
      ) : null}
      {!loading && tab === 'pool' ? (
        <ShiftPool
          requests={asArray(pool.data)}
          currentEmployeeId={currentEmployeeId}
          claiming={claimMutation.isPending}
          onClaim={(request) => setDialog({ type: 'claim', request })}
          onRefresh={refreshCoverage}
        />
      ) : null}
      {!loading && tab === 'mine' ? (
        <MyRequestsView
          posted={myPosted}
          covering={covering}
          currentEmployeeId={currentEmployeeId}
          cancelling={cancelMutation.isPending}
          onCancel={(request) => setDialog({ type: 'cancel', request })}
          onRefresh={refreshCoverage}
        />
      ) : null}
      {!loading && tab === 'pending' ? (
        <PendingApprovalView
          requests={sortCoverage(asArray(pending.data)).filter((request) => request.status === 'PENDING_APPROVAL')}
          loading={pending.isLoading}
          error={pending.error}
          managerNotes={managerNotes}
          setManagerNote={(id, managerNote) => setManagerNotes((current) => ({ ...current, [id]: managerNote }))}
          approving={approveMutation.isPending}
          rejecting={rejectMutation.isPending}
          onApprove={(request) => setDialog({ type: 'approve', request })}
          onReject={(request) => setDialog({ type: 'reject', request })}
          onRefresh={refreshManagerCoverage}
        />
      ) : null}
      {!loading && tab === 'upcoming' ? (
        <UpcomingCoverageView
          requests={sortCoverage(asArray(upcoming.data)).filter((request) => request.status === 'APPROVED')}
          loading={upcoming.isLoading}
          error={upcoming.error}
          onRefresh={refreshManagerCoverage}
        />
      ) : null}
      {!loading && tab === 'history' ? (
        <CoverageHistoryView
          requests={sortHistory(asArray(history.data).filter((request) => request.status !== 'PENDING_APPROVAL'))}
          loading={history.isLoading}
          error={history.error}
          onRefresh={refreshManagerCoverage}
        />
      ) : null}
      <ConfirmDialog
        visible={Boolean(dialog)}
        title={dialogTitle(dialog?.type, t)}
        message={dialogMessage(dialog?.type, t)}
        onCancel={() => setDialog(undefined)}
        actions={dialog ? [
          { label: t('cancel'), variant: 'secondary', onPress: () => setDialog(undefined) },
          {
            label: dialogActionLabel(dialog.type, t),
            accessibilityLabel: dialogActionAccessibilityLabel(dialog.type),
            variant: dialog.type === 'reject' || dialog.type === 'cancel' ? 'danger' : 'primary',
            disabled: claimMutation.isPending || cancelMutation.isPending || approveMutation.isPending || rejectMutation.isPending,
            onPress: () => {
              if (dialog.type === 'claim') claimMutation.mutate(dialog.request.id);
              if (dialog.type === 'cancel') cancelMutation.mutate(dialog.request.id);
              if (dialog.type === 'approve') approveMutation.mutate(dialog.request.id);
              if (dialog.type === 'reject') rejectMutation.mutate({ id: dialog.request.id, managerNote: managerNotes[dialog.request.id] ?? '' });
            }
          }
        ] : []}
      />
    </AppScreen>
  );
}

function NewRequestForm({ form, setForm, replacementOptions, submitting, onSubmit }: { form: FormState; setForm: (form: FormState) => void; replacementOptions: EmployeePublic[]; submitting: boolean; onSubmit: () => void }) {
  const { t } = useI18n();
  return (
    <>
      <PaperCard>
        <Text style={styles.cardTitle}>{t('coverageNewRequest')}</Text>
        <Text style={styles.notice}>{t('coverageResponsibilityNotice')}</Text>
        <ChoiceGroup label={t('coverageStore')} options={stores.map((store) => ({ value: store, label: t(storeLabelKey(store)) }))} value={form.store} onChange={(store) => setForm({ ...form, store: store as StoreCode })} />
        <BrandTextInput label={t('coverageShiftDate')} value={form.shiftDate} placeholder="YYYY-MM-DD" onChangeText={(shiftDate) => setForm({ ...form, shiftDate })} />
        <ChoiceGroup label={t('coverageShiftType')} options={shiftTypes.map((type) => ({ value: type, label: t(shiftTypeKey(type)) }))} value={form.shiftType} onChange={(shiftType) => setForm({ ...form, shiftType: shiftType as ShiftType })} />
        <View style={styles.twoColumn}>
          <BrandTextInput label={t('coverageStartTime')} value={form.startTime} placeholder="HH:mm" onChangeText={(startTime) => setForm({ ...form, startTime })} />
          <BrandTextInput label={t('coverageEndTime')} value={form.endTime} placeholder="HH:mm" onChangeText={(endTime) => setForm({ ...form, endTime })} />
        </View>
        <ChoiceGroup label={t('coveragePosition')} options={positions.map((position) => ({ value: position, label: t(positionLabelKey(position)) }))} value={form.position} onChange={(position) => setForm({ ...form, position: position as Position })} />
        <ChoiceGroup label={t('coverageType')} options={[{ value: 'PUBLIC', label: t('coverageTypePublic') }, { value: 'DIRECT', label: t('coverageTypeDirect') }]} value={form.coverageType} onChange={(coverageType) => setForm({ ...form, coverageType: coverageType as CoverageType, replacementEmployeeId: '' })} />
        <Text style={styles.helper}>{form.coverageType === 'PUBLIC' ? t('coveragePublicHelp') : t('coverageDirectHelp')}</Text>
        {form.coverageType === 'DIRECT' ? (
          <ChoiceGroup
            label={t('coverageReplacementEmployee')}
            options={replacementOptions.map((employee) => ({ value: String(employee.id), label: employee.displayName }))}
            value={form.replacementEmployeeId}
            onChange={(replacementEmployeeId) => setForm({ ...form, replacementEmployeeId })}
            emptyLabel={t('coverageNoReplacementEmployees')}
          />
        ) : null}
        <BrandTextInput label={t('coverageReason')} value={form.reason} multiline maxLength={500} onChangeText={(reason) => setForm({ ...form, reason })} />
        <PrimaryButton label={submitting ? t('coverageSubmitting') : t('coverageSubmitRequest')} onPress={onSubmit} disabled={submitting} />
      </PaperCard>
    </>
  );
}

function ShiftPool({ requests, currentEmployeeId, claiming, onClaim, onRefresh }: { requests: CoverageRequest[]; currentEmployeeId?: number; claiming: boolean; onClaim: (request: CoverageRequest) => void; onRefresh: () => void }) {
  const { t } = useI18n();
  const openRequests = requests.filter((request) => request.coverageType === 'PUBLIC' && request.status === 'OPEN');
  return (
    <>
      <SectionBanner label={t('coverageShiftPool')} tone="burgundy" />
      <SecondaryButton label={t('refresh')} onPress={onRefresh} />
      {openRequests.length === 0 ? <EmptyState message={t('coveragePoolEmpty')} /> : null}
      {openRequests.map((request) => {
        const ownRequest = request.requestedBy.id === currentEmployeeId;
        return (
          <CoverageCard key={request.id} request={request} viewerId={currentEmployeeId}>
            {ownRequest ? null : <PrimaryButton label={t('coverageCoverShift')} onPress={() => onClaim(request)} disabled={claiming} />}
          </CoverageCard>
        );
      })}
    </>
  );
}

function MyRequestsView({ posted, covering, currentEmployeeId, cancelling, onCancel, onRefresh }: { posted: CoverageRequest[]; covering: CoverageRequest[]; currentEmployeeId?: number; cancelling: boolean; onCancel: (request: CoverageRequest) => void; onRefresh: () => void }) {
  const { t } = useI18n();
  return (
    <>
      <SectionBanner label={t('coverageMyRequests')} tone="burgundy" />
      <SecondaryButton label={t('refresh')} onPress={onRefresh} />
      <Text style={styles.sectionTitle}>{t('coverageRequestsPosted')}</Text>
      {posted.length === 0 ? <EmptyState message={t('coverageNoPostedRequests')} /> : null}
      {posted.map((request) => (
        <CoverageCard key={request.id} request={request} viewerId={currentEmployeeId}>
          {canCancel(request, currentEmployeeId) ? <SecondaryButton label={t('coverageCancelRequest')} onPress={() => onCancel(request)} disabled={cancelling} /> : null}
        </CoverageCard>
      ))}
      <Text style={styles.sectionTitle}>{t('coverageShiftsCovering')}</Text>
      {covering.length === 0 ? <EmptyState message={t('coverageNoCoveringRequests')} /> : null}
      {covering.map((request) => <CoverageCard key={request.id} request={request} viewerId={currentEmployeeId} />)}
    </>
  );
}

function PendingApprovalView({ requests, loading, error, managerNotes, setManagerNote, approving, rejecting, onApprove, onReject, onRefresh }: { requests: CoverageRequest[]; loading: boolean; error: unknown; managerNotes: Record<number, string>; setManagerNote: (id: number, note: string) => void; approving: boolean; rejecting: boolean; onApprove: (request: CoverageRequest) => void; onReject: (request: CoverageRequest) => void; onRefresh: () => void }) {
  const { t } = useI18n();
  return (
    <>
      <SectionBanner label={t('coveragePendingApproval')} tone="burgundy" />
      <SecondaryButton label={t('refresh')} onPress={onRefresh} />
      {loading ? <EmptyState message={t('loading')} /> : null}
      {error ? <ManagerErrorState error={error} onRefresh={onRefresh} /> : null}
      {!loading && !error && requests.length === 0 ? <EmptyState message={t('coveragePendingEmpty')} /> : null}
      {!loading && !error ? requests.map((request) => (
        <CoverageCard key={request.id} request={request}>
          <BrandTextInput
            label={t('coverageManagerNote')}
            value={managerNotes[request.id] ?? ''}
            multiline
            maxLength={500}
            onChangeText={(value) => setManagerNote(request.id, value)}
          />
          <PrimaryButton label={approving ? t('loading') : t('coverageApprove')} onPress={() => onApprove(request)} disabled={approving || rejecting} />
          <SecondaryButton label={rejecting ? t('loading') : t('coverageReject')} onPress={() => onReject(request)} disabled={approving || rejecting} />
        </CoverageCard>
      )) : null}
    </>
  );
}

function UpcomingCoverageView({ requests, loading, error, onRefresh }: { requests: CoverageRequest[]; loading: boolean; error: unknown; onRefresh: () => void }) {
  const { t } = useI18n();
  return (
    <>
      <SectionBanner label={t('coverageUpcoming14Days')} tone="burgundy" />
      <SecondaryButton label={t('refresh')} onPress={onRefresh} />
      {loading ? <EmptyState message={t('loading')} /> : null}
      {error ? <ManagerErrorState error={error} onRefresh={onRefresh} /> : null}
      {!loading && !error && requests.length === 0 ? <EmptyState message={t('coverageUpcomingEmpty')} /> : null}
      {!loading && !error ? requests.map((request) => (
        <CoverageCard key={request.id} request={request}>
          <ReplacementFlow request={request} />
          <ReviewMetadata request={request} />
        </CoverageCard>
      )) : null}
    </>
  );
}

function CoverageHistoryView({ requests, loading, error, onRefresh }: { requests: CoverageRequest[]; loading: boolean; error: unknown; onRefresh: () => void }) {
  const { t } = useI18n();
  return (
    <>
      <SectionBanner label={t('coverageHistory')} tone="burgundy" />
      <SecondaryButton label={t('refresh')} onPress={onRefresh} />
      {loading ? <EmptyState message={t('loading')} /> : null}
      {error ? <ManagerErrorState error={error} onRefresh={onRefresh} /> : null}
      {!loading && !error && requests.length === 0 ? <EmptyState message={t('coverageHistoryEmpty')} /> : null}
      {!loading && !error ? requests.map((request) => (
        <CoverageCard key={request.id} request={request}>
          <ReplacementFlow request={request} />
          <ReviewMetadata request={request} />
        </CoverageCard>
      )) : null}
    </>
  );
}

function ReplacementFlow({ request }: { request: CoverageRequest }) {
  const { t } = useI18n();
  return <LabelValue label={t('coverageChange')} value={`${request.requestedBy.displayName} → ${request.replacementEmployee?.displayName ?? t('coverageClaimed')}`} />;
}

function ReviewMetadata({ request }: { request: CoverageRequest }) {
  const { t } = useI18n();
  return (
    <>
      {request.approvedBy ? <LabelValue label={t('coverageApprovedBy')} value={request.approvedBy.displayName} /> : null}
      {request.approvedAt ? <LabelValue label={t('coverageApprovedAt')} value={formatDateTime(request.approvedAt)} /> : null}
      {request.reviewedBy && !request.approvedBy ? <LabelValue label={t('coverageReviewedBy')} value={request.reviewedBy.displayName} /> : null}
      {request.reviewedAt && !request.approvedAt ? <LabelValue label={t('coverageReviewedAt')} value={formatDateTime(request.reviewedAt)} /> : null}
    </>
  );
}

function ManagerErrorState({ error, onRefresh }: { error: unknown; onRefresh: () => void }) {
  const { t } = useI18n();
  return (
    <PaperCard tone="danger">
      <Text style={styles.errorText}>{t(managerErrorKey(error))}</Text>
      <SecondaryButton label={t('retry')} onPress={onRefresh} />
    </PaperCard>
  );
}

function CoverageCard({ request, viewerId, children }: { request: CoverageRequest; viewerId?: number; children?: React.ReactNode }) {
  const { t } = useI18n();
  const replacement = request.replacementEmployee?.displayName ?? (request.replacementAssigned ? t('coverageClaimed') : t('empty'));
  return (
    <PaperCard>
      <View style={styles.cardHeader}>
        <Text style={styles.cardTitle}>{formatDate(request.shiftDate)}</Text>
        <StatusBadge label={t(statusKey(request.status))} tone={statusTone(request.status)} />
      </View>
      <LabelValue label={t('coverageStore')} value={t(storeLabelKey(request.store))} />
      <LabelValue label={t('coverageShift')} value={[t(shiftTypeKey(request.shiftType)), timeRange(request)].filter(Boolean).join(' · ')} />
      <LabelValue label={t('coveragePosition')} value={t(positionLabelKey(request.position))} />
      <LabelValue label={t('coverageType')} value={t(request.coverageType === 'PUBLIC' ? 'coverageTypePublic' : 'coverageTypeDirect')} />
      <LabelValue label={t('coverageOriginalEmployee')} value={request.requestedBy.displayName} />
      <LabelValue label={t('coverageReplacementEmployee')} value={replacement} />
      {request.reason ? <LabelValue label={t('coverageReason')} value={request.reason} /> : null}
      {request.managerNote ? <LabelValue label={t('coverageManagerNote')} value={request.managerNote} /> : null}
      <LabelValue label={t('coveragePosted')} value={formatDateTime(request.createdAt)} />
      {request.requestedBy.id === viewerId ? <StatusBadge label={t('coverageYourRequest')} tone="paper" /> : null}
      {children}
    </PaperCard>
  );
}

function TabNav({ active, setActive, isBusinessPartner }: { active: CoverageTab; setActive: (tab: CoverageTab) => void; isBusinessPartner: boolean }) {
  const { t } = useI18n();
  const tabs: Array<{ key: CoverageTab; label: TranslationKey }> = [
    { key: 'new', label: 'coverageNewRequest' },
    { key: 'pool', label: 'coverageShiftPool' },
    { key: 'mine', label: 'coverageMyRequests' }
  ];
  if (isBusinessPartner) {
    tabs.push(
      { key: 'pending', label: 'coveragePendingApproval' },
      { key: 'upcoming', label: 'coverageUpcoming14Days' },
      { key: 'history', label: 'coverageHistory' }
    );
  }
  return (
    <View style={styles.tabWrap}>
      {tabs.map((tab) => (
        <Pressable key={tab.key} accessibilityRole="button" accessibilityLabel={t(tab.label)} onPress={() => setActive(tab.key)} style={[styles.tabChip, active === tab.key ? styles.tabChipActive : null]}>
          <Text style={[styles.tabText, active === tab.key ? styles.tabTextActive : null]}>{t(tab.label)}</Text>
        </Pressable>
      ))}
    </View>
  );
}

function ChoiceGroup({ label, options, value, onChange, emptyLabel }: { label: string; options: Array<{ value: string; label: string }>; value: string; onChange: (value: string) => void; emptyLabel?: string }) {
  return (
    <View style={styles.choiceSection}>
      <Text style={styles.fieldLabel}>{label}</Text>
      {options.length === 0 ? <Text style={styles.helper}>{emptyLabel}</Text> : null}
      <View style={styles.choiceWrap}>
        {options.map((option) => (
          <Pressable key={option.value} accessibilityRole="button" accessibilityLabel={option.label} onPress={() => onChange(option.value)} style={[styles.choice, value === option.value ? styles.choiceActive : null]}>
            <Text style={[styles.choiceText, value === option.value ? styles.choiceTextActive : null]}>{option.label}</Text>
          </Pressable>
        ))}
      </View>
    </View>
  );
}

function validateForm(form: FormState): TranslationKey | 'COVERAGE_FRONTEND_VALIDATION' | undefined {
  if (!form.shiftDate || !/^\d{4}-\d{2}-\d{2}$/.test(form.shiftDate)) return 'coverageDateRequired';
  if (form.shiftDate < today()) return 'coveragePastDateError';
  if ((form.startTime && !isTime(form.startTime)) || (form.endTime && !isTime(form.endTime))) return 'coverageTimeFormatError';
  if (form.startTime && form.endTime && form.endTime <= form.startTime) return 'coverageEndAfterStartError';
  if (form.coverageType === 'DIRECT' && !form.replacementEmployeeId) return 'coverageReplacementRequired';
  return undefined;
}

function errorKey(error: unknown): TranslationKey {
  if (isApiErrorLike(error)) {
    const keys: Record<string, TranslationKey> = {
      COVERAGE_DATE_REQUIRED: 'coverageDateRequired',
      coverageDateRequired: 'coverageDateRequired',
      coveragePastDateError: 'coveragePastDateError',
      coverageEndAfterStartError: 'coverageEndAfterStartError',
      coverageTimeFormatError: 'coverageTimeFormatError',
      coverageReplacementRequired: 'coverageReplacementRequired',
      COVERAGE_REPLACEMENT_REQUIRED: 'coverageReplacementRequired',
      COVERAGE_REPLACEMENT_ACTIVE_EMPLOYEE_REQUIRED: 'coverageReplacementActiveRequired',
      COVERAGE_REPLACEMENT_CANNOT_BE_SELF: 'coverageReplacementSelfError',
      COVERAGE_PUBLIC_REPLACEMENT_NOT_ALLOWED: 'coveragePublicReplacementError',
      COVERAGE_CANNOT_CLAIM_OWN_REQUEST: 'coverageClaimOwnError',
      COVERAGE_REQUEST_ALREADY_CLAIMED: 'coverageAlreadyClaimedError',
      COVERAGE_REQUEST_NOT_CLAIMABLE: 'coverageNotClaimableError',
      COVERAGE_REQUEST_NOT_CANCELLABLE: 'coverageNotCancellableError',
      COVERAGE_REQUEST_NOT_PENDING_APPROVAL: 'coverageNotPendingError',
      COVERAGE_REQUEST_NOT_FOUND: 'coverageNotFoundError',
      COVERAGE_CANCEL_OWN_REQUEST_REQUIRED: 'coverageCancelOwnError',
      COVERAGE_END_TIME_MUST_BE_AFTER_START_TIME: 'coverageEndAfterStartError',
      AUTH_BUSINESS_PARTNER_REQUIRED: 'coverageManagerPermissionError',
      AUTH_ACTIVE_EMPLOYEE_REQUIRED: 'coverageActiveEmployeeRequired',
      AUTH_INVALID_TOKEN: 'coverageAuthExpiredError',
      VALIDATION_FAILED: 'coverageValidationError'
    };
    return keys[error.code] ?? 'coverageApiError';
  }
  return 'coverageApiError';
}

function managerErrorKey(error: unknown): TranslationKey {
  if (isApiErrorLike(error)) {
    if (error.status === 403 || error.code === 'AUTH_BUSINESS_PARTNER_REQUIRED') return 'coverageManagerPermissionError';
    if (error.code === 'AUTH_INVALID_TOKEN') return 'coverageAuthExpiredError';
    if (error.status === 404 || error.code === 'COVERAGE_REQUEST_NOT_FOUND') return 'coverageNotFoundError';
    if (error.status === 409 || error.code === 'COVERAGE_REQUEST_NOT_PENDING_APPROVAL') return 'coverageNotPendingError';
  }
  return errorKey(error);
}

function isRefreshableManagerError(error: unknown) {
  return isApiErrorLike(error) && [404, 409].includes(error.status ?? 0);
}

function isAuthInvalidError(error: unknown) {
  return isApiErrorLike(error) && error.code === 'AUTH_INVALID_TOKEN';
}

function isApiErrorLike(error: unknown): error is { code: string; status?: number } {
  return Boolean(error && typeof error === 'object' && 'code' in error && typeof (error as { code?: unknown }).code === 'string');
}

function shiftTypeKey(type: ShiftType): TranslationKey {
  return ({ LUNCH: 'coverageShiftLunch', DINNER: 'coverageShiftDinner', DOUBLE: 'coverageShiftDouble' } as const)[type];
}

function statusKey(status: CoverageStatus): TranslationKey {
  return ({
    OPEN: 'coverageStatusOpen',
    PENDING_APPROVAL: 'coverageStatusPending',
    APPROVED: 'coverageStatusApproved',
    REJECTED: 'coverageStatusRejected',
    CANCELLED: 'coverageStatusCancelled'
  } as const)[status];
}

function statusTone(status: CoverageStatus): 'green' | 'burgundy' | 'paper' | 'danger' {
  if (status === 'APPROVED') return 'green';
  if (status === 'REJECTED') return 'danger';
  if (status === 'CANCELLED') return 'paper';
  return 'burgundy';
}

function canCancel(request: CoverageRequest, currentEmployeeId?: number) {
  return request.requestedBy.id === currentEmployeeId && (request.status === 'OPEN' || request.status === 'PENDING_APPROVAL');
}

function isCoverageManager(employee?: EmployeePrivate) {
  return employee?.positions?.some((position) => ['OWNER', 'MANAGER', 'FINANCIAL_MANAGER'].includes(position)) ?? false;
}

function sortCoverage(requests: CoverageRequest[]) {
  return [...requests].sort((left, right) => {
    const leftRank = ['OPEN', 'PENDING_APPROVAL'].includes(left.status) ? 0 : 1;
    const rightRank = ['OPEN', 'PENDING_APPROVAL'].includes(right.status) ? 0 : 1;
    if (leftRank !== rightRank) return leftRank - rightRank;
    const dateComparison = left.shiftDate.localeCompare(right.shiftDate);
    return dateComparison !== 0 ? dateComparison : right.id - left.id;
  });
}

function sortHistory(requests: CoverageRequest[]) {
  return [...requests].sort((left, right) => processedAt(right).localeCompare(processedAt(left)) || right.id - left.id);
}

function processedAt(request: CoverageRequest) {
  return request.reviewedAt ?? request.approvedAt ?? request.updatedAt ?? request.createdAt ?? '';
}

function dedupeById(requests: CoverageRequest[]) {
  return Array.from(new Map(requests.map((request) => [request.id, request])).values());
}

function asArray<T>(value?: T[]) {
  return Array.isArray(value) ? value : [];
}

function today() {
  return new Date().toISOString().slice(0, 10);
}

function isTime(value: string) {
  return /^([01]\d|2[0-3]):[0-5]\d$/.test(value);
}

function timeRange(request: CoverageRequest) {
  if (!request.startTime && !request.endTime) return '';
  return [formatTime(request.startTime), formatTime(request.endTime)].filter(Boolean).join('–');
}

function formatTime(value?: string | null) {
  return value ? value.slice(0, 5) : '';
}

function formatDate(value: string) {
  return value;
}

function formatDateTime(value: string) {
  return value ? value.replace('T', ' ').slice(0, 16) : '';
}

function dialogTitle(type: PendingDialog['type'] | undefined, t: (key: TranslationKey) => string) {
  if (type === 'claim') return t('coverageClaimConfirmTitle');
  if (type === 'cancel') return t('coverageCancelConfirmTitle');
  if (type === 'approve') return t('coverageApproveConfirmTitle');
  if (type === 'reject') return t('coverageRejectConfirmTitle');
  return '';
}

function dialogMessage(type: PendingDialog['type'] | undefined, t: (key: TranslationKey) => string) {
  if (type === 'claim') return t('coverageClaimConfirmMessage');
  if (type === 'cancel') return t('coverageCancelConfirmMessage');
  if (type === 'approve') return t('coverageApproveConfirmMessage');
  if (type === 'reject') return t('coverageRejectConfirmMessage');
  return '';
}

function dialogActionLabel(type: PendingDialog['type'], t: (key: TranslationKey) => string) {
  if (type === 'claim') return t('coverageCoverShift');
  if (type === 'cancel') return t('coverageCancelRequest');
  if (type === 'approve') return t('coverageApprove');
  return t('coverageReject');
}

function dialogActionAccessibilityLabel(type: PendingDialog['type']) {
  if (type === 'claim') return 'Confirm cover shift';
  if (type === 'cancel') return 'Confirm cancel coverage request';
  if (type === 'approve') return 'Confirm approve coverage request';
  return 'Confirm reject coverage request';
}

const styles = StyleSheet.create({
  tabWrap: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: spacing.sm
  },
  tabChip: {
    borderWidth: 1,
    borderColor: colors.border,
    borderRadius: 6,
    paddingVertical: spacing.sm,
    paddingHorizontal: spacing.md,
    backgroundColor: colors.paperLight
  },
  tabChipActive: {
    backgroundColor: colors.forestGreen,
    borderColor: colors.forestGreen
  },
  tabText: {
    color: colors.ink,
    fontFamily: typography.interface,
    fontWeight: '800'
  },
  tabTextActive: {
    color: colors.white
  },
  cardTitle: {
    color: colors.ink,
    fontFamily: typography.display,
    fontSize: 20,
    fontWeight: '800'
  },
  cardHeader: {
    gap: spacing.sm
  },
  notice: {
    color: colors.burgundy,
    fontFamily: typography.interface,
    fontWeight: '800'
  },
  helper: {
    color: colors.mutedInk,
    fontFamily: typography.interface,
    fontSize: 14
  },
  fieldLabel: {
    color: colors.ink,
    fontFamily: typography.interface,
    fontWeight: '800'
  },
  choiceSection: {
    gap: spacing.sm
  },
  choiceWrap: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: spacing.sm
  },
  choice: {
    borderWidth: 1,
    borderColor: colors.border,
    borderRadius: 6,
    backgroundColor: colors.paperLight,
    paddingHorizontal: spacing.md,
    paddingVertical: spacing.sm
  },
  choiceActive: {
    backgroundColor: colors.burgundy,
    borderColor: colors.burgundy
  },
  choiceText: {
    color: colors.ink,
    fontFamily: typography.interface,
    fontWeight: '800'
  },
  choiceTextActive: {
    color: colors.white
  },
  twoColumn: {
    gap: spacing.md
  },
  sectionTitle: {
    color: colors.ink,
    fontFamily: typography.display,
    fontSize: 20,
    fontWeight: '800'
  },
  errorText: {
    color: colors.error,
    fontFamily: typography.interface,
    fontWeight: '800'
  }
});
