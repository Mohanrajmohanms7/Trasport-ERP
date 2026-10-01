/**
 * Display helpers for units of measure on order lines (booking / trip / invoice).
 * A line's quantity is always in the unit stored on that line; nothing is converted.
 */
export function uomLabel(uom: { symbol?: string | null; code?: string | null; label?: string | null } | null | undefined): string {
  if (!uom) return '';
  const s = (uom.label || uom.symbol || '').toString().trim();
  return s || (uom.code || '').toString();
}

/** "2 Unit M-Sand", "5.5 Ton Blue Metal 20mm". */
export function orderLineText(quantity: any, uom: any, materialName?: string | null): string {
  const q = quantity === null || quantity === undefined || quantity === '' ? '' : Number(quantity).toLocaleString('en-IN', { maximumFractionDigits: 3 });
  return [q, uomLabel(uom), materialName || ''].filter(x => x !== '').join(' ');
}
