import { FormValidationDirective } from '../../shared/form-validation.directive';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { FfDatepickerComponent, FfDropdownComponent, FfNumberComponent, FfSelectOption, FfTextboxComponent } from '@ff/ui';
import { FeatureService } from '../../services/feature.service';
import { FamilyExpense, FamilyExpenseService, FamilyFilter, FamilyOption, FamilyReport } from '../../services/family-expense.service';
import { FfNotificationService } from '../../shared-ui/infrastructure/services/ff-notification.service';
import { ConfirmationDialogComponent } from '../../shared/confirmation-dialog/confirmation-dialog';
import { AttachmentsPanelComponent } from '../../shared/attachments-panel/attachments-panel';

type Tab = 'expenses' | 'reports' | 'categories';

/**
 * Family Expenses (optional add-on, docs/FAMILY_EXPENSES.md): the owner's personal expense register.
 * Completely separate from business expenses — nothing here changes cash/bank, accounts, dashboard or business reports.
 */
@Component({
  selector: 'app-family-expenses',
  standalone: true,
  imports: [FormValidationDirective, CommonModule, ReactiveFormsModule, MatDialogModule, FfDatepickerComponent, FfDropdownComponent, FfNumberComponent,
    FfTextboxComponent, AttachmentsPanelComponent],
  templateUrl: './family-expenses.html'
})
export class FamilyExpensesComponent implements OnInit {
  private api = inject(FamilyExpenseService);
  private notify = inject(FfNotificationService);
  private dialog = inject(MatDialog);
  private fb = inject(FormBuilder);
  readonly features = inject(FeatureService);

  readonly today = new Date().toISOString().slice(0, 10);
  tab = signal<Tab>('expenses');

  // options
  categories = signal<FamilyOption[]>([]);
  modes = signal<FamilyOption[]>([]);
  allCategories = signal<FamilyOption[]>([]);

  // register
  month = signal(this.today.slice(0, 7));
  filterCategory = signal('');
  filterMode = signal('');
  rows = signal<FamilyExpense[]>([]);
  totalAmount = signal(0);
  summary = signal<any>(null);
  loading = signal(false);

  // editor
  showEditor = signal(false);
  editing = signal<FamilyExpense | null>(null);
  saving = signal(false);
  form = this.fb.group({
    expenseDate: [this.today, Validators.required],
    category: ['', Validators.required],
    amount: [null as number | null, [Validators.required, Validators.min(0.01)]],
    paymentMode: ['CASH', Validators.required],
    description: [''],
    memberName: [''],
    referenceNo: ['']
  });

  // reports
  readonly reportList = [
    { key: 'daily', label: 'Daily' }, { key: 'monthly', label: 'Monthly' }, { key: 'category', label: 'Category-wise' },
    { key: 'mode', label: 'Payment mode' }, { key: 'yearly', label: 'Yearly' }, { key: 'comparison', label: 'Category comparison' },
    { key: 'register', label: 'All entries' }];
  reportKey = signal('monthly');
  reportFrom = signal(this.fyStart());
  reportTo = signal(this.today);
  report = signal<FamilyReport | null>(null);
  exporting = signal<string | null>(null);

  // categories tab
  newCategory = signal('');

  readonly canAdd = computed(() => { this.features.disabled(); return this.features.has('family-expenses.create'); });
  readonly canEdit = computed(() => { this.features.disabled(); return this.features.has('family-expenses.edit'); });
  readonly canDelete = computed(() => { this.features.disabled(); return this.features.has('family-expenses.delete'); });
  readonly canReports = computed(() => { this.features.disabled(); return this.features.has('family-expenses.reports'); });
  readonly canExport = computed(() => { this.features.disabled(); return this.features.has('family-expenses.export') && this.features.has('export'); });
  readonly canCategories = computed(() => { this.features.disabled(); return this.features.has('family-expenses.categories'); });

  get categoryOptions(): FfSelectOption[] {
    const list = [...this.categories()];
    const cur = this.editing()?.category;
    if (cur && !list.some(c => c.code === cur)) list.push({ code: cur, name: this.editing()?.categoryName || cur });
    return [{ label: '-- Choose category --', value: '' }, ...list.map(c => ({ label: c.name, value: c.code }))];
  }
  get modeOptions(): FfSelectOption[] {
    const list = [...this.modes()];
    const cur = this.editing()?.paymentMode;
    if (cur && !list.some(m => m.code === cur)) list.push({ code: cur, name: this.editing()?.paymentModeName || cur });
    return list.map(m => ({ label: m.name, value: m.code }));
  }

  ngOnInit(): void {
    this.loadOptions();
    this.load();
  }

  private fyStart(): string {
    const d = new Date();
    const y = d.getMonth() >= 3 ? d.getFullYear() : d.getFullYear() - 1;
    return `${y}-04-01`;
  }

  private monthRange(): FamilyFilter {
    const [y, m] = this.month().split('-').map(Number);
    const last = new Date(y, m, 0).getDate();
    return { from: `${this.month()}-01`, to: `${this.month()}-${String(last).padStart(2, '0')}`, category: this.filterCategory(), mode: this.filterMode() };
  }

  private err(e: any, fallback: string): string {
    const errors = e?.error?.errors;
    return (Array.isArray(errors) && errors.length ? String(errors[0]) : '') || e?.error?.message || fallback;
  }

  loadOptions(): void {
    this.api.options().subscribe({
      next: r => { this.categories.set(r.data?.categories || []); this.modes.set(r.data?.paymentModes || []); },
      error: () => {}
    });
  }

  load(): void {
    this.loading.set(true);
    this.api.summary(this.month()).subscribe({ next: r => this.summary.set(r.data), error: () => this.summary.set(null) });
    this.api.list(this.monthRange()).subscribe({
      next: r => { this.rows.set(r.data?.content || []); this.totalAmount.set(Number(r.data?.totalAmount || 0)); this.loading.set(false); },
      error: e => { this.rows.set([]); this.loading.set(false); this.notify.error(this.err(e, 'Could not load family expenses')); }
    });
  }

  setTab(t: Tab): void {
    this.tab.set(t);
    if (t === 'reports' && !this.report()) this.runReport();
    if (t === 'categories') this.loadCategories();
  }

  // ------------------------------------------------------------- editor
  openAdd(): void {
    this.editing.set(null);
    this.form.reset({ expenseDate: this.today, category: '', amount: null, paymentMode: this.modes().some(m => m.code === 'CASH') ? 'CASH' : (this.modes()[0]?.code || 'CASH'), description: '', memberName: '', referenceNo: '' });
    this.showEditor.set(true);
  }

  openEdit(e: FamilyExpense): void {
    this.editing.set(e);
    this.form.reset({ expenseDate: e.expenseDate, category: e.category, amount: e.amount, paymentMode: e.paymentMode,
      description: e.description || '', memberName: e.memberName || '', referenceNo: e.referenceNo || '' });
    this.showEditor.set(true);
  }

  save(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); this.notify.error('Fill in date, category, amount and payment mode.'); return; }
    this.saving.set(true);
    const v = this.form.getRawValue() as any;
    const body: FamilyExpense = { ...v, expenseDate: String(v.expenseDate).slice(0, 10), amount: Number(v.amount) };
    const id = this.editing()?.id;
    const call = id ? this.api.update(id, body) : this.api.create(body);
    call.subscribe({
      next: r => {
        this.saving.set(false);
        this.notify.success(id ? 'Family expense updated' : `Family expense ${r.data?.expenseNumber || ''} saved`);
        if (!id && r.data) this.editing.set(r.data);   // stays open so a bill can be attached
        else this.showEditor.set(false);
        this.load();
      },
      error: e => { this.saving.set(false); this.notify.error(this.err(e, 'Could not save')); }
    });
  }

  remove(e: FamilyExpense): void {
    if (!e.id) return;
    this.dialog.open(ConfirmationDialogComponent, {
      data: { title: 'Delete family expense', message: `Delete ${e.expenseNumber} (${e.categoryName}, ₹${e.amount})?`, confirmText: 'Delete', type: 'danger' }
    }).afterClosed().subscribe(ok => {
      if (!ok) return;
      this.api.remove(e.id!).subscribe({
        next: () => { this.notify.success('Family expense deleted'); this.load(); },
        error: err => this.notify.error(this.err(err, 'Could not delete'))
      });
    });
  }

  // ------------------------------------------------------------- reports
  runReport(): void {
    this.api.report(this.reportKey(), { from: this.reportFrom(), to: this.reportTo() }).subscribe({
      next: r => this.report.set(r.data),
      error: e => { this.report.set(null); this.notify.error(this.err(e, 'Could not run the report')); }
    });
  }

  download(key: string, format: 'xlsx' | 'pdf', filter: FamilyFilter): void {
    this.exporting.set(key + format);
    this.api.export(key, format, filter).subscribe({
      next: blob => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `family-expenses-${key}_${this.today}.${format}`;
        a.click();
        setTimeout(() => URL.revokeObjectURL(url), 1000);
        this.exporting.set(null);
      },
      error: () => { this.exporting.set(null); this.notify.error('Export failed. Try again.'); }
    });
  }
  exportList(format: 'xlsx' | 'pdf'): void { this.download('register', format, this.monthRange()); }
  exportReport(format: 'xlsx' | 'pdf'): void { this.download(this.reportKey(), format, { from: this.reportFrom(), to: this.reportTo() }); }

  cell(row: Record<string, any>, c: { key: string; type: string }): string {
    const v = row[c.key];
    if (v === null || v === undefined || v === '') return '—';
    if (c.type === 'money') return '₹' + Number(v).toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
    if (c.type === 'percent') return Number(v).toLocaleString('en-IN', { maximumFractionDigits: 1 }) + '%';
    if (c.type === 'number') return Number(v).toLocaleString('en-IN');
    if (c.type === 'date') return this.dmy(String(v));
    return String(v);
  }
  isNum(c: { type: string }): boolean { return c.type === 'money' || c.type === 'number' || c.type === 'percent'; }
  dmy(d: string): string { const [y, m, day] = String(d).slice(0, 10).split('-'); return day ? `${day}-${m}-${y}` : d; }
  inr(v: any): string { return '₹' + Number(v || 0).toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 }); }

  // ------------------------------------------------------------- categories
  loadCategories(): void {
    this.api.categories().subscribe({ next: r => this.allCategories.set(r.data || []), error: e => this.notify.error(this.err(e, 'Could not load categories')) });
  }
  private afterCategories(r: any, ok: string): void { this.allCategories.set(r.data || []); this.notify.success(ok); this.loadOptions(); }
  addCategory(): void {
    const name = this.newCategory().trim();
    if (!name) return;
    this.api.addCategory(name).subscribe({ next: r => { this.newCategory.set(''); this.afterCategories(r, `Category "${name}" added`); }, error: e => this.notify.error(this.err(e, 'Could not add')) });
  }
  renameCategory(c: FamilyOption, name: string): void {
    const n = name.trim();
    if (!c.id || !n || n === c.name) return;
    this.api.updateCategory(c.id, { name: n }).subscribe({ next: r => this.afterCategories(r, 'Category renamed'), error: e => { this.notify.error(this.err(e, 'Could not rename')); this.loadCategories(); } });
  }
  toggleCategory(c: FamilyOption): void {
    if (!c.id) return;
    const status = c.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE';
    this.api.updateCategory(c.id, { status }).subscribe({ next: r => this.afterCategories(r, status === 'ACTIVE' ? 'Category active' : 'Category inactive'), error: e => this.notify.error(this.err(e, 'Could not change')) });
  }
  deleteCategory(c: FamilyOption): void {
    if (!c.id) return;
    this.api.deleteCategory(c.id).subscribe({ next: r => this.afterCategories(r, 'Category deleted'), error: e => this.notify.error(this.err(e, 'Could not delete')) });
  }
}
