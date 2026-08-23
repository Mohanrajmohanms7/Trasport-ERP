import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FfPageAction } from '../../infrastructure/models/ff-config.interface';
import { FfButtonComponent } from '../../base/ff-button/ff-button.component';

@Component({
  selector: 'ff-toolbar',
  standalone: true,
  imports: [CommonModule, FfButtonComponent],
  templateUrl: './ff-toolbar.component.html',
  styleUrl: './ff-toolbar.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class FfToolbarComponent {
  readonly title = input<string>('');
  readonly actions = input<FfPageAction[]>([]);
}
