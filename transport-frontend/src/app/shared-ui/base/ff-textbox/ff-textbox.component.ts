import { ChangeDetectionStrategy, Component, forwardRef, input } from '@angular/core';
import { NG_VALIDATORS, NG_VALUE_ACCESSOR } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { FfControlBase } from '../_base/ff-control.base';

@Component({
  selector: 'ff-textbox',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './ff-textbox.component.html',
  styleUrl: './ff-textbox.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [
    { provide: NG_VALUE_ACCESSOR, useExisting: forwardRef(() => FfTextboxComponent), multi: true },
    { provide: NG_VALIDATORS, useExisting: forwardRef(() => FfTextboxComponent), multi: true }
  ]
})
export class FfTextboxComponent extends FfControlBase<string> {
  /** HTML input type (text, tel, email, etc.) */
  readonly type = input<'text' | 'tel' | 'email' | 'url' | 'search'>('text');
  /** When true, only digits (and optional spaces) are allowed — for phone fields. */
  readonly digitsOnly = input<boolean>(false);
  /** When digitsOnly, allow space characters (e.g. phone formatting). */
  readonly allowSpaces = input<boolean>(true);
  readonly maxlength = input<number | null>(null);
  readonly inputMode = input<string>('');

  onKeyDown(event: KeyboardEvent): void {
    if (!this.digitsOnly()) return;
    if (event.ctrlKey || event.metaKey || event.altKey) return;

    const allowedKeys = [
      'Backspace',
      'Delete',
      'Tab',
      'Escape',
      'Enter',
      'ArrowLeft',
      'ArrowRight',
      'Home',
      'End'
    ];
    if (allowedKeys.includes(event.key)) return;
    if (this.allowSpaces() && event.key === ' ') return;
    if (!/^\d$/.test(event.key)) {
      event.preventDefault();
    }
  }

  onPaste(event: ClipboardEvent): void {
    if (!this.digitsOnly()) return;
    event.preventDefault();
    const text = event.clipboardData?.getData('text') ?? '';
    const sanitized = this.sanitizeDigits(text);
    const el = event.target as HTMLInputElement;
    const start = el.selectionStart ?? el.value.length;
    const end = el.selectionEnd ?? el.value.length;
    let next = el.value.slice(0, start) + sanitized + el.value.slice(end);
    next = this.sanitizeDigits(next);
    const max = this.maxlength();
    if (max != null) next = next.slice(0, max);
    el.value = next;
    this.emitValue(next);
  }

  onInput(event: Event): void {
    const target = event.target as HTMLInputElement;
    let next = target.value;
    if (this.digitsOnly()) {
      next = this.sanitizeDigits(next);
      if (target.value !== next) target.value = next;
    }
    const max = this.maxlength();
    if (max != null && next.length > max) {
      next = next.slice(0, max);
      target.value = next;
    }
    this.emitValue(next);
  }

  private sanitizeDigits(raw: string): string {
    if (this.allowSpaces()) {
      return raw.replace(/[^\d\s]/g, '');
    }
    return raw.replace(/\D/g, '');
  }
}
