/**
 * Resolves the logged-in tenant company id.
 * Prefer JWT claim, then localStorage. Never defaults to company 1 (AKS).
 */

function readJwtPayload(token: string | null): Record<string, unknown> | null {
  if (!token) return null;
  try {
    const part = token.split('.')[1];
    if (!part) return null;
    const json = atob(part.replace(/-/g, '+').replace(/_/g, '/'));
    return JSON.parse(json);
  } catch {
    return null;
  }
}

function toPositiveId(value: unknown): number | null {
  const id = typeof value === 'number' ? value : Number(value);
  if (!Number.isFinite(id) || id <= 0) return null;
  return id;
}

export function resolveTenantCompanyId(): number {
  const token = localStorage.getItem('token');
  const fromJwt = toPositiveId(readJwtPayload(token)?.['companyId']);
  if (fromJwt != null) {
    localStorage.setItem('companyId', String(fromJwt));
    return fromJwt;
  }

  const fromStorage = toPositiveId(localStorage.getItem('companyId'));
  if (fromStorage != null) {
    return fromStorage;
  }

  throw new Error('No company is assigned to this session. Please log out and log in again.');
}

export function tryResolveTenantCompanyId(): number | null {
  try {
    return resolveTenantCompanyId();
  } catch {
    return null;
  }
}

export function resolveTenantBranchId(): number | null {
  const token = localStorage.getItem('token');
  const fromJwt = toPositiveId(readJwtPayload(token)?.['branchId']);
  if (fromJwt != null) {
    localStorage.setItem('branchId', String(fromJwt));
    return fromJwt;
  }

  const fromStorage = toPositiveId(localStorage.getItem('branchId'));
  if (fromStorage != null) {
    return fromStorage;
  }

  return null;
}

export function tryResolveTenantBranchId(): number | null {
  try {
    return resolveTenantBranchId();
  } catch {
    return null;
  }
}

