# Biangbiang Management (Restaurant Ops)

First production feature scaffold for a two-location restaurant operations app.

See [docs/account-auth-foundation.md](docs/account-auth-foundation.md) for setup, bootstrap, API, and security notes.

## Tech stack

- **Mobile:** Expo, React Native, TypeScript, Expo Router, TanStack Query, React Hook Form, Zod, and Expo SecureStore.
- **Backend:** Java 21, Spring Boot, Spring Security, Spring Data JPA, Bean Validation, and JWT authentication.
- **Database and API:** MySQL, Flyway migrations, and OpenAPI.

## Feature screenshots

These development screenshots illustrate four modules for restaurant operations across Seattle and Redmond. They show captured UI states, not end-to-end acceptance results, and may differ from the committed application. Screenshots containing employee names or individual shift records have been omitted.

### 1. Profile and language settings

The profile module provides account settings and English / Simplified Chinese language controls. This capture shows the language control, security actions, and partially translated navigation; it does not verify complete localization or successful account changes.

<p>
  <img src="docs/screenshots/profile-language-settings.png" alt="Profile language control with mixed English and Simplified Chinese interface labels" width="280">
</p>

### 2. Shift coverage

The request form includes store, date, shift type, times, position, and public or direct coverage options. The manager view below shows an empty pending-approval queue. These captures do not demonstrate a submitted request, a successful pickup, or an approval decision. The UI states that the original employee remains responsible until a manager approves coverage.

| Coverage request form | Manager queue: empty state |
| --- | --- |
| <img src="docs/screenshots/shift-coverage-request.png" alt="New shift coverage request form with store, date, shift, position, and coverage type fields" width="280"> | <img src="docs/screenshots/shift-pending-approval-empty.png" alt="Manager pending-approval queue with no requests waiting for approval" width="280"> |

### 3. Inventory counts

The inventory entry screen offers business selection for BiangBiang Front and Paper Fan, with a History tab. This screenshot shows the selection screen only; it does not verify count submission or PDF export.

<p>
  <img src="docs/screenshots/inventory-business-selection.png" alt="Inventory count business selection for BiangBiang Front and Paper Fan" width="280">
</p>

**Development issue observed:** a separate inventory-history capture displays `Unable to open PDF`. That capture contains an employee name and is excluded. PDF viewing remains unverified; these screenshots provide no evidence of a fix or successful PDF sharing.

### 4. Order planning

The order screens show business selection, products grouped by vendor, latest inventory values, quantity controls, and draft / submit actions. One capture displays `Latest inventory imported for reference`. Visible controls do not establish that draft saving, leaving and resuming an order, or submission succeeds.

| Select a business | Inventory reference and quantities |
| --- | --- |
| <img src="docs/screenshots/order-business-selection.png" alt="New order business selection for BiangBiang Front and Paper Fan" width="280"> | <img src="docs/screenshots/order-inventory-reference.png" alt="Vendor products showing latest inventory values and order quantity controls" width="280"> |

| Draft and submit controls | Development issue: submission failure |
| --- | --- |
| <img src="docs/screenshots/order-draft-and-submit-controls.png" alt="Inventory reference import message with Save Draft and Submit Order controls" width="280"> | <img src="docs/screenshots/order-submission-failed.png" alt="Order Submission Failed dialog reporting ORDER_SESSION_HAS_NO_LINES" width="280"> |

**Development issue observed:** the failure capture reports `ORDER_SESSION_HAS_NO_LINES` with zero items and zero total quantity. It documents a failed attempt, not a successful order submission. The cause and any subsequent fix have not been verified as part of this documentation update.
