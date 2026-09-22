import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { WorkOrder, WorkOrderService, workOrderError } from '../../services/work-order.service';

@Component({
  selector: 'app-work-order-detail',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './work-order-detail.html'
})
export class WorkOrderDetailComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private workOrders = inject(WorkOrderService);
  private auth = inject(AuthService);

  loading = signal(true);
  error = signal<string | null>(null);
  feedback = signal<string | null>(null);
  order = signal<WorkOrder | null>(null);
  canWrite = signal(false);

  name = signal('');
  description = signal('');
  priority = signal('NORMAL');
  diagnosis = signal('');
  estimatedCost = signal('');
  requestedDate = signal('');
  completionNotes = signal('');
  actualCost = signal('');
  cancellationReason = signal('');

  ngOnInit() {
    const roles = this.auth.currentUser()?.roles || [];
    this.canWrite.set(roles.some(role =>
      role === 'SUPER_ADMIN' || role === 'COMPANY_ADMIN' || role === 'BRANCH_MANAGER'));
    this.load();
  }

  load() {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!Number.isFinite(id)) {
      this.loading.set(false);
      this.error.set('Work order not found.');
      return;
    }
    this.loading.set(true);
    this.error.set(null);
    this.workOrders.get(id).subscribe({
      next: res => {
        if (!res?.success || !res.data) {
          this.error.set(res?.message || 'Unable to load the work order.');
          this.order.set(null);
        } else {
          this.apply(res.data);
        }
        this.loading.set(false);
      },
      error: err => {
        this.error.set(workOrderError(err));
        this.loading.set(false);
      }
    });
  }

  save() {
    const current = this.order();
    if (!current) return;
    this.feedback.set(null);
    this.error.set(null);
    const body: Record<string, unknown> = current.status === 'IN_PROGRESS'
      ? {
          diagnosis: this.diagnosis(),
          estimatedCost: this.estimatedCost().trim() ? Number(this.estimatedCost()) : null
        }
      : {
          name: this.name().trim(),
          description: this.description(),
          priority: this.priority(),
          diagnosis: this.diagnosis(),
          estimatedCost: this.estimatedCost().trim() ? Number(this.estimatedCost()) : null,
          requestedDate: this.requestedDate() || null
        };
    this.workOrders.update(current.id, body).subscribe({
      next: res => {
        if (res?.success && res.data) {
          this.apply(res.data);
          this.feedback.set('Work order updated.');
        } else {
          this.error.set(res?.message || 'Update failed.');
        }
      },
      error: err => this.error.set(workOrderError(err))
    });
  }

  start() {
    this.transition(id => this.workOrders.start(id), 'Work order started.');
  }

  complete() {
    const current = this.order();
    if (!current) return;
    if (!this.completionNotes().trim()) {
      this.error.set('Completion notes are required.');
      return;
    }
    const cost = this.actualCost().trim();
    this.transition(
      id => this.workOrders.complete(id, {
        completionNotes: this.completionNotes().trim(),
        actualCost: cost ? Number(cost) : null
      }),
      'Work order completed.');
  }

  cancel() {
    const current = this.order();
    if (!current) return;
    if (!this.cancellationReason().trim()) {
      this.error.set('A cancellation reason is required.');
      return;
    }
    this.transition(
      id => this.workOrders.cancel(id, { cancellationReason: this.cancellationReason().trim() }),
      'Work order cancelled.');
  }

  private transition(call: (id: number) => any, message: string) {
    const current = this.order();
    if (!current) return;
    this.error.set(null);
    this.feedback.set(null);
    call(current.id).subscribe({
      next: (res: any) => {
        if (res?.success && res.data) {
          this.apply(res.data);
          this.feedback.set(message);
        } else {
          this.error.set(res?.message || 'The action failed.');
        }
      },
      error: (err: any) => this.error.set(workOrderError(err))
    });
  }

  private apply(order: WorkOrder) {
    this.order.set(order);
    this.name.set(order.name || '');
    this.description.set(order.description || '');
    this.priority.set(order.priority || 'NORMAL');
    this.diagnosis.set(order.diagnosis || '');
    this.estimatedCost.set(order.estimatedCost != null ? String(order.estimatedCost) : '');
    this.requestedDate.set(order.requestedDate || '');
    this.completionNotes.set(order.completionNotes || '');
  }
}
