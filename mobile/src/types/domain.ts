export type StoreCode = 'SEATTLE' | 'REDMOND';

export type Position =
  | 'OWNER'
  | 'FINANCIAL_MANAGER'
  | 'MANAGER'
  | 'FOOD_RUNNER'
  | 'HOST'
  | 'BARTENDER'
  | 'SERVER_ONE_STAR'
  | 'SERVER_TWO_STAR'
  | 'SHIFT_LEADER';

export type EmployeeStatus = 'ACTIVE' | 'DEACTIVATED';

export type EmployeePrivate = {
  id: number;
  englishName: string;
  preferredName: string;
  displayName: string;
  email: string;
  phone: string;
  homeStore: StoreCode;
  eligibleStores: StoreCode[];
  positions: Position[];
  status: EmployeeStatus;
  toastPin?: string;
  lastLoginAt?: string;
  createdAt: string;
};

export type EmployeePublic = {
  id: number;
  displayName: string;
  profilePhotoKey?: string;
  homeStore: StoreCode;
  positions: Position[];
  status: EmployeeStatus;
};

export type Invitation = {
  id: number;
  positions: Position[];
  creatorId: number;
  createdAt: string;
  expiresAt: string;
  usedAt?: string;
  revokedAt?: string;
  tokenVersion: number;
  status: 'ACTIVE' | 'USED' | 'EXPIRED' | 'REVOKED' | 'SUPERSEDED';
  activationLink?: string;
};

export type CatalogCategory =
  | 'PACKAGING'
  | 'DISPOSABLES'
  | 'BEER'
  | 'SAKE'
  | 'SOJU'
  | 'SPIRITS'
  | 'COCKTAIL_INGREDIENTS'
  | 'NON_ALCOHOLIC_BEVERAGES'
  | 'TEA_AND_MILK_TEA'
  | 'PRODUCE'
  | 'CLEANING'
  | 'RESTROOM_SUPPLIES'
  | 'OFFICE_AND_POS'
  | 'FROZEN_FOOD'
  | 'DRY_GOODS'
  | 'REFRIGERATED_FOOD'
  | 'OTHER';

export type CatalogUnit = 'EA' | 'BOTTLE' | 'CAN' | 'BAG' | 'BOX' | 'CASE' | 'PACK' | 'CARTON' | 'GALLON' | 'LITER' | 'COUNT' | 'ROLL' | 'CONTAINER' | 'OTHER';

export type PurchaseOrderStatus = 'DRAFT' | 'SUBMITTED' | 'APPROVED' | 'REJECTED' | 'ORDERED' | 'PARTIALLY_RECEIVED' | 'RECEIVED' | 'CANCELLED';
export type InventoryCountStatus = 'DRAFT' | 'IN_PROGRESS' | 'SUBMITTED' | 'REVIEWED' | 'LOCKED' | 'COMPLETED' | 'CANCELLED';
export type OrderPlanStatus = 'DRAFT' | 'IN_PROGRESS' | 'SUBMITTED' | 'APPROVED' | 'RETURNED' | 'COMPLETED' | 'REJECTED' | 'CANCELLED';
export type InventoryBusiness = 'BIANGBIANG_FRONT' | 'PAPER_FAN';
export type OrderBusiness = 'BIANGBIANG_FRONT' | 'PAPER_FAN';

export type Vendor = {
  id: number;
  locationCode: StoreCode;
  name: string;
  vendorCode?: string;
  deliveryDays: string[];
  active: boolean;
  notes?: string;
};

export type OrderProduct = {
  id: number;
  locationCode: StoreCode;
  vendorId: number;
  vendorName: string;
  vendorProductCode?: string;
  name: string;
  category: CatalogCategory;
  inventoryBusiness?: InventoryBusiness;
  orderBusiness?: OrderBusiness;
  orderBusinessName?: string;
  inventoryUnit: CatalogUnit;
  inventoryUnitLabel?: string;
  orderUnit: CatalogUnit;
  orderUnitLabel?: string;
  packageSpecification?: string;
  unitPrice?: number;
  active: boolean;
  parLevel?: number;
  reorderPoint?: number;
};

export type PurchaseOrderLine = {
  id: number;
  productId: number;
  productNameSnapshot: string;
  productCodeSnapshot?: string;
  packageSpecificationSnapshot?: string;
  orderUnitSnapshot: CatalogUnit;
  unitPriceSnapshot: number;
  sourceInventoryQuantitySnapshot?: number;
  suggestedOrderQuantity?: number;
  finalOrderQuantity?: number;
  currentInventoryQuantity?: number;
  requestedQuantity: number;
  approvedQuantity?: number;
  receivedQuantity: number;
  lineTotal: number;
  notes?: string;
};

export type PurchaseOrder = {
  id: number;
  locationCode: StoreCode;
  orderBusiness?: OrderBusiness;
  orderBusinessName?: string;
  vendorId: number;
  vendorName: string;
  sourceInventorySessionId?: number;
  orderPlanSessionId?: number;
  orderNumber: string;
  businessDate: string;
  expectedDeliveryDate?: string;
  status: PurchaseOrderStatus;
  subtotal: number;
  tax: number;
  fees: number;
  total: number;
  currency: string;
  submittedByEmployeeId?: number;
  submittedByNameSnapshot?: string;
  submittedAt?: string;
  approvedByEmployeeId?: number;
  approvedByNameSnapshot?: string;
  approvedAt?: string;
  orderedAt?: string;
  receivedAt?: string;
  rejectedByEmployeeId?: number;
  rejectedByNameSnapshot?: string;
  rejectedAt?: string;
  rejectionReason?: string;
  vendorConfirmationNumber?: string;
  externalOrderNotes?: string;
  pdf?: {
    documentId: number;
    purchaseOrderId: number;
    versionNumber: number;
    filename: string;
    mimeType: string;
    generatedAt: string;
    generatedByEmployeeId: number;
    checksumSha256: string;
    currentVersion: boolean;
  };
  lines: PurchaseOrderLine[];
};

export type InventoryCountSession = {
  id: number;
  locationCode: StoreCode;
  inventoryBusiness: InventoryBusiness;
  inventoryBusinessName: string;
  businessDate: string;
  status: InventoryCountStatus;
  createdByEmployeeId?: number;
  assignedCounterEmployeeId?: number;
  dueDate?: string;
  dueTime?: string;
  startedAt?: string;
  completedByEmployeeId?: number;
  completedByNameSnapshot?: string;
  completedAt?: string;
  submittedByEmployeeId?: number;
  submittedByNameSnapshot?: string;
  submittedAt?: string;
  reviewedByEmployeeId?: number;
  reviewedByNameSnapshot?: string;
  reviewedAt?: string;
  lockedByEmployeeId?: number;
  lockedByNameSnapshot?: string;
  lockedAt?: string;
  overrideReason?: string;
  cancelledAt?: string;
  notes?: string;
  createdAt?: string;
  updatedAt?: string;
  lines: Array<{ id: number; productId: number; vendorId: number; vendorName: string; productCode?: string; productName: string; packageSpecification?: string; inventoryUnit: CatalogUnit; previousCount?: number; quantityOnHand: number; unit: CatalogUnit; notes?: string }>;
};

export type OrderPlanLine = {
  id: number;
  productId: number;
  vendorId: number;
  vendorNameSnapshot: string;
  productCodeSnapshot?: string;
  productNameSnapshot: string;
  packageSpecificationSnapshot?: string;
  inventoryUnitSnapshot: CatalogUnit;
  orderUnitSnapshot: CatalogUnit;
  unitPriceSnapshot?: number;
  sourceInventoryQuantity?: number;
  parLevelSnapshot?: number;
  reorderPointSnapshot?: number;
  previousOrderQuantity?: number;
  suggestedOrderQuantity?: number;
  finalOrderQuantity?: number;
  notes?: string;
};

export type OrderPlan = {
  id: number;
  locationCode: StoreCode;
  orderBusiness: OrderBusiness;
  orderBusinessName: string;
  sourceInventorySessionId?: number;
  inventoryBusinessDate?: string;
  inventoryCompletedByEmployeeId?: number;
  inventoryCompletedByNameSnapshot?: string;
  inventoryCompletedAt?: string;
  businessDate: string;
  status: OrderPlanStatus;
  createdByEmployeeId: number;
  assignedOrdererEmployeeId?: number;
  dueDate?: string;
  dueTime?: string;
  notes?: string;
  startedAt?: string;
  submittedByEmployeeId?: number;
  submittedByNameSnapshot?: string;
  submittedAt?: string;
  lastModifiedByEmployeeId?: number;
  lastModifiedByNameSnapshot?: string;
  updatedAt?: string;
  reviewedByEmployeeId?: number;
  reviewedByNameSnapshot?: string;
  reviewedAt?: string;
  reviewNote?: string;
  completedByEmployeeId?: number;
  completedByNameSnapshot?: string;
  completedAt?: string;
  cancelledAt?: string;
  lines: OrderPlanLine[];
  vendorOrders: PurchaseOrder[];
};

export type OrderPlanPdfMetadata = {
  documentId: number;
  orderPlanId: number;
  version: number;
  filename: string;
  mimeType: string;
  byteSize: number;
  sha256: string;
  generatedAt: string;
  generatedByEmployeeId: number;
  generatedByNameSnapshot: string;
  current: boolean;
};

export type OrderInventoryReference = {
  inventoryCountId?: number;
  locationCode: StoreCode;
  orderBusiness: OrderBusiness;
  orderBusinessName: string;
  countedAt?: string;
  lines: Array<{
    inventoryProductId: number;
    itemName: string;
    normalizedItemName: string;
    unit?: string;
    quantity: number;
  }>;
};
