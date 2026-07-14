package com.restaurant.ops.employee;

import java.util.EnumSet;
import java.util.Set;

public enum Position {
  OWNER,
  FINANCIAL_MANAGER,
  MANAGER,
  FOOD_RUNNER,
  HOST,
  BARTENDER,
  SERVER_ONE_STAR,
  SERVER_TWO_STAR,
  SHIFT_LEADER;

  private static final Set<Position> BUSINESS_PARTNER_POSITIONS = EnumSet.of(OWNER, MANAGER, FINANCIAL_MANAGER);
  private static final Set<Position> RESTAURANT_OPERATING_POSITIONS = EnumSet.of(FOOD_RUNNER, HOST, BARTENDER, SERVER_ONE_STAR, SERVER_TWO_STAR, SHIFT_LEADER);

  public enum PermissionGroup {
    BUSINESS_PARTNER,
    RESTAURANT_POSITION
  }

  public static Set<Position> normalize(Set<Position> positions) {
    EnumSet<Position> normalized = positions.isEmpty() ? EnumSet.noneOf(Position.class) : EnumSet.copyOf(positions);
    if (normalized.contains(SERVER_TWO_STAR)) {
      normalized.remove(SERVER_ONE_STAR);
    }
    return normalized;
  }

  public static Set<Position> normalizeAfterAssigning(Set<Position> positions, Position assigned) {
    EnumSet<Position> normalized = positions.isEmpty() ? EnumSet.noneOf(Position.class) : EnumSet.copyOf(positions);
    normalized.add(assigned);
    if (assigned == SERVER_ONE_STAR) {
      normalized.remove(SERVER_TWO_STAR);
    }
    if (assigned == SERVER_TWO_STAR) {
      normalized.remove(SERVER_ONE_STAR);
    }
    return normalized;
  }

  public static Set<Position> allWorkingPositionsFor(Position position) {
    if (isBusinessPartner(position)) {
      return EnumSet.copyOf(RESTAURANT_OPERATING_POSITIONS);
    }
    return EnumSet.of(position);
  }

  public static boolean isBusinessPartner(Position position) {
    return BUSINESS_PARTNER_POSITIONS.contains(position);
  }

  public PermissionGroup permissionGroup() {
    return isBusinessPartner(this) ? PermissionGroup.BUSINESS_PARTNER : PermissionGroup.RESTAURANT_POSITION;
  }

  public static Set<Position> restaurantOperatingPositions() {
    return EnumSet.copyOf(RESTAURANT_OPERATING_POSITIONS);
  }

  public static Set<Position> businessPartnerPositions() {
    return EnumSet.copyOf(BUSINESS_PARTNER_POSITIONS);
  }
}
