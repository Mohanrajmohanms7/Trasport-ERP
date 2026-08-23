import { ChangeDetectionStrategy, Component, forwardRef, input } from '@angular/core';
import { NG_VALIDATORS, NG_VALUE_ACCESSOR } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { FfControlBase } from '../_base/ff-control.base';
import { FfSelectOption } from '../../infrastructure/models/ff-config.interface';

@Component({
  selector: 'ff-radio',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './ff-radio.component.html',
  styleUrl: './ff-radio.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [
    { provide: NG_VALUE_ACCESSOR, useExisting: forwardRef(() => FfRadioComponent), multi: true },
    { provide: NG_VALIDATORS, useExisting: forwardRef(() => FfRadioComponent), multi: true }
  ]
})
export class FfRadioComponent extends FfControlBase<unknown> {
  readonly options = input<FfSelectOption[]>([]);
  readonly layout = input<'vertical' | 'horizontal'>('vertical');

  select(value: unknown): void {
    if (this.isDisabled() || this.isReadonly()) return;
    this.emitValue(value);
    this.markTouched();
  }

  isSelected(optionValue: unknown): boolean {
    const current = this.value();
    return current === optionValue;
  }
}
