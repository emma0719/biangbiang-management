import React, { useMemo, useState } from 'react';
import { Pressable, ScrollView, StyleSheet, Switch, Text, TextInput, View } from 'react-native';
import { BrandTextInput, EmptyState, InlineMessage, LabelValue, PaperCard, PrimaryButton, SecondaryButton, SectionBanner, StatusBadge } from '../../components/designSystem';
import { useI18n } from '../../i18n/I18nProvider';
import { TranslationKey } from '../../i18n/translations';
import { colors, spacing, typography } from '../../theme/theme';
import { InventoryBusiness, InventoryCountSession, OrderBusiness, OrderInventoryReference, OrderPlan, OrderPlanPdfMetadata, OrderProduct, PurchaseOrder, PurchaseOrderStatus, Vendor } from '../../types/domain';

export type OrderingSection = 'dashboard' | 'vendors' | 'editor' | 'inventory' | 'plans' | 'review' | 'receiving' | 'history' | 'catalog';
export type Quantities = Record<number, string>;
export type InventoryCatalogForm = { name: string; unit: string; vendorName: string };
export const inventoryBusinesses: Array<{ code: InventoryBusiness; label: string }> = [
  { code: 'BIANGBIANG_FRONT', label: 'BiangBiang Front' },
  { code: 'PAPER_FAN', label: 'Paper Fan' }
];
export const orderBusinesses: Array<{ code: OrderBusiness; label: string }> = inventoryBusinesses;
type DisplayPlanLine = {
  id?: number;
  productId: number;
  vendorNameSnapshot: string;
  productNameSnapshot: string;
  productCodeSnapshot?: string;
  packageSpecificationSnapshot?: string;
  inventoryUnitSnapshot: string;
  orderUnitSnapshot: string;
  unitPriceSnapshot?: number;
  sourceInventoryQuantity?: number;
  parLevelSnapshot?: number;
  suggestedOrderQuantity?: number;
  finalOrderQuantity?: number;
};
type OrderSessionSort = 'newest' | 'oldest' | 'updated' | 'status';

export function SectionNav({ active, setActive, sections, labels }: { active: OrderingSection; setActive: (section: OrderingSection) => void; sections?: OrderingSection[]; labels?: Partial<Record<OrderingSection, string>> }) {
  const { t } = useI18n();
  const items: Array<[OrderingSection, string]> = [
    ['dashboard', t('orderingDashboard')],
    ['vendors', t('vendorSelection')],
    ['editor', t('vendorOrderEditor')],
    ['inventory', t('inventoryCount')],
    ['plans', t('orderPlans')],
    ['review', t('orderReview')],
    ['receiving', t('receiving')],
    ['history', t('orderHistory')],
    ['catalog', t('catalogManagement')]
  ];
  const visible = sections ? items.filter(([key]) => sections.includes(key)) : items;
  return <View style={styles.navWrap}>{visible.map(([key, label]) => <Chip key={key} label={labels?.[key] ?? label} active={active === key} onPress={() => setActive(key)} />)}</View>;
}

export function OrderingDashboard({ orders, counts, plans, vendorCount, onSection }: { orders: PurchaseOrder[]; counts: InventoryCountSession[]; plans: OrderPlan[]; vendorCount: number; onSection: (section: OrderingSection) => void }) {
  const { t } = useI18n();
  const statusCount = (status: PurchaseOrderStatus) => orders.filter((order) => order.status === status).length;
  const latestCompleted = counts.find((count) => ['SUBMITTED', 'REVIEWED', 'LOCKED', 'COMPLETED'].includes(count.status));
  return (
    <>
      <SectionBanner label={t('orderingDashboard')} tone="green" />
      <SectionBanner label={t('inventoryDashboard')} tone="green" />
      <View style={styles.grid}>
        <SummaryCard label={t('startInventoryCount')} value={String(counts.filter((count) => count.status === 'DRAFT').length)} onPress={() => onSection('inventory')} />
        <SummaryCard label={t('inventoryInProgress')} value={String(counts.filter((count) => count.status === 'IN_PROGRESS').length)} onPress={() => onSection('inventory')} />
        <SummaryCard label={t('submittedCountsAwaitingReview')} value={String(counts.filter((count) => count.status === 'SUBMITTED').length)} onPress={() => onSection('inventory')} />
        <SummaryCard label={t('latestCompletedInventory')} value={latestCompleted?.businessDate ?? '-'} onPress={() => onSection('plans')} />
      </View>
      <SectionBanner label={t('orderingWork')} tone="burgundy" />
      <View style={styles.grid}>
        <SummaryCard label={t('createOrder')} value={String(vendorCount)} onPress={() => onSection('vendors')} />
        <SummaryCard label={t('createOrderPlan')} value={String(plans.length)} onPress={() => onSection('plans')} />
        <SummaryCard label={t('orderPlansInProgress')} value={String(plans.filter((plan) => plan.status === 'DRAFT' || plan.status === 'IN_PROGRESS').length)} onPress={() => onSection('plans')} />
        <SummaryCard label={t('vendorOrdersAwaitingApproval')} value={String(statusCount('SUBMITTED'))} onPress={() => onSection('review')} />
        <SummaryCard label={t('draftOrders')} value={String(statusCount('DRAFT'))} onPress={() => onSection('history')} />
        <SummaryCard label={t('awaitingApproval')} value={String(statusCount('SUBMITTED'))} onPress={() => onSection('review')} />
        <SummaryCard label={t('readyToOrder')} value={String(statusCount('APPROVED'))} onPress={() => onSection('review')} />
        <SummaryCard label={t('deliveriesToReceive')} value={String(statusCount('ORDERED') + statusCount('PARTIALLY_RECEIVED'))} onPress={() => onSection('receiving')} />
      </View>
    </>
  );
}

export function VendorSelectionView({ vendors, products, selectedVendorId, onSelect }: { vendors: Vendor[]; products: OrderProduct[]; selectedVendorId?: number; onSelect: (vendor: Vendor) => void }) {
  const { t } = useI18n();
  return (
    <>
      <SectionBanner label={t('vendorSelection')} tone="green" />
      {vendors.length === 0 ? <EmptyState message={t('empty')} /> : null}
      {vendors.map((vendor) => (
        <Pressable key={vendor.id} onPress={() => onSelect(vendor)}>
          <PaperCard>
            <Text style={styles.cardTitle}>{vendor.name}</Text>
            <StatusBadge label={`${products.filter((product) => product.vendorId === vendor.id).length} ${t('product')}`} tone={vendor.id === selectedVendorId ? 'green' : 'paper'} />
            <LabelValue label={t('expectedDelivery')} value={vendor.deliveryDays?.join(', ') || t('empty')} />
          </PaperCard>
        </Pressable>
      ))}
    </>
  );
}

export function VendorOrderEditorView(props: { vendor?: Vendor; products: OrderProduct[]; search: string; setSearch: (value: string) => void; quantities: Quantities; setQuantities: (value: Quantities) => void; inventory: Quantities; setInventory: (value: Quantities) => void; hideZero: boolean; setHideZero: (value: boolean) => void; subtotal: number; canCreateOrder: boolean; submitterName: string; onSave: () => void; onSubmit: () => void }) {
  const { t } = useI18n();
  return (
    <>
      <SectionBanner label={props.vendor?.name ?? t('selectVendor')} tone="burgundy" />
      <PaperCard>
        <BrandTextInput label={t('search')} value={props.search} onChangeText={props.setSearch} />
        <View style={styles.toggleRow}><Text style={styles.bodyText}>{t('hideZeroQuantity')}</Text><Switch value={props.hideZero} onValueChange={props.setHideZero} /></View>
      </PaperCard>
      {props.products.length ? props.products.map((product) => <ProductQuantityRow key={product.id} product={product} quantity={props.quantities[product.id] ?? '0'} inventory={props.inventory[product.id] ?? ''} setQuantity={(value) => props.setQuantities({ ...props.quantities, [product.id]: value })} setInventory={(value) => props.setInventory({ ...props.inventory, [product.id]: value })} />) : <EmptyState message={t('noProductsFound')} />}
      <PaperCard><LabelValue label={t('grandTotal')} value={money(props.subtotal)} /><LabelValue label={t('submittingAs')} value={props.submitterName} /><PrimaryButton label={t('saveDraft')} onPress={props.onSave} disabled={!props.canCreateOrder} /><PrimaryButton label={t('submitForApproval')} onPress={props.onSubmit} disabled={!props.canCreateOrder} /></PaperCard>
    </>
  );
}

export function OrderWorkflowView(props: { businessLabel: string; vendors: Vendor[]; products: OrderProduct[]; allProducts: OrderProduct[]; selectedVendor?: Vendor; search: string; setSearch: (value: string) => void; quantities: Quantities; setQuantities: (value: Quantities) => void; saving: boolean; submitting: boolean; message?: string; error?: string; canManageCatalog?: boolean; managingCatalog?: boolean; catalogForm?: InventoryCatalogForm; setCatalogForm?: (form: InventoryCatalogForm) => void; editingCatalogProductId?: number; catalogSaving?: boolean; catalogDeletingProductId?: number; catalogMessage?: string; catalogError?: string; inventoryReference?: OrderInventoryReference; importingReference?: boolean; onImportLatestInventory?: () => void; onChangeBusiness: () => void; onManageCatalog?: () => void; onSaveCatalog?: () => void; onEditCatalog?: (product: OrderProduct) => void; onCancelEditCatalog?: () => void; onDeleteCatalog?: (product: OrderProduct) => void; onSelectVendor: (vendor: Vendor) => void; onSave: () => void; onSubmit: () => void }) {
  const { t } = useI18n();
  const filteredProducts = props.products.filter((product) => product.name.toLowerCase().includes(props.search.toLowerCase()));
  const itemCount = props.products.filter((product) => numberOrZero(props.quantities[product.id]) > 0).length;
  const totalQuantity = props.products.reduce((sum, product) => sum + numberOrZero(props.quantities[product.id]), 0);
  const disabled = !props.selectedVendor || props.products.length === 0 || props.saving || props.submitting;
  return (
    <>
      <SectionBanner label={`${t('newOrder')} · ${props.businessLabel}`} tone="burgundy" />
      <SecondaryButton label={t('changeOrderBusiness')} onPress={props.onChangeBusiness} />
      {props.canManageCatalog ? <SecondaryButton label={props.managingCatalog ? t('done') : t('manageItems')} onPress={props.onManageCatalog ?? (() => undefined)} /> : null}
      <SecondaryButton label={props.importingReference ? t('importingLatestInventory') : t('importLatestInventory')} onPress={props.onImportLatestInventory ?? (() => undefined)} disabled={props.importingReference} />
      {props.managingCatalog && props.catalogForm && props.setCatalogForm && props.onSaveCatalog ? (
        <PaperCard>
          <Text style={styles.cardTitle}>{props.editingCatalogProductId ? t('editItem') : t('addItem')}</Text>
          {props.catalogMessage ? <InlineMessage type="success" message={props.catalogMessage} /> : null}
          {props.catalogError ? <InlineMessage type="error" message={props.catalogError} /> : null}
          <BrandTextInput label={t('itemName')} value={props.catalogForm.name} onChangeText={(name) => props.setCatalogForm?.({ ...props.catalogForm!, name })} />
          <BrandTextInput label={t('unit')} value={props.catalogForm.unit} onChangeText={(unit) => props.setCatalogForm?.({ ...props.catalogForm!, unit })} />
          <BrandTextInput label={t('vendor')} value={props.catalogForm.vendorName} placeholder={t('unknownVendor')} onChangeText={(vendorName) => props.setCatalogForm?.({ ...props.catalogForm!, vendorName })} />
          <View style={styles.inventoryManageHeader}>
            <PrimaryButton label={props.editingCatalogProductId ? t('save') : t('addItem')} onPress={props.onSaveCatalog} disabled={Boolean(props.catalogSaving)} />
            {props.editingCatalogProductId ? <SecondaryButton label={t('cancel')} onPress={props.onCancelEditCatalog ?? (() => undefined)} disabled={Boolean(props.catalogSaving)} /> : null}
          </View>
        </PaperCard>
      ) : null}
      {props.vendors.length === 0 ? <EmptyState message={t('empty')} /> : null}
      <View style={styles.orderShell}>
        <View style={styles.vendorRail}>
          {props.vendors.map((vendor) => {
            const active = vendor.id === props.selectedVendor?.id;
            const count = props.allProducts.filter((product) => product.vendorId === vendor.id).length;
            return (
              <Pressable key={vendor.id} accessibilityRole="button" accessibilityLabel={vendor.name} onPress={() => props.onSelectVendor(vendor)} style={[styles.vendorRailItem, active ? styles.vendorRailItemActive : null]}>
                <Text style={[styles.vendorRailText, active ? styles.vendorRailTextActive : null]}>{vendor.name}</Text>
                <Text style={[styles.vendorRailMeta, active ? styles.vendorRailTextActive : null]}>{count}</Text>
              </Pressable>
            );
          })}
        </View>
        <View style={styles.orderProducts}>
          <BrandTextInput label={t('searchProducts')} value={props.search} onChangeText={props.setSearch} />
          {props.products.length === 0 ? <EmptyState message={t('noProductsForVendor')} /> : null}
          {props.products.length > 0 && filteredProducts.length === 0 ? <EmptyState message={t('noProductsFound')} /> : null}
          {filteredProducts.map((product) => (
            <OrderProductRow
              key={product.id}
              product={product}
              quantity={props.quantities[product.id] ?? '0'}
              latestInventory={latestInventoryForProduct(product, props.inventoryReference)}
              managingCatalog={Boolean(props.managingCatalog)}
              deleting={props.catalogDeletingProductId === product.id}
              onEdit={props.onEditCatalog ? () => props.onEditCatalog?.(product) : undefined}
              onDelete={props.onDeleteCatalog ? () => props.onDeleteCatalog?.(product) : undefined}
              setQuantity={(value) => props.setQuantities({ ...props.quantities, [product.id]: cleanQuantityInput(value) })}
              increment={() => props.setQuantities({ ...props.quantities, [product.id]: String(numberOrZero(props.quantities[product.id]) + 1) })}
              decrement={() => props.setQuantities({ ...props.quantities, [product.id]: String(Math.max(numberOrZero(props.quantities[product.id]) - 1, 0)) })}
            />
          ))}
        </View>
      </View>
      <PaperCard>
        <LabelValue label={t('vendor')} value={props.selectedVendor?.name ?? t('empty')} />
        <LabelValue label={t('items')} value={String(itemCount)} />
        <LabelValue label={t('totalQuantity')} value={String(totalQuantity)} />
        {props.message ? <InlineMessage type="success" message={props.message} /> : null}
        {props.error ? <InlineMessage type="error" message={props.error} /> : null}
        <PrimaryButton label={props.saving ? t('loading') : t('saveDraft')} onPress={props.onSave} disabled={disabled} />
        <PrimaryButton label={props.submitting ? t('submittingOrder') : t('submitOrder')} onPress={props.onSubmit} disabled={disabled} />
      </PaperCard>
    </>
  );
}

export function InventoryBusinessSelector({ onSelect }: { onSelect: (business: InventoryBusiness) => void }) {
  const { t } = useI18n();
  return (
    <>
      <SectionBanner label={t('selectInventoryBusiness')} tone="green" />
      <PaperCard>
        {inventoryBusinesses.map((business) => (
          <Pressable key={business.code} accessibilityRole="button" accessibilityLabel={business.label} onPress={() => onSelect(business.code)} style={styles.businessButton}>
            <Text style={styles.businessButtonTitle}>{business.label}</Text>
          </Pressable>
        ))}
      </PaperCard>
    </>
  );
}

export function OrderBusinessSelector({ onSelect }: { onSelect: (business: OrderBusiness) => void }) {
  const { t } = useI18n();
  return (
    <>
      <SectionBanner label={t('selectOrderBusiness')} tone="burgundy" />
      <PaperCard>
        {orderBusinesses.map((business) => (
          <Pressable key={business.code} accessibilityRole="button" accessibilityLabel={business.label} onPress={() => onSelect(business.code)} style={styles.businessButton}>
            <Text style={styles.businessButtonTitle}>{business.label}</Text>
          </Pressable>
        ))}
      </PaperCard>
    </>
  );
}

function OrderProductRow({ product, quantity, latestInventory, managingCatalog, deleting, onEdit, onDelete, setQuantity, increment, decrement }: { product: OrderProduct; quantity: string; latestInventory?: string; managingCatalog?: boolean; deleting?: boolean; onEdit?: () => void; onDelete?: () => void; setQuantity: (value: string) => void; increment: () => void; decrement: () => void }) {
  const { t } = useI18n();
  const displayQuantity = quantity == null ? '0' : quantity;
  return (
    <PaperCard>
      <Text style={styles.cardTitle}>{product.name}</Text>
      <LabelValue label={t('packageSpec')} value={[product.orderUnit, product.packageSpecification].filter(Boolean).join(' · ') || t('empty')} />
      <LabelValue label={t('latestInventory')} value={latestInventory ?? '—'} />
      <View style={styles.stepperRow}>
        <StepperButton label="-" accessibilityLabel={`${t('decreaseQuantity')} ${product.name}`} onPress={decrement} />
        <BrandTextInput label={t('orderQuantity')} value={displayQuantity} keyboardType="number-pad" onChangeText={setQuantity} onBlur={() => setQuantity(displayQuantity)} />
        <StepperButton label="+" accessibilityLabel={`${t('increaseQuantity')} ${product.name}`} onPress={increment} />
        {managingCatalog ? (
          <View style={styles.inventoryProductActions}>
            <Pressable accessibilityRole="button" accessibilityLabel={`${t('editItem')} ${product.name}`} testID={`order-edit-item-${product.id}`} onPress={onEdit} style={styles.inventoryIconButton}><Text style={styles.inventoryIconText}>✎</Text></Pressable>
            <Pressable accessibilityRole="button" accessibilityLabel={`${t('deleteItem')} ${product.name}`} testID={`order-delete-item-${product.id}`} onPress={onDelete} style={[styles.inventoryIconButton, styles.inventoryIconDanger, deleting ? styles.inventorySubmitButtonDisabled : null]} disabled={deleting}><Text style={styles.inventoryIconText}>🗑</Text></Pressable>
          </View>
        ) : null}
      </View>
    </PaperCard>
  );
}

function StepperButton({ label, accessibilityLabel, onPress }: { label: string; accessibilityLabel: string; onPress: () => void }) {
  return <Pressable accessibilityRole="button" accessibilityLabel={accessibilityLabel} onPress={onPress} style={styles.stepperButton}><Text style={styles.stepperText}>{label}</Text></Pressable>;
}

export function InventoryCountView({ businessLabel, vendors, products, selectedVendor, inventory, setInventory, search, setSearch, submitterName, saving, submitting, message, error, canManageCatalog, managingCatalog, catalogForm, setCatalogForm, editingCatalogProductId, catalogSaving, catalogDeletingProductId, catalogMessage, catalogError, onManageCatalog, onSaveCatalog, onEditCatalog, onCancelEditCatalog, onDeleteCatalog, onChangeBusiness, onSelectVendor, onSubmit }: { businessLabel: string; vendors: Vendor[]; products: OrderProduct[]; selectedVendor?: Vendor; inventory: Quantities; setInventory: (value: Quantities) => void; search: string; setSearch: (value: string) => void; submitterName: string; saving: boolean; submitting: boolean; message?: string; error?: string; canManageCatalog?: boolean; managingCatalog?: boolean; catalogForm?: InventoryCatalogForm; setCatalogForm?: (form: InventoryCatalogForm) => void; editingCatalogProductId?: number; catalogSaving?: boolean; catalogDeletingProductId?: number; catalogMessage?: string; catalogError?: string; onManageCatalog?: () => void; onSaveCatalog?: () => void; onEditCatalog?: (product: OrderProduct) => void; onCancelEditCatalog?: () => void; onDeleteCatalog?: (product: OrderProduct) => void; onChangeBusiness: () => void; onSelectVendor: (vendor: Vendor) => void; onSubmit: () => void }) {
  const { t } = useI18n();
  const productCountsByVendor = useMemo(() => {
    const counts = new Map<number, number>();
    products.forEach((product) => counts.set(product.vendorId, (counts.get(product.vendorId) ?? 0) + 1));
    return counts;
  }, [products]);
  const currentVendorProducts = products.filter((product) => selectedVendor ? product.vendorId === selectedVendor.id : false);
  const scopedProducts = managingCatalog ? products : currentVendorProducts;
  const visibleProducts = scopedProducts.filter((product) => product.name.toLowerCase().includes(search.toLowerCase()));
  const countedItems = products.filter((product) => inventory[product.id] !== undefined && inventory[product.id] !== '').length;
  const totalCountedQuantity = products.reduce((sum, product) => sum + numberOrZero(inventory[product.id]), 0);
  const canSave = !saving && !submitting;
  const currentVendorName = selectedVendor?.name === 'No Delivery' ? t('localPurchaseNoDelivery') : selectedVendor?.name ?? t('empty');
  return (
    <>
      <SectionBanner label={`${t('inventoryCount')} · ${businessLabel}`} tone="green" />
      <SecondaryButton label={t('changeInventoryBusiness')} onPress={onChangeBusiness} />
      {canManageCatalog ? <SecondaryButton label={managingCatalog ? t('done') : t('manageItems')} onPress={onManageCatalog ?? (() => undefined)} /> : null}
      <PaperCard><LabelValue label={t('inventorySubmittingAs')} value={submitterName} /></PaperCard>
      {message ? <InlineMessage type="success" message={message} /> : null}
      {error ? <InlineMessage type="error" message={error} /> : null}
      {managingCatalog && catalogForm && setCatalogForm && onSaveCatalog ? (
        <PaperCard>
          <Text style={styles.cardTitle}>{editingCatalogProductId ? t('editItem') : t('addItem')}</Text>
          {catalogMessage ? <InlineMessage type="success" message={catalogMessage} /> : null}
          {catalogError ? <InlineMessage type="error" message={catalogError} /> : null}
          <BrandTextInput label={t('itemName')} value={catalogForm.name} onChangeText={(name) => setCatalogForm({ ...catalogForm, name })} />
          <BrandTextInput label={t('unit')} value={catalogForm.unit} onChangeText={(unit) => setCatalogForm({ ...catalogForm, unit })} />
          <BrandTextInput label={t('vendor')} value={catalogForm.vendorName} placeholder={t('unknownVendor')} onChangeText={(vendorName) => setCatalogForm({ ...catalogForm, vendorName })} />
          <View style={styles.inventoryManageHeader}>
            <PrimaryButton label={editingCatalogProductId ? t('save') : t('addItem')} onPress={onSaveCatalog} disabled={Boolean(catalogSaving)} />
            {editingCatalogProductId ? <SecondaryButton label={t('cancel')} onPress={onCancelEditCatalog ?? (() => undefined)} disabled={Boolean(catalogSaving)} /> : null}
          </View>
        </PaperCard>
      ) : null}
      {vendors.length === 0 ? <EmptyState message={t('noProductsForVendor')} /> : (
        <View style={styles.inventoryShell}>
          <ScrollView accessibilityLabel={t('vendorSelection')} style={styles.inventoryVendorRail} contentContainerStyle={styles.inventoryVendorRailContent}>
            {vendors.map((vendor) => {
              const active = selectedVendor?.id === vendor.id;
              const vendorName = vendor.name === 'No Delivery' ? t('localPurchaseNoDelivery') : vendor.name;
              return (
                <Pressable key={vendor.id} accessibilityRole="button" accessibilityLabel={`${t('selectVendor')} ${vendorName}`} onPress={() => onSelectVendor(vendor)} style={[styles.vendorRailItem, active ? styles.vendorRailItemActive : null]}>
                  <Text style={[styles.vendorRailText, active ? styles.vendorRailTextActive : null]}>{vendorName}</Text>
                  <Text style={styles.vendorRailMeta}>{productCountsByVendor.get(vendor.id) ?? 0}</Text>
                </Pressable>
              );
            })}
          </ScrollView>
          <View style={styles.inventoryProducts}>
            <BrandTextInput label={t('searchProducts')} value={search} onChangeText={setSearch} />
            <ScrollView style={styles.inventoryProductScroller} contentContainerStyle={styles.inventoryProductList}>
              {scopedProducts.length === 0 ? <EmptyState message={t('noProductsForVendor')} /> : null}
              {scopedProducts.length > 0 && visibleProducts.length === 0 ? <EmptyState message={t('noProductsFound')} /> : null}
              {visibleProducts.map((product) => (
                <InventoryProductRow
                  key={product.id}
                  product={product}
                  value={inventory[product.id] ?? ''}
                  setValue={(value) => setInventory({ ...inventory, [product.id]: cleanInventoryInput(value) })}
                  commitValue={() => {
                    const current = inventory[product.id];
                    if (current !== undefined && current !== '') {
                      setInventory({ ...inventory, [product.id]: String(numberOrZero(current)) });
                    }
                  }}
                  managingCatalog={Boolean(managingCatalog)}
                  deleting={catalogDeletingProductId === product.id}
                  onEdit={onEditCatalog ? () => onEditCatalog(product) : undefined}
                  onDelete={onDeleteCatalog ? () => onDeleteCatalog(product) : undefined}
                />
              ))}
            </ScrollView>
            <View style={styles.inventoryActionBar}>
              <Text style={styles.cardTitle}>{currentVendorName}</Text>
              <LabelValue label={t('countedItems')} value={String(countedItems)} />
              <LabelValue label={t('vendorProducts')} value={String(products.length)} />
              <LabelValue label={t('totalCountedQuantity')} value={String(totalCountedQuantity)} />
              <Pressable
                accessibilityRole="button"
                accessibilityLabel={t('submitInventory')}
                accessibilityState={{ disabled: !canSave }}
                testID="inventory-submit-button"
                onPress={canSave ? onSubmit : undefined}
                style={[styles.inventorySubmitButton, !canSave ? styles.inventorySubmitButtonDisabled : null]}
              >
                <Text style={styles.inventorySubmitButtonText}>{submitting ? t('submittingInventory') : t('submitInventory')}</Text>
              </Pressable>
            </View>
          </View>
        </View>
      )}
    </>
  );
}

export function InventoryCatalogManagementView({ businessLabel, products, form, setForm, editingProductId, saving, deletingProductId, message, error, onSave, onEdit, onCancelEdit, onDelete }: { businessLabel: string; products: OrderProduct[]; form: InventoryCatalogForm; setForm: (form: InventoryCatalogForm) => void; editingProductId?: number; saving: boolean; deletingProductId?: number; message?: string; error?: string; onSave: () => void; onEdit: (product: OrderProduct) => void; onCancelEdit: () => void; onDelete: (product: OrderProduct) => void }) {
  const { t } = useI18n();
  return (
    <>
      <SectionBanner label={`${t('manageInventoryItems')} · ${businessLabel}`} tone="green" />
      <PaperCard>
        {message ? <InlineMessage type="success" message={message} /> : null}
        {error ? <InlineMessage type="error" message={error} /> : null}
        <BrandTextInput label={t('itemName')} value={form.name} onChangeText={(name) => setForm({ ...form, name })} />
        <BrandTextInput label={t('unit')} value={form.unit} onChangeText={(unit) => setForm({ ...form, unit })} />
        <BrandTextInput label={t('vendor')} value={form.vendorName} placeholder={t('unknownVendor')} onChangeText={(vendorName) => setForm({ ...form, vendorName })} />
        <PrimaryButton label={editingProductId ? t('save') : t('addItem')} onPress={onSave} disabled={saving} />
        {editingProductId ? <SecondaryButton label={t('cancel')} onPress={onCancelEdit} disabled={saving} /> : null}
      </PaperCard>
      {products.length === 0 ? <EmptyState message={t('noProductsFound')} /> : products.map((product) => (
        <PaperCard key={product.id}>
          <LabelValue label={t('vendor')} value={product.vendorName || t('unknownVendor')} />
          <LabelValue label={t('itemName')} value={product.name} />
          <LabelValue label={t('unit')} value={product.inventoryUnitLabel ?? product.inventoryUnit} />
          <LabelValue label={t('statusActive')} value={product.active ? t('statusActive') : t('orderStatusCancelled')} />
          <SecondaryButton label={t('editItem')} accessibilityLabel={`${t('editItem')} ${product.name}`} onPress={() => onEdit(product)} disabled={saving || Boolean(deletingProductId)} />
          <SecondaryButton label={deletingProductId === product.id ? t('loading') : t('deleteItem')} accessibilityLabel={`${t('deleteItem')} ${product.name}`} onPress={() => onDelete(product)} disabled={saving || Boolean(deletingProductId)} />
        </PaperCard>
      ))}
    </>
  );
}

function InventoryProductRow({ product, value, setValue, commitValue, managingCatalog, deleting, onEdit, onDelete }: { product: OrderProduct; value: string; setValue: (value: string) => void; commitValue: () => void; managingCatalog?: boolean; deleting?: boolean; onEdit?: () => void; onDelete?: () => void }) {
  const { t } = useI18n();
  const unit = product.inventoryUnitLabel || product.inventoryUnit;
  return (
    <View style={styles.inventoryProductCard}>
      <View style={styles.inventoryProductRow}>
        {managingCatalog ? <Text style={styles.inventoryVendorText}>{product.vendorName || t('unknownVendor')}</Text> : null}
        <Text style={styles.inventoryProductName}>{product.name}</Text>
        <Text style={styles.inventoryUnitText}>{unit}</Text>
        <TextInput
          accessibilityLabel={`${t('quantity')} ${product.name}`}
          keyboardType="number-pad"
          onBlur={commitValue}
          onChangeText={setValue}
          placeholderTextColor={colors.mutedInk}
          style={styles.inventoryQuantityInput}
          value={value}
        />
        {managingCatalog ? (
          <View style={styles.inventoryProductActions}>
            <Pressable
              accessibilityRole="button"
              accessibilityLabel={`${t('editItem')} ${product.name}`}
              testID={`inventory-edit-item-${product.id}`}
              onPress={onEdit}
              style={styles.inventoryIconButton}
            >
              <Text style={styles.inventoryIconText}>✎</Text>
            </Pressable>
            <Pressable
              accessibilityRole="button"
              accessibilityLabel={`${t('deleteItem')} ${product.name}`}
              testID={`inventory-delete-item-${product.id}`}
              onPress={onDelete}
              style={[styles.inventoryIconButton, styles.inventoryIconDanger, deleting ? styles.inventorySubmitButtonDisabled : null]}
              disabled={deleting}
            >
              <Text style={styles.inventoryIconText}>🗑</Text>
            </Pressable>
          </View>
        ) : null}
      </View>
    </View>
  );
}

export type OrderPlanPdfState = {
  metadata?: OrderPlanPdfMetadata;
  loading: boolean;
  unavailable: boolean;
  errorMessage?: string;
  actionErrorMessage?: string;
  viewing: boolean;
  downloading: boolean;
  onView: () => void;
  onDownload: () => void;
};

export function OrderPlanView({ plans, counts, products, finalQuantities, setFinalQuantities, submitterName, onCreate, onSave, onSubmit, onGenerate, pdfState }: { plans: OrderPlan[]; counts: InventoryCountSession[]; products: OrderProduct[]; finalQuantities: Quantities; setFinalQuantities: (value: Quantities) => void; submitterName: string; onCreate: () => void; onSave: () => void; onSubmit: () => void; onGenerate: () => void; pdfState?: OrderPlanPdfState }) {
  const { t } = useI18n();
  const plan = plans[0];
  const eligibleCounts = counts.filter((count) => ['SUBMITTED', 'REVIEWED', 'LOCKED', 'COMPLETED'].includes(count.status));
  const lines: DisplayPlanLine[] = plan?.lines ?? products.map((product): DisplayPlanLine => ({
    productId: product.id,
    vendorNameSnapshot: product.vendorName,
    productNameSnapshot: product.name,
    productCodeSnapshot: product.vendorProductCode,
    packageSpecificationSnapshot: product.packageSpecification,
    inventoryUnitSnapshot: product.inventoryUnit,
    orderUnitSnapshot: product.orderUnit,
    unitPriceSnapshot: product.unitPrice,
    sourceInventoryQuantity: 0,
    parLevelSnapshot: product.parLevel,
    suggestedOrderQuantity: product.parLevel,
    finalOrderQuantity: product.parLevel
  }));
  return (
    <>
      <SectionBanner label={t('orderPlans')} tone="burgundy" />
      <PaperCard>
        <LabelValue label={t('selectInventorySource')} value={eligibleCounts[0] ? `${eligibleCounts[0].businessDate} · ${eligibleCounts[0].completedByNameSnapshot ?? t('empty')}` : t('empty')} />
        <LabelValue label={t('inventoryBusinessDate')} value={plan?.inventoryBusinessDate ?? eligibleCounts[0]?.businessDate ?? '-'} />
        <LabelValue label={t('completedBy')} value={plan?.inventoryCompletedByNameSnapshot ?? eligibleCounts[0]?.completedByNameSnapshot ?? '-'} />
        <LabelValue label={t('submittingAs')} value={submitterName} />
        <PrimaryButton label={t('createOrderPlan')} onPress={onCreate} disabled={eligibleCounts.length === 0} />
      </PaperCard>
      <ApprovedPdfPanel state={pdfState} />
      {lines.map((line) => (
        <PaperCard key={line.productId}>
          <Text style={styles.cardTitle}>{line.vendorNameSnapshot} · {line.productNameSnapshot}</Text>
          <LabelValue label={t('inventoryBusinessDate')} value={plan?.inventoryBusinessDate ?? eligibleCounts[0]?.businessDate ?? '-'} />
          <LabelValue label={t('completedBy')} value={plan?.inventoryCompletedByNameSnapshot ?? eligibleCounts[0]?.completedByNameSnapshot ?? '-'} />
          <LabelValue label={t('currentInventory')} value={String(line.sourceInventoryQuantity ?? 0)} />
          <LabelValue label={t('parLevel')} value={line.parLevelSnapshot == null ? t('empty') : String(line.parLevelSnapshot)} />
          <LabelValue label={t('suggestedQuantity')} value={line.suggestedOrderQuantity == null ? t('empty') : String(line.suggestedOrderQuantity)} />
          <BrandTextInput label={t('finalQuantity')} value={finalQuantities[line.productId] ?? String(line.finalOrderQuantity ?? '')} keyboardType="decimal-pad" onChangeText={(value) => setFinalQuantities({ ...finalQuantities, [line.productId]: value })} />
        </PaperCard>
      ))}
      <PrimaryButton label={t('saveDraft')} onPress={onSave} disabled={!plan} />
      <PrimaryButton label={t('submitForApproval')} onPress={onSubmit} disabled={!plan} />
      <PrimaryButton label={t('generateVendorOrders')} onPress={onGenerate} disabled={!plan} />
    </>
  );
}

export function OrderSessionReviewView({ session, pdfState, onEditAmounts }: { session?: OrderPlan; pdfState?: OrderPlanPdfState; onEditAmounts?: (session: OrderPlan) => void }) {
  const { t } = useI18n();
  if (!session) return <EmptyState message={t('empty')} />;
  return <>
    <SectionBanner label={`${t('orderDetails')} · Session #${session.id}`} tone="burgundy" />
    <PaperCard>
      <LabelValue label="Session ID" value={String(session.id)} />
      <LabelValue label="Status" value={session.status} />
      <LabelValue label={t('submittedBy')} value={session.submittedByNameSnapshot ?? '-'} />
      <LabelValue label="Submitted At" value={session.submittedAt ?? '-'} />
      <LabelValue label={t('lastModifiedBy')} value={session.lastModifiedByNameSnapshot ?? '-'} />
      <LabelValue label={t('lastUpdated')} value={session.updatedAt ?? '-'} />
      <LabelValue label={t('items')} value={String(session.lines.filter((line) => Number(line.finalOrderQuantity ?? 0) > 0).length)} />
      <LabelValue label={t('vendor')} value={vendorSummary(session)} />
    </PaperCard>
    <ApprovedPdfPanel state={pdfState} />
    {session.lines.map((line) => (
      <PaperCard key={line.id}>
        <Text style={styles.cardTitle}>{line.vendorNameSnapshot} · {line.productNameSnapshot}</Text>
        <LabelValue label={t('orderQuantity')} value={String(line.finalOrderQuantity ?? 0)} />
        <LabelValue label={t('packageSpec')} value={[line.orderUnitSnapshot, line.packageSpecificationSnapshot].filter(Boolean).join(' · ') || t('empty')} />
      </PaperCard>
    ))}
    {onEditAmounts && session.status === 'SUBMITTED' ? <PrimaryButton label={t('editOrderAmounts')} onPress={() => onEditAmounts(session)} /> : null}
  </>;
}

export function OrderSessionAmountsEditView({ session, quantities, setQuantities, saving, errorMessage, onCancel, onSave }: { session?: OrderPlan; quantities: Quantities; setQuantities: (value: Quantities) => void; saving: boolean; errorMessage?: string; onCancel: () => void; onSave: () => void }) {
  const { t } = useI18n();
  if (!session) return <EmptyState message={t('empty')} />;
  const vendors = Array.from(new Set(session.lines.map((line) => line.vendorNameSnapshot)));
  return <>
    <SectionBanner label={`${t('editOrderAmounts')} · Session #${session.id}`} tone="burgundy" />
    <PaperCard>
      <LabelValue label="Session ID" value={String(session.id)} />
      <LabelValue label="Status" value={session.status} />
      <LabelValue label={t('submittedBy')} value={session.submittedByNameSnapshot ?? '-'} />
      <LabelValue label="Submitted At" value={session.submittedAt ?? '-'} />
      {errorMessage ? <InlineMessage type="error" message={errorMessage} /> : null}
    </PaperCard>
    {vendors.map((vendor) => (
      <View key={vendor}>
        <SectionBanner label={vendor} tone="burgundy" />
        {session.lines.filter((line) => line.vendorNameSnapshot === vendor).map((line) => (
          <PaperCard key={line.id}>
            <Text style={styles.cardTitle}>{line.productNameSnapshot}</Text>
            <LabelValue label={t('originalSubmittedQuantity')} value={String(line.finalOrderQuantity ?? 0)} />
            <LabelValue label={t('packageSpec')} value={[line.orderUnitSnapshot, line.packageSpecificationSnapshot].filter(Boolean).join(' · ') || t('empty')} />
            <BrandTextInput label={t('orderQuantity')} value={quantities[line.id] ?? String(line.finalOrderQuantity ?? 0)} keyboardType="decimal-pad" onChangeText={(value) => setQuantities({ ...quantities, [line.id]: cleanQuantityInput(value) })} />
          </PaperCard>
        ))}
      </View>
    ))}
    <PaperCard>
      <SecondaryButton label={t('cancel')} onPress={onCancel} disabled={saving} />
      <PrimaryButton label={saving ? t('savingChanges') : t('saveChanges')} onPress={onSave} disabled={saving || session.status !== 'SUBMITTED'} />
    </PaperCard>
  </>;
}

export function OrderSessionHistoryView({ sessions, viewingPdf, downloadingPdf, removingDraftId, canEditSubmitted, pdfErrorMessage, onStartNew, onOpen, onViewPdf, onDownloadPdf, onEditAmounts, onRemoveDraft }: { sessions: OrderPlan[]; viewingPdf?: number; downloadingPdf?: number; removingDraftId?: number; canEditSubmitted: boolean; pdfErrorMessage?: string; onStartNew: () => void; onOpen: (session: OrderPlan) => void; onViewPdf: (session: OrderPlan) => void; onDownloadPdf: (session: OrderPlan) => void; onEditAmounts: (session: OrderPlan) => void; onRemoveDraft: (session: OrderPlan) => void }) {
  const { t } = useI18n();
  const [sort, setSort] = useState<OrderSessionSort>('newest');
  const drafts = sessions.filter((session) => ['DRAFT', 'IN_PROGRESS'].includes(session.status));
  const history = sessions
    .filter((session) => !['DRAFT', 'IN_PROGRESS', 'CANCELLED'].includes(session.status))
    .sort((left, right) => compareOrderSessions(left, right, sort));
  return <>
    <SectionBanner label={t('orderHistory')} tone="green" />
    {pdfErrorMessage ? <InlineMessage type="error" message={pdfErrorMessage} /> : null}
    <PrimaryButton label={t('startNewOrder')} onPress={onStartNew} />
    <PaperCard>
      <Text style={styles.cardTitle}>{t('sort')}</Text>
      <View style={styles.filterChips}>
        <Chip label={t('newestFirst')} active={sort === 'newest'} onPress={() => setSort('newest')} />
        <Chip label={t('oldestFirst')} active={sort === 'oldest'} onPress={() => setSort('oldest')} />
        <Chip label={t('lastUpdatedSort')} active={sort === 'updated'} onPress={() => setSort('updated')} />
        <Chip label={t('statusSort')} active={sort === 'status'} onPress={() => setSort('status')} />
      </View>
    </PaperCard>
    {drafts.map((session) => <PaperCard key={`draft-${session.id}`}><Text style={styles.cardTitle}>{t('continueEditing')} · Session #{session.id}</Text><StatusBadge label={session.status} /><SecondaryButton label={t('continueEditing')} onPress={() => onOpen(session)} /><SecondaryButton label={removingDraftId === session.id ? t('loading') : t('removeDraft')} onPress={() => onRemoveDraft(session)} disabled={Boolean(removingDraftId)} /></PaperCard>)}
    {history.length === 0 ? <EmptyState message={t('empty')} /> : history.map((session) => <PaperCard key={session.id}>
      <Text style={styles.cardTitle}>Order Session #{session.id}</Text>
      <LabelValue label="Store" value={session.locationCode} />
      <LabelValue label={t('orderBusiness')} value={session.orderBusinessName ?? orderBusinessName(session.orderBusiness)} />
      <LabelValue label={t('submittedBy')} value={session.submittedByNameSnapshot ?? '-'} />
      <LabelValue label="Submitted At" value={session.submittedAt ?? '-'} />
      <LabelValue label={t('lastModifiedBy')} value={session.lastModifiedByNameSnapshot ?? '-'} />
      <LabelValue label={t('lastUpdated')} value={session.updatedAt ?? '-'} />
      <LabelValue label="Status" value={session.status} />
      <LabelValue label={t('items')} value={String(session.lines.filter((line) => Number(line.finalOrderQuantity ?? 0) > 0).length)} />
      <LabelValue label={t('vendor')} value={vendorSummary(session)} />
      <SecondaryButton label={t('viewSession')} onPress={() => onOpen(session)} />
      <SecondaryButton label={viewingPdf === session.id ? t('loading') : t('viewPdf')} onPress={() => onViewPdf(session)} disabled={Boolean(viewingPdf)} />
      <SecondaryButton label={downloadingPdf === session.id ? t('loading') : t('downloadPdf')} onPress={() => onDownloadPdf(session)} disabled={Boolean(downloadingPdf)} />
      {canEditSubmitted && session.status === 'SUBMITTED' ? <SecondaryButton label={t('editOrderAmounts')} onPress={() => onEditAmounts(session)} /> : null}
    </PaperCard>)}
  </>;
}

function ApprovedPdfPanel({ state }: { state?: OrderPlanPdfState }) {
  const { t } = useI18n();
  if (!state) {
    return (
      <PaperCard>
        <Text style={styles.cardTitle}>{t('sessionPdf')}</Text>
        <LabelValue label={t('sessionPdf')} value={t('pdfNotAvailable')} />
      </PaperCard>
    );
  }
  if (state.loading) {
    return (
      <PaperCard>
        <Text style={styles.cardTitle}>{t('sessionPdf')}</Text>
        <LabelValue label={t('sessionPdf')} value={t('loading')} />
      </PaperCard>
    );
  }
  if (state.unavailable) {
    return (
      <PaperCard>
        <Text style={styles.cardTitle}>{t('sessionPdf')}</Text>
        <LabelValue label={t('sessionPdf')} value={t('pdfNotAvailable')} />
      </PaperCard>
    );
  }
  if (state.errorMessage) {
    return (
      <PaperCard tone="danger">
        <Text style={styles.cardTitle}>{t('sessionPdf')}</Text>
        <Text accessibilityRole="alert" style={styles.errorText}>{state.errorMessage}</Text>
      </PaperCard>
    );
  }
  if (!state.metadata) return null;
  return (
    <PaperCard>
      <Text style={styles.cardTitle}>{t('sessionPdf')}</Text>
      <LabelValue label={t('filename')} value={state.metadata.filename} />
      <LabelValue label={t('version')} value={String(state.metadata.version)} />
      <LabelValue label={t('generatedBy')} value={state.metadata.generatedByNameSnapshot} />
      <LabelValue label={t('generatedAt')} value={state.metadata.generatedAt} />
      <LabelValue label={t('fileSize')} value={formatBytes(state.metadata.byteSize)} />
      {state.actionErrorMessage ? <Text accessibilityRole="alert" style={styles.errorText}>{state.actionErrorMessage}</Text> : null}
      <SecondaryButton label={t('viewPdf')} onPress={state.onView} disabled={state.viewing || state.downloading} />
      <SecondaryButton label={t('downloadPdf')} onPress={state.onDownload} disabled={state.viewing || state.downloading} />
    </PaperCard>
  );
}

export function OrderReviewView({ order, isBusinessPartner, rejectReason, setRejectReason, pdfActionErrorMessage, viewingPdf, downloadingPdf, onApprove, onReject, onOrdered, onViewPdf, onDownloadPdf }: { order?: PurchaseOrder; isBusinessPartner: boolean; rejectReason: string; setRejectReason: (value: string) => void; pdfActionErrorMessage?: string; viewingPdf?: boolean; downloadingPdf?: boolean; onApprove: () => void; onReject: () => void; onOrdered: () => void; onViewPdf: () => void; onDownloadPdf: () => void }) {
  const { t } = useI18n();
  if (!order) return <EmptyState message={t('empty')} />;
  return <><SectionBanner label={order.orderNumber} tone="burgundy" /><OrderCard order={order} /><PaperCard>{order.submittedByNameSnapshot ? <LabelValue label={t('submittedBy')} value={order.submittedByNameSnapshot} /> : null}{order.approvedByNameSnapshot ? <LabelValue label={t('approvedBy')} value={order.approvedByNameSnapshot} /> : null}{pdfActionErrorMessage ? <InlineMessage type="error" message={pdfActionErrorMessage} /> : null}{order.pdf ? <><SecondaryButton label={viewingPdf ? t('loading') : t('viewApprovedPdf')} onPress={onViewPdf} disabled={Boolean(viewingPdf || downloadingPdf)} /><SecondaryButton label={downloadingPdf ? t('loading') : t('downloadPdf')} onPress={onDownloadPdf} disabled={Boolean(viewingPdf || downloadingPdf)} /></> : <LabelValue label={t('viewApprovedPdf')} value={t('pdfUnavailable')} />}</PaperCard>{isBusinessPartner ? <PaperCard><PrimaryButton label={t('approve')} onPress={onApprove} disabled={order.status !== 'SUBMITTED'} /><BrandTextInput label={t('rejectionReason')} value={rejectReason} onChangeText={setRejectReason} /><SecondaryButton label={t('reject')} onPress={onReject} disabled={!rejectReason || order.status !== 'SUBMITTED'} /><PrimaryButton label={t('markAsOrdered')} onPress={onOrdered} disabled={order.status !== 'APPROVED'} /></PaperCard> : <EmptyState message={t('businessPartnerOnly')} />}</>;
}

export function ReceivingView({ order, receivedNow, setReceivedNow, onReceive }: { order?: PurchaseOrder; receivedNow: Quantities; setReceivedNow: (value: Quantities) => void; onReceive: () => void }) {
  const { t } = useI18n();
  if (!order) return <EmptyState message={t('empty')} />;
  const lines = Array.isArray(order.lines) ? order.lines : [];
  return <><SectionBanner label={t('receiving')} tone="green" />{lines.length === 0 ? <EmptyState message={t('empty')} /> : lines.map((line) => <PaperCard key={line.id}><Text style={styles.cardTitle}>{line.productNameSnapshot}</Text><LabelValue label={t('orderQuantity')} value={String(line.approvedQuantity ?? line.requestedQuantity)} /><LabelValue label={t('remaining')} value={String((line.approvedQuantity ?? line.requestedQuantity) - line.receivedQuantity)} /><BrandTextInput label={t('receivedNow')} value={receivedNow[line.id] ?? '0'} keyboardType="decimal-pad" onChangeText={(value) => setReceivedNow({ ...receivedNow, [line.id]: value })} /></PaperCard>)}<PrimaryButton label={t('receiveDelivery')} onPress={onReceive} disabled={lines.length === 0} /></>;
}

export function OrderHistoryView({ orders, onOpen }: { orders: PurchaseOrder[]; onOpen: (order: PurchaseOrder) => void }) {
  const { t } = useI18n();
  const [businessFilter, setBusinessFilter] = useState<OrderBusiness | 'ALL'>('ALL');
  const visibleOrders = orders
    .filter((order) => businessFilter === 'ALL' || order.orderBusiness === businessFilter)
    .sort((left, right) => {
      const leftTime = left.receivedAt ?? left.approvedAt ?? left.submittedAt ?? left.businessDate;
      const rightTime = right.receivedAt ?? right.approvedAt ?? right.submittedAt ?? right.businessDate;
      const comparison = rightTime.localeCompare(leftTime);
      return comparison !== 0 ? comparison : right.id - left.id;
    });
  return (
    <>
      <SectionBanner label={t('orderHistory')} tone="green" />
      <PaperCard>
        <Text style={styles.cardTitle}>{t('orderBusiness')}</Text>
        <View style={styles.filterChips}>
          <Chip label={t('allBusinesses')} accessibilityLabel={`${t('orderBusiness')} ${t('allBusinesses')}`} active={businessFilter === 'ALL'} onPress={() => setBusinessFilter('ALL')} />
          {orderBusinesses.map((business) => <Chip key={business.code} label={business.label} accessibilityLabel={`${t('orderBusiness')} ${business.label}`} active={businessFilter === business.code} onPress={() => setBusinessFilter(business.code)} />)}
        </View>
      </PaperCard>
      {visibleOrders.length === 0 ? <EmptyState message={t('empty')} /> : visibleOrders.map((order) => <Pressable key={order.id} onPress={() => onOpen(order)}><OrderCard order={order} /></Pressable>)}
    </>
  );
}

export function InventoryHistoryView({ counts, message, exportError, exportingCountId, onExportPdf }: { counts: InventoryCountSession[]; message?: string; exportError?: string; exportingCountId?: number; onExportPdf?: (count: InventoryCountSession) => void }) {
  const { t } = useI18n();
  const [businessFilter, setBusinessFilter] = useState<InventoryBusiness | 'ALL'>('ALL');
  const submittedCounts = counts
    .filter((count) => isSubmittedInventoryHistoryStatus(count.status))
    .filter((count) => businessFilter === 'ALL' || count.inventoryBusiness === businessFilter)
    .sort((left, right) => {
      const timeComparison = inventoryHistoryTime(right).localeCompare(inventoryHistoryTime(left));
      return timeComparison !== 0 ? timeComparison : right.id - left.id;
    });
  return (
    <>
      <SectionBanner label={t('history')} tone="green" />
      {message ? <InlineMessage type="success" message={message} /> : null}
      {exportError ? <InlineMessage type="error" message={exportError} /> : null}
      <PaperCard>
        <Text style={styles.cardTitle}>{t('inventoryCount')}</Text>
        <View style={styles.historyControls}>
          <View style={styles.historyFilterGroup}>
            <Text style={styles.bodyText}>{t('inventoryBusiness')}</Text>
            <View style={styles.filterChips}>
              <Chip label={t('allBusinesses')} accessibilityLabel={`${t('inventoryBusiness')} ${t('allBusinesses')}`} active={businessFilter === 'ALL'} onPress={() => setBusinessFilter('ALL')} />
              {inventoryBusinesses.map((business) => <Chip key={business.code} label={business.label} accessibilityLabel={`${t('inventoryBusiness')} ${business.label}`} active={businessFilter === business.code} onPress={() => setBusinessFilter(business.code)} />)}
            </View>
          </View>
          <View style={styles.historyFilterGroup}>
            <Text style={styles.bodyText}>{t('sort')}</Text>
            <View style={styles.filterChips}>
              <Chip label={t('latestFirst')} active onPress={() => undefined} />
            </View>
          </View>
        </View>
        {submittedCounts.length === 0 ? <LabelValue label={t('inventoryCount')} value={t('noSubmittedInventoryCounts')} /> : submittedCounts.map((count) => (
          <View key={count.id} style={styles.historyLine}>
            <LabelValue label={t('inventoryBusiness')} value={count.inventoryBusinessName ?? inventoryBusinessName(count.inventoryBusiness)} />
            <LabelValue label={count.businessDate} value={inventoryHistoryStatusLabel(count.status, t)} />
            <LabelValue label={t('countedBy')} value={count.completedByNameSnapshot ?? count.submittedByNameSnapshot ?? t('unknownEmployee')} />
            <SecondaryButton label={exportingCountId === count.id ? t('exportingPdf') : t('exportPdf')} disabled={Boolean(exportingCountId)} onPress={() => onExportPdf?.(count)} />
          </View>
        ))}
      </PaperCard>
    </>
  );
}

function inventoryHistoryTime(count: InventoryCountSession) {
  return count.completedAt ?? count.submittedAt ?? count.updatedAt ?? count.createdAt ?? count.businessDate ?? '';
}

function isSubmittedInventoryHistoryStatus(status: string) {
  return ['SUBMITTED', 'REVIEWED', 'LOCKED', 'COMPLETED'].includes(status);
}

function inventoryHistoryStatusLabel(status: string, t: (key: TranslationKey) => string) {
  if (status === 'SUBMITTED') return t('submitted');
  if (status === 'REVIEWED') return t('reviewed');
  if (status === 'LOCKED') return t('locked');
  if (status === 'COMPLETED') return t('completed');
  return status;
}

export function inventoryBusinessName(business?: InventoryBusiness) {
  return inventoryBusinesses.find((item) => item.code === business)?.label ?? '';
}

export function orderBusinessName(business?: OrderBusiness) {
  return orderBusinesses.find((item) => item.code === business)?.label ?? '';
}

export function CatalogManagementView({ products, vendors, isBusinessPartner }: { products: OrderProduct[]; vendors: Vendor[]; isBusinessPartner: boolean }) {
  const { t } = useI18n();
  return <><SectionBanner label={t('catalogManagement')} tone="burgundy" />{!isBusinessPartner ? <EmptyState message={t('businessPartnerOnly')} /> : null}<PaperCard><LabelValue label={t('vendor')} value={String(vendors.length)} /><LabelValue label={t('product')} value={String(products.length)} /></PaperCard>{products.length === 0 ? <EmptyState message={t('noProductsFound')} /> : products.slice(0, 40).map((product) => <PaperCard key={product.id}><Text style={styles.cardTitle}>{product.name}</Text><LabelValue label={t('vendor')} value={product.vendorName} /><LabelValue label={t('category')} value={product.category} /></PaperCard>)}</>;
}

export function today() {
  return new Date().toISOString().slice(0, 10);
}

export function numberOrZero(value?: string) {
  const parsed = Number(value);
  return Number.isFinite(parsed) && parsed >= 0 ? parsed : 0;
}

function ProductQuantityRow({ product, quantity, inventory, setQuantity, setInventory }: { product: OrderProduct; quantity: string; inventory: string; setQuantity: (value: string) => void; setInventory: (value: string) => void }) {
  const { t } = useI18n();
  const total = Number(quantity || 0) * Number(product.unitPrice ?? 0);
  return (
    <PaperCard>
      <Text style={styles.cardTitle}>{product.name}</Text>
      <LabelValue label={t('packageSpec')} value={product.packageSpecification ?? t('empty')} />
      <LabelValue label={t('unitPrice')} value={product.unitPrice == null ? t('empty') : money(product.unitPrice)} />
      <View style={styles.inputRow}><BrandTextInput label={t('currentInventory')} value={inventory} keyboardType="decimal-pad" onChangeText={setInventory} /><BrandTextInput label={t('orderQuantity')} value={quantity} keyboardType="decimal-pad" onChangeText={setQuantity} /></View>
      <LabelValue label={t('lineTotal')} value={money(total)} />
    </PaperCard>
  );
}

function OrderCard({ order }: { order: PurchaseOrder }) {
  const { t } = useI18n();
  return <PaperCard><Text style={styles.cardTitle}>{order.orderNumber}</Text><StatusBadge label={t(statusKey(order.status))} tone={order.status === 'REJECTED' || order.status === 'CANCELLED' ? 'danger' : 'green'} /><LabelValue label={t('orderBusiness')} value={order.orderBusinessName ?? orderBusinessName(order.orderBusiness)} /><LabelValue label={t('vendor')} value={order.vendorName} /></PaperCard>;
}

function SummaryCard({ label, value, onPress }: { label: string; value: string; onPress: () => void }) {
  return <Pressable onPress={onPress} style={styles.summaryCard}><Text style={styles.summaryValue}>{value}</Text><Text style={styles.summaryLabel}>{label}</Text></Pressable>;
}

function Chip({ label, accessibilityLabel, active, onPress }: { label: string; accessibilityLabel?: string; active: boolean; onPress: () => void }) {
  return <Pressable accessibilityRole="button" accessibilityLabel={accessibilityLabel ?? label} onPress={onPress} style={[styles.chip, active ? styles.chipActive : null]}><Text style={[styles.chipText, active ? styles.chipTextActive : null]}>{label}</Text></Pressable>;
}

function statusKey(status: PurchaseOrderStatus) {
  const keys: Record<PurchaseOrderStatus, TranslationKey> = {
    DRAFT: 'orderStatusDraft',
    SUBMITTED: 'orderStatusSubmitted',
    APPROVED: 'orderStatusApproved',
    REJECTED: 'orderStatusRejected',
    ORDERED: 'orderStatusOrdered',
    PARTIALLY_RECEIVED: 'orderStatusPartiallyReceived',
    RECEIVED: 'orderStatusReceived',
    CANCELLED: 'orderStatusCancelled'
  };
  return keys[status];
}

function money(value: number) {
  return `$${value.toFixed(2)}`;
}

function cleanQuantityInput(value: string) {
  if (value === '') return '';
  return String(Math.floor(numberOrZero(value)));
}

function cleanInventoryInput(value: string) {
  if (value === '') return '';
  if (!/^\d+$/.test(value)) return '';
  return String(Math.max(0, Number.parseInt(value, 10)));
}

function latestInventoryForProduct(product: OrderProduct, reference?: OrderInventoryReference) {
  if (!reference?.inventoryCountId) return '—';
  const key = normalizeReferenceName(product.name);
  const line = reference.lines.find((item) => item.normalizedItemName === key || normalizeReferenceName(item.itemName) === key);
  if (!line) return '—';
  const quantity = line.quantity == null ? '0' : String(line.quantity);
  return [quantity, line.unit].filter(Boolean).join(' ');
}

function vendorSummary(session: OrderPlan) {
  const vendors = Array.from(new Set(session.lines.filter((line) => Number(line.finalOrderQuantity ?? 0) > 0).map((line) => line.vendorNameSnapshot).filter(Boolean)));
  if (vendors.length === 0) return '-';
  if (vendors.length <= 3) return vendors.join(', ');
  return `${vendors.slice(0, 3).join(', ')} +${vendors.length - 3}`;
}

function compareOrderSessions(left: OrderPlan, right: OrderPlan, sort: OrderSessionSort): number {
  if (sort === 'oldest') {
    const comparison = orderSessionSubmittedTime(left).localeCompare(orderSessionSubmittedTime(right));
    return comparison !== 0 ? comparison : left.id - right.id;
  }
  if (sort === 'updated') {
    const comparison = orderSessionUpdatedTime(right).localeCompare(orderSessionUpdatedTime(left));
    return comparison !== 0 ? comparison : right.id - left.id;
  }
  if (sort === 'status') {
    const comparison = orderSessionStatusRank(left.status) - orderSessionStatusRank(right.status);
    return comparison !== 0 ? comparison : compareOrderSessions(left, right, 'newest');
  }
  const comparison = orderSessionSubmittedTime(right).localeCompare(orderSessionSubmittedTime(left));
  return comparison !== 0 ? comparison : right.id - left.id;
}

function orderSessionSubmittedTime(session: OrderPlan) {
  return session.submittedAt ?? session.updatedAt ?? session.businessDate ?? '';
}

function orderSessionUpdatedTime(session: OrderPlan) {
  return session.updatedAt ?? session.submittedAt ?? session.businessDate ?? '';
}

function orderSessionStatusRank(status: string) {
  if (status === 'DRAFT' || status === 'IN_PROGRESS') return 0;
  if (status === 'SUBMITTED') return 1;
  if (status === 'APPROVED') return 2;
  if (status === 'RETURNED' || status === 'REJECTED') return 3;
  return 4;
}

function normalizeReferenceName(value: string) {
  return value.trim().toLowerCase().replace(/[^a-z0-9]+/g, " ").trim();
}

function formatBytes(bytes: number) {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

const styles = StyleSheet.create({
  navWrap: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: spacing.sm
  },
  chip: {
    borderWidth: 1,
    borderColor: colors.border,
    borderRadius: 6,
    paddingVertical: spacing.sm,
    paddingHorizontal: spacing.md,
    backgroundColor: colors.paperLight
  },
  chipActive: {
    backgroundColor: colors.forestGreen,
    borderColor: colors.forestGreen
  },
  chipText: {
    color: colors.ink,
    fontFamily: typography.interface,
    fontWeight: '800'
  },
  chipTextActive: {
    color: colors.white
  },
  grid: {
    gap: spacing.md
  },
  summaryCard: {
    minHeight: 82,
    borderWidth: 1,
    borderColor: colors.border,
    borderRadius: 6,
    backgroundColor: colors.paperLight,
    padding: spacing.md,
    justifyContent: 'center'
  },
  summaryValue: {
    color: colors.burgundy,
    fontFamily: typography.display,
    fontSize: 28,
    fontWeight: '800'
  },
  summaryLabel: {
    color: colors.ink,
    fontFamily: typography.interface,
    fontWeight: '800'
  },
  cardTitle: {
    color: colors.ink,
    fontFamily: typography.display,
    fontSize: 20,
    fontWeight: '800'
  },
  bodyText: {
    color: colors.ink,
    fontFamily: typography.interface,
    fontWeight: '700'
  },
  errorText: {
    color: colors.burgundy,
    fontFamily: typography.interface,
    fontWeight: '800'
  },
  toggleRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between'
  },
  inputRow: {
    gap: spacing.md
  },
  businessButton: {
    borderWidth: 1,
    borderColor: colors.border,
    borderRadius: 6,
    backgroundColor: colors.paperLight,
    paddingHorizontal: spacing.md,
    paddingVertical: spacing.md,
    marginBottom: spacing.sm
  },
  businessButtonTitle: {
    color: colors.ink,
    fontFamily: typography.interface,
    fontSize: 16,
    fontWeight: '900'
  },
  productRow: {
    borderTopWidth: 1,
    borderTopColor: colors.border,
    paddingTop: spacing.md,
    gap: spacing.sm
  },
  orderShell: {
    flexDirection: 'row',
    alignItems: 'flex-start',
    gap: spacing.md
  },
  inventoryShell: {
    flexDirection: 'row',
    alignItems: 'flex-start',
    gap: spacing.md
  },
  inventoryVendorRail: {
    width: 108,
    maxWidth: 132,
    maxHeight: 620
  },
  inventoryVendorRailContent: {
    gap: spacing.sm
  },
  vendorRail: {
    width: 104,
    gap: spacing.sm
  },
  vendorRailItem: {
    borderWidth: 1,
    borderColor: colors.border,
    borderRadius: 6,
    backgroundColor: colors.paperLight,
    padding: spacing.sm,
    minHeight: 68,
    justifyContent: 'center'
  },
  vendorRailItemActive: {
    backgroundColor: colors.forestGreen,
    borderColor: colors.forestGreen
  },
  vendorRailText: {
    color: colors.ink,
    fontFamily: typography.interface,
    fontWeight: '800',
    fontSize: 13
  },
  vendorRailTextActive: {
    color: colors.white
  },
  vendorRailMeta: {
    color: colors.mutedInk,
    fontFamily: typography.interface,
    fontSize: 12,
    marginTop: spacing.xs
  },
  orderProducts: {
    flex: 1,
    minWidth: 0,
    gap: spacing.md
  },
  inventoryProducts: {
    flex: 1,
    minWidth: 0,
    gap: spacing.md
  },
  inventoryProductScroller: {
    maxHeight: 500
  },
  inventoryProductList: {
    gap: spacing.sm,
    paddingBottom: 96
  },
  inventoryActionBar: {
    borderWidth: 1,
    borderColor: colors.border,
    borderRadius: 6,
    backgroundColor: colors.paperLight,
    gap: spacing.sm,
    padding: spacing.sm,
    marginBottom: spacing.lg
  },
  inventorySubmitButton: {
    alignItems: 'center',
    backgroundColor: colors.forestGreen,
    borderRadius: 6,
    justifyContent: 'center',
    minHeight: 48,
    paddingHorizontal: spacing.md,
    paddingVertical: spacing.sm
  },
  inventorySubmitButtonDisabled: {
    opacity: 0.55
  },
  inventorySubmitButtonText: {
    color: colors.white,
    fontFamily: typography.interface,
    fontWeight: '900'
  },
  inventoryProductCard: {
    borderWidth: 1,
    borderColor: colors.border,
    borderRadius: 6,
    backgroundColor: colors.paperLight,
    paddingHorizontal: spacing.sm,
    paddingVertical: spacing.xs
  },
  inventoryProductRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.sm,
    justifyContent: 'space-between'
  },
  inventoryManageHeader: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: spacing.sm
  },
  inventoryProductActions: {
    flexDirection: 'row',
    gap: spacing.xs
  },
  inventoryIconButton: {
    alignItems: 'center',
    backgroundColor: colors.forestGreen,
    borderRadius: 6,
    justifyContent: 'center',
    minHeight: 40,
    width: 40
  },
  inventoryIconDanger: {
    backgroundColor: colors.burgundy
  },
  inventoryIconText: {
    color: colors.white,
    fontFamily: typography.interface,
    fontSize: 18,
    fontWeight: '900'
  },
  inventoryProductName: {
    color: colors.ink,
    flex: 1,
    fontFamily: typography.interface,
    fontSize: 15,
    fontWeight: '800'
  },
  inventoryVendorText: {
    color: colors.mutedInk,
    fontFamily: typography.interface,
    fontSize: 12,
    fontWeight: '800',
    width: 92
  },
  inventoryUnitText: {
    color: colors.mutedInk,
    fontFamily: typography.interface,
    fontSize: 12,
    fontWeight: '800',
    textAlign: 'right',
    width: 48
  },
  inventoryQuantityInput: {
    borderWidth: 1,
    borderColor: colors.border,
    borderRadius: 6,
    color: colors.ink,
    fontFamily: typography.interface,
    fontSize: 16,
    fontWeight: '800',
    minHeight: 42,
    paddingHorizontal: spacing.sm,
    paddingVertical: spacing.xs,
    textAlign: 'center',
    width: 82
  },
  historyControls: {
    gap: spacing.md,
    marginBottom: spacing.md,
    marginTop: spacing.sm
  },
  historyFilterGroup: {
    gap: spacing.sm
  },
  filterChips: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: spacing.sm
  },
  historyLine: {
    borderTopWidth: 1,
    borderTopColor: colors.border,
    gap: spacing.sm,
    paddingTop: spacing.md,
    marginTop: spacing.md
  },
  stepperRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.sm
  },
  stepperButton: {
    width: 42,
    minHeight: 42,
    borderRadius: 6,
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: colors.burgundy
  },
  stepperText: {
    color: colors.white,
    fontFamily: typography.interface,
    fontSize: 22,
    fontWeight: '900'
  }
});
