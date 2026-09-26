import { AttachmentsPanelComponent } from '../../shared/attachments-panel/attachments-panel';
import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { MaintenanceRequest, MaintenanceRequestService, maintenanceRequestError } from '../../services/maintenance-request.service';

@Component({
  selector: 'app-maintenance-request-detail',
  standalone: true,
  imports: [AttachmentsPanelComponent, CommonModule, FormsModule, RouterLink],
  templateUrl: './maintenance-request-detail.html'
})
export class MaintenanceRequestDetailComponent implements OnInit {
  readonly steps = [
    { key: 'OPEN', label: 'Reported' },
    { key: 'UNDER_REVIEW', label: 'Under review' },
    { key: 'APPROVED', label: 'Approved' },
    { key: 'CONVERTED', label: 'Work order created' }
  ];

  stepDone(row: any, index: number): boolean {
    const order = ['OPEN', 'UNDER_REVIEW', 'APPROVED', 'CONVERTED'];
    const at = order.indexOf(row?.status);
    return at >= index;
  }

  private requests = inject(MaintenanceRequestService);
  private route = inject(ActivatedRoute);
  private auth = inject(AuthService);

  loading = signal(true);
  error = signal<string | null>(null);
  feedback = signal<string | null>(null);
  request = signal<MaintenanceRequest | null>(null);
  remarks = signal('');
  cancelReason = signal('');
  readonly canManage = signal(false);

  ngOnInit() {
    const roles = this.auth.currentUser()?.roles || [];
    this.canManage.set(roles.some(role =>
      role === 'SUPER_ADMIN' || role === 'COMPANY_ADMIN' || role === 'BRANCH_MANAGER'));
    this.load();
  }

  load() {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.loading.set(true);
    this.requests.get(id).subscribe({
      next: res => {
        this.request.set(res?.success ? res.data : null);
        if (!res?.success) this.error.set(res?.message || 'Unable to load the request.');
        this.loading.set(false);
      },
      error: err => {
        this.error.set(maintenanceRequestError(err));
        this.loading.set(false);
      }
    });
  }

  review() {
    const row = this.request();
    if (!row) return;
    this.act(this.requests.review(row.id, this.remarks()), 'Request reviewed.');
  }

  approve() {
    const row = this.request();
    if (!row) return;
    this.act(this.requests.approve(row.id), 'Request approved.');
  }

  cancel() {
    const row = this.request();
    if (!row) return;
    this.act(this.requests.cancel(row.id, this.cancelReason()), 'Request cancelled.');
  }

  convert() {
    const row = this.request();
    if (!row) return;
    this.act(this.requests.convert(row.id), 'Work order created.');
  }

  private act(call: ReturnType<MaintenanceRequestService['approve']>, message: string) {
    this.error.set(null);
    this.feedback.set(null);
    call.subscribe({
      next: res => {
        if (res?.success) {
          this.request.set(res.data);
          this.feedback.set(message);
          return;
        }
        this.error.set(res?.message || 'The action failed.');
      },
      error: err => this.error.set(maintenanceRequestError(err))
    });
  }
}
