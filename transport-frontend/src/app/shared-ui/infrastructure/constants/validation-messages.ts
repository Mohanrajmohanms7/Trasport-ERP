export const FF_VALIDATION_MESSAGES: Record<string, string> = {
  required: 'This field is required',
  email: 'Enter a valid email address',
  phone: 'Enter a valid phone number',
  minlength: 'Value is too short',
  maxlength: 'Value is too long',
  min: 'Value is too small',
  max: 'Value is too large',
  pattern: 'Enter a valid value in the correct format',
  decimal: 'Enter a valid decimal number',
  currency: 'Enter a valid amount',
  mismatch: 'Values do not match',
  server: 'Validation failed on server'
};

export const FF_DEFAULT_PAGE_SIZE = 20;
export const FF_PAGE_SIZE_OPTIONS = [10, 20, 50, 100];
export const FF_THEME_STORAGE_KEY = 'ff-theme';
export const FF_CONTRAST_STORAGE_KEY = 'ff-contrast';
