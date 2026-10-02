import { Directive, ElementRef, OnDestroy, OnInit, inject } from '@angular/core';
import { FormGroupDirective, NgForm } from '@angular/forms';

/**
 * One consistent "required fields" behaviour for every <form> in the app (no new form framework):
 * - an invalid form is never submitted (the submit is stopped before the screen's own save handler runs);
 * - every field is marked touched, so ff-* fields show "Please enter …" / "Please select …";
 * - plain inputs/selects get a red border and the same kind of message under them;
 * - the first invalid field is scrolled into view and focused.
 * Import FormValidationDirective in a component and it applies to all its <form> elements.
 */
let globalInstalled = false;

function installGlobalSubmitGuard(): void {
  if (globalInstalled || typeof document === 'undefined') return;
  globalInstalled = true;
  // Capture phase on document runs before Angular's (ngSubmit) on the form itself.
  document.addEventListener('submit', (ev: Event) => {
    const form = ev.target as HTMLFormElement | null;
    if (!form || form.tagName !== 'FORM') return;
    const invalid = form.classList.contains('ng-invalid') || (form.noValidate ? false : !form.checkValidity());
    if (!invalid) return;
    ev.preventDefault();
    ev.stopImmediatePropagation();
    ev.stopPropagation();
    form.dispatchEvent(new CustomEvent('ff-invalid-submit'));
  }, true);
}

@Directive({ selector: 'form', standalone: true })
export class FormValidationDirective implements OnInit, OnDestroy {
  private el = inject<ElementRef<HTMLFormElement>>(ElementRef);
  private group = inject(FormGroupDirective, { optional: true, self: true });
  private ngForm = inject(NgForm, { optional: true, self: true });
  private readonly onInvalid = () => this.showErrors();
  private readonly onEdit = () => { if (this.el.nativeElement.classList.contains('ff-submitted')) this.refreshNativeMessages(); };

  constructor() {
    installGlobalSubmitGuard();
  }

  ngOnInit(): void {
    const f = this.el.nativeElement;
    f.addEventListener('ff-invalid-submit', this.onInvalid);
    f.addEventListener('input', this.onEdit);
    f.addEventListener('change', this.onEdit);
  }

  ngOnDestroy(): void {
    const f = this.el.nativeElement;
    f.removeEventListener('ff-invalid-submit', this.onInvalid);
    f.removeEventListener('input', this.onEdit);
    f.removeEventListener('change', this.onEdit);
  }

  private showErrors(): void {
    const form = this.el.nativeElement;
    (this.group?.form ?? this.ngForm?.form)?.markAllAsTouched();
    form.classList.add('ff-submitted');
    setTimeout(() => {
      this.refreshNativeMessages();
      const first = form.querySelector<HTMLElement>(
        '.ff-field--error input, .ff-field--error select, .ff-field--error textarea, .ff-field--error button, '
        + 'input.ng-invalid, select.ng-invalid, textarea.ng-invalid, input:invalid, select:invalid, textarea:invalid');
      if (first) {
        first.scrollIntoView({ block: 'center', behavior: 'smooth' });
        first.focus({ preventScroll: true });
      }
    });
  }

  /** Message under each plain (non ff-*) invalid field; removed as soon as the field is filled. */
  private refreshNativeMessages(): void {
    const form = this.el.nativeElement;
    form.querySelectorAll('.ff-native-error').forEach(e => e.remove());
    form.querySelectorAll<HTMLInputElement>('input, select, textarea').forEach(ctrl => {
      if (ctrl.closest('.ff-field')) return;                       // ff-* fields show their own message
      const invalid = ctrl.classList.contains('ng-invalid') || (ctrl.willValidate && !ctrl.checkValidity());
      if (!invalid || ctrl.type === 'hidden' || ctrl.disabled) return;
      const msg = document.createElement('span');
      msg.className = 'ff-native-error';
      msg.setAttribute('role', 'alert');
      msg.textContent = this.messageFor(ctrl);
      ctrl.insertAdjacentElement('afterend', msg);
    });
  }

  private messageFor(ctrl: HTMLInputElement): string {
    const label = ctrl.closest('label');
    let name = '';
    if (label) {
      const clone = label.cloneNode(true) as HTMLElement;
      clone.querySelectorAll('input, select, textarea, .ff-native-error, span.material-icons').forEach(n => n.remove());
      name = (clone.textContent || '').replace(/\*/g, '').replace(/\s+/g, ' ').trim();
    }
    name = (name || ctrl.getAttribute('aria-label') || ctrl.getAttribute('placeholder') || 'this field').replace(/^(select|enter|choose)\s+/i, '');
    const empty = !ctrl.value || (ctrl.value + '').trim() === '';
    if (!empty) {
      if (ctrl.validity?.rangeUnderflow) return `${name} must be at least ${ctrl.min}`;
      if (ctrl.validity?.rangeOverflow) return `${name} must be at most ${ctrl.max}`;
      if (ctrl.validity?.typeMismatch) return `Please enter a valid ${name}`;
      return `Please check ${name}`;
    }
    const pick = ctrl.tagName === 'SELECT' || ['date', 'datetime-local', 'time', 'month', 'file'].includes(ctrl.type);
    return `Please ${pick ? 'select' : 'enter'} ${name}`;
  }
}
