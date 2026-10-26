import { Component, EventEmitter, Input, OnInit, Output, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { FfNotificationService } from '../../shared-ui/infrastructure/services/ff-notification.service';

/** One row of the custom export: a COPY of the invoice's amounts (the invoice itself is never touched). */
interface Row {
  invoiceId: number;
  invoiceNumber: string;
  invoiceDate: string;
  customer: string;
  officialNet: number;
  subtotal: number | null;
  discount: number | null;
  gst: number | null;
  paid: number | null;
}

/**
 * Invoice List → Custom PDF / Custom Excel. Edits only a copy of the listed invoices' amounts for one downloaded file:
 * no invoice API is called and nothing is saved. Taxable = subtotal − discount, net = taxable + GST, balance = net − paid
 * (the server recalculates and validates the same way). The file always shows the official amounts next to the custom ones.
 */
@Component({
  selector: 'app-invoice-custom-export',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
  <div class="fixed inset-0 z-[90] flex items-center justify-center p-2 bg-slate-950/70" role="dialog" aria-label="Custom export">
    <div class="w-full max-w-6xl max-h-[92vh] flex flex-col rounded-xl bg-[var(--ff-surface-card)] border border-[var(--ff-border-default)]">
      <div class="flex items-start justify-between gap-3 p-4 border-b border-[var(--ff-border-default)]">
        <div>
          <h2 class="text-base font-bold">{{ lineInvoiceId ? 'Custom PDF — adjust qty, rate, amount, GST % per line' : 'Custom ' + (format === 'pdf' ? 'PDF' : 'Excel') + ' — adjusted amounts' }}</h2>
          <p class="text-xs text-[var(--ff-text-secondary)]">{{ rows().length }} listed invoice(s). Changes apply to this file only — invoices are not changed and nothing is saved.
            The file shows the official invoice amounts next to the adjusted ones.</p>
        </div>
        <button type="button" class="w-9 h-9 rounded-lg hover:bg-[var(--ff-surface-hover)]" (click)="closed.emit()" aria-label="Close"><span class="material-icons">close</span></button>
      </div>
      @if (lineInvoiceId) {
      <div class="overflow-auto flex-1" data-testid="custom-lines">
        @if (lineLoading()) { <p class="p-4 text-sm text-[var(--ff-text-muted)]">Loading invoice lines…</p> }
        <table class="w-full text-xs">
          <thead class="sticky top-0 bg-[var(--ff-surface-muted,#f1f5f9)] text-[var(--ff-text-secondary)]">
            <tr><th class="p-2 text-left">Trip / Material</th><th class="p-2 text-right">Official qty</th><th class="p-2 text-right">Quantity</th>
              <th class="p-2 text-right">Rate</th><th class="p-2 text-right">Official amount</th><th class="p-2 text-right">Amount</th>
              <th class="p-2 text-right">GST %</th><th class="p-2 text-right">Tax</th>
              <th class="p-2 text-right">Total</th><th class="p-2 text-right">Difference</th></tr>
          </thead>
          <tbody>
            @for (l of lines(); track l.detailId; let i = $index) {
              <tr class="border-t border-[var(--ff-border-default)] align-top" [class.bg-amber-50]="l.qty !== l.officialQty || l.amount !== l.officialAmount || l.gstPct !== l.officialGst">
                <td class="p-2"><b>{{ l.material }}</b><div class="text-[10px] text-[var(--ff-text-muted)]">{{ l.trip }} · official {{ l.officialRate | number:'1.2-2' }} / unit · GST {{ l.officialGst }}%</div></td>
                <td class="p-2 text-right">{{ l.officialQty | number:'1.0-3' }}</td>
                <td class="p-1 text-right"><input type="number" min="0" step="0.001" class="w-24 h-8 rounded border px-1.5 text-right text-xs bg-[var(--ff-surface-card)]"
                    [class.border-rose-500]="!!lineError(l)" [attr.aria-label]="'Quantity ' + l.material" [ngModel]="l.qty" (ngModelChange)="setLine(i, 'qty', $event)" /></td>
                <td class="p-1 text-right"><input type="number" min="0" step="0.01" class="w-24 h-8 rounded border px-1.5 text-right text-xs bg-[var(--ff-surface-card)]"
                    [attr.aria-label]="'Rate ' + l.material" [ngModel]="l.rate" (ngModelChange)="setLine(i, 'rate', $event)" /></td>
                <td class="p-2 text-right">{{ l.officialAmount | number:'1.2-2' }}</td>
                <td class="p-1 text-right"><input type="number" min="0" step="0.01" class="w-28 h-8 rounded border px-1.5 text-right text-xs bg-[var(--ff-surface-card)]"
                    [class.border-rose-500]="!!lineError(l)" [attr.aria-label]="'Amount ' + l.material" [ngModel]="l.amount" (ngModelChange)="setLine(i, 'amount', $event)" /></td>
                <td class="p-1 text-right"><input type="number" min="0" max="28" step="0.01" class="w-16 h-8 rounded border px-1.5 text-right text-xs bg-[var(--ff-surface-card)]"
                    [attr.aria-label]="'GST % ' + l.material" [ngModel]="l.gstPct" (ngModelChange)="setLine(i, 'gstPct', $event)" /></td>
                <td class="p-2 text-right">{{ lineTax(l) | number:'1.2-2' }}</td>
                <td class="p-2 text-right font-semibold">{{ (l.amount || 0) + lineTax(l) | number:'1.2-2' }}</td>
                <td class="p-2 text-right" [class.text-rose-600]="(l.amount || 0) + lineTax(l) !== l.officialTotal">{{ (l.amount || 0) + lineTax(l) - l.officialTotal | number:'1.2-2' }}</td>
              </tr>
              @if (lineError(l); as e) { <tr><td colspan="10" class="px-2 pb-2 text-[11px] text-rose-600" role="alert">{{ e }}</td></tr> }
            }
          </tbody>
        </table>
      </div>
      } @else {
      <div class="overflow-auto flex-1">
        <table class="w-full text-xs">
          <thead class="sticky top-0 bg-[var(--ff-surface-muted,#f1f5f9)] text-[var(--ff-text-secondary)]">
            <tr>
              <th class="p-2 text-left">Invoice</th><th class="p-2 text-left">Customer</th>
              <th class="p-2 text-right">Official net</th>
              <th class="p-2 text-right">Subtotal</th><th class="p-2 text-right">Discount</th><th class="p-2 text-right">Taxable</th>
              <th class="p-2 text-right">GST</th><th class="p-2 text-right">Net</th><th class="p-2 text-right">Paid</th>
              <th class="p-2 text-right">Balance</th><th class="p-2 text-right">Difference</th>
            </tr>
          </thead>
          <tbody>
            @for (r of rows(); track r.invoiceId; let i = $index) {
              <tr class="border-t border-[var(--ff-border-default)] align-top" [class.bg-amber-50]="changed(r)">
                <td class="p-2 font-semibold whitespace-nowrap">{{ r.invoiceNumber }}<div class="font-normal text-[10px] text-[var(--ff-text-muted)]">{{ r.invoiceDate }}</div></td>
                <td class="p-2">{{ r.customer }}</td>
                <td class="p-2 text-right whitespace-nowrap">{{ r.officialNet | number:'1.2-2' }}</td>
                @for (f of editable; track f) {
                  <td class="p-1 text-right">
                    <input type="number" min="0" step="0.01" class="w-24 h-8 rounded border px-1.5 text-right text-xs bg-[var(--ff-surface-card)]"
                           [class.border-rose-500]="!!fieldError(r, f)" [attr.aria-label]="f + ' ' + r.invoiceNumber"
                           [ngModel]="r[f]" (ngModelChange)="set(i, f, $event)" />
                  </td>
                  @if (f === 'discount') { <td class="p-2 text-right whitespace-nowrap">{{ taxable(r) | number:'1.2-2' }}</td> }
                  @if (f === 'gst') { <td class="p-2 text-right whitespace-nowrap font-semibold">{{ net(r) | number:'1.2-2' }}</td> }
                }
                <td class="p-2 text-right whitespace-nowrap">{{ net(r) - (r.paid || 0) | number:'1.2-2' }}</td>
                <td class="p-2 text-right whitespace-nowrap" [class.text-rose-600]="net(r) - r.officialNet !== 0">{{ net(r) - r.officialNet | number:'1.2-2' }}</td>
              </tr>
              @if (rowError(r); as e) { <tr><td colspan="11" class="px-2 pb-2 text-[11px] text-rose-600" role="alert">{{ r.invoiceNumber }}: {{ e }}</td></tr> }
            }
          </tbody>
          <tfoot class="border-t-2 border-[var(--ff-border-default)] font-bold">
            <tr>
              <td class="p-2" colspan="2">Total</td>
              <td class="p-2 text-right">{{ totals().officialNet | number:'1.2-2' }}</td>
              <td class="p-2 text-right">{{ totals().subtotal | number:'1.2-2' }}</td>
              <td class="p-2 text-right">{{ totals().discount | number:'1.2-2' }}</td>
              <td class="p-2 text-right">{{ totals().taxable | number:'1.2-2' }}</td>
              <td class="p-2 text-right">{{ totals().gst | number:'1.2-2' }}</td>
              <td class="p-2 text-right">{{ totals().net | number:'1.2-2' }}</td>
              <td class="p-2 text-right">{{ totals().paid | number:'1.2-2' }}</td>
              <td class="p-2 text-right">{{ totals().balance | number:'1.2-2' }}</td>
              <td class="p-2 text-right">{{ totals().net - totals().officialNet | number:'1.2-2' }}</td>
            </tr>
          </tfoot>
        </table>
      </div>
      }
      <div class="flex flex-wrap items-center justify-end gap-2 p-4 border-t border-[var(--ff-border-default)]">
        @if (lineInvoiceId ? linesInvalid() : hasErrors()) { <span class="text-xs text-rose-600 mr-auto">Correct the highlighted amounts to download.</span> }
        <button type="button" class="h-10 px-4 rounded-lg border border-[var(--ff-border-default)] text-sm font-semibold" (click)="lineInvoiceId ? resetLines() : reset()">Reset to official</button>
        <button type="button" class="h-10 px-4 rounded-lg border border-[var(--ff-border-default)] text-sm font-semibold" (click)="closed.emit()">Cancel</button>
        <button type="button" class="h-10 px-4 rounded-lg bg-[var(--ff-color-primary-600)] text-white text-sm font-semibold disabled:opacity-50"
                [disabled]="busy() || (lineInvoiceId ? (linesInvalid() || !lines().length) : (hasErrors() || !rows().length))" (click)="lineInvoiceId ? downloadLines() : download()">{{ busy() ? 'Preparing…' : 'Download ' + (format === 'pdf' ? 'PDF' : 'Excel') }}</button>
      </div>
    </div>
  </div>
  `
})
export class InvoiceCustomExportComponent implements OnInit {
  private http = inject(HttpClient);
  private notify = inject(FfNotificationService);

  /** The invoices currently listed (copied; never modified). */
  @Input() invoices: any[] = [];
  @Input() format: 'pdf' | 'xlsx' = 'xlsx';
  @Output() closed = new EventEmitter<void>();
  /** Row action: one invoice, line by line (only Quantity and Amount editable). */
  @Input() lineInvoiceId: number | null = null;

  lines = signal<LineRow[]>([]);
  private officialLines: LineRow[] = [];
  lineLoading = signal(false);
  private interState = false;

  readonly editable: ('subtotal' | 'discount' | 'gst' | 'paid')[] = ['subtotal', 'discount', 'gst', 'paid'];
  rows = signal<Row[]>([]);
  private official: Row[] = [];
  busy = signal(false);

  ngOnInit(): void {
    if (this.lineInvoiceId) { this.loadLines(this.lineInvoiceId); return; }
    this.official = (this.invoices || []).filter(i => i?.id != null).map(i => ({
      invoiceId: Number(i.id),
      invoiceNumber: i.invoiceNumber || '',
      invoiceDate: i.invoiceDate || '',
      customer: i.customer?.name || i.customerName || '',
      officialNet: round2(i.netAmount),
      subtotal: round2(i.subtotal),
      discount: round2(i.discount),
      gst: round2(i.taxAmount),
      paid: round2(i.paidAmount)
    }));
    this.reset();
  }

  reset(): void { this.rows.set(this.official.map(r => ({ ...r }))); }

  // ----- line mode: a copy of the invoice's lines; nothing is saved -----
  private loadLines(id: number): void {
    this.lineLoading.set(true);
    this.http.get<any>(`/api/v1/invoices/${id}`).subscribe({
      next: r => {
        const inv = r?.data || {};
        this.interState = String(inv.supplyType || '').toUpperCase() === 'INTER_STATE';
        this.officialLines = (inv.details || []).filter((d: any) => !d.isDeleted).map((d: any) => ({
          detailId: Number(d.id), material: d.material?.name || '', trip: d.trip?.tripNumber || '',
          gstPct: Number(d.gstPercentage || 0),
          officialQty: round3(d.quantity), officialAmount: round2(d.taxableAmount), officialGst: Number(d.gstPercentage || 0),
          officialRate: Number(d.quantity) ? round2(Number(d.taxableAmount || 0) / Number(d.quantity)) : 0,
          rate: Number(d.quantity) ? round2(Number(d.taxableAmount || 0) / Number(d.quantity)) : 0,
          officialTotal: round2(Number(d.taxableAmount || 0) + Number(d.cgst || 0) + Number(d.sgst || 0) + Number(d.igst || 0)),
          qty: round3(d.quantity), amount: round2(d.taxableAmount)
        }));
        this.resetLines();
        this.lineLoading.set(false);
      },
      error: () => { this.lineLoading.set(false); this.notify.error('Could not load the invoice lines'); }
    });
  }
  resetLines(): void { this.lines.set(this.officialLines.map(l => ({ ...l }))); }
  /** Qty or Rate → Amount = Qty × Rate; Amount → Rate = Amount ÷ Qty; GST % → Tax (2-decimal rounding as the invoice). */
  setLine(i: number, f: 'qty' | 'amount' | 'rate' | 'gstPct', v: any): void {
    const n = v === '' || v === null || v === undefined ? null : Number(v);
    this.lines.update(list => list.map((l, k) => {
      if (k !== i) return l;
      const x: LineRow = { ...l, [f]: n } as LineRow;
      if (f === 'qty' || f === 'rate') x.amount = x.qty !== null && x.rate !== null ? round2(x.qty * x.rate) : null;
      if (f === 'amount') x.rate = x.qty && x.amount !== null ? round2(x.amount / x.qty) : x.rate;
      return x;
    }));
  }
  /** Same GST rule as the invoice: IGST = rate; otherwise CGST + SGST = half the rate each. */
  lineTax(l: LineRow): number {
    const a = l.amount || 0;
    const g = l.gstPct || 0;
    return this.interState ? round2(a * g / 100) : round2(round2(a * g / 200) * 2);
  }
  lineError(l: LineRow): string | null {
    if (l.qty === null || Number.isNaN(l.qty)) return 'Enter a quantity';
    if (l.amount === null || Number.isNaN(l.amount)) return 'Enter an amount';
    if (l.qty < 0 || l.amount < 0 || (l.rate ?? 0) < 0) return 'Cannot be negative';
    if (l.gstPct === null || Number.isNaN(l.gstPct) || l.gstPct < 0 || l.gstPct > 28) return 'GST % must be between 0 and 28';
    if (Math.round(l.qty * 1000) !== Number((l.qty * 1000).toFixed(6))) return 'Quantity: at most 3 decimals';
    if (Math.round(l.amount * 100) !== Number((l.amount * 100).toFixed(6))) return 'Amount: at most 2 decimals';
    return null;
  }
  linesInvalid = computed(() => this.lines().some(l => !!this.lineError(l)));

  downloadLines(): void {
    if (this.busy() || this.linesInvalid() || !this.lineInvoiceId) return;
    this.busy.set(true);
    const body = this.lines().map(l => ({ detailId: l.detailId, quantity: l.qty, amount: l.amount, rate: l.rate, gstPercentage: l.gstPct }));
    this.http.post(`/api/v1/exports/invoices/${this.lineInvoiceId}/custom-lines`, body, { responseType: 'blob' }).subscribe({
      next: blob => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url; a.download = `invoice_custom_${new Date().toISOString().slice(0, 10)}.pdf`; a.click();
        setTimeout(() => URL.revokeObjectURL(url), 1000);
        this.busy.set(false);
        this.notify.success('Custom PDF downloaded — the invoice was not changed');
      },
      error: async e => {
        this.busy.set(false);
        let msg = e?.status === 403 ? 'Your role cannot export invoices.' : 'Custom PDF failed. Try again.';
        try { const j = JSON.parse(await (e?.error as Blob).text()); msg = j?.errors?.[0] || j?.message || msg; } catch { /* keep default */ }
        this.notify.error(msg);
      }
    });
  }

  set(index: number, field: 'subtotal' | 'discount' | 'gst' | 'paid', value: any): void {
    const v = value === '' || value === null || value === undefined ? null : Number(value);
    this.rows.update(list => list.map((r, i) => i === index ? { ...r, [field]: v } : r));
  }

  taxable(r: Row): number { return round2((r.subtotal || 0) - (r.discount || 0)); }
  net(r: Row): number { return round2(this.taxable(r) + (r.gst || 0)); }
  changed(r: Row): boolean {
    const o = this.official.find(x => x.invoiceId === r.invoiceId);
    return !!o && (o.subtotal !== r.subtotal || o.discount !== r.discount || o.gst !== r.gst || o.paid !== r.paid);
  }

  fieldError(r: Row, f: 'subtotal' | 'discount' | 'gst' | 'paid'): string | null {
    const v = r[f];
    if (v === null || Number.isNaN(v)) return 'Enter an amount';
    if (v < 0) return 'Cannot be negative';
    if (Math.round(v * 100) !== Number((v * 100).toFixed(6))) return 'At most 2 decimals';
    if (v > 999999999999.99) return 'Too large';
    return null;
  }
  rowError(r: Row): string | null {
    for (const f of this.editable) { const e = this.fieldError(r, f); if (e) return f[0].toUpperCase() + f.slice(1) + ': ' + e; }
    if ((r.discount || 0) > (r.subtotal || 0)) return 'Discount cannot be more than the subtotal';
    if ((r.paid || 0) > this.net(r)) return 'Paid cannot be more than the net amount';
    return null;
  }
  hasErrors = computed(() => this.rows().some(r => !!this.rowError(r)));

  totals = computed(() => this.rows().reduce((t, r) => ({
    officialNet: round2(t.officialNet + r.officialNet),
    subtotal: round2(t.subtotal + (r.subtotal || 0)),
    discount: round2(t.discount + (r.discount || 0)),
    taxable: round2(t.taxable + this.taxable(r)),
    gst: round2(t.gst + (r.gst || 0)),
    net: round2(t.net + this.net(r)),
    paid: round2(t.paid + (r.paid || 0)),
    balance: round2(t.balance + this.net(r) - (r.paid || 0))
  }), { officialNet: 0, subtotal: 0, discount: 0, taxable: 0, gst: 0, net: 0, paid: 0, balance: 0 }));

  /** Builds the file on the server from the custom amounts (read-only endpoint; no invoice is changed). */
  download(): void {
    if (this.busy() || this.hasErrors()) return;
    this.busy.set(true);
    const body = this.rows().map(r => ({ invoiceId: r.invoiceId, subtotal: r.subtotal, discount: r.discount, gst: r.gst, paid: r.paid }));
    this.http.post(`/api/v1/exports/invoices/custom`, body, { params: { format: this.format }, responseType: 'blob' }).subscribe({
      next: blob => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `invoices_custom_${new Date().toISOString().slice(0, 10)}.${this.format}`;
        a.click();
        setTimeout(() => URL.revokeObjectURL(url), 1000);
        this.busy.set(false);
        this.notify.success('Custom export downloaded — invoices were not changed');
      },
      error: async e => {
        this.busy.set(false);
        let msg = e?.status === 403 ? 'Your role cannot export invoices.' : 'Custom export failed. Try again.';
        try { const j = JSON.parse(await (e?.error as Blob).text()); msg = j?.errors?.[0] || j?.message || msg; } catch { /* keep default */ }
        this.notify.error(msg);
      }
    });
  }
}

interface LineRow {
  detailId: number; material: string; trip: string; gstPct: number | null; officialGst: number;
  officialQty: number; officialAmount: number; officialTotal: number; officialRate: number;
  qty: number | null; amount: number | null; rate: number | null;
}

function round3(v: any): number {
  const n = Number(v || 0);
  return Math.round((n + Number.EPSILON) * 1000) / 1000;
}

function round2(v: any): number {
  const n = Number(v || 0);
  return Math.round((n + Number.EPSILON) * 100) / 100;
}
