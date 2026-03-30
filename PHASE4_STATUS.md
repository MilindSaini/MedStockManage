# MedStock Phase 4 Build Status

Phase 4 (Alerts + Notifications) has been implemented with minimal-impact integration so existing Phase 1-3 behavior remains intact.

## Backend completed
- Added alert status model and computation utility:
  - `AlertType` enum
  - `MedicineStatusUtil.computeStatus(expiryDate, currentStock, threshold)`
- Added alert log persistence for dedup:
  - `AlertLog` entity mapped to existing `alert_logs` table
  - `AlertLogRepository.existsByMedicineIdAndAlertTypeAndCreatedAtAfter(...)`
- Added `AlertService`:
  - `getGroupedAlerts(storeId)` returns grouped alert buckets
  - Notification dedup within configurable lookback window
  - Real-time stock-trigger alert dispatch for `LOW_STOCK` and `OUT_OF_STOCK`
- Added `AlertController` endpoint:
  - `GET /api/alerts/grouped`
- Added `NotificationService`:
  - `sendSms(phone, msg)`
  - `sendEmail(to, subject, html)`
  - `sendWhatsApp(phone, msg)`
  - Thymeleaf template rendering helper used for alert emails
- Added scheduler support:
  - `SchedulerConfig` with `@EnableScheduling` and pool size 4
  - `SchedulerService` jobs:
    - `checkBatchPromotion` at 6:00 AM
    - `checkExpiryAlerts` at 8:00 AM
    - `checkLowStock` at 8:30 AM
    - `checkOutOfStock` at 9:00 AM
- Added real-time stock alert trigger in `StockService.adjustStock()` after successful transaction save.

## Templates added
- `expiry-alert.html`
- `low-stock-alert.html`
- `welcome.html`
- `password-reset.html`

## Frontend completed
- Updated `AlertsPage.jsx` to consume backend grouped alerts from `GET /api/alerts/grouped` via React Query.
- Existing UI/UX and navigation behavior for alerts cards is preserved.

## Config updates
- Added Thymeleaf starter dependency in backend `pom.xml`.
- Added optional Twilio and dedup config keys in backend `application.yml`.
- Added sample env keys in root `.env.example`:
  - `MEDSTOCK_TWILIO_ACCOUNT_SID`
  - `MEDSTOCK_TWILIO_AUTH_TOKEN`
  - `MEDSTOCK_TWILIO_FROM_PHONE`
  - `MEDSTOCK_TWILIO_FROM_WHATSAPP`
  - `MEDSTOCK_ALERT_DEDUP_HOURS`

## Validation
- Backend compile: `mvn -DskipTests compile` passed.
- Frontend build: `npm run build` passed.

## Notes
- SMS/WhatsApp are no-op unless Twilio env vars are set.
- Email sends depend on existing Spring Mail configuration.
- Alert dedup is persisted in `alert_logs` and defaults to 24 hours.
