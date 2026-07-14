import { apiBinary, ApiBinaryResponse } from './client';

export function getInventoryCountPdf(inventoryCountId: number): Promise<ApiBinaryResponse> {
  return apiBinary(`/api/inventory-counts/${inventoryCountId}/pdf`);
}
