# OneAppetite Backend — Issues & Fix Guide

**Audit date:** 2026-04-20
**Environment tested:** `http://localhost:8081` (branch `main`, post-merge)
**Source references:** `OneAppetite_Synopsis.docx`, `OneAppetite_FRD_1.docx`

This document lists every issue found during the endpoint audit + live API testing, the root cause, and the concrete fix. Fixes are ordered by priority (highest first). Each issue includes the exact file/line to edit so a teammate can pick it up cold.

---

## Priority legend

- 🔴 **Critical** — security / data-corruption / money loss. Fix before any release.
- 🟠 **High** — broken user experience or incorrect behaviour.
- 🟡 **Medium** — leak or inconsistency, not directly exploitable.
- 🟢 **Low** — cleanup / polish.

---

## 🔴 ISSUE 1 — Negative / zero cart quantity accepted

### Evidence
```
POST /api/cart/add   {"userId":29, "menuItemId":10, "quantity":-5}
→ 200 OK
→ OrderItem created with quantity=-5, price=-250.0
→ MenuItem.quantityAvailable went from 21 to 26 (stock INCREASED)
```

### Why it fails
- `dto/request/CartRequest.java` has no `@Min(1)` constraint on `quantity`.
- `controller/OrderItemController.add(...)` does not annotate the body with `@Valid`, so even if constraints existed they would be ignored.
- `service/OrderItemService.addProductToCart` does `remainingStock = item.getQuantityAvailable() - request.quantity();` — with a negative quantity this ADDS to stock, then stores a negative-priced line.
- Status as of 2026-04-22: `CartRequest.quantity` already has `@Min(1)`. The remaining gap is just `@Valid` on `OrderItemController.add(...)` — step 1 of the fix below is already done.

### Fix
1. `dto/request/CartRequest.java`
   ```java
   public record CartRequest(
       @NotNull Integer userId,
       @NotNull Integer menuItemId,
       @Min(value = 1, message = "Quantity must be at least 1") int quantity
   ) {}
   ```
2. `controller/OrderItemController.java`
   ```java
   public ResponseEntity<OrderItemDTO> add(@Valid @RequestBody CartRequest request) { ... }
   ```
3. Defense-in-depth in `service/OrderItemService.addProductToCart` (first line of the method):
   ```java
   if (request.quantity() <= 0)
       throw new IllegalArgumentException("Quantity must be at least 1");
   ```

### Verification
```
curl -X POST :8081/api/cart/add -H "Content-Type: application/json" \
     -d '{"userId":29,"menuItemId":10,"quantity":0}'
# expected: 400 with {"quantity":"Quantity must be at least 1"}
```

---

## 🔴 ISSUE 2 — Two `placeOrder` endpoints; one accepts negative-total carts

### Evidence
```
POST /orders/place/29
→ 200 OK, tokenNumber=OA-6001, status=PLACED, totalAmount=-100.0
# wallet was NOT debited
```

### Why it fails
- `OrderController.placeOrder` calls `OrderService.placeOrder`, which generates a token and saves the order but has **no validation** of `totalAmount` and **no wallet debit**.
- `OrderItemController.placeOrder` calls `OrderItemService.placeOrder`, which does validate (`total <= 0f`) and debits the wallet — but does NOT generate a `tokenNumber`.
- Result: users can abuse BUG 1 + this endpoint to "place" an order for free / negative total, OR place a legitimate order that has no token.

### Fix
Consolidate to a single flow:

1. Delete `controller/OrderController.placeOrder` and `service/OrderService.placeOrder` (or keep the service method `private`).
2. Extract token generation into a helper — new file `service/TokenService.java`:
   ```java
   @Service
   public class TokenService {
       @Autowired private OrderRepository orderRepo;
       private final SecureRandom rng = new SecureRandom();

       public String generateUniqueToken() {
           String token;
           do { token = "OA-" + (1000 + rng.nextInt(9000)); }
           while (orderRepo.existsByTokenNumber(token));
           return token;
       }
   }
   ```
3. In `OrderItemService.placeOrder`, right before `return mapToCartDTO(orderRepo.save(cart));`:
   ```java
   if (cart.getTokenNumber() == null)
       cart.setTokenNumber(tokenService.generateUniqueToken());
   ```

### Verification
```
POST /api/cart/place/29  → tokenNumber must be non-null
/orders/place/29          → 404 (endpoint removed)
```

---

## 🔴 ISSUE 3 — Most write endpoints have no authentication

### Evidence
```
PUT /vendors/3/image    {"vendorImageUrl":"http://evil.com/x.png"}   → 200 OK
PUT /orders/1/status?status=READY                                     → processed, no caller check
POST /api/wallet/29/topup                                             → anyone can credit anyone
POST /menu/9, DELETE /menu/9/15                                       → no ownership check
POST /api/cart/place/{otherUserId}                                    → drain someone else's wallet
```

### Why it fails
Only `controller/AdminController` calls `AdminService.verifyAdmin(callerId)`. Every other controller accepts requests with no `X-User-Id` header and no identity check.

### Fix
Create a shared guard in `service/AuthGuardService.java` (or extend `AdminService`):

```java
@Service
public class AuthGuardService {
    @Autowired private UserRepository userRepo;

    public User verifyCaller(Integer callerId) {
        if (callerId == null)
            throw new InvalidCredentialsException("Missing X-User-Id header.");
        User u = userRepo.findById(callerId)
            .orElseThrow(() -> new InvalidCredentialsException("Invalid caller."));
        if (Boolean.FALSE.equals(u.getIsActive()))
            throw new InvalidCredentialsException("Account disabled.");
        return u;
    }

    public void verifySelfOrAdmin(Integer callerId, Integer targetUserId) {
        User c = verifyCaller(callerId);
        if (c.getRole() != Role.ADMIN && !c.getUserId().equals(targetUserId))
            throw new InvalidCredentialsException("Forbidden.");
    }

    public void verifyVendor(Integer callerId, Integer vendorId) {
        User c = verifyCaller(callerId);
        if (c.getRole() != Role.ADMIN &&
           (c.getRole() != Role.VENDOR || !c.getUserId().equals(vendorId)))
            throw new InvalidCredentialsException("Not your resource.");
    }
}
```

Apply in every mutating controller:

| Controller | Endpoint | Guard to call |
|---|---|---|
| `WalletController` | GET/POST `/api/wallet/{userId}` | `verifySelfOrAdmin(callerId, userId)` |
| `OrderItemController` | all `/api/cart/**` with `userId` | `verifySelfOrAdmin(callerId, userId)` |
| `OrderItemController` | `POST /reduce/{orderItemId}` | look up order → `verifySelfOrAdmin(callerId, order.user.userId)` |
| `MenuItemController` | POST/PUT/DELETE `/menu/{vendorId}/**` | `verifyVendor(callerId, vendorId)` |
| `VendorController` | `PUT /vendors/{id}/image` | `verifyVendor(callerId, id)` |
| `OrderController` | `PUT /orders/{id}/status` | load order → `verifyVendor(callerId, order.vendor.userId)` |

Each controller method gains one parameter:
```java
@RequestHeader(value = "X-User-Id", required = false) Integer callerId
```
and calls the appropriate guard on line 1 of the method body.

### Verification
- Call any protected endpoint with no header → `401 {"error":"Missing X-User-Id header."}`
- Call with an employee's id against a vendor endpoint → `401 {"error":"Not your resource."}`
- Call with the correct owner's id → `200`

---

## 🔴 ISSUE 4 — `RuntimeException` returns HTTP 500 with full stack trace

### Evidence
Every "not found" / "empty cart" / "out of stock" error returns:
```json
{
  "status": 500,
  "error": "Internal Server Error",
  "trace": "java.lang.RuntimeException: Item not found\r\n\tat com.cts.mfrp.oa.service...",
  "message": "Item not found",
  "path": "/api/cart/add"
}
```

### Why it fails
- Services throw bare `RuntimeException("…")`.
- `exception/GlobalExceptionHandler` has no catch-all for `RuntimeException`, so Spring's default error pipeline runs, which includes `trace` by default.

### Fix
1. Add to `exception/GlobalExceptionHandler.java`:
   ```java
   @ExceptionHandler(RuntimeException.class)
   public ResponseEntity<Map<String,String>> handleRuntime(RuntimeException e) {
       return ResponseEntity.status(HttpStatus.BAD_REQUEST)
               .body(Map.of("error", e.getMessage()));
   }
   ```
2. Replace bare throws with typed exceptions. Examples:
   - `throw new RuntimeException("Item not found")` → `throw new ResourceNotFoundException("Item not found")`
   - `throw new RuntimeException("Cart is empty.")` → new class `InvalidCartStateException` (mapped to 400)
   - `throw new RuntimeException("No active cart found...")` → `ResourceNotFoundException`
3. `application.properties` — disable trace leakage globally:
   ```properties
   server.error.include-stacktrace=never
   server.error.include-exception=false
   server.error.include-message=always
   ```

### Verification
Empty-cart place order must return:
```json
{ "error": "Cart is empty." }
```
with status 400 and no `trace` field.

---

## 🔴 ISSUE 5 — Stock + wallet race conditions

### Evidence (logical, not yet reproduced under concurrency)
Two simultaneous `POST /api/cart/add` calls for the last unit of a MenuItem can both pass the stock check.
Two simultaneous `POST /api/cart/place/{userId}` can both pass the balance check.

### Why it fails
`@Transactional` gives read-committed isolation only. `findById(…)` does not lock the row. The `SELECT → check → UPDATE` window is non-atomic.

### Fix
1. `repository/MenuItemRepository.java`
   ```java
   @Lock(LockModeType.PESSIMISTIC_WRITE)
   @Query("select m from MenuItem m where m.itemId = :id")
   Optional<MenuItem> findForUpdate(@Param("id") Integer id);
   ```
   Use `findForUpdate` instead of `findById` in `addProductToCart` and `reduceQuantity`.
2. Same treatment for `UserRepository` when reading the wallet inside `WalletService.debit`:
   ```java
   @Lock(LockModeType.PESSIMISTIC_WRITE)
   @Query("select u from User u where u.userId = :id")
   Optional<User> findForUpdate(@Param("id") Integer id);
   ```

### Verification
Load-test with 20 parallel `cart/add` calls for an item with `quantityAvailable=1` — only one should succeed.

---

## 🟠 ISSUE 6 — `tokenNumber` is null when using `/api/cart/place`

### Why it fails
Token is generated inside `OrderService.placeOrder`, not `OrderItemService.placeOrder`. The cart was created during `addProductToCart` without a token.

### Fix
Covered by ISSUE 2 fix (TokenService + inject into `OrderItemService.placeOrder`).

---

## 🟠 ISSUE 7 — Login leaks real role on role-mismatch

### Evidence
```
POST /api/auth/login   (valid email + valid password for an EMPLOYEE, requested role=ADMIN)
→ 401 "No admin account found for this email. Your account is registered as employee."
# (unknown-email and wrong-password now correctly return the generic "Invalid email or password.")
```

### Why it fails
`AuthService.login:137-142` still has a separate branch for role-mismatch that echoes both the requested role and the account's actual role back to the caller. A supplied-valid-credentials probe with rotating role values enumerates the account type.

### Fix
Collapse the role-mismatch branch in `service/AuthService.login` to the same generic message used for unknown email / wrong password:
```java
if (user.getRole() != requestedRole) {
    throw new InvalidCredentialsException("Invalid email or password.");
}
```
No other branch needs to change — the email-not-found and wrong-password paths already return the generic message.

---

## 🟠 ISSUE 8 — No rate limiting on login / register

### Why it fails
No filter or bucket on authentication endpoints. Brute-force / credential-stuffing is wide open.

### Fix
Add Bucket4j or a simple in-memory `ConcurrentHashMap<String, AtomicInteger>` keyed by IP with a 5-req/min cap on `/api/auth/**`. For a real deployment, put this behind a reverse proxy (nginx / Cloudflare) that does the limiting.

---

## 🟡 ISSUE 9 — Seed data integrity

### Evidence
- Vendor `userId=9` "Bean's Kitchen" has `vendorType="Veg"` but has menu item `itemId=9` "Classic Beef Burger" (`dietaryType=Non-Veg`).
- Menu items `itemId=13` and `itemId=14` are exact duplicates ("Andhra Chicken Biryani", same price, same vendor).
- Vendor IDs `13, 15, 21, 23` referenced from tests but don't exist — returns 500 instead of 404.
- Note: these offending rows are live-MySQL state, not code. `config/DataLoader.java` only seeds `City` / `Campus` / `Building`; the vendor + menu rows above were inserted separately. The SQL cleanup step below is therefore a one-off DB operation — the service-layer rule and unique constraint prevent recurrence.

### Fix
1. Data cleanup SQL (run once):
   ```sql
   DELETE FROM menu_items WHERE item_id = 14;
   UPDATE users SET vendor_type = 'Non-Veg' WHERE user_id = 9;
   ```
2. Business rule in `service/MenuItemService.addMenuItem`:
   ```java
   if ("Veg".equalsIgnoreCase(vendor.getVendorType())
       && !"Veg".equalsIgnoreCase(item.getDietaryType()))
       throw new InvalidCartStateException("Veg vendor cannot list non-veg items.");
   ```
3. Schema constraint:
   ```sql
   ALTER TABLE menu_items
     ADD CONSTRAINT uq_vendor_item UNIQUE (vendor_id, item_name, meal_course);
   ```

---

## 🟡 ISSUE 10 — Unbounded wallet top-up

### Why it fails
`TopUpRequest.amount` has `@DecimalMin(1.0)` but no upper limit. An attacker (or a buggy client) could top up 10^9.

### Fix
```java
@DecimalMin(value = "1.0", message = "Minimum top-up amount is 1")
@DecimalMax(value = "50000.0", message = "Maximum top-up is 50,000 per transaction")
Double amount;
```
Also: add a daily aggregate cap in `WalletService.topUp` (sum of today's top-ups per user ≤ configurable ceiling).

---

## 🟡 ISSUE 11 — `reduceQuantity` always restocks only 1 unit

### Why it fails
`OrderItemService.reduceQuantity` does `menuItem.setQuantityAvailable(+1)` regardless of actual decrement semantics. When the cart line had qty=5 and the line is deleted (qty=1 path deletes), only 1 unit is returned to stock.

### Fix
When deleting a line entirely:
```java
if (item.getQuantity() > 1) {
    menuItem.setQuantityAvailable(menuItem.getQuantityAvailable() + 1); // single decrement
    item.setQuantity(item.getQuantity() - 1);
    ...
} else {
    menuItem.setQuantityAvailable(menuItem.getQuantityAvailable() + item.getQuantity());
    itemRepo.delete(item);
}
```

---

## 🟡 ISSUE 12 — Cart `Order` row never garbage-collected when last item removed

### Why it fails
`reduceQuantity` deletes the last `OrderItem` but leaves the parent `Order` with `status=CART` and `totalAmount=0`. Future `findByUser_UserIdAndStatus(userId, CART)` still returns this empty cart.

### Fix
After delete, if `itemRepo.findByOrder_OrderId(order.getOrderId()).isEmpty()` → `orderRepo.delete(order)`.

---

## 🟢 ISSUE 13 — CORS hardcoded to `http://localhost:4200`

### Why it fails
`config/CorsConfig.java` (or the `CorsFilter` bean) only allows localhost:4200. Any staging/production frontend hostname will be rejected.

### Fix
Externalise to `application.properties`:
```properties
app.cors.allowed-origins=http://localhost:4200,https://oneappetite.example.com
```
Read with `@Value("${app.cors.allowed-origins}")` and split on commas.

---

## 🟢 ISSUE 14 — `Role` mismatch between DB and domain synonyms

### Why it fails
Code compares `vendorType` using `"Veg"` / `"NonVeg"` string literals sprinkled across controllers/services. Typos (`"Non-Veg"` vs `"NonVeg"`) silently break filters.

### Fix
Convert `User.vendorType` to an enum `VendorType { VEG, NON_VEG }` with `@Enumerated(EnumType.STRING)`. Do a one-shot data-normalisation migration.

---

## 🟢 ISSUE 15 — `/vendors/{id}/veg` and `/vendors/{id}/nonveg` filter in Java, not SQL

### Why it fails
Controller fetches all vendors then filters with a stream. Fine for 5 rows, quadratic behaviour at scale.

### Fix
Add repository methods:
```java
List<User> findByBuilding_BuildingIdAndVendorType(Integer buildingId, String vendorType);
```
Call directly from the service.

---

## Deviations from Synopsis / FRD (feature gaps, not bugs)

| Requirement | Status | Notes / where to add |
|---|---|---|
| WebSocket "order ready" notifications | **MISSING** | Add `@EnableWebSocketMessageBroker` config, STOMP endpoint `/ws`, subscribe topic `/topic/orders/{userId}`. `OrderEventPublisher` already fires events — just wire a listener that `SimpMessagingTemplate.convertAndSend(...)`. |
| Gemini AI personalised suggestions | **MISSING** | Add Spring AI + Gemini client; new endpoint `GET /api/ai/suggest/{userId}` taking time-of-day + order history. |
| Demand forecasting dashboard | **MISSING** | Aggregate `orders` + `order_items` by day/hour/item, feed to Gemini. |
| Spring Security / JWT | **MISSING** | Current `X-User-Id` header model is easy to spoof. Long-term: replace with JWT bearer + Spring Security filter chain. |
| Vendor Kanban queue | **MISSING** | No endpoint returns orders for a vendor filtered by status. Add `GET /api/vendor/{vendorId}/orders?status=PLACED,PREPARING,READY`. |
| Building access level (Full / Limited / Under Renovation) | **MISSING** | Add `accessLevel` enum column on `Building`. |
| "Out of stock across entire building" toggle | **MISSING** | Currently per-MenuItem per-vendor. Requires a building-wide flag or a bulk endpoint. |
| FRD validations (email format, 10-digit phone, masked password) | ✅ implemented | `RegisterRequest` covers these. |
| 1000 initial wallet credit for employees | ✅ implemented | `AuthService.register` sets `walletBalance = 1000.0` for `Role.EMPLOYEE`. |
| UPI top-up (simulated) | ✅ implemented | `WalletController.topUp`. |

---

## Suggested fix rollout (one PR per row)

| PR | Closes | Est. effort |
|---|---|---|
| **A** — Input validation + typed exceptions + global RuntimeException handler + hide stack traces | 1, 4 | S |
| **B** — Merge `/orders/place` into `/api/cart/place`; introduce `TokenService` | 2, 6 | S |
| **C** — `AuthGuardService` and apply across wallet/cart/menu/vendor/order-status endpoints | 3 | M |
| **D** — Generic login failure message | 7 | XS |
| **E** — Pessimistic locks on `MenuItem` and `User.walletBalance` | 5 | M |
| **F** — Seed-data cleanup + vendor-type/dietary-type constraint + enum | 9, 14 | S |
| **G** — Rate limit `/api/auth/**` | 8 | S |
| **H** — Wallet top-up cap + daily aggregate cap | 10 | XS |
| **I** — `reduceQuantity` full-quantity restock + empty cart cleanup | 11, 12 | XS |
| **J** — CORS externalised + DB-side filtering for vendor type | 13, 15 | XS |
| **K** — WebSocket + vendor queue endpoint (synopsis gap) | feature | L |
| **L** — Gemini AI integration (synopsis gap) | feature | L |

Do A + B + C + D together first — they close every 🔴 in roughly a day.
