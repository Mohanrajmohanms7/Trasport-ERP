import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FfBreadcrumbItem } from '../../infrastructure/models/ff-config.interface';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'ff-breadcrumb',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './ff-breadcrumb.component.html',
  styleUrl: './ff-breadcrumb.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class FfBreadcrumbComponent {
  readonly items = input.required<FfBreadcrumbItem[]>();
}
