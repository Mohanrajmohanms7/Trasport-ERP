import { featureGuard } from './guards/feature.guard';
import { Routes } from '@angular/router';
// Screens load on first visit (lazy) so login downloads only the shell, not all 50 screens.
import { AppShellComponent } from './layout/app-shell/app-shell';
import { LoginComponent } from './auth/login/login';
import { authGuard } from './guards/auth.guard';
import { setupGuard } from './guards/setup.guard';
import { subscriptionGuard } from './guards/subscription.guard';

export const routes: Routes = [
  { path: 'login', component: LoginComponent },
  { path: 'forgot-password', loadComponent: () => import('./auth/forgot-password/forgot-password').then(m => m.ForgotPasswordComponent) },
  { path: 'reset-password', loadComponent: () => import('./auth/reset-password/reset-password').then(m => m.ResetPasswordComponent) },
  { path: 'access-denied', loadComponent: () => import('./auth/access-denied/access-denied').then(m => m.AccessDeniedComponent) },
  { path: 'setup', loadComponent: () => import('./components/setup-wizard/setup-wizard').then(m => m.SetupWizardComponent), canActivate: [authGuard] },
  { path: 'renewal', loadComponent: () => import('./components/renewal/renewal').then(m => m.RenewalComponent), canActivate: [authGuard, subscriptionGuard] },
  {
    path: '',
    component: AppShellComponent,
    canActivate: [authGuard, subscriptionGuard],
    canActivateChild: [featureGuard],
    children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
      { path: 'dashboard', loadComponent: () => import('./components/dashboard/dashboard').then(m => m.DashboardComponent), canActivate: [setupGuard] },
      { path: 'platform-admin', loadComponent: () => import('./components/platform-admin/platform-admin').then(m => m.PlatformAdminComponent) },
      { path: 'platform-admin/:section', loadComponent: () => import('./components/platform-admin/platform-admin').then(m => m.PlatformAdminComponent) },
      { path: 'masters', loadComponent: () => import('./components/branch-master/branch-master').then(m => m.BranchMasterComponent) },
      { path: 'branches', redirectTo: 'masters', pathMatch: 'full' },
      { path: 'lookup-values', loadComponent: () => import('./components/master-management/master-management').then(m => m.MasterManagementComponent), data: { mode: 'lookup' } },
      { path: 'profile', loadComponent: () => import('./auth/profile/profile').then(m => m.UserProfileComponent) },
      { path: 'users-roles', loadComponent: () => import('./components/user-role-management/user-role-management').then(m => m.UserRoleManagementComponent) },
      { path: 'company-admin', loadComponent: () => import('./components/company-administration/company-administration').then(m => m.CompanyAdministrationComponent) },
      { path: 'vehicles', loadComponent: () => import('./components/vehicle-details-console/vehicle-details-console').then(m => m.VehicleDetailsConsoleComponent) },
      { path: 'work-orders', loadComponent: () => import('./components/work-orders/work-order-list').then(m => m.WorkOrderListComponent) },
      { path: 'work-orders/new', loadComponent: () => import('./components/work-orders/work-order-form').then(m => m.WorkOrderFormComponent) },
      { path: 'work-orders/:id', loadComponent: () => import('./components/work-orders/work-order-detail').then(m => m.WorkOrderDetailComponent) },
      { path: 'spare-parts', loadComponent: () => import('./components/spare-parts/spare-part-catalog').then(m => m.SparePartCatalogComponent) },
      { path: 'inventory/warehouses', loadComponent: () => import('./components/inventory/warehouse-list').then(m => m.WarehouseListComponent) },
      { path: 'inventory/warehouses/new', loadComponent: () => import('./components/inventory/warehouse-form').then(m => m.WarehouseFormComponent) },
      { path: 'inventory/warehouses/:id', loadComponent: () => import('./components/inventory/warehouse-form').then(m => m.WarehouseFormComponent) },
      { path: 'inventory/stock', loadComponent: () => import('./components/inventory/stock-list').then(m => m.StockListComponent) },
      { path: 'inventory/stock/opening-balance', loadComponent: () => import('./components/inventory/opening-balance-form').then(m => m.OpeningBalanceFormComponent) },
      { path: 'inventory/stock/receipt', loadComponent: () => import('./components/inventory/stock-receipt-form').then(m => m.StockReceiptFormComponent) },
      { path: 'inventory/transactions', loadComponent: () => import('./components/inventory/inventory-transaction-list').then(m => m.InventoryTransactionListComponent) },
      { path: 'maintenance-requests', loadComponent: () => import('./components/maintenance-requests/maintenance-request-list').then(m => m.MaintenanceRequestListComponent) },
      { path: 'maintenance-requests/new', loadComponent: () => import('./components/maintenance-requests/maintenance-request-form').then(m => m.MaintenanceRequestFormComponent) },
      { path: 'maintenance-requests/:id/edit', loadComponent: () => import('./components/maintenance-requests/maintenance-request-form').then(m => m.MaintenanceRequestFormComponent) },
      { path: 'maintenance-requests/:id', loadComponent: () => import('./components/maintenance-requests/maintenance-request-detail').then(m => m.MaintenanceRequestDetailComponent) },
      { path: 'drivers', loadComponent: () => import('./components/driver-details-console/driver-details-console').then(m => m.DriverDetailsConsoleComponent) }, 
      { path: 'driver-payroll', loadComponent: () => import('./components/driver-payroll-console/driver-payroll-console').then(m => m.DriverPayrollConsoleComponent) },
      { path: 'customers', loadComponent: () => import('./components/customer-details-console/customer-details-console').then(m => m.CustomerDetailsConsoleComponent) },
      { path: 'materials-quarries', loadComponent: () => import('./components/material-quarry-console/material-quarry-console').then(m => m.MaterialQuarryConsoleComponent) },
      { path: 'bookings', loadComponent: () => import('./components/booking-details-console/booking-details-console').then(m => m.BookingDetailsConsoleComponent) },
      { path: 'trips-planning', loadComponent: () => import('./components/trip-details-console/trip-details-console').then(m => m.TripDetailsConsoleComponent) },
      { path: 'fuel-logs', loadComponent: () => import('./components/fuel-details-console/fuel-details-console').then(m => m.FuelDetailsConsoleComponent) },
      { path: 'expense-logs', loadComponent: () => import('./components/expense-details-console/expense-details-console').then(m => m.ExpenseDetailsConsoleComponent) },
      { path: 'family-expenses', loadComponent: () => import('./components/family-expenses/family-expenses').then(m => m.FamilyExpensesComponent) },
      { path: 'support', loadComponent: () => import('./components/support/support-console').then(m => m.SupportConsoleComponent) },
      { path: 'payment-logs', loadComponent: () => import('./components/payment-details-console/payment-details-console').then(m => m.PaymentDetailsConsoleComponent) },
      { path: 'billing-invoices', loadComponent: () => import('./components/invoice-details-console/invoice-details-console').then(m => m.InvoiceDetailsConsoleComponent) },
      { path: 'accounts-ledger', loadComponent: () => import('./components/accounts-details-console/accounts-details-console').then(m => m.AccountsDetailsConsoleComponent) },
      { path: 'reports', loadComponent: () => import('./components/reports-hub/reports-hub').then(m => m.ReportsHubComponent) },
      { path: 'payables', loadComponent: () => import('./components/payables-console/payables-console').then(m => m.PayablesConsoleComponent) },
      { path: 'suppliers', loadComponent: () => import('./components/supplier-master/supplier-master').then(m => m.SupplierMasterComponent) },
      { path: 'reports-bi', loadComponent: () => import('./components/report-details-console/report-details-console').then(m => m.ReportDetailsConsoleComponent) },
      { path: 'mobility-ai', loadComponent: () => import('./components/mobility-details-console/mobility-details-console').then(m => m.MobilityDetailsConsoleComponent) },
      { path: 'ui-playground', loadComponent: () => import('./shared-ui/playground/pages/ff-playground.component').then(m => m.FfPlaygroundComponent) }
    ]
  },
  { path: '**', redirectTo: 'login' }
];
