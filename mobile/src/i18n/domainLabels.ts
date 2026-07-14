import { Invitation, Position, StoreCode } from '../types/domain';
import { TranslationKey } from './translations';

const storeKeys: Record<StoreCode, TranslationKey> = {
  SEATTLE: 'storeSeattle',
  REDMOND: 'storeRedmond'
};

const positionKeys: Record<Position, TranslationKey> = {
  OWNER: 'positionOwner',
  FINANCIAL_MANAGER: 'positionFinancialManager',
  MANAGER: 'positionManager',
  FOOD_RUNNER: 'positionFoodRunner',
  HOST: 'positionHost',
  BARTENDER: 'positionBartender',
  SERVER_ONE_STAR: 'positionServerOneStar',
  SERVER_TWO_STAR: 'positionServerTwoStar',
  SHIFT_LEADER: 'positionShiftLeader'
};

export function storeLabelKey(store: StoreCode): TranslationKey {
  return storeKeys[store];
}

export function positionLabelKey(position: Position): TranslationKey {
  return positionKeys[position];
}

const invitationStatusKeys: Record<Invitation['status'], TranslationKey> = {
  ACTIVE: 'invitationActive',
  USED: 'invitationUsed',
  EXPIRED: 'invitationExpired',
  REVOKED: 'invitationRevoked',
  SUPERSEDED: 'invitationSuperseded'
};

export function invitationStatusLabelKey(status: Invitation['status']): TranslationKey {
  return invitationStatusKeys[status];
}
