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
const caLogin = await (await fetch(`${API}/auth/login`, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ username: 'ca1', password: 'Secret@123' }) })).json();
const tenant = caLogin?.data?.token ? caLogin.data : d;
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
  }, tenant);
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
  }, tenant);
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
{
  // Real first login through the login form as a freshly onboarded client admin.
  const fl = await browser.newContext({ viewport: { width: 1440, height: 900 } });
  const fp = await fl.newPage();
  await fp.goto('http://localhost:4200/login', { waitUntil: 'networkidle' });
  await fp.fill('input[formcontrolname=username]', 'velan.admin');
  await fp.fill('input[formcontrolname=password]', 'Velan@2026');
  await fp.click('button[type=submit]');
  await fp.waitForTimeout(4000);
  console.log('FIRST_LOGIN_URL ' + fp.url());
  await fp.screenshot({ path: 'shots/first-login.png' });
  await fl.close();
}
{
  // Masters restructure: menu, Branch Master, Lookup Lists, New vehicle / New customer dialogs; log any page errors.
  const ctx = await browser.newContext({ viewport: { width: 1440, height: 900 } });
  await ctx.addInitScript(s => {
    localStorage.setItem('token', s.token); localStorage.setItem('refreshToken', s.refreshToken);
    localStorage.setItem('username', s.username); localStorage.setItem('name', s.name || 'Admin');
    localStorage.setItem('roles', JSON.stringify(s.roles)); localStorage.setItem('subscriptionExpired', 'false');
    localStorage.setItem('companyId', String(s.companyId)); if (s.branchId) localStorage.setItem('branchId', String(s.branchId));
  }, tenant);
  const pg = await ctx.newPage();
  pg.on('pageerror', e => console.log('PAGE_ERROR ' + pg.url() + ' ' + e.message.slice(0, 150)));
  for (const [path, name] of [['/branches', 'm-branches'], ['/lookups', 'm-lookups'], ['/vehicles?new=1', 'm-vehicle-new'], ['/customers?new=1', 'm-customer-new'], ['/masters', 'm-masters-redirect'], ['/company-admin', 'm-settings']]) {
    await pg.goto('http://localhost:4200' + path, { waitUntil: 'networkidle' });
    await pg.waitForTimeout(1500);
    console.log('ROUTE ' + path + ' -> ' + pg.url());
    await pg.screenshot({ path: `shots/${name}.png` });
  }
  await ctx.close();
}
{ // NEW_FORMS: open the New vehicle / New customer dialogs
  const ctx = await browser.newContext({ viewport: { width: 1440, height: 900 } });
  await ctx.addInitScript(s => {
    localStorage.setItem('token', s.token); localStorage.setItem('refreshToken', s.refreshToken); localStorage.setItem('username', s.username);
    localStorage.setItem('roles', JSON.stringify(['COMPANY_ADMIN'])); localStorage.setItem('subscriptionExpired', 'false'); localStorage.setItem('companyId', String(s.companyId));
  }, tenant);
  const pg = await ctx.newPage();
  for (const [r, name] of [['vehicles?new=1', 'new-vehicle'], ['customers?new=1', 'new-customer']]) {
    await pg.goto('http://localhost:4200/' + r, { waitUntil: 'networkidle' }); await pg.waitForTimeout(1500);
    await pg.screenshot({ path: `shots/x-${name}.png` });
  }
  await ctx.close();
}
{ // BULK_SHOTS: vehicle list, edit dialog, upload preview
  const ctx = await browser.newContext({ viewport: { width: 1440, height: 900 } });
  await ctx.addInitScript(s => {
    localStorage.setItem('token', s.token); localStorage.setItem('refreshToken', s.refreshToken); localStorage.setItem('username', s.username);
    localStorage.setItem('roles', JSON.stringify(['COMPANY_ADMIN'])); localStorage.setItem('subscriptionExpired', 'false'); localStorage.setItem('companyId', String(s.companyId));
  }, tenant);
  const pg = await ctx.newPage();
  await pg.goto('http://localhost:4200/vehicles', { waitUntil: 'networkidle' }); await pg.waitForTimeout(1500);
  await pg.screenshot({ path: 'shots/x-vehicle-list.png' });
  try { await pg.locator('button:has-text("Edit")').first().click(); await pg.waitForTimeout(1200); await pg.screenshot({ path: 'shots/x-vehicle-edit.png' }); await pg.keyboard.press('Escape'); } catch (e) { console.log('edit shot', e.message); }
  await pg.goto('http://localhost:4200/customers', { waitUntil: 'networkidle' }); await pg.waitForTimeout(1500);
  await pg.screenshot({ path: 'shots/x-customer-list.png' });
  await pg.goto('http://localhost:4200/vehicles', { waitUntil: 'networkidle' }); await pg.waitForTimeout(1200);
  try {
    await pg.locator('button:has-text("Upload")').first().click(); await pg.waitForTimeout(600);
    const fs = await import('fs'); const p = '/home/runner/work/Trasport-ERP/Trasport-ERP/.ci/veh.xlsx';
    if (fs.existsSync(p)) { await pg.setInputFiles('input[type=file]', p); await pg.locator('button:has-text("Upload")').last().click(); await pg.waitForTimeout(2500); }
    await pg.screenshot({ path: 'shots/x-upload-preview.png' });
  } catch (e) { console.log('upload shot', e.message); }
  await ctx.close();
}
{ // SETTLE_SHOT
  for (const [vw, tag] of [[{ width: 1440, height: 1000 }, 'd'], [{ width: 390, height: 844 }, 'm']]) {
    const ctx = await browser.newContext({ viewport: vw });
    await ctx.addInitScript(s => {
      localStorage.setItem('token', s.token); localStorage.setItem('refreshToken', s.refreshToken); localStorage.setItem('username', s.username);
      localStorage.setItem('roles', JSON.stringify(['COMPANY_ADMIN'])); localStorage.setItem('subscriptionExpired', 'false'); localStorage.setItem('companyId', String(s.companyId));
    }, tenant);
    const pg = await ctx.newPage();
    await pg.goto('http://localhost:4200/payment-logs', { waitUntil: 'networkidle' }); await pg.waitForTimeout(1200);
    try { await pg.locator('button:has-text("Settlement Dashboard")').first().click(); await pg.waitForTimeout(2500); } catch (e) { console.log('settle', e.message); }
    await pg.screenshot({ path: `shots/x-settlement-${tag}.png`, fullPage: tag === 'd' });
    await ctx.close();
  }
}
{ // UOM_SHOTS: order units — PKC (Unit-only client) booking / trip / invoice screens, UOM tab, platform admin per client
  const pk = (await (await fetch(`${API}/auth/login`, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ username: 'pkc.admin', password: 'Pkc@2026' }) })).json()).data || tenant;
  const ctx = await browser.newContext({ viewport: { width: 1440, height: 900 } });
  await ctx.addInitScript(s => {
    localStorage.setItem('token', s.token); localStorage.setItem('refreshToken', s.refreshToken); localStorage.setItem('username', s.username);
    localStorage.setItem('roles', JSON.stringify(s.roles || ['COMPANY_ADMIN'])); localStorage.setItem('subscriptionExpired', 'false');
    localStorage.setItem('companyId', String(s.companyId)); if (s.branchId) localStorage.setItem('branchId', String(s.branchId));
  }, pk);
  const pg = await ctx.newPage();
  pg.on('pageerror', e => console.log('PAGE_ERROR ' + pg.url() + ' ' + e.message.slice(0, 150)));
  const shot = async (name) => { await pg.waitForTimeout(1500); await pg.screenshot({ path: `shots/uom-${name}.png` }); };
  try {
    await pg.goto('http://localhost:4200/bookings', { waitUntil: 'networkidle' }); await shot('booking-list');
    await pg.locator('button:has-text("Register Booking")').first().click(); await shot('booking-new');
    await pg.keyboard.press('Escape');
    await pg.goto('http://localhost:4200/trips-planning', { waitUntil: 'networkidle' }); await shot('trip-list');
    await pg.locator('button[title*="Edit"], button:has-text("Edit")').first().click(); await shot('trip-edit');
    await pg.goto('http://localhost:4200/billing-invoices', { waitUntil: 'networkidle' }); await shot('invoice-list');
    await pg.locator('text=Ready for billing').first().click().catch(() => {}); await shot('invoice-ready');
    await pg.goto('http://localhost:4200/materials-quarries', { waitUntil: 'networkidle' });
    await pg.locator('button:has-text("UOM Master")').first().click(); await shot('uom-tab');
  } catch (e) { console.log('UOM shot', e.message.slice(0, 160)); await shot('error'); }
  await ctx.close();
  const pc = await browser0().newContext({ viewport: { width: 1440, height: 900 } });
  await pc.addInitScript(s => {
    localStorage.setItem('token', s.token); localStorage.setItem('refreshToken', s.refreshToken); localStorage.setItem('username', s.username);
    localStorage.setItem('roles', JSON.stringify(['SUPER_ADMIN'])); localStorage.setItem('subscriptionExpired', 'false'); localStorage.setItem('companyId', String(s.companyId));
  }, d);
  const pp = await pc.newPage();
  try {
    await pp.goto('http://localhost:4200/platform-admin/features', { waitUntil: 'networkidle' }); await pp.waitForTimeout(1500);
    const sel = pp.locator('select').first(); const opts = await sel.locator('option').allTextContents();
    const i = opts.findIndex(o => o.includes('PKC')); await sel.selectOption({ index: i > 0 ? i : 1 });
    await pp.waitForTimeout(2000); await pp.screenshot({ path: 'shots/uom-platform-client.png' });
  } catch (e) { console.log('UOM platform shot', e.message.slice(0, 160)); }
  await pc.close();
}
{ // FX_SHOTS: Family Expenses add-on (company admin of company 1; smoke switched it on)
  const ctx = await browser.newContext({ viewport: { width: 1440, height: 900 } });
  await ctx.addInitScript(s => {
    localStorage.setItem('token', s.token); localStorage.setItem('refreshToken', s.refreshToken); localStorage.setItem('username', s.username);
    localStorage.setItem('roles', JSON.stringify(['COMPANY_ADMIN'])); localStorage.setItem('subscriptionExpired', 'false'); localStorage.setItem('companyId', String(s.companyId));
  }, tenant);
  const pg = await ctx.newPage();
  pg.on('pageerror', e => console.log('PAGE_ERROR ' + pg.url() + ' ' + e.message.slice(0, 150)));
  try {
    await pg.goto('http://localhost:4200/family-expenses', { waitUntil: 'networkidle' }); await pg.waitForTimeout(1500);
    await pg.screenshot({ path: 'shots/fx-list.png' });
    await pg.locator('button:has-text("Add Expense")').first().click(); await pg.waitForTimeout(1000);
    await pg.screenshot({ path: 'shots/fx-add.png' }); await pg.keyboard.press('Escape');
    await pg.goto('http://localhost:4200/family-expenses', { waitUntil: 'networkidle' }); await pg.waitForTimeout(800);
    await pg.locator('button:has-text("Reports")').first().click(); await pg.waitForTimeout(1500);
    await pg.screenshot({ path: 'shots/fx-reports.png' });
    await pg.locator('button:has-text("Categories")').first().click(); await pg.waitForTimeout(1200);
    await pg.screenshot({ path: 'shots/fx-categories.png' });
  } catch (e) { console.log('FX shot', e.message.slice(0, 160)); }
  await ctx.close();
  const mc = await browser.newContext({ viewport: { width: 390, height: 844 } });
  await mc.addInitScript(s => {
    localStorage.setItem('token', s.token); localStorage.setItem('refreshToken', s.refreshToken); localStorage.setItem('username', s.username);
    localStorage.setItem('roles', JSON.stringify(['COMPANY_ADMIN'])); localStorage.setItem('subscriptionExpired', 'false'); localStorage.setItem('companyId', String(s.companyId));
  }, tenant);
  const mp = await mc.newPage();
  await mp.goto('http://localhost:4200/family-expenses', { waitUntil: 'networkidle' }); await mp.waitForTimeout(1500);
  await mp.screenshot({ path: 'shots/fx-mobile.png' });
  await mc.close();
}
const lg = await browser.newContext({ viewport: { width: 390, height: 844 } });
const lp = await lg.newPage();
await lp.goto('http://localhost:4200/login', { waitUntil: 'networkidle' });
await lp.screenshot({ path: 'shots/m-login.png' });
await browser.close();
