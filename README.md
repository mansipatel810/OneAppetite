# OneAppetite

> Corporate cafeteria management — campus discovery, multi-vendor cart, real-time inventory, and an AI concierge.

OneAppetite is a full-stack corporate cafeteria management system that lets employees on a campus discover food vendors building-by-building, browse menus, add items from multiple vendors into a single cart, pay via in-app wallet, and pick up orders with a token number. Vendors get a Kanban board for live order management; admins get a control panel for vendor and employee oversight. The platform is backed by an AI chatbot (gemini / Llama 3.3 70B) for natural-language help.

---

## Live Demo

| Service  | URL                                               |
|----------|---------------------------------------------------|
| Frontend | https://oneappetite-frontend.onrender.com         |
| Backend  | https://oneappetite.onrender.com                  |

> Free-tier Render services sleep after 15 min of idle. First request after sleep takes 30–60s to wake up. This is normal — refresh once and the app loads.

---

## Architecture

```
                    ┌──────────────────────────────────────────────┐
                    │              END USER (browser)              │
                    └─────────────────────┬────────────────────────┘
                                          │  HTTPS
                                          ▼
       ┌────────────────────────────────────────────────────────────────┐
       │  Render Static Site  —  oneappetite-frontend.onrender.com      │
       │  ────────────────────────────────────────────────────────────  │
       │  Angular 21 SPA (standalone components, provideRouter, RxJS)   │
       │  • LoginComponent / RegisterComponent / ForgotPasswordModal    │
       │  • DashboardComponent (City → Campus → Building → Vendor)      │
       │  • MenuComponent (grouped by meal course)                      │
       │  • CartViewComponent (multi-vendor buckets)                    │
       │  • VendorKanbanComponent / VendorMenuComponent                 │
       │  • AdminUsersComponent                                         │
       │  • ChatbotComponent (floating)                                 │
       │  Services: AuthService, CartService, MenuService, OrderService,│
       │            WalletService, NotificationService, FoodService     │
       └─────────────────────────┬──────────────────────────────────────┘
                                 │  fetch() / HttpClient
                                 │  CORS-restricted to frontend origin
                                 ▼
       ┌────────────────────────────────────────────────────────────────┐
       │  Render Web Service  —  oneappetite.onrender.com               │
       │  ────────────────────────────────────────────────────────────  │
       │  Spring Boot 4.0.4 / Java 17 (Docker, multi-stage build)       │
       │                                                                │
       │  Controllers              Services             Events          │
       │  ─────────────────────    ──────────────────   ─────────────── │
       │  /api/auth                AuthService          OrderPlacedEvent│
       │  /api/users               UserService          OrderReadyEvent │
       │  /api/admin               AdminService                ▲        │
       │  /api/v1/{cities,         LocationService             │        │
       │           campuses,                            Listeners       │
       │           buildings}                           ─────────────── │
       │  /vendors                 VendorService        OrderPlaced→DB  │
       │  /menu                    MenuItemService      OrderReady→MP3  │
       │  /orders                  OrderService                         │
       │  /api/cart                OrderItemService  ◄──[@Version       │
       │  /api/wallet              WalletService         + FOR UPDATE]  │
       │  /api/notifications       NotificationService                  │
       │  /chat                    ChatService ──────────┐              │
       │                                                 │              │
       │  GlobalExceptionHandler maps:                   │              │
       │   • OutOfStockException             → 409       │              │
       │   • ObjectOptimisticLockingFailure  → 409       │              │
       │   • InsufficientBalanceException    → 402       │              │
       │   • InvalidCredentialsException     → 401       │              │
       │   • ResourceNotFoundException       → 404       │              │
       │   • EmailAlreadyExistsException     → 409       │              │
       └──────────────────┬──────────────────────────────┼──────────────┘
                          │ JDBC over SSL                │ HTTPS
                          ▼                              ▼
       ┌──────────────────────────────────┐   ┌─────────────────────────┐
       │   Aiven Managed MySQL (cloud)    │   │      gemini API           │
       │   ────────────────────────────   │   │   Llama 3.3 70B         │
       │   Tables:                        │   │   (AI chatbot backend)  │
       │   • users (role, wallet_balance, │   └─────────────────────────┘
       │            is_active)            │
       │   • cities, campuses, buildings  │
       │   • menu_items (@Version, stock) │
       │   • orders (status, token, time) │
       │   • order_items                  │
       │   • notifications                │
       └──────────────────────────────────┘
```

---

## Tech Stack

| Layer       | Tech                                                                  |
|-------------|-----------------------------------------------------------------------|
| Frontend    | Angular 21 (standalone components, signals-ready), TypeScript 5.9, RxJS 7.8, Reactive Forms |
| Backend     | Spring Boot 4.0.4, Spring Data JPA, Spring Web MVC, Spring Validation |
| Language    | Java 17                                                               |
| Database    | MySQL 8 (Aiven managed cloud, SSL required)                           |
| ORM         | Hibernate (with `@Version` optimistic locking + pessimistic write locks) |
| Auth        | jBCrypt 0.4 password hashing                                          |
| AI          | gemini API — `llama-3.3-70b-versatile`                                  |
| Build       | Maven (backend), `@angular/build:application` (frontend)              |
| Deploy      | Render.com (Docker Web Service + Angular Static Site)                 |
| Dev proxy   | `proxy.conf.json` (Vite dev server → `localhost:8081`)                |

---

## Core Feature Set

| Module              | Capabilities                                                                                  |
|---------------------|-----------------------------------------------------------------------------------------------|
| **Authentication**  | Register (Employee / Vendor / Admin with secret), Login, OTP-based forgot-password           |
| **Discovery**       | Pre-seeded Chennai campus hierarchy: 5 campuses, multiple buildings per campus               |
| **Browsing**        | Menu items grouped by meal course (Breakfast / Lunch / Dinner), dietary-type badges, stock pills |
| **Cart**            | Multi-vendor cart with per-vendor buckets, live quantity steppers, real-time total           |
| **Inventory**       | `@Version` optimistic locking + `SELECT … FOR UPDATE` pessimistic locking — race-condition safe |
| **Wallet**          | In-app balance, `InsufficientBalanceException` → HTTP 402                                    |
| **Order Placement** | Token number generation, status: `CART → PLACED → PREPARING → READY → PICKED_UP → COMPLETED`  |
| **Vendor Kanban**   | Live order pipeline by status column, single-click status transitions                        |
| **Vendor Menu CRUD**| Create / update / delete items, toggle stock status                                          |
| **Notifications**   | Spring `ApplicationEvent` pub/sub, in-app bell with unread badge, MP3 audio on order ready   |
| **AI Chatbot**      | Domain-scoped system prompt, conversation history, friendly fallback on rate-limit           |
| **Admin Panel**     | List users by role, toggle active status, X-User-Id header verification                      |

---

## Concurrency Control — How We Prevent "Two Users Buy the Last Unit"

The single most production-grade piece of the system. Two layers of protection:

```
  Customer A (tab 1)                    Customer B (tab 2)
       │                                       │
       │ POST /orders/place                    │ POST /orders/place
       ▼                                       ▼
  ┌──────────────────────┐              ┌──────────────────────┐
  │ TX 1 START           │              │ TX 2 START           │
  │ SELECT … FOR UPDATE  │ ◄── row lock │ SELECT … FOR UPDATE  │
  │   on menu_items[42]  │              │   blocks here ⏸     │
  │                      │              │                      │
  │ qty_available = 1    │              │                      │
  │ UPDATE → qty = 0     │              │                      │
  │ version: 7 → 8       │              │                      │
  │ COMMIT               │              │                      │
  └──────────────────────┘              │                      │
       │                                │ ⏵ unblocked           │
       │ → 201 Created                  │ qty_available = 0     │
       │   { tokenNumber, … }           │ → throws              │
       │                                │   OutOfStockException │
                                        │ ROLLBACK              │
                                        └──────────────────────┘
                                              │
                                              ▼
                                        409 Conflict
                                        { "error": "Sorry, this
                                          item just went out of
                                          stock!" }
```

- **Layer 1 — Pessimistic write lock** (`MenuItemRepository.findByIdForUpdate`): the row is locked from the moment we read it; concurrent readers must wait until the lock-holder commits.
- **Layer 2 — Optimistic `@Version`** (`MenuItem.version`): if any code path updates `menu_items` without going through the lock, Hibernate's version check will reject the stale write with `ObjectOptimisticLockingFailureException`.
- **Translation:** `GlobalExceptionHandler` maps both exceptions to HTTP **409 Conflict** with a single, user-friendly message — the frontend has one error path to handle.

---

## Project Structure

```
OneAppetite/
├── Backend/
│   └── OneAppetite/
│       ├── Dockerfile                  # multi-stage Maven → Temurin JRE
│       ├── system.properties           # Java 17 pin for Render
│       ├── pom.xml                     # Spring Boot 4.0.4
│       └── src/main/
│           ├── java/com/cts/mfrp/oa/
│           │   ├── controller/         # 13 REST controllers
│           │   ├── service/            # business logic
│           │   ├── repository/         # JPA repos (incl. findByIdForUpdate)
│           │   ├── model/              # @Entity classes
│           │   ├── dto/                # request + response DTOs
│           │   ├── events/             # OrderPlacedEvent, OrderReadyEvent
│           │   ├── listener/           # @EventListener beans
│           │   ├── exception/          # custom exceptions + GlobalExceptionHandler
│           │   ├── config/             # CorsConfig, DataLoader
│           │   └── util/               # Mp3Player
│           └── resources/
│               ├── application.properties
│               └── orderReady.mp3
└── Frontend/
    ├── package.json
    ├── angular.json
    ├── proxy.conf.json                 # dev-only API proxy
    └── src/
        ├── main.ts                     # browser bootstrap
        ├── environments/
        │   ├── environment.ts          # prod: apiBase = render backend URL
        │   └── environment.development.ts  # dev: apiBase = '' (uses proxy)
        └── app/
            ├── app.config.ts           # provideRouter, provideHttpClient
            ├── app.routes.ts
            ├── login/  register/  forgot-password/
            ├── dashboard/  menu/  cart-view/  my-orders/
            ├── vendor-kanban/  vendor-menu/  vendor-settings/
            ├── admin/admin-users/  admin-settings/
            ├── chatbot/                # AI assistant
            ├── navbar/  sidebar/  shell/  settings/  toast/
            ├── services/               # 9 typed Angular services
            └── guards/
```

---

## Local Development

### Prerequisites
- Java 17+
- Node.js 20+ (Node 25 works but emits warnings)
- A MySQL 8 database (local or Aiven)

### Backend
```bash
cd Backend/OneAppetite

# Set DB env vars (or put them in your IDE run config)
export DB_HOST=localhost
export DB_PORT=3306
export DB_NAME=oneappetite
export DB_USER=root
export DB_PASSWORD=secret
export GEMINI_API_KEY=gsk_...       # optional, chatbot disabled if blank

./mvnw spring-boot:run
# Backend listens on http://localhost:8081
```

### Frontend
```bash
cd Frontend
npm install
npm start                            # ng serve on http://localhost:4200
# proxy.conf.json forwards /api/* and /orders, /menu, /vendors to :8081
```

On first boot, `DataLoader` seeds Chennai with 5 campuses and their buildings if the `cities` table is empty.

---

## Deployment

### Backend (Render Web Service)
- **Runtime:** Docker (via included `Dockerfile`)
- **Root directory:** `Backend/OneAppetite`
- **Environment variables:** `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`, `gemini_API_KEY`, `CORS_ALLOWED_ORIGINS`
- **Port:** auto-injected by Render via `$PORT` (read by `server.port=${PORT:8081}`)

### Frontend (Render Static Site)
- **Build:** `npm install && npm run build:prod`
- **Publish:** `dist/Frontend/browser`
- **Rewrite rule:** `/* → /index.html` (Angular SPA routing)

CORS is whitelisted server-side via the `CORS_ALLOWED_ORIGINS` env var (comma-separated list of allowed frontend origins).

---

## Team & Module Ownership

| Member | Module                                     | Files of interest                                              |
|--------|--------------------------------------------|----------------------------------------------------------------|
| 1      | Authentication & Admin Control Plane       | `AuthController`, `AdminController`, `login.component`, `admin-users.component` |
| 2      | Location Hierarchy & Vendor Discovery      | `DataLoader`, `CityController`, `CampusController`, `BuildingController`, `dashboard.component`, `menu.component` |
| 3      | Cart, Wallet & Concurrency Control ⭐      | `OrderItemService`, `MenuItemRepository.findByIdForUpdate`, `GlobalExceptionHandler`, `cart.service.ts` |
| 4      | Vendor Operations, Events & AI             | `OrderController`, `events/*`, `listener/*`, `Mp3Player`, `ChatService`, `vendor-kanban.component`, `chatbot.component` |

---

## Roadmap

- Spring Security with JWT (replace current `X-User-Id` admin guard + localStorage session)
- WebSocket push for order status updates (replace polling)
- Payment gateway integration (Razorpay / Stripe)
- Vendor analytics dashboard (revenue, top items, prep-time SLAs)
- Image upload to S3 (currently URL-only)
- Order rating & feedback loop
