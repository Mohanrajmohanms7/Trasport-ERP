// Responsive audit: every screen at phone / tablet / desktop sizes. Writes shots/responsive/audit.json + screenshots.
// Finds: elements outside the screen that are not inside a scrollable box (cut off or causing sideways scroll),
// clipped content, small touch targets (phones), and whether the main "Add / New" popup fits and scrolls.
import { chromium } from 'playwright';
import fs from 'fs';

const API = 'http://localhost:8080/api/v1';
const APP = 'http://localhost:4200';
const login = async (u, p) => (await (await fetch(`${API}/auth/login`, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ username: u, password: p }) })).json()).data;
const tenant = await login('pkc.admin', 'Pkc@2026') || await login('ca1', 'Secret@123');
const platform = await login('admin', 'Admin@123');

const routesFile = fs.readFileSync(process.env.ROUTES_TS || '../transport-frontend/src/app/app.routes.ts', 'utf8');
const tenantRoutes = [...new Set([...routesFile.matchAll(/path: '([^']+)'/g)].map(m => m[1])
  .filter(p => p && !p.includes(':') && !['login', '**', 'platform-admin'].includes(p) && !p.startsWith('platform-admin')))];
const platformRoutes = ['dashboard', 'companies', 'subscriptions', 'licenses', 'billing', 'users', 'audit', 'tickets', 'announcements', 'settings', 'backups', 'features']
  .map(s => `platform-admin/${s}`);

const VIEWPORTS = [
  ['phone', { width: 360, height: 780 }], ['phone-land', { width: 780, height: 360 }],
  ['tablet', { width: 768, height: 1024 }], ['tablet-land', { width: 1024, height: 768 }], ['desktop', { width: 1440, height: 900 }]
];
const SHOT = new Set(['phone', 'tablet']);
fs.mkdirSync('shots/responsive', { recursive: true });

const browser = await chromium.launch();
const results = [];

const measure = (scopeSel) => {
  const vw = window.innerWidth, vh = window.innerHeight;
  const root = scopeSel ? document.querySelector(scopeSel) : document.body;
  if (!root) return null;
  const label = el => {
    const cls = (el.className && typeof el.className === 'string') ? '.' + el.className.trim().split(/\s+/).slice(0, 3).join('.') : '';
    let comp = ''; for (let p = el; p; p = p.parentElement) { if (p.tagName && p.tagName.includes('-')) { comp = p.tagName.toLowerCase(); break; } }
    const text = (el.innerText || el.value || el.getAttribute('aria-label') || '').trim().replace(/\s+/g, ' ').slice(0, 40);
    return `${comp} ${el.tagName.toLowerCase()}${cls}${text ? ' "' + text + '"' : ''}`.trim();
  };
  const scrollableX = el => { for (let p = el.parentElement; p; p = p.parentElement) { const o = getComputedStyle(p).overflowX; if (o === 'auto' || o === 'scroll') return p; } return null; };
  const visible = el => { const r = el.getBoundingClientRect(); const s = getComputedStyle(el); return r.width > 0 && r.height > 0 && s.visibility !== 'hidden' && s.display !== 'none' && s.opacity !== '0'; };
  const out = [];
  for (const el of root.querySelectorAll('*')) {
    if (!visible(el) || el.closest('.cdk-overlay-container') && !scopeSel) continue;
    const r = el.getBoundingClientRect();
    if (r.right > vw + 2 && r.left < vw && !scrollableX(el)) {
      // only leaves / controls / text (not every wrapper of an offender)
      const childOver = [...el.children].some(c => visible(c) && c.getBoundingClientRect().right > vw + 2);
      if (!childOver) out.push({ el: label(el), right: Math.round(r.right), width: Math.round(r.width) });
    }
  }
  const clipped = [];
  for (const el of root.querySelectorAll('div,section,td,th,span,p,h1,h2,h3,label,button,a')) {
    if (!visible(el)) continue;
    const s = getComputedStyle(el);
    if ((s.overflowX === 'hidden' || s.overflowX === 'clip') && el.scrollWidth > el.clientWidth + 4 && s.textOverflow !== 'ellipsis' && el.clientWidth > 40) {
      clipped.push({ el: label(el), hiddenPx: el.scrollWidth - el.clientWidth });
    }
  }
  const small = [];
  if (vw <= 800) for (const el of root.querySelectorAll('button, a[href], input:not([type=hidden]), select, [role=button]')) {
    if (!visible(el)) continue;
    const r = el.getBoundingClientRect();
    if ((r.height < 32 || r.width < 32) && r.width > 0 && r.top < vh * 3) small.push({ el: label(el), w: Math.round(r.width), h: Math.round(r.height) });
  }
  return {
    pageOverflowX: Math.max(0, document.documentElement.scrollWidth - vw),
    offscreen: out.slice(0, 15), offscreenCount: out.length,
    clipped: clipped.slice(0, 10), clippedCount: clipped.length,
    smallTargets: small.slice(0, 10), smallCount: small.length
  };
};

async function audit(session, route, vpName, vp, idx) {
  const ctx = await browser.newContext({ viewport: vp, deviceScaleFactor: vpName.startsWith('phone') ? 2 : 1, hasTouch: vpName.startsWith('phone') || vpName.startsWith('tablet') });
  await ctx.addInitScript(s => {
    localStorage.setItem('token', s.token); localStorage.setItem('refreshToken', s.refreshToken || ''); localStorage.setItem('username', s.username);
    localStorage.setItem('roles', JSON.stringify(s.roles || [])); localStorage.setItem('subscriptionExpired', 'false');
    if (s.companyId) localStorage.setItem('companyId', String(s.companyId)); if (s.branchId) localStorage.setItem('branchId', String(s.branchId));
  }, session);
  const pg = await ctx.newPage();
  const errors = [];
  pg.on('pageerror', e => errors.push(e.message.slice(0, 120)));
  const rec = { route, vp: vpName };
  try {
    await pg.goto(`${APP}/${route}`, { waitUntil: 'networkidle', timeout: 25000 });
    await pg.waitForTimeout(700);
    rec.page = await pg.evaluate(measure, null);
    if (SHOT.has(vpName)) await pg.screenshot({ path: `shots/responsive/${vpName}-${String(idx).padStart(2, '0')}-${route.replace(/\//g, '_')}.png` });
    // Main add / new popup (phone + tablet portrait only)
    if (vpName === 'phone' || vpName === 'tablet') {
      const btn = pg.locator('button:visible').filter({ hasText: /^\s*(\+\s*)?(add|new|register|create|plan|record|report an issue|upload)\b/i }).first();
      if (await btn.count()) {
        rec.dialogButton = ((await btn.textContent()) || '').trim().slice(0, 40);
        await btn.click({ timeout: 4000 }).catch(() => {});
        await pg.waitForTimeout(800);
        const sel = await pg.evaluate(() => {
          const c = [...document.querySelectorAll('[role=dialog], .cdk-overlay-pane, app-master-form-dialog > div, app-quick-create > div, .fixed.inset-0')]
            .filter(e => e.getBoundingClientRect().width > 0);
          const el = c[c.length - 1];
          if (!el) return null;
          el.setAttribute('data-ra-dialog', '1');
          const box = (el.matches('.fixed.inset-0') && el.firstElementChild) ? el.firstElementChild : el;
          const r = box.getBoundingClientRect();
          const scroller = [box, ...box.querySelectorAll('*')].find(x => { const s = getComputedStyle(x); return (s.overflowY === 'auto' || s.overflowY === 'scroll') && x.scrollHeight > x.clientHeight; });
          return { left: Math.round(r.left), right: Math.round(r.right), top: Math.round(r.top), bottom: Math.round(r.bottom),
            fitsWidth: r.left >= -1 && r.right <= innerWidth + 1, fitsHeight: r.top >= -1 && (r.bottom <= innerHeight + 1 || !!scroller),
            scrolls: !!scroller };
        });
        if (sel) {
          rec.dialog = { ...sel, inner: await pg.evaluate(measure, '[data-ra-dialog]') };
          if (SHOT.has(vpName)) await pg.screenshot({ path: `shots/responsive/${vpName}-${String(idx).padStart(2, '0')}-${route.replace(/\//g, '_')}-dialog.png` });
        }
      }
    }
  } catch (e) { rec.error = e.message.slice(0, 160); }
  if (errors.length) rec.pageErrors = errors.slice(0, 3);
  results.push(rec);
  await ctx.close();
}

let i = 0;
for (const r of tenantRoutes) { i++; for (const [n, vp] of VIEWPORTS) await audit(tenant, r, n, vp, i); }
for (const r of platformRoutes) { i++; for (const [n, vp] of VIEWPORTS) await audit(platform, r, n, vp, i); }
await browser.close();
fs.writeFileSync('shots/responsive/audit.json', JSON.stringify(results, null, 1));

// Short summary for the CI log
const bad = results.filter(r => r.page && (r.page.offscreenCount || r.page.clippedCount || r.page.pageOverflowX) || r.dialog && (!r.dialog.fitsWidth || !r.dialog.fitsHeight || r.dialog.inner?.offscreenCount));
console.log(`RESPONSIVE audited ${results.length} screen×size, ${bad.length} with problems; ${results.filter(r => r.error).length} errors`);
