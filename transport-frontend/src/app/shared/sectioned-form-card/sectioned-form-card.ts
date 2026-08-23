import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-sectioned-form-card',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './sectioned-form-card.html',
  styles: []
})
export class SectionedFormCardComponent {
  @Input() title: string = '';
  @Input() icon: string = 'info';
  @Input() colorClass: 'blue' | 'green' | 'amber' | 'rose' | 'indigo' = 'blue';

  get iconColorClass(): string {
    const maps = {
      blue: 'text-blue-600',
      green: 'text-emerald-600',
      amber: 'text-amber-600',
      rose: 'text-rose-600',
      indigo: 'text-indigo-600'
    };
    return maps[this.colorClass] || maps.blue;
  }
}
