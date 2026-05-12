# OneAppetite — Demo Day Cheat Sheet

**Stack:** Angular 21 SPA · Spring Boot 4 / Java 17 · MySQL on Aiven · Hosted on Render
**Live:** Frontend → `oneappetite-frontend.onrender.com` · Backend → `oneappetite.onrender.com`

---

## Architecture (one glance)

```
 BROWSER                  RENDER STATIC SITE             RENDER WEB SERVICE              AIVEN CLOUD
┌─────────┐  HTTPS  ┌───────────────────────┐  HTTPS  ┌────────────────────────┐  JDBC  ┌──────────────┐
│  User   │ ──────▶ │  Angular 21 SPA       │ ──────▶ │  Spring Boot 4         │ ─────▶ │  MySQL 8     │
│ browser │ ◀────── │  - Components         │ ◀────── │  @RestController       │ ◀───── │  TLS-only    │
└─────────┘   JSON  │  - Services (RxJS)    │  JSON   │  @Service @Transactional        │  HikariCP    │
                    │  - HttpClient         │         │  @Repository (JPA)              │  pool        │
                    │  - environment.ts     │         │  CorsConfig + env vars          └──────────────┘
                    └───────────────────────┘         │  GlobalExceptionHandler                  │
                                                      │       │                                  │
                                                      │       └─▶ Groq API (Llama 3.3) ──── HTTPS ┘
                                                      └────────────────────────┘

 CONCURRENCY (Place Order):  @Transactional → SELECT ... FOR UPDATE (5s timeout) → decrement stock
                              → debit wallet → set token → COMMIT → release locks → publish event
 SAFETY NET:                  @Version column rejects stale writes from any other code path → HTTP 409
```

---

## 10 Panic-Glance One-Liners — *"If they ask X, you say Y"*

| # | If they ask… | You say… |
|---|---|---|
| 1 | **What's the tech stack?** | "Angular 21 standalone components, Spring Boot 4 on Java 17, MySQL 8 on Aiven Cloud, deployed on Render — Docker for the backend, Static Site for the frontend." |
| 2 | **What if two users buy the last item?** | "We use `@Lock(PESSIMISTIC_WRITE)` on the menu item via `findByIdForUpdate` — it runs `SELECT FOR UPDATE`. The second transaction blocks, then sees stock = 0 and gets HTTP 409 Conflict with a friendly message." |
| 3 | **Why no microservices?** | "Microservices solve a scaling problem we don't have yet. Our monolith has clean layer separation, so if we ever need to split, the surgery is clean — services become deployables." |
| 4 | **Why no Spring Security / JWT?** | "Scope decision. We use BCrypt for passwords and `X-User-Id` header for admin endpoints. Spring Security is item #1 on our roadmap." |
| 5 | **How do you talk to the database safely?** | "JDBC over TLS — `ssl-mode=REQUIRED` in the connection string. Aiven refuses plaintext. Credentials live in env vars, never in Git." |
| 6 | **What does `@Transactional` give you?** | "Atomicity. Our `placeAllCarts` writes to 5 tables — orders, order_items, menu_items, users (wallet), notifications. Any failure rolls back everything. No half-charged customers." |
| 7 | **Where does business logic live?** | "The `@Service` layer. Controllers are one-liners — they just deserialize input and call the service. Repositories are interfaces — Spring Data writes the impl." |
| 8 | **How are errors handled?** | "Domain exceptions thrown from `@Service`, caught by `GlobalExceptionHandler` (`@RestControllerAdvice`), mapped to HTTP codes: 401 invalid creds, 402 low balance, 404 not found, 409 out of stock." |
| 9 | **What's the event system for?** | "Decoupling. When an order moves to READY, we publish `OrderReadyEvent`. Two `@EventListener` beans react — one writes a DB notification, one plays an MP3. Adding SMS tomorrow means one new listener, zero touches to order code." |
| 10 | **How is data kept in sync between frontend and backend?** | "We don't trust client-side cache for anything stock-sensitive. Backend re-verifies stock INSIDE the lock during checkout. Frontend uses `BehaviorSubject` for cart state so every component updates in the same tick." |

---

## 5 Files to Have Open in the IDE

1. `Backend/.../service/AuthService.java` — login + BCrypt + role check
2. `Backend/.../repository/MenuItemRepository.java` — the `FOR UPDATE` query ⭐
3. `Backend/.../service/OrderItemService.java` — `placeAllCarts()` line 180
4. `Backend/.../exception/GlobalExceptionHandler.java` — clean HTTP error mapping
5. `Backend/src/main/resources/application.properties` — Aiven config + env vars

---

## Who Answers What

| Member | Owns | Flagship line |
|---|---|---|
| **1** | Login, Register, Admin panel | "BCrypt + per-user salt + admin-secret guard" |
| **2** | Campus hierarchy, Vendor & menu browsing | "City → Campus → Building → Vendor, seeded by `DataLoader`" |
| **3** ⭐ | Cart, Wallet, Order placement | "Two-layer concurrency: pessimistic lock + `@Version` backstop" |
| **4** | Vendor Kanban, Events, Notifications, AI | "Spring `ApplicationEvent` pub/sub, Groq Llama 3.3 chatbot" |

---

## Last-Minute Reminders

- **Wake the backend 2 min before demo** (free tier sleeps after 15 min idle — first request takes 30–60s)
- **Two browser tabs ready** for the race-condition demo on the same item
- **Close email/slack tabs** — clean browser window
- **If something breaks live, pivot to code:** *"Let me show you the implementation instead"* → switch to IDE
- **Speak slowly during the concurrency story** — it's the flagship; let it land

---

*Print this. Tape it to the laptop. You've got this.*
