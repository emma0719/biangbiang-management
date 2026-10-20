# Biangbiang Management (Restaurant Ops)

First production feature scaffold for a two-location restaurant operations app.

See [docs/account-auth-foundation.md](docs/account-auth-foundation.md) for setup, bootstrap, API, and security notes.

## Tech stack

- **Mobile:** Expo, React Native, TypeScript, Expo Router, TanStack Query, React Hook Form, Zod, and Expo SecureStore.
- **Backend:** Java 21, Spring Boot, Spring Security, Spring Data JPA, Bean Validation, and JWT authentication.
- **Database and API:** MySQL, Flyway migrations, and OpenAPI.

## Feature screenshots

Restaurant operations for Seattle and Redmond: profile settings, shift coverage, inventory counts, order planning, and PDF reports.

### 1. Profile and language settings

Account settings and English / Simplified Chinese language controls.

<p>
  <img src="docs/screenshots/profile-language-settings.png" alt="Profile language control with mixed English and Simplified Chinese interface labels" width="280">
</p>

### 2. Shift coverage

Shift coverage requests with store, date, position, public or direct coverage options, and a manager approval queue.

| Coverage request form | Manager queue: empty state |
| --- | --- |
| <img src="docs/screenshots/shift-coverage-request.png" alt="New shift coverage request form with store, date, shift, position, and coverage type fields" width="280"> | <img src="docs/screenshots/shift-pending-approval-empty.png" alt="Manager pending-approval queue with no requests waiting for approval" width="280"> |

### 3. Inventory counts

Inventory counts for BiangBiang Front and Paper Fan, with business selection and history.

<p>
  <img src="docs/screenshots/inventory-business-selection.png" alt="Inventory count business selection for BiangBiang Front and Paper Fan" width="280">
</p>

### 4. Order planning

Order planning with business selection, vendor groups, inventory references, quantity controls, draft saving, and submission.

| Select a business | Inventory reference and quantities |
| --- | --- |
| <img src="docs/screenshots/order-business-selection.png" alt="New order business selection for BiangBiang Front and Paper Fan" width="280"> | <img src="docs/screenshots/order-inventory-reference.png" alt="Vendor products showing latest inventory values and order quantity controls" width="280"> |

| Draft and submit controls | Submission feedback |
| --- | --- |
| <img src="docs/screenshots/order-draft-and-submit-controls.png" alt="Inventory reference import message with Save Draft and Submit Order controls" width="280"> | <img src="docs/screenshots/order-submission-failed.png" alt="Order Submission Failed dialog reporting ORDER_SESSION_HAS_NO_LINES" width="280"> |

<p>
  <img src="docs/screenshots/order-submission-success.png" alt="Order Submitted success confirmation with View History, Stay on Order, and Back to Profile actions" width="280">
</p>

### 5. PDF reports

<p>
  <img src="docs/screenshots/pdf-dark-background-rendering.png" alt="PDF report with vendor groups, items, quantities, and units" width="792">
</p>
