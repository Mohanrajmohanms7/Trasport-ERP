import { ChangeDetectionStrategy, Component, forwardRef, input } from '@angular/core';
import { NG_VALIDATORS, NG_VALUE_ACCESSOR } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { FfControlBase } from '../_base/ff-control.base';

@Component({
  selector: 'ff-number',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './ff-number.component.html',
  styleUrl: './ff-number.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [
    { provide: NG_VALUE_ACCESSOR, useExisting: forwardRef(() => FfNumberComponent), multi: true },
    { provide: NG_VALIDATORS, useExisting: forwardRef(() => FfNumberComponent), multi: true }
  ]
})
export class FfNumberComponent extends FfControlBase<number | null> {
  readonly min = input<number | null>(null);
  readonly max = input<number | null>(null);
  readonly step = input<number | null>(null);
  /** When false, blocks decimal point (integers only). */
  readonly allowDecimal = input<boolean>(true);
  /** When true, allows a leading minus sign. */
  readonly allowNegative = input<boolean>(false);

  onKeyDown(event: KeyboardEvent): void {
    if (event.ctrlKey || event.metaKey || event.altKey) return;

    const allowedKeys = [
      'Backspace',
      'Delete',
      'Tab',
      'Escape',
      'Enter',
      'ArrowLeft',
      'ArrowRight',
      'ArrowUp',
      'ArrowDown',
      'Home',
      'End'
    ];
    if (allowedKeys.includes(event.key)) return;

    if (event.key === '.' || event.key === ',') {
      if (!this.allowDecimal()) {
        event.preventDefault();
        return;
      }
      const value = (event.target as HTMLInputElement).value;
      if (value.includes('.') || value.includes(',')) {
        event.preventDefault();
      }
      return;
    }

    if (event.key === '-') {
      if (!this.allowNegative()) {
        event.preventDefault();
        return;
      }
      const el = event.target as HTMLInputElement;
      if (el.selectionStart !== 0 || el.value.includes('-')) {
        event.preventDefault();
      }
      return;
    }

    // Block e/E/+ and any non-digit
    if (!/^\d$/.test(event.key)) {
      event.preventDefault();
    }
  }

  onPaste(event: ClipboardEvent): void {
    event.preventDefault();
    const text = event.clipboardData?.getData('text') ?? '';
    const sanitized = this.sanitize(text);
    const el = event.target as HTMLInputElement;
    const start = el.selectionStart ?? el.value.length;
    const end = el.selectionEnd ?? el.value.length;
    const next = el.value.slice(0, start) + sanitized + el.value.slice(end);
    const finalValue = this.sanitize(next);
    el.value = finalValue;
    this.emitFromRaw(finalValue);
  }

  onInput(event: Event): void {
    const el = event.target as HTMLInputElement;
    const sanitized = this.sanitize(el.value);
    if (el.value !== sanitized) {
      el.value = sanitized;
    }
    this.emitFromRaw(sanitized);
  }

  private emitFromRaw(raw: string): void {
    if (raw === '' || raw === '-' || raw === '.' || raw === '-.') {
      this.emitValue(null);
      return;
    }
    const num = Number(raw.replace(',', '.'));
    this.emitValue(isNaN(num) ? null : num);
  }

  private sanitize(raw: string): string {
    let out = '';
    let hasDot = false;
    for (let i = 0; i < raw.length; i++) {
      const ch = raw[i];
      if (ch >= '0' && ch <= '9') {
        out += ch;
        continue;
      }
      if ((ch === '.' || ch === ',') && this.allowDecimal() && !hasDot) {
        out += '.';
        hasDot = true;
        continue;
      }
      if (ch === '-' && this.allowNegative() && out.length === 0) {
        out += '-';
      }
    }
    return out;
  }
}
