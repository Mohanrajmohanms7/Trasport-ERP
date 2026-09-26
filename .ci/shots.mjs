import { chromium } from 'playwright';
const __b = await chromium.launch();
const browser0 = () => __b;
const browser = __b;
const API = 'http://localhost:8080/api/v1';
const login = await (await fetch(`${API}/auth/login`, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ username: 'admin', password: 'Admin@123' }) })).json();
const d = login.data;
const H = { 'Content-Type': 'application/json', Authorization: `Bearer ${d.token}` };
const call = async (m, p, b) => (await fetch(API + p, { method: m, headers: H, body: b ? JSON.stringify(b) : undefined })).json();
const pl = await call('GET', '/driver-payrolls?size=200&sort=id,desc');
console.log('PAYROLL LIST', JSON.stringify((pl.data?.content || []).map(p => [p.id, p.status, p.payrollNumber])), 'total', pl.data?.totalElements);
// Repro: finish the DRAFT payroll, then list again (was a cancelled payroll disappearing?)
const draft = (pl.data?.content || []).find(p => p.status === 'DRAFT');
if (draft) {
  for (const [m, p, b] of [['PUT', `/driver-payrolls/${draft.id}`, { driverId: draft.driver.id, payYear: draft.payYear, payMonth: draft.payMonth, allowanceAmount: 0, advanceAdjustment: 500, deductions: [{ deductionType: 'FINE', amount: 100, remarks: 'Late delivery' }] }],
      ['POST', `/driver-payrolls/${draft.id}/approve`], ['POST', `/driver-payrolls/${draft.id}/post`], ['POST', `/driver-payrolls/${draft.id}/pay`, { paymentMethod: 'BANK_TRANSFER', paymentReference: 'UTR45821' }]]) {
    const r = await call(m, p, b);
    const again = await call('GET', '/driver-payrolls?size=200&sort=id,desc');
    console.log('AFTER', m, p, r.success, JSON.stringify((again.data?.content || []).map(x => [x.id, x.status])), 'total', again.data?.totalElements);
  }
}
const routes = (process.env.ROUTES || 'dashboard').split(',');
// Platform operator view (real SUPER_ADMIN roles)
{
  for (const [vw, tag] of [[{ width: 1440, height: 900 }, 'pd'], [{ width: 390, height: 844 }, 'pm']]) {
    const ctx = await browser0().newContext({ viewport: vw });
    await ctx.addInitScript(s => {
      localStorage.setItem('token', s.token); localStorage.setItem('refreshToken', s.refreshToken);
      localStorage.setItem('username', s.username); localStorage.setItem('name', 'Platform Operator');
      localStorage.setItem('roles', JSON.stringify(['SUPER_ADMIN'])); localStorage.setItem('subscriptionExpired', 'false');
      localStorage.setItem('companyId', String(s.companyId));
    }, d);
    const pg = await ctx.newPage();
    for (const r of ['platform-admin/dashboard', 'platform-admin/companies', 'platform-admin/backups']) {
      await pg.goto('http://localhost:4200/' + r, { waitUntil: 'networkidle' });
      await pg.waitForTimeout(1500);
      await pg.screenshot({ path: `shots/${tag}-${r.replace(/\//g, '_')}.png` });
    }
    await ctx.close();
  }
}

for (const [vw, tag] of [[{ width: 1440, height: 900 }, 'd'], [{ width: 390, height: 844 }, 'm']]) {
  const ctx = await browser.newContext({ viewport: vw });
  await ctx.addInitScript(s => {
    localStorage.setItem('token', s.token); localStorage.setItem('refreshToken', s.refreshToken);
    localStorage.setItem('username', s.username); localStorage.setItem('name', 'Accounts Team');
    localStorage.setItem('roles', JSON.stringify(['COMPANY_ADMIN']));
    localStorage.setItem('subscriptionExpired', 'false'); localStorage.setItem('companyId', String(s.companyId));
    if (s.branchId) localStorage.setItem('branchId', String(s.branchId));
  }, d);
  const page = await ctx.newPage();
  for (const r of routes) {
    try {
      await page.goto('http://localhost:4200/' + r, { waitUntil: 'networkidle', timeout: 30000 });
      await page.waitForTimeout(1200);
      await page.screenshot({ path: `shots/${tag}-${r.replace(/\//g, '_')}.png` });
    } catch (e) { console.log('ERR', r, e.message.slice(0, 100)); }
  }
  await ctx.close();
}
{
  const ctx = await browser.newContext({ viewport: { width: 390, height: 844 }, deviceScaleFactor: 2 });
  await ctx.addInitScript(s => {
    localStorage.setItem('token', s.token); localStorage.setItem('refreshToken', s.refreshToken);
    localStorage.setItem('username', s.username); localStorage.setItem('name', 'Accounts Team');
    localStorage.setItem('roles', JSON.stringify(['COMPANY_ADMIN'])); localStorage.setItem('subscriptionExpired', 'false');
    localStorage.setItem('companyId', String(s.companyId)); if (s.branchId) localStorage.setItem('branchId', String(s.branchId));
  }, d);
  const pg = await ctx.newPage();
  await pg.goto('http://localhost:4200/driver-payroll', { waitUntil: 'networkidle' });
  await pg.waitForTimeout(1200);
  await pg.screenshot({ path: 'shots/x-mobile-payroll-list.png' });
  await pg.locator('button:has-text("PAID")').first().click();
  await pg.waitForTimeout(800);
  await pg.screenshot({ path: 'shots/x-mobile-payroll-detail.png', fullPage: true });
  await pg.getByLabel('Open menu').click();
  await pg.waitForTimeout(600);
  await pg.screenshot({ path: 'shots/x-mobile-menu.png' });
  await ctx.close();
}
const lg = await browser.newContext({ viewport: { width: 390, height: 844 } });
const lp = await lg.newPage();
await lp.goto('http://localhost:4200/login', { waitUntil: 'networkidle' });
await lp.screenshot({ path: 'shots/m-login.png' });
await browser.close();
