import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { EmptyStateComponent } from '../../shared/empty-state/empty-state';

@Component({
  selector: 'app-access-denied',
  standalone: true,
  imports: [CommonModule, RouterModule, MatButtonModule, EmptyStateComponent],
  template: `
    <div class="min-h-screen bg-slate-950 flex flex-col items-center justify-center p-6 text-center">
      <app-empty-state
        icon="gpp_bad"
        title="403 - Permission Denied"
        message="You do not have the administrative roles or permissions required to view this console screen."
        type="unauthorized">
      </app-empty-state>
      <button mat-flat-button color="primary" routerLink="/masters" class="mt-6">
        Return to Master Data Management
      </button>
    </div>
  `
})
export class AccessDeniedComponent {}
