# Understanding the Java project

Read the code in this order: `Product`, `StockMovement`, `StockPurchase`, `Sale`, `InventoryManager`, `SalesReport`, then `App`. The backend uses ordinary Java classes, explicit private fields, constructors, getters, loops, a map and lists. There are no Java records, Spring, Django or database frameworks.

## How the supplied books connect to the code

| Topic | Head First Java third edition | Java Complete Guide | Project example |
|---|---|---|---|
| Classes and objects | Chapters 2 and 4 | Chapter 5 | One `Product` object represents one kind of goods. |
| Constructors and encapsulation | Chapters 4 and 9 | Chapter 5, especially 5.4 | Product fields are private and its constructor rejects invalid data. |
| Interfaces and polymorphism | Chapters 7 and 8 | Chapter 6, especially 6.4 | A `StockMovement` reference can hold either a purchase or a sale. |
| Collections and generics | Chapter 11 | Chapter 7 | `Map<String, Product>` finds products by ID; typed lists store transactions. |
| Exceptions | Chapter 13 | Chapter 8 | `BusinessException` signals insufficient stock; the API reports the message. |
| Saving data | Chapter 16 | Chapter 8 | JSON text is saved to a local file and loaded at startup. |
| Concurrent requests | Chapter 18 | Chapter 9 introduces threads | `synchronized` prevents two requests changing stock at the same time. |

These are topic references to the supplied books, not copied examples. Javalin, Jackson JSON annotations, HTTP status codes and Maven are additional tools required to turn the Java model into a running web project. The assignment requires Javalin even though the books also show console or desktop applications.

## One request from start to finish

1. A trader enters a product and quantity in the browser.
2. `app.js` sends JSON to `/api/sales` with POST.
3. `App` reads the JSON into a small `MovementInput` object.
4. `InventoryManager` looks up the product in its map and creates a `Sale` object.
5. The manager calls `stockChange()` through the `StockMovement` interface.
6. A sale returns a negative number. `Product.adjustStock` rejects a negative resulting stock balance.
7. The manager saves the updated product and transaction together, then returns a successful response.
8. The browser refreshes the inventory, history and report.

## Encapsulation

`Product` declares fields such as `private final int quantity`. Other code can call `getQuantity()` but cannot assign directly to that field. `final` means the field cannot be reassigned after construction. `adjustStock` validates a change and returns a new Product with the new quantity. This lets the manager save the new state before making it visible to other requests. A getter does not mean a public setter is required for every field.

The JSON annotations on constructors tell Jackson which JSON properties match which parameters when loading saved data. They are persistence instructions, not replacements for Java constructors or validation.

## Polymorphism

Both a `StockPurchase` and a `Sale` implement `StockMovement`. The interface declares `stockChange()`; the classes implement it with `@Override`.

```java
StockMovement purchase = new StockPurchase("p1", "product1", "Beans", 5, 200, "2026-11-02");
StockMovement sale = new Sale("s1", "product1", "Beans", 5, 300, 200, "2026-11-02");
System.out.println(purchase.stockChange()); // 5
System.out.println(sale.stockChange());     // -5
```

The same call has different behavior because the actual object differs. The manager uses this behavior to change stock. An interface is a suitable choice because purchases and sales share a stock effect contract without needing a forced parent class. The brief permits inheritance **or** an interface.

## Exceptions and files

`BusinessException extends RuntimeException` is our custom exception. The product or manager throws it for a business rule violation. Javalin catches it through the registered exception handler and returns HTTP 400 with a message. File failures are different: they return HTTP 500 because the app could not save. Invalid data must never be treated as a successful sale.

Saving uses a temporary file and an atomic replacement so a partial write does not overwrite the last complete snapshot. `FileChannel` and the process lock are the most advanced part of this small project; their job is to protect the data file. `try` and `finally` ensure temporary files are cleaned up. JSON text is used instead of Java native object serialization.

## Money and reports

K5.25 is stored as 525 ngwee in a `long`. `BigDecimal` converts the input exactly. A `double` can introduce rounding errors for money. Quantity is an `int` because this version sells whole units.

`SalesReport.generate` loops over sales in the requested date range and adds their revenue and saved costs. Gross profit equals revenue minus cost of goods sold. Updating a product's prices does not change a sale already recorded. Stock valuation is current, and profit excludes other business expenses.

## Practice before the interview

- Find a product's private fields, constructor and getters. Explain why there is no direct quantity setter.
- Follow a purchase and a sale through `record`. Explain the positive and negative stock effects.
- Try to sell 20 units after buying only 10. Find where the exception is thrown and handled.
- Explain why a map is useful for finding a product by ID and why lists suit transaction history.
- Close and restart the application. Find the file that preserves the records.
- Read the tests, especially overselling, restart and failed saving. Run `mvn test`.
- Change a visible label, then make a small Java change you understand and rerun the tests.

Each group member should understand the code they submit and make their own genuine contributions. Use this guide to study the implementation, not to claim work or understanding that you do not have.
