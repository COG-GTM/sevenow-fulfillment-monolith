# 7NOW Fulfillment Monolith — Architecture

<img src="assets/7-eleven-logo.svg" alt="7-Eleven" height="40" align="right"/>

This document describes the monolith **as it is today** so a later session can plan service extraction from it.
It covers the deployable, the Gradle modules and their dependencies, the REST surface per module, **which module
owns which table**, the cross-module seams, and the order state machine.

## 1. Deployable

| | |
|---|---|
| Runtime | one Spring Boot 2.0.3 application, `com.sevenow.fulfillment.SevenowApplicationMain` (module `sevenow-application`), Java 8, port 8080 in-container / 8081 on the host |
| Database | one MySQL 5.7 schema `sevenow` (user `mysqluser`), migrated by Flyway from `sevenow-flyway/src/main/resources/db/migration` |
| Persistence | Spring Data JPA / Hibernate 5.2, `spring.jpa.generate-ddl=false` (schema is Flyway-owned) |
| Wiring | `SevenowApplicationMain` `@Import`s `CustomerServiceConfiguration`, `OrderServiceConfiguration`, `StoreServiceConfiguration`; `DriverServiceConfiguration` is picked up by `@ComponentScan` |
| API docs | Springfox Swagger 2 (`common-swagger`), one Docket scanning `com.sevenow.fulfillment` → `/swagger-ui.html` |
| Observability | Spring Boot Actuator + Micrometer Prometheus registry (counters `placed_orders`, `approved_orders`) |
| Build / CI | Gradle 4.10.2 wrapper; `./build-and-test-all.sh` = compile → `docker compose` MySQL → Flyway migrate → build → integration tests → start app → end-to-end tests |

## 2. Module map

```
                       ┌──────────────────────┐
                       │  sevenow-application │   Spring Boot main, Dockerfile
                       └──────────┬───────────┘
        ┌──────────────┬──────────┴──────────┬────────────────┐
        ▼              ▼                     ▼                ▼
 customer-service  order-service        store-service    driver-service      (web + domain services)
        │              │  ├── customer-service (direct Java call)
        │              │  ├── customer-service-api
        │              │  └── store-service-api
        ▼              ▼                     ▼                ▼
 customer-service-api  order-service-api  store-service-api  driver-service-api   (request/response DTOs)
        └──────────────┴──────────┬──────────┴────────────────┘
                                  ▼
                           sevenow-domain          ALL JPA entities + Spring Data repositories
                                  │
                      sevenow-common-jpa (orm.xml for Money/Address)
                                  │
                           sevenow-common          Money, Address, PersonName, Jackson MoneyModule
```

Supporting modules: `common-swagger` (Docket), `sevenow-flyway` (migrations, no Java), `sevenow-test-util`,
`sevenow-end-to-end-tests-common` (shared REST-assured scenarios) and `sevenow-end-to-end-tests` (runs them against the
Docker-compose stack). `buildSrc` holds the `IntegrationTestsPlugin`, `WaitForMySql` and the `SevenowServicePlugin` legacy plugin.

| Module | Package | Compile deps on other modules | Role |
|---|---|---|---|
| `sevenow-common` | `…common` | — | value objects (`Money`, `Address`, `PersonName`), Jackson module, `UnsupportedStateTransitionException` |
| `sevenow-common-jpa` | — | common | `META-INF/orm.xml` embeddable mappings |
| `sevenow-domain` | `…domain` | common, common-jpa | **every** entity (`Store`, `Product`, `StoreCatalog`, `Customer`, `Driver`, `Plan`, `Action`, `Order`, `OrderLineItem(s)`, `OrderRevision`, …) and repository (`StoreRepository`, `CustomerRepository`, `DriverRepository`, `OrderRepository`), `DomainConfiguration` |
| `sevenow-customer-service-api` | `…customerservice.api.web` | common | `CreateCustomerRequest/Response` |
| `sevenow-customer-service` | `…customerservice` | common-swagger, customer-service-api, domain | `CustomerService` (create, `validateOrderForCustomer`), `CustomerController` |
| `sevenow-store-service-api` | `…storeservice.events` | common | `CreateStoreRequest`, `StoreCatalogDTO`, `ProductDTO` |
| `sevenow-store-service` | `…storeservice` | common-swagger, store-service-api, domain | `StoreService`, `StoreController` |
| `sevenow-driver-service-api` | `…driverservice.api` | common | `CreateDriverRequest/Response`, `DriverAvailability` |
| `sevenow-driver-service` | `…driverservice` | common-swagger, driver-service-api, domain | `DriverService`, `DriverController` |
| `sevenow-order-service-api` | `…orderservice.api` | common | `CreateOrderRequest/Response`, `ReviseOrderRequest`, `OrderAcceptance`, `OrderDetails` |
| `sevenow-order-service` | `…orderservice` | common-swagger, domain, customer-service-api, store-service-api, order-service-api, **customer-service** | `OrderService` (create + substitution rule, cancel, revise, accept + driver scheduling, state transitions), `OrderController`, `TicketController`, `GetOrderResponse` |
| `sevenow-application` | `…` | the four `*-service` modules | main class, `application.properties`, Dockerfile |

## 3. REST surface per module

| Module | Endpoints |
|---|---|
| customer-service | `POST /customers`, `GET /customers/{customerId}` |
| store-service | `POST /stores`, `GET /stores/{storeId}` |
| driver-service | `POST /drivers`, `POST /drivers/{driverId}/availability`, `GET /drivers/{driverId}` |
| order-service | `POST /orders`, `GET /orders/{orderId}`, `GET /orders?customerId=`, `POST /orders/{orderId}/{cancel,revise,accept,picking,packed,ready,pickedup,delivered}` |

All controllers live in the same servlet; there is no API gateway and no inter-module HTTP — collaboration is by Java
method calls and shared JPA entities inside one transaction.

## 4. Table ownership per module

Schema after `V1` (FTGO), `V2__rebrand_7now.sql`, `V3__seed_7now_data.sql`. "Owner" = the module whose service writes the
table; "also read/written by" lists every other module that touches it, which is exactly what extraction has to untangle.

| Table | Owner module | Entity / mapping | Key columns | Also read/written by |
|---|---|---|---|---|
| `customers` | customer-service | `Customer` (`@GeneratedValue` via `hibernate_sequence`) | `id`, `first_name`, `last_name` | order-service reads through `CustomerService.validateOrderForCustomer` (Java call, same tx) |
| `hibernate_sequence` | customer-service (only sequence user) | Hibernate table generator | `next_val` | — |
| `stores` | store-service | `Store` | `id`, `store_number`, `name`, `street1..zip` | order-service: `OrderService.createOrder` loads `Store` via `StoreRepository`; `Order.store` is a JPA `@ManyToOne` and `GET /orders` reads `store.name` |
| `store_products` | store-service | `Store.products` `@ElementCollection` of `Product` | `store_id` (FK → stores), `id`, `name`, `price`, `on_hand_quantity`, `substitute_product_id` | order-service reads `Store.findProduct` for price, stock and substitution at order time |
| `drivers` | driver-service | `Driver` | `id`, `available`, `first_name`, `last_name`, `street1..zip` | order-service: `DriverRepository.findAllAvailable()` and `Order.assignedDriver` `@ManyToOne` in `OrderService.scheduleDelivery` |
| `driver_actions` | driver-service | `Driver.plan.actions` `@ElementCollection` of `Action` | `driver_id` (FK → drivers), `order_id` (FK → orders), `type`, `time` | **written by order-service** (`Driver.addAction` during accept); `Action.order` is a `@ManyToOne` to `Order` |
| `orders` | order-service | `Order` | `id`, `version`, `order_state`, `customer_id`, `store_id` (FK → stores), `assigned_driver_id` (FK → drivers), `accept_time`, `picking_time`, `packed_time`, `ready_for_pickup_time`, `picked_up_time`, `delivered_time`, `ready_by`, `order_minimum`, `delivery_*`, `payment_token` | driver-service reads orders through `Action.order` when serving `GET /drivers/{id}` plan |
| `order_line_items` | order-service | `Order.orderLineItems` `@ElementCollection` of `OrderLineItem` | `order_id` (FK → orders), `product_id`, `name`, `price`, `quantity`, `substituted_from_product_id` | — |
| `flyway_schema_history` | sevenow-flyway | — | — | — |

### Cross-module seams (what a split must address)

1. **`orders.store_id` → `stores`** and the `Order.store` `@ManyToOne`: order-service needs store name, product price,
   `on_hand_quantity` and `substitute_product_id` at order time. Candidate: a `GET /stores/{id}` call or a replicated
   product snapshot in order-service (the line item already copies `name`/`price`).
2. **`orders.assigned_driver_id` → `drivers`** and **`driver_actions.order_id` → `orders`**: `OrderService.scheduleDelivery`
   mutates both aggregates in one transaction (picks a random available driver, appends PICKUP/DROPOFF actions). Candidate:
   a delivery/driver service that reacts to an `OrderAccepted` event and owns `driver_actions` (store only `order_id`, no FK).
3. **`customers`**: order-service calls `CustomerService.validateOrderForCustomer(customerId, total)` in-process. Pure
   validation → easy first extraction (sync call or saga step).
4. **`sevenow-domain` module**: every entity and repository is shared. Extraction has to move each aggregate into its
   owning service module first (`Store`/`Product` → store-service, `Driver`/`Plan`/`Action` → driver-service,
   `Customer` → customer-service, `Order*` → order-service) and replace entity references with ids.
5. **`hibernate_sequence`** is only used by `Customer`; the other entities use `auto_increment`.

### Suggested extraction order

| # | Service | Tables | Why first/last |
|---|---|---|---|
| 1 | Customer | `customers`, `hibernate_sequence` | single table, one read-only in-process call from orders |
| 2 | Store / Catalog | `stores`, `store_products` | read-only from orders; price/stock lookup becomes an API call or a local replica |
| 3 | Delivery (Driver) | `drivers`, `driver_actions` | needs the accept flow to become event-driven (`OrderAccepted` → schedule) |
| 4 | Order (remaining monolith) | `orders`, `order_line_items` | owner of the state machine and substitution rule |

## 5. Order state machine

```
            POST /orders                 /accept                 /picking            /packed
 (new) ──────────────▶ APPROVED ──────────────▶ ACCEPTED ──────────────▶ PICKING ──────────▶ PACKED
                          │                 (driver scheduled)                                  │ /ready
                          │ /cancel  /revise (stay APPROVED)                                    ▼
                          ▼                                                             READY_FOR_PICKUP
                      CANCELLED                                                                 │ /pickedup
                                                                                                ▼
                                                                  DELIVERED ◀── /delivered ── PICKED_UP
```

Transitions are enforced in `Order` (`acceptTicket`, `notePicking`, `notePacked`, `noteReadyForPickup`, `notePickedUp`,
`noteDelivered`, `cancel`, `revise`); any other move throws `UnsupportedStateTransitionException`. `PICKING` and
`PACKED` replace FTGO's `PREPARING` and are the store-associate steps of 7NOW fulfillment.

## 6. Substitution rule (order-service)

`OrderService.makeOrderLineItems` — for each requested `productId`:

1. unknown id → `InvalidProductIdException` → `400`;
2. `onHandQuantity > 0` → normal line item;
3. `onHandQuantity == 0` and `substituteProductId` set and the substitute is in stock → line item for the substitute
   (its name and price), `substitutedFromProductId` = requested id;
4. otherwise the id is collected and the whole order is rejected with `ProductOutOfStockException` → `400`
   `{ "rejectedProductIds": [...] }`.

Stock is **not** decremented when ordering (not required by the demo); the rule is evaluated against the current
`store_products.on_hand_quantity`.

## 7. Seed data (`V3`)

Store `#38114` (Irving, TX) with 18 products — cold drinks, snacks, pizza, hot food — including out-of-stock
`RED-BULL-12OZ`→`MONSTER-16OZ`, `DORITOS-NACHO-9OZ`→`DORITOS-COOLRANCH-9OZ`, `PIZZA-PEPPERONI-WHOLE`→`PIZZA-CHEESE-WHOLE`
and `WINGS-8PC` (no substitute); drivers `1` (available) and `2`; customer `1`.

## 8. Migrating an existing FTGO database

`V2`/`V3` are written to run on a populated FTGO schema as well as on an empty one:

| Legacy data | What the migration does |
|---|---|
| `orders.order_state = 'PREPARING'` | becomes `PICKING`; `preparing_time` is copied into `picking_time` before the column is dropped |
| `restaurant_menu_items` rows | become `store_products` with `on_hand_quantity = 0` and no substitute — i.e. **unavailable until stock is loaded**. The migration does not invent inventory; load counts (update `on_hand_quantity`) before taking orders against a migrated store |
| rows with ids `1`/`2` in `stores`, `drivers`, `customers` | `V3` uses `insert ignore`, so existing rows win and the demo seed is skipped for that id; the demo catalog is only loaded into store 1 if it is the seeded `#38114`, and only for product ids not already present (`store_products` keeps its FTGO shape: no primary key, duplicates are left as they are) |
| `hibernate_sequence` | set to `greatest(current, 1000, max(customers.id) + 1)` — never moved backwards |
