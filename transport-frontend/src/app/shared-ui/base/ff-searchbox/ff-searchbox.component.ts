import { ChangeDetectionStrategy, Component, forwardRef, output } from '@angular/core';
import { NG_VALIDATORS, NG_VALUE_ACCESSOR } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { FfControlBase } from '../_base/ff-control.base';

@Component({
  selector: 'ff-searchbox',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './ff-searchbox.component.html',
  styleUrl: './ff-searchbox.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [
    { provide: NG_VALUE_ACCESSOR, useExisting: forwardRef(() => FfSearchboxComponent), multi: true },
    { provide: NG_VALIDATORS, useExisting: forwardRef(() => FfSearchboxComponent), multi: true }
  ]
})
export class FfSearchboxComponent extends FfControlBase<string> {
  readonly ffSearch = output<string>();

  onInput(event: Event): void {
    const value = (event.target as HTMLInputElement).value;
    this.emitValue(value);
  }

  onKeydown(event: KeyboardEvent): void {
    if (event.key === 'Enter') {
      event.preventDefault();
      this.ffSearch.emit(this.value() ?? '');
    }
  }

  clear(): void {
    if (this.isDisabled() || this.isReadonly()) return;
    this.emitValue('');
    this.ffSearch.emit('');
  }

  search(): void {
    if (this.isDisabled()) return;
    this.ffSearch.emit(this.value() ?? '');
  }
}
