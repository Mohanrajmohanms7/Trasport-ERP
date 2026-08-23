import { ChangeDetectionStrategy, Component, forwardRef, signal } from '@angular/core';
import { NG_VALIDATORS, NG_VALUE_ACCESSOR } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { FfControlBase } from '../_base/ff-control.base';

@Component({
  selector: 'ff-password',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './ff-password.component.html',
  styleUrl: './ff-password.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [
    { provide: NG_VALUE_ACCESSOR, useExisting: forwardRef(() => FfPasswordComponent), multi: true },
    { provide: NG_VALIDATORS, useExisting: forwardRef(() => FfPasswordComponent), multi: true }
  ]
})
export class FfPasswordComponent extends FfControlBase<string> {
  readonly showToggle = signal(false);

  onInput(event: Event): void {
    this.emitValue((event.target as HTMLInputElement).value);
  }

  toggleVisibility(): void {
    if (this.isDisabled() || this.isReadonly()) return;
    this.showToggle.update(v => !v);
  }
}
