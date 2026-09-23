import { Routes } from '@angular/router';
import { AppShellComponent } from './layout/app-shell/app-shell';
import { LoginComponent } from './auth/login/login';
import { ForgotPasswordComponent } from './auth/forgot-password/forgot-password';
import { ResetPasswordComponent } from './auth/reset-password/reset-password';
import { UserProfileComponent } from './auth/profile/profile';
import { AccessDeniedComponent } from './auth/access-denied/access-denied';
import { MasterManagementComponent } from './components/master-management/master-management';
import { DashboardComponent } from './components/dashboard/dashboard';
import { UserRoleManagementComponent } from './components/user-role-management/user-role-management';
import { CompanyAdministrationComponent } from './components/company-administration/company-administration';
import { VehicleDetailsConsoleComponent } from './components/vehicle-details-console/vehicle-details-console';
import { WorkOrderListComponent } from './components/work-orders/work-order-list';
import { WorkOrderFormComponent } from './components/work-orders/work-order-form';
import { WorkOrderDetailComponent } from './components/work-orders/work-order-detail';
import { SparePartCatalogComponent } from './components/spare-parts/spare-part-catalog';
import { WarehouseListComponent } from './components/inventory/warehouse-list';
import { WarehouseFormComponent } from './components/inventory/warehouse-form';
import { StockListComponent } from './components/inventory/stock-list';
import { OpeningBalanceFormComponent } from './components/inventory/opening-balance-form';
import { InventoryTransactionListComponent } from './components/inventory/inventory-transaction-list';
import { MaintenanceRequestListComponent } from './components/maintenance-requests/maintenance-request-list';
import { MaintenanceRequestFormComponent } from './components/maintenance-requests/maintenance-request-form';
import { MaintenanceRequestDetailComponent } from './components/maintenance-requests/maintenance-request-detail';
import { DriverDetailsConsoleComponent } from './components/driver-details-console/driver-details-console';
import { CustomerDetailsConsoleComponent } from './components/customer-details-console/customer-details-console';
import { MaterialQuarryConsoleComponent } from './components/material-quarry-console/material-quarry-console';
import { BookingDetailsConsoleComponent } from './components/booking-details-console/booking-details-console';
import { TripDetailsConsoleComponent } from './components/trip-details-console/trip-details-console';
import { FuelDetailsConsoleComponent } from './components/fuel-details-console/fuel-details-console';
import { ExpenseDetailsConsoleComponent } from './components/expense-details-console/expense-details-console';
import { PaymentDetailsConsoleComponent } from './components/payment-details-console/payment-details-console';
import { InvoiceDetailsConsoleComponent } from './components/invoice-details-console/invoice-details-console';
import { AccountsDetailsConsoleComponent } from './components/accounts-details-console/accounts-details-console';
import { ReportDetailsConsoleComponent } from './components/report-details-console/report-details-console';
import { MobilityDetailsConsoleComponent } from './components/mobility-details-console/mobility-details-console';
import { SetupWizardComponent } from './components/setup-wizard/setup-wizard';
import { PlatformAdminComponent } from './components/platform-admin/platform-admin';
import { RenewalComponent } from './components/renewal/renewal';
import { FfPlaygroundComponent } from './shared-ui/playground/pages/ff-playground.component';
import { authGuard } from './guards/auth.guard';
import { setupGuard } from './guards/setup.guard';
import { subscriptionGuard } from './guards/subscription.guard';

export const routes: Routes = [
  { path: 'login', component: LoginComponent },
  { path: 'forgot-password', component: ForgotPasswordComponent },
  { path: 'reset-password', component: ResetPasswordComponent },
  { path: 'access-denied', component: AccessDeniedComponent },
  { path: 'setup', component: SetupWizardComponent, canActivate: [authGuard] },
  { path: 'renewal', component: RenewalComponent, canActivate: [authGuard, subscriptionGuard] },
  {
    path: '',
    component: AppShellComponent,
    canActivate: [authGuard, subscriptionGuard],

    children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
      { path: 'dashboard', component: DashboardComponent, canActivate: [setupGuard] },
      { path: 'platform-admin', component: PlatformAdminComponent },
      { path: 'masters', component: MasterManagementComponent },
      { path: 'profile', component: UserProfileComponent },
      { path: 'users-roles', component: UserRoleManagementComponent },
      { path: 'company-admin', component: CompanyAdministrationComponent },
      { path: 'vehicles', component: VehicleDetailsConsoleComponent },
      { path: 'work-orders', component: WorkOrderListComponent },
      { path: 'work-orders/new', component: WorkOrderFormComponent },
      { path: 'work-orders/:id', component: WorkOrderDetailComponent },
      { path: 'spare-parts', component: SparePartCatalogComponent },
      { path: 'inventory/warehouses', component: WarehouseListComponent },
      { path: 'inventory/warehouses/new', component: WarehouseFormComponent },
      { path: 'inventory/warehouses/:id', component: WarehouseFormComponent },
      { path: 'inventory/stock', component: StockListComponent },
      { path: 'inventory/stock/opening-balance', component: OpeningBalanceFormComponent },
      { path: 'inventory/transactions', component: InventoryTransactionListComponent },
      { path: 'maintenance-requests', component: MaintenanceRequestListComponent },
      { path: 'maintenance-requests/new', component: MaintenanceRequestFormComponent },
      { path: 'maintenance-requests/:id/edit', component: MaintenanceRequestFormComponent },
      { path: 'maintenance-requests/:id', component: MaintenanceRequestDetailComponent },
      { path: 'drivers', component: DriverDetailsConsoleComponent }, 
      { path: 'customers', component: CustomerDetailsConsoleComponent },
      { path: 'materials-quarries', component: MaterialQuarryConsoleComponent },
      { path: 'bookings', component: BookingDetailsConsoleComponent },
      { path: 'trips-planning', component: TripDetailsConsoleComponent },
      { path: 'fuel-logs', component: FuelDetailsConsoleComponent },
      { path: 'expense-logs', component: ExpenseDetailsConsoleComponent },
      { path: 'payment-logs', component: PaymentDetailsConsoleComponent },
      { path: 'billing-invoices', component: InvoiceDetailsConsoleComponent },
      { path: 'accounts-ledger', component: AccountsDetailsConsoleComponent },
      { path: 'reports-bi', component: ReportDetailsConsoleComponent },
      { path: 'mobility-ai', component: MobilityDetailsConsoleComponent },
      { path: 'ui-playground', component: FfPlaygroundComponent }
    ]
  },
  { path: '**', redirectTo: 'login' }
];
