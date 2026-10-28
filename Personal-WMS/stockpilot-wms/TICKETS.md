# StockPilot ticket backlog

These are written the way they'd arrive at work: from users who describe what they saw,
not where the code is wrong. Each ticket has hints hidden in a collapsed section. Try
without them first; open one hint at a time if you're stuck.

**Recommended order:** WMS-101, WMS-102, WMS-401, WMS-103, WMS-201, WMS-104, WMS-105,
WMS-402, WMS-208, WMS-301, WMS-203, then anything you like. WMS-106 and WMS-205 are the
hardest; save them for last.

**Definition of done for every ticket**
- The problem is reproduced (in the app or in a test) before it's fixed.
- A test covers the fix or the new behavior.
- `./mvnw test` passes.
- One branch and one pull request per ticket, with the ticket number in the commit message.

---

## Bugs

### WMS-101: PO total shows a strange long number
**Type:** Bug | **Priority:** Medium | **Difficulty:** Easy
**Reported by:** Purchasing

> Open PO-00001. The order total says $4.449999999999999. It should be $4.45. Accounting
> noticed totals are sometimes off by a fraction of a cent, and they don't trust the report.

**Acceptance criteria**
- The total for PO-00001 shows exactly $4.45.
- The test `PurchaseOrderServiceTest.orderTotalIsExact` passes.
- The fix is in how the total is calculated, not only in how it's displayed.

<details><summary>Hint 1</summary>

Look at how `calculateTotal` adds things up and what type it uses along the way.
</details>
<details><summary>Hint 2</summary>

`double` can't represent most decimal fractions exactly (0.1 is really 0.1000000000000000055...).
Money and quantities in business systems use `BigDecimal` end to end. Look at how
`SalesOrderService.calculateTotal` does it.
</details>

---

### WMS-102: Purchase order stays "Partially received" forever
**Type:** Bug | **Priority:** High | **Difficulty:** Easy
**Reported by:** Receiving dock

> PO-00002 was for 10 cases of tape. Truck 1 brought 6 cases, truck 2 brought the other 4.
> We received both. The PO still says "Partially received" and nothing is left to receive,
> so it never closes. If the whole order comes on one truck, it closes fine.

**Steps to reproduce:** On PO-00002, receive 6, then receive 4.

**Acceptance criteria**
- After all quantity is received, across any number of receipts, the line is Received and
  the PO is Received.
- The test `PurchaseOrderServiceTest.partialReceiptsThatCompleteTheLineCloseTheOrder` passes.

<details><summary>Hint 1</summary>

Find where a line's status is decided during receiving. What number is it comparing?
</details>
<details><summary>Hint 2</summary>

It compares the quantity of *this receipt* to the ordered quantity, instead of the
*total received so far*.
</details>

---

### WMS-103: Inventory export is jumbled in Excel
**Type:** Bug | **Priority:** Medium | **Difficulty:** Easy
**Reported by:** Inventory control

> Inventory > Export CSV. When I open it in Excel, some rows are shifted: the location
> shows up under "description" and the quantity is under "zone". Bolts, nuts and gloves
> are wrong; zip ties look fine.

**Acceptance criteria**
- Every row in the export has exactly 6 columns, whatever the description contains.
- A test proves a description containing commas and quotes exports correctly.

<details><summary>Hint 1</summary>

What do the broken parts' descriptions have in common?
</details>
<details><summary>Hint 2</summary>

CSV fields that contain commas, quotes, or line breaks must be wrapped in double quotes,
and any quote inside must be doubled (`"` becomes `""`). That's RFC 4180. Write a small
`escape(String)` helper, or use a library like Apache Commons CSV.
</details>

---

### WMS-104: Bin shows negative quantity after picking
**Type:** Bug | **Priority:** High | **Difficulty:** Medium
**Reported by:** Warehouse supervisor

> A picker picked all 12 gloves for SO-00001 from B-01-01. The system let them, but that
> bin only had 10. Now B-01-01 shows -2 and B-01-02 still shows 40. Physically there are
> 0 in B-01-01. The system should have stopped the pick.

**Steps to reproduce:** Floor screens > Pick > SO-00001 > pick 12 from B-01-01.

**Acceptance criteria**
- A pick can't take more than the chosen location holds. The picker sees a clear message
  saying how many are in that location.
- Picking from a location where the part has never been stocked is also rejected.
- Tests cover both cases.
- Bonus: write a SQL query that finds any rows already negative, and decide how to fix
  that existing data (an adjustment, with a reason, is how ERPs usually do it).

<details><summary>Hint 1</summary>

Compare how `InventoryService.transfer` checks stock with how picking checks stock.
</details>
<details><summary>Hint 2</summary>

The pick validation uses the part's total on hand across *all* locations instead of the
quantity in the location being picked from.
</details>

---

### WMS-105: Available quantity is wrong after cancelling an order
**Type:** Bug | **Priority:** High | **Difficulty:** Medium
**Reported by:** Customer service

> ACME cancelled SO-00001 (12 gloves). It shows Cancelled, but the gloves still show
> 12 allocated and only 38 available. We have 50 on the shelf. Now the web store thinks
> we're running low.

**Acceptance criteria**
- Cancelling an issued order releases its allocation. Available goes back up.
- Cancelling a draft order (never allocated) doesn't change allocation.
- Tests for both.
- Bonus: write a SQL query that recalculates what `qty_allocated` *should* be for every part
  from open sales order lines, and shows any parts where the stored number disagrees.
  This "integrity check" is a common ERP support task.

<details><summary>Hint 1</summary>

Allocation goes up in `issue` and down in `pick`. What happens in `cancel`?
</details>
<details><summary>Hint 2</summary>

For each line, the amount still allocated is ordered minus picked. Release that.
</details>

---

### WMS-106: Two pickers can over-pick the same bin at the same time
**Type:** Bug | **Priority:** Medium | **Difficulty:** Hard
**Reported by:** IT (found in log review)
**Do this after WMS-104.**

> Two pickers on two orders both picked the last 5 units from the same bin within the same
> second. Both picks succeeded. The bin went negative even after the WMS-104 fix.

**Acceptance criteria**
- Concurrent picks from the same location can't drive it negative.
- A test runs two picks in parallel threads and proves only one succeeds when there isn't
  enough for both.

<details><summary>Hint 1</summary>

Both requests read "5 on hand", both pass validation, both write. This is a race condition
("lost update").
</details>
<details><summary>Hint 2</summary>

Options: optimistic locking (add a `version` column to `inventory_level` with `@Version`,
then retry or fail on conflict), or pessimistic locking (`@Lock(PESSIMISTIC_WRITE)` on the
repository query, which issues `SELECT ... FOR UPDATE`). Either needs a Flyway migration
if you add a column. Write up which you chose and why in the pull request.
</details>

---

## Features

### WMS-201: Paginate and sort the parts list
**Type:** Feature | **Difficulty:** Easy

> With 5,000 parts the Parts page will be unusable. Show 25 per page with next/previous,
> and let me sort by part number or available quantity.

<details><summary>Hint</summary>

Spring Data supports `Pageable` and `Page<T>` in repository methods.
</details>

---

### WMS-202: Packing step and packing slip
**Type:** Feature | **Difficulty:** Medium

> After picking, we pack. Add a Packed status between Picked and Shipped, and a printable
> packing slip page (customer, ship-to, lines, quantities) that looks right when printed.

---

### WMS-203: Reorder suggestions that create a draft PO
**Type:** Feature | **Difficulty:** Medium

> The dashboard lists parts below their reorder point. Add a "Create PO" button that makes a
> draft PO for a chosen vendor with one line per low part. Suggested quantity: enough to get
> back to twice the reorder point.

**Stretch:** add a "preferred vendor" to parts (needs a migration) and group suggestions by
vendor.

---

### WMS-204: Cycle counts with variance approval
**Type:** Feature | **Difficulty:** Medium-Hard

> Counters count a location on the floor screens. If the count matches, nothing changes.
> If it doesn't, the variance goes to a supervisor screen to approve or reject. Approved
> variances post as adjustments with reason "Cycle count".

---

### WMS-205: Lot numbers and expiration dates
**Type:** Feature | **Difficulty:** Hard

> Some parts (adhesives, batteries) need lot tracking. Mark a part as lot-tracked. Receiving
> it requires a lot number and expiration date. Picking it should suggest the lot that
> expires first (FEFO). The part page shows quantity by location and lot.

This one touches the schema, every inventory movement, receiving, picking, and the screens.
Plan it before coding: write the migration and a short design note in the pull request.

---

### WMS-206: Average cost on receipt
**Type:** Feature | **Difficulty:** Medium

> Standard cost is typed in by hand and goes stale. When we receive a PO line, update the
> part's average cost: (old qty x old cost + received qty x PO cost per each) / new qty.

Remember the PO cost may be per case. Write the math as a unit test first.

---

### WMS-207: User management screen
**Type:** Feature | **Difficulty:** Medium

> Admins should be able to add users, reset passwords, change roles, and disable people
> who leave, without touching the database.

Also: the starter passwords are printed in this repo. Force a password change on first
login, or at least document how to change them.

---

### WMS-208: Purchase order API
**Type:** Feature | **Difficulty:** Easy-Medium

> Our vendor portal wants to read POs. Add `GET /api/purchase-orders/{poNumber}` and
> `GET /api/purchase-orders?status=ISSUED`. Follow the style of the sales order API.

---

## Integrations

### WMS-301: Web store orders imported twice
**Type:** Bug/Integration | **Priority:** High | **Difficulty:** Medium

> The web store resent order WEB-1001 after a timeout, and now we have two sales orders
> for it. We almost shipped it twice.

**Steps to reproduce:** Run the connector with WEB-1001.json, then copy the same file into
`connector/inbox/` again.

**Acceptance criteria**
- The WMS refuses a second order with the same external reference from the API, returning
  `409 Conflict` with the existing SO number.
- The connector treats 409 as "already imported" and moves the file to `processed/`,
  not `failed/`.
- The database enforces it too (a unique index), because two requests could arrive at
  the same moment.

<details><summary>Hint</summary>

This is called idempotency. Three layers: a check in the service, a unique constraint in a
new migration, and handling of the conflict in the connector.
</details>

---

### WMS-302: Import parts from CSV
**Type:** Feature | **Difficulty:** Medium

> We're migrating 2,000 parts from the old system. Let me upload a CSV
> (part_number, description, standard_cost, reorder_point). Create new parts, update
> existing ones, and show a summary: created, updated, and rows with errors (with the
> row number and reason). Don't stop at the first bad row.

---

### WMS-303: Shipping rates and labels (stub carrier)
**Type:** Integration | **Difficulty:** Medium

> On the Ship step, get rate quotes and buy a label instead of typing tracking numbers.

Build a `CarrierClient` interface with a fake implementation that returns made-up rates
and tracking numbers. Keep it behind the interface so a real carrier API (EasyPost,
ShipStation) can be dropped in later. Store the label cost on the order.

---

### WMS-304: Accounting export of shipped orders
**Type:** Integration | **Difficulty:** Medium

> Accounting wants each shipped order sent over as an invoice. For now, write a JSON file
> per shipped order into an `outbox/` folder, once only, even if the job runs again.

Similar to how ERPs sync invoices to QuickBooks. Think about what marks an order as
"already exported".

---

## Ops (on-premise server work)

### WMS-401: Backup and restore script
**Type:** Ops | **Difficulty:** Easy

> Write `scripts/backup.sh` that dumps the `wms` database to `backups/wms-YYYY-MM-DD-HHMM.sql`
> with `mysqldump`, and `scripts/restore.sh <file>` that restores a backup into `wms_sandbox`
> (never straight into production). Document how to schedule the backup nightly.

Test it for real: back up, make changes in the app, restore into the sandbox, and run the
app against the sandbox to confirm the data is from the backup.

---

### WMS-402: Add barcodes to parts
**Type:** Feature + Ops | **Difficulty:** Easy-Medium

> Parts need a barcode (UPC) field. The floor lookup should find a part by barcode as well
> as by part number.

Practice the safe schema change: a new migration `V3__add_part_barcode.sql`, test it on
the sandbox database first, then run it on the main one. Never edit V1 or V2.

---

### WMS-403: Build and run a release like a real server
**Type:** Ops | **Difficulty:** Easy

> Package the app as a single file and run it without Maven or VS Code, the way it would
> run on a company server.

`./mvnw package -DskipTests` builds `target/stockpilot-wms-0.1.0-SNAPSHOT.jar`. Run it
with `java -jar`. Bonus: give the release a version number, write release notes, and tag
it in Git.

---

### WMS-404: Health check for IT monitoring
**Type:** Ops | **Difficulty:** Easy

> IT wants a URL they can check every minute to know the server and database are up.

Add Spring Boot Actuator and expose only the `health` endpoint without login. Make sure
nothing sensitive is exposed.

---

### WMS-405: Run everything with Docker Compose
**Type:** Ops | **Difficulty:** Medium

> New developers spend an afternoon installing MySQL. Make `docker compose up` start
> MySQL and the app together.

Install Docker Desktop when you get here (`brew install --cask docker`). Write a
`Dockerfile` for the app and a `compose.yaml` with MySQL and the app. Bonus: switch the
tests to Testcontainers so they run against real MySQL instead of H2.
