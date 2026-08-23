import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FfButtonComponent } from '../../base/ff-button/ff-button.component';

@Component({
  selector: 'ff-empty-state',
  standalone: true,
  imports: [CommonModule, FfButtonComponent],
  templateUrl: './ff-empty-state.component.html',
  styleUrl: './ff-empty-state.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class FfEmptyStateComponent {
  readonly icon = input<string>('inbox');
  readonly title = input<string>('No data found');
  readonly message = input<string>('Get started by creating your first record.');
  readonly actionLabel = input<string>('');
  readonly actionIcon = input<string>('add');

  readonly action = output<void>();
}
