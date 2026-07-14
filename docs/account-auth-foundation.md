# Account and Authorization Foundation

This repository starts with two apps:

- `backend`: Spring Boot, Spring Security, Spring Data JPA, Flyway, MySQL, JWT access tokens, rotating refresh tokens, Bean Validation, and OpenAPI.
- `mobile`: Expo, React Native, TypeScript, Expo Router, TanStack Query, React Hook Form, Zod, and Expo SecureStore.

## Local backend setup

Create a MySQL database and set environment variables before starting Spring Boot:

```sh
export DB_URL=jdbc:mysql://localhost:3306/restaurant_ops
export DB_USERNAME=restaurant_ops
export DB_PASSWORD=restaurant_ops
export JWT_SECRET='replace-with-at-least-32-random-bytes'
export TOAST_PIN_ENCRYPTION_KEY='replace-with-a-different-random-secret'
export APP_PUBLIC_BASE_URL=http://localhost:8081
export CORS_ALLOWED_ORIGINS=http://localhost:8081,http://localhost:19006
export PROFILE_PHOTO_STORAGE_DIR=./work/profile-photos
export PROFILE_PHOTO_MAX_BYTES=2097152
export RATE_LIMIT_PER_MINUTE=10
```

To create the first manager in development, set bootstrap variables once:

```sh
export BOOTSTRAP_MANAGER_ENABLED=true
export BOOTSTRAP_MANAGER_EMAIL=manager@example.com
export BOOTSTRAP_MANAGER_PHONE=2065550100
export BOOTSTRAP_MANAGER_PASSWORD='change-me-locally'
export BOOTSTRAP_MANAGER_ENGLISH_NAME='Manager'
export BOOTSTRAP_MANAGER_PREFERRED_NAME='Manager'
export BOOTSTRAP_MANAGER_TOAST_PIN='0001'
```

The initializer skips creation when a manager with the bootstrap email already exists. Do not use bootstrap as a public registration path.

## Implemented foundations

- Invitation creation, validation, regeneration, revocation, one-time activation, hash-only tokens, and 24-hour expiry.
- Employee activation with required profile fields, positions from the invitation, normalized email and phone, unique Toast PIN hash, and transactional invitation use.
- Login by email or phone, JWT access tokens, server-side refresh tokens, rotation, logout current device, logout all devices, and deactivation revocation.
- Password reset and contact-change token workflows with provider interfaces and development adapters.
- Employee profile, store preferences, Toast PIN update audit logging, manager employee details, manager position updates, employee deactivation, and force logout.
- Backend authorization helpers for manager checks, form-template permissions, submitted-form modification, operational store access, shift coverage, private employee data, and employee management.
- Expo mobile screens for login, invitation activation, activation result, profile, language settings, security actions, invitation management, and employee management.

## Security notes

Secrets are read from environment variables. Raw invitation, reset, and refresh tokens are never stored. Toast PINs are protected with a hash for uniqueness and are only returned on private profile or manager detail responses.

`JWT_SECRET` signs access tokens. `TOAST_PIN_ENCRYPTION_KEY` protects Toast PIN values at rest and must be separate. Production startup fails if either key is absent in the `prod` profile. Do not reuse these examples outside local development.

The profile-photo API is backed by `ProfilePhotoStorage`; the local implementation writes to `PROFILE_PHOTO_STORAGE_DIR`, validates PNG/JPEG/WebP content signatures, and enforces `PROFILE_PHOTO_MAX_BYTES`.

`CORS_ALLOWED_ORIGINS` is a comma-separated list of trusted frontend origins. `RATE_LIMIT_PER_MINUTE` controls the in-memory MVP limiter for login, activation, password reset, and contact verification endpoints. The limiter is intentionally behind a small interface so it can later be replaced with Redis or another shared store.

## Local verification testing

Run the backend with the `dev` profile to expose the development-only verification outbox:

```sh
SPRING_PROFILES_ACTIVE=dev mvn spring-boot:run
```

After requesting an email or phone change, read the local verification token:

```sh
curl 'http://localhost:8080/api/dev/verification/latest?purpose=contact&channel=email&destination=new@example.com'
curl 'http://localhost:8080/api/dev/verification/latest?purpose=contact&channel=phone&destination=2065550100'
```

The `/api/dev/verification/**` endpoints are not registered outside the `dev` profile and should never be enabled in production.

## Database integration tests

Preferred:

```sh
cd backend
mvn test -Dtest=AccountFlowMySqlIntegrationTest
```

This uses Testcontainers and requires Docker.

Alternative local MySQL profile:

```sh
export LOCAL_MYSQL_TESTS=true
export LOCAL_MYSQL_TEST_DB_URL=jdbc:mysql://localhost:3306/restaurant_ops_test
export LOCAL_MYSQL_TEST_DB_USERNAME=restaurant_ops_test
export LOCAL_MYSQL_TEST_DB_PASSWORD='test-password'
cd backend
mvn test -Dtest=AccountFlowLocalMySqlIntegrationTest
```

The URL must contain `_test`; Flyway runs against that dedicated test database.

## Commands

```sh
cd backend
mvn test

cd ../mobile
npm install
npm test
npm start
```
