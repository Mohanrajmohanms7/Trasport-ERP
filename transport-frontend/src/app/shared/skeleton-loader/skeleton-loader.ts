import { Component, input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-skeleton-loader',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="flex flex-col gap-3 w-full" [ngClass]="className()">
      @for (row of [].constructor(rows()); track $index) {
        <div class="flex gap-4 w-full">
          @for (col of [].constructor(cols()); track $index) {
            <div class="h-6 bg-slate-800 rounded animate-skeleton flex-1"></div>
          }
        </div>
      }
    </div>
  `
})
export class SkeletonLoaderComponent {
  rows = input<number>(3);
  cols = input<number>(4);
  className = input<string>('');
}
