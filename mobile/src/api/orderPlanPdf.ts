import { api, apiBinary, ApiBinaryResponse } from './client';
import { OrderPlanPdfMetadata } from '../types/domain';

export function getOrderPlanPdfMetadata(orderPlanId: number) {
  return api<OrderPlanPdfMetadata>(`/api/order-plans/${orderPlanId}/pdf`);
}

export function getOrderPlanPdfView(orderPlanId: number): Promise<ApiBinaryResponse> {
  return apiBinary(`/api/order-plans/${orderPlanId}/pdf/view`);
}

export function getOrderPlanPdfDownload(orderPlanId: number): Promise<ApiBinaryResponse> {
  return apiBinary(`/api/order-plans/${orderPlanId}/pdf/download`);
}
