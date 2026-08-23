import { Component, signal, computed, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, NavigationEnd, RouterModule } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatMenuModule } from '@angular/material/menu';
import { filter } from 'rxjs/operators';
import { AuthService } from '../services/auth.service';

interface MenuItem {
  label: string;
  route: string;
  icon: string;
  badge?: string;
}

@Component({
  selector: 'app-layout',
  standalone: true,
  imports: [CommonModule, RouterModule, MatIconModule, MatButtonModule, MatMenuModule],
  templateUrl: './layout.html',
  styles: []
})
export class LayoutComponent implements OnInit {
  private router = inject(Router);
  private auth = inject(AuthService);

  sidebarCollapsed = signal<boolean>(false);
  activeRoute = signal<string>('/');
  notificationCount = signal<number>(0);
  userProfile = signal<{ name: string; role: string; email: string }>({
    name: '',
    role: '',
    email: ''
  });

  menuItems = signal<MenuItem[]>([
    { label: 'Dashboard', route: '/dashboard', icon: 'dashboard' },
    { label: 'Master Data', route: '/masters', icon: 'corporate_fare' },
    { label: 'Vehicle Console', route: '/vehicles', icon: 'commute' },
    { label: 'Driver Console', route: '/drivers', icon: 'contact_page' },
    { label: 'Customer Console', route: '/customers', icon: 'supervised_user_circle' },
    { label: 'Material & Quarry', route: '/materials-quarries', icon: 'landscape' },
    { label: 'Booking Console', route: '/bookings', icon: 'receipt_long' },
    { label: 'Trip Planning & Dispatch', route: '/trips-planning', icon: 'local_shipping' },
    { label: 'Fuel Console', route: '/fuel-logs', icon: 'local_gas_station' },
    { label: 'Expense Console', route: '/expense-logs', icon: 'account_balance_wallet' },
    { label: 'Payment Console', route: '/payment-logs', icon: 'account_balance' },
    { label: 'Sales Billing & GST', route: '/billing-invoices', icon: 'receipt_long' },
    { label: 'Accounts & GL', route: '/accounts-ledger', icon: 'account_balance' },
    { label: 'Reports & BI', route: '/reports-bi', icon: 'assessment' },
    { label: 'Mobility & AI', route: '/mobility-ai', icon: 'edgesensor_high' },
    { label: 'Users & Roles', route: '/users-roles', icon: 'admin_panel_settings' }
  ]);

  breadcrumbs = computed(() => {
    const url = this.activeRoute();
    if (url === '/' || url === '') {
      return ['Dashboard'];
    }
    return ['Home', ...url.split('/').filter(x => x).map(segment => {
      return segment.charAt(0).toUpperCase() + segment.slice(1);
    })];
  });

  ngOnInit() {
    this.syncProfile();
    this.activeRoute.set(this.router.url);
    this.router.events.pipe(
      filter(event => event instanceof NavigationEnd)
    ).subscribe((event: any) => {
      this.activeRoute.set(event.urlAfterRedirects);
    });
  }

  private syncProfile() {
    const user = this.auth.currentUser();
    const roles = user?.roles || JSON.parse(localStorage.getItem('roles') || '[]');
    this.userProfile.set({
      name: user?.name || localStorage.getItem('name') || '',
      role: Array.isArray(roles) && roles.length ? String(roles[0]) : '',
      email: user?.email || localStorage.getItem('email') || ''
    });
  }

  toggleSidebar() {
    this.sidebarCollapsed.update(val => !val);
  }

  logout() {
    this.auth.logout().subscribe({
      next: () => this.router.navigate(['/login']),
      error: () => this.router.navigate(['/login'])
    });
  }
}
