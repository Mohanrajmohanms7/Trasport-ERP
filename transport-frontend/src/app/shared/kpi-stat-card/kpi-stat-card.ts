import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-kpi-stat-card',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './kpi-stat-card.html',
  styles: []
})
export class KpiStatCardComponent {
  @Input() icon: string = 'show_chart';
  @Input() label: string = '';
  @Input() value: string | number = '';
  @Input() subLabel: string = '';
  @Input() colorClass: 'blue' | 'green' | 'amber' | 'rose' | 'indigo' = 'blue';

  get bgClass(): string {
    const maps = {
      blue: 'bg-blue-50 text-blue-600',
      green: 'bg-emerald-50 text-emerald-600',
      amber: 'bg-amber-50 text-amber-600',
      rose: 'bg-rose-50 text-rose-600',
      indigo: 'bg-indigo-50 text-indigo-600'
    };
    return maps[this.colorClass] || maps.blue;
  }
}
