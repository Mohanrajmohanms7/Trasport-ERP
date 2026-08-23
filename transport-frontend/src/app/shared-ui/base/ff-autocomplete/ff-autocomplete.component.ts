import {
  ChangeDetectionStrategy,
  Component,
  computed,
  forwardRef,
  input,
  signal
} from '@angular/core';
import { NG_VALIDATORS, NG_VALUE_ACCESSOR } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { FfControlBase } from '../_base/ff-control.base';
import { FfSelectOption } from '../../infrastructure/models/ff-config.interface';

@Component({
  selector: 'ff-autocomplete',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './ff-autocomplete.component.html',
  styleUrl: './ff-autocomplete.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [
    { provide: NG_VALUE_ACCESSOR, useExisting: forwardRef(() => FfAutocompleteComponent), multi: true },
    { provide: NG_VALIDATORS, useExisting: forwardRef(() => FfAutocompleteComponent), multi: true }
  ]
})
export class FfAutocompleteComponent extends FfControlBase<unknown> {
  readonly options = input<FfSelectOption[]>([]);
  readonly displayWith = input<(value: unknown) => string>((v) => {
    const opt = this.options().find(o => o.value === v);
    return opt?.label ?? (v != null ? String(v) : '');
  });

  readonly query = signal('');
  readonly panelOpen = signal(false);
  readonly activeIndex = signal(-1);

  readonly filteredOptions = computed(() => {
    const q = this.query().trim().toLowerCase();
    const opts = this.options();
    if (!q) return opts;
    return opts.filter(o => o.label.toLowerCase().includes(q));
  });

  readonly inputDisplay = computed(() => {
    if (this.panelOpen()) return this.query();
    const v = this.value();
    if (v === null || v === undefined || v === '') return this.query();
    return this.displayWith()(v);
  });

  override writeValue(value: unknown): void {
    super.writeValue(value);
    if (value !== null && value !== undefined && value !== '') {
      this.query.set(this.displayWith()(value));
    } else {
      this.query.set('');
    }
  }

  onInput(event: Event): void {
    const text = (event.target as HTMLInputElement).value;
    this.query.set(text);
    this.panelOpen.set(true);
    this.activeIndex.set(0);
    if (!text) {
      this.emitValue(null);
    }
  }

  selectOption(opt: FfSelectOption): void {
    if (opt.disabled) return;
    this.emitValue(opt.value);
    this.query.set(opt.label);
    this.panelOpen.set(false);
    this.markTouched();
  }

  onFocusInput(): void {
    this.onFocus();
    if (!this.isDisabled() && !this.isReadonly()) {
      this.panelOpen.set(true);
    }
  }

  onBlurInput(): void {
    setTimeout(() => {
      this.panelOpen.set(false);
      this.onBlur();
    }, 150);
  }

  onKeydown(event: KeyboardEvent): void {
    const opts = this.filteredOptions();
    if (!this.panelOpen() || !opts.length) return;

    if (event.key === 'ArrowDown') {
      event.preventDefault();
      this.activeIndex.update(i => Math.min(i + 1, opts.length - 1));
    } else if (event.key === 'ArrowUp') {
      event.preventDefault();
      this.activeIndex.update(i => Math.max(i - 1, 0));
    } else if (event.key === 'Enter') {
      event.preventDefault();
      const idx = this.activeIndex();
      if (idx >= 0 && opts[idx]) {
        this.selectOption(opts[idx]);
      }
    } else if (event.key === 'Escape') {
      this.panelOpen.set(false);
    }
  }

  clear(): void {
    this.emitValue(null);
    this.query.set('');
    this.panelOpen.set(false);
  }
}
