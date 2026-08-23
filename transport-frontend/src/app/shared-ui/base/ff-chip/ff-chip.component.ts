import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { CommonModule } from '@angular/common';

export type FfChipVariant = 'filled' | 'outline';

@Component({
  selector: 'ff-chip',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './ff-chip.component.html',
  styleUrl: './ff-chip.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class FfChipComponent {
  readonly label = input.required<string>();
  readonly icon = input<string>('');
  readonly variant = input<FfChipVariant>('filled');
  readonly removable = input<boolean>(false);
  readonly disabled = input<boolean>(false);

  readonly ffRemove = output<void>();

  readonly classes = computed(() =>
    [
      'ff-chip',
      `ff-chip--${this.variant()}`,
      this.disabled() ? 'ff-chip--disabled' : ''
    ].filter(Boolean).join(' ')
  );

  remove(event: Event): void {
    event.stopPropagation();
    if (this.disabled()) return;
    this.ffRemove.emit();
  }
}
