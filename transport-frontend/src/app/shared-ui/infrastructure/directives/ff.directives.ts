import { Directive, ElementRef, HostListener, inject, input } from '@angular/core';
import { AuthService } from '../../../services/auth.service';

/** Trims whitespace on blur for text inputs */
@Directive({
  selector: 'input[ffTrim], textarea[ffTrim]',
  standalone: true
})
export class FfTrimDirective {
  private el = inject(ElementRef<HTMLInputElement | HTMLTextAreaElement>);

  @HostListener('blur')
  onBlur(): void {
    const value = this.el.nativeElement.value;
    if (typeof value === 'string') {
      const trimmed = value.trim();
      if (trimmed !== value) {
        this.el.nativeElement.value = trimmed;
        this.el.nativeElement.dispatchEvent(new Event('input', { bubbles: true }));
      }
    }
  }
}

/** Forces uppercase on input */
@Directive({
  selector: 'input[ffUppercase]',
  standalone: true
})
export class FfUppercaseDirective {
  private el = inject(ElementRef<HTMLInputElement>);

  @HostListener('input')
  onInput(): void {
    const start = this.el.nativeElement.selectionStart;
    const end = this.el.nativeElement.selectionEnd;
    const upper = this.el.nativeElement.value.toUpperCase();
    if (this.el.nativeElement.value !== upper) {
      this.el.nativeElement.value = upper;
      this.el.nativeElement.setSelectionRange(start, end);
      this.el.nativeElement.dispatchEvent(new Event('input', { bubbles: true }));
    }
  }
}

/**
 * Structural-like attribute directive: hides host when user lacks permission.
 * Usage: <button ffPermission="driver.create">Add</button>
 * Or with roles: <div [ffRole]="['SUPER_ADMIN','ADMIN']">...</div>
 */
@Directive({
  selector: '[ffPermission]',
  standalone: true,
  host: {
    '[style.display]': 'allowed() ? null : "none"'
  }
})
export class FfPermissionDirective {
  private auth = inject(AuthService);
  ffPermission = input.required<string>();

  allowed(): boolean {
    const perm = this.ffPermission();
    if (!perm) return true;
    const user = this.auth.currentUser();
    if (!user) return false;
    // SUPER_ADMIN bypass
    if (user.roles?.includes('SUPER_ADMIN')) return true;
    // Permission codes may arrive as ROLE_ codes today; allow role match or future permission list
    return user.roles?.some(r => r === perm || r.endsWith(perm)) ?? false;
  }
}

@Directive({
  selector: '[ffRole]',
  standalone: true,
  host: {
    '[style.display]': 'hasRole() ? null : "none"'
  }
})
export class FfRoleDirective {
  private auth = inject(AuthService);
  ffRole = input.required<string | string[]>();

  hasRole(): boolean {
    const required = this.ffRole();
    const roles = Array.isArray(required) ? required : [required];
    const user = this.auth.currentUser();
    if (!user?.roles?.length) return false;
    if (user.roles.includes('SUPER_ADMIN')) return true;
    return roles.some(r => user.roles.includes(r));
  }
}
