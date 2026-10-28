# StockPilot WMS

A small warehouse management system built with Java and Spring Boot, modeled on the
workflows of an on-premise ERP/WMS like Fishbowl: parts, bin locations, purchase orders,
receiving, sales orders, allocation, picking, shipping, and a full inventory audit trail.

It ships with **planted bugs** and a **ticket backlog** ([TICKETS.md](TICKETS.md)) so you can
practice the real job: reproduce bugs, fix them, add features, build integrations, and run
the server like IT would.

**What's inside**

| Part | What it does |
|---|---|
| Office screens | Parts, inventory, POs, sales orders, locations, vendors, customers (desktop browser) |
| Floor screens | Receive, pick, and look up items from a phone on the same Wi-Fi |
| REST API | Session-token login like Fishbowl's API, plus parts, inventory, and sales order endpoints |
| Connector | A separate Java service that imports web store orders through the API |
| Database | MySQL, with the schema managed by Flyway migrations |

Tech: Java 21, Spring Boot 3.4, Spring Security, Spring Data JPA (Hibernate), Thymeleaf,
Flyway, MySQL, JUnit 5, springdoc OpenAPI, GitHub Actions.

---

## 1. First-time setup (Mac + VS Code)

You need: Java 21, MySQL (running), VS Code with the **Extension Pack for Java** and
**Spring Boot Extension Pack**, and Git.

**Step 1: Put the project somewhere sensible**

```bash
mkdir -p ~/Projects
# unzip stockpilot-wms.zip into ~/Projects, then:
cd ~/Projects/stockpilot-wms
chmod +x mvnw
```

**Step 2: Create the databases**

Open `docs/setup-mysql.sql` and change `ChangeMe-123` to a password of your choice
(at least 8 characters). Then run it as root:

```bash
mysql -u root -p < docs/setup-mysql.sql
```

This creates two databases, `wms` (your "production") and `wms_sandbox` (for testing),
and a `wms_app` user that the application logs in as.

**Step 3: Tell the app your database password**

```bash
cp config/local.yml.example config/local.yml
```

Edit `config/local.yml` and put in the password from step 2. This file is in `.gitignore`,
so your password never goes to GitHub.

**Step 4: Start the app**

```bash
./mvnw spring-boot:run
```

The first run downloads Maven and all libraries, which takes a few minutes. Then Flyway
creates the tables and loads sample data. You're up when you see `Started WmsApplication`.

**Step 5: Sign in**

Open http://localhost:8080

| Username | Password | Role |
|---|---|---|
| admin | admin123 | Office: everything |
| picker | picker123 | Warehouse: floor screens only |
| connector | connector123 | Used by the connector service for the API |

**Step 6: Put it on GitHub**

Create an empty repository on github.com (no README), then:

```bash
git init
git add .
git commit -m "Initial import of StockPilot WMS"
git branch -M main
git remote add origin https://github.com/<your-username>/stockpilot-wms.git
git push -u origin main
```

**Opening it in VS Code:** File > Open Folder > `stockpilot-wms`. Wait for the Java
extension to finish importing (progress shows in the bottom bar). You can then run the app
from the **Spring Boot Dashboard** in the sidebar, or the **Run and Debug** panel
(configurations "WMS", "WMS (sandbox database)", and "Connector" are included).
Running in debug mode lets you set breakpoints, which is the fastest way to hunt bugs.

---

## 2. A guided tour: the full flow

The sample data has 8 parts, 7 locations, 3 vendors, 3 customers, 2 open POs, and 1 open
sales order. Try this end to end as the `admin` user:

1. **Create a part.** Parts > New part. Give it a number like `SCR-0832`, a cost, and a
   reorder point. It starts with no stock.
2. **Order it.** Purchase orders > New purchase order. Pick a vendor, then add a line for
   your part. Try ordering in **CS** (case of 12). Click **Issue to vendor**.
3. **Receive it.** On the PO, use the Receive form: receive part of the quantity into
   `A-01-01`, then the rest. Check the part page: the stock is in eaches.
4. **Sell it.** Sales orders > New sales order, add a line for your part, then
   **Issue and allocate**. The part's "allocated" goes up and "available" goes down.
5. **Pick it.** Use the Pick form on the order (or the floor screens). Stock leaves the bin.
6. **Ship it.** Once fully picked, choose a carrier and mark it shipped.
7. **Audit it.** Inventory > History shows every movement, who did it, and why.

Pay attention while you do this. Some things won't behave the way they should. That's on
purpose; see [TICKETS.md](TICKETS.md).

---

## 3. Floor screens on your phone

1. Find your Mac's local IP address: `ipconfig getifaddr en0` (e.g. `192.168.1.25`).
2. On a phone on the same Wi-Fi, open `http://192.168.1.25:8080/floor`.
3. Sign in as `picker` / `picker123`.

If the phone can't connect, macOS may be blocking it: System Settings > Network >
Firewall > Options, and allow incoming connections for Java.

---

## 4. The REST API

Interactive docs: http://localhost:8080/swagger-ui.html

The API works like Fishbowl's: an integration logs in with its app details plus a user's
credentials, gets a token, and sends that token on every request.

```bash
# 1. Log in and get a token
curl -s -X POST http://localhost:8080/api/login \
  -H "Content-Type: application/json" \
  -d '{"appName":"My test app","appId":"test-001","username":"connector","password":"connector123"}'

# Copy the "token" value from the response, then:
TOKEN=paste-token-here

# 2. Call the API
curl -s http://localhost:8080/api/parts -H "Authorization: Bearer $TOKEN"
curl -s "http://localhost:8080/api/inventory?partNumber=GLV-L" -H "Authorization: Bearer $TOKEN"

# 3. Create a sales order
curl -s -X POST http://localhost:8080/api/sales-orders \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"customerCode":"ACME","externalRef":"TEST-1","lines":[{"partNumber":"BOX-1212","quantity":5,"unitPrice":1.50}]}'
```

Tokens expire after 60 minutes without use, and all tokens are lost when the server
restarts (they live in memory).

---

## 5. The web store connector

`connector/` is a separate Spring Boot app, like a third-party integration installed next
to an ERP server. Every 15 seconds it reads order files from `connector/inbox/`, logs in to
the WMS API, and creates sales orders. Each file then moves to `processed/`, or to `failed/`
with an `.error.txt` file explaining why.

With the WMS running, open a second terminal:

```bash
cd connector
cp samples/*.json inbox/
../mvnw spring-boot:run
```

Watch the log, then check Sales orders in the browser. One of the three sample orders
fails on purpose; read its `.error.txt` in `connector/failed/` to see why.

---

## 6. Tests

```bash
./mvnw test                          # WMS tests (no MySQL needed: they use an in-memory database)
./mvnw -f connector/pom.xml test     # connector tests
```

**Two WMS tests fail on purpose.** They reproduce bugs WMS-101 and WMS-102. Your first job
is to make them pass. GitHub Actions runs the same tests on every push and pull request,
so the build badge on your repo turns green when you fix them.

---

## 7. Sandbox vs production

Like a real on-prem install, there are two databases. Practice risky changes on the sandbox:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=sandbox
```

To wipe the main database and start fresh with sample data:

```bash
mysql -u root -p < docs/reset-database.sql
```

---

## 8. Project map

```
src/main/java/com/stockpilot/wms/
  domain/       Entities: Part, Location, InventoryLevel, PurchaseOrder, SalesOrder, ...
  repository/   Spring Data interfaces (database queries)
  service/      Business rules: receiving, allocation, picking, adjustments
  web/          Controllers for the office and floor screens
  api/          REST controllers, request/response DTOs, API error handling
  config/       Security (form login + API tokens), starter users, API docs
src/main/resources/
  db/migration/ Flyway SQL migrations (schema + sample data)
  templates/    Thymeleaf HTML pages
  static/css/   Stylesheet
src/test/java/  JUnit tests
connector/      The separate web store integration service
docs/           Database setup and reset scripts
config/         Your local settings (local.yml is not committed)
```

How a request flows: **browser > controller (web/) > service (service/) > repository >
MySQL**. Business rules live in services, never in controllers or templates. Every stock
change goes through `InventoryService`, which also writes the audit trail.

---

## 9. Working like it's a job

For each ticket:

1. `git checkout -b fix/WMS-102-po-never-closes` (or `feature/...`)
2. Reproduce the problem in the app first.
3. Write or run a test that shows the bug.
4. Fix it. Run `./mvnw test`.
5. Commit with the ticket number: `git commit -m "WMS-102: close PO when all lines are received"`
6. Push and open a pull request on GitHub. Let CI pass, then merge.

---

## 10. Troubleshooting

**`Access denied for user 'wms_app'@'localhost'`**: the password in `config/local.yml`
doesn't match the one in `docs/setup-mysql.sql`. Fix one so they match.

**`Communications link failure`**: MySQL isn't running. `brew services start mysql`.

**`Unknown database 'wms'`**: you skipped step 2.

**`Port 8080 was already in use`**: the app is already running in another terminal or in
VS Code. Stop that one first.

**Flyway warns that MySQL 9 is newer than it has been tested with**: that's a warning,
not an error. The app still starts.

**`Validate failed: Migrations have failed validation`**: a migration file that already ran
was edited. Never change a migration after it has run; add a new `V3__...sql` instead.
For a learning database you can also reset it (section 7).

**Anything else**: read `logs/wms.log`. The answer is usually in the first error.
