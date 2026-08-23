# AKS Transport — PostgreSQL Seed Pack

Company: **AKS Transport**, Thannirpandhal, Perambalur, Tamil Nadu.

## Prerequisites
1. Database migrated with Flyway (`V1`–`V28`).
2. PostgreSQL connected as app user.

## Execute
```bash
psql -U transport_admin -d transport_erp -f database/seed/000_reset_all.sql
psql -U transport_admin -d transport_erp -f database/seed/099_run_all.sql
```
Or run files `001` … `050` in numeric order after `000_reset_all.sql`.

## Logins (BCrypt)
| User | Password | Role |
|------|----------|------|
| superadmin | Super@123 | SUPER_ADMIN |
| admin | Admin@123 | COMPANY_ADMIN |
| manager | Manager@123 | MANAGER |
| operator | Operator@123 | OPERATOR |
| accountant | Accountant@123 | ACCOUNTANT |
| driver1 | Driver@123 | DRIVER |
| viewer | Viewer@123 | VIEWER |

## After seed
Set `app.bootstrap.enabled=false` in `application.yml` so DataInitializer does not create a second DEMO company.
