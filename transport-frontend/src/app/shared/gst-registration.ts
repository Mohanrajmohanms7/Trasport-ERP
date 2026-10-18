import { AbstractControl, FormGroup, Validators } from '@angular/forms';

/** Company GST registration (server rules: util/GstRegistration). GSTIN is asked for and required only when Registered. */
export const GST_STATUSES = [
  { value: 'REGISTERED', label: 'Registered (has GSTIN)' },
  { value: 'UNREGISTERED', label: 'Unregistered' },
  { value: 'PENDING', label: 'Registration pending' }
];
export const GSTIN_PATTERN = /^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$/;

/** Wire a form's status → GSTIN rule: Registered = required + format; otherwise cleared (stored as null) and not required. */
export function wireGstRegistration(form: FormGroup, statusName = 'gstRegistrationStatus', gstinName = 'gstNumber'): void {
  const status = form.get(statusName), gstin = form.get(gstinName);
  if (!status || !gstin) return;
  const apply = (s: string | null) => applyGstRule(gstin, s);
  status.valueChanges.subscribe(apply);
  apply(status.value);
}

export function applyGstRule(gstin: AbstractControl, status: string | null): void {
  if (status === 'REGISTERED') {
    gstin.setValidators([Validators.required, Validators.pattern(GSTIN_PATTERN)]);
  } else {
    gstin.clearValidators();
    if (gstin.value) gstin.setValue(null, { emitEvent: false });   // no stale GSTIN for Unregistered / Pending
  }
  gstin.updateValueAndValidity({ emitEvent: false });
}
