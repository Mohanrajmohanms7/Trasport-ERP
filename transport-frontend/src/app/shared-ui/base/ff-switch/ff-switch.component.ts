import { ChangeDetectionStrategy, Component, forwardRef } from '@angular/core';
import { NG_VALIDATORS, NG_VALUE_ACCESSOR } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { FfControlBase } from '../_base/ff-control.base';

@Component({
  selector: 'ff-switch',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './ff-switch.component.html',
  styleUrl: './ff-switch.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [
    { provide: NG_VALUE_ACCESSOR, useExisting: forwardRef(() => FfSwitchComponent), multi: true },
    { provide: NG_VALIDATORS, useExisting: forwardRef(() => FfSwitchComponent), multi: true }
  ]
})
export class FfSwitchComponent extends FfControlBase<boolean> {
  override writeValue(value: boolean | null): void {
    this.value.set(!!value);
  }

  onSwitchChange(event: Event): void {
    if (this.isDisabled() || this.isReadonly()) return;
    const checked = (event.target as HTMLInputElement).checked;
    this.emitValue(checked);
    this.markTouched();
  }

  onKeydown(event: KeyboardEvent): void {
    if (event.key === ' ') {
      event.preventDefault();
      this.emitValue(!this.value());
      this.markTouched();
    }
  }
}
