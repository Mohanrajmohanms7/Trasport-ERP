import { Pipe, PipeTransform } from '@angular/core';

@Pipe({ name: 'ffCurrency', standalone: true })
export class FfCurrencyPipe implements PipeTransform {
  transform(
    value: number | string | null | undefined,
    currency = 'INR',
    locale = 'en-IN'
  ): string {
    if (value === null || value === undefined || value === '') return '';
    const num = typeof value === 'string' ? parseFloat(value) : value;
    if (isNaN(num)) return '';
    return new Intl.NumberFormat(locale, {
      style: 'currency',
      currency,
      minimumFractionDigits: 2
    }).format(num);
  }
}

@Pipe({ name: 'ffDateFormat', standalone: true })
export class FfDateFormatPipe implements PipeTransform {
  transform(
    value: string | Date | null | undefined,
    format: 'short' | 'medium' | 'long' = 'medium'
  ): string {
    if (!value) return '';
    const date = typeof value === 'string' ? new Date(value) : value;
    if (isNaN(date.getTime())) return '';

    const options: Intl.DateTimeFormatOptions =
      format === 'short'
        ? { day: '2-digit', month: '2-digit', year: 'numeric' }
        : format === 'long'
          ? { day: 'numeric', month: 'long', year: 'numeric' }
          : { day: '2-digit', month: 'short', year: 'numeric' };

    return new Intl.DateTimeFormat('en-IN', options).format(date);
  }
}

@Pipe({ name: 'ffTruncate', standalone: true })
export class FfTruncatePipe implements PipeTransform {
  transform(value: string | null | undefined, limit = 50, trail = '…'): string {
    if (!value) return '';
    return value.length > limit ? value.substring(0, limit).trimEnd() + trail : value;
  }
}
