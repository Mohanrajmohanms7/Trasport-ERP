"""DB-only: one sample row per missing PKR module for admin_pkr001 (company_id=7)."""
import psycopg2
from datetime import date, datetime
from decimal import Decimal

CONN = dict(
    host="localhost",
    port=5432,
    dbname="transport_erp",
    user="transport_admin",
    password="AdminPass123",
)

COMPANY_ID = 7
BRANCH_ID = 7
USERNAME = "admin_pkr001"
NOW = datetime.now()
TODAY = date.today()

# Existing PKR masters (already in DB)
CUSTOMER_ID = 29
SITE_ID = 26
VEHICLE_ID = 7
DRIVER_ID = 6

# PKR lookup ids
UNIT_TON = 247
CAT_AGGREGATE = 244
VEH_TYPE_TIPPER = 235
VEH_CAT_HEAVY = 237
VEH_CAP_16 = 239


def fetch_one(cur, sql, params=()):
    cur.execute(sql, params)
    row = cur.fetchone()
    return row[0] if row else None


def main():
    conn = psycopg2.connect(**CONN)
    cur = conn.cursor()

    cur.execute(
        "SELECT id, username, company_id, branch_id FROM app_users WHERE username=%s AND is_deleted=false",
        (USERNAME,),
    )
    user = cur.fetchone()
    if not user:
        raise SystemExit("admin_pkr001 not found")
    print(f"OK user={user[1]} company_id={user[2]} branch_id={user[3]}")

    # --- Material ---
    material_id = fetch_one(
        cur,
        "SELECT id FROM materials WHERE company_id=%s AND code=%s AND is_deleted=false",
        (COMPANY_ID, "MAT000001"),
    )
    if not material_id:
        cur.execute(
            """
            INSERT INTO materials (
              code, name, description, category_id, unit_id, default_rate, density,
              status, company_id, branch_id, created_by, created_date, updated_by,
              updated_date, is_deleted, version
            ) VALUES (
              %s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,false,0
            ) RETURNING id
            """,
            (
                "MAT000001",
                "20 MM Blue Metal",
                "PKR demo material",
                CAT_AGGREGATE,
                UNIT_TON,
                Decimal("850.00"),
                None,
                "ACTIVE",
                COMPANY_ID,
                BRANCH_ID,
                USERNAME,
                NOW,
                USERNAME,
                NOW,
            ),
        )
        material_id = cur.fetchone()[0]
        print(f"Inserted material id={material_id}")
    else:
        print(f"Material exists id={material_id}")

    # --- Material price ---
    price_id = fetch_one(
        cur,
        "SELECT id FROM material_prices WHERE company_id=%s AND material_id=%s AND is_deleted=false",
        (COMPANY_ID, material_id),
    )
    if not price_id:
        cur.execute(
            """
            INSERT INTO material_prices (
              material_id, material_rate, transport_rate, royalty_rate, loading_charge,
              effective_date, description, is_deleted, version, created_by, created_date,
              updated_by, updated_date, code, name, status, company_id, branch_id
            ) VALUES (
              %s,%s,%s,%s,%s,%s,%s,false,0,%s,%s,%s,%s,%s,%s,%s,%s,%s
            ) RETURNING id
            """,
            (
                material_id,
                Decimal("850.00"),
                Decimal("450.00"),
                Decimal("35.00"),
                Decimal("120.00"),
                TODAY,
                "PKR demo price",
                USERNAME,
                NOW,
                USERNAME,
                NOW,
                "MPR000001",
                "20 MM price",
                "ACTIVE",
                COMPANY_ID,
                BRANCH_ID,
            ),
        )
        price_id = cur.fetchone()[0]
        print(f"Inserted material_price id={price_id}")
    else:
        print(f"Material price exists id={price_id}")

    # --- Quarry ---
    quarry_id = fetch_one(
        cur,
        "SELECT id FROM quarries WHERE company_id=%s AND code=%s AND is_deleted=false",
        (COMPANY_ID, "QRY000001"),
    )
    if not quarry_id:
        cur.execute(
            """
            INSERT INTO quarries (
              code, name, description, location_address, status, company_id, branch_id,
              created_by, created_date, updated_by, updated_date, is_deleted, version,
              owner_name, contact_number, gst_number
            ) VALUES (
              %s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,false,0,%s,%s,%s
            ) RETURNING id
            """,
            (
                "QRY000001",
                "PKR Main Quarry",
                "PKR demo quarry",
                "Quarry Road, Perambalur",
                "ACTIVE",
                COMPANY_ID,
                BRANCH_ID,
                USERNAME,
                NOW,
                USERNAME,
                NOW,
                "Quarry Owner",
                "9876501001",
                "33AAKPKRQ001A1Z5",
            ),
        )
        quarry_id = cur.fetchone()[0]
        print(f"Inserted quarry id={quarry_id}")
    else:
        print(f"Quarry exists id={quarry_id}")

    # --- Loading location ---
    location_id = fetch_one(
        cur,
        "SELECT id FROM loading_locations WHERE company_id=%s AND location_code=%s AND is_deleted=false",
        (COMPANY_ID, "LOC000001"),
    )
    if not location_id:
        cur.execute(
            """
            INSERT INTO loading_locations (
              location_code, loading_point, loading_charges, description, is_deleted,
              version, created_by, created_date, updated_by, updated_date, code, name,
              status, company_id, branch_id
            ) VALUES (
              %s,%s,%s,%s,false,0,%s,%s,%s,%s,%s,%s,%s,%s,%s
            ) RETURNING id
            """,
            (
                "LOC000001",
                "PKR Main Quarry Yard",
                Decimal("120.00"),
                "PKR demo loading point",
                USERNAME,
                NOW,
                USERNAME,
                NOW,
                "LOC000001",
                "PKR Main Quarry Yard",
                "ACTIVE",
                COMPANY_ID,
                BRANCH_ID,
            ),
        )
        location_id = cur.fetchone()[0]
        print(f"Inserted loading_location id={location_id}")
    else:
        print(f"Loading location exists id={location_id}")

    # --- Supplier ---
    supplier_id = fetch_one(
        cur,
        "SELECT id FROM suppliers WHERE company_id=%s AND code=%s AND is_deleted=false",
        (COMPANY_ID, "SUP000001"),
    )
    if not supplier_id:
        cur.execute(
            """
            INSERT INTO suppliers (
              code, name, description, email, phone, address, gst_number, status,
              company_id, branch_id, created_by, created_date, updated_by, updated_date,
              is_deleted, version
            ) VALUES (
              %s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,false,0
            ) RETURNING id
            """,
            (
                "SUP000001",
                "PKR Highway Fuel Pump",
                "PKR demo supplier",
                "pump@pkr.local",
                "9876501002",
                "NH Road, Perambalur",
                "33AAKPKRS001A1Z5",
                "ACTIVE",
                COMPANY_ID,
                BRANCH_ID,
                USERNAME,
                NOW,
                USERNAME,
                NOW,
            ),
        )
        supplier_id = cur.fetchone()[0]
        print(f"Inserted supplier id={supplier_id}")
    else:
        print(f"Supplier exists id={supplier_id}")

    # Patch vehicle type/category/capacity if missing
    cur.execute(
        """
        UPDATE vehicles
        SET type_id = COALESCE(type_id, %s),
            category_id = COALESCE(category_id, %s),
            capacity_id = COALESCE(capacity_id, %s),
            branch_id = COALESCE(branch_id, %s),
            status = 'AVAILABLE',
            updated_by = %s,
            updated_date = %s
        WHERE id = %s AND company_id = %s
        """,
        (
            VEH_TYPE_TIPPER,
            VEH_CAT_HEAVY,
            VEH_CAP_16,
            BRANCH_ID,
            USERNAME,
            NOW,
            VEHICLE_ID,
            COMPANY_ID,
        ),
    )

    # --- Booking ---
    booking_id = fetch_one(
        cur,
        "SELECT id FROM bookings WHERE company_id=%s AND booking_number=%s AND is_deleted=false",
        (COMPANY_ID, "BKG260001001"),
    )
    if not booking_id:
        cur.execute(
            """
            INSERT INTO bookings (
              booking_number, booking_date, customer_id, delivery_site_id, status,
              priority, remarks, description, is_deleted, version, created_by,
              created_date, updated_by, updated_date, company_id, branch_id, code, name
            ) VALUES (
              %s,%s,%s,%s,%s,%s,%s,%s,false,0,%s,%s,%s,%s,%s,%s,%s,%s
            ) RETURNING id
            """,
            (
                "BKG260001001",
                TODAY,
                CUSTOMER_ID,
                SITE_ID,
                "APPROVED",
                "MEDIUM",
                "PKR single-entry demo booking",
                "16 ton blue metal delivery",
                USERNAME,
                NOW,
                USERNAME,
                NOW,
                COMPANY_ID,
                BRANCH_ID,
                "BKG000001",
                "PKR Demo Booking",
            ),
        )
        booking_id = cur.fetchone()[0]
        print(f"Inserted booking id={booking_id}")
    else:
        print(f"Booking exists id={booking_id}")

    # --- Booking detail ---
    detail_id = fetch_one(
        cur,
        "SELECT id FROM booking_details WHERE booking_id=%s AND is_deleted=false",
        (booking_id,),
    )
    qty = Decimal("16.00")
    rate = Decimal("850.00")
    transport = Decimal("450.00")
    royalty = Decimal("35.00")
    loading = Decimal("120.00")
    gst_pct = Decimal("18.00")
    # net similar to AKS pattern: qty * (rate+transport+royalty+loading) * (1+gst/100)
    base = qty * (rate + transport + royalty + loading)
    net_amount = (base * (Decimal("1") + gst_pct / Decimal("100"))).quantize(Decimal("0.01"))
    if not detail_id:
        cur.execute(
            """
            INSERT INTO booking_details (
              booking_id, material_id, quantity, rate, transport_rate, royalty_rate,
              loading_charge, gst_percentage, net_amount, description, is_deleted,
              version, created_by, created_date, updated_by, updated_date, code, name,
              status, company_id, branch_id
            ) VALUES (
              %s,%s,%s,%s,%s,%s,%s,%s,%s,%s,false,0,%s,%s,%s,%s,%s,%s,%s,%s,%s
            ) RETURNING id
            """,
            (
                booking_id,
                material_id,
                qty,
                rate,
                transport,
                royalty,
                loading,
                gst_pct,
                net_amount,
                "PKR demo booking line",
                USERNAME,
                NOW,
                USERNAME,
                NOW,
                "BD000001",
                "Booking detail",
                "ACTIVE",
                COMPANY_ID,
                BRANCH_ID,
            ),
        )
        detail_id = cur.fetchone()[0]
        print(f"Inserted booking_detail id={detail_id} net={net_amount}")
    else:
        print(f"Booking detail exists id={detail_id}")

    # --- Trip ---
    trip_id = fetch_one(
        cur,
        "SELECT id FROM trips WHERE company_id=%s AND trip_number=%s AND is_deleted=false",
        (COMPANY_ID, "TRP260001001"),
    )
    if not trip_id:
        cur.execute(
            """
            INSERT INTO trips (
              trip_number, trip_date, booking_id, vehicle_id, driver_id, status,
              remarks, description, is_deleted, version, created_by, created_date,
              updated_by, updated_date, company_id, branch_id, code, name
            ) VALUES (
              %s,%s,%s,%s,%s,%s,%s,%s,false,0,%s,%s,%s,%s,%s,%s,%s,%s
            ) RETURNING id
            """,
            (
                "TRP260001001",
                TODAY,
                booking_id,
                VEHICLE_ID,
                DRIVER_ID,
                "COMPLETED",
                "PKR single-entry demo trip",
                "Completed delivery to PKR site",
                USERNAME,
                NOW,
                USERNAME,
                NOW,
                COMPANY_ID,
                BRANCH_ID,
                "TRP000001",
                "PKR Demo Trip",
            ),
        )
        trip_id = cur.fetchone()[0]
        print(f"Inserted trip id={trip_id}")
    else:
        print(f"Trip exists id={trip_id}")

    # --- Fuel ---
    fuel_id = fetch_one(
        cur,
        "SELECT id FROM fuel_entries WHERE company_id=%s AND fuel_entry_number=%s AND is_deleted=false",
        (COMPANY_ID, "FUEL000001"),
    )
    if not fuel_id:
        cur.execute(
            """
            INSERT INTO fuel_entries (
              fuel_entry_number, fuel_date, vehicle_id, driver_id, trip_id, fuel_station,
              fuel_quantity, rate_per_litre, total_amount, payment_method, invoice_number,
              current_odometer, previous_odometer, remarks, description, is_deleted,
              version, created_by, created_date, updated_by, updated_date, code, name,
              status, company_id, branch_id
            ) VALUES (
              %s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,false,0,%s,%s,%s,%s,%s,%s,%s,%s,%s
            ) RETURNING id
            """,
            (
                "FUEL000001",
                TODAY,
                VEHICLE_ID,
                DRIVER_ID,
                trip_id,
                "PKR Highway Fuel Pump",
                Decimal("40.00"),
                Decimal("95.00"),
                Decimal("3800.00"),
                "UPI",
                "FUEL-INV-001",
                Decimal("10085"),
                Decimal("10000"),
                "PKR demo fuel",
                "Diesel fill for demo trip",
                USERNAME,
                NOW,
                USERNAME,
                NOW,
                "FUEL000001",
                "Fuel entry #1",
                "ACTIVE",
                COMPANY_ID,
                BRANCH_ID,
            ),
        )
        fuel_id = cur.fetchone()[0]
        print(f"Inserted fuel id={fuel_id}")
    else:
        print(f"Fuel exists id={fuel_id}")

    # --- Expense ---
    expense_id = fetch_one(
        cur,
        "SELECT id FROM expenses WHERE company_id=%s AND expense_number=%s AND is_deleted=false",
        (COMPANY_ID, "EXP000001"),
    )
    if not expense_id:
        cur.execute(
            """
            INSERT INTO expenses (
              expense_number, expense_date, category, vehicle_id, driver_id, trip_id,
              description, amount, gst_amount, total_amount, payment_method, status,
              remarks, is_deleted, version, created_by, created_date, updated_by,
              updated_date, company_id, branch_id, code, name
            ) VALUES (
              %s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,false,0,%s,%s,%s,%s,%s,%s,%s,%s
            ) RETURNING id
            """,
            (
                "EXP000001",
                TODAY,
                "TOLL",
                VEHICLE_ID,
                DRIVER_ID,
                trip_id,
                "Toll for PKR demo trip",
                Decimal("350.00"),
                Decimal("0.00"),
                Decimal("350.00"),
                "UPI",
                "APPROVED",
                "PKR demo expense",
                USERNAME,
                NOW,
                USERNAME,
                NOW,
                COMPANY_ID,
                BRANCH_ID,
                "EXP000001",
                "Expense #1",
            ),
        )
        expense_id = cur.fetchone()[0]
        print(f"Inserted expense id={expense_id}")
    else:
        print(f"Expense exists id={expense_id}")

    # --- Sales invoice ---
    invoice_id = fetch_one(
        cur,
        "SELECT id FROM sales_invoices WHERE company_id=%s AND invoice_number=%s AND is_deleted=false",
        (COMPANY_ID, "INV260001001"),
    )
    subtotal = base.quantize(Decimal("0.01"))
    discount = Decimal("0.00")
    invoice_net = net_amount
    if not invoice_id:
        cur.execute(
            """
            INSERT INTO sales_invoices (
              invoice_number, invoice_date, customer_id, status, payment_terms,
              subtotal, discount, net_amount, is_deleted, version, created_by,
              created_date, updated_by, updated_date, company_id, branch_id, code,
              name, description
            ) VALUES (
              %s,%s,%s,%s,%s,%s,%s,%s,false,0,%s,%s,%s,%s,%s,%s,%s,%s,%s
            ) RETURNING id
            """,
            (
                "INV260001001",
                TODAY,
                CUSTOMER_ID,
                "GENERATED",
                "NET_30",
                subtotal,
                discount,
                invoice_net,
                USERNAME,
                NOW,
                USERNAME,
                NOW,
                COMPANY_ID,
                BRANCH_ID,
                "INV000001",
                "PKR Demo Invoice",
                "Invoice for PKR demo trip",
            ),
        )
        invoice_id = cur.fetchone()[0]
        print(f"Inserted invoice id={invoice_id}")
    else:
        print(f"Invoice exists id={invoice_id}")

    inv_detail_id = fetch_one(
        cur,
        "SELECT id FROM sales_invoice_details WHERE invoice_id=%s AND is_deleted=false",
        (invoice_id,),
    )
    if not inv_detail_id:
        freight = (qty * transport).quantize(Decimal("0.01"))
        loading_amt = (qty * loading).quantize(Decimal("0.01"))
        royalty_amt = (qty * royalty).quantize(Decimal("0.01"))
        taxable = subtotal
        cgst = (taxable * Decimal("0.09")).quantize(Decimal("0.01"))
        sgst = cgst
        igst = Decimal("0.00")
        cur.execute(
            """
            INSERT INTO sales_invoice_details (
              invoice_id, trip_id, material_id, quantity, rate, freight_charges,
              loading_charges, royalty, gst_percentage, cgst, sgst, igst, net_amount,
              is_deleted, version, created_by, created_date, updated_by, updated_date,
              code, name, description, status, company_id, branch_id
            ) VALUES (
              %s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,false,0,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s
            ) RETURNING id
            """,
            (
                invoice_id,
                trip_id,
                material_id,
                qty,
                rate,
                freight,
                loading_amt,
                royalty_amt,
                gst_pct,
                cgst,
                sgst,
                igst,
                invoice_net,
                USERNAME,
                NOW,
                USERNAME,
                NOW,
                "IND000001",
                "Invoice line",
                "PKR demo invoice line",
                "ACTIVE",
                COMPANY_ID,
                BRANCH_ID,
            ),
        )
        inv_detail_id = cur.fetchone()[0]
        print(f"Inserted invoice detail id={inv_detail_id}")
    else:
        print(f"Invoice detail exists id={inv_detail_id}")

    # --- Receipt / payment ---
    receipt_id = fetch_one(
        cur,
        "SELECT id FROM customer_receipts WHERE company_id=%s AND receipt_number=%s AND is_deleted=false",
        (COMPANY_ID, "RCT000001"),
    )
    if not receipt_id:
        cur.execute(
            """
            INSERT INTO customer_receipts (
              receipt_number, receipt_date, customer_id, booking_id, amount_received,
              advance_amount, payment_method, reference_number, remarks, description,
              is_deleted, version, created_by, created_date, updated_by, updated_date,
              company_id, branch_id, code, name, status
            ) VALUES (
              %s,%s,%s,%s,%s,%s,%s,%s,%s,%s,false,0,%s,%s,%s,%s,%s,%s,%s,%s,%s
            ) RETURNING id
            """,
            (
                "RCT000001",
                TODAY,
                CUSTOMER_ID,
                booking_id,
                Decimal("5000.00"),
                Decimal("0.00"),
                "UPI",
                "UPI-PKR-DEMO-001",
                "Partial payment against PKR demo invoice",
                "PKR demo receipt",
                USERNAME,
                NOW,
                USERNAME,
                NOW,
                COMPANY_ID,
                BRANCH_ID,
                "RCT000001",
                "Receipt #1",
                "ACTIVE",
            ),
        )
        receipt_id = cur.fetchone()[0]
        print(f"Inserted receipt id={receipt_id}")
    else:
        print(f"Receipt exists id={receipt_id}")

    conn.commit()
    print("\n=== COMMITTED: PKR company_id=7 single entries ===")
    print(
        {
            "company_id": COMPANY_ID,
            "customer_id": CUSTOMER_ID,
            "site_id": SITE_ID,
            "vehicle_id": VEHICLE_ID,
            "driver_id": DRIVER_ID,
            "material_id": material_id,
            "price_id": price_id,
            "quarry_id": quarry_id,
            "location_id": location_id,
            "supplier_id": supplier_id,
            "booking_id": booking_id,
            "booking_detail_id": detail_id,
            "trip_id": trip_id,
            "fuel_id": fuel_id,
            "expense_id": expense_id,
            "invoice_id": invoice_id,
            "invoice_detail_id": inv_detail_id,
            "receipt_id": receipt_id,
        }
    )
    cur.close()
    conn.close()


if __name__ == "__main__":
    main()
