import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormArray, FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { FfButtonComponent, FfDatepickerComponent, FfDropdownComponent, FfNumberComponent, FfSelectOption, FfTextboxComponent } from '@ff/ui';
import { AuthService } from '../../services/auth.service';
import { MasterService } from '../../services/master.service';
import { DriverAdvance, DriverPayrollService, PaySlab, SlabPayroll } from '../../services/driver-payroll.service';
import { ConfirmationDialogComponent } from '../../shared/confirmation-dialog/confirmation-dialog';
import { resolveTenantCompanyId } from '../../shared/tenant-context';
import { FfNotificationService } from '../../shared-ui/infrastructure/services/ff-notification.service';

type Tab = 'payrolls' | 'advances' | 'slabs';

/**
 * Driver Daily Slab Payroll.
 * Every trip count, daily amount, gross and net shown here comes from the backend.
 */
@Component({
  selector: 'app-driver-payroll-console',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, MatDialogModule, FfButtonComponent, FfDatepickerComponent, FfDropdownComponent, FfNumberComponent, FfTextboxComponent],
  templateUrl: './driver-payroll-console.html'
})
export class DriverPayrollConsoleComponent implements OnInit {
  private api = inject(DriverPayrollService);
  private auth = inject(AuthService);
  private masters = inject(MasterService);
  private fb = inject(FormBuilder);
  private dialog = inject(MatDialog);
  private notify = inject(FfNotificationService);
  private companyId = resolveTenantCompanyId();

  readonly today = new Date().toISOString().slice(0, 10);
  readonly months = ['January', 'February', 'March', 'April', 'May', 'June', 'July', 'August', 'September', 'October', 'November', 'December'];

  readonly roles = computed(() => this.auth.currentUser()?.roles ?? []);
  readonly isDriverOnly = computed(() => {
    const r = this.roles();
    return r.includes('DRIVER') && !r.some(x => ['SUPER_ADMIN', 'COMPANY_ADMIN', 'ADMIN', 'BRANCH_MANAGER', 'ACCOUNTANT'].includes(x));
  });
  readonly canPost = computed(() => this.roles().some(x => ['SUPER_ADMIN', 'COMPANY_ADMIN', 'ADMIN', 'ACCOUNTANT'].includes(x)));

  tab = signal<Tab>('payrolls');
  loading = signal(false);
  payrolls = signal<SlabPayroll[]>([]);
  selected = signal<SlabPayroll | null>(null);
  drivers = signal<any[]>([]);
  advances = signal<DriverAdvance[]>([]);
  slabs = signal<PaySlab[]>([]);
  outstanding = signal<number | null>(null);

  showGenerate = signal(false);
  editingId = signal<number | null>(null);
  showPay = signal(false);
  showAdvance = signal(false);

  filterForm = this.fb.group({ driverId: [''], payYear: [''], payMonth: [''], status: [''] });

  generateForm = this.fb.group({
    driverId: ['', Validators.required],
    payYear: [new Date().getFullYear(), [Validators.required, Validators.min(2000)]],
    payMonth: [new Date().getMonth() + 1, Validators.required],
    allowanceAmount: [0, Validators.min(0)],
    advanceAdjustment: [0, Validators.min(0)],
    description: [''],
    deductions: this.fb.array([])
  });

  payForm = this.fb.group({
    paymentMethod: ['CASH', Validators.required],
    paymentDate: [this.today, Validators.required],
    paymentReference: ['']
  });

  advanceForm = this.fb.group({
    driverId: ['', Validators.required],
    amount: [null as number | null, [Validators.required, Validators.min(1)]],
    advanceDate: [this.today, Validators.required],
    paymentMethod: ['CASH', Validators.required],
    paymentReference: [''],
    remarks: ['']
  });

  slabForm = this.fb.group({ rows: this.fb.array([]) });

  readonly statusOptions: FfSelectOption[] = [
    { label: 'All statuses', value: '' },
    ...['DRAFT', 'APPROVED', 'POSTED', 'PAID', 'CANCELLED'].map(s => ({ label: s, value: s }))
  ];
  readonly methodOptions: FfSelectOption[] = [
    { label: 'Cash', value: 'CASH' }, { label: 'Bank Transfer', value: 'BANK_TRANSFER' },
    { label: 'Cheque', value: 'CHEQUE' }, { label: 'UPI', value: 'UPI' }
  ];
  readonly deductionTypeOptions: FfSelectOption[] = [
    { label: 'Fine', value: 'FINE' }, { label: 'Damage', value: 'DAMAGE' }, { label: 'Other', value: 'OTHER' }
  ];
  get monthOptions(): FfSelectOption[] {
    return this.months.map((m, i) => ({ label: m, value: i + 1 }));
  }
  get monthFilterOptions(): FfSelectOption[] {
    return [{ label: 'All months', value: '' }, ...this.monthOptions];
  }
  get driverOptions(): FfSelectOption[] {
    return this.drivers().map(d => ({ label: [d.code, d.name].filter(Boolean).join(' — '), value: d.id }));
  }
  get driverFilterOptions(): FfSelectOption[] {
    return [{ label: 'All drivers', value: '' }, ...this.driverOptions];
  }
  get deductionRows(): FormArray {
    return this.generateForm.get('deductions') as FormArray;
  }
  get slabRows(): FormArray {
    return this.slabForm.get('rows') as FormArray;
  }

  ngOnInit(): void {
    if (this.isDriverOnly()) {
      this.loadMine();
      return;
    }
    this.masters.getMasters<any>('drivers', this.companyId, { size: 500 }).subscribe(res => {
      if (res.success && res.data) this.drivers.set((res.data as any).content || res.data);
    });
    this.generateForm.get('driverId')!.valueChanges.subscribe(v => {
      if (!this.editingId()) this.onDriverPicked(v);
    });
    this.loadPayrolls();
    this.loadSlabs();
  }

  setTab(t: Tab): void {
    this.tab.set(t);
    if (t === 'advances') this.loadAdvances();
    if (t === 'slabs') this.loadSlabs();
    if (t === 'payrolls') this.loadPayrolls();
  }

  monthName(m: number): string {
    return this.months[m - 1] ?? String(m);
  }

  slabLabel(from: number | null, to: number | null): string {
    if (from == null) return 'No slab';
    if (to == null) return `${from}+ trips`;
    return from === to ? `${from} trip${from === 1 ? '' : 's'}` : `${from}–${to} trips`;
  }

  private err(e: any, fallback: string): string {
    const errors = e?.error?.errors;
    const detail = Array.isArray(errors) && errors.length ? String(errors[0]) : '';
    const title = e?.error?.message || '';
    if (title && detail && !detail.startsWith(title)) return `${title}: ${detail}`;
    return detail || title || fallback;
  }

  // ---------------------------------------------------------------- payroll list / detail

  loadPayrolls(): void {
    const f = this.filterForm.getRawValue();
    this.loading.set(true);
    this.api.search({
      driverId: f.driverId ? Number(f.driverId) : null,
      payYear: f.payYear ? Number(f.payYear) : null,
      payMonth: f.payMonth ? Number(f.payMonth) : null,
      status: f.status || null
    }).subscribe({
      next: res => {
        this.payrolls.set(res.success && res.data ? (res.data.content || res.data) : []);
        this.loading.set(false);
      },
      error: e => {
        this.loading.set(false);
        this.notify.error(this.err(e, 'Failed to load payrolls'));
      }
    });
  }

  loadMine(): void {
    this.loading.set(true);
    this.api.myPayrolls().subscribe({
      next: res => {
        this.payrolls.set(res.success && res.data ? res.data : []);
        this.loading.set(false);
      },
      error: e => {
        this.loading.set(false);
        this.notify.error(this.err(e, 'Failed to load your salary slips'));
      }
    });
  }

  open(p: SlabPayroll): void {
    this.selected.set(p);
  }

  private refresh(p: SlabPayroll | null, message: string): void {
    this.notify.success(message);
    if (p) this.selected.set(p);
    this.loadPayrolls();
  }

  private confirm(title: string, message: string, action: () => void, type: 'info' | 'danger' = 'info'): void {
    this.dialog.open(ConfirmationDialogComponent, { data: { title, message, confirmText: title, type } })
      .afterClosed().subscribe(ok => { if (ok) action(); });
  }

  approve(p: SlabPayroll): void {
    this.confirm('Approve', `Approve ${p.payrollNumber}? Trips are re-read before approval.`, () =>
      this.api.approve(p.id).subscribe({ next: r => this.refresh(r.data, 'Payroll approved'), error: e => this.notify.error(this.err(e, 'Approve failed')) }));
  }

  post(p: SlabPayroll): void {
    this.confirm('Post', `Post ${p.payrollNumber} to accounts? Salary expense ₹${p.grossAmount} will be recorded and the payroll locked.`, () =>
      this.api.post(p.id).subscribe({ next: r => this.refresh(r.data, 'Payroll posted to accounts'), error: e => this.notify.error(this.err(e, 'Posting failed')) }));
  }

  recalculate(p: SlabPayroll): void {
    this.api.recalculate(p.id).subscribe({ next: r => this.refresh(r.data, 'Recalculated from trips'), error: e => this.notify.error(this.err(e, 'Recalculation failed')) });
  }

  cancel(p: SlabPayroll): void {
    this.confirm('Cancel Payroll', `Cancel ${p.payrollNumber}? Accounting entries are reversed and any advance recovery is returned.`, () =>
      this.api.cancel(p.id).subscribe({ next: r => this.refresh(r.data, 'Payroll cancelled'), error: e => this.notify.error(this.err(e, 'Cancel failed')) }), 'danger');
  }

  remove(p: SlabPayroll): void {
    this.confirm('Delete Draft', `Delete draft ${p.payrollNumber}?`, () =>
      this.api.remove(p.id).subscribe({
        next: () => { this.selected.set(null); this.refresh(null, 'Draft deleted'); },
        error: e => this.notify.error(this.err(e, 'Delete failed'))
      }), 'danger');
  }

  downloadSlip(p: SlabPayroll): void {
    this.api.slipPdf(p.id, this.isDriverOnly()).subscribe({
      next: blob => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `Salary_Slip_${p.payrollNumber}.pdf`;
        a.click();
        URL.revokeObjectURL(url);
      },
      error: () => this.notify.error('Failed to download salary slip')
    });
  }

  // ---------------------------------------------------------------- generate / edit

  openGenerate(p?: SlabPayroll): void {
    this.editingId.set(p?.id ?? null);
    while (this.deductionRows.length) this.deductionRows.removeAt(0);
    const now = new Date();
    const prev = new Date(now.getFullYear(), now.getMonth() - 1, 1);
    this.generateForm.reset({
      driverId: (p?.driver?.id ?? '') as any,
      payYear: p?.payYear ?? prev.getFullYear(),
      payMonth: p?.payMonth ?? prev.getMonth() + 1,
      allowanceAmount: p?.allowanceAmount ?? 0,
      advanceAdjustment: p?.advanceAdjustment ?? 0,
      description: p?.description ?? ''
    });
    (p?.deductions ?? []).forEach(d => this.addDeduction(d));
    if (p) {
      this.generateForm.get('driverId')?.disable();
      this.generateForm.get('payYear')?.disable();
      this.generateForm.get('payMonth')?.disable();
    } else {
      this.generateForm.enable();
    }
    this.outstanding.set(null);
    if (p?.driver?.id) this.onDriverPicked(p.driver.id);
    this.showGenerate.set(true);
  }

  onDriverPicked(driverId: any): void {
    const id = Number(driverId);
    if (!id) { this.outstanding.set(null); return; }
    this.api.outstanding(id).subscribe({ next: r => this.outstanding.set(Number(r.data ?? 0)), error: () => this.outstanding.set(null) });
  }

  addDeduction(d?: any): void {
    this.deductionRows.push(this.fb.group({
      deductionType: [d?.deductionType ?? 'FINE', Validators.required],
      amount: [d?.amount ?? null, [Validators.required, Validators.min(0.01)]],
      deductionDate: [d?.deductionDate ?? this.today],
      remarks: [d?.remarks ?? '']
    }));
  }

  removeDeduction(i: number): void {
    this.deductionRows.removeAt(i);
  }

  submitGenerate(): void {
    if (this.generateForm.invalid) {
      this.generateForm.markAllAsTouched();
      return;
    }
    const v = this.generateForm.getRawValue() as any;
    const req = {
      driverId: Number(v.driverId),
      payYear: Number(v.payYear),
      payMonth: Number(v.payMonth),
      allowanceAmount: Number(v.allowanceAmount || 0),
      advanceAdjustment: Number(v.advanceAdjustment || 0),
      description: v.description || undefined,
      deductions: (v.deductions || []).map((d: any) => ({ ...d, amount: Number(d.amount) }))
    };
    const id = this.editingId();
    this.loading.set(true);
    (id ? this.api.update(id, req) : this.api.generate(req)).subscribe({
      next: r => {
        this.loading.set(false);
        this.showGenerate.set(false);
        this.refresh(r.data, id ? 'Draft recalculated' : 'Payroll generated from completed trips');
      },
      error: e => {
        this.loading.set(false);
        this.notify.error(this.err(e, 'Payroll could not be generated'));
      }
    });
  }

  // ---------------------------------------------------------------- pay

  openPay(p: SlabPayroll): void {
    this.selected.set(p);
    this.payForm.reset({ paymentMethod: 'CASH', paymentDate: this.today, paymentReference: '' });
    this.showPay.set(true);
  }

  submitPay(): void {
    const p = this.selected();
    if (!p || this.payForm.invalid) return;
    const v = this.payForm.getRawValue();
    this.api.pay(p.id, { paymentMethod: v.paymentMethod!, paymentDate: v.paymentDate || undefined, paymentReference: v.paymentReference || undefined }).subscribe({
      next: r => { this.showPay.set(false); this.refresh(r.data, 'Salary paid'); },
      error: e => this.notify.error(this.err(e, 'Payment failed'))
    });
  }

  // ---------------------------------------------------------------- advances

  loadAdvances(): void {
    const driverId = this.filterForm.get('driverId')?.value;
    this.api.getAdvances(driverId ? Number(driverId) : null).subscribe({
      next: r => this.advances.set(r.success && r.data ? (r.data.content || r.data) : []),
      error: e => this.notify.error(this.err(e, 'Failed to load advances'))
    });
  }

  openAdvance(): void {
    this.advanceForm.reset({ driverId: '', amount: null, advanceDate: this.today, paymentMethod: 'CASH', paymentReference: '', remarks: '' });
    this.showAdvance.set(true);
  }

  submitAdvance(): void {
    if (this.advanceForm.invalid) {
      this.advanceForm.markAllAsTouched();
      return;
    }
    const v = this.advanceForm.getRawValue();
    this.api.issueAdvance({
      driver: { id: Number(v.driverId) },
      amount: Number(v.amount),
      advanceDate: v.advanceDate || undefined,
      paymentMethod: v.paymentMethod!,
      paymentReference: v.paymentReference || undefined,
      remarks: v.remarks || undefined
    }).subscribe({
      next: r => { this.showAdvance.set(false); this.notify.success(`Advance ${r.data?.advanceNumber} issued`); this.loadAdvances(); },
      error: e => this.notify.error(this.err(e, 'Advance could not be issued'))
    });
  }

  cancelAdvance(a: DriverAdvance): void {
    this.confirm('Cancel Advance', `Cancel advance ${a.advanceNumber}? The cash entry is reversed.`, () =>
      this.api.cancelAdvance(a.id!).subscribe({
        next: () => { this.notify.success('Advance cancelled'); this.loadAdvances(); },
        error: e => this.notify.error(this.err(e, 'Cancel failed'))
      }), 'danger');
  }

  // ---------------------------------------------------------------- slabs

  loadSlabs(): void {
    this.api.getSlabs().subscribe({
      next: r => {
        this.slabs.set(r.data ?? []);
        while (this.slabRows.length) this.slabRows.removeAt(0);
        (r.data ?? []).forEach(s => this.addSlab(s));
        if (!this.slabRows.length) this.addSlab();
      },
      error: () => this.slabs.set([])
    });
  }

  addSlab(s?: PaySlab): void {
    const last = this.slabRows.length ? this.slabRows.at(this.slabRows.length - 1).value : null;
    const nextFrom = last ? (Number(last.tripsTo || last.tripsFrom) + 1) : 1;
    this.slabRows.push(this.fb.group({
      tripsFrom: [s?.tripsFrom ?? nextFrom, [Validators.required, Validators.min(1)]],
      tripsTo: [s?.tripsTo ?? null],
      dailyAmount: [s?.dailyAmount ?? null, [Validators.required, Validators.min(0)]]
    }));
  }

  removeSlab(i: number): void {
    this.slabRows.removeAt(i);
  }

  saveSlabs(): void {
    if (this.slabForm.invalid) {
      this.slabForm.markAllAsTouched();
      return;
    }
    const rows = (this.slabRows.getRawValue() as any[]).map(r => ({
      tripsFrom: Number(r.tripsFrom),
      tripsTo: r.tripsTo === null || r.tripsTo === '' ? null : Number(r.tripsTo),
      dailyAmount: Number(r.dailyAmount)
    }));
    this.api.saveSlabs(rows).subscribe({
      next: r => { this.slabs.set(r.data ?? []); this.notify.success('Daily pay slabs saved'); },
      error: e => this.notify.error(this.err(e, 'Slabs could not be saved'))
    });
  }
}
