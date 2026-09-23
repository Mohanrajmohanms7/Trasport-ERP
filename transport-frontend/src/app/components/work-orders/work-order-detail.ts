import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { AuthService } from '../../services/auth.service';
import { SparePart, SparePartService } from '../../services/spare-part.service';
import { WorkOrder, WorkOrderLabourLine, WorkOrderPartLine, WorkOrderService, workOrderError } from '../../services/work-order.service';

@Component({
  selector: 'app-work-order-detail',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './work-order-detail.html'
})
export class WorkOrderDetailComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private workOrders = inject(WorkOrderService);
  private sparePartsApi = inject(SparePartService);
  private http = inject(HttpClient);
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

  spareParts = signal<SparePart[]>([]);
  people = signal<{ id: number; name: string }[]>([]);
  partSparePartId = signal('');
  partQuantity = signal('');
  partRate = signal('');
  partNotes = signal('');
  partEditingId = signal<number | null>(null);
  labourUserId = signal('');
  labourDescription = signal('');
  labourHours = signal('');
  labourRate = signal('');
  labourDate = signal('');
  labourNotes = signal('');
  labourEditingId = signal<number | null>(null);

  ngOnInit() {
    const roles = this.auth.currentUser()?.roles || [];
    this.canWrite.set(roles.some(role =>
      role === 'SUPER_ADMIN' || role === 'COMPANY_ADMIN' || role === 'BRANCH_MANAGER'));
    this.load();
    this.sparePartsApi.list().subscribe({
      next: res => this.spareParts.set(res?.success && res.data ? res.data.content || [] : []),
      error: () => this.spareParts.set([])
    });
    this.http.get<any>('/api/v1/users', { params: { page: '0', size: '100' } }).subscribe({
      next: res => {
        const rows = res?.data?.content || [];
        this.people.set(rows.map((row: any) => ({
          id: row.id,
          name: row.name || row.username || String(row.id)
        })));
      },
      error: () => this.people.set([])
    });
  }

  accountingLabel(status?: string | null): string {
    if (status === 'POSTED') return 'Posted';
    if (status === 'REVERSED') return 'Reversed';
    return 'Not Posted';
  }

  canEditLines(order: WorkOrder): boolean {
    return this.canWrite() && (order.status === 'OPEN' || order.status === 'IN_PROGRESS');
  }

  savePart() {
    const current = this.order();
    if (!current || !this.canEditLines(current)) return;
    this.error.set(null);
    this.feedback.set(null);
    if (!this.partSparePartId() || !this.partQuantity().trim()) {
      this.error.set('Part and quantity are required.');
      return;
    }
    const body: Record<string, unknown> = {
      sparePartId: Number(this.partSparePartId()),
      quantity: Number(this.partQuantity()),
      notes: this.partNotes().trim() || null
    };
    if (this.partRate().trim()) body['unitRate'] = Number(this.partRate());
    const editing = this.partEditingId();
    const call = editing
      ? this.workOrders.updatePart(current.id, editing, body)
      : this.workOrders.addPart(current.id, body);
    call.subscribe({
      next: res => this.afterLine(res, 'Part saved.'),
      error: err => this.error.set(workOrderError(err))
    });
  }

  editPart(line: WorkOrderPartLine) {
    this.partEditingId.set(line.id);
    this.partSparePartId.set(line.sparePartId != null ? String(line.sparePartId) : '');
    this.partQuantity.set(String(line.quantity));
    this.partRate.set(line.unitRate != null ? String(line.unitRate) : '');
    this.partNotes.set(line.notes || '');
  }

  removePart(line: WorkOrderPartLine) {
    const current = this.order();
    if (!current || !this.canEditLines(current)) return;
    this.workOrders.removePart(current.id, line.id).subscribe({
      next: res => this.afterLine(res, 'Part removed.'),
      error: err => this.error.set(workOrderError(err))
    });
  }

  saveLabour() {
    const current = this.order();
    if (!current || !this.canEditLines(current)) return;
    this.error.set(null);
    this.feedback.set(null);
    if (!this.labourDescription().trim() || !this.labourHours().trim() || !this.labourRate().trim()) {
      this.error.set('Description, hours, and rate are required.');
      return;
    }
    const body: Record<string, unknown> = {
      appUserId: this.labourUserId() ? Number(this.labourUserId()) : null,
      description: this.labourDescription().trim(),
      hours: Number(this.labourHours()),
      rate: Number(this.labourRate()),
      workDate: this.labourDate() || null,
      notes: this.labourNotes().trim() || null
    };
    const editing = this.labourEditingId();
    const call = editing
      ? this.workOrders.updateLabour(current.id, editing, body)
      : this.workOrders.addLabour(current.id, body);
    call.subscribe({
      next: res => this.afterLine(res, 'Labour saved.'),
      error: err => this.error.set(workOrderError(err))
    });
  }

  editLabour(line: WorkOrderLabourLine) {
    this.labourEditingId.set(line.id);
    this.labourUserId.set(line.appUserId != null ? String(line.appUserId) : '');
    this.labourDescription.set(line.description || '');
    this.labourHours.set(String(line.hours));
    this.labourRate.set(line.rate != null ? String(line.rate) : '');
    this.labourDate.set(line.workDate || '');
    this.labourNotes.set(line.notes || '');
  }

  removeLabour(line: WorkOrderLabourLine) {
    const current = this.order();
    if (!current || !this.canEditLines(current)) return;
    this.workOrders.removeLabour(current.id, line.id).subscribe({
      next: res => this.afterLine(res, 'Labour removed.'),
      error: err => this.error.set(workOrderError(err))
    });
  }

  private afterLine(res: { success?: boolean; data?: WorkOrder; message?: string } | null, message: string) {
    if (res?.success && res.data) {
      this.apply(res.data);
      this.feedback.set(message);
      this.partEditingId.set(null);
      this.partQuantity.set('');
      this.partRate.set('');
      this.partNotes.set('');
      this.labourEditingId.set(null);
      this.labourDescription.set('');
      this.labourHours.set('');
      this.labourRate.set('');
      this.labourDate.set('');
      this.labourNotes.set('');
      this.labourUserId.set('');
    } else {
      this.error.set(res?.message || 'The line could not be saved.');
    }
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
