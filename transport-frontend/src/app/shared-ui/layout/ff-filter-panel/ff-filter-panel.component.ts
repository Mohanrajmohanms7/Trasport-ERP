import {
  ChangeDetectionStrategy,
  Component,
  effect,
  inject,
  input,
  output
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { FfButtonComponent } from '../../base/ff-button/ff-button.component';
import { FfDropdownComponent } from '../../base/ff-dropdown/ff-dropdown.component';
import { FfTextboxComponent } from '../../base/ff-textbox/ff-textbox.component';
import { FfDatepickerComponent } from '../../base/ff-datepicker/ff-datepicker.component';
import { FfSelectOption } from '../../infrastructure/models/ff-config.interface';

export interface FfFilterField {
  id: string;
  label: string;
  type: 'text' | 'select' | 'date';
  options?: { label: string; value: string }[];
}

@Component({
  selector: 'ff-filter-panel',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    FfButtonComponent,
    FfDropdownComponent,
    FfTextboxComponent,
    FfDatepickerComponent
  ],
  templateUrl: './ff-filter-panel.component.html',
  styleUrl: './ff-filter-panel.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class FfFilterPanelComponent {
  private fb = inject(FormBuilder);

  readonly fields = input<FfFilterField[]>([]);
  readonly collapsible = input<boolean>(true);
  readonly expanded = input<boolean>(true);

  readonly filterApply = output<Record<string, string>>();
  readonly filterReset = output<void>();

  form: FormGroup = this.fb.group({});

  constructor() {
    effect(() => {
      const fields = this.fields();
      const group: Record<string, FormControl> = {};
      for (const field of fields) {
        group[field.id] = new FormControl(field.type === 'select' ? null : '');
      }
      this.form = this.fb.group(group);
    });
  }

  apply(): void {
    const raw = this.form.getRawValue() as Record<string, unknown>;
    const result: Record<string, string> = {};
    Object.keys(raw).forEach(key => {
      const v = raw[key];
      result[key] = v == null ? '' : String(v);
    });
    this.filterApply.emit(result);
  }

  reset(): void {
    const patch: Record<string, unknown> = {};
    for (const field of this.fields()) {
      patch[field.id] = field.type === 'select' ? null : '';
    }
    this.form.reset(patch);
    this.filterReset.emit();
  }

  toSelectOptions(field: FfFilterField): FfSelectOption[] {
    return (field.options || []).map(o => ({ label: o.label, value: o.value }));
  }
}
