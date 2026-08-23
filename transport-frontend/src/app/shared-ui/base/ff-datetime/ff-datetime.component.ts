import { ChangeDetectionStrategy, Component, forwardRef, input } from '@angular/core';
import { NG_VALIDATORS, NG_VALUE_ACCESSOR } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { FfControlBase } from '../_base/ff-control.base';

@Component({
  selector: 'ff-datetime',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './ff-datetime.component.html',
  styleUrl: './ff-datetime.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [
    { provide: NG_VALUE_ACCESSOR, useExisting: forwardRef(() => FfDatetimeComponent), multi: true },
    { provide: NG_VALIDATORS, useExisting: forwardRef(() => FfDatetimeComponent), multi: true }
  ]
})
export class FfDatetimeComponent extends FfControlBase<string> {
  readonly min = input<string>('');
  readonly max = input<string>('');

  onInput(event: Event): void {
    this.emitValue((event.target as HTMLInputElement).value);
  }
}
