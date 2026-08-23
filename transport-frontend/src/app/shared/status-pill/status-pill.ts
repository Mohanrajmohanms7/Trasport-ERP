import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-status-pill',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './status-pill.html',
  styles: []
})
export class StatusPillComponent {
  @Input() status: string = '';

  get classes(): { container: string; dot: string } {
    const s = (this.status || '').toLowerCase().trim();
    if (s === 'active' || s === 'completed' || s === 'approved' || s === 'success') {
      return {
        container: 'bg-emerald-50 text-emerald-700 border-emerald-100',
        dot: 'bg-emerald-500'
      };
    }
    if (s === 'on trip' || s === 'on_trip' || s === 'dispatched' || s === 'in progress' || s === 'posted') {
      return {
        container: 'bg-blue-50 text-blue-700 border-blue-100',
        dot: 'bg-blue-500'
      };
    }
    if (s === 'on leave' || s === 'on_leave' || s === 'pending' || s === 'draft') {
      return {
        container: 'bg-amber-50 text-amber-700 border-amber-100',
        dot: 'bg-amber-500'
      };
    }
    if (s === 'inactive' || s === 'cancelled' || s === 'rejected' || s === 'failed') {
      return {
        container: 'bg-rose-50 text-rose-700 border-rose-100',
        dot: 'bg-rose-500'
      };
    }
    return {
      container: 'bg-slate-50 text-slate-700 border-slate-100',
      dot: 'bg-slate-500'
    };
  }
}
