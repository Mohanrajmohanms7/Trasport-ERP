import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { CommonModule } from '@angular/common';

export interface FfStepItem {
  id: string;
  label: string;
  optional?: boolean;
}

@Component({
  selector: 'ff-stepper',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './ff-stepper.component.html',
  styleUrl: './ff-stepper.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class FfStepperComponent {
  readonly steps = input.required<FfStepItem[]>();
  readonly activeStep = input<number>(0);
  readonly linear = input<boolean>(false);

  readonly stepChange = output<number>();

  readonly progress = computed(() => {
    const total = this.steps().length;
    if (total <= 1) return 100;
    return Math.round((this.activeStep() / (total - 1)) * 100);
  });

  goTo(index: number): void {
    if (this.linear() && index > this.activeStep() + 1) return;
    if (index < 0 || index >= this.steps().length) return;
    this.stepChange.emit(index);
  }

  stepState(index: number): 'done' | 'active' | 'pending' {
    if (index < this.activeStep()) return 'done';
    if (index === this.activeStep()) return 'active';
    return 'pending';
  }
}
