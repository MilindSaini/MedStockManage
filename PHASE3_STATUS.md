# MedStock Phase 3 Build Status

Phase 3 (Inventory Core) has been implemented in backend and frontend while keeping existing Phase 1/2 behavior intact.

## Backend completed
- Added entities:
  - `Medicine`
  - `BatchGroup`
  - `StockTransaction`
  - `EmployeePermission`
- Added repositories:
  - `MedicineRepository`
  - `BatchGroupRepository`
  - `StockTransactionRepository`
  - `EmployeePermissionRepository`
- Added migration `V10__add_medicine_current_stock_and_active_batch.sql`:
  - `medicines.active_batch_id`
  - `medicines.current_stock` generated column
  - indexes for both columns
- Added DTOs for medicines, stock adjustments, and employee management.
- Added services:
  - `MedicineService` with store-scoped CRUD, soft delete, pagination, search, filters, sorting
  - `StockService` with append-only transaction writes and stock validation
  - `BatchService` with automatic batch promotion on stock zero
  - `EmployeeService` for add/remove employees and permission updates
  - `PermissionGuard` with `assertCanAdd/assertCanEdit/assertCanDelete/assertCanViewFinance/assertCanSell`
- Added controllers/endpoints:
  - `MedicineController` under `/api/medicines`
  - `StockController` under `/api/stock`
  - `EmployeeController` under `/api/employees`
- Added endpoint for frontend permission UI guard:
  - `GET /api/employees/my-permissions`

## Frontend completed
- Added hooks:
  - `useMedicines` (React Query + optimistic stock updates)
  - `usePermissions`
- Added component:
  - `StockAdjustButton`
- Added pages:
  - `InventoryPage`
  - `AddMedicinePage`
  - `AlertsPage`
  - `OutOfStockPage`
  - `EmployeesPage`
- Added protected routes in `App.jsx`:
  - `/inventory`
  - `/inventory/add`
  - `/alerts`
  - `/out-of-stock`
  - `/employees`
- Updated owner and employee dashboards with links to Phase 3 pages.

## Validation
- Backend compile: `mvn -DskipTests compile` passed.
- Frontend build: `npm run build` passed.

## Notes
- Store scoping is enforced from authenticated user context (`UserPrincipal.storeId`) in services.
- Employee actions are guarded via role + permission checks.
- Existing Phase 1/2 auth flow and role switching remain in place.
