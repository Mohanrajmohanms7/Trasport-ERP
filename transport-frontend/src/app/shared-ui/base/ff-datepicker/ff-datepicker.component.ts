import { ChangeDetectionStrategy, Component, forwardRef, input } from '@angular/core';
import { NG_VALIDATORS, NG_VALUE_ACCESSOR } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { FfControlBase } from '../_base/ff-control.base';

@Component({
  selector: 'ff-datepicker',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './ff-datepicker.component.html',
  styleUrl: './ff-datepicker.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [
    { provide: NG_VALUE_ACCESSOR, useExisting: forwardRef(() => FfDatepickerComponent), multi: true },
    { provide: NG_VALIDATORS, useExisting: forwardRef(() => FfDatepickerComponent), multi: true }
  ]
})
export class FfDatepickerComponent extends FfControlBase<string> {
  readonly min = input<string>('');
  readonly max = input<string>('');

  onInput(event: Event): void {
    this.emitValue((event.target as HTMLInputElement).value);
  }

  onPickerClick(event: MouseEvent): void {
    const target = event.target as HTMLInputElement;
    if (this.isDisabled() || this.isReadonly()) return;
    try {
      if (typeof target.showPicker === 'function') {
        target.showPicker();
      }
    } catch (e) {
      // Ignore or log fallback
    }
  }
}
