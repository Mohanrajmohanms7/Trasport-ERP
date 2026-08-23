import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'ff-sidebar',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './ff-sidebar.component.html',
  styleUrl: './ff-sidebar.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class FfSidebarComponent {
  readonly open = input<boolean>(false);
  readonly title = input<string>('');
  readonly width = input<string>('420px');
  readonly position = input<'left' | 'right'>('right');

  readonly closed = output<void>();

  onBackdropClick(): void {
    this.closed.emit();
  }

  onCloseClick(): void {
    this.closed.emit();
  }
}
