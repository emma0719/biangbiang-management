import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { router } from 'expo-router';
import React, { useEffect, useMemo, useRef, useState } from 'react';
import { Alert } from 'react-native';
import { ApiError, api } from '../api/client';
import { getInventoryCountPdf } from '../api/inventoryPdf';
import { getOrderPlanPdfDownload, getOrderPlanPdfMetadata, getOrderPlanPdfView } from '../api/orderPlanPdf';
import { getPurchaseOrderPdf } from '../api/purchaseOrderPdf';
import { ConfirmDialog } from '../components/ConfirmDialog';
import { AppScreen, EmptyState, ErrorState } from '../components/designSystem';
import { useI18n } from '../i18n/I18nProvider';
import { TranslationKey } from '../i18n/translations';
import { layout } from '../theme/theme';
import { EmployeePrivate, InventoryBusiness, InventoryCountSession, OrderBusiness, OrderInventoryReference, OrderPlan, OrderProduct, PurchaseOrder, StoreCode, Vendor } from '../types/domain';
import { downloadPdfFile, PdfViewTarget, preparePdfViewTarget, viewPdfFile } from '../utils/orderPlanPdfFile';
import {
  CatalogManagementView,
  InventoryCatalogForm,
  InventoryCountView,
  InventoryBusinessSelector,
  OrderBusinessSelector,
  inventoryBusinessName,
  orderBusinessName,
  InventoryHistoryView,
  numberOrZero,
  OrderHistoryView,
  OrderWorkflowView,
  OrderPlanView,
  OrderingDashboard,
  OrderingSection,
  OrderReviewView,
  Quantities,
  ReceivingView,
  SectionNav,
  today,
  VendorOrderEditorView,
  VendorSelectionView
} from './ordering/OrderingViews';

const locationCode: StoreCode = 'SEATTLE';
type OrderingMode = 'full' | 'order' | 'inventory';
type InventoryDialog =
  | { type: 'leave'; leave: () => void }
  | { type: 'draftChoice' };
const modeSections: Record<OrderingMode, OrderingSection[]> = {
  full: ['dashboard', 'vendors', 'editor', 'inventory', 'plans', 'review', 'receiving', 'history', 'catalog'],
  order: ['vendors', 'review', 'history'],
  inventory: ['inventory', 'history']
};
const initialSection: Record<OrderingMode, OrderingSection> = {
  full: 'dashboard',
  order: 'vendors',
  inventory: 'inventory'
};

export function OrderingScreen({ mode = 'full' }: { mode?: OrderingMode }) {
  const { t } = useI18n();
  const queryClient = useQueryClient();
  const [section, setSection] = useState<OrderingSection>(initialSection[mode]);
  const [selectedVendorId, setSelectedVendorId] = useState<number | undefined>();
  const [selectedOrderId, setSelectedOrderId] = useState<number | undefined>();
  const [quantities, setQuantities] = useState<Quantities>({});
  const [inventory, setInventory] = useState<Quantities>({});
  const [inventoryNotes, setInventoryNotes] = useState<Record<number, string>>({});
  const [search, setSearch] = useState('');
  const [hideZero, setHideZero] = useState(false);
  const [rejectReason, setRejectReason] = useState('');
  const [receivedNow, setReceivedNow] = useState<Quantities>({});
  const [planFinalQuantities, setPlanFinalQuantities] = useState<Quantities>({});
  const [pdfPreflightError, setPdfPreflightError] = useState<TranslationKey | undefined>();
  const [orderMessage, setOrderMessage] = useState<TranslationKey | undefined>();
  const [orderError, setOrderError] = useState<TranslationKey | undefined>();
  const [inventoryMessage, setInventoryMessage] = useState<TranslationKey | undefined>();
  const [inventoryError, setInventoryError] = useState<TranslationKey | undefined>();
  const [inventoryPdfError, setInventoryPdfError] = useState<TranslationKey | undefined>();
  const [inventoryDirty, setInventoryDirty] = useState(false);
  const [activeInventoryCountId, setActiveInventoryCountId] = useState<number | undefined>();
  const [startNewInventoryCount, setStartNewInventoryCount] = useState(false);
  const [inventoryDialog, setInventoryDialog] = useState<InventoryDialog | undefined>();
  const [inventoryDialogError, setInventoryDialogError] = useState<TranslationKey | undefined>();
  const [selectedInventoryBusiness, setSelectedInventoryBusiness] = useState<InventoryBusiness | undefined>();
  const [manageInventoryCatalog, setManageInventoryCatalog] = useState(false);
  const [catalogForm, setCatalogForm] = useState<InventoryCatalogForm>({ name: '', unit: 'bt', vendorName: '' });
  const [editingCatalogProductId, setEditingCatalogProductId] = useState<number | undefined>();
  const [catalogMessage, setCatalogMessage] = useState<TranslationKey | undefined>();
  const [catalogError, setCatalogError] = useState<TranslationKey | undefined>();
  const [deleteCatalogProduct, setDeleteCatalogProduct] = useState<OrderProduct | undefined>();
  const [selectedOrderBusiness, setSelectedOrderBusiness] = useState<OrderBusiness | undefined>();
  const [manageOrderCatalog, setManageOrderCatalog] = useState(false);
  const [orderCatalogForm, setOrderCatalogForm] = useState<InventoryCatalogForm>({ name: '', unit: 'case', vendorName: '' });
  const [editingOrderCatalogProductId, setEditingOrderCatalogProductId] = useState<number | undefined>();
  const [orderCatalogMessage, setOrderCatalogMessage] = useState<TranslationKey | undefined>();
  const [orderCatalogError, setOrderCatalogError] = useState<TranslationKey | undefined>();
  const [deleteOrderCatalogProduct, setDeleteOrderCatalogProduct] = useState<OrderProduct | undefined>();
  const [latestInventoryReference, setLatestInventoryReference] = useState<OrderInventoryReference | undefined>();
  const promptedForDraft = useRef(false);

  const profile = useQuery({ queryKey: ['me'], queryFn: () => api<EmployeePrivate>('/api/me') });
  const vendors = useQuery({ queryKey: ['ordering', 'vendors'], queryFn: () => api<Vendor[]>(`/api/vendors?locationCode=${locationCode}&activeOnly=true`) });
  const inventoryProductQuery = selectedInventoryBusiness ? `&inventoryBusiness=${selectedInventoryBusiness}` : '';
  const orderProductQuery = mode === 'order' && selectedOrderBusiness ? `&orderBusiness=${selectedOrderBusiness}` : '';
  const products = useQuery({
    queryKey: ['ordering', 'products', mode, selectedInventoryBusiness ?? selectedOrderBusiness ?? 'all'],
    enabled: (mode !== 'inventory' || Boolean(selectedInventoryBusiness)) && (mode !== 'order' || Boolean(selectedOrderBusiness)),
    queryFn: () => api<OrderProduct[]>(`/api/order-products?locationCode=${locationCode}&active=true${mode === 'order' ? orderProductQuery : inventoryProductQuery}`)
  });
  const orders = useQuery({ queryKey: ['ordering', 'orders'], queryFn: () => api<PurchaseOrder[]>(`/api/purchase-orders?locationCode=${locationCode}`) });
  const counts = useQuery({ queryKey: ['ordering', 'counts'], queryFn: () => api<InventoryCountSession[]>(`/api/inventory-counts?locationCode=${locationCode}`) });
  const plans = useQuery({ queryKey: ['ordering', 'plans'], queryFn: () => api<OrderPlan[]>(`/api/order-plans?locationCode=${locationCode}`), enabled: mode !== 'inventory' });

  const vendorList = asArray(vendors.data);
  const productList = asArray(products.data);
  const orderList = asArray(orders.data);
  const countList = asArray(counts.data);
  const planList = asArray(plans.data);
  const latestEligibleCount = countList.find((count) => ['SUBMITTED', 'REVIEWED', 'LOCKED', 'COMPLETED'].includes(count.status));
  const activePlan = planList[0];
  const orderPlanPdf = useQuery({
    queryKey: ['order-plan-pdf', activePlan?.id],
    enabled: section === 'plans' && Boolean(activePlan?.id),
    queryFn: () => getOrderPlanPdfMetadata(activePlan!.id),
    retry: (failureCount, error) => !hasApiCode(error, 'ORDER_PLAN_PDF_NOT_FOUND') && failureCount < 2
  });
  const orderableVendorIds = new Set(productList.map((product) => product.vendorId));
  const selectedVendor = vendorList.find((vendor) => vendor.id === selectedVendorId) ?? vendorList.find((vendor) => orderableVendorIds.has(vendor.id)) ?? vendorList[0];
  const selectedOrder = orderList.find((order) => order.id === selectedOrderId) ?? orderList[0];
  const selectedVendorProducts = productList.filter((product) => !selectedVendor || product.vendorId === selectedVendor.id);
  const vendorProducts = selectedVendorProducts
    .filter((product) => product.name.toLowerCase().includes(search.toLowerCase()) || product.vendorProductCode?.toLowerCase().includes(search.toLowerCase()));
  const visibleProducts = hideZero ? vendorProducts.filter((product) => Number(quantities[product.id] ?? '0') > 0) : vendorProducts;
  const isBusinessPartner = profile.data?.positions?.some((position) => ['OWNER', 'MANAGER', 'FINANCIAL_MANAGER'].includes(position)) ?? false;
  const submitterName = profile.data?.displayName ?? '';
  const estimatedSubtotal = useMemo(() => selectedVendorProducts.reduce((sum, product) => sum + Number(quantities[product.id] || 0) * Number(product.unitPrice ?? 0), 0), [selectedVendorProducts, quantities]);
  const existingInventoryDraft = countList.find((count) => (!selectedInventoryBusiness || count.inventoryBusiness === selectedInventoryBusiness) && (count.status === 'DRAFT' || count.status === 'IN_PROGRESS'));

  useEffect(() => {
    if (mode !== 'inventory' || section !== 'inventory' || !selectedInventoryBusiness || promptedForDraft.current || !existingInventoryDraft) return;
    promptedForDraft.current = true;
    setInventoryDialog({ type: 'draftChoice' });
  }, [existingInventoryDraft, mode, section, selectedInventoryBusiness, t]);

  const refresh = () => {
    queryClient.invalidateQueries({ queryKey: ['ordering'] });
    queryClient.invalidateQueries({ queryKey: ['order-plan-pdf'] });
  };

  const createOrder = useMutation({
    mutationFn: async () => {
      if (!selectedVendor) throw new Error('missing vendor');
      if (!selectedOrderBusiness) throw new Error('missing order business');
      return api<PurchaseOrder>('/api/purchase-orders', { method: 'POST', body: JSON.stringify({ locationCode, vendorId: selectedVendor.id, businessDate: today(), orderBusiness: selectedOrderBusiness }) });
    },
    onSuccess: (order) => {
      setSelectedOrderId(order.id);
      setSection(mode === 'order' ? 'vendors' : 'editor');
      refresh();
    }
  });

  const saveDraft = useMutation({
    mutationFn: async () => {
      const existingDraft = selectedOrderId ? orderList.find((order) => order.id === selectedOrderId) : orderList.find((order) => order.vendorId === selectedVendor?.id && order.status === 'DRAFT' && (!selectedOrderBusiness || order.orderBusiness === selectedOrderBusiness));
      const order = existingDraft ?? await createOrder.mutateAsync();
      const lines = selectedVendorProducts.map((product) => ({ productId: product.id, currentInventoryQuantity: numberOrZero(inventory[product.id]), requestedQuantity: numberOrZero(quantities[product.id]), notes: '' }));
      return api<PurchaseOrder>(`/api/purchase-orders/${order.id}/lines`, { method: 'PUT', body: JSON.stringify({ lines }) });
    },
    onSuccess: (order) => {
      setSelectedOrderId(order.id);
      setOrderError(undefined);
      setOrderMessage('success');
      refresh();
    },
    onError: () => {
      setOrderMessage(undefined);
      setOrderError('apiError');
    }
  });

  const submitOrder = useMutation({
    mutationFn: async () => {
      const saved = await saveDraft.mutateAsync();
      return api<PurchaseOrder>(`/api/purchase-orders/${saved.id}/submit`, { method: 'POST' });
    },
    onSuccess: (order) => {
      setSelectedOrderId(order.id);
      setOrderError(undefined);
      setOrderMessage('success');
      queryClient.setQueryData<PurchaseOrder[]>(['ordering', 'orders'], (current) => {
        const existing = asArray(current);
        return [order, ...existing.filter((item) => item.id !== order.id)];
      });
      setSection('history');
      refresh();
    },
    onError: () => {
      setOrderMessage(undefined);
      setOrderError('apiError');
    }
  });

  const confirmSubmitOrder = () => {
    Alert.alert(t('submitForApproval'), `${t('submittingAs')}: ${submitterName}`, [
      { text: t('cancel') },
      { text: t('confirm'), onPress: () => submitOrder.mutate() }
    ]);
  };

  const createCount = useMutation({
    mutationFn: () => {
      if (!selectedInventoryBusiness) throw new Error('missing inventory business');
      return api<InventoryCountSession>('/api/inventory-counts', { method: 'POST', body: JSON.stringify({ locationCode, inventoryBusiness: selectedInventoryBusiness, businessDate: today() }) });
    },
    onSuccess: (count) => {
      setSection('inventory');
      queryClient.setQueryData(['ordering', 'activeCount'], count);
      refresh();
    }
  });

  const buildInventoryLines = (includeUntouchedWhenEmpty = false) => {
    const touchedProducts = productList.filter((product) => Object.prototype.hasOwnProperty.call(inventory, product.id));
    const productsToSave = touchedProducts.length > 0 ? touchedProducts : includeUntouchedWhenEmpty ? productList : [];
    return productsToSave.map((product) => ({
      productId: product.id,
      quantityOnHand: numberOrZero(inventory[product.id]),
      unit: product.inventoryUnit,
      notes: inventoryNotes[product.id] ?? ''
    }));
  };

  const saveCount = useMutation({
    mutationFn: async (options?: { includeUntouchedWhenEmpty?: boolean }) => {
      const existingActiveCount = activeInventoryCountId ? { id: activeInventoryCountId } : undefined;
      const resumableCount = startNewInventoryCount ? undefined : counts.data?.find((item) => item.inventoryBusiness === selectedInventoryBusiness && (item.status === 'DRAFT' || item.status === 'IN_PROGRESS'));
      const count = existingActiveCount ?? resumableCount ?? await createCount.mutateAsync();
      setActiveInventoryCountId(count.id);
      const lines = buildInventoryLines(Boolean(options?.includeUntouchedWhenEmpty));
      return api<InventoryCountSession>(`/api/inventory-counts/${count.id}/lines`, { method: 'PUT', body: JSON.stringify({ lines }) });
    },
    onSuccess: () => {
      setInventoryError(undefined);
      setInventoryDialogError(undefined);
      setInventoryMessage('countSaved');
      setInventoryDirty(false);
      setStartNewInventoryCount(false);
      setSection('inventory');
      refresh();
    },
    onError: () => {
      setInventoryMessage(undefined);
      setInventoryError('apiError');
    }
  });

  const submitCount = useMutation({
    mutationFn: async () => {
      const saved = await saveCount.mutateAsync({ includeUntouchedWhenEmpty: true });
      return api<InventoryCountSession>(`/api/inventory-counts/${saved.id}/submit`, { method: 'POST' });
    },
    onSuccess: (count) => {
      setInventoryError(undefined);
      setInventoryMessage('inventorySubmittedSuccessfully');
      setInventoryDirty(false);
      setActiveInventoryCountId(undefined);
      setStartNewInventoryCount(true);
      queryClient.setQueryData<InventoryCountSession[]>(['ordering', 'counts'], (current) => {
        const existing = asArray(current);
        const withoutSubmitted = existing.filter((item) => item.id !== count.id);
        return [count, ...withoutSubmitted];
      });
      setSection(mode === 'inventory' ? 'history' : 'inventory');
      refresh();
    },
    onError: () => {
      setInventoryMessage(undefined);
      setInventoryError('unableToSubmitInventory');
      setInventoryDirty(true);
    }
  });

  const confirmSubmitInventory = () => {
    setInventoryError(undefined);
    submitCount.mutate();
  };

  const exportInventoryPdf = useMutation({
    mutationFn: async ({ count, target }: { count: InventoryCountSession; target?: PdfViewTarget }) => {
      try {
        const file = await getInventoryCountPdf(count.id);
        await viewPdfFile(file, file.filename ?? `inventory-count-${count.id}.pdf`, target);
      } catch (error) {
        target?.close();
        throw error;
      }
    },
    onSuccess: () => {
      setInventoryPdfError(undefined);
    },
    onError: () => {
      setInventoryPdfError('unableToExportPdf');
    }
  });

  const handleExportInventoryPdf = (count: InventoryCountSession) => {
    exportInventoryPdf.reset();
    setInventoryPdfError(undefined);
    const target = preparePdfViewTarget();
    if (target === null) {
      setInventoryPdfError('unableToExportPdf');
      return;
    }
    exportInventoryPdf.mutate({ count, target });
  };

  const resetCatalogForm = () => {
    setCatalogForm({ name: '', unit: 'bt', vendorName: '' });
    setEditingCatalogProductId(undefined);
  };

  const resetOrderCatalogForm = () => {
    setOrderCatalogForm({ name: '', unit: 'case', vendorName: '' });
    setEditingOrderCatalogProductId(undefined);
  };

  const saveOrderCatalogItem = useMutation({
    mutationFn: async () => {
      if (!selectedOrderBusiness) throw new Error('missing order business');
      if (!orderCatalogForm.name.trim() || !orderCatalogForm.unit.trim()) throw new Error('missing catalog fields');
      if (editingOrderCatalogProductId) {
        return api<OrderProduct>(`/api/order-catalog/${editingOrderCatalogProductId}`, {
          method: 'PATCH',
          body: JSON.stringify({
            name: orderCatalogForm.name.trim(),
            unit: orderCatalogForm.unit.trim(),
            vendorName: orderCatalogForm.vendorName.trim() || null,
            active: true
          })
        });
      }
      return api<OrderProduct>('/api/order-catalog', {
        method: 'POST',
        body: JSON.stringify({
          locationCode,
          orderBusiness: selectedOrderBusiness,
          name: orderCatalogForm.name.trim(),
          unit: orderCatalogForm.unit.trim(),
          vendorName: orderCatalogForm.vendorName.trim() || null
        })
      });
    },
    onSuccess: () => {
      setOrderCatalogError(undefined);
      setOrderCatalogMessage(editingOrderCatalogProductId ? 'orderItemUpdated' : 'orderItemAdded');
      resetOrderCatalogForm();
      refresh();
    },
    onError: () => {
      setOrderCatalogMessage(undefined);
      setOrderCatalogError('unableToSaveOrderItem');
    }
  });

  const deleteOrderCatalogItem = useMutation({
    mutationFn: async (product: OrderProduct) => api<OrderProduct>(`/api/order-catalog/${product.id}`, { method: 'DELETE' }),
    onSuccess: () => {
      setOrderCatalogError(undefined);
      setOrderCatalogMessage('orderItemDeleted');
      setDeleteOrderCatalogProduct(undefined);
      refresh();
    },
    onError: () => {
      setOrderCatalogMessage(undefined);
      setOrderCatalogError('unableToDeleteOrderItem');
    }
  });

  const importLatestInventory = useMutation({
    mutationFn: async () => {
      if (!selectedOrderBusiness) throw new Error('missing order business');
      return api<OrderInventoryReference>(`/api/order-inventory-reference?locationCode=${locationCode}&business=${selectedOrderBusiness}`);
    },
    onSuccess: (reference) => {
      setLatestInventoryReference(reference);
      setOrderError(undefined);
      setOrderMessage(reference.inventoryCountId ? 'latestInventoryImported' : 'noSubmittedInventoryForBusiness');
    },
    onError: () => {
      setOrderMessage(undefined);
      setOrderError('unableToImportLatestInventory');
    }
  });

  const saveInventoryCatalogItem = useMutation({
    mutationFn: async () => {
      if (!selectedInventoryBusiness) throw new Error('missing inventory business');
      if (!catalogForm.name.trim() || !catalogForm.unit.trim()) throw new Error('missing catalog fields');
      if (editingCatalogProductId) {
        return api<OrderProduct>(`/api/inventory-catalog/${editingCatalogProductId}`, {
          method: 'PATCH',
          body: JSON.stringify({
            name: catalogForm.name.trim(),
            unit: catalogForm.unit.trim(),
            vendorName: catalogForm.vendorName.trim() || null,
            active: true
          })
        });
      }
      return api<OrderProduct>('/api/inventory-catalog', {
        method: 'POST',
        body: JSON.stringify({
          locationCode,
          inventoryBusiness: selectedInventoryBusiness,
          name: catalogForm.name.trim(),
          unit: catalogForm.unit.trim(),
          vendorName: catalogForm.vendorName.trim() || null
        })
      });
    },
    onSuccess: () => {
      setCatalogError(undefined);
      setCatalogMessage(editingCatalogProductId ? 'inventoryItemUpdated' : 'inventoryItemAdded');
      resetCatalogForm();
      refresh();
    },
    onError: () => {
      setCatalogMessage(undefined);
      setCatalogError('unableToSaveInventoryItem');
    }
  });

  const deleteInventoryCatalogItem = useMutation({
    mutationFn: async (product: OrderProduct) => api<OrderProduct>(`/api/inventory-catalog/${product.id}`, { method: 'DELETE' }),
    onSuccess: () => {
      setCatalogError(undefined);
      setCatalogMessage('inventoryItemDeleted');
      setDeleteCatalogProduct(undefined);
      refresh();
    },
    onError: () => {
      setCatalogMessage(undefined);
      setCatalogError('unableToDeleteInventoryItem');
    }
  });

  const transition = useMutation({
    mutationFn: ({ path, body }: { path: string; body?: object }) => api<PurchaseOrder>(path, { method: 'POST', body: body ? JSON.stringify(body) : undefined }),
    onSuccess: (order) => {
      setSelectedOrderId(order.id);
      refresh();
    }
  });

  const createPlan = useMutation({
    mutationFn: async () => {
      if (!latestEligibleCount) throw new Error('missing inventory source');
      return api<OrderPlan>('/api/order-plans', { method: 'POST', body: JSON.stringify({ locationCode, sourceInventorySessionId: latestEligibleCount.id, businessDate: today() }) });
    },
    onSuccess: refresh
  });

  const savePlan = useMutation({
    mutationFn: async () => {
      if (!activePlan) throw new Error('missing order plan');
      const lines = activePlan.lines.map((line) => ({ productId: line.productId, finalOrderQuantity: numberOrZero(planFinalQuantities[line.productId] ?? String(line.finalOrderQuantity ?? 0)), notes: line.notes ?? '' }));
      return api<OrderPlan>(`/api/order-plans/${activePlan.id}/lines`, { method: 'PUT', body: JSON.stringify({ lines }) });
    },
    onSuccess: refresh
  });

  const generateVendorOrders = useMutation({
    mutationFn: async () => {
      if (!activePlan) throw new Error('missing order plan');
      return api<PurchaseOrder[]>(`/api/order-plans/${activePlan.id}/generate-vendor-orders`, { method: 'POST' });
    },
    onSuccess: refresh
  });

  const submitPlan = useMutation({
    mutationFn: async () => {
      const saved = await savePlan.mutateAsync();
      return api<OrderPlan>(`/api/order-plans/${saved.id}/submit`, { method: 'POST' });
    },
    onSuccess: refresh
  });

  const confirmSubmitPlan = () => {
    Alert.alert(t('submitForApproval'), `${t('submittingAs')}: ${submitterName}`, [
      { text: t('cancel') },
      { text: t('confirm'), onPress: () => submitPlan.mutate() }
    ]);
  };

  const viewOrderPlanPdf = useMutation({
    mutationFn: async (target?: PdfViewTarget) => {
      if (!activePlan) throw new Error('missing order plan');
      try {
        const file = await getOrderPlanPdfView(activePlan.id);
        await viewPdfFile(file, orderPlanPdf.data?.filename ?? file.filename ?? 'order-plan.pdf', target);
      } catch (error) {
        target?.close();
        throw error;
      }
    }
  });

  const downloadOrderPlanPdf = useMutation({
    mutationFn: async () => {
      if (!activePlan) throw new Error('missing order plan');
      const file = await getOrderPlanPdfDownload(activePlan.id);
      await downloadPdfFile(file, orderPlanPdf.data?.filename ?? file.filename ?? 'order-plan.pdf');
    }
  });

  const handleViewOrderPlanPdf = () => {
    viewOrderPlanPdf.reset();
    downloadOrderPlanPdf.reset();
    setPdfPreflightError(undefined);
    const target = preparePdfViewTarget();
    if (target === null) {
      setPdfPreflightError('unableToOpenPdf');
      return;
    }
    viewOrderPlanPdf.mutate(target);
  };

  const handleDownloadOrderPlanPdf = () => {
    viewOrderPlanPdf.reset();
    downloadOrderPlanPdf.reset();
    setPdfPreflightError(undefined);
    downloadOrderPlanPdf.mutate();
  };

  const viewPurchaseOrderPdf = useMutation({
    mutationFn: async ({ order, target }: { order: PurchaseOrder; target?: PdfViewTarget }) => {
      try {
        const file = await getPurchaseOrderPdf(order.id);
        await viewPdfFile(file, order.pdf?.filename ?? file.filename ?? `purchase-order-${order.id}.pdf`, target);
      } catch (error) {
        target?.close();
        throw error;
      }
    }
  });

  const downloadPurchaseOrderPdf = useMutation({
    mutationFn: async (order: PurchaseOrder) => {
      const file = await getPurchaseOrderPdf(order.id);
      await downloadPdfFile(file, order.pdf?.filename ?? file.filename ?? `purchase-order-${order.id}.pdf`);
    }
  });

  const handleViewPurchaseOrderPdf = (order?: PurchaseOrder) => {
    if (!order) return;
    viewPurchaseOrderPdf.reset();
    downloadPurchaseOrderPdf.reset();
    setPdfPreflightError(undefined);
    const target = preparePdfViewTarget();
    if (target === null) {
      setPdfPreflightError('unableToOpenPdf');
      return;
    }
    viewPurchaseOrderPdf.mutate({ order, target });
  };

  const handleDownloadPurchaseOrderPdf = (order?: PurchaseOrder) => {
    if (!order) return;
    viewPurchaseOrderPdf.reset();
    downloadPurchaseOrderPdf.reset();
    setPdfPreflightError(undefined);
    downloadPurchaseOrderPdf.mutate(order);
  };

  const pdfUnavailable = hasApiCode(orderPlanPdf.error, 'ORDER_PLAN_PDF_NOT_FOUND');
  const pdfMetadataErrorMessage = orderPlanPdf.error && !pdfUnavailable ? t(pdfMessageKey(orderPlanPdf.error, 'pdfNetworkError')) : undefined;
  const pdfActionErrorMessage = pdfPreflightError
    ? t(pdfPreflightError)
    : viewOrderPlanPdf.error
    ? t(pdfMessageKey(viewOrderPlanPdf.error, 'unableToOpenPdf'))
    : downloadOrderPlanPdf.error
      ? t(pdfMessageKey(downloadOrderPlanPdf.error, 'unableToDownloadPdf'))
      : undefined;

  const sections = modeSections[mode];
  const sectionLabels = mode === 'order'
    ? { vendors: t('newOrder'), review: t('reviewAndReceiving'), history: t('historyOrders') }
    : mode === 'inventory'
      ? { inventory: t('inventoryCount'), history: t('history') }
      : undefined;
  const screenTitle = mode === 'order' ? t('mainNavOrder') : mode === 'inventory' ? t('mainNavInventory') : t('ordering');
  const screenSubtitle = mode === 'order' ? t('orderingWork') : mode === 'inventory' ? t('inventoryDashboard') : t('orderingDashboard');
  const activeMainTab = mode === 'order' ? 'order' : mode === 'inventory' ? 'inventory' : undefined;

  if (profile.isLoading || vendors.isLoading || products.isLoading || orders.isLoading || counts.isLoading || (mode !== 'inventory' && plans.isLoading)) return <AppScreen title={screenTitle} activeMainTab={activeMainTab}><EmptyState message={t('loading')} /></AppScreen>;
  if (profile.error || vendors.error || products.error || orders.error || counts.error || (mode !== 'inventory' && plans.error)) return <ErrorState message={t('loadOrderingError')} />;
  if (!profile.data) return <ErrorState message={t('networkError')} />;

  const continueInventoryDraft = () => {
    if (!existingInventoryDraft) return;
    setActiveInventoryCountId(existingInventoryDraft.id);
    setStartNewInventoryCount(false);
    setInventory(loadInventoryDraft(existingInventoryDraft));
    setInventoryDirty(false);
    setInventoryDialogError(undefined);
    setInventoryDialog(undefined);
  };

  const startNewInventoryDraft = () => {
    setActiveInventoryCountId(undefined);
    setStartNewInventoryCount(true);
    setInventory({});
    setInventoryNotes({});
    setInventoryDirty(false);
    setInventoryDialogError(undefined);
    setInventoryDialog(undefined);
  };

  const selectInventoryBusiness = (business: InventoryBusiness) => {
    setSelectedInventoryBusiness(business);
    setSelectedVendorId(undefined);
    setManageInventoryCatalog(false);
    resetCatalogForm();
    setCatalogMessage(undefined);
    setCatalogError(undefined);
    setInventory({});
    setInventoryNotes({});
    setInventoryDirty(false);
    setActiveInventoryCountId(undefined);
    setStartNewInventoryCount(false);
    setInventoryDialog(undefined);
    promptedForDraft.current = false;
    setSearch('');
  };

  const selectOrderBusiness = (business: OrderBusiness) => {
    setSelectedOrderBusiness(business);
    setSelectedVendorId(undefined);
    setSelectedOrderId(undefined);
    setManageOrderCatalog(false);
    resetOrderCatalogForm();
    setOrderCatalogMessage(undefined);
    setOrderCatalogError(undefined);
    setDeleteOrderCatalogProduct(undefined);
    setLatestInventoryReference(undefined);
    setOrderMessage(undefined);
    setOrderError(undefined);
    setQuantities({});
    setSearch('');
  };

  const changeOrderBusiness = () => {
    setSelectedOrderBusiness(undefined);
    setSelectedVendorId(undefined);
    setSelectedOrderId(undefined);
    setManageOrderCatalog(false);
    resetOrderCatalogForm();
    setOrderCatalogMessage(undefined);
    setOrderCatalogError(undefined);
    setDeleteOrderCatalogProduct(undefined);
    setLatestInventoryReference(undefined);
    setOrderMessage(undefined);
    setOrderError(undefined);
    setQuantities({});
    setSearch('');
  };

  const changeInventoryBusiness = () => {
    const leave = () => {
      setSelectedInventoryBusiness(undefined);
      setSelectedVendorId(undefined);
      setManageInventoryCatalog(false);
      resetCatalogForm();
      setCatalogMessage(undefined);
      setCatalogError(undefined);
      setInventory({});
      setInventoryNotes({});
      setActiveInventoryCountId(undefined);
      setSearch('');
      promptedForDraft.current = false;
    };
    if (confirmInventoryLeave(leave)) leave();
  };

  const discardInventoryEdits = () => {
    const draft = activeInventoryCountId ? countList.find((count) => count.id === activeInventoryCountId) : startNewInventoryCount ? undefined : existingInventoryDraft;
    setInventory(draft ? loadInventoryDraft(draft) : {});
    setInventoryNotes({});
    setInventoryDirty(false);
    setInventoryDialogError(undefined);
  };

  const confirmInventoryLeave = (leave: () => void) => {
    if (mode !== 'inventory' || section !== 'inventory' || !inventoryDirty) return true;
    setInventoryDialogError(undefined);
    setInventoryDialog({ type: 'leave', leave });
    return false;
  };

  const saveInventoryAndLeave = (leave: () => void) => {
    saveCount.mutate(undefined, {
      onSuccess: () => {
        setInventoryDialog(undefined);
        setInventoryDialogError(undefined);
        leave();
      },
      onError: () => setInventoryDialogError('apiError')
    });
  };

  const leaveWithoutSavingInventory = (leave: () => void) => {
    discardInventoryEdits();
    setInventoryDialog(undefined);
    leave();
  };

  const setInventorySection = (next: OrderingSection) => {
    if (next === 'inventory') {
      setSection(next);
      return;
    }
    if (confirmInventoryLeave(() => setSection(next))) {
      setSection(next);
    }
  };

  return (
    <AppScreen title={screenTitle} subtitle={screenSubtitle} maxWidth={layout.managerMaxWidth} activeMainTab={activeMainTab} onBeforeMainTabChange={(path) => confirmInventoryLeave(() => router.replace(path))}>
      <SectionNav active={section} setActive={mode === 'inventory' ? setInventorySection : setSection} sections={sections} labels={sectionLabels} />
      {section === 'dashboard' && sections.includes('dashboard') ? <OrderingDashboard orders={orderList} counts={countList} plans={planList} vendorCount={vendorList.length} onSection={(next) => sections.includes(next) ? setSection(next) : setSection(initialSection[mode])} /> : null}
      {section === 'vendors' && sections.includes('vendors') && mode === 'order' && !selectedOrderBusiness ? <OrderBusinessSelector onSelect={selectOrderBusiness} /> : null}
      {section === 'vendors' && sections.includes('vendors') && mode === 'order' && selectedOrderBusiness ? <OrderWorkflowView businessLabel={orderBusinessName(selectedOrderBusiness)} vendors={vendorList.filter((vendor) => productList.some((product) => product.vendorId === vendor.id))} products={selectedVendorProducts} allProducts={productList} selectedVendor={selectedVendor} search={search} setSearch={setSearch} quantities={quantities} setQuantities={setQuantities} saving={saveDraft.isPending || createOrder.isPending} submitting={submitOrder.isPending} message={orderMessage ? t(orderMessage) : undefined} error={orderError ? t(orderError) : undefined} canManageCatalog={isBusinessPartner} managingCatalog={manageOrderCatalog} catalogForm={orderCatalogForm} setCatalogForm={(form) => { setOrderCatalogForm(form); setOrderCatalogError(undefined); }} editingCatalogProductId={editingOrderCatalogProductId} catalogSaving={saveOrderCatalogItem.isPending} catalogDeletingProductId={deleteOrderCatalogItem.isPending ? deleteOrderCatalogItem.variables?.id : undefined} catalogMessage={orderCatalogMessage ? t(orderCatalogMessage) : undefined} catalogError={orderCatalogError ? t(orderCatalogError) : undefined} inventoryReference={latestInventoryReference} importingReference={importLatestInventory.isPending} onImportLatestInventory={() => importLatestInventory.mutate()} onChangeBusiness={changeOrderBusiness} onManageCatalog={() => { setManageOrderCatalog((value) => !value); resetOrderCatalogForm(); }} onSaveCatalog={() => saveOrderCatalogItem.mutate()} onEditCatalog={(product) => { setEditingOrderCatalogProductId(product.id); setOrderCatalogForm({ name: product.name, unit: product.orderUnitLabel ?? product.inventoryUnitLabel ?? 'case', vendorName: product.vendorName === 'Unknown vendor' ? '' : product.vendorName }); setOrderCatalogMessage(undefined); setOrderCatalogError(undefined); }} onCancelEditCatalog={resetOrderCatalogForm} onDeleteCatalog={(product) => setDeleteOrderCatalogProduct(product)} onSelectVendor={(vendor) => { setSelectedVendorId(vendor.id); setSearch(''); }} onSave={() => saveDraft.mutate()} onSubmit={confirmSubmitOrder} /> : null}
      {section === 'vendors' && sections.includes('vendors') && mode !== 'order' ? <VendorSelectionView vendors={vendorList} products={productList} selectedVendorId={selectedVendor?.id} onSelect={(vendor) => { setSelectedVendorId(vendor.id); setSection('editor'); }} /> : null}
      {section === 'editor' && sections.includes('editor') ? <VendorOrderEditorView vendor={selectedVendor} products={visibleProducts} search={search} setSearch={setSearch} quantities={quantities} setQuantities={setQuantities} inventory={inventory} setInventory={setInventory} hideZero={hideZero} setHideZero={setHideZero} subtotal={estimatedSubtotal} canCreateOrder={Boolean(selectedVendor)} submitterName={submitterName} onSave={() => saveDraft.mutate()} onSubmit={confirmSubmitOrder} /> : null}
      {section === 'inventory' && sections.includes('inventory') && !selectedInventoryBusiness ? <InventoryBusinessSelector onSelect={selectInventoryBusiness} /> : null}
      {section === 'inventory' && sections.includes('inventory') && (mode !== 'inventory' || selectedInventoryBusiness) ? <InventoryCountView businessLabel={selectedInventoryBusiness ? inventoryBusinessName(selectedInventoryBusiness) : t('inventoryCount')} vendors={vendorList.filter((vendor) => productList.some((product) => product.vendorId === vendor.id))} products={productList} selectedVendor={selectedVendor} inventory={inventory} setInventory={(next) => { setInventory(next); setInventoryDirty(true); setInventoryError(undefined); setInventoryDialogError(undefined); }} search={search} setSearch={setSearch} submitterName={submitterName} saving={saveCount.isPending || createCount.isPending} submitting={submitCount.isPending} message={inventoryMessage ? t(inventoryMessage) : undefined} error={inventoryDialog?.type === 'leave' ? undefined : inventoryError ? t(inventoryError) : undefined} canManageCatalog={isBusinessPartner} managingCatalog={manageInventoryCatalog} catalogForm={catalogForm} setCatalogForm={(form) => { setCatalogForm(form); setCatalogError(undefined); }} editingCatalogProductId={editingCatalogProductId} catalogSaving={saveInventoryCatalogItem.isPending} catalogDeletingProductId={deleteInventoryCatalogItem.isPending ? deleteInventoryCatalogItem.variables?.id : undefined} catalogMessage={catalogMessage ? t(catalogMessage) : undefined} catalogError={catalogError ? t(catalogError) : undefined} onManageCatalog={() => { setManageInventoryCatalog((value) => !value); resetCatalogForm(); }} onSaveCatalog={() => saveInventoryCatalogItem.mutate()} onEditCatalog={(product) => { setEditingCatalogProductId(product.id); setCatalogForm({ name: product.name, unit: product.inventoryUnitLabel ?? 'bt', vendorName: product.vendorName === 'Unknown vendor' ? '' : product.vendorName }); setCatalogMessage(undefined); setCatalogError(undefined); }} onCancelEditCatalog={resetCatalogForm} onDeleteCatalog={(product) => setDeleteCatalogProduct(product)} onChangeBusiness={changeInventoryBusiness} onSelectVendor={(vendor) => { setSelectedVendorId(vendor.id); setSearch(''); }} onSubmit={confirmSubmitInventory} /> : null}
      {section === 'plans' && sections.includes('plans') ? <OrderPlanView plans={planList} counts={countList} products={productList} finalQuantities={planFinalQuantities} setFinalQuantities={setPlanFinalQuantities} submitterName={submitterName} onCreate={() => createPlan.mutate()} onSave={() => savePlan.mutate()} onSubmit={confirmSubmitPlan} onGenerate={() => generateVendorOrders.mutate()} pdfState={{ metadata: orderPlanPdf.data, loading: orderPlanPdf.isLoading, unavailable: pdfUnavailable, errorMessage: pdfMetadataErrorMessage, actionErrorMessage: pdfActionErrorMessage, viewing: viewOrderPlanPdf.isPending, downloading: downloadOrderPlanPdf.isPending, onView: handleViewOrderPlanPdf, onDownload: handleDownloadOrderPlanPdf }} /> : null}
      {section === 'review' && sections.includes('review') ? <OrderReviewView order={selectedOrder} isBusinessPartner={isBusinessPartner} rejectReason={rejectReason} setRejectReason={setRejectReason} pdfActionErrorMessage={pdfPreflightError ? t(pdfPreflightError) : viewPurchaseOrderPdf.error ? t(pdfMessageKey(viewPurchaseOrderPdf.error, 'unableToOpenPdf')) : downloadPurchaseOrderPdf.error ? t(pdfMessageKey(downloadPurchaseOrderPdf.error, 'unableToDownloadPdf')) : undefined} viewingPdf={viewPurchaseOrderPdf.isPending} downloadingPdf={downloadPurchaseOrderPdf.isPending} onApprove={() => selectedOrder && transition.mutate({ path: `/api/purchase-orders/${selectedOrder.id}/approve` })} onReject={() => selectedOrder && transition.mutate({ path: `/api/purchase-orders/${selectedOrder.id}/reject`, body: { reason: rejectReason } })} onOrdered={() => selectedOrder && transition.mutate({ path: `/api/purchase-orders/${selectedOrder.id}/mark-ordered` })} onViewPdf={() => handleViewPurchaseOrderPdf(selectedOrder)} onDownloadPdf={() => handleDownloadPurchaseOrderPdf(selectedOrder)} /> : null}
      {section === 'review' && mode === 'order' ? <ReceivingView order={selectedOrder} receivedNow={receivedNow} setReceivedNow={setReceivedNow} onReceive={() => selectedOrder && transition.mutate({ path: `/api/purchase-orders/${selectedOrder.id}/receive`, body: { lines: asArray(selectedOrder.lines).map((line) => ({ lineId: line.id, quantityReceivedNow: numberOrZero(receivedNow[line.id]), allowOverReceive: false })) } })} /> : null}
      {section === 'receiving' && sections.includes('receiving') ? <ReceivingView order={selectedOrder} receivedNow={receivedNow} setReceivedNow={setReceivedNow} onReceive={() => selectedOrder && transition.mutate({ path: `/api/purchase-orders/${selectedOrder.id}/receive`, body: { lines: asArray(selectedOrder.lines).map((line) => ({ lineId: line.id, quantityReceivedNow: numberOrZero(receivedNow[line.id]), allowOverReceive: false })) } })} /> : null}
      {section === 'history' && sections.includes('history') && mode === 'inventory' ? <InventoryHistoryView counts={countList} message={inventoryMessage ? t(inventoryMessage) : undefined} exportError={inventoryPdfError ? t(inventoryPdfError) : undefined} exportingCountId={exportInventoryPdf.isPending ? exportInventoryPdf.variables?.count.id : undefined} onExportPdf={handleExportInventoryPdf} /> : null}
      {section === 'history' && sections.includes('history') && mode !== 'inventory' ? <OrderHistoryView orders={orderList} onOpen={(order) => { setSelectedOrderId(order.id); setSection('review'); }} /> : null}
      {section === 'catalog' && sections.includes('catalog') ? <CatalogManagementView products={productList} vendors={vendorList} isBusinessPartner={isBusinessPartner} /> : null}
      <ConfirmDialog
        visible={inventoryDialog?.type === 'leave'}
        title={t('saveInventoryDraftBeforeLeaving')}
        errorMessage={inventoryDialogError ? t(inventoryDialogError) : undefined}
        onCancel={() => setInventoryDialog(undefined)}
        actions={inventoryDialog?.type === 'leave' ? [
          { label: t('saveAndLeave'), onPress: () => saveInventoryAndLeave(inventoryDialog.leave), disabled: saveCount.isPending },
          { label: t('leaveWithoutSaving'), onPress: () => leaveWithoutSavingInventory(inventoryDialog.leave), variant: 'danger', disabled: saveCount.isPending },
          { label: t('cancel'), onPress: () => setInventoryDialog(undefined), variant: 'secondary', disabled: saveCount.isPending }
        ] : []}
      />
      <ConfirmDialog
        visible={inventoryDialog?.type === 'draftChoice'}
        title={t('continuePreviousInventoryCount')}
        onCancel={startNewInventoryDraft}
        actions={[
          { label: t('continueEditing'), onPress: continueInventoryDraft },
          { label: t('startNewCount'), onPress: startNewInventoryDraft, variant: 'secondary' }
        ]}
      />
      <ConfirmDialog
        visible={Boolean(deleteCatalogProduct)}
        title={deleteCatalogProduct ? `${t('deleteItem')} ${deleteCatalogProduct.name}?` : t('deleteItem')}
        message={t('inventoryItemDeleteHistoryNotice')}
        onCancel={() => setDeleteCatalogProduct(undefined)}
        actions={deleteCatalogProduct ? [
          { label: t('deleteItem'), onPress: () => deleteInventoryCatalogItem.mutate(deleteCatalogProduct), variant: 'danger', disabled: deleteInventoryCatalogItem.isPending },
          { label: t('cancel'), onPress: () => setDeleteCatalogProduct(undefined), variant: 'secondary', disabled: deleteInventoryCatalogItem.isPending }
        ] : []}
      />
      <ConfirmDialog
        visible={Boolean(deleteOrderCatalogProduct)}
        title={deleteOrderCatalogProduct ? `${t('deleteItem')} ${deleteOrderCatalogProduct.name}?` : t('deleteItem')}
        message={t('orderItemDeleteHistoryNotice')}
        onCancel={() => setDeleteOrderCatalogProduct(undefined)}
        actions={deleteOrderCatalogProduct ? [
          { label: t('deleteItem'), onPress: () => deleteOrderCatalogItem.mutate(deleteOrderCatalogProduct), variant: 'danger', disabled: deleteOrderCatalogItem.isPending },
          { label: t('cancel'), onPress: () => setDeleteOrderCatalogProduct(undefined), variant: 'secondary', disabled: deleteOrderCatalogItem.isPending }
        ] : []}
      />
    </AppScreen>
  );
}

function loadInventoryDraft(count: InventoryCountSession) {
  return count.lines.reduce((next, line) => ({ ...next, [line.productId]: String(line.quantityOnHand ?? '') }), {} as Quantities);
}

function asArray<T>(value: T[] | null | undefined): T[] {
  return Array.isArray(value) ? value : [];
}

function hasApiCode(error: unknown, code: string) {
  return error instanceof ApiError ? error.code === code : (error as { code?: string } | null)?.code === code;
}

function pdfMessageKey(error: unknown, fallback: TranslationKey): TranslationKey {
  const code = error instanceof ApiError ? error.code : (error as { code?: string } | null)?.code;
  if (code === 'ORDER_PLAN_PDF_NOT_FOUND') return 'pdfNotAvailable';
  if (code === 'AUTH_STORE_REQUIRED' || code === 'AUTH_ACTIVE_EMPLOYEE_REQUIRED') return 'pdfPermissionDenied';
  if (code === 'AUTH_INVALID_TOKEN' || code === 'AUTH_INVALID_REFRESH_TOKEN' || code === 'AUTH_REQUIRED') return 'pdfSessionExpired';
  if (code === 'NETWORK_ERROR') return 'pdfNetworkError';
  return fallback;
}
