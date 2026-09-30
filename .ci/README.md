# Automated checks (.ci)

Run by `.github/workflows/verify.yml` on every pull request to `main`, on pushes to any `ci/**` branch, or manually
(Actions → verify → Run workflow).

| File | What it does |
|---|---|
| `smoke.sh` | ~180 end-to-end API checks against a freshly booted app + PostgreSQL (all migrations): masters, booking → trip → invoice → receipt, GST, payroll, inventory/costing, payables, reports, exports, attachments, bulk upload, security & tenant isolation, subscription, onboarding. Prints `PASS …` / `FAIL …` and `SMOKE_FAILS=n`. |
| `seed_pkc_demo.py` | Copy of `tools/demo/seed_pkc_demo.py` used by the smoke to load the PKC demo company and check every report has data. |
| `shots.mjs` + `routes.txt` | Playwright screenshots (desktop + phone) of the routes listed in `routes.txt`; pushed to branch `<ci-branch>-out/shots`. |

How to use:
1. Open a PR to `main` → the run must be green (the **Gate** step fails on any backend/boot/frontend failure or any `FAIL`).
2. For screenshots: push the branch as `ci/<name>`, put the routes in `.ci/routes.txt` (comma-separated, e.g. `payment-logs,reports`),
   then look at branch `ci/<name>-out`. Delete both branches afterwards.
3. When you add a feature, append its checks to the end of `smoke.sh` (before `echo "SMOKE_FAILS=$FAILS"`), reusing the helpers
   `post / put / api / as / j / pass / fail` and the variables created earlier (tokens `$TOKEN` platform admin, `$TC` company admin,
   `$TO` operator, `$TD` driver, `$NT` second company; ids `$CUST $MAT $BOOK $TRIP $INV $DRV …`).

Note: the file grew section by section during development, so a few checks appear twice; that is harmless.
