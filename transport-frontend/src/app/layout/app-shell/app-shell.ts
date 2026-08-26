import { Component, signal, computed, inject, HostListener, ViewChild, ElementRef, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, NavigationEnd, RouterModule } from '@angular/router';
import { MatMenuModule } from '@angular/material/menu';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { ConfirmationDialogComponent } from '../../shared/confirmation-dialog/confirmation-dialog';
import { Subject, forkJoin, of } from 'rxjs';
import { debounceTime, distinctUntilChanged, filter, switchMap, catchError, takeUntil } from 'rxjs/operators';
import { FfThemeService } from '../../shared-ui/infrastructure/services/ff-theme.service';
import { AuthService } from '../../services/auth.service';
import { MasterService } from '../../services/master.service';
import { BookingMgmtService } from '../../services/booking-mgmt.service';
import { TripMgmtService } from '../../services/trip-mgmt.service';
import { resolveTenantCompanyId } from '../../shared/tenant-context';

interface MenuItem {
  label: string;
  route: string;
  icon: string;
  badge?: string;
}

interface MenuGroup {
  groupName: string;
  items: MenuItem[];
}

interface SearchHit {
  id: string;
  category: string;
  title: string;
  subtitle: string;
  icon: string;
  route: string;
}

import { FfToastComponent } from '@ff/ui';

@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [CommonModule, RouterModule, MatMenuModule, MatDialogModule, FfToastComponent],
  templateUrl: './app-shell.html',
  styles: [`
    :host { display: block; height: 100%; }
    /* ::ng-deep so height reaches routed component hosts (encapsulation otherwise blocks it) */
    :host ::ng-deep .app-outlet-host > *:not(router-outlet) {
      display: flex;
      flex-direction: column;
      flex: 1 1 auto;
      min-height: 0;
      height: 100%;
      overflow: hidden;
    }
  `]
})
export class AppShellComponent implements OnDestroy {
  private router = inject(Router);
  private theme = inject(FfThemeService);
  private auth = inject(AuthService);
  private masterService = inject(MasterService);
  private bookingService = inject(BookingMgmtService);
  private tripService = inject(TripMgmtService);
  private dialog = inject(MatDialog);

  @ViewChild('globalSearchInput') globalSearchInput?: ElementRef<HTMLInputElement>;
  @ViewChild('paletteInput') paletteInput?: ElementRef<HTMLInputElement>;

  sidebarCollapsed = signal<boolean>(false);
  activeRoute = signal<string>('/');
  readonly isDarkMode = this.theme.isDark;

  searchOpen = signal(false);
  searchQuery = signal('');
  searchLoading = signal(false);
  searchHits = signal<SearchHit[]>([]);
  private searchTerm$ = new Subject<string>();
  private destroy$ = new Subject<void>();

  /** Live profile from AuthService (login / localStorage / /auth/profile). */
  readonly userProfile = computed(() => {
    const user = this.auth.currentUser();
    const roles = user?.roles?.length ? user.roles : [];
    const desc = user?.description || '';
    let avatar: string | null = null;
    if (desc.includes('|||')) {
      avatar = desc.split('|||')[0];
    } else if (desc.startsWith('data:image/')) {
      avatar = desc;
    }
    return {
      name: user?.name || user?.username || 'User',
      role: roles.length ? roles.join(', ') : 'User',
      email: user?.email || user?.username || '',
      username: user?.username || '',
      avatar: avatar
    };
  });

  readonly isSuperAdmin = computed(() => {
    const user = this.auth.currentUser();
    const roles = user?.roles || [];
    return roles.includes('SUPER_ADMIN');
  });

  menuGroups = signal<MenuGroup[]>([
    {
      groupName: 'MASTERS',
      items: [
        { label: 'Vehicle Master', route: '/vehicles', icon: 'local_shipping' },
        { label: 'Driver Master', route: '/drivers', icon: 'person' },
        { label: 'Customer Master', route: '/customers', icon: 'group' },
        { label: 'Branch Master', route: '/masters', icon: 'store' },
        { label: 'Material & Quarry', route: '/materials-quarries', icon: 'category' }
      ]
    },
    {
      groupName: 'OPERATIONS',
      items: [
        { label: 'Booking', route: '/bookings', icon: 'receipt_long' },
        { label: 'Trip Management', route: '/trips-planning', icon: 'map' },
        { label: 'Fuel Management', route: '/fuel-logs', icon: 'local_gas_station' },
        { label: 'Expense Management', route: '/expense-logs', icon: 'account_balance_wallet' }
      ]
    },
    {
      groupName: 'FINANCE',
      items: [
        { label: 'Invoice', route: '/billing-invoices', icon: 'description' },
        { label: 'Payments', route: '/payment-logs', icon: 'payments' },
        { label: 'Accounts', route: '/accounts-ledger', icon: 'account_balance' },
        { label: 'Reports', route: '/reports-bi', icon: 'assessment' }
      ]
    },
    {
      groupName: 'ADMIN',
      items: [
        { label: 'User & Roles', route: '/users-roles', icon: 'manage_accounts' },
        { label: 'System Settings', route: '/company-admin', icon: 'settings' }
      ]
    }
  ]);

  readonly filteredMenuGroups = computed(() => {
    const groups = this.menuGroups();
    const currentUser = this.auth.currentUser();
    const roles = currentUser?.roles || [];
    
    if (roles.includes('SUPER_ADMIN')) {
      return [
        {
          groupName: 'SAAS PLATFORM',
          items: [
            { label: 'Platform Admin', route: '/platform-admin', icon: 'admin_panel_settings' }
          ]
        }
      ];
    }
    return groups;
  });

  breadcrumbs = computed(() => {
    const url = this.activeRoute();
    if (url === '/' || url === '/dashboard' || url === '') {
      return ['Dashboard'];
    }
    return ['Dashboard', ...url.split('/').filter(x => x).map(segment => {
      if (segment === 'platform-admin') return 'Platform Admin';
      if (segment === 'vehicles') return 'Vehicle Master';
      if (segment === 'drivers') return 'Driver Master';
      if (segment === 'customers') return 'Customer Master';
      if (segment === 'masters') return 'Branch Master';
      if (segment === 'materials-quarries') return 'Material & Quarry';
      if (segment === 'bookings') return 'Booking';
      if (segment === 'trips-planning') return 'Trip Management';
      if (segment === 'fuel-logs') return 'Fuel Management';
      if (segment === 'expense-logs') return 'Expense Management';
      if (segment === 'billing-invoices') return 'Invoice';
      if (segment === 'payment-logs') return 'Payments';
      if (segment === 'accounts-ledger') return 'Accounts';
      if (segment === 'reports-bi') return 'Reports';
      if (segment === 'users-roles') return 'User & Role Management';
      if (segment === 'company-admin') return 'System Settings';
      return segment.charAt(0).toUpperCase() + segment.slice(1);
    })];
  });

  constructor() {
    this.theme.loadPersisted();
    this.refreshProfile();

    this.router.events.pipe(
      filter(event => event instanceof NavigationEnd),
      takeUntil(this.destroy$)
    ).subscribe((event: any) => {
      this.activeRoute.set(event.urlAfterRedirects);
    });

    this.searchTerm$.pipe(
      debounceTime(280),
      distinctUntilChanged(),
      switchMap(term => this.runSearch(term)),
      takeUntil(this.destroy$)
    ).subscribe(hits => {
      this.searchHits.set(hits);
      this.searchLoading.set(false);
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  @HostListener('document:keydown', ['$event'])
  onGlobalKeydown(event: KeyboardEvent): void {
    const key = event.key?.toLowerCase();
    if ((event.ctrlKey || event.metaKey) && key === 'k') {
      event.preventDefault();
      this.openSearch();
      return;
    }
    if (key === 'escape' && this.searchOpen()) {
      event.preventDefault();
      this.closeSearch();
    }
  }

  openSearch(): void {
    this.searchOpen.set(true);
    if (!this.searchQuery().trim()) {
      this.searchHits.set(this.menuHits(''));
    }
    setTimeout(() => this.paletteInput?.nativeElement?.focus(), 0);
  }

  closeSearch(): void {
    this.searchOpen.set(false);
    this.searchQuery.set('');
    this.searchHits.set([]);
    this.searchLoading.set(false);
  }

  onSearchInput(value: string): void {
    this.searchQuery.set(value);
    const term = value.trim();
    if (!term) {
      this.searchHits.set(this.menuHits(''));
      this.searchLoading.set(false);
      return;
    }
    this.searchLoading.set(true);
    this.searchTerm$.next(term);
  }

  selectHit(hit: SearchHit): void {
    this.closeSearch();
    this.router.navigateByUrl(hit.route);
  }

  private companyId(): number {
    return resolveTenantCompanyId();
  }

  private menuHits(term: string): SearchHit[] {
    const q = term.toLowerCase();
    const pages: SearchHit[] = [
      { id: 'page-dashboard', category: 'Page', title: 'Dashboard', subtitle: 'Overview & KPIs', icon: 'dashboard', route: '/dashboard' },
      ...this.menuGroups().flatMap(g =>
        g.items.map(item => ({
          id: `page-${item.route}`,
          category: 'Page',
          title: item.label,
          subtitle: g.groupName,
          icon: item.icon,
          route: item.route
        }))
      )
    ];
    if (!q) return pages.slice(0, 8);
    return pages.filter(p =>
      p.title.toLowerCase().includes(q) || p.subtitle.toLowerCase().includes(q)
    );
  }

  private runSearch(term: string) {
    const q = term.toLowerCase();
    const companyId = this.companyId();
    const empty = { success: true, data: { content: [] as any[] } };

    return forkJoin({
      vehicles: this.masterService.getMasters<any>('vehicles', companyId, { search: term, size: 5, page: 0 }).pipe(catchError(() => of(empty))),
      drivers: this.masterService.getMasters<any>('drivers', companyId, { search: term, size: 5, page: 0 }).pipe(catchError(() => of(empty))),
      customers: this.masterService.getMasters<any>('customers', companyId, { search: term, size: 5, page: 0 }).pipe(catchError(() => of(empty))),
      bookings: this.bookingService.getBookings({ companyId, size: 30, page: 0 }).pipe(catchError(() => of(empty))),
      trips: this.tripService.getTrips({ companyId, size: 30, page: 0 }).pipe(catchError(() => of(empty)))
    }).pipe(
      switchMap(({ vehicles, drivers, customers, bookings, trips }) => {
        const hits: SearchHit[] = [...this.menuHits(term)];

        for (const v of vehicles?.data?.content || []) {
          hits.push({
            id: `vehicle-${v.id}`,
            category: 'Vehicle',
            title: v.code || v.name || `Vehicle #${v.id}`,
            subtitle: [v.name, v.model, v.status].filter(Boolean).join(' · '),
            icon: 'local_shipping',
            route: '/vehicles'
          });
        }
        for (const d of drivers?.data?.content || []) {
          hits.push({
            id: `driver-${d.id}`,
            category: 'Driver',
            title: d.name || d.code || `Driver #${d.id}`,
            subtitle: [d.code, d.licenseNumber, d.phoneNumber].filter(Boolean).join(' · '),
            icon: 'person',
            route: '/drivers'
          });
        }
        for (const c of customers?.data?.content || []) {
          hits.push({
            id: `customer-${c.id}`,
            category: 'Customer',
            title: c.name || c.code || `Customer #${c.id}`,
            subtitle: [c.code, c.phone, c.gstNumber].filter(Boolean).join(' · '),
            icon: 'group',
            route: '/customers'
          });
        }

        const bookingRows = bookings?.data?.content || [];
        for (const b of bookingRows) {
          const hay = `${b.bookingNumber || ''} ${b.code || ''} ${b.customer?.name || ''} ${b.status || ''}`.toLowerCase();
          if (!hay.includes(q)) continue;
          hits.push({
            id: `booking-${b.id}`,
            category: 'Booking',
            title: b.bookingNumber || b.code || `Booking #${b.id}`,
            subtitle: [b.customer?.name, b.status].filter(Boolean).join(' · '),
            icon: 'receipt_long',
            route: '/bookings'
          });
        }

        const tripRows = trips?.data?.content || [];
        for (const t of tripRows) {
          const hay = `${t.tripNumber || ''} ${t.code || ''} ${t.booking?.bookingNumber || ''} ${t.vehicle?.name || ''} ${t.driver?.name || ''} ${t.status || ''}`.toLowerCase();
          if (!hay.includes(q)) continue;
          hits.push({
            id: `trip-${t.id}`,
            category: 'Trip',
            title: t.tripNumber || t.code || `Trip #${t.id}`,
            subtitle: [t.booking?.bookingNumber, t.status].filter(Boolean).join(' · '),
            icon: 'map',
            route: '/trips-planning'
          });
        }

        return of(hits.slice(0, 20));
      })
    );
  }

  private refreshProfile(): void {
    if (!this.auth.isAuthenticated()) return;
    this.auth.getProfile().subscribe({
      error: () => {
        // Keep login/localStorage values if profile fetch fails
      }
    });
  }

  toggleSidebar() {
    this.sidebarCollapsed.update(val => !val);
  }

  toggleTheme() {
    this.theme.toggleTheme();
  }

  logout() {
    const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
      width: '380px',
      data: {
        title: 'Sign Out Confirm',
        message: 'Are you sure you want to end your current session and sign out of TransaFlow?',
        confirmText: 'Sign Out',
        cancelText: 'Cancel',
        type: 'danger'
      }
    });

    dialogRef.afterClosed().subscribe(confirmed => {
      if (confirmed) {
        this.auth.logout().subscribe({
          next: () => {
            this.auth.clearLocalSession();
            this.router.navigate(['/login']);
          },
          error: () => {
            this.auth.clearLocalSession();
            this.router.navigate(['/login']);
          }
        });
      }
    });
  }
}
