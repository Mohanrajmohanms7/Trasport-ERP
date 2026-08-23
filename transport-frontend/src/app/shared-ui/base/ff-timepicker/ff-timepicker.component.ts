import { ChangeDetectionStrategy, Component, forwardRef, input } from '@angular/core';
import { NG_VALIDATORS, NG_VALUE_ACCESSOR } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { FfControlBase } from '../_base/ff-control.base';

@Component({
  selector: 'ff-timepicker',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './ff-timepicker.component.html',
  styleUrl: './ff-timepicker.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [
    { provide: NG_VALUE_ACCESSOR, useExisting: forwardRef(() => FfTimepickerComponent), multi: true },
    { provide: NG_VALIDATORS, useExisting: forwardRef(() => FfTimepickerComponent), multi: true }
  ]
})
export class FfTimepickerComponent extends FfControlBase<string> {
  readonly min = input<string>('');
  readonly max = input<string>('');
  readonly step = input<number | null>(null);

  onInput(event: Event): void {
    this.emitValue((event.target as HTMLInputElement).value);
  }
}
