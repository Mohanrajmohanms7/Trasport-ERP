import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

export function ffRequired(control: AbstractControl): ValidationErrors | null {
  const value = control.value;
  if (value === null || value === undefined || value === '') {
    return { required: true };
  }
  if (typeof value === 'string' && value.trim() === '') {
    return { required: true };
  }
  return null;
}

export function ffEmail(control: AbstractControl): ValidationErrors | null {
  if (!control.value) return null;
  const emailRegex = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/;
  return emailRegex.test(String(control.value)) ? null : { email: true };
}

export function ffPhone(control: AbstractControl): ValidationErrors | null {
  if (!control.value) return null;
  const phoneRegex = /^[+]?[\d\s()-]{7,15}$/;
  return phoneRegex.test(String(control.value).trim()) ? null : { phone: true };
}

export function ffDecimal(decimals = 2): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    if (control.value === null || control.value === undefined || control.value === '') {
      return null;
    }
    const regex = new RegExp(`^-?\\d+(\\.\\d{1,${decimals}})?$`);
    return regex.test(String(control.value)) ? null : { decimal: { decimals } };
  };
}

export function ffCurrency(control: AbstractControl): ValidationErrors | null {
  if (control.value === null || control.value === undefined || control.value === '') {
    return null;
  }
  const num = Number(control.value);
  return !isNaN(num) && num >= 0 ? null : { currency: true };
}

export function ffMinLength(min: number): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    if (!control.value) return null;
    return String(control.value).length >= min
      ? null
      : { minlength: { requiredLength: min, actualLength: String(control.value).length } };
  };
}

export function ffMaxLength(max: number): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    if (!control.value) return null;
    return String(control.value).length <= max
      ? null
      : { maxlength: { requiredLength: max, actualLength: String(control.value).length } };
  };
}

export function ffPattern(pattern: RegExp | string, errorKey = 'pattern'): ValidatorFn {
  const regex = typeof pattern === 'string' ? new RegExp(pattern) : pattern;
  return (control: AbstractControl): ValidationErrors | null => {
    if (!control.value) return null;
    return regex.test(String(control.value)) ? null : { [errorKey]: true };
  };
}

/** Cross-field: confirmPassword must match password */
export function ffMatchFields(sourceKey: string, confirmKey: string): ValidatorFn {
  return (group: AbstractControl): ValidationErrors | null => {
    const source = group.get(sourceKey)?.value;
    const confirm = group.get(confirmKey)?.value;
    if (source === null || confirm === null || source === undefined || confirm === undefined) {
      return null;
    }
    return source === confirm ? null : { mismatch: true };
  };
}
