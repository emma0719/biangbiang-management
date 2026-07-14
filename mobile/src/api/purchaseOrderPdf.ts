import { apiBinary, ApiBinaryResponse } from './client';

export function getPurchaseOrderPdf(purchaseOrderId: number): Promise<ApiBinaryResponse> {
  return apiBinary(`/api/purchase-orders/${purchaseOrderId}/pdf`);
}
