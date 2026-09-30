# TransaFlow — User Guide (English)

**Quarry-to-site transport ERP.** This guide explains, step by step, how to run your transport business in TransaFlow:
from setting up the company to sending invoices, collecting money, paying drivers and reading reports.

> **Where do I start?** Follow the chapters in order. Chapters 1–3 are done once. Chapters 4–9 are your daily work.
> Chapters 10–14 are weekly / monthly work.

---

## Quick map — the complete flow

| Step | What you do | Menu |
|---|---|---|
| 1 | Check company, branch GSTIN, financial year; create users | Admin → System Settings, Masters → Branch Master, Admin → Users & Roles |
| 2 | Create materials, quarries, customers, vehicles, drivers, suppliers | Masters |
| 3 | Set driver daily pay slabs | Finance → Driver Payroll → Daily Pay Slabs |
| 4 | Take a customer order (booking) and approve it | Operations → Bookings |
| 5 | Plan the trip, dispatch the truck, complete delivery (weighbridge + POD) | Operations → Trips & Dispatch |
| 6 | Enter fuel and trip expenses (toll, bata…) and approve them | Operations → Fuel, Operations → Expenses |
| 7 | Make the invoice from completed trips and approve it | Finance → Invoices |
| 8 | Record the customer's payment | Finance → Customer Receipts |
| 9 | Give driver advances; generate, post and pay monthly driver salary | Finance → Driver Payroll |
| 10 | Maintenance: driver reports a problem → work order → parts → complete | Maintenance & Stores |
| 11 | Spare parts stock: receive, issue to jobs, reorder | Maintenance & Stores |
| 12 | Supplier bills and supplier payments | Finance → Supplier Bills & Payments |
| 13 | Check reports, dues and unbilled trips | Reports |
| 14 | Month-end checklist | — |

**Example company used in this guide:** *PKC Transport*, Coimbatore (GSTIN 33ABCPK1234F1Z5).

---

## Before you begin

- **Signing in:** open the TransaFlow link, enter the **username** and **password** given by your administrator.
  You land on the **Dashboard**. Your company initials (for example **PKC**) show above *TransaFlow* in the left menu.
- **Phone use:** tap **☰** (top left) to open the menu.
- **Mandatory fields** are marked with **\***. The Save button stays disabled until they are filled.
- **Dates:** you can enter past dates (e.g. yesterday's trip), but **never future dates**.
- **Numbers are automatic:** booking, trip, invoice, receipt, payroll numbers are created by the system per financial
  year, for example `INV-2627/00001` (financial year 2026-27, invoice 1).
- **Approve = final:** most documents are saved as **Draft / Submitted / Pending** first. Only **Approve** (or **Post**)
  sends them to accounts. After approval they cannot be edited — cancel and create again if needed.
- **Subscription notice:** 3 days before your plan ends, an amber message appears at the top. The company admin can
  click **Renew now**.
- **Roles (who can do what):**

| Role | Typical person | Can do |
|---|---|---|
| Company Admin | Owner / manager | Everything, including users, branches and settings |
| Accountant | Accounts staff | Invoices, receipts, payroll posting, supplier payments, journals, finance reports |
| Branch Manager | Yard in-charge | Operations and most finance screens for his branch |
| Operator | Dispatcher / clerk | Bookings, trips, fuel, expenses, maintenance (not accounts posting) |
| Viewer | Auditor / partner | Can see everything, cannot change anything |
| Driver | Driver (mobile) | Report vehicle problems, see own salary slips |

### One-person company (single login)
If one person runs everything, you need **only the Company Admin login** created when your company was onboarded —
no other users are required.
- The Company Admin can open every menu: masters, bookings, trips, fuel, expenses, invoices, receipts, payroll,
  maintenance, stores, supplier bills, accounts, reports and settings, for **all branches** of the company.
- The admin can create, approve, post and pay their own entries. The *second person must approve* setting is
  automatically ignored while the company has only one active staff login.
- The admin cannot delete or deactivate their own login, and a company always keeps at least one active Company Admin —
  so you can never lock yourself out.
- When staff join later, just add users with the right roles; nothing else changes.

---

## Chapter 1 — One-time company setup (Company Admin)

### 1.1 System Settings — *Admin → System Settings*
**Purpose:** company details used on invoices and numbering.
- **Company Profile:** name, GSTIN, PAN, address, phone, email. *GSTIN is important — it decides GST type.*
- **Financial Years:** a year such as **01-Apr-2026 to 31-Mar-2027** must exist and be **Open**. Nothing can be posted
  to accounts on a date outside an open financial year.
- **Settings:** number prefixes (e.g. `INV-`, `TRP-`, `BKG-`) and options such as
  `REQUIRE_SEPARATE_APPROVER = true` (the person who creates an expense/invoice cannot approve it) and
  `BOOKING_QTY_TOLERANCE_PERCENT` (allow trips to exceed booked quantity by a few %).

### 1.2 Branch Master — *Masters → Branch Master*
**Purpose:** your offices / yards. Every vehicle, driver, user and store belongs to a branch.
- **Mandatory:** Code, Name. **Recommended:** GSTIN, Manager, Phone, Address.
- Example: `HO` — *Head Office, Coimbatore* — GSTIN `33ABCPK1234F1Z5`; `SLM` — *Salem Yard*.
- **Why GSTIN matters:** if the branch GSTIN state (33 = Tamil Nadu) is the same as the customer's state → **CGST + SGST**;
  different state → **IGST**.
- A branch that still has users, vehicles, drivers, customers, stores or transactions **cannot be deleted** — use
  **Deactivate**. The last branch cannot be deleted.

### 1.3 Users & Roles — *Admin → Users & Roles*
Create a login for each staff member: name, username, password (8+ characters), branch, **role**.
For drivers who use the mobile app, create a user with role **DRIVER** and then link it in *Driver Master*.

### 1.4 Dropdown Lists — *Admin → Dropdown Lists*
Vehicle types (Tipper, Trailer), capacities (10 T, 20 T), fuel types, expense categories, units. Add your own values here.

---

## Chapter 2 — Masters (create once, update when needed)

> **Order:** Materials & Quarries → Customers → Vehicles → Drivers → Suppliers.

### 2.1 Material & Quarry — *Masters → Material & Quarry*
**Purpose:** what you carry and where you load it.
- **Materials tab** — *Code\*, Name\*, Category\*, Unit\**, default **material rate**, **transport rate**, **royalty rate**,
  **loading charge** (all *per unit*, usually per ton).
  Example: `MSAND` — *M-Sand* — Ton — material ₹900, transport ₹350, royalty ₹60, loading ₹40.
- **Quarries tab** — quarry name and location, e.g. *Madukkarai Quarry*.
- **Loading Locations** — loading points with charges; **Pricing** — dated material prices; **UOM Conversions**
  (e.g. 1 unit = 4.5 tons).
- **Next:** Customer Master.

### 2.2 Customer Master — *Masters → Customer Master*
**Purpose:** who you deliver to and bill.
- Click **New customer**. **Mandatory:** Customer code\*, Customer name\*.
  **Important:** GSTIN (decides IGST vs CGST+SGST), phone, address, **credit limit**, status.
- Example: `C-SLC` — *Sri Lakshmi Constructions*, GSTIN `33AAXFS1234K1Z5`, credit limit ₹5,00,000.
- After saving, select the customer and add:
  - **Delivery sites** (unloading sites), e.g. *Saravanampatti Site, Coimbatore* — used in bookings.
  - **Contact persons** and **documents** (GST certificate, agreement).
- **Edit:** select the customer → **Edit customer**. **Delete:** only if the customer has no bookings/invoices;
  otherwise set status Inactive.
- **Next:** Vehicle Master.

### 2.3 Vehicle Master — *Masters → Vehicle Master*
**Purpose:** your trucks and their papers.
- Click **New vehicle**. **Mandatory:** Registration number\* (e.g. `TN38AB4521`), Display name\*.
  Also: type, category, capacity, brand, model, chassis/engine no, ownership (own / hired / client), branch,
  **insurance, fitness and permit expiry dates**, current odometer.
- Select a vehicle to add **photo**, **documents** (RC, insurance, permit — with expiry dates),
  see **service history**, and **assign a driver**.
- Expiry dates feed the **Document expiry (compliance)** report — renew before they lapse.
- A vehicle under maintenance (open work order) is blocked from new trips.
- **Delete** only if unused; otherwise make it Inactive.
- **Next:** Driver Master.

### 2.4 Driver Master — *Masters → Driver Master*
**Purpose:** drivers, licences and salary settings.
- **Add Driver** — *Code\*, Name\*, Licence number\**, licence expiry, phone, status, branch, photo.
- In the driver's drawer: documents (licence, Aadhaar), attendance, **salary configuration** (basic salary — use 0 for
  pure trip-based pay), and the **Login** tab — choose the driver's login (a user with the DRIVER role) and click
  **Save login link** so the driver can use the phone app.
- **Next:** Supplier Master (if you buy parts/services on credit) → Driver pay slabs.

### 2.5 Supplier Master — *Masters → Supplier Master*
**Purpose:** workshops, spare-part shops, tyre dealers, fuel pumps.
- **Mandatory:** Code\*, Name\*. Also GSTIN (checked), phone, **credit days** (bill due date = bill date + credit days).
- Example: `SUP-001` — *Sri Murugan Auto Works*, credit 30 days.

### Bulk upload from Excel
Customer, Vehicle, Driver and Supplier masters, Materials, Spare Parts and Stock (opening stock) have an **Upload** button.
1. Click **Upload** → **Download template** (dropdown columns such as Ownership, Branch, Unit already list the allowed values).
2. Fill one row per record (columns with * are required), save, then **Choose file** → **Upload**.
3. Every row is checked: required fields, formats (dates, GSTIN, phone, numbers), dropdown values, duplicates inside the file
   and records that already exist. Each row shows **Valid** or **Invalid** with the exact reason.
4. Fix the Excel and upload again, or click **Remove invalid**. **Create** is enabled only when every remaining row is valid;
   all rows are created together — if anything fails, nothing is saved.

---

## Chapter 3 — Driver daily pay slabs — *Finance → Driver Payroll → Daily Pay Slabs*
**Purpose:** how much a driver earns per day based on completed trips that day.
- Example: **1 trip = ₹500**, **2 or more trips = ₹1,000** (leave *Trips To* empty on the last slab).
- The system never multiplies per trip — 4 trips in a day still earn one day's slab amount.
- Only admins / accountants can change slabs.

---

## Chapter 4 — Booking (customer order) — *Operations → Bookings*
**Purpose:** record what the customer ordered.
1. Click **Register Booking Request**.
2. Choose **Customer\*** and **Delivery site**, priority.
3. Add material lines: **Material\***, **Quantity\*** (e.g. 60 tons), **Material rate\***, **Transport rate\***,
   **Royalty\***, **Loading\***, **GST %\*** (e.g. 5). Default rates come from the material master.
4. Save → status **PENDING**. Booking value = quantity × (material + transport + royalty + loading) + GST.
5. **Approve** the booking. *Trips can only be planned on approved bookings.*

**Rules to know**
- A booking with trips cannot be rejected; its quantity cannot be reduced below what trips already moved.
- When all booked quantity is delivered the booking becomes **COMPLETED** automatically.
- **Close** an approved booking early when the customer needs no more loads (no trips may be on the road).
- Attach the customer's **purchase order** in the booking.

**Next:** plan trips.

---

## Chapter 5 — Trip, dispatch and delivery — *Operations → Trips & Dispatch*
**Purpose:** each truck load from quarry to site.

### 5.1 Plan the trip
Click **Plan Dispatch Trip** and enter:
- **Booking\*** (only approved bookings are listed), **Trip date\*** (not future, not before the booking date),
- **Vehicle** and **Driver** (needed before dispatch), **Quarry** and **Loading point**,
- Material line(s): **Material\*** (must be on the booking) and **Quantity\*** (e.g. 20 tons).

Save → status **PLANNED**, number like `TRP-2627/00001`.
The total of all trips cannot exceed the booked quantity (plus the tolerance %).

### 5.2 Dispatch → Complete
- **Dispatch** when the truck leaves the quarry (vehicle + driver required) → **DISPATCHED**.
- **Complete** when it reaches the site → **COMPLETED**.
- Order is fixed: PLANNED → DISPATCHED → COMPLETED. Only a PLANNED trip can be cancelled.

### 5.3 Weighbridge and POD
Open the completed trip (**Edit**):
- **Loaded weight** (quarry weighbridge) and **Delivered weight** (site weighbridge), e.g. 20.00 / 19.80 →
  **shortage 0.20 t**. Delivered cannot be more than loaded. **The delivered weight is what gets billed.**
- Upload **POD / delivery challan / LR** in the trip documents.
- Once the trip is on an invoice it is locked.

**Next:** fuel & expenses, then invoice.

---

## Chapter 6 — Fuel and expenses

### 6.1 Fuel — *Operations → Fuel*
- **Fuel Entry Logs → Add Fuel Entry:** vehicle, driver, **link the trip**, **fuel station\***, **litres\***,
  **rate per litre\***, **previous\*** and **current odometer\***, **payment method\***, date, bill photo.
- Saved as **DRAFT** → **Approve** posts it to accounts (Fuel Expense / Cash or Bank).
- Only approved fuel counts in cost reports. Linking the trip gives correct **trip profit**.
- **Fuel Requests:** a driver/dispatcher can request fuel for a trip; the request is approved and then fulfilled by a fuel entry.

### 6.2 Expenses — *Operations → Expenses*
- **Record Expense Voucher:** **Expense date** (can be past, not future), **Category\*** (Toll, Driver Bata, Parking,
  Vehicle Repair, Insurance, Office…), vehicle, driver, **trip**, **Amount\***, **GST amount**, **Payment method\***,
  **Description\***. Attach the bill.
- Status **SUBMITTED** → **Approve** posts to accounts (on the expense date). Reject if wrong.
- *Driver Bata* expenses appear on the driver's salary slip for information (they are already paid).
- If `REQUIRE_SEPARATE_APPROVER` is on, a different user must approve.

---

## Chapter 7 — Invoice — *Finance → Invoices*
**Purpose:** bill the customer for completed trips.
1. Open **Ready for billing** — all completed trips not yet invoiced.
2. Tick the trips of **one customer** (or *select all of this customer*) → **One invoice for N trips**.
   (Or use the **Generate** button on a single trip.)
3. A **DRAFT** invoice is created: quantity = delivered weight; rates from the booking.
   Line value = qty × (rate + freight + loading + royalty). **Discount is taken before GST.**
4. **Edit** the draft if needed: invoice date (not future, not before the trip date), place of supply (2-digit state code,
   e.g. 33), discount, payment terms.
5. **Approve** → posts **Customer Receivable** / **Freight Income** + **GST Liability**. Number `INV-2627/000xx`.
6. **Print / PDF** and attach the **signed copy / e-way bill**.

**GST example:** 19.8 t × ₹1,350 = ₹26,730 taxable; GST 5% = ₹1,336.50 → CGST ₹668.25 + SGST ₹668.25
(same state) → total ₹28,066.50. For an out-of-state customer the tax is IGST ₹1,336.50.

**Rules:** a trip can be billed only once; one invoice per customer; cancelling an approved invoice reverses its
accounts; the **Unbilled completed trips** report shows trips you forgot to bill.

**Next:** collect payment.

---

## Chapter 8 — Customer receipt — *Finance → Customer Receipts*
**Purpose:** record money received from customers.
1. **Record Payment Receipt:** **Customer\***, date, **Amount received\***, **Payment method\*** (Cash / Bank / UPI /
   Cheque), reference (UTR / cheque no).
2. Allocate the amount to the customer's **open invoices** (oldest first). Any extra stays as **advance**.
3. Save → **Approve**. Invoices become **PAID** / **PARTIALLY PAID**; accounts: Cash/Bank ↔ Customer Receivable.
4. Attach the cheque copy / UPI screenshot. See the **Customer Ledger** tab for the running balance.

### Settlement Dashboard (Customer Receipts → Settlement Dashboard)
- **Received in period / Settled against invoices / Advance not yet applied** follow the period buttons (This month, Last month,
  Financial year, All time). **Customers owe** and **Overdue** are always as of today.
- **Dues by age** — click a box (Not yet due, 1–30, 31–60 … days late) to see those invoices.
- **Customers with dues** (largest first): **Receive** opens a receipt for that customer, **Ledger** shows their running
  balance, **Apply advance** uses money they paid in advance against their oldest open invoices.
- **Credit limit:** a booking cannot be approved if the customer's dues plus the booking value would exceed their credit
  limit (0 = no limit). Collect payment or raise the limit in Customer Master.

**Next:** driver settlement (monthly).

---

## Chapter 9 — Driver advances and salary — *Finance → Driver Payroll*

### 9.1 Advances (any time)
**Advances tab → Give Advance:** driver, amount, date, paid by (cash / bank / UPI). Posted to *Driver Advances*.
An advance can be cancelled only if nothing has been recovered yet.

### 9.2 Monthly payroll
1. **Generate Payroll:** driver, **month**, year; optional allowance, **advance recovery** (not more than outstanding),
   deductions (fine / damage / other with reason).
2. The system calculates from **completed trips** in that month:
   day-wise trips → slab amount per day → **trip earnings** + basic + allowance = **gross**;
   **net = gross − deductions − advance recovery**.
   Example: Day 1 = 1 trip ₹500, Day 2 = 2 trips ₹1,000, Day 3 = 4 trips ₹1,000 → gross ₹2,500; advance ₹500 → net ₹2,000.
3. **Approve** (trips re-read) → **Post to Accounts** (salary expense, advance recovered) → **Pay Salary**
   (cash / bank, date, reference).
4. **Salary Slip** PDF shows the daily breakdown. Drivers see their own posted slips under **My Salary**.

**Rules:** one active payroll per driver per month; future months not allowed; cancel reverses all entries and returns
advance recovery.

---

## Chapter 10 — Maintenance — *Maintenance & Stores*
1. **Maintenance Requests:** a driver (phone) or staff clicks **Report a problem** — vehicle, problem, priority,
   odometer, damage photos. Status **OPEN**.
2. Office **Reviews** → **Approves** → **Converts to Work Order** (or cancels, e.g. fixed on the road).
3. **Work Orders:** add **parts** (spare part + quantity) and **labour** lines; **Issue** parts from a warehouse
   (stock goes down; a returned part can be issued again); **Start** the job.
4. **Complete:** enter the **actual cost** (parts from stock + labour + outside charges) and **how the outside cost
   was paid** (Credit = workshop bill, Cash, Bank). Parts cost moves from inventory to repair expense; the outside part
   becomes a supplier bill (credit) or a cash/bank payment.
5. A vehicle with an open work order cannot be put on new trips. A work order still holding issued parts cannot be
   cancelled — return the parts first.

---

## Chapter 11 — Spare parts and stock — *Maintenance & Stores*
- **Spare Parts:** code, name, unit, default rate, **reorder level**, photo.
- **Warehouses:** your stores (one per branch or yard).
- **Stock → Opening stock:** existing quantity with **unit cost**. **Receive stock:** warehouse, part, quantity,
  **rate**, supplier, bill number and **payment mode** (Credit → a supplier bill is created automatically; Cash / Bank).
- Stock shows **average cost**, **value** and status **OK / REORDER / OUT**. Old stock without a cost can be valued
  once with **Set cost**.
- **Inventory Transactions:** every receipt, issue and return with rate and work order.

---

## Chapter 12 — Supplier bills & payments — *Finance → Supplier Bills & Payments*
- **Bills:** created automatically (credit stock receipts, credit workshop jobs) or **New bill** for workshop / tyre /
  office bills: supplier, their bill no, date, due date (auto from credit days), category, taxable amount, GST.
  Save as draft → **Approve** (posts expense + GST input / Accounts Payable).
- **Pay supplier:** supplier, date, amount, method, reference; allocate to bills (or oldest first automatically).
  Cancelling a payment re-opens the bills.
- The **Supplier outstanding & ageing** report shows what you owe and what is overdue.

---

## Chapter 13 — Reports — *Reports → Reports*
Pick a report on the left, set the date range (Today / This month / Last month / Financial year) and filters, click
**Run**, then **Excel** or **PDF**. Useful reports:

| Need | Report |
|---|---|
| Owner's daily view | **Management KPIs**, **Monthly business summary** |
| Trips not yet billed | **Unbilled completed trips** |
| Who owes us | **Customer outstanding & ageing** |
| What we owe | **Supplier outstanding & ageing** |
| GST returns | **Sales register (GST)**, **GST summary (rate-wise)** |
| Truck performance | **Vehicle performance & profitability**, **Fuel efficiency**, **Trip profitability** |
| Losses in transit | **Weighbridge shortage** |
| Drivers | **Driver performance**, **Driver payroll register**, **Driver advances** |
| Maintenance & stores | **Work order register**, **Maintenance cost by vehicle**, **Stock valuation**, **Reorder list** |
| Accounts | **Day book**, **Account ledger / cash & bank book**, **Account balances**; P&L and Balance Sheet in **Financial Statements** |
| Pending work | **Pending approvals & actions**, **Document expiry** |

Every list screen also has **Excel** and **PDF** buttons.

---

## Chapter 14 — Month-end checklist
1. **Pending approvals** report → clear draft bookings, invoices, receipts, expenses, fuel, payroll.
2. **Unbilled completed trips** = 0 (invoice everything delivered).
3. Approve all fuel and expenses of the month; link them to trips (**Unlinked costs** report).
4. Generate → approve → post → pay **driver payroll** for the month.
5. Record all **customer receipts**; follow up **Customer outstanding** (61–90 and 90+ days first).
6. Pay due **supplier bills**.
7. Check **Document expiry** (insurance / fitness / permit / licence).
8. Download **Sales register** and **GST summary** for GST filing; review **Monthly business summary**.

---

## Common questions

| Question | Answer |
|---|---|
| Trip screen does not show my booking | The booking is not **Approved**, or it is already completed/closed. |
| "Booking quantity exceeded" | Trips already moved the booked quantity. Increase the booking quantity or set the tolerance %. |
| Cannot dispatch | Select a vehicle and a driver on the trip first. |
| Trip is not in *Ready for billing* | The trip must be **COMPLETED** and not already on an invoice. |
| Invoice shows IGST instead of CGST+SGST | Customer GSTIN (or place of supply) is in another state than your branch GSTIN. |
| "Future-dated posting" | Use today's date or an earlier date. |
| "Second person must approve" | Your company requires a different user to approve what you created. |
| Cannot delete a customer / vehicle / branch | It is already used. Set it to **Inactive** instead. |
| Payroll says "Pay slabs not configured" | Set the daily pay slabs first (Chapter 3). |
| Only the admin can log in | The subscription has ended — the admin must **Renew**. |
