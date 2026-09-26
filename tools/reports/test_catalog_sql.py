import json, re, subprocess, sys
import os
R=json.load(open(os.path.join(os.path.dirname(os.path.abspath(__file__)),'..','..','transport-backend','src','main','resources','reports','report-catalog.json')))
params=dict(companyId='1',branchId='NULL',**{'from':"'2026-01-01'",'to':"'2026-09-30'"},vehicleId='NULL',driverId='NULL',customerId='NULL',status='NULL',
            warehouseId='NULL',sparePartId='NULL',category='NULL',accountCode='NULL',days='NULL')
bad=0
for r in R:
    sql=r['sql']
    sql=re.sub(r':([a-zA-Z]+)', lambda m: params.get(m.group(1), m.group(0)), sql)
    p=subprocess.run(['psql','-h','localhost','-U','erp','-d','erp','-v','ON_ERROR_STOP=1','-tAc',sql],capture_output=True,text=True,env={'PGPASSWORD':'erp'})
    if p.returncode!=0:
        bad+=1; print('FAIL',r['key'],p.stderr.strip()[:300])
    else:
        # verify column keys exist in result
        p2=subprocess.run(['psql','-h','localhost','-U','erp','-d','erp','-c',f"SELECT * FROM ({sql}) q LIMIT 0"],capture_output=True,text=True,env={'PGPASSWORD':'erp'})
        hdr=[h.strip() for h in p2.stdout.splitlines()[0].split('|')] if p2.stdout else []
        miss=[c['key'] for c in r['columns'] if c['key'] not in hdr]
        if miss: bad+=1; print('COLS',r['key'],miss)
print('bad',bad,'of',len(R))
