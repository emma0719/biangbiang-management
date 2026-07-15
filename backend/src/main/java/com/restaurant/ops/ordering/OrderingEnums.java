package com.restaurant.ops.ordering;

public final class OrderingEnums {
  private OrderingEnums() {}

  public enum CatalogCategory {
    PACKAGING,
    DISPOSABLES,
    BEER,
    SAKE,
    SOJU,
    SPIRITS,
    COCKTAIL_INGREDIENTS,
    NON_ALCOHOLIC_BEVERAGES,
    TEA_AND_MILK_TEA,
    PRODUCE,
    CLEANING,
    RESTROOM_SUPPLIES,
    OFFICE_AND_POS,
    FROZEN_FOOD,
    DRY_GOODS,
    REFRIGERATED_FOOD,
    OTHER
  }

  public enum CatalogUnit {
    EA,
    BOTTLE,
    CAN,
    BAG,
    BOX,
    CASE,
    PACK,
    CARTON,
    GALLON,
    LITER,
    COUNT,
    ROLL,
    CONTAINER,
    OTHER
  }

  public enum InventoryBusiness {
    BIANGBIANG_FRONT("BiangBiang Front"),
    PAPER_FAN("Paper Fan");

    private final String displayName;

    InventoryBusiness(String displayName) {
      this.displayName = displayName;
    }

    public String displayName() {
      return displayName;
    }
  }

  public enum OrderBusiness {
    BIANGBIANG_FRONT("BiangBiang Front"),
    PAPER_FAN("Paper Fan");

    private final String displayName;

    OrderBusiness(String displayName) {
      this.displayName = displayName;
    }

    public String displayName() {
      return displayName;
    }
  }

  public enum InventoryCountStatus {
    DRAFT,
    IN_PROGRESS,
    SUBMITTED,
    REVIEWED,
    LOCKED,
    COMPLETED,
    CANCELLED
  }

  public enum OrderPlanStatus {
    DRAFT,
    IN_PROGRESS,
    SUBMITTED,
    APPROVED,
    RETURNED,
    COMPLETED,
    REJECTED,
    CANCELLED
  }

  public enum PurchaseOrderStatus {
    DRAFT,
    SUBMITTED,
    APPROVED,
    REJECTED,
    ORDERED,
    PARTIALLY_RECEIVED,
    RECEIVED,
    CANCELLED
  }
}
