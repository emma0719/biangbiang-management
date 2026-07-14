import { translations } from '../src/i18n/translations';

describe('mobile localization and validation fixtures', () => {
  it('renders English and Spanish strings for auth and manager flows', () => {
    expect(translations.en.loginTitle).toBe('Sign in');
    expect(translations.es.loginTitle).toBe('Iniciar sesion');
    expect(translations.en.logoutAll).toBeTruthy();
    expect(translations.es.createInvitation).toBeTruthy();
    expect(translations.en.storeSeattle).toBe('Seattle');
    expect(translations.es.positionShiftLeader).toBeTruthy();
  });

  it('keeps duplicate-field rendering translatable', () => {
    expect(translations.en.duplicateField).toContain('already');
    expect(translations.es.duplicateField).toContain('uso');
  });
});
