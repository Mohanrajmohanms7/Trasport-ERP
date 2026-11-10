import { Directive, ElementRef, Input, effect, inject, signal } from '@angular/core';
import { PermissionStore } from './permission-store.service';

/** Hides the element when the user's role lacks the permission: <button ffCan="invoices:approve">. The server enforces it too. */
@Directive({ selector: '[ffCan]', standalone: true })
export class FfCanDirective {
  private el = inject(ElementRef<HTMLElement>);
  private store = inject(PermissionStore);
  private key = signal('');
  @Input() set ffCan(v: string) { this.key.set(v || ''); }
  constructor() {
    effect(() => {
      const ok = this.store.canKey(this.key());
      this.el.nativeElement.style.display = ok ? '' : 'none';
      this.el.nativeElement.setAttribute('aria-hidden', ok ? 'false' : 'true');
    });
  }
}
