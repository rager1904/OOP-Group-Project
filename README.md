# Market Stock and Sales Tracker

A Java and Javalin application for small market traders in Zambia who need reliable product, stock purchase, sales and estimated gross profit records. This is Part 2 of the ZUCT Object-Oriented Programming group project.

## Run

Requires **JDK 17 or newer**. The included runnable JAR needs no Maven or internet access. On Windows double-click `start.bat`, or open a terminal in this folder:

```powershell
java -jar target/market-stock-tracker-1.0.0.jar
```

Open **http://127.0.0.1:7070**. Stop with Ctrl+C. Keep the terminal open while using the page. If port 7070 is occupied, stop the other application first. Always start from the project folder so the same data directory is used.

To build from source, install Maven 3.9+ and run:

```powershell
mvn clean package
java -jar target/market-stock-tracker-1.0.0.jar
```

`mvn test` runs domain and real HTTP integration tests. The first source build downloads dependencies. Java 17 is the compilation target; dependencies are pinned in `pom.xml`.

## Use

1. Add a product with buying and selling prices in kwacha. It starts with zero stock.
2. Record a stock purchase to increase its quantity.
3. Record a sale to reduce stock. Selling more than available stock is rejected.
4. Search products or click Edit to change a name or future prices.
5. Set optional report dates and inspect revenue, cost of goods sold and estimated gross profit.
6. Restart the application and confirm the saved records remain.

Example: buying price K5, selling price K8, purchase 10 units, sell 3. Remaining stock is 7, revenue K24, cost of sales K15, estimated gross profit K9.

## Classes and OOP requirements

| Requirement | Implementation |
|---|---|
| Real domain classes | `Product`, `StockPurchase`, `Sale`, `InventoryManager`, `SalesReport` |
| Encapsulation | Ordinary Java classes declare explicit private fields, constructors and getters. Products are immutable; `adjustStock` validates changes. Manager state and persistence fields are private; snapshot collections are immutable. |
| Interface relationship | `StockPurchase` and `Sale` implement `StockMovement`. |
| Polymorphism | `InventoryManager.record` calls `movement.stockChange()` through the interface. Purchase returns a positive quantity; sale returns a negative quantity. |
| Custom exceptions | `BusinessException` is thrown for invalid prices, quantities, duplicate names, missing products and insufficient stock; `App` maps it to HTTP 400. |
| Collections | A map indexes products by ID; lists retain purchase and sale records. |
| REST API | Nine routes, including GET, POST and PUT. |
| Persistence | JSON snapshot written to `data/inventory.json`, then replaced atomically. |
| Front end | Plain HTML/CSS/JavaScript served from classpath `/public` by Javalin. |

`App` configures routes, request boundaries, JSON mapping and error responses. `SalesReport` calculates summaries from one consistent snapshot. Small input classes in `App` describe API request bodies. Their setters let Jackson read JSON; the domain classes validate the values before saving.

## API

| Method | Path | Purpose |
|---|---|---|
| GET | `/api/products` | List products |
| GET | `/api/products/{id}` | Get one product |
| POST | `/api/products` | Create product |
| PUT | `/api/products/{id}` | Update name and prices, keeping stock |
| GET | `/api/purchases` | Purchase history |
| POST | `/api/purchases` | Record purchase |
| GET | `/api/sales` | Sales history |
| POST | `/api/sales` | Record sale |
| GET | `/api/reports?from=2026-11-01&to=2026-11-30` | Inclusive date range report |

Product request: `{"name":"Tomatoes","buyingPrice":5.00,"sellingPrice":8.00}`. Purchase/sale request: `{"productId":"ID from product response","quantity":3}`. Send `Content-Type: application/json` for writes.

**Request prices are kwacha. Response price and report money fields are integer ngwee** (100 ngwee = K1). This avoids floating-point money errors. Quantities are whole units from 1 to 1,000,000 per transaction. Prices range from K0 to K1,000,000 with at most two decimal places. Stock cannot exceed 1,000,000 units. Dates are generated on the server in Africa/Lusaka time.

Successful writes return 201 (create/transaction) or 200 (update). Domain validation returns 400, blocked origins 403, wrong request media type 415, and saving failures 500. Framework validation errors may use Javalin's standard JSON format. A missing product currently returns 400 with a clear message.

## Accounting decisions and limits

Each transaction records its name and price snapshots so subsequent product edits do not rewrite history. Sales use the product's buying price at sale time as an **estimated** unit cost. This is not FIFO or weighted-average accounting. Purchases use the current buying price; update that price before recording a purchase at a new cost. Reports exclude rent, tax, transport, refunds and other expenses. Current stock valuation uses current prices and is not a historical closing balance. Transactions are permanent in this version; corrections, returns and deletions require a future auditable reversal workflow.

## Storage and security

Mutations are synchronized to prevent concurrent requests overselling stock. The manager writes and flushes a temporary snapshot, atomically replaces the data file, and only then updates memory. A process lock prevents two application instances from writing the same data folder. Invalid JSON fails startup rather than silently discarding records. Use a local filesystem supporting atomic replacement. A whole file is rewritten per transaction: appropriate for a small classroom app, but large histories need a transactional database and pagination.

The server binds only to loopback, checks Host and Origin, requires JSON writes, caps request bodies, uses a restrictive Content Security Policy and renders user content with `textContent`. No SQL, user file uploads, external URL fetching or polymorphic JSON deserialization is used. There is **no login or multi-user permission system**: anyone with access to the local computer can use it. Do not expose it to the network. Shared deployment requires authentication, authorization, HTTPS, database transactions, audit logs, CSRF controls suited to the chosen authentication, dependency review and backups.

Stop the app and copy `data/inventory.json` to back it up. Restore only while stopped. Keep business records and the entire `data` folder out of GitHub; `.gitignore` excludes them. A filesystem or machine failure still needs backups; atomic replacement does not provide disaster recovery.

## Group members

| Name | Student Number | Programme |
|---|---:|---|
| Faith Bwalya | 2420924 | Cyber Security |
| Francis Banda | 2300842 | Cyber Security |
| Mwalula Chisanga | 2420947 | Cyber Security |
| Uchi Chinyama | 2420921 | Cyber Security |
| Natasha Mutale | 2421007 | Cyber Security |



## References

- [Javalin documentation](https://javalin.io/documentation)
- [Apache Maven](https://maven.apache.org/)
