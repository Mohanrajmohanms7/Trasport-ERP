import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FfBreadcrumbItem, FfPageAction } from '../../infrastructure/models/ff-config.interface';
import { FfButtonComponent } from '../../base/ff-button/ff-button.component';

@Component({
  selector: 'ff-page-header',
  standalone: true,
  imports: [CommonModule, RouterModule, FfButtonComponent],
  templateUrl: './ff-page-header.component.html',
  styleUrl: './ff-page-header.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class FfPageHeaderComponent {
  readonly title = input.required<string>();
  readonly subtitle = input<string>('');
  readonly breadcrumbs = input<FfBreadcrumbItem[]>([]);
  readonly actions = input<FfPageAction[]>([]);

  readonly actionClick = output<FfPageAction>();

  onAction(action: FfPageAction): void {
    if (action.disabled) return;
    this.actionClick.emit(action);
  }
}
