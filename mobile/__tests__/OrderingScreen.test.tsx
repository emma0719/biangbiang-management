import { fireEvent, render, screen, waitFor } from '@testing-library/react-native';
import * as SecureStore from 'expo-secure-store';
import { router } from 'expo-router';
import React from 'react';
import { Alert, Platform, StyleSheet } from 'react-native';
import { api, apiBinary, ApiError } from '../src/api/client';
import { translations } from '../src/i18n/translations';
import { OrderingScreen } from '../src/screens/OrderingScreen';
import { downloadPdfFile, preparePdfViewTarget, viewPdfFile } from '../src/utils/orderPlanPdfFile';
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
  return { api: jest.fn(), apiBinary: jest.fn(), ApiError: MockApiError };
});

jest.mock('../src/utils/orderPlanPdfFile', () => ({
  preparePdfViewTarget: jest.fn(),
  viewPdfFile: jest.fn().mockResolvedValue(undefined),
  downloadPdfFile: jest.fn().mockResolvedValue(undefined)
}));

let mockedPathname = '/order';
jest.mock('expo-router', () => ({ router: { replace: jest.fn(), push: jest.fn() }, usePathname: () => mockedPathname }));

describe('OrderingScreen', () => {
  let pdfTarget: { location: { replace: jest.Mock }; close: jest.Mock };

  beforeEach(() => {
    jest.clearAllMocks();
    mockedPathname = '/order';
    catalogProducts = products.map((product) => ({ ...product }));
    pdfTarget = { location: { replace: jest.fn() }, close: jest.fn() };
    (SecureStore.getItemAsync as jest.Mock).mockResolvedValue(null);
    (apiBinary as jest.Mock).mockResolvedValue(pdfBytes);
    (preparePdfViewTarget as jest.Mock).mockReturnValue(pdfTarget);
    (viewPdfFile as jest.Mock).mockClear();
    (downloadPdfFile as jest.Mock).mockClear();
    (api as jest.Mock).mockImplementation((path: string, init?: RequestInit) => {
      if (path === '/api/me') return Promise.resolve(profile);
      if (path.startsWith('/api/vendors')) return Promise.resolve(vendors);
      if (path.startsWith('/api/order-products')) return Promise.resolve(productsForPath(path));
      if (path === '/api/order-catalog' && init?.method === 'POST') return Promise.resolve(createOrderCatalogProduct(init));
      if (path.startsWith('/api/order-catalog/') && init?.method === 'PATCH') return Promise.resolve(updateOrderCatalogProduct(path, init));
      if (path.startsWith('/api/order-catalog/') && init?.method === 'DELETE') return Promise.resolve(deleteOrderCatalogProduct(path));
      if (path.startsWith('/api/order-inventory-reference')) return Promise.resolve(orderInventoryReference);
      if (path === '/api/inventory-catalog' && init?.method === 'POST') return Promise.resolve(createInventoryCatalogProduct(init));
      if (path.startsWith('/api/inventory-catalog/') && init?.method === 'PATCH') return Promise.resolve(updateInventoryCatalogProduct(path, init));
      if (path.startsWith('/api/inventory-catalog/') && init?.method === 'DELETE') return Promise.resolve(deleteInventoryCatalogProduct(path));
      if (path.startsWith('/api/purchase-orders?')) return Promise.resolve(orders);
      if (path.startsWith('/api/inventory-counts?')) return Promise.resolve(counts);
      if (path.startsWith('/api/order-plans?')) return Promise.resolve(plans);
      if (path === '/api/order-plans/70/pdf') return Promise.reject(new ApiError('ORDER_PLAN_PDF_NOT_FOUND', 404));
      if (path === '/api/inventory-counts' && init?.method === 'POST') return Promise.resolve(inventoryDraft);
      if (path.includes('/api/inventory-counts/') && path.includes('/lines')) return Promise.resolve(inventoryDraft);
      if (path.includes('/api/inventory-counts/') && path.includes('/submit')) return Promise.resolve({ ...inventoryDraft, status: 'SUBMITTED', completedByNameSnapshot: 'Alex' });
      if (path === '/api/order-plans' && init?.method === 'POST') return Promise.resolve(plans[0]);
      if (path.includes('/api/order-plans/') && path.includes('/lines')) return Promise.resolve(plans[0]);
      if (path.includes('/api/order-plans/') && path.includes('/submit')) return Promise.resolve({ ...plans[0], status: 'SUBMITTED', submittedByNameSnapshot: 'Alex' });
      if (path.includes('/api/order-plans/') && path.includes('/generate-vendor-orders')) return Promise.resolve(orders);
      if (path === '/api/order-sessions' && init?.method === 'POST') return Promise.resolve({ ...plans[0], status: 'DRAFT', createdByEmployeeId: profile.id, orderBusiness: 'BIANGBIANG_FRONT' });
      if (path.includes('/api/order-sessions/') && path.includes('/lines')) return Promise.resolve({ ...plans[0], status: 'IN_PROGRESS', createdByEmployeeId: profile.id, orderBusiness: 'BIANGBIANG_FRONT' });
      if (path.includes('/api/order-sessions/') && path.includes('/submit')) return Promise.resolve({ ...plans[0], status: 'SUBMITTED', submittedByNameSnapshot: 'Alex', orderBusiness: 'BIANGBIANG_FRONT' });
      if (path.includes('/api/order-sessions/') && path.includes('/amounts')) return Promise.resolve({ ...plans[0], status: 'SUBMITTED', submittedByNameSnapshot: 'Alex', lastModifiedByNameSnapshot: 'Alex', updatedAt: '2026-07-15T18:00:00Z', orderBusiness: 'BIANGBIANG_FRONT', lines: [{ ...plans[0].lines[0], finalOrderQuantity: 9 }] });
      if (path.includes('/api/order-sessions/') && path.includes('/cancel')) return Promise.resolve({ ...plans[0], status: 'CANCELLED', orderBusiness: 'BIANGBIANG_FRONT' });
      if (path === '/api/purchase-orders' && init?.method === 'POST') return Promise.resolve({ ...orders[0], id: 30, status: 'DRAFT', lines: [] });
      if (path.includes('/lines')) return Promise.resolve({ ...orders[0], id: Number(path.match(/purchase-orders\/(\d+)/)?.[1] ?? orders[0].id), status: 'DRAFT', lines: [line] });
      if (path.includes('/submit')) return Promise.resolve({ ...orders[0], status: 'SUBMITTED', lines: [line] });
      return Promise.resolve(orders[0]);
    });
  });

  const selectFrontInventoryBusiness = async () => {
    fireEvent.press(await screen.findByText('BiangBiang Front'));
  };

  const openFrontInventoryCount = async () => {
    await selectFrontInventoryBusiness();
    return screen.findByLabelText('Quantity Black (Dry mix)');
  };

  const selectFrontOrderBusiness = async () => {
    fireEvent.press(await screen.findByText('BiangBiang Front'));
  };

  it('renders vendors, updates quantity subtotal, and exposes approval workflow for business partners', async () => {
    render(<OrderingScreen />, { wrapper: TestProviders });

    expect(await screen.findByText('Ordering')).toBeTruthy();
    fireEvent.press(await screen.findByText('Vendor selection'));
    expect(await screen.findByText('Wellpack')).toBeTruthy();

    fireEvent.press(screen.getByText('Wellpack'));
    fireEvent.changeText((await screen.findAllByLabelText('Order Quantity'))[0], '3');
    await waitFor(() => expect(screen.getAllByText('$7.50').length).toBeGreaterThanOrEqual(1));

    fireEvent.press(screen.getByText('Order Review'));
    expect(await screen.findByText('Approve')).toBeTruthy();
    expect(screen.getByText('Reject')).toBeTruthy();

    fireEvent.press(screen.getByText('Receiving'));
    expect(await screen.findByLabelText('Received now')).toBeTruthy();
  });

  it('keeps the old ordering workspace compatible', async () => {
    render(<OrderingScreen />, { wrapper: TestProviders });

    expect(await screen.findByText('Ordering')).toBeTruthy();
    expect(await screen.findByText('Start Inventory Count')).toBeTruthy();
    expect(await screen.findByText('Create Order')).toBeTruthy();
  });

  it('shows only purchase order workspaces in order mode', async () => {
    render(<OrderingScreen mode="order" />, { wrapper: TestProviders });

    expect((await screen.findAllByText('New Order')).length).toBeGreaterThan(0);
    expect(screen.getByText('Order Details')).toBeTruthy();
    expect(screen.getByText('History')).toBeTruthy();
    expect(screen.queryByText('Inventory Count')).toBeNull();
    expect(screen.queryByText('Order Plans')).toBeNull();
  });

  it('supports vendor rail ordering, search, summary, and draft saving in order mode', async () => {
    render(<OrderingScreen mode="order" />, { wrapper: TestProviders });

    await selectFrontOrderBusiness();
    expect((await screen.findAllByText('Wellpack')).length).toBeGreaterThan(0);
    expect(screen.queryByText('No Delivery')).toBeNull();
    expect(screen.getByText('Black (Dry mix)')).toBeTruthy();
    expect(screen.queryByText('Emergency gloves')).toBeNull();

    fireEvent.press(screen.getByLabelText('Increase quantity Black (Dry mix)'));
    expect(await screen.findByDisplayValue('1')).toBeTruthy();
    expect(screen.getByText('Items')).toBeTruthy();
    expect(screen.getByText('Total Quantity')).toBeTruthy();
    expect(screen.getAllByText('Latest inventory').length).toBeGreaterThan(0);
    expect(screen.queryByText('Estimated Total')).toBeNull();

    fireEvent.press(screen.getByLabelText('Decrease quantity Black (Dry mix)'));
    fireEvent.press(screen.getByLabelText('Decrease quantity Black (Dry mix)'));
    expect((await screen.findAllByDisplayValue('0')).length).toBeGreaterThan(0);

    fireEvent.changeText(screen.getAllByLabelText('Order Quantity')[0], '3');
    expect(await screen.findByDisplayValue('3')).toBeTruthy();
    fireEvent.changeText(screen.getAllByLabelText('Order Quantity')[0], '');
    fireEvent.press(screen.getByText('Save Draft'));

    await waitFor(() => expect(api).toHaveBeenCalledWith('/api/order-sessions', expect.objectContaining({ method: 'POST' })));
    const createCall = (api as jest.Mock).mock.calls.find(([path, init]) => path === '/api/order-sessions' && init?.method === 'POST');
    expect(JSON.parse(String(createCall[1].body))).toEqual(expect.objectContaining({ locationCode: 'SEATTLE', orderBusiness: 'BIANGBIANG_FRONT' }));
    await waitFor(() => expect(api).toHaveBeenCalledWith('/api/order-sessions/70/lines', expect.objectContaining({ method: 'PUT' })));
    expect(await screen.findByText('Saved')).toBeTruthy();

    fireEvent.press(screen.getByText('Change business'));
    fireEvent.press(await screen.findByText('Paper Fan'));
    fireEvent.press(await screen.findByLabelText('No Delivery'));
    expect(await screen.findByText('Emergency gloves')).toBeTruthy();
    expect(screen.queryByText('Black (Dry mix)')).toBeNull();
    fireEvent.changeText(screen.getByLabelText('Search products'), 'black');
    expect(await screen.findByText('No products found')).toBeTruthy();
  });

  it('imports latest inventory as read-only reference in order mode', async () => {
    render(<OrderingScreen mode="order" />, { wrapper: TestProviders });

    await selectFrontOrderBusiness();
    expect(await screen.findByText('Black (Dry mix)')).toBeTruthy();
    expect(screen.getAllByText('—').length).toBeGreaterThan(0);

    fireEvent.press(screen.getByText('Import latest inventory'));

    await waitFor(() => expect(api).toHaveBeenCalledWith('/api/order-inventory-reference?locationCode=SEATTLE&business=BIANGBIANG_FRONT'));
    expect(await screen.findByText('Latest inventory imported for reference')).toBeTruthy();
    expect(await screen.findByText('12 box')).toBeTruthy();
    expect(screen.getAllByLabelText('Order Quantity')[0].props.value).toBe('0');
  });

  it('allows business partners to manage order catalog items inline without inventory catalog calls', async () => {
    render(<OrderingScreen mode="order" />, { wrapper: TestProviders });

    await selectFrontOrderBusiness();
    fireEvent.press(await screen.findByText('Manage items'));
    fireEvent.changeText(screen.getByLabelText('Item name'), 'Front Napkins');
    fireEvent.changeText(screen.getByLabelText('Unit'), 'case');
    fireEvent.changeText(screen.getByLabelText('Vendor'), 'Paper Supply');
    fireEvent.press(screen.getByLabelText('Add item'));

    await waitFor(() => expect(api).toHaveBeenCalledWith('/api/order-catalog', expect.objectContaining({
      method: 'POST',
      body: JSON.stringify({ locationCode: 'SEATTLE', orderBusiness: 'BIANGBIANG_FRONT', name: 'Front Napkins', unit: 'case', vendorName: 'Paper Supply' })
    })));
    expect(await screen.findByText('Order item added')).toBeTruthy();
    expect(api).not.toHaveBeenCalledWith('/api/inventory-catalog', expect.anything());
  });

  it('shows a success dialog after submitting an order session and opens refreshed history', async () => {
    let sessionSubmitted = false;
    (api as jest.Mock).mockImplementation((path: string, init?: RequestInit) => {
      if (path === '/api/me') return Promise.resolve(profile);
      if (path.startsWith('/api/vendors')) return Promise.resolve(vendors);
      if (path.startsWith('/api/order-products')) return Promise.resolve(productsForPath(path));
      if (path.startsWith('/api/purchase-orders?')) return Promise.resolve([]);
      if (path.startsWith('/api/inventory-counts?')) return Promise.resolve(counts);
      if (path.startsWith('/api/order-plans?')) return Promise.resolve(sessionSubmitted ? [{ ...plans[0], status: 'SUBMITTED', submittedByNameSnapshot: 'Alex', submittedAt: '2026-07-14T20:00:00Z', orderBusiness: 'BIANGBIANG_FRONT' }] : plans);
      if (path === '/api/order-sessions' && init?.method === 'POST') return Promise.resolve({ ...plans[0], status: 'DRAFT', createdByEmployeeId: profile.id, orderBusiness: 'BIANGBIANG_FRONT' });
      if (path === '/api/order-sessions/70/lines' && init?.method === 'PUT') return Promise.resolve({ ...plans[0], status: 'IN_PROGRESS', createdByEmployeeId: profile.id, orderBusiness: 'BIANGBIANG_FRONT' });
      if (path === '/api/order-sessions/70/submit' && init?.method === 'POST') { sessionSubmitted = true; return Promise.resolve({ ...plans[0], status: 'SUBMITTED', submittedByNameSnapshot: 'Alex', submittedAt: '2026-07-14T20:00:00Z', orderBusiness: 'BIANGBIANG_FRONT' }); }
      return defaultApiResponse(path, init);
    });
    render(<OrderingScreen mode="order" />, { wrapper: TestProviders });

    await selectFrontOrderBusiness();
    fireEvent.press(await screen.findByLabelText('Increase quantity Black (Dry mix)'));
    fireEvent.press(screen.getByText('Submit Order'));

    await waitFor(() => expect(api).toHaveBeenCalledWith('/api/order-sessions', expect.objectContaining({ method: 'POST' })));
    await waitFor(() => expect(api).toHaveBeenCalledWith('/api/order-sessions/70/lines', expect.objectContaining({ method: 'PUT' })));
    await waitFor(() => expect(api).toHaveBeenCalledWith('/api/order-sessions/70/submit', { method: 'POST', body: undefined }));
    expect(await screen.findByText('Order Submitted')).toBeTruthy();
    expect(screen.getByText('Your order was submitted successfully. You can view the submitted order and export its PDF from Order History.')).toBeTruthy();
    expect(screen.getByText('Stay on Order')).toBeTruthy();
    expect(screen.getByText('View History')).toBeTruthy();
    fireEvent.press(screen.getByText('View History'));
    expect(await screen.findByText('Order History')).toBeTruthy();
    expect(await screen.findByText('Order Session #70')).toBeTruthy();
    expect(await screen.findByText('View PDF')).toBeTruthy();
    expect(screen.queryByText('Save Draft')).toBeNull();
  });

  it('keeps the user on the submitted order when Stay on Order is selected', async () => {
    let sessionSubmitted = false;
    (api as jest.Mock).mockImplementation((path: string, init?: RequestInit) => {
      if (path.startsWith('/api/order-plans?')) return Promise.resolve(sessionSubmitted ? [{ ...plans[0], status: 'SUBMITTED', submittedByNameSnapshot: 'Alex', submittedAt: '2026-07-14T20:00:00Z', orderBusiness: 'BIANGBIANG_FRONT' }] : plans);
      if (path === '/api/order-sessions/70/submit' && init?.method === 'POST') {
        sessionSubmitted = true;
        return Promise.resolve({ ...plans[0], status: 'SUBMITTED', submittedByNameSnapshot: 'Alex', submittedAt: '2026-07-14T20:00:00Z', orderBusiness: 'BIANGBIANG_FRONT' });
      }
      return defaultApiResponse(path, init);
    });
    render(<OrderingScreen mode="order" />, { wrapper: TestProviders });

    await selectFrontOrderBusiness();
    fireEvent.press(await screen.findByLabelText('Increase quantity Black (Dry mix)'));
    fireEvent.press(screen.getByText('Submit Order'));
    fireEvent.press(await screen.findByText('Stay on Order'));

    expect(await screen.findByText('Order Details · Session #70')).toBeTruthy();
    expect(screen.getByText('SUBMITTED')).toBeTruthy();
    expect(screen.queryByText('Submit Order')).toBeNull();
  });

  it('shows a failure dialog without clearing the order draft when submit fails', async () => {
    (api as jest.Mock).mockImplementation((path: string, init?: RequestInit) => {
      if (path === '/api/order-sessions/70/submit' && init?.method === 'POST') return Promise.reject(new ApiError('ORDER_SESSION_HAS_NO_LINES', 409));
      return defaultApiResponse(path, init);
    });
    render(<OrderingScreen mode="order" />, { wrapper: TestProviders });

    await selectFrontOrderBusiness();
    fireEvent.press(await screen.findByLabelText('Increase quantity Black (Dry mix)'));
    fireEvent.press(screen.getByText('Submit Order'));

    expect(await screen.findByText('Order Submission Failed')).toBeTruthy();
    expect(screen.getByText('ORDER_SESSION_HAS_NO_LINES')).toBeTruthy();
    expect(screen.queryByText('Order Submitted')).toBeNull();
    fireEvent.press(screen.getByText('Stay on Order'));
    expect(await screen.findByText('Submit Order')).toBeTruthy();
    expect(screen.getAllByLabelText('Order Quantity')[0].props.value).toBe('1');
  });

  it('disables order submit while submitting to prevent duplicate submit calls', async () => {
    let resolveSubmit: (order: any) => void = () => undefined;
    (api as jest.Mock).mockImplementation((path: string, init?: RequestInit) => {
      if (path === '/api/order-sessions/70/submit' && init?.method === 'POST') return new Promise((resolve) => { resolveSubmit = resolve; });
      return defaultApiResponse(path, init);
    });
    render(<OrderingScreen mode="order" />, { wrapper: TestProviders });

    await selectFrontOrderBusiness();
    fireEvent.press(await screen.findByLabelText('Increase quantity Black (Dry mix)'));
    fireEvent.press(screen.getByText('Submit Order'));
    await waitFor(() => expect(screen.getByText('Submitting order...')).toBeDisabled());
    fireEvent.press(screen.getByText('Submitting order...'));

    expect((api as jest.Mock).mock.calls.filter(([path, init]) => path === '/api/order-sessions/70/submit' && init?.method === 'POST')).toHaveLength(1);
    resolveSubmit({ ...plans[0], status: 'SUBMITTED', submittedByNameSnapshot: 'Alex', submittedAt: '2026-07-14T20:00:00Z', orderBusiness: 'BIANGBIANG_FRONT' });
  });

  it('opens the session PDF directly from order history with the authenticated PDF flow', async () => {
    (api as jest.Mock).mockImplementation((path: string, init?: RequestInit) => {
      if (path.startsWith('/api/order-plans?')) return Promise.resolve([{ ...plans[0], status: 'SUBMITTED', submittedByNameSnapshot: 'Alex', submittedAt: '2026-07-14T20:00:00Z', orderBusiness: 'BIANGBIANG_FRONT' }]);
      return defaultApiResponse(path, init);
    });
    render(<OrderingScreen mode="order" />, { wrapper: TestProviders });

    fireEvent.press(await screen.findByText('History'));
    fireEvent.press(await screen.findByText('View PDF'));

    expect(preparePdfViewTarget).toHaveBeenCalledTimes(1);
    await waitFor(() => expect(apiBinary).toHaveBeenCalledWith('/api/order-plans/70/pdf/view'));
    expect(apiBinary).not.toHaveBeenCalledWith('/api/purchase-orders/70/pdf');
    expect(viewPdfFile).toHaveBeenCalledWith(pdfBytes, 'order-plan-70-v1.pdf', pdfTarget);
  });

  it('downloads the session PDF directly from order history with the authenticated PDF flow', async () => {
    (api as jest.Mock).mockImplementation((path: string, init?: RequestInit) => {
      if (path.startsWith('/api/order-plans?')) return Promise.resolve([{ ...plans[0], status: 'SUBMITTED', submittedByNameSnapshot: 'Alex', submittedAt: '2026-07-14T20:00:00Z', orderBusiness: 'BIANGBIANG_FRONT' }]);
      return defaultApiResponse(path, init);
    });
    render(<OrderingScreen mode="order" />, { wrapper: TestProviders });

    fireEvent.press(await screen.findByText('History'));
    fireEvent.press(await screen.findByText('Download PDF'));

    await waitFor(() => expect(apiBinary).toHaveBeenCalledWith('/api/order-plans/70/pdf/download'));
    expect(apiBinary).not.toHaveBeenCalledWith('/api/purchase-orders/70/pdf');
    expect(downloadPdfFile).toHaveBeenCalledWith(pdfBytes, 'order-plan-70-v1.pdf');
  });

  it('lets business partners edit submitted order amounts and refreshes the session PDF state', async () => {
    const submittedSession = { ...plans[0], status: 'SUBMITTED', submittedByNameSnapshot: 'Alex', submittedAt: '2026-07-14T20:00:00Z', updatedAt: '2026-07-14T20:00:00Z', orderBusiness: 'BIANGBIANG_FRONT' };
    (api as jest.Mock).mockImplementation((path: string, init?: RequestInit) => {
      if (path.startsWith('/api/order-plans?')) return Promise.resolve([submittedSession]);
      if (path === '/api/order-sessions/70/amounts' && init?.method === 'PUT') {
        return Promise.resolve({ ...submittedSession, lastModifiedByNameSnapshot: 'Alex', updatedAt: '2026-07-15T18:00:00Z', lines: [{ ...plans[0].lines[0], finalOrderQuantity: 9 }] });
      }
      return defaultApiResponse(path, init);
    });
    render(<OrderingScreen mode="order" />, { wrapper: TestProviders });

    await selectFrontOrderBusiness();
    fireEvent.press(await screen.findByText('History'));
    fireEvent.press(await screen.findByText('Edit Order Amounts'));
    fireEvent.changeText(await screen.findByLabelText('Order Quantity'), '9');
    fireEvent.press(screen.getByText('Save Changes'));

    await waitFor(() => expect(api).toHaveBeenCalledWith('/api/order-sessions/70/amounts', expect.objectContaining({
      method: 'PUT',
      body: JSON.stringify({ lines: [{ lineId: 701, finalOrderQuantity: 9, notes: '' }] })
    })));
    expect(await screen.findByText('Order Updated')).toBeTruthy();
    expect(screen.getByText('The order amounts were updated successfully. The session PDF has been refreshed.')).toBeTruthy();
  });

  it('keeps submitted amount edits on screen when saving fails', async () => {
    const submittedSession = { ...plans[0], status: 'SUBMITTED', submittedByNameSnapshot: 'Alex', submittedAt: '2026-07-14T20:00:00Z', orderBusiness: 'BIANGBIANG_FRONT' };
    (api as jest.Mock).mockImplementation((path: string, init?: RequestInit) => {
      if (path.startsWith('/api/order-plans?')) return Promise.resolve([submittedSession]);
      if (path === '/api/order-sessions/70/amounts' && init?.method === 'PUT') return Promise.reject(new ApiError('ORDER_PLAN_PDF_RENDER_FAILED', 500));
      return defaultApiResponse(path, init);
    });
    render(<OrderingScreen mode="order" />, { wrapper: TestProviders });

    await selectFrontOrderBusiness();
    fireEvent.press(await screen.findByText('History'));
    fireEvent.press(await screen.findByText('Edit Order Amounts'));
    fireEvent.changeText(await screen.findByLabelText('Order Quantity'), '9');
    fireEvent.press(screen.getByText('Save Changes'));

    expect(await screen.findByText('Unable to update order')).toBeTruthy();
    expect(screen.queryByText('Order Updated')).toBeNull();
    fireEvent.press(screen.getByText('Stay on Order'));
    expect(screen.getByLabelText('Order Quantity').props.value).toBe('9');
  });

  it('removes draft order sessions from history without showing PDF actions', async () => {
    (api as jest.Mock).mockImplementation((path: string, init?: RequestInit) => {
      if (path.startsWith('/api/order-plans?')) return Promise.resolve([{ ...plans[0], status: 'DRAFT', orderBusiness: 'BIANGBIANG_FRONT' }]);
      if (path === '/api/order-sessions/70/cancel' && init?.method === 'POST') return Promise.resolve({ ...plans[0], status: 'CANCELLED', orderBusiness: 'BIANGBIANG_FRONT' });
      return defaultApiResponse(path, init);
    });
    render(<OrderingScreen mode="order" />, { wrapper: TestProviders });

    fireEvent.press(await screen.findByText('History'));
    expect(await screen.findByText('Continue Editing · Session #70')).toBeTruthy();
    expect(screen.queryByText('View PDF')).toBeNull();
    fireEvent.press(screen.getByText('Remove Draft'));

    await waitFor(() => expect(api).toHaveBeenCalledWith('/api/order-sessions/70/cancel', { method: 'POST', body: undefined }));
  });

  it('disables order mode actions while saving', async () => {
    let resolveCreate: (order: typeof plans[number]) => void = () => undefined;
    (api as jest.Mock).mockImplementation((path: string, init?: RequestInit) => {
      if (path === '/api/order-sessions' && init?.method === 'POST') return new Promise((resolve) => { resolveCreate = resolve; });
      return defaultApiResponse(path, init);
    });

    render(<OrderingScreen mode="order" />, { wrapper: TestProviders });
    await selectFrontOrderBusiness();
    fireEvent.press(await screen.findByText('Save Draft'));

    await waitFor(() => expect(screen.getByText('Loading')).toBeDisabled());
    expect(screen.getByText('Submit Order')).toBeDisabled();
    resolveCreate({ ...plans[0], status: 'DRAFT' });
  });

  it('shows inventory count and history in inventory mode without order or order plan workspaces', async () => {
    render(<OrderingScreen mode="inventory" />, { wrapper: TestProviders });

    expect((await screen.findAllByText('Inventory Count')).length).toBeGreaterThan(0);
    expect(screen.getByText('History')).toBeTruthy();
    expect(screen.queryByText('Order Plans')).toBeNull();
    expect(screen.queryByText('Vendor selection')).toBeNull();
    expect(screen.queryByText('Order Review')).toBeNull();
    expect(screen.queryByText('Receiving')).toBeNull();
  });

  it('keeps inventory count reachable for operational employees', async () => {
    (api as jest.Mock).mockImplementation((path: string) => {
      if (path === '/api/me') return Promise.resolve({ ...profile, positions: ['HOST'] });
      if (path.startsWith('/api/vendors')) return Promise.resolve(vendors);
      if (path.startsWith('/api/order-products')) return Promise.resolve(productsForPath(path));
      if (path.startsWith('/api/purchase-orders?')) return Promise.resolve(orders);
      if (path.startsWith('/api/inventory-counts?')) return Promise.resolve(counts);
      if (path.startsWith('/api/order-plans?')) return Promise.resolve(plans);
      return Promise.resolve({});
    });

    render(<OrderingScreen />, { wrapper: TestProviders });

    fireEvent.press(await screen.findByText('Start Inventory Count'));
    await selectFrontInventoryBusiness();
    expect(await screen.findByText('Inventory Count · BiangBiang Front')).toBeTruthy();
    expect(await screen.findByLabelText('Quantity Black (Dry mix)')).toBeTruthy();
    expect(screen.queryByText('Business Partner only')).toBeNull();

    fireEvent.press(screen.getByText('Order Review'));
    expect(await screen.findByText('Business Partner only')).toBeTruthy();
    expect(screen.queryByText('Approve')).toBeNull();
    expect(screen.queryByText('Reject')).toBeNull();
  });

  it('shows the authenticated submitter before submitting a vendor purchase order', async () => {
    const alert = jest.spyOn(Alert, 'alert').mockImplementation(jest.fn());
    render(<OrderingScreen />, { wrapper: TestProviders });

    fireEvent.press(await screen.findByText('Vendor selection'));
    fireEvent.press(await screen.findByText('Wellpack'));
    expect(await screen.findByText('This order will be submitted as')).toBeTruthy();
    expect(screen.getByText('Alex')).toBeTruthy();

    fireEvent.press(screen.getByText('Submit for Approval'));
    expect(alert).toHaveBeenCalledWith(
      'Submit for Approval',
      'This order will be submitted as: Alex',
      expect.any(Array)
    );
    alert.mockRestore();
  });

  it('submits inventory counts as the authenticated employee', async () => {
    render(<OrderingScreen />, { wrapper: TestProviders });

    fireEvent.press(await screen.findByText('Start Inventory Count'));
    await selectFrontInventoryBusiness();
    fireEvent.changeText(await screen.findByLabelText('Quantity Black (Dry mix)'), '');
    fireEvent.press(screen.getByText('Submit Inventory'));

    await waitFor(() => expect(api).toHaveBeenCalledWith('/api/inventory-counts', expect.objectContaining({ method: 'POST' })));
    const createCountCall = (api as jest.Mock).mock.calls.find(([path, init]) => path === '/api/inventory-counts' && init?.method === 'POST');
    expect(JSON.parse(createCountCall?.[1]?.body as string)).toMatchObject({ locationCode: 'SEATTLE', inventoryBusiness: 'BIANGBIANG_FRONT' });
    await waitFor(() => expect(api).toHaveBeenCalledWith('/api/inventory-counts/40/lines', {
      method: 'PUT',
      body: JSON.stringify({ lines: [{ productId: 10, quantityOnHand: 0, unit: 'BOX', notes: '' }] })
    }));
    await waitFor(() => expect(api).toHaveBeenCalledWith('/api/inventory-counts/40/submit', { method: 'POST' }));
    expect(await screen.findByText('Inventory submitted successfully')).toBeTruthy();
    await waitFor(() => {
      const countListRequests = (api as jest.Mock).mock.calls.filter(([path]) => String(path).startsWith('/api/inventory-counts?'));
      expect(countListRequests.length).toBeGreaterThanOrEqual(2);
    });
  });

  it('shows inventory submit progress and preserves input when submit fails', async () => {
    let rejectSubmit: (error: Error) => void = () => undefined;
    const submitPromise = new Promise((_resolve, reject) => {
      rejectSubmit = reject;
    });
    (api as jest.Mock).mockImplementation((path: string, init?: RequestInit) => {
      if (path.includes('/api/inventory-counts/') && path.includes('/submit')) return submitPromise;
      return defaultApiResponse(path, init);
    });
    render(<OrderingScreen mode="inventory" />, { wrapper: TestProviders });

    await openFrontInventoryCount();
    fireEvent.changeText(screen.getByLabelText('Quantity Black (Dry mix)'), '6');
    fireEvent.press(screen.getByTestId('inventory-submit-button'));

    expect(await screen.findByText('Submitting inventory...')).toBeTruthy();
    await waitFor(() => expect(api).toHaveBeenCalledWith('/api/inventory-counts', expect.objectContaining({ method: 'POST' })));
    await waitFor(() => expect(api).toHaveBeenCalledWith('/api/inventory-counts/40/lines', {
      method: 'PUT',
      body: JSON.stringify({ lines: [
        { productId: 10, quantityOnHand: 6, unit: 'BOX', notes: '' }
      ] })
    }));
    rejectSubmit(new Error('offline'));
    expect(await screen.findByText('Unable to submit inventory')).toBeTruthy();
    expect(await screen.findByDisplayValue('6')).toBeTruthy();
    expect(screen.queryByText('Inventory submitted successfully')).toBeNull();
  });

  it('submits inventory through the test id even when no quantities were touched', async () => {
    render(<OrderingScreen mode="inventory" />, { wrapper: TestProviders });

    await selectFrontInventoryBusiness();
    await screen.findByLabelText('Quantity Black (Dry mix)');
    fireEvent.press(await screen.findByTestId('inventory-submit-button'));

    await waitFor(() => expect(api).toHaveBeenCalledWith('/api/inventory-counts', expect.objectContaining({ method: 'POST' })));
    await waitFor(() => expect(api).toHaveBeenCalledWith('/api/inventory-counts/40/lines', {
      method: 'PUT',
      body: JSON.stringify({ lines: [
        { productId: 10, quantityOnHand: 0, unit: 'BOX', notes: '' },
        { productId: 12, quantityOnHand: 0, unit: 'BOTTLE', notes: '' }
      ] })
    }));
    await waitFor(() => expect(api).toHaveBeenCalledWith('/api/inventory-counts/40/submit', { method: 'POST' }));
    expect(await screen.findByText('Inventory submitted successfully')).toBeTruthy();
  });

  it('supports vendor rail inventory counts, current-vendor search, summary, and draft saving', async () => {
    render(<OrderingScreen mode="inventory" />, { wrapper: TestProviders });

    await openFrontInventoryCount();
    expect((await screen.findAllByText('Wellpack')).length).toBeGreaterThan(0);
    expect(screen.queryByText('Local Purchase / No Delivery')).toBeNull();
    expect(screen.getByText('Black (Dry mix)')).toBeTruthy();
    expect(screen.queryByText('Emergency gloves')).toBeNull();
    expect(screen.getByText('Counted Items')).toBeTruthy();
    expect(screen.getByText('Vendor Products')).toBeTruthy();
    expect(screen.getByText('Total Counted Quantity')).toBeTruthy();
    const vendorRailStyle = StyleSheet.flatten(screen.getByLabelText('Vendor selection').props.style);
    expect(vendorRailStyle.width).toBeGreaterThanOrEqual(104);
    expect(vendorRailStyle.width).toBeLessThanOrEqual(120);
    expect(vendorRailStyle.maxWidth).toBeLessThanOrEqual(140);

    expect(screen.getAllByDisplayValue('').length).toBeGreaterThan(0);
    expect(screen.getByText('box')).toBeTruthy();
    expect(screen.queryByText('case')).toBeNull();
    expect(screen.queryByText('Current Inventory')).toBeNull();
    expect(screen.queryByText('BOX')).toBeNull();
    expect(screen.queryByText('Par level')).toBeNull();
    expect(screen.queryByText('Nothing here yet')).toBeNull();
    expect(screen.getByText('This inventory will be submitted as')).toBeTruthy();
    expect(screen.queryByText('This order will be submitted as')).toBeNull();
    const quantityInputStyle = StyleSheet.flatten(screen.getByLabelText('Quantity Black (Dry mix)').props.style);
    expect(quantityInputStyle.width).toBeGreaterThanOrEqual(72);
    expect(quantityInputStyle.width).toBeLessThanOrEqual(96);
    expect(quantityInputStyle.minHeight).toBeGreaterThanOrEqual(40);
    expect(quantityInputStyle.minHeight).toBeLessThanOrEqual(48);

    fireEvent.changeText(screen.getByLabelText('Quantity Black (Dry mix)'), '4');
    expect(await screen.findByDisplayValue('4')).toBeTruthy();
    expect(screen.getAllByText('4').length).toBeGreaterThanOrEqual(1);

    fireEvent.changeText(screen.getByLabelText('Search products'), 'missing');
    expect(await screen.findByText('No products found')).toBeTruthy();
    expect(screen.queryByText('Emergency gloves')).toBeNull();
    fireEvent.changeText(screen.getByLabelText('Search products'), '');

    fireEvent.changeText(screen.getByLabelText('Quantity Black (Dry mix)'), '0');
    expect(await screen.findByDisplayValue('0')).toBeTruthy();
    fireEvent.changeText(screen.getByLabelText('Quantity Black (Dry mix)'), '-3');
    expect((await screen.findAllByDisplayValue('')).length).toBeGreaterThan(0);
    fireEvent.changeText(screen.getByLabelText('Quantity Black (Dry mix)'), 'abc');
    expect((await screen.findAllByDisplayValue('')).length).toBeGreaterThan(0);
    fireEvent.changeText(screen.getByLabelText('Quantity Black (Dry mix)'), '');
    expect(screen.queryByText('Save Draft')).toBeNull();
    expect(screen.getAllByText('Submit Inventory').length).toBe(1);

    fireEvent.press(screen.getByText('Change business'));
    fireEvent.press(await screen.findByText('Leave Without Saving'));
    fireEvent.press(await screen.findByText('Paper Fan'));
    expect(await screen.findByText('Emergency gloves')).toBeTruthy();
    expect(screen.queryByText('Black (Dry mix)')).toBeNull();
    expect(screen.getAllByDisplayValue('').length).toBeGreaterThan(0);
  });

  it('allows business partners to manage inventory catalog items for the selected business', async () => {
    render(<OrderingScreen mode="inventory" />, { wrapper: TestProviders });

    await openFrontInventoryCount();
    fireEvent.press(await screen.findByText('Manage items'));
    expect(await screen.findByText('Done')).toBeTruthy();
    expect(screen.getByText('Black (Dry mix)')).toBeTruthy();
    expect(screen.getByLabelText('Quantity Black (Dry mix)')).toBeTruthy();
    expect(screen.getByTestId('inventory-edit-item-10')).toBeTruthy();
    expect(screen.getByTestId('inventory-delete-item-10')).toBeTruthy();
    fireEvent.changeText(await screen.findByLabelText('Item name'), 'Tsingdao beer');
    fireEvent.changeText(screen.getByLabelText('Unit'), 'bt');
    fireEvent.changeText(screen.getByLabelText('Vendor'), '');
    fireEvent.press(screen.getAllByText('Add item').at(-1)!);

    await waitFor(() => expect(api).toHaveBeenCalledWith('/api/inventory-catalog', expect.objectContaining({
      method: 'POST',
      body: JSON.stringify({ locationCode: 'SEATTLE', inventoryBusiness: 'BIANGBIANG_FRONT', name: 'Tsingdao beer', unit: 'bt', vendorName: null })
    })));
    expect(await screen.findByText('Inventory item added')).toBeTruthy();
    await waitFor(() => expect(screen.getByText('Tsingdao beer')).toBeTruthy());
    expect(screen.getAllByText('Unknown vendor').length).toBeGreaterThan(0);
    expect(screen.getByTestId('inventory-edit-item-13')).toBeTruthy();

    fireEvent.press(screen.getByTestId('inventory-edit-item-13'));
    expect(await screen.findByDisplayValue('Tsingdao beer')).toBeTruthy();
    fireEvent.press(screen.getByText('Save'));
    await waitFor(() => expect(api).toHaveBeenCalledWith('/api/inventory-catalog/13', expect.objectContaining({ method: 'PATCH' })));
    expect(await screen.findByText('Inventory item updated')).toBeTruthy();

    fireEvent.press(screen.getByText('Change business'));
    fireEvent.press(await screen.findByText('Paper Fan'));
    expect(await screen.findByText('Emergency gloves')).toBeTruthy();
    expect(screen.queryByText('Tsingdao beer')).toBeNull();

    fireEvent.press(screen.getByText('Manage items'));
    fireEvent.changeText(await screen.findByLabelText('Item name'), 'gunpowder gin');
    fireEvent.changeText(screen.getByLabelText('Unit'), 'bt');
    fireEvent.press(screen.getAllByText('Add item').at(-1)!);

    await waitFor(() => expect(api).toHaveBeenCalledWith('/api/inventory-catalog', expect.objectContaining({
      method: 'POST',
      body: JSON.stringify({ locationCode: 'SEATTLE', inventoryBusiness: 'PAPER_FAN', name: 'gunpowder gin', unit: 'bt', vendorName: null })
    })));
    expect(await screen.findByText('Inventory item added')).toBeTruthy();
    expect(await screen.findByText('gunpowder gin')).toBeTruthy();
    expect(screen.queryByText('Tsingdao beer')).toBeNull();
  });

  it('hides inventory catalog management from regular employees and soft-deletes manager items with confirmation', async () => {
    (api as jest.Mock).mockImplementation((path: string) => {
      if (path === '/api/me') return Promise.resolve({ ...profile, positions: ['HOST'] });
      if (path.startsWith('/api/vendors')) return Promise.resolve(vendors);
      if (path.startsWith('/api/order-products')) return Promise.resolve(productsForPath(path));
      if (path.startsWith('/api/purchase-orders?')) return Promise.resolve(orders);
      if (path.startsWith('/api/inventory-counts?')) return Promise.resolve(counts);
      if (path.startsWith('/api/order-plans?')) return Promise.resolve(plans);
      return Promise.resolve({});
    });
    const employeeRender = render(<OrderingScreen mode="inventory" />, { wrapper: TestProviders });
    await openFrontInventoryCount();
    expect(screen.queryByText('Manage items')).toBeNull();
    employeeRender.unmount();

    (api as jest.Mock).mockImplementation((path: string, init?: RequestInit) => {
      if (path === '/api/me') return Promise.resolve(profile);
      if (path.startsWith('/api/vendors')) return Promise.resolve(vendors);
      if (path.startsWith('/api/order-products')) return Promise.resolve(productsForPath(path));
      if (path.startsWith('/api/inventory-catalog/') && init?.method === 'DELETE') return Promise.resolve(deleteInventoryCatalogProduct(path));
      return defaultApiResponse(path, init);
    });
    render(<OrderingScreen mode="inventory" />, { wrapper: TestProviders });
    await openFrontInventoryCount();
    expect(await screen.findByText('Yuzu Sparkling Sake')).toBeTruthy();
    fireEvent.press(screen.getByText('Manage items'));
    expect(await screen.findByText('Done')).toBeTruthy();
    expect(screen.getByLabelText('Quantity Yuzu Sparkling Sake')).toBeTruthy();
    fireEvent.press(await screen.findByTestId('inventory-delete-item-12'));
    expect(await screen.findByText('Delete item Yuzu Sparkling Sake?')).toBeTruthy();
    expect(screen.getByText('This item will be removed from future counts, but history will remain available.')).toBeTruthy();
    fireEvent.press(screen.getAllByText('Delete item').at(-1)!);
    await waitFor(() => expect(api).toHaveBeenCalledWith('/api/inventory-catalog/12', { method: 'DELETE' }));
    expect(await screen.findByText('Inventory item deleted')).toBeTruthy();
    await waitFor(() => expect(screen.queryByText('Yuzu Sparkling Sake')).toBeNull());
  });

  it('prompts before leaving dirty inventory edits and supports save or discard choices', async () => {
    const firstRender = render(<OrderingScreen mode="inventory" />, { wrapper: TestProviders });

    await openFrontInventoryCount();
    fireEvent.changeText(screen.getByLabelText('Quantity Black (Dry mix)'), '');
    fireEvent.press(screen.getAllByText('History')[0]);

    expect(await screen.findByText('Save inventory draft before leaving?')).toBeTruthy();
    expect(screen.getByText('Cancel')).toBeTruthy();
    fireEvent.press(screen.getByText('Save and Leave'));
    await waitFor(() => expect(api).toHaveBeenCalledWith('/api/inventory-counts/40/lines', {
      method: 'PUT',
      body: JSON.stringify({ lines: [
        { productId: 10, quantityOnHand: 0, unit: 'BOX', notes: '' }
      ] })
    }));
    const partialBlankPayload = JSON.parse((api as jest.Mock).mock.calls.find(([path]) => path === '/api/inventory-counts/40/lines')?.[1]?.body as string);
    expect(partialBlankPayload.lines).toHaveLength(1);
    expect(partialBlankPayload.lines[0].quantityOnHand).toBe(0);
    expect(typeof partialBlankPayload.lines[0].quantityOnHand).toBe('number');
    expect((await screen.findAllByText('History')).length).toBeGreaterThan(0);
    firstRender.unmount();

    render(<OrderingScreen mode="inventory" />, { wrapper: TestProviders });
    await openFrontInventoryCount();
    fireEvent.changeText(screen.getByLabelText('Quantity Black (Dry mix)'), '3');
    fireEvent.press(screen.getByText('History'));

    expect(await screen.findByText('Save inventory draft before leaving?')).toBeTruthy();
    fireEvent.press(screen.getByText('Leave Without Saving'));
    expect((await screen.findAllByText('History')).length).toBeGreaterThan(0);
  });

  it('guards bottom navigation when inventory edits are dirty', async () => {
    mockedPathname = '/inventory';
    render(<OrderingScreen mode="inventory" />, { wrapper: TestProviders });

    await openFrontInventoryCount();
    fireEvent.changeText(screen.getByLabelText('Quantity Black (Dry mix)'), '2');
    fireEvent.press(screen.getByLabelText('Profile'));

    expect(await screen.findByText('Save inventory draft before leaving?')).toBeTruthy();
    expect(router.replace).not.toHaveBeenCalledWith('/profile');

    fireEvent.press(screen.getByText('Cancel'));
    expect(router.replace).not.toHaveBeenCalledWith('/profile');

    fireEvent.press(screen.getByLabelText('Order'));
    expect(await screen.findByText('Save inventory draft before leaving?')).toBeTruthy();
    fireEvent.press(screen.getByText('Cancel'));
    expect(router.replace).not.toHaveBeenCalledWith('/order');

    fireEvent.press(screen.getByLabelText('Profile'));
    expect(await screen.findByText('Save inventory draft before leaving?')).toBeTruthy();
    fireEvent.press(screen.getByText('Leave Without Saving'));
    expect(api).not.toHaveBeenCalledWith('/api/inventory-counts/40/lines', expect.any(Object));
    expect(router.replace).toHaveBeenCalledWith('/profile');
  });

  it('leaves inventory without a prompt when there are no unsaved edits or after submit succeeds', async () => {
    let inventorySubmitted = false;
    (api as jest.Mock).mockImplementation((path: string, init?: RequestInit) => {
      if (path.startsWith('/api/inventory-counts?')) {
        return Promise.resolve(inventorySubmitted ? [{ ...inventoryDraft, status: 'SUBMITTED', completedByNameSnapshot: 'Alex' }, ...counts] : counts);
      }
      if (path.includes('/api/inventory-counts/') && path.includes('/submit')) {
        inventorySubmitted = true;
        return Promise.resolve({ ...inventoryDraft, status: 'SUBMITTED', completedByNameSnapshot: 'Alex' });
      }
      return defaultApiResponse(path, init);
    });
    const alert = jest.spyOn(Alert, 'alert').mockImplementation((title, message, buttons) => {
      buttons?.find((button) => button.text === 'Confirm')?.onPress?.();
    });
    render(<OrderingScreen mode="inventory" />, { wrapper: TestProviders });

    await selectFrontInventoryBusiness();
    fireEvent.press(await screen.findByText('History'));
    expect(screen.queryByText('Save inventory draft before leaving?')).toBeNull();

    fireEvent.press(screen.getAllByText('Inventory Count')[0]);
    fireEvent.changeText(await screen.findByLabelText('Quantity Black (Dry mix)'), '4');
    fireEvent.press(screen.getByText('Submit Inventory'));
    await waitFor(() => expect(api).toHaveBeenCalledWith('/api/inventory-counts/40/submit', { method: 'POST' }));
    expect(await screen.findByText('Inventory submitted successfully')).toBeTruthy();
    expect(screen.getAllByText('Counted by').length).toBeGreaterThan(0);
    expect(screen.getAllByText('Alex').length).toBeGreaterThan(0);
    fireEvent.press(screen.getAllByText('History')[0]);
    expect(screen.queryByText('Save inventory draft before leaving?')).toBeNull();
    alert.mockRestore();
  });

  it('prompts to continue or start a new draft inventory count on entry', async () => {
    const draftWithLine = { ...inventoryDraft, lines: [{ productId: 10, quantityOnHand: 5, unit: 'BOX', notes: '' }] };
    (api as jest.Mock).mockImplementation((path: string, init?: RequestInit) => {
      if (path.startsWith('/api/inventory-counts?')) return Promise.resolve([draftWithLine]);
      return defaultApiResponse(path, init);
    });

    const firstRender = render(<OrderingScreen mode="inventory" />, { wrapper: TestProviders });
    await selectFrontInventoryBusiness();
    expect(await screen.findByText('Continue previous inventory count?')).toBeTruthy();
    fireEvent.press(screen.getByText('Continue Editing'));
    expect(await screen.findByDisplayValue('5')).toBeTruthy();
    fireEvent.changeText(screen.getByLabelText('Quantity Black (Dry mix)'), '6');
    fireEvent.press(screen.getByText('History'));
    fireEvent.press(await screen.findByText('Save and Leave'));
    await waitFor(() => expect(api).toHaveBeenCalledWith('/api/inventory-counts/40/lines', expect.objectContaining({ method: 'PUT' })));
    firstRender.unmount();

    render(<OrderingScreen mode="inventory" />, { wrapper: TestProviders });
    await selectFrontInventoryBusiness();
    expect(await screen.findByText('Continue previous inventory count?')).toBeTruthy();
    fireEvent.press(screen.getByText('Start New Count'));
    expect((await screen.findAllByDisplayValue('')).length).toBeGreaterThan(0);
    fireEvent.changeText(screen.getByLabelText('Quantity Black (Dry mix)'), '0');
    fireEvent.press(screen.getByText('History'));
    fireEvent.press(await screen.findByText('Save and Leave'));
    await waitFor(() => expect(api).toHaveBeenCalledWith('/api/inventory-counts', expect.objectContaining({ method: 'POST' })));
    await waitFor(() => expect(api).toHaveBeenCalledWith('/api/inventory-counts/40/lines', {
      method: 'PUT',
      body: JSON.stringify({ lines: [
        { productId: 10, quantityOnHand: 0, unit: 'BOX', notes: '' }
      ] })
    }));
  });

  it('blocks inventory navigation while saving and keeps local input after save errors', async () => {
    let resolveSave: (count: typeof inventoryDraft) => void = () => undefined;
    (api as jest.Mock).mockImplementation((path: string, init?: RequestInit) => {
      if (path.includes('/api/inventory-counts/') && path.includes('/lines')) return new Promise((resolve) => { resolveSave = resolve; });
      return defaultApiResponse(path, init);
    });

    const pendingRender = render(<OrderingScreen mode="inventory" />, { wrapper: TestProviders });
    await openFrontInventoryCount();
    fireEvent.changeText(screen.getByLabelText('Quantity Black (Dry mix)'), '6');
    fireEvent.press(screen.getByText('History'));
    expect(await screen.findByText('Save inventory draft before leaving?')).toBeTruthy();
    fireEvent.press(screen.getByText('Save and Leave'));
    await waitFor(() => expect(screen.getByLabelText('Submit Inventory')).toBeDisabled());
    resolveSave(inventoryDraft);
    pendingRender.unmount();

    (api as jest.Mock).mockImplementation((path: string, init?: RequestInit) => {
      if (path.includes('/api/inventory-counts/') && path.includes('/lines')) return Promise.reject(new Error('offline'));
      return defaultApiResponse(path, init);
    });
    render(<OrderingScreen mode="inventory" />, { wrapper: TestProviders });
    await openFrontInventoryCount();
    fireEvent.changeText(screen.getByLabelText('Quantity Black (Dry mix)'), '7');
    fireEvent.press(screen.getByText('History'));
    expect(await screen.findByText('Save inventory draft before leaving?')).toBeTruthy();
    fireEvent.press(screen.getByText('Save and Leave'));
    expect(await screen.findByText('Unable to save changes')).toBeTruthy();
    expect(screen.getAllByText('Unable to save changes')).toHaveLength(1);
    expect(await screen.findByDisplayValue('7')).toBeTruthy();
    expect(screen.queryByText('No submitted inventory counts yet')).toBeNull();
    fireEvent.changeText(screen.getByLabelText('Quantity Black (Dry mix)'), '8');
    expect(screen.queryByText('Unable to save changes')).toBeNull();
  });

  it('keeps inventory history separate from purchase order history', async () => {
    (api as jest.Mock).mockImplementation((path: string, init?: RequestInit) => {
      if (path.startsWith('/api/inventory-counts?')) return Promise.resolve([
        { ...inventoryDraft, id: 41, status: 'DRAFT', businessDate: '2026-06-17', lines: [] },
        { ...inventoryDraft, id: 42, status: 'IN_PROGRESS', businessDate: '2026-06-18', lines: [] },
        counts[0],
        counts[1]
      ]);
      return defaultApiResponse(path, init);
    });
    render(<OrderingScreen mode="inventory" />, { wrapper: TestProviders });

    fireEvent.press(await screen.findByText('History'));
    expect((await screen.findAllByText('Inventory Count')).length).toBeGreaterThanOrEqual(1);
    expect(screen.getByText('All businesses')).toBeTruthy();
    expect(screen.getByText('Latest first')).toBeTruthy();
    expect(screen.getAllByText('Counted by').length).toBeGreaterThanOrEqual(2);
    expect(screen.getByText('Mary')).toBeTruthy();
    expect(screen.getByText('Hannah')).toBeTruthy();
    expect(screen.getAllByText('Submitted').length).toBeGreaterThanOrEqual(2);
    expect(screen.getAllByText('Export PDF').length).toBeGreaterThan(0);
    const historyJson = JSON.stringify(screen.toJSON());
    expect(historyJson.indexOf('2026-06-16')).toBeGreaterThanOrEqual(0);
    expect(historyJson.indexOf('2026-06-16')).toBeLessThan(historyJson.indexOf('2026-06-15'));
    fireEvent.press(screen.getByLabelText('Business BiangBiang Front'));
    expect(screen.getByText('Mary')).toBeTruthy();
    expect(screen.queryByText('Hannah')).toBeNull();
    fireEvent.press(screen.getByLabelText('Business Paper Fan'));
    expect(screen.queryByText('Mary')).toBeNull();
    expect(screen.getByText('Hannah')).toBeTruthy();
    fireEvent.press(screen.getByLabelText('Business All businesses'));
    expect(screen.getByText('Mary')).toBeTruthy();
    expect(screen.getByText('Hannah')).toBeTruthy();
    expect(screen.queryByText('DRAFT')).toBeNull();
    expect(screen.queryByText('IN_PROGRESS')).toBeNull();
    expect(screen.queryByText('2026-06-17')).toBeNull();
    expect(screen.queryByText('2026-06-18')).toBeNull();
    expect(screen.queryByText('Order Plans')).toBeNull();
    expect(screen.queryByText('PO-20260616-WELLPACK-001')).toBeNull();
  });

  it('exports submitted inventory count PDFs through the authenticated binary endpoint', async () => {
    let resolveBinary: (value: typeof pdfBytes) => void = () => undefined;
    (apiBinary as jest.Mock).mockReturnValue(new Promise((resolve) => { resolveBinary = resolve; }));

    render(<OrderingScreen mode="inventory" />, { wrapper: TestProviders });

    fireEvent.press(await screen.findByText('History'));
    fireEvent.press((await screen.findAllByText('Export PDF'))[0]);

    expect(await screen.findByText('Exporting PDF...')).toBeTruthy();
    await waitFor(() => expect(apiBinary).toHaveBeenCalledWith('/api/inventory-counts/61/pdf'));
    resolveBinary({ ...pdfBytes, filename: 'inventory-count-61.pdf' });
    await waitFor(() => expect(viewPdfFile).toHaveBeenCalledWith(expect.objectContaining({ filename: 'inventory-count-61.pdf' }), 'inventory-count-61.pdf', pdfTarget));
    expect(screen.queryByText('Unable to export PDF')).toBeNull();
  });

  it('shows a readable inventory PDF export failure without changing history filtering', async () => {
    (apiBinary as jest.Mock).mockRejectedValueOnce(new Error('offline'));
    (api as jest.Mock).mockImplementation((path: string, init?: RequestInit) => {
      if (path.startsWith('/api/inventory-counts?')) return Promise.resolve([
        { ...inventoryDraft, id: 41, status: 'DRAFT', businessDate: '2026-06-17', lines: [] },
        { ...inventoryDraft, id: 42, status: 'IN_PROGRESS', businessDate: '2026-06-18', lines: [] },
        counts[0]
      ]);
      return defaultApiResponse(path, init);
    });

    render(<OrderingScreen mode="inventory" />, { wrapper: TestProviders });

    fireEvent.press(await screen.findByText('History'));
    fireEvent.press(await screen.findByText('Export PDF'));

    await waitFor(() => expect(apiBinary).toHaveBeenCalledWith('/api/inventory-counts/60/pdf'));
    expect(await screen.findByText('Unable to export PDF')).toBeTruthy();
    expect(screen.queryByText('DRAFT')).toBeNull();
    expect(screen.queryByText('IN_PROGRESS')).toBeNull();
  });

  it('shows an empty inventory history state when only draft or in-progress counts exist', async () => {
    (api as jest.Mock).mockImplementation((path: string, init?: RequestInit) => {
      if (path.startsWith('/api/inventory-counts?')) return Promise.resolve([
        { ...inventoryDraft, id: 41, status: 'DRAFT', businessDate: '2026-06-17', lines: [{ productId: 10, quantityOnHand: 5, unit: 'BOX', notes: '' }] },
        { ...inventoryDraft, id: 42, status: 'IN_PROGRESS', businessDate: '2026-06-18', lines: [] }
      ]);
      return defaultApiResponse(path, init);
    });
    render(<OrderingScreen mode="inventory" />, { wrapper: TestProviders });
    await selectFrontInventoryBusiness();
    expect(await screen.findByText('Continue previous inventory count?')).toBeTruthy();
    fireEvent.press(screen.getByText('Continue Editing'));
    expect(await screen.findByDisplayValue('5')).toBeTruthy();

    fireEvent.press(screen.getByText('History'));
    expect(await screen.findByText('No submitted inventory counts yet')).toBeTruthy();
    expect(screen.queryByText('DRAFT')).toBeNull();
    expect(screen.queryByText('IN_PROGRESS')).toBeNull();
  });

  it('shows separate order planning with selected inventory source and server suggestions', async () => {
    render(<OrderingScreen />, { wrapper: TestProviders });

    fireEvent.press(await screen.findByText('Order Plans'));
    expect(await screen.findByText('2026-06-15 · Mary')).toBeTruthy();
    expect((await screen.findAllByText('Mary')).length).toBeGreaterThanOrEqual(1);
    expect(await screen.findByText('6')).toBeTruthy();
    expect(await screen.findByDisplayValue('7')).toBeTruthy();
  });

  it('shows order plan PDF metadata without fetching PDF bytes into the metadata query', async () => {
    (api as jest.Mock).mockImplementation((path: string, init?: RequestInit) => {
      if (path === '/api/order-plans/70/pdf') return Promise.resolve(orderPlanPdfMetadata);
      return defaultApiResponse(path, init);
    });

    render(<OrderingScreen />, { wrapper: TestProviders });

    fireEvent.press(await screen.findByText('Order Plans'));
    expect(await screen.findByText('Session PDF')).toBeTruthy();
    expect(await screen.findByText('order-plan-70-v1.pdf')).toBeTruthy();
    expect(await screen.findByText('Manager One')).toBeTruthy();
    expect(await screen.findByText('12.0 KB')).toBeTruthy();
    expect(apiBinary).not.toHaveBeenCalled();
  });

  it('shows a non-error order plan PDF empty state for 404 and does not retry it', async () => {
    render(<OrderingScreen />, { wrapper: TestProviders });

    fireEvent.press(await screen.findByText('Order Plans'));
    expect(await screen.findByText('PDF will be available after this order session is submitted.')).toBeTruthy();
    expect(screen.queryByText('View PDF')).toBeNull();
    expect(screen.queryByText('Download PDF')).toBeNull();

    await waitFor(() => {
      expect((api as jest.Mock).mock.calls.filter(([path]) => path === '/api/order-plans/70/pdf')).toHaveLength(1);
    });
  });

  it('views and downloads order plan PDFs through authenticated binary endpoints', async () => {
    (api as jest.Mock).mockImplementation((path: string, init?: RequestInit) => {
      if (path === '/api/order-plans/70/pdf') return Promise.resolve(orderPlanPdfMetadata);
      return defaultApiResponse(path, init);
    });

    render(<OrderingScreen />, { wrapper: TestProviders });

    fireEvent.press(await screen.findByText('Order Plans'));
    fireEvent.press(await screen.findByText('View PDF'));
    expect(preparePdfViewTarget).toHaveBeenCalledTimes(1);
    await waitFor(() => expect(apiBinary).toHaveBeenCalledWith('/api/order-plans/70/pdf/view'));
    expect((preparePdfViewTarget as jest.Mock).mock.invocationCallOrder[0]).toBeLessThan((apiBinary as jest.Mock).mock.invocationCallOrder[0]);
    await waitFor(() => expect(viewPdfFile).toHaveBeenCalledWith(pdfBytes, 'order-plan-70-v1.pdf', pdfTarget));
    expect((apiBinary as jest.Mock).mock.calls[0][0]).not.toContain('?');

    fireEvent.press(await screen.findByText('Download PDF'));
    await waitFor(() => expect(apiBinary).toHaveBeenCalledWith('/api/order-plans/70/pdf/download'));
    expect(downloadPdfFile).toHaveBeenCalledWith(pdfBytes, 'order-plan-70-v1.pdf');
    expect((apiBinary as jest.Mock).mock.calls[1][0]).not.toContain('?');
  });

  it('does not fetch the order plan PDF when the popup is blocked', async () => {
    (preparePdfViewTarget as jest.Mock).mockReturnValue(null);
    (api as jest.Mock).mockImplementation((path: string, init?: RequestInit) => {
      if (path === '/api/order-plans/70/pdf') return Promise.resolve(orderPlanPdfMetadata);
      return defaultApiResponse(path, init);
    });

    render(<OrderingScreen />, { wrapper: TestProviders });

    fireEvent.press(await screen.findByText('Order Plans'));
    fireEvent.press(await screen.findByText('View PDF'));

    expect(apiBinary).not.toHaveBeenCalled();
    expect(viewPdfFile).not.toHaveBeenCalled();
    expect(await screen.findByText('Unable to open PDF')).toBeTruthy();
  });

  it('closes the pre-opened PDF window when the authenticated view request fails', async () => {
    (apiBinary as jest.Mock).mockRejectedValue(new ApiError('NETWORK_ERROR', 500));
    (api as jest.Mock).mockImplementation((path: string, init?: RequestInit) => {
      if (path === '/api/order-plans/70/pdf') return Promise.resolve(orderPlanPdfMetadata);
      return defaultApiResponse(path, init);
    });

    render(<OrderingScreen />, { wrapper: TestProviders });

    fireEvent.press(await screen.findByText('Order Plans'));
    fireEvent.press(await screen.findByText('View PDF'));

    await waitFor(() => expect(pdfTarget.close).toHaveBeenCalledTimes(1));
    expect(await screen.findByText('Unable to reach the server for this PDF')).toBeTruthy();
  });

  it('writes the PDF Blob URL only to the pre-opened target and revokes it later', async () => {
    const actual = jest.requireActual('../src/utils/orderPlanPdfFile') as typeof import('../src/utils/orderPlanPdfFile');
    const originalPlatform = Platform.OS;
    const originalLocation = window.location;
    Object.defineProperty(window, 'location', { configurable: true, value: { href: 'http://localhost:8081/ordering' } });
    const originalHref = window.location.href;
    const originalCreateObjectUrl = URL.createObjectURL;
    const originalRevokeObjectUrl = URL.revokeObjectURL;
    const objectUrl = 'blob:http://localhost/order-plan-70';
    const target = { location: { replace: jest.fn() }, close: jest.fn() } as unknown as Window;
    Object.defineProperty(Platform, 'OS', { configurable: true, value: 'web' });
    Object.defineProperty(URL, 'createObjectURL', { configurable: true, value: jest.fn(() => objectUrl) });
    Object.defineProperty(URL, 'revokeObjectURL', { configurable: true, value: jest.fn() });
    jest.useFakeTimers();
    try {
      await actual.viewPdfFile(pdfBytes, 'order-plan-70-v1.pdf', target);

      expect(target.location.replace).toHaveBeenCalledWith(objectUrl);
      expect(window.location.href).toBe(originalHref);
      expect(URL.revokeObjectURL).not.toHaveBeenCalled();
      jest.advanceTimersByTime(59999);
      expect(URL.revokeObjectURL).not.toHaveBeenCalled();
      jest.advanceTimersByTime(1);
      expect(URL.revokeObjectURL).toHaveBeenCalledWith(objectUrl);
    } finally {
      jest.useRealTimers();
      Object.defineProperty(window, 'location', { configurable: true, value: originalLocation });
      Object.defineProperty(Platform, 'OS', { configurable: true, value: originalPlatform });
      Object.defineProperty(URL, 'createObjectURL', { configurable: true, value: originalCreateObjectUrl });
      Object.defineProperty(URL, 'revokeObjectURL', { configurable: true, value: originalRevokeObjectUrl });
    }
  });

  it('disables PDF buttons while loading and shows readable API errors', async () => {
    let resolveBinary: (value: typeof pdfBytes) => void = () => undefined;
    (apiBinary as jest.Mock).mockReturnValue(new Promise((resolve) => { resolveBinary = resolve; }));
    (api as jest.Mock).mockImplementation((path: string, init?: RequestInit) => {
      if (path === '/api/order-plans/70/pdf') return Promise.resolve(orderPlanPdfMetadata);
      return defaultApiResponse(path, init);
    });

    const firstRender = render(<OrderingScreen />, { wrapper: TestProviders });
    fireEvent.press(await screen.findByText('Order Plans'));
    fireEvent.press(await screen.findByText('View PDF'));
    await waitFor(() => expect(screen.getByText('View PDF')).toBeDisabled());
    fireEvent.press(screen.getByText('View PDF'));
    expect(apiBinary).toHaveBeenCalledTimes(1);
    resolveBinary(pdfBytes);
    firstRender.unmount();

    (apiBinary as jest.Mock).mockRejectedValue(new ApiError('AUTH_STORE_REQUIRED', 403));
    render(<OrderingScreen />, { wrapper: TestProviders });
    fireEvent.press(await screen.findByText('Order Plans'));
    fireEvent.press(await screen.findByText('Download PDF'));
    expect(await screen.findByText('You do not have permission to access this PDF')).toBeTruthy();
  });

  it('submits order plans through the order-plan endpoint with authenticated submitter confirmation', async () => {
    const alert = jest.spyOn(Alert, 'alert').mockImplementation((title, message, buttons) => {
      buttons?.find((button) => button.text === 'Confirm')?.onPress?.();
    });
    render(<OrderingScreen />, { wrapper: TestProviders });

    fireEvent.press(await screen.findByText('Order Plans'));
    fireEvent.press(await screen.findByText('Submit for Approval'));

    expect(alert).toHaveBeenCalledWith(
      'Submit for Approval',
      'This order will be submitted as: Alex',
      expect.any(Array)
    );
    await waitFor(() => expect(api).toHaveBeenCalledWith('/api/order-plans/70/submit', { method: 'POST' }));
    alert.mockRestore();
  });


  it('hides PDF actions before approval and opens approved purchase order PDFs through the authenticated binary endpoint', async () => {
    render(<OrderingScreen />, { wrapper: TestProviders });

    fireEvent.press(await screen.findByText('Order Review'));
    expect(await screen.findByText('Approved PDF is not available yet')).toBeTruthy();
    expect(screen.queryByText('Download PDF')).toBeNull();

    (api as jest.Mock).mockImplementation((path: string) => {
      if (path === '/api/me') return Promise.resolve(profile);
      if (path.startsWith('/api/vendors')) return Promise.resolve(vendors);
      if (path.startsWith('/api/order-products')) return Promise.resolve(products);
      if (path.startsWith('/api/purchase-orders?')) return Promise.resolve([approvedOrder]);
      if (path.startsWith('/api/inventory-counts?')) return Promise.resolve([]);
      return Promise.resolve(approvedOrder);
    });

    render(<OrderingScreen />, { wrapper: TestProviders });
    fireEvent.press(await screen.findByText('Order Review'));
    expect(await screen.findByText('Submitted by')).toBeTruthy();
    expect(screen.getByText('Orderer One')).toBeTruthy();
    expect(screen.getByText('Approved by')).toBeTruthy();
    expect(screen.getByText('Manager One')).toBeTruthy();
    expect(screen.getByText('View Approved PDF')).toBeTruthy();
    expect(screen.getByText('Download PDF')).toBeTruthy();

    fireEvent.press(screen.getByText('View Approved PDF'));
    await waitFor(() => expect(preparePdfViewTarget).toHaveBeenCalled());
    await waitFor(() => expect(apiBinary).toHaveBeenCalledWith('/api/purchase-orders/20/pdf'));
    expect(viewPdfFile).toHaveBeenCalledWith(expect.anything(), approvedOrder.pdf.filename, pdfTarget);
  });

  it('renders explicit empty catalog and backend error states', async () => {
    (api as jest.Mock).mockImplementation((path: string) => {
      if (path === '/api/me') return Promise.resolve({ ...profile, positions: ['HOST'] });
      if (path.startsWith('/api/vendors')) return Promise.resolve([]);
      if (path.startsWith('/api/order-products')) return Promise.resolve([]);
      if (path.startsWith('/api/purchase-orders?')) return Promise.resolve([]);
      if (path.startsWith('/api/inventory-counts?')) return Promise.resolve([]);
      if (path.startsWith('/api/order-plans?')) return Promise.resolve([]);
      return Promise.resolve({});
    });

    const emptyRender = render(<OrderingScreen />, { wrapper: TestProviders });
    fireEvent.press(await screen.findByText('Vendor selection'));
    expect(await screen.findByText('Nothing here yet')).toBeTruthy();
    fireEvent.press(screen.getByText('Inventory Count'));
    expect(await screen.findByText('No products for this vendor')).toBeTruthy();
    emptyRender.unmount();

    (api as jest.Mock).mockRejectedValue(new Error('offline'));
    render(<OrderingScreen />, { wrapper: TestProviders });
    expect(await screen.findByText('Unable to load ordering data')).toBeTruthy();
  });

  it('contains Simplified Chinese ordering translations', () => {
    expect(translations.zh.ordering).toBe('订货');
    expect(translations.zh.submitForApproval).toBe('提交审批');
  });
});

const profile = {
  id: 1,
  englishName: 'Alex',
  preferredName: 'Alex',
  displayName: 'Alex',
  email: 'alex@example.com',
  phone: '+12065550101',
  homeStore: 'SEATTLE',
  eligibleStores: ['SEATTLE'],
  positions: ['MANAGER'],
  status: 'ACTIVE',
  createdAt: '2026-06-15T00:00:00Z'
};

const vendors = [
  { id: 1, locationCode: 'SEATTLE', name: 'Wellpack', deliveryDays: ['WEDNESDAY'], active: true },
  { id: 2, locationCode: 'SEATTLE', name: 'No Delivery', deliveryDays: [], active: true }
];

const products = [
  { id: 10, locationCode: 'SEATTLE', vendorId: 1, vendorName: 'Wellpack', vendorProductCode: 'L32B', name: 'Black (Dry mix)', category: 'PACKAGING', inventoryBusiness: 'BIANGBIANG_FRONT', orderBusiness: 'BIANGBIANG_FRONT', orderBusinessName: 'BiangBiang Front', inventoryUnit: 'BOX', inventoryUnitLabel: 'box', orderUnit: 'BOX', orderUnitLabel: 'box', packageSpecification: 'case', unitPrice: 2.5, parLevel: 10, active: true },
  { id: 12, locationCode: 'SEATTLE', vendorId: 1, vendorName: 'Wellpack', vendorProductCode: 'SG-YUZU', name: 'Yuzu Sparkling Sake', category: 'SAKE', inventoryBusiness: 'BIANGBIANG_FRONT', orderBusiness: 'BIANGBIANG_FRONT', orderBusinessName: 'BiangBiang Front', inventoryUnit: 'BOTTLE', inventoryUnitLabel: 'bt', orderUnit: 'CASE', orderUnitLabel: 'case', packageSpecification: 'case', active: true },
  { id: 11, locationCode: 'SEATTLE', vendorId: 2, vendorName: 'No Delivery', vendorProductCode: 'LOCAL-1', name: 'Emergency gloves', category: 'DISPOSABLES', inventoryBusiness: 'PAPER_FAN', orderBusiness: 'PAPER_FAN', orderBusinessName: 'Paper Fan', inventoryUnit: 'BOX', inventoryUnitLabel: 'box', orderUnit: 'BOX', orderUnitLabel: 'box', packageSpecification: 'box', active: true }
];
let catalogProducts: Array<any> = products.map((product) => ({ ...product }));

const line = { id: 100, productId: 10, productNameSnapshot: 'Black (Dry mix)', productCodeSnapshot: 'L32B', packageSpecificationSnapshot: 'case', orderUnitSnapshot: 'BOX', unitPriceSnapshot: 2.5, requestedQuantity: 3, receivedQuantity: 0, lineTotal: 7.5 };

const inventoryDraft = { id: 40, locationCode: 'SEATTLE', inventoryBusiness: 'BIANGBIANG_FRONT', inventoryBusinessName: 'BiangBiang Front', businessDate: '2026-06-16', status: 'DRAFT', lines: [] };

const counts = [
  { id: 60, locationCode: 'SEATTLE', inventoryBusiness: 'BIANGBIANG_FRONT', inventoryBusinessName: 'BiangBiang Front', businessDate: '2026-06-15', status: 'SUBMITTED', completedByNameSnapshot: 'Mary', completedAt: '2026-06-15T17:00:00Z', createdAt: '2026-06-15T16:00:00Z', updatedAt: '2026-06-15T17:00:00Z', lines: [] },
  { id: 61, locationCode: 'SEATTLE', inventoryBusiness: 'PAPER_FAN', inventoryBusinessName: 'Paper Fan', businessDate: '2026-06-16', status: 'SUBMITTED', completedByNameSnapshot: 'Hannah', completedAt: '2026-06-16T17:00:00Z', createdAt: '2026-06-16T16:00:00Z', updatedAt: '2026-06-16T17:00:00Z', lines: [] }
];

const plans = [
  {
    id: 70,
    locationCode: 'SEATTLE',
    sourceInventorySessionId: 60,
    inventoryBusinessDate: '2026-06-15',
    inventoryCompletedByNameSnapshot: 'Mary',
    inventoryCompletedAt: '2026-06-15T17:00:00Z',
    businessDate: '2026-06-16',
    status: 'IN_PROGRESS',
    createdByEmployeeId: 1,
    lines: [
      { id: 701, productId: 10, vendorId: 1, vendorNameSnapshot: 'Wellpack', productCodeSnapshot: 'L32B', productNameSnapshot: 'Black (Dry mix)', packageSpecificationSnapshot: 'case', inventoryUnitSnapshot: 'BOX', orderUnitSnapshot: 'BOX', unitPriceSnapshot: 2.5, sourceInventoryQuantity: 4, parLevelSnapshot: 10, suggestedOrderQuantity: 6, finalOrderQuantity: 7 }
    ],
    vendorOrders: []
  }
];

const orderPlanPdfMetadata = {
  documentId: 7001,
  orderPlanId: 70,
  version: 1,
  filename: 'order-plan-70-v1.pdf',
  mimeType: 'application/pdf',
  byteSize: 12288,
  sha256: 'abc123',
  generatedAt: '2026-06-17T12:00:00Z',
  generatedByEmployeeId: 2,
  generatedByNameSnapshot: 'Manager One',
  current: true
};

const pdfBytes = {
  arrayBuffer: new Uint8Array([37, 80, 68, 70]).buffer,
  blob: new Blob([new Uint8Array([37, 80, 68, 70])], { type: 'application/pdf' }),
  mimeType: 'application/pdf',
  filename: 'order-plan-70-v1.pdf',
  byteSize: 4
};

const orderInventoryReference = {
  inventoryCountId: 60,
  locationCode: 'SEATTLE',
  orderBusiness: 'BIANGBIANG_FRONT',
  orderBusinessName: 'BiangBiang Front',
  countedAt: '2026-06-15T17:00:00Z',
  lines: [
    { inventoryProductId: 10, itemName: 'Black (Dry mix)', normalizedItemName: 'black dry mix', unit: 'box', quantity: 12 }
  ]
};

function defaultApiResponse(path: string, init?: RequestInit) {
  if (path === '/api/me') return Promise.resolve(profile);
  if (path.startsWith('/api/vendors')) return Promise.resolve(vendors);
  if (path.startsWith('/api/order-products')) return Promise.resolve(productsForPath(path));
  if (path === '/api/order-catalog' && init?.method === 'POST') return Promise.resolve(createOrderCatalogProduct(init));
  if (path.startsWith('/api/order-catalog/') && init?.method === 'PATCH') return Promise.resolve(updateOrderCatalogProduct(path, init));
  if (path.startsWith('/api/order-catalog/') && init?.method === 'DELETE') return Promise.resolve(deleteOrderCatalogProduct(path));
  if (path.startsWith('/api/order-inventory-reference')) return Promise.resolve(orderInventoryReference);
  if (path === '/api/inventory-catalog' && init?.method === 'POST') return Promise.resolve(createInventoryCatalogProduct(init));
  if (path.startsWith('/api/inventory-catalog/') && init?.method === 'PATCH') return Promise.resolve(updateInventoryCatalogProduct(path, init));
  if (path.startsWith('/api/inventory-catalog/') && init?.method === 'DELETE') return Promise.resolve(deleteInventoryCatalogProduct(path));
  if (path.startsWith('/api/purchase-orders?')) return Promise.resolve(orders);
  if (path.startsWith('/api/inventory-counts?')) return Promise.resolve(counts);
  if (path.startsWith('/api/order-plans?')) return Promise.resolve(plans);
  if (path === '/api/inventory-counts' && init?.method === 'POST') return Promise.resolve(inventoryDraft);
  if (path.includes('/api/inventory-counts/') && path.includes('/lines')) return Promise.resolve(inventoryDraft);
  if (path.includes('/api/inventory-counts/') && path.includes('/submit')) return Promise.resolve({ ...inventoryDraft, status: 'SUBMITTED', completedByNameSnapshot: 'Alex' });
  if (path === '/api/order-plans' && init?.method === 'POST') return Promise.resolve(plans[0]);
  if (path.includes('/api/order-plans/') && path.includes('/lines')) return Promise.resolve(plans[0]);
  if (path.includes('/api/order-plans/') && path.includes('/submit')) return Promise.resolve({ ...plans[0], status: 'SUBMITTED', submittedByNameSnapshot: 'Alex' });
  if (path.includes('/api/order-plans/') && path.includes('/generate-vendor-orders')) return Promise.resolve(orders);
  if (path === '/api/order-sessions' && init?.method === 'POST') return Promise.resolve({ ...plans[0], status: 'DRAFT', createdByEmployeeId: profile.id, orderBusiness: 'BIANGBIANG_FRONT' });
  if (path.includes('/api/order-sessions/') && path.includes('/lines')) return Promise.resolve({ ...plans[0], status: 'IN_PROGRESS', createdByEmployeeId: profile.id, orderBusiness: 'BIANGBIANG_FRONT' });
  if (path.includes('/api/order-sessions/') && path.includes('/submit')) return Promise.resolve({ ...plans[0], status: 'SUBMITTED', submittedByNameSnapshot: 'Alex', orderBusiness: 'BIANGBIANG_FRONT' });
  if (path === '/api/purchase-orders' && init?.method === 'POST') return Promise.resolve({ ...orders[0], id: 30, status: 'DRAFT', lines: [] });
  if (path.includes('/lines')) return Promise.resolve({ ...orders[0], id: Number(path.match(/purchase-orders\/(\d+)/)?.[1] ?? orders[0].id), status: 'DRAFT', lines: [line] });
  if (path.includes('/submit')) return Promise.resolve({ ...orders[0], status: 'SUBMITTED', lines: [line] });
  return Promise.resolve(orders[0]);
}

function productsForPath(path: string) {
  const source = catalogProducts ?? products;
  if (path.includes('active=true')) {
    const active = source.filter((product) => product.active);
    if (path.includes('inventoryBusiness=BIANGBIANG_FRONT')) return active.filter((product) => product.inventoryBusiness === 'BIANGBIANG_FRONT');
    if (path.includes('inventoryBusiness=PAPER_FAN')) return active.filter((product) => product.inventoryBusiness === 'PAPER_FAN');
    if (path.includes('orderBusiness=BIANGBIANG_FRONT')) return active.filter((product) => product.orderBusiness === 'BIANGBIANG_FRONT');
    if (path.includes('orderBusiness=PAPER_FAN')) return active.filter((product) => product.orderBusiness === 'PAPER_FAN');
    return active;
  }
  if (path.includes('inventoryBusiness=BIANGBIANG_FRONT')) return source.filter((product) => product.inventoryBusiness === 'BIANGBIANG_FRONT');
  if (path.includes('inventoryBusiness=PAPER_FAN')) return source.filter((product) => product.inventoryBusiness === 'PAPER_FAN');
  if (path.includes('orderBusiness=BIANGBIANG_FRONT')) return source.filter((product) => product.orderBusiness === 'BIANGBIANG_FRONT');
  if (path.includes('orderBusiness=PAPER_FAN')) return source.filter((product) => product.orderBusiness === 'PAPER_FAN');
  return source;
}

function createOrderCatalogProduct(init?: RequestInit) {
  const body = JSON.parse(String(init?.body ?? '{}'));
  const product = {
    id: 15,
    locationCode: body.locationCode,
    vendorId: 99,
    vendorName: body.vendorName || 'Unknown vendor',
    vendorProductCode: undefined,
    name: body.name,
    category: 'OTHER',
    orderBusiness: body.orderBusiness,
    orderBusinessName: body.orderBusiness === 'PAPER_FAN' ? 'Paper Fan' : 'BiangBiang Front',
    inventoryUnit: body.unit === 'bt' ? 'BOTTLE' : 'OTHER',
    inventoryUnitLabel: body.unit,
    orderUnit: body.unit === 'bt' ? 'BOTTLE' : 'OTHER',
    orderUnitLabel: body.unit,
    packageSpecification: undefined,
    active: true
  };
  catalogProducts = [...catalogProducts, product];
  return product;
}

function updateOrderCatalogProduct(path: string, init?: RequestInit) {
  const id = Number(path.match(/order-catalog\/(\d+)/)?.[1]);
  const body = JSON.parse(String(init?.body ?? '{}'));
  catalogProducts = catalogProducts.map((product) => product.id === id ? {
    ...product,
    name: body.name,
    vendorName: body.vendorName || 'Unknown vendor',
    inventoryUnit: body.unit === 'bt' ? 'BOTTLE' : 'OTHER',
    inventoryUnitLabel: body.unit,
    orderUnit: body.unit === 'bt' ? 'BOTTLE' : 'OTHER',
    orderUnitLabel: body.unit,
    active: body.active ?? product.active
  } : product);
  return catalogProducts.find((product) => product.id === id);
}

function deleteOrderCatalogProduct(path: string) {
  const id = Number(path.match(/order-catalog\/(\d+)/)?.[1]);
  catalogProducts = catalogProducts.map((product) => product.id === id ? { ...product, active: false } : product);
  return catalogProducts.find((product) => product.id === id);
}

function createInventoryCatalogProduct(init?: RequestInit) {
  const body = JSON.parse(String(init?.body ?? '{}'));
  const product = {
    id: body.name === 'gunpowder gin' ? 14 : 13,
    locationCode: body.locationCode,
    vendorId: 99,
    vendorName: 'Unknown vendor',
    vendorProductCode: undefined,
    name: body.name,
    category: 'OTHER',
    inventoryBusiness: body.inventoryBusiness,
    inventoryUnit: body.unit === 'bt' ? 'BOTTLE' : 'OTHER',
    inventoryUnitLabel: body.unit,
    orderUnit: body.unit === 'bt' ? 'BOTTLE' : 'OTHER',
    orderUnitLabel: body.unit,
    packageSpecification: undefined,
    active: true
  };
  catalogProducts = [...catalogProducts, product];
  return product;
}

function updateInventoryCatalogProduct(path: string, init?: RequestInit) {
  const id = Number(path.match(/inventory-catalog\/(\d+)/)?.[1]);
  const body = JSON.parse(String(init?.body ?? '{}'));
  catalogProducts = catalogProducts.map((product) => product.id === id ? {
    ...product,
    name: body.name,
    vendorName: body.vendorName || 'Unknown vendor',
    inventoryUnit: body.unit === 'bt' ? 'BOTTLE' : 'OTHER',
    inventoryUnitLabel: body.unit,
    orderUnit: body.unit === 'bt' ? 'BOTTLE' : 'OTHER',
    orderUnitLabel: body.unit,
    active: body.active ?? product.active
  } : product);
  return catalogProducts.find((product) => product.id === id);
}

function deleteInventoryCatalogProduct(path: string) {
  const id = Number(path.match(/inventory-catalog\/(\d+)/)?.[1]);
  catalogProducts = catalogProducts.map((product) => product.id === id ? { ...product, active: false } : product);
  return catalogProducts.find((product) => product.id === id);
}

const orders = [
  { id: 20, locationCode: 'SEATTLE', orderBusiness: 'BIANGBIANG_FRONT', orderBusinessName: 'BiangBiang Front', vendorId: 1, vendorName: 'Wellpack', orderNumber: 'PO-20260616-WELLPACK-001', businessDate: '2026-06-16', status: 'SUBMITTED', subtotal: 7.5, tax: 0, fees: 0, total: 7.5, currency: 'USD', lines: [line] }
];

const approvedOrder = {
  ...orders[0],
  status: 'APPROVED',
  submittedByNameSnapshot: 'Orderer One',
  submittedAt: '2026-06-16T18:00:00Z',
  approvedByNameSnapshot: 'Manager One',
  approvedAt: '2026-06-16T19:00:00Z',
  pdf: {
    documentId: 50,
    versionNumber: 1,
    filename: 'OrderPlan_SEATTLE_2026-06-16_PO-20260616-WELLPACK-001_v1.pdf',
    mimeType: 'application/pdf',
    generatedAt: '2026-06-16T19:00:01Z',
    generatedByEmployeeId: 1,
    checksumSha256: 'abc123',
    currentVersion: true
  }
};
