import { Component, inject } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Location } from '@angular/common';
import { DispatchLoadsComponent } from './dispatch-loads';

/** Full-screen "Dispatch loads" page (/dispatch-loads?booking=ID) used by the Booking list and Trips & Dispatch. */
@Component({
  selector: 'app-dispatch-loads-page',
  standalone: true,
  imports: [DispatchLoadsComponent],
  template: `
  <div class="console-page h-full min-h-0 flex flex-col gap-3 font-sans text-[var(--ff-text-primary)]">
    <app-dispatch-loads [bookingId]="bookingId" [fullPage]="true" (closed)="back()" (done)="router.navigate(['/trips-planning'])" />
  </div>`
})
export class DispatchLoadsPageComponent {
  private route = inject(ActivatedRoute);
  private location = inject(Location);
  router = inject(Router);
  bookingId: number | null = Number(this.route.snapshot.queryParamMap.get('booking')) || null;
  back(): void { if (window.history.length > 1) this.location.back(); else this.router.navigate(['/trips-planning']); }
}
