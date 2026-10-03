# Help & Support tickets (V78)

Clients report problems from inside TransaFlow; the platform admin answers, tracks and closes them.

## Numbers
`<client prefix>-TKT-00001`, a running number per client: PKC → `PKC-TKT-00001`, AKS → `AKS-TKT-00001`.
The prefix is the company code without trailing digits (PKC001 → PKC), fixed at the client's first ticket
(`saas_support_ticket_counters`). If another client already uses those letters, the full code is used (AKS002-TKT-…).

## Who sees what
| User | Can |
|---|---|
| Company Admin / Admin | Report; see **all** tickets of their company; reply, confirm, reopen |
| Other company users (incl. Viewer) | Report; see **only their own** tickets |
| Driver-only logins | No (they report through their manager) |
| Platform admin (SUPER_ADMIN) | All clients' tickets: reply, **internal notes**, status, priority, assign, resolve, close, dashboard |

The client API (`/api/v1/support/**`) takes the company from the logged-in user, never from the request. **Internal notes
and internal events (assignment) are removed on the server** before a client response is built; client screens never
receive the raw ticket. The platform API is `/api/v1/platform-admin/tickets/**` (SUPER_ADMIN only).

## Lifecycle
Open → In progress → Waiting for client → Resolved → Closed. A client reply to "Waiting for client" moves it back to
In progress. Resolve needs a resolution text (shown to the client). Resolved → client **Issue fixed** → Closed, or
**Still not working** (with a message) → In progress (reopen count +1). Closed tickets take no replies (internal notes
only); a returning problem is a new ticket.

## Context captured automatically
Company, user, module and screen (from the menu), page URL, browser (User-Agent), app version, time; the open record
when a screen provides it (`SupportContextService.setRecord`, done for trips: "TRP-…").

## Targets (overdue)
First reply / fix: Critical 1 h / 4 h, High 4 h / 1 day, Medium 8 h / 3 days, Low 1 day / 5 days
(`SupportTicketService.firstResponseHours / resolveHours`). Overdue = Open or In progress past the fix target.

## History
`saas_support_ticket_events` (created, status, priority, assigned, reopened) + replies form the timeline; every action
is also in `audit_logs` (entity `saas_support_tickets`).

## Next phases
2. Attachments (screenshots / PDF) stored in the database (survive deploys), public vs internal; auto-close Resolved
   after 7 days. 3. In-app notification bell for both sides, critical banner. 4. Record context on more screens.
