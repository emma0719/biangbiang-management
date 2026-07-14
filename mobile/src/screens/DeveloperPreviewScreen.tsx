import React, { useMemo, useState } from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';
import { Field } from '../components/Field';
import {
  AppScreen,
  DangerButton,
  EmployeeAvatar,
  ErrorState,
  LabelValue,
  PaperCard,
  PrimaryButton,
  SecondaryButton,
  SectionBanner,
  StatusBadge
} from '../components/designSystem';
import { invitationStatusLabelKey, positionLabelKey, storeLabelKey } from '../i18n/domainLabels';
import { TranslationKey } from '../i18n/translations';
import { useI18n } from '../i18n/I18nProvider';
import { colors, spacing, typography } from '../theme/theme';
import { EmployeePrivate, Invitation, Position, StoreCode } from '../types/domain';

type PreviewId =
  | 'login'
  | 'activation'
  | 'invalidInvitation'
  | 'activationSuccess'
  | 'forgotPassword'
  | 'profile'
  | 'editProfile'
  | 'stores'
  | 'toastPin'
  | 'emailRequest'
  | 'emailVerify'
  | 'phoneRequest'
  | 'phoneVerify'
  | 'profilePhoto'
  | 'security'
  | 'language'
  | 'managerCreateInvitation'
  | 'managerInvitationHistory'
  | 'managerEmployeeList'
  | 'managerEmployeeDetail';

const previewSections: { title: string; tone: 'burgundy' | 'green'; items: { id: PreviewId; labelKey: TranslationKey }[] }[] = [
  {
    title: 'AUTHENTICATION',
    tone: 'burgundy',
    items: [
      { id: 'login', labelKey: 'previewLogin' },
      { id: 'activation', labelKey: 'previewInvitationActivation' },
      { id: 'invalidInvitation', labelKey: 'previewInvalidInvitation' },
      { id: 'activationSuccess', labelKey: 'previewActivationSuccess' },
      { id: 'forgotPassword', labelKey: 'previewForgotPassword' }
    ]
  },
  {
    title: 'PROFILE',
    tone: 'green',
    items: [
      { id: 'profile', labelKey: 'previewProfile' },
      { id: 'editProfile', labelKey: 'previewEditProfile' },
      { id: 'stores', labelKey: 'previewEligibleStores' },
      { id: 'toastPin', labelKey: 'previewChangeToastPin' },
      { id: 'emailRequest', labelKey: 'previewChangeEmailRequest' },
      { id: 'emailVerify', labelKey: 'previewChangeEmailVerification' },
      { id: 'phoneRequest', labelKey: 'previewChangePhoneRequest' },
      { id: 'phoneVerify', labelKey: 'previewChangePhoneVerification' },
      { id: 'profilePhoto', labelKey: 'previewProfilePhoto' }
    ]
  },
  {
    title: 'MANAGEMENT',
    tone: 'burgundy',
    items: [
      { id: 'managerCreateInvitation', labelKey: 'previewManagerCreateInvitation' },
      { id: 'managerInvitationHistory', labelKey: 'previewManagerInvitationHistory' },
      { id: 'managerEmployeeList', labelKey: 'previewManagerEmployeeList' },
      { id: 'managerEmployeeDetail', labelKey: 'previewManagerEmployeeDetail' }
    ]
  },
  {
    title: 'SYSTEM STATES',
    tone: 'green',
    items: [
      { id: 'security', labelKey: 'previewSecuritySettings' },
      { id: 'language', labelKey: 'previewLanguageSettings' }
    ]
  }
];

const previewItems = previewSections.flatMap((section) => section.items);

const stores: StoreCode[] = ['SEATTLE', 'REDMOND'];
const positions: Position[] = ['SERVER_TWO_STAR', 'SHIFT_LEADER', 'HOST', 'BARTENDER', 'FOOD_RUNNER'];

const mockProfile: EmployeePrivate = {
  id: 104,
  englishName: 'Emma',
  preferredName: 'Emma',
  displayName: 'Emma',
  email: 'emma@dev.example.com',
  phone: '+12065551104',
  homeStore: 'SEATTLE',
  eligibleStores: ['SEATTLE', 'REDMOND'],
  positions,
  status: 'ACTIVE',
  toastPin: '1104',
  lastLoginAt: '2026-06-15T08:42:00-07:00',
  createdAt: '2026-05-20T17:10:00-07:00'
};

const mockEmployees: EmployeePrivate[] = [
  mockEmployee(101, 'Alex', ['MANAGER']),
  mockEmployee(102, 'Mini', ['MANAGER']),
  mockEmployee(103, 'Annie', ['SERVER_TWO_STAR', 'SHIFT_LEADER']),
  mockProfile,
  mockEmployee(105, 'Hannah', ['SERVER_TWO_STAR', 'SHIFT_LEADER', 'FOOD_RUNNER', 'HOST']),
  mockEmployee(106, 'David', ['SERVER_TWO_STAR', 'SHIFT_LEADER', 'HOST', 'BARTENDER', 'FOOD_RUNNER']),
  mockEmployee(107, 'Amy', ['SERVER_ONE_STAR', 'BARTENDER']),
  mockEmployee(108, 'Julie', ['SERVER_ONE_STAR']),
  mockEmployee(109, 'Tony', ['SERVER_ONE_STAR', 'BARTENDER']),
  mockEmployee(110, 'Ethan', ['BARTENDER', 'FOOD_RUNNER']),
  mockEmployee(111, 'Mary', ['HOST']),
  mockEmployee(112, 'Sela', ['FOOD_RUNNER']),
  mockEmployee(113, 'Alyssa', ['FOOD_RUNNER']),
  mockEmployee(114, 'Bianca', ['FOOD_RUNNER']),
  mockEmployee(115, 'Tommy', ['FOOD_RUNNER']),
  mockEmployee(116, 'Cystal', ['FOOD_RUNNER']),
  mockEmployee(117, 'Yumi', ['FINANCIAL_MANAGER']),
  mockEmployee(118, 'Alison', ['OWNER']),
  mockEmployee(119, 'Sia', ['OWNER'])
];

function mockEmployee(id: number, name: string, employeePositions: Position[]): EmployeePrivate {
  return {
    id,
    englishName: name,
    preferredName: name,
    displayName: name,
    email: `${name.toLowerCase()}@dev.example.com`,
    phone: `+1206555${String(1000 + id).slice(-4)}`,
    homeStore: 'SEATTLE',
    eligibleStores: ['SEATTLE', 'REDMOND'],
    positions: employeePositions,
    status: 'ACTIVE',
    toastPin: String(1000 + id).slice(-4),
    createdAt: '2026-06-16T09:00:00-07:00'
  };
}

const mockInvitations: Invitation[] = [
  {
    id: 501,
    positions: ['HOST'],
    creatorId: 101,
    createdAt: '2026-06-15T09:00:00-07:00',
    expiresAt: '2026-06-16T09:00:00-07:00',
    tokenVersion: 2,
    status: 'ACTIVE',
    activationLink: 'https://ops.example.test/activate?token=preview-active'
  },
  {
    id: 502,
    positions: ['SERVER_TWO_STAR', 'BARTENDER'],
    creatorId: 101,
    createdAt: '2026-06-13T13:25:00-07:00',
    expiresAt: '2026-06-14T13:25:00-07:00',
    usedAt: '2026-06-13T18:05:00-07:00',
    tokenVersion: 1,
    status: 'USED'
  },
  {
    id: 503,
    positions: ['FOOD_RUNNER'],
    creatorId: 101,
    createdAt: '2026-06-10T10:30:00-07:00',
    expiresAt: '2026-06-11T10:30:00-07:00',
    revokedAt: '2026-06-10T15:00:00-07:00',
    tokenVersion: 1,
    status: 'REVOKED'
  }
];

export function DeveloperPreviewScreen() {
  const { t, locale, setLocale } = useI18n();
  const [selected, setSelected] = useState<PreviewId>('login');
  const activeLabel = useMemo(() => previewItems.find((item) => item.id === selected)?.labelKey ?? 'previewLogin', [selected]);

  if (!__DEV__) {
    return <ErrorState message={t('networkError')} />;
  }

  return (
    <AppScreen title={t('developerPreview')} subtitle={t('previewBanner')} maxWidth={860}>
      <PaperCard tone="danger">
        <StatusBadge label={t('previewNotProduction')} tone="danger" />
        <Text style={styles.previewNotice}>{t('previewNoApiCalls')}</Text>
        <PrimaryButton label={t(locale === 'en' ? 'previewSwitchToSpanish' : 'previewSwitchToEnglish')} onPress={() => setLocale(locale === 'en' ? 'es' : 'en')} />
      </PaperCard>
      <Text style={styles.previewChoose}>{t('previewChooseScreen')}</Text>
      {previewSections.map((section) => (
        <View key={section.title} style={styles.previewSection}>
          <SectionBanner label={section.title} tone={section.tone} />
          <View style={styles.previewGrid}>
            {section.items.map((item) => (
              <Pressable
                key={item.id}
                accessibilityRole="button"
                accessibilityLabel={t(item.labelKey)}
                onPress={() => setSelected(item.id)}
                style={({ pressed }) => [styles.previewChip, selected === item.id ? styles.previewChipActive : null, pressed ? styles.previewChipPressed : null]}
              >
                <Text style={[styles.previewChipText, selected === item.id ? styles.previewChipTextActive : null]}>{t(item.labelKey)}</Text>
              </Pressable>
            ))}
          </View>
        </View>
      ))}
      <PaperCard>
        <Text style={styles.panelTitle}>{t(activeLabel)}</Text>
        <PreviewPanel selected={selected} />
      </PaperCard>
    </AppScreen>
  );
}

function PreviewPanel({ selected }: { selected: PreviewId }) {
  switch (selected) {
    case 'login':
      return <LoginPreview />;
    case 'activation':
      return <ActivationPreview />;
    case 'invalidInvitation':
      return <MessagePreview titleKey="previewInvalidInvitation" bodyKey="expiredInvitation" />;
    case 'activationSuccess':
      return <MessagePreview titleKey="previewActivationSuccess" bodyKey="activationSuccess" />;
    case 'forgotPassword':
      return <ForgotPasswordPreview />;
    case 'profile':
      return <ProfilePreview />;
    case 'editProfile':
      return <EditProfilePreview />;
    case 'stores':
      return <StoresPreview />;
    case 'toastPin':
      return <ToastPinPreview />;
    case 'emailRequest':
      return <ContactRequestPreview kind="email" />;
    case 'emailVerify':
      return <ContactVerifyPreview kind="email" />;
    case 'phoneRequest':
      return <ContactRequestPreview kind="phone" />;
    case 'phoneVerify':
      return <ContactVerifyPreview kind="phone" />;
    case 'profilePhoto':
      return <ProfilePhotoPreview />;
    case 'security':
      return <SecurityPreview />;
    case 'language':
      return <LanguagePreview />;
    case 'managerCreateInvitation':
      return <ManagerCreateInvitationPreview />;
    case 'managerInvitationHistory':
      return <ManagerInvitationHistoryPreview />;
    case 'managerEmployeeList':
      return <ManagerEmployeeListPreview />;
    case 'managerEmployeeDetail':
      return <ManagerEmployeeDetailPreview />;
  }
}

function LoginPreview() {
  const { t } = useI18n();
  return (
    <Panel>
      <Field label={t('identifier')} value="maya.chen@example.test" editable={false} />
      <Field label={t('password')} value="preview-password" secureTextEntry editable={false} />
      <PrimaryButton label={t('submit')} onPress={() => undefined} />
      <Text>{t('forgotPassword')}</Text>
    </Panel>
  );
}

function ActivationPreview() {
  const { t } = useI18n();
  return (
    <Panel>
      <Text>{t('positions')}: {positions.map((position) => t(positionLabelKey(position))).join(', ')}</Text>
      <Field label={t('englishName')} value="Avery Morgan" editable={false} />
      <Field label={t('preferredName')} value="Avery" editable={false} />
      <Field label={t('email')} value="avery.morgan@example.test" editable={false} />
      <Field label={t('phone')} value="+12065550192" editable={false} />
      <Field label={t('password')} value="preview-password" secureTextEntry editable={false} />
      <Field label={t('toastPin')} value="6142" editable={false} />
      <Text>{t('homeStore')}: {t(storeLabelKey('SEATTLE'))}</Text>
      <Text>{t('eligibleStores')}: {stores.map((store) => t(storeLabelKey(store))).join(', ')}</Text>
      <PrimaryButton label={t('submit')} onPress={() => undefined} />
    </Panel>
  );
}

function MessagePreview({ titleKey, bodyKey }: { titleKey: TranslationKey; bodyKey: TranslationKey }) {
  const { t } = useI18n();
  return (
    <Panel>
      <Text style={styles.messageTitle}>{t(titleKey)}</Text>
      <Text>{t(bodyKey)}</Text>
    </Panel>
  );
}

function ForgotPasswordPreview() {
  const { t } = useI18n();
  return (
    <Panel>
      <Field label={t('email')} value="maya.chen@example.test" editable={false} />
      <PrimaryButton label={t('requestChange')} onPress={() => undefined} />
      <Text>{t('previewVerificationCode')}: 384921</Text>
    </Panel>
  );
}

function ProfilePreview() {
  const { t } = useI18n();
  return (
    <Panel>
      <EmployeeSummary employee={mockProfile} showPrivate />
      <Text>{t('previewLastLogin')}: {mockProfile.lastLoginAt}</Text>
      <Text>{t('previewAuthenticationMetadata')}: {t('previewSessionMetadataValue')}</Text>
    </Panel>
  );
}

function EditProfilePreview() {
  const { t } = useI18n();
  return (
    <Panel>
      <Field label={t('englishName')} value={mockProfile.englishName} editable={false} />
      <Field label={t('preferredName')} value={mockProfile.preferredName} editable={false} />
      <PrimaryButton label={t('save')} onPress={() => undefined} />
    </Panel>
  );
}

function StoresPreview() {
  const { t } = useI18n();
  return (
    <Panel>
      <Text>{t('homeStore')}: {t(storeLabelKey(mockProfile.homeStore))}</Text>
      <Text>{t('readOnlyHomeStore')}</Text>
      <Text>{t('eligibleStores')}: {mockProfile.eligibleStores.map((store) => t(storeLabelKey(store))).join(', ')}</Text>
      <PrimaryButton label={t('save')} onPress={() => undefined} />
    </Panel>
  );
}

function ToastPinPreview() {
  const { t } = useI18n();
  return (
    <Panel>
      <Field label={t('toastPin')} value="9137" editable={false} />
      <PrimaryButton label={t('save')} onPress={() => undefined} />
      <Text>{t('toastPinSuccess')}</Text>
    </Panel>
  );
}

function ContactRequestPreview({ kind }: { kind: 'email' | 'phone' }) {
  const { t } = useI18n();
  return (
    <Panel>
      <Field label={t(kind)} value={kind === 'email' ? 'maya.preview@example.test' : '+12065550999'} editable={false} />
      <PrimaryButton label={t('requestChange')} onPress={() => undefined} />
      <Text>{t(kind === 'email' ? 'emailRequestSuccess' : 'phoneRequestSuccess')}</Text>
    </Panel>
  );
}

function ContactVerifyPreview({ kind }: { kind: 'email' | 'phone' }) {
  const { t } = useI18n();
  return (
    <Panel>
      <Field label={t('verificationToken')} value={kind === 'email' ? 'email-384921' : 'phone-927404'} editable={false} />
      <PrimaryButton label={t('verify')} onPress={() => undefined} />
      <Text>{t(kind === 'email' ? 'emailVerifySuccess' : 'phoneVerifySuccess')}</Text>
    </Panel>
  );
}

function ProfilePhotoPreview() {
  const { t } = useI18n();
  return (
    <Panel>
      <EmployeeAvatar name={mockProfile.displayName} />
      <Text>{t('previewPhotoSelected')}</Text>
      <PrimaryButton label={t('selectPhoto')} onPress={() => undefined} />
      <PrimaryButton label={t('uploadPhoto')} onPress={() => undefined} />
      <PrimaryButton label={t('removePhoto')} onPress={() => undefined} />
    </Panel>
  );
}

function SecurityPreview() {
  const { t } = useI18n();
  return (
    <Panel>
      <Text>{t('previewCurrentDevice')}: {t('previewWebPreviewDevice')}</Text>
      <Text>{t('previewAllDevices')}: 3</Text>
      <PrimaryButton label={t('logout')} onPress={() => undefined} />
      <PrimaryButton label={t('logoutAll')} onPress={() => undefined} />
    </Panel>
  );
}

function LanguagePreview() {
  const { t, locale } = useI18n();
  return (
    <Panel>
      <Text>{t('previewSelectedLanguage')}: {t(locale === 'en' ? 'languageEnglish' : 'languageSpanish')}</Text>
      <Text>{t('languageEnglish')} / {t('languageSpanish')}</Text>
    </Panel>
  );
}

function ManagerCreateInvitationPreview() {
  const { t } = useI18n();
  return (
    <Panel>
      <Text>{t('previewMockManager')}: {mockProfile.displayName}</Text>
      <Text>{t('positions')}: {t(positionLabelKey('HOST'))}, {t(positionLabelKey('BARTENDER'))}</Text>
      <Text>{t('homeStore')}: {t(storeLabelKey('SEATTLE'))}</Text>
      <PrimaryButton label={t('createInvitation')} onPress={() => undefined} />
      <Text>{t('previewInvitationLink')}: https://ops.example.test/activate?token=preview-new</Text>
    </Panel>
  );
}

function ManagerInvitationHistoryPreview() {
  const { t } = useI18n();
  return (
    <Panel>
      {mockInvitations.map((invitation) => (
        <View key={invitation.id} style={styles.previewItem}>
          <StatusBadge label={`#${invitation.id} ${t(invitationStatusLabelKey(invitation.status))}`} tone={invitation.status === 'ACTIVE' ? 'green' : invitation.status === 'REVOKED' ? 'danger' : 'paper'} />
          <Text>{t('positions')}: {invitation.positions.map((position) => t(positionLabelKey(position))).join(', ')}</Text>
          <Text>{t('previewInvitationLink')}: {invitation.activationLink ?? t('empty')}</Text>
          <SecondaryButton label={t('regenerate')} onPress={() => undefined} />
          <DangerButton label={t('revoke')} onPress={() => undefined} />
        </View>
      ))}
    </Panel>
  );
}

function ManagerEmployeeListPreview() {
  return (
    <Panel>
      {mockEmployees.map((employee) => <EmployeeSummary key={employee.id} employee={employee} />)}
    </Panel>
  );
}

function ManagerEmployeeDetailPreview() {
  const { t } = useI18n();
  return (
    <Panel>
      <Text style={styles.messageTitle}>{t('previewEmployeeDetail')}</Text>
      <EmployeeSummary employee={mockEmployees[1]} showPrivate />
      <PrimaryButton label={t('forceLogout')} onPress={() => undefined} />
      <DangerButton label={t('deactivate')} onPress={() => undefined} />
    </Panel>
  );
}

function EmployeeSummary({ employee, showPrivate }: { employee: EmployeePrivate; showPrivate?: boolean }) {
  const { t } = useI18n();
  return (
    <View style={[styles.previewItem, employee.status === 'DEACTIVATED' ? styles.previewItemDanger : null]}>
      <View style={styles.employeeHeader}>
        <EmployeeAvatar name={employee.displayName} />
        <View style={styles.employeeHeaderText}>
          <Text style={styles.employeeName}>{employee.displayName}</Text>
          <StatusBadge label={t(employee.status === 'ACTIVE' ? 'statusActive' : 'statusDeactivated')} tone={employee.status === 'ACTIVE' ? 'green' : 'danger'} />
        </View>
      </View>
      <LabelValue label={t('homeStore')} value={t(storeLabelKey(employee.homeStore))} />
      <LabelValue label={t('eligibleStores')} value={employee.eligibleStores.map((store) => t(storeLabelKey(store))).join(', ')} />
      <LabelValue label={t('positions')} value={employee.positions.map((position) => t(positionLabelKey(position))).join(', ')} />
      {showPrivate ? (
        <>
          <LabelValue label={t('email')} value={employee.email} />
          <LabelValue label={t('phone')} value={employee.phone} />
          <LabelValue label={t('toastPin')} value={employee.toastPin ?? ''} />
        </>
      ) : null}
    </View>
  );
}

function Panel({ children }: { children: React.ReactNode }) {
  return <View style={styles.panel}>{children}</View>;
}

const styles = StyleSheet.create({
  previewNotice: {
    color: colors.ink,
    fontFamily: typography.interface,
    fontWeight: '700'
  },
  previewChoose: {
    color: colors.ink,
    fontFamily: typography.display,
    fontSize: 20,
    fontWeight: '800'
  },
  previewSection: {
    gap: spacing.sm
  },
  previewGrid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: spacing.sm
  },
  previewChip: {
    minHeight: 44,
    borderWidth: 1,
    borderColor: colors.border,
    backgroundColor: colors.white,
    borderRadius: 4,
    paddingVertical: spacing.sm,
    paddingHorizontal: spacing.md,
    justifyContent: 'center'
  },
  previewChipActive: {
    borderColor: colors.forestGreen,
    backgroundColor: '#E5F0E9'
  },
  previewChipPressed: {
    opacity: 0.82
  },
  previewChipText: {
    color: colors.ink,
    fontFamily: typography.interface,
    fontWeight: '700'
  },
  previewChipTextActive: {
    color: colors.forestGreenDark,
    fontWeight: '800'
  },
  panelTitle: {
    color: colors.ink,
    fontFamily: typography.display,
    fontSize: 24,
    fontWeight: '800'
  },
  panel: {
    gap: spacing.md
  },
  messageTitle: {
    color: colors.ink,
    fontFamily: typography.display,
    fontSize: 20,
    fontWeight: '800'
  },
  employeeHeader: {
    flexDirection: 'row',
    gap: spacing.md,
    alignItems: 'center'
  },
  employeeHeaderText: {
    flex: 1,
    gap: spacing.xs
  },
  employeeName: {
    color: colors.ink,
    fontFamily: typography.display,
    fontSize: 19,
    fontWeight: '800'
  },
  previewItem: {
    borderWidth: 1,
    borderColor: colors.border,
    borderRadius: 6,
    backgroundColor: colors.white,
    padding: spacing.md,
    gap: spacing.md
  },
  previewItemDanger: {
    borderColor: colors.error,
    backgroundColor: '#F7E4D8'
  }
});
