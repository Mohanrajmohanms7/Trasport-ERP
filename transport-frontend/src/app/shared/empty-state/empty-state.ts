import { Component, input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-empty-state',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="flex flex-col items-center justify-center text-center p-8 gap-4 rounded-2xl border border-dashed border-slate-700/50 bg-slate-900/20 max-w-md mx-auto">
      <div class="w-16 h-16 rounded-full bg-slate-800/50 flex items-center justify-center border border-slate-700">
        <span class="material-icons text-3xl" [ngClass]="iconColorClass()">{{ icon() }}</span>
      </div>
      <div class="flex flex-col gap-1.5">
        <h3 class="text-lg font-bold text-white">{{ title() }}</h3>
        <p class="text-slate-450 text-xs leading-relaxed">{{ message() }}</p>
      </div>
    </div>
  `
})
export class EmptyStateComponent {
  icon = input<string>('inbox');
  title = input<string>('No Data Found');
  message = input<string>('There are no items registered in this category yet.');
  type = input<string>('no-data'); // 'no-data', 'error', 'unauthorized'

  iconColorClass() {
    switch (this.type()) {
      case 'error':
        return 'text-rose-500';
      case 'unauthorized':
        return 'text-amber-500';
      case 'no-data':
      default:
        return 'text-blue-500';
    }
  }
}
