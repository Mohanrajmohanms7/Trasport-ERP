import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-renewal',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './renewal.html',
  styles: [`
    .pricing-card {
      transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
    }
    .pricing-card:hover {
      transform: translateY(-4px);
    }
  `]
})
export class RenewalComponent implements OnInit {
  private authService = inject(AuthService);
  private router = inject(Router);

  plans = signal<any[]>([]);
  selectedPlan = signal<any | null>(null);
  paymentMethod = 'UPI';
  loading = signal<boolean>(false);
  errorMessage = signal<string>('');
  successMessage = signal<string>('');
  currentUser = this.authService.currentUser;

  paymentMethods = [
    { value: 'UPI', label: 'UPI / QR Code Scan' },
    { value: 'BANK_TRANSFER', label: 'Direct NetBanking / Bank Transfer' },
    { value: 'CARD', label: 'Credit / Debit Card Online' },
    { value: 'CASH', label: 'Cash Payment' }
  ];

  ngOnInit(): void {
    this.loadPlans();
  }

  loadPlans(): void {
    this.loading.set(true);
    this.authService.getActivePlans().subscribe({
      next: (res) => {
        if (res.success && res.data) {
          // Sort plans by price ASC
          const sorted = res.data.sort((a, b) => a.price - b.price);
          this.plans.set(sorted);
          // Default selection to first plan
          if (sorted.length > 0) {
            this.selectedPlan.set(sorted[0]);
          }
        }
        this.loading.set(false);
      },
      error: (err) => {
        this.errorMessage.set('Failed to fetch subscription plans: ' + (err.error?.message || err.message));
        this.loading.set(false);
      }
    });
  }

  selectPlan(plan: any): void {
    this.selectedPlan.set(plan);
  }

  renew(): void {
    const plan = this.selectedPlan();
    if (!plan) return;

    this.loading.set(true);
    this.errorMessage.set('');
    this.successMessage.set('');

    this.authService.renewSubscription(plan.id, this.paymentMethod).subscribe({
      next: (res) => {
        if (res.success) {
          this.successMessage.set('Subscription successfully renewed! Thank you.');
          this.authService.setSubscriptionExpired(false);
          setTimeout(() => {
            this.router.navigate(['/']);
          }, 1500);
        } else {
          this.errorMessage.set(res.message || 'Renewal failed. Please check payment details.');
          this.loading.set(false);
        }
      },
      error: (err) => {
        this.errorMessage.set(err.error?.message || err.message || 'Failed to process renewal.');
        this.loading.set(false);
      }
    });
  }

  logout(): void {
    this.authService.logout().subscribe({
      next: () => {
        this.router.navigate(['/login']);
      },
      error: () => {
        this.router.navigate(['/login']);
      }
    });
  }
}
