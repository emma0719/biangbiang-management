import { api } from './client';
import { EmployeePublic, Position, StoreCode } from '../types/domain';

export type CoverageType = 'PUBLIC' | 'DIRECT';
export type CoverageStatus = 'OPEN' | 'PENDING_APPROVAL' | 'APPROVED' | 'REJECTED' | 'CANCELLED';
export type ShiftType = 'LUNCH' | 'DINNER' | 'DOUBLE';

export type CoverageEmployeeSummary = {
  id: number;
  displayName: string;
};

export type CoverageRequest = {
  id: number;
  store: StoreCode;
  requestedBy: CoverageEmployeeSummary;
  replacementEmployee?: CoverageEmployeeSummary | null;
  replacementAssigned: boolean;
  coverageType: CoverageType;
  shiftDate: string;
  startTime?: string | null;
  endTime?: string | null;
  shiftType: ShiftType;
  position: Position;
  reason?: string | null;
  managerNote?: string | null;
  status: CoverageStatus;
  approvedBy?: CoverageEmployeeSummary | null;
  approvedAt?: string | null;
  reviewedBy?: CoverageEmployeeSummary | null;
  reviewedAt?: string | null;
  createdAt: string;
  updatedAt: string;
};

export type CreateCoverageRequestPayload = {
  store: StoreCode;
  coverageType: CoverageType;
  shiftDate: string;
  startTime?: string | null;
  endTime?: string | null;
  shiftType: ShiftType;
  position: Position;
  reason?: string | null;
  replacementEmployeeId?: number | null;
};

export type RejectCoverageRequestPayload = {
  managerNote?: string | null;
};

export function getCoveragePool() {
  return api<CoverageRequest[]>('/api/coverage-requests/pool');
}

export function getMyCoverageRequests() {
  return api<CoverageRequest[]>('/api/coverage-requests/mine');
}

export function getCoverageInvolvingMe() {
  return api<CoverageRequest[]>('/api/coverage-requests/involving-me');
}

export function createCoverageRequest(payload: CreateCoverageRequestPayload) {
  return api<CoverageRequest>('/api/coverage-requests', { method: 'POST', body: JSON.stringify(payload) });
}

export function claimCoverageRequest(id: number) {
  return api<CoverageRequest>(`/api/coverage-requests/${id}/claim`, { method: 'POST' });
}

export function cancelCoverageRequest(id: number) {
  return api<CoverageRequest>(`/api/coverage-requests/${id}/cancel`, { method: 'POST' });
}

export function getCoveragePending() {
  return api<CoverageRequest[]>('/api/coverage-requests/pending');
}

export function getCoverageUpcoming(days = 14) {
  return api<CoverageRequest[]>(`/api/coverage-requests/upcoming?days=${days}`);
}

export function getCoverageHistory() {
  return api<CoverageRequest[]>('/api/coverage-requests/history');
}

export function approveCoverageRequest(id: number) {
  return api<CoverageRequest>(`/api/coverage-requests/${id}/approve`, { method: 'POST' });
}

export function rejectCoverageRequest(id: number, payload: RejectCoverageRequestPayload) {
  return api<CoverageRequest>(`/api/coverage-requests/${id}/reject`, { method: 'POST', body: JSON.stringify(payload) });
}

export function getActiveEmployees() {
  return api<EmployeePublic[]>('/api/employees');
}
