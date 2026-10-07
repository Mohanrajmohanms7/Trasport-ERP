"""Create the Neon database for the Render test environment, switch the Render backend to it, deploy, verify.

Never prints a password or key (GitHub masks them; values are registered with ::add-mask::).
"""
import json, os, subprocess, sys, time, urllib.error, urllib.request

NEON = "https://console.neon.tech/api/v2"
RENDER = "https://api.render.com/v1"
PROJECT, DB, SERVICE = "transaflow-test", "transport_erp", "transport-backend"
MODE = os.environ.get("MODE", "setup")
REGION = {"oregon": "aws-us-west-2", "ohio": "aws-us-east-2", "virginia": "aws-us-east-1",
          "frankfurt": "aws-eu-central-1", "singapore": "aws-ap-southeast-1"}


def mask(v):
    if v:
        print(f"::add-mask::{v}")


def call(base, key, method, path, body=None, ok=(200, 201, 202, 204)):
    req = urllib.request.Request(base + path, method=method, data=json.dumps(body).encode() if body is not None else None,
                                 headers={"Authorization": f"Bearer {key}", "Accept": "application/json", "Content-Type": "application/json"})
    for attempt in range(8):
        try:
            with urllib.request.urlopen(req, timeout=60) as r:
                raw = r.read()
                return json.loads(raw) if raw else {}
        except urllib.error.HTTPError as e:
            msg = e.read().decode()[:300]
            if e.code in (423, 429, 500, 502, 503) and attempt < 7:   # Neon: operation still running / rate limit
                time.sleep(5 + attempt * 3); continue
            if e.code in ok:
                return {}
            raise SystemExit(f"{method} {path} -> HTTP {e.code}: {msg}")


neon = lambda m, p, b=None: call(NEON, os.environ["NEON_API_KEY"], m, p, b)
render = lambda m, p, b=None: call(RENDER, os.environ["RENDER_API_KEY"], m, p, b)
report = []
def step(msg):
    print("==>", msg); report.append(msg)


def post_comment(text):
    tok, repo, sha = os.environ.get("GH_TOKEN"), os.environ.get("GH_REPO"), os.environ.get("GH_SHA")
    if not (tok and repo and sha):
        return
    req = urllib.request.Request(f"https://api.github.com/repos/{repo}/commits/{sha}/comments", method="POST",
                                 data=json.dumps({"body": text}).encode(),
                                 headers={"Authorization": f"Bearer {tok}", "Accept": "application/vnd.github+json"})
    try:
        urllib.request.urlopen(req, timeout=30)
    except Exception as e:
        print("comment failed", e)


def main():
    # 1. Render backend service and its region
    svcs = render("GET", f"/services?name={SERVICE}&limit=20")
    svc = next((s["service"] for s in svcs if s["service"]["name"] == SERVICE), None)
    if not svc:
        raise SystemExit(f"Render service {SERVICE} not found (services: {[s['service']['name'] for s in svcs]})")
    sid = svc["id"]; region = (svc.get("serviceDetails") or {}).get("region", "singapore")
    step(f"Render service {SERVICE} found, region {region}")

    # 2. Neon project (create once, reuse afterwards)
    projects = neon("GET", "/projects").get("projects", [])
    proj = next((p for p in projects if p["name"] == PROJECT), None)
    if not proj:
        if MODE != "setup":
            raise SystemExit("Neon project not found — run with mode=setup")
        created = neon("POST", "/projects", {"project": {"name": PROJECT, "pg_version": 16, "region_id": REGION.get(region, "aws-ap-southeast-1")}})
        proj = created["project"]; step(f"Neon project {PROJECT} created in {proj['region_id']}")
    else:
        step(f"Neon project {PROJECT} already exists in {proj['region_id']} — reused")
    pid = proj["id"]
    branches = neon("GET", f"/projects/{pid}/branches")["branches"]
    br = next((b for b in branches if b.get("default") or b.get("primary")), branches[0])
    ep = next(e for e in neon("GET", f"/projects/{pid}/endpoints")["endpoints"] if e["type"] == "read_write")
    host = ep["host"]                                   # direct host (no -pooler)
    roles = [r for r in neon("GET", f"/projects/{pid}/branches/{br['id']}/roles")["roles"] if not r.get("protected")]
    role = roles[0]["name"]
    pwd = neon("GET", f"/projects/{pid}/branches/{br['id']}/roles/{role}/reveal_password")["password"]
    mask(pwd)
    dbs = [d["name"] for d in neon("GET", f"/projects/{pid}/branches/{br['id']}/databases")["databases"]]
    if DB not in dbs:
        neon("POST", f"/projects/{pid}/branches/{br['id']}/databases", {"database": {"name": DB, "owner_name": role}})
        step(f"Database {DB} created (owner {role})")
    else:
        step(f"Database {DB} present")

    env = dict(os.environ, PGPASSWORD=pwd, PGSSLMODE="require")
    def sql(q):
        for attempt in range(6):
            r = subprocess.run(["psql", "-h", host, "-U", role, "-d", DB, "-tAc", q], env=env, capture_output=True, text=True)
            if r.returncode == 0:
                return r.stdout.strip()
            time.sleep(5)                                 # compute waking up
        raise SystemExit("psql failed: " + r.stderr.strip()[:300])
    step("Neon connection OK — " + sql("select split_part(version(), ' on ', 1)"))

    # 3. Point Render at Neon and deploy
    jdbc = f"jdbc:postgresql://{host}/{DB}?sslmode=require"
    if MODE == "setup":
        for k, v in (("DATABASE_URL", jdbc), ("DATABASE_USERNAME", role), ("DATABASE_PASSWORD", pwd)):
            render("PUT", f"/services/{sid}/env-vars/{k}", {"value": v})
        step(f"Render settings updated: DATABASE_URL = jdbc:postgresql://{host}/{DB}?sslmode=require, DATABASE_USERNAME = {role}, DATABASE_PASSWORD = (hidden)")
        dep = render("POST", f"/services/{sid}/deploys", {"clearCache": "do_not_clear"})
        did = dep.get("id") or dep.get("deploy", {}).get("id")
        step(f"Render deploy started ({did})")
        status, t0 = "", time.time()
        while time.time() - t0 < 1500:
            time.sleep(20)
            d = render("GET", f"/services/{sid}/deploys/{did}")
            status = d.get("status") or d.get("deploy", {}).get("status")
            print(f"   deploy status: {status} ({int(time.time() - t0)} s)")
            if status in ("live", "build_failed", "update_failed", "canceled", "deactivated", "pre_deploy_failed"):
                break
        step(f"Render deploy finished: {status}")
        if status != "live":
            raise SystemExit("Deploy did not go live — check Render → transport-backend → Logs")

    # 4. Verify: migrations on Neon + admin login through the public app
    mig = sql("select count(*) || ' applied, latest V' || max(version::int) from flyway_schema_history where success")
    step("Flyway on Neon: " + mig)
    step("Platform admin on Neon: " + sql("select count(*) from app_users where username = 'admin' and is_deleted = false") + " user 'admin'")
    envs = render("GET", f"/services/{sid}/env-vars?limit=100")
    admin_pwd = next((e["envVar"]["value"] for e in envs if e["envVar"]["key"] == "APP_BOOTSTRAP_ADMIN_PASSWORD"), None)
    mask(admin_pwd)
    url = (svc.get("serviceDetails") or {}).get("url") or f"https://{SERVICE}.onrender.com"
    login = urllib.request.Request(url + "/api/v1/auth/login", method="POST", data=json.dumps({"username": "admin", "password": admin_pwd}).encode(),
                                   headers={"Content-Type": "application/json"})
    try:
        with urllib.request.urlopen(login, timeout=90) as r:
            ok = '"token"' in r.read().decode()
    except urllib.error.HTTPError as e:
        ok = False; print("login HTTP", e.code)
    step("Admin login through the live app: " + ("OK" if ok else "FAILED"))

    with open(os.environ.get("GITHUB_STEP_SUMMARY", "/dev/null"), "a") as f:
        f.write("## Neon setup\n\n" + "\n".join(f"- {m}" for m in report) + "\n")
    print("RESULT:", "SUCCESS" if ok else "CHECK")


if __name__ == "__main__":
    err = None
    try:
        main()
    except SystemExit as e:
        err = str(e)
    except Exception as e:
        err = f"{type(e).__name__}: {e}"
    text = "NEON-SETUP " + ("FAILED: " + err if err else "OK") + "\n" + "\n".join("- " + m for m in report)
    print(text)
    post_comment(text)
    sys.exit(1 if err else 0)
