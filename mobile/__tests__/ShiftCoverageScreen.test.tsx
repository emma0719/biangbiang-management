import { fireEvent, render, screen, waitFor } from '@testing-library/react-native';
import React from 'react';
import Shift from '../app/shift';
import { api, ApiError } from '../src/api/client';
import { TestProviders } from '../testUtils';

jest.mock('../src/api/client', () => {
  class MockApiError extends Error {
    code: string;
    status?: number;
    constructor(code: string, status?: number) {
      super(code);
      this.code = code;
      this.status = status;
      this.name = 'ApiError';
    }
  }
  return { api: jest.fn(), ApiError: MockApiError };
});

jest.mock('../src/auth/tokenStore', () => ({ clearTokens: jest.fn() }));
jest.mock('expo-router', () => ({ router: { replace: jest.fn(), push: jest.fn() }, usePathname: () => '/shift' }));

describe('ShiftCoverageScreen', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    poolRequests = [openPoolRequest];
    mineRequests = [openOwnRequest, approvedOwnRequest, rejectedOwnRequest];
    involvingRequests = [coveringRequest, openOwnRequest];
    pendingRequests = [managerPendingRequest];
    upcomingRequests = [managerApprovedUpcomingRequest];
    historyRequests = [managerApprovedUpcomingRequest, managerRejectedRequest, managerCancelledRequest, managerPendingRequest];
    currentProfile = employeeProfile;
    createdPayloads = [];
    claimAlreadyTaken = false;
    createFails = false;
    approveAlreadyProcessed = false;
    pendingForbidden = false;
    approvePromise = undefined;
    (api as jest.Mock).mockImplementation(mockApi);
  });

  it('shows the three employee coverage tabs', async () => {
    render(<Shift />, { wrapper: TestProviders });

    expect(await screen.findByText('New Request')).toBeTruthy();
    expect(screen.getByText('Shift Pool')).toBeTruthy();
    expect(screen.getByText('My Requests')).toBeTruthy();
    expect(screen.queryByText('Pending Approval')).toBeNull();
  });

  it('hides replacement employee for Public and shows it for Direct', async () => {
    render(<Shift />, { wrapper: TestProviders });

    await screen.findByText('Submit Request');
    expect(screen.queryByText('Replacement Employee')).toBeNull();

    fireEvent.press(screen.getByText('Direct'));

    expect(await screen.findByText('Replacement Employee')).toBeTruthy();
    expect(screen.getByText('Jordan')).toBeTruthy();
    expect(screen.queryByText('Alex')).toBeNull();
  });

  it('requires replacement employee for Direct requests', async () => {
    render(<Shift />, { wrapper: TestProviders });

    await screen.findByText('Submit Request');
    fireEvent.press(screen.getByText('Direct'));
    fireEvent.press(screen.getByText('Submit Request'));

    expect(await screen.findByText('Choose a replacement employee')).toBeTruthy();
    expect(api).not.toHaveBeenCalledWith('/api/coverage-requests', expect.anything());
  });

  it('blocks past dates and invalid end time before submit', async () => {
    render(<Shift />, { wrapper: TestProviders });

    await screen.findByText('Submit Request');
    fireEvent.changeText(screen.getByLabelText('Shift Date'), '2000-01-01');
    fireEvent.press(screen.getByText('Submit Request'));
    expect(await screen.findByText('Shift date cannot be in the past')).toBeTruthy();

    fireEvent.changeText(screen.getByLabelText('Shift Date'), futureDate());
    fireEvent.changeText(screen.getByLabelText('Start Time'), '18:00');
    fireEvent.changeText(screen.getByLabelText('End Time'), '17:00');
    fireEvent.press(screen.getByText('Submit Request'));
    expect(await screen.findByText('End time must be later than start time')).toBeTruthy();
  });

  it('creates a Public request, clears the form, and switches to My Requests', async () => {
    render(<Shift />, { wrapper: TestProviders });

    await screen.findByText('Submit Request');
    fireEvent.changeText(screen.getByLabelText('Shift Date'), futureDate());
    fireEvent.changeText(screen.getByLabelText('Reason'), 'Family appointment');
    fireEvent.press(screen.getByText('Submit Request'));

    await waitFor(() => expect(api).toHaveBeenCalledWith('/api/coverage-requests', expect.objectContaining({ method: 'POST' })));
    expect(createdPayloads[0]).toEqual(expect.objectContaining({ coverageType: 'PUBLIC', replacementEmployeeId: null }));
    expect(await screen.findByText('Coverage request submitted.')).toBeTruthy();
    expect(screen.getByText('Requests I Posted')).toBeTruthy();
  });

  it('creates Direct request payload with replacement employee', async () => {
    render(<Shift />, { wrapper: TestProviders });

    await screen.findByText('Submit Request');
    fireEvent.press(screen.getByText('Direct'));
    fireEvent.press(await screen.findByText('Jordan'));
    fireEvent.changeText(screen.getByLabelText('Shift Date'), futureDate());
    fireEvent.press(screen.getByText('Submit Request'));

    await waitFor(() => expect(createdPayloads[0]).toEqual(expect.objectContaining({ coverageType: 'DIRECT', replacementEmployeeId: 2 })));
  });

  it('shows public open requests in Shift Pool and disables claiming own request', async () => {
    poolRequests = [openPoolRequest, openOwnRequest];
    render(<Shift />, { wrapper: TestProviders });

    fireEvent.press(await screen.findByText('Shift Pool'));

    expect(await screen.findByText('Need dinner coverage')).toBeTruthy();
    expect(screen.getAllByText('Your request')).toHaveLength(1);
    expect(screen.getAllByText('Cover Shift')).toHaveLength(1);
  });

  it('asks for confirmation before claim and removes successful claim from pool', async () => {
    render(<Shift />, { wrapper: TestProviders });

    fireEvent.press(await screen.findByText('Shift Pool'));
    fireEvent.press(await screen.findByText('Cover Shift'));

    expect(await screen.findByText('Are you sure you can cover this shift?')).toBeTruthy();
    fireEvent.press(screen.getAllByText('Cover Shift')[1]);

    await waitFor(() => expect(api).toHaveBeenCalledWith('/api/coverage-requests/100/claim', expect.objectContaining({ method: 'POST' })));
    expect(await screen.findByText('Coverage request sent for manager approval.')).toBeTruthy();
    expect(await screen.findByText('No open shifts need coverage right now.')).toBeTruthy();
  });

  it('shows claimed-by-someone-else error and refreshes pool', async () => {
    claimAlreadyTaken = true;
    render(<Shift />, { wrapper: TestProviders });

    fireEvent.press(await screen.findByText('Shift Pool'));
    fireEvent.press(await screen.findByText('Cover Shift'));
    fireEvent.press(screen.getAllByText('Cover Shift')[1]);

    expect(await screen.findByText('This shift was already claimed by another employee. Shift Pool has been refreshed.')).toBeTruthy();
    expect(api).toHaveBeenCalledWith('/api/coverage-requests/pool');
  });

  it('merges mine and involving-me without duplicate records', async () => {
    render(<Shift />, { wrapper: TestProviders });

    fireEvent.press(await screen.findByText('My Requests'));

    expect(await screen.findByText('Requests I Posted')).toBeTruthy();
    expect(screen.getByText("Shifts I'm Covering")).toBeTruthy();
    expect(screen.getAllByText('Your request')).toHaveLength(3);
    expect(screen.getAllByText('Need lunch coverage')).toHaveLength(1);
    expect(screen.getByText('Covered by me')).toBeTruthy();
  });

  it('allows cancel for OPEN/PENDING own requests but not final states', async () => {
    mineRequests = [openOwnRequest, pendingOwnRequest, approvedOwnRequest, rejectedOwnRequest, cancelledOwnRequest];
    render(<Shift />, { wrapper: TestProviders });

    fireEvent.press(await screen.findByText('My Requests'));

    expect(await screen.findAllByText('Cancel Request')).toHaveLength(2);
    expect(screen.getByText('Approved')).toBeTruthy();
    expect(screen.getByText('Rejected')).toBeTruthy();
    expect(screen.getByText('Cancelled')).toBeTruthy();

    fireEvent.press(screen.getAllByText('Cancel Request')[0]);
    expect(await screen.findByText('Cancel this coverage request?')).toBeTruthy();
    fireEvent.press(screen.getByLabelText('Confirm cancel coverage request'));
    await waitFor(() => expect(api).toHaveBeenCalledWith('/api/coverage-requests/102/cancel', expect.objectContaining({ method: 'POST' })));
  });

  it('maps ordinary API errors to user-readable messages', async () => {
    createFails = true;
    render(<Shift />, { wrapper: TestProviders });

    await screen.findByText('Submit Request');
    fireEvent.changeText(screen.getByLabelText('Shift Date'), futureDate());
    fireEvent.press(screen.getByText('Submit Request'));

    expect(await screen.findByText('Please check the form and try again')).toBeTruthy();
  });

  it('shows manager tabs only for business partners', async () => {
    currentProfile = managerProfile;
    render(<Shift />, { wrapper: TestProviders });

    expect(await screen.findByText('Pending Approval')).toBeTruthy();
    expect(screen.getByText('Upcoming 14 Days')).toBeTruthy();
    expect(screen.getByText('History')).toBeTruthy();
  });

  it('renders pending approval requests with manager actions', async () => {
    currentProfile = managerProfile;
    render(<Shift />, { wrapper: TestProviders });

    fireEvent.press(await screen.findByText('Pending Approval'));

    expect(await screen.findByText('Need manager approval')).toBeTruthy();
    expect(screen.getByText('Sam')).toBeTruthy();
    expect(screen.getByText('Jordan')).toBeTruthy();
    expect(screen.getByLabelText('Manager note')).toBeTruthy();
    expect(screen.getByText('Approve')).toBeTruthy();
    expect(screen.getByText('Reject')).toBeTruthy();
  });

  it('approves pending coverage and removes it from pending list', async () => {
    currentProfile = managerProfile;
    render(<Shift />, { wrapper: TestProviders });

    fireEvent.press(await screen.findByText('Pending Approval'));
    fireEvent.press(await screen.findByText('Approve'));
    expect(await screen.findByText('Approve this coverage request and record the shift change?')).toBeTruthy();
    fireEvent.press(screen.getByLabelText('Confirm approve coverage request'));

    await waitFor(() => expect(api).toHaveBeenCalledWith('/api/coverage-requests/300/approve', expect.objectContaining({ method: 'POST' })));
    expect(await screen.findByText('Coverage request approved.')).toBeTruthy();
    expect(await screen.findByText('No coverage requests are waiting for approval.')).toBeTruthy();
  });

  it('rejects pending coverage with manager note and removes it from pending list', async () => {
    currentProfile = managerProfile;
    render(<Shift />, { wrapper: TestProviders });

    fireEvent.press(await screen.findByText('Pending Approval'));
    fireEvent.changeText(await screen.findByLabelText('Manager note'), 'Already covered offline');
    fireEvent.press(screen.getByText('Reject'));
    expect(await screen.findByText('Reject this coverage request?')).toBeTruthy();
    fireEvent.press(screen.getByLabelText('Confirm reject coverage request'));

    await waitFor(() => expect(api).toHaveBeenCalledWith('/api/coverage-requests/300/reject', expect.objectContaining({
      method: 'POST',
      body: JSON.stringify({ managerNote: 'Already covered offline' })
    })));
    expect(await screen.findByText('Coverage request rejected.')).toBeTruthy();
    expect(await screen.findByText('No coverage requests are waiting for approval.')).toBeTruthy();
  });

  it('prevents duplicate approve clicks while approval is in flight', async () => {
    currentProfile = managerProfile;
    const deferredApproval = deferred(managerApprovedUpcomingRequest);
    approvePromise = deferredApproval.promise;
    render(<Shift />, { wrapper: TestProviders });

    fireEvent.press(await screen.findByText('Pending Approval'));
    fireEvent.press(await screen.findByText('Approve'));
    fireEvent.press(await screen.findByLabelText('Confirm approve coverage request'));

    await waitFor(() => expect(screen.getByLabelText('Confirm approve coverage request')).toBeDisabled());
    fireEvent.press(screen.getByLabelText('Confirm approve coverage request'));
    expect(countApiPath('/api/coverage-requests/300/approve')).toBe(1);
    deferredApproval.resolve(managerApprovedUpcomingRequest);
    expect(await screen.findByText('Coverage request approved.')).toBeTruthy();
  });

  it('refreshes pending list when another manager already processed a request', async () => {
    currentProfile = managerProfile;
    approveAlreadyProcessed = true;
    render(<Shift />, { wrapper: TestProviders });

    fireEvent.press(await screen.findByText('Pending Approval'));
    fireEvent.press(await screen.findByText('Approve'));
    fireEvent.press(await screen.findByLabelText('Confirm approve coverage request'));

    expect(await screen.findByText('This coverage request has already been processed. Lists have been refreshed.')).toBeTruthy();
    await waitFor(() => expect(countApiPath('/api/coverage-requests/pending')).toBeGreaterThan(1));
  });

  it('shows upcoming approved coverage changes only', async () => {
    currentProfile = managerProfile;
    upcomingRequests = [managerApprovedUpcomingRequest, managerRejectedRequest];
    render(<Shift />, { wrapper: TestProviders });

    fireEvent.press(await screen.findByText('Upcoming 14 Days'));

    expect(await screen.findByText('Approved upcoming coverage')).toBeTruthy();
    expect(screen.getByText('Mini')).toBeTruthy();
    expect(screen.queryByText('Rejected coverage')).toBeNull();
  });

  it('shows approved rejected and cancelled coverage history without duplicating pending', async () => {
    currentProfile = managerProfile;
    render(<Shift />, { wrapper: TestProviders });

    fireEvent.press(await screen.findByText('History'));

    expect(await screen.findByText('Approved upcoming coverage')).toBeTruthy();
    expect(screen.getByText('Rejected coverage')).toBeTruthy();
    expect(screen.getByText('Cancelled by employee')).toBeTruthy();
    expect(screen.queryByText('Need manager approval')).toBeNull();
  });

  it('shows a clear manager permission error when backend returns 403', async () => {
    currentProfile = managerProfile;
    pendingForbidden = true;
    render(<Shift />, { wrapper: TestProviders });

    fireEvent.press(await screen.findByText('Pending Approval'));

    expect(await screen.findByText('Manager permission is required to review coverage requests.')).toBeTruthy();
  });
});

let poolRequests: CoverageRequestFixture[] = [];
let mineRequests: CoverageRequestFixture[] = [];
let involvingRequests: CoverageRequestFixture[] = [];
let pendingRequests: CoverageRequestFixture[] = [];
let upcomingRequests: CoverageRequestFixture[] = [];
let historyRequests: CoverageRequestFixture[] = [];
let createdPayloads: Array<Record<string, unknown>> = [];
let claimAlreadyTaken = false;
let createFails = false;
let approveAlreadyProcessed = false;
let pendingForbidden = false;
let approvePromise: Promise<CoverageRequestFixture> | undefined;
let currentProfile: ProfileFixture;

function mockApi(path: string, init?: RequestInit) {
  if (path === '/api/me') return Promise.resolve(currentProfile);
  if (path === '/api/employees') return Promise.resolve(employees);
  if (path === '/api/coverage-requests/pool') return Promise.resolve(poolRequests);
  if (path === '/api/coverage-requests/mine') return Promise.resolve(mineRequests);
  if (path === '/api/coverage-requests/involving-me') return Promise.resolve(involvingRequests);
  if (path === '/api/coverage-requests/pending') {
    if (pendingForbidden) return Promise.reject(new ApiError('AUTH_BUSINESS_PARTNER_REQUIRED', 403));
    return Promise.resolve(pendingRequests);
  }
  if (path === '/api/coverage-requests/upcoming?days=14') return Promise.resolve(upcomingRequests);
  if (path === '/api/coverage-requests/history') return Promise.resolve(historyRequests);
  if (path === '/api/coverage-requests' && init?.method === 'POST') {
    if (createFails) {
      createFails = false;
      return Promise.reject(new ApiError('VALIDATION_FAILED', 400));
    }
    const payload = JSON.parse(String(init.body));
    createdPayloads.push(payload);
    const created = { ...openOwnRequest, id: 900, ...payload, requestedBy: { id: currentProfile.id, displayName: currentProfile.displayName }, status: payload.coverageType === 'DIRECT' ? 'PENDING_APPROVAL' : 'OPEN' };
    mineRequests = [created, ...mineRequests];
    return Promise.resolve(created);
  }
  if (path === '/api/coverage-requests/100/claim') {
    if (claimAlreadyTaken) {
      claimAlreadyTaken = false;
      poolRequests = [];
      return Promise.reject(new ApiError('COVERAGE_REQUEST_ALREADY_CLAIMED', 409));
    }
    poolRequests = poolRequests.filter((request) => request.id !== 100);
    involvingRequests = [{ ...openPoolRequest, status: 'PENDING_APPROVAL', replacementAssigned: true, replacementEmployee: { id: currentProfile.id, displayName: currentProfile.displayName } }];
    return Promise.resolve(involvingRequests[0]);
  }
  if (path === '/api/coverage-requests/300/approve') {
    if (approveAlreadyProcessed) {
      pendingRequests = [];
      return Promise.reject(new ApiError('COVERAGE_REQUEST_NOT_PENDING_APPROVAL', 409));
    }
    if (approvePromise) return approvePromise.then((approved) => {
      pendingRequests = pendingRequests.filter((request) => request.id !== 300);
      return approved;
    });
    pendingRequests = pendingRequests.filter((request) => request.id !== 300);
    upcomingRequests = [managerApprovedUpcomingRequest];
    historyRequests = [managerApprovedUpcomingRequest, ...historyRequests.filter((request) => request.id !== 300)];
    return Promise.resolve(managerApprovedUpcomingRequest);
  }
  if (path === '/api/coverage-requests/300/reject') {
    const body = JSON.parse(String(init?.body ?? '{}'));
    const rejected = { ...managerPendingRequest, status: 'REJECTED', managerNote: body.managerNote, reviewedBy: { id: managerProfile.id, displayName: managerProfile.displayName }, reviewedAt: '2026-07-15T20:00:00Z' };
    pendingRequests = pendingRequests.filter((request) => request.id !== 300);
    historyRequests = [rejected, ...historyRequests.filter((request) => request.id !== 300)];
    return Promise.resolve(rejected);
  }
  const cancelMatch = path.match(/^\/api\/coverage-requests\/(\d+)\/cancel$/);
  if (cancelMatch) {
    const id = Number(cancelMatch[1]);
    mineRequests = mineRequests.map((request) => request.id === id ? { ...request, status: 'CANCELLED' } : request);
    return Promise.resolve(mineRequests.find((request) => request.id === id));
  }
  return Promise.resolve({});
}

type CoverageRequestFixture = {
  id: number;
  store: string;
  requestedBy: { id: number; displayName: string };
  replacementEmployee?: { id: number; displayName: string } | null;
  replacementAssigned: boolean;
  coverageType: string;
  shiftDate: string;
  startTime?: string | null;
  endTime?: string | null;
  shiftType: string;
  position: string;
  reason?: string | null;
  managerNote?: string | null;
  status: string;
  approvedBy?: { id: number; displayName: string } | null;
  approvedAt?: string | null;
  reviewedBy?: { id: number; displayName: string } | null;
  reviewedAt?: string | null;
  createdAt: string;
  updatedAt: string;
};

type ProfileFixture = {
  id: number;
  englishName: string;
  preferredName: string;
  displayName: string;
  email: string;
  phone: string;
  homeStore: string;
  eligibleStores: string[];
  positions: string[];
  status: string;
  createdAt: string;
};

const employeeProfile: ProfileFixture = {
  id: 1,
  englishName: 'Alex',
  preferredName: 'Alex',
  displayName: 'Alex',
  email: 'alex@example.com',
  phone: '+12065550101',
  homeStore: 'SEATTLE',
  eligibleStores: ['SEATTLE'],
  positions: ['HOST'],
  status: 'ACTIVE',
  createdAt: '2026-07-01T00:00:00Z'
};

const managerProfile = {
  ...employeeProfile,
  id: 9,
  englishName: 'Mini',
  preferredName: 'Mini',
  displayName: 'Mini',
  email: 'mini@example.com',
  positions: ['MANAGER']
};

const employees = [
  { id: 1, displayName: 'Alex', profilePhotoKey: null, positions: ['HOST'], homeStore: 'SEATTLE', status: 'ACTIVE' },
  { id: 2, displayName: 'Jordan', profilePhotoKey: null, positions: ['BARTENDER'], homeStore: 'REDMOND', status: 'ACTIVE' },
  { id: 3, displayName: 'Inactive Pat', profilePhotoKey: null, positions: ['HOST'], homeStore: 'SEATTLE', status: 'DEACTIVATED' }
];

const openPoolRequest: CoverageRequestFixture = {
  id: 100,
  store: 'SEATTLE',
  requestedBy: { id: 4, displayName: 'Sam' },
  replacementEmployee: null,
  replacementAssigned: false,
  coverageType: 'PUBLIC',
  shiftDate: futureDate(),
  startTime: '17:00:00',
  endTime: '22:00:00',
  shiftType: 'DINNER',
  position: 'HOST',
  reason: 'Need dinner coverage',
  status: 'OPEN',
  createdAt: '2026-07-15T18:00:00Z',
  updatedAt: '2026-07-15T18:00:00Z'
};

const openOwnRequest: CoverageRequestFixture = {
  ...openPoolRequest,
  id: 101,
  requestedBy: { id: 1, displayName: 'Alex' },
  reason: 'Need lunch coverage',
  shiftType: 'LUNCH'
};

const pendingOwnRequest: CoverageRequestFixture = {
  ...openOwnRequest,
  id: 102,
  status: 'PENDING_APPROVAL',
  replacementAssigned: true,
  replacementEmployee: { id: 2, displayName: 'Jordan' }
};

const approvedOwnRequest: CoverageRequestFixture = {
  ...pendingOwnRequest,
  id: 103,
  status: 'APPROVED',
  reason: 'Approved coverage'
};

const rejectedOwnRequest: CoverageRequestFixture = {
  ...pendingOwnRequest,
  id: 104,
  status: 'REJECTED',
  reason: 'Rejected coverage',
  managerNote: 'Talk to manager first'
};

const cancelledOwnRequest: CoverageRequestFixture = {
  ...openOwnRequest,
  id: 105,
  status: 'CANCELLED'
};

const coveringRequest: CoverageRequestFixture = {
  ...openPoolRequest,
  id: 200,
  status: 'PENDING_APPROVAL',
  replacementAssigned: true,
  replacementEmployee: { id: 1, displayName: 'Alex' },
  reason: 'Covered by me'
};

const managerPendingRequest: CoverageRequestFixture = {
  ...openPoolRequest,
  id: 300,
  requestedBy: { id: 4, displayName: 'Sam' },
  replacementEmployee: { id: 2, displayName: 'Jordan' },
  replacementAssigned: true,
  coverageType: 'PUBLIC',
  status: 'PENDING_APPROVAL',
  reason: 'Need manager approval'
};

const managerApprovedUpcomingRequest: CoverageRequestFixture = {
  ...managerPendingRequest,
  id: 301,
  status: 'APPROVED',
  reason: 'Approved upcoming coverage',
  approvedBy: { id: 9, displayName: 'Mini' },
  approvedAt: '2026-07-15T20:00:00Z',
  reviewedBy: { id: 9, displayName: 'Mini' },
  reviewedAt: '2026-07-15T20:00:00Z'
};

const managerRejectedRequest: CoverageRequestFixture = {
  ...managerPendingRequest,
  id: 302,
  status: 'REJECTED',
  reason: 'Rejected coverage',
  managerNote: 'Cannot approve',
  reviewedBy: { id: 9, displayName: 'Mini' },
  reviewedAt: '2026-07-15T19:00:00Z'
};

const managerCancelledRequest: CoverageRequestFixture = {
  ...managerPendingRequest,
  id: 303,
  status: 'CANCELLED',
  reason: 'Cancelled by employee',
  replacementEmployee: null,
  replacementAssigned: false,
  reviewedAt: undefined
};

function countApiPath(path: string) {
  return (api as jest.Mock).mock.calls.filter(([calledPath]) => calledPath === path).length;
}

function deferred<T>(value: T) {
  let resolve: (result: T) => void = () => undefined;
  const promise = new Promise<T>((promiseResolve) => {
    resolve = promiseResolve;
  });
  return { promise, resolve: (result: T = value) => resolve(result) };
}

function futureDate() {
  const date = new Date();
  date.setDate(date.getDate() + 7);
  return date.toISOString().slice(0, 10);
}
