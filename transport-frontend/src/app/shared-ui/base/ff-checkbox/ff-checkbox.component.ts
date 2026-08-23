import { ChangeDetectionStrategy, Component, forwardRef } from '@angular/core';
import { AbstractControl, NG_VALIDATORS, NG_VALUE_ACCESSOR, ValidationErrors } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { FfControlBase } from '../_base/ff-control.base';

@Component({
  selector: 'ff-checkbox',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './ff-checkbox.component.html',
  styleUrl: './ff-checkbox.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [
    { provide: NG_VALUE_ACCESSOR, useExisting: forwardRef(() => FfCheckboxComponent), multi: true },
    { provide: NG_VALIDATORS, useExisting: forwardRef(() => FfCheckboxComponent), multi: true }
  ]
})
export class FfCheckboxComponent extends FfControlBase<boolean> {
  override writeValue(value: boolean | null): void {
    this.value.set(!!value);
  }

  override validate(_control: AbstractControl): ValidationErrors | null {
    if (this.isRequired() && !this.value()) {
      return { required: true };
    }
    return null;
  }

  onToggle(event: Event): void {
    if (this.isDisabled() || this.isReadonly()) return;
    const checked = (event.target as HTMLInputElement).checked;
    this.emitValue(checked);
    this.markTouched();
  }
}
