import { ChangeDetectionStrategy, Component, forwardRef, input } from '@angular/core';
import { NG_VALIDATORS, NG_VALUE_ACCESSOR } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { FfControlBase } from '../_base/ff-control.base';

@Component({
  selector: 'ff-textarea',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './ff-textarea.component.html',
  styleUrl: './ff-textarea.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [
    { provide: NG_VALUE_ACCESSOR, useExisting: forwardRef(() => FfTextareaComponent), multi: true },
    { provide: NG_VALIDATORS, useExisting: forwardRef(() => FfTextareaComponent), multi: true }
  ]
})
export class FfTextareaComponent extends FfControlBase<string> {
  readonly rows = input<number>(4);
  readonly maxLength = input<number | null>(null);
  readonly autoResize = input<boolean>(false);

  onInput(event: Event): void {
    const target = event.target as HTMLTextAreaElement;
    this.emitValue(target.value);
    if (this.autoResize()) {
      target.style.height = 'auto';
      target.style.height = `${target.scrollHeight}px`;
    }
  }
}
