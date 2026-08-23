import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'ff-card',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './ff-card.component.html',
  styleUrl: './ff-card.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class FfCardComponent {
  readonly title = input<string>('');
  readonly subtitle = input<string>('');
  readonly padding = input<'none' | 'sm' | 'md' | 'lg'>('md');
  readonly hoverable = input<boolean>(false);
}
