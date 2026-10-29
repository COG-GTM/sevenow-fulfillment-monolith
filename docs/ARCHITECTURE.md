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
| `stores` | **inventory-service** (`sevenow_inventory`) — was store-service | `com.sevenow.inventory.domain.Store` | `id`, `store_number`, `name`, `street1..zip` | monolith keeps its own `sevenow.stores` copy so `sevenow-store-service` and `Order.store` still work; new consumers use `GET /stores/{id}` on inventory-service (8082) |
| `store_products` | **inventory-service** (`sevenow_inventory`) — was store-service | `Store.products` `@ElementCollection` of `Product` | `store_id` (FK → stores), `id`, `name`, `price`, `on_hand_quantity`, `substitute_product_id` (unique `(store_id, id)` in `sevenow_inventory`) | stock is decremented only by inventory-service `POST /reservations`; the monolith's `OrderService.createOrder` still reads its local copy for price/substitution (does not decrement) |
| `drivers` | **delivery-service** (`sevenow_delivery`) — was driver-service | `com.sevenow.delivery.domain.Driver` | `id`, `available`, `first_name`, `last_name`, `street1..zip` | order-service calls `POST /deliveries` via `DeliveryServiceClient` when `sevenow.delivery-service.url` is set; otherwise `LocalDeliveryScheduler` uses the monolith's local `sevenow.drivers` (in-process tests) |
| `driver_actions` | **delivery-service** (`sevenow_delivery`) — was driver-service | `Driver.plan.actions` `@ElementCollection` of `Action` | `driver_id` (FK → drivers), `order_id` (**plain bigint, no FK**), `type`, `time` | written only by delivery-service `POST /deliveries`; `Action` holds `orderId`, no `@ManyToOne Order` |
| `orders` | order-service | `Order` | `id`, `version`, `order_state`, `customer_id`, `store_id` (FK → stores), `assigned_driver_id` (plain column since `V4`, holds a delivery-service driver id; `Order.assignedDriverId` is a `Long`, not a `@ManyToOne`), `accept_time`, `picking_time`, `packed_time`, `ready_for_pickup_time`, `picked_up_time`, `delivered_time`, `ready_by`, `order_minimum`, `delivery_*`, `payment_token` | `GET /orders/{id}` reads `driverActions` from delivery-service `GET /drivers/{id}` filtered by `orderId` |
| `order_line_items` | order-service | `Order.orderLineItems` `@ElementCollection` of `OrderLineItem` | `order_id` (FK → orders), `product_id`, `name`, `price`, `quantity`, `substituted_from_product_id` | — |
| `flyway_schema_history` | sevenow-flyway | — | — | — |

### Cross-module seams (what a split must address)

> Status: seams 1 and 2 are addressed by `services/inventory-service` and `services/delivery-service` — see §9.

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

## 9. Extracted services

Two services have been carved out of the ownership map above. Each is a standalone Spring Boot 2.0.3 / Java 8 app
(single `build.gradle` applying `SevenowServicePlugin` + `IntegrationTestsPlugin` + the Flyway plugin), with its own schema,
Flyway migrations in `src/main/resources/db/migration` (also applied on startup), its own copies of the value objects
(`Money`/`Address`/`PersonName`, mapped with `@Embeddable` instead of `orm.xml` — a root `META-INF/orm.xml` inside a Boot fat
jar is registered twice and fails with "Duplicate XML entry"), Actuator + Micrometer Prometheus (`/actuator/prometheus`), a Springfox Docket on its own package
(`/swagger-ui.html`) and a Dockerfile. Both are in `settings.gradle` and `docker-compose.yml`; `mysql/schema.sql` creates
their schemas.

| Service | Path / package | Port | Schema | Owns | Migrations |
|---|---|---|---|---|---|
| inventory-service | `services/inventory-service`, `com.sevenow.inventory` | 8082 | `sevenow_inventory` | `stores`, `store_products`, `reservations`, `reservation_items` | `V1` stores/products (V1+V2 monolith shapes), `V2` store `#38114` + 18 products, `V3` reservations |
| delivery-service | `services/delivery-service`, `com.sevenow.delivery` | 8083 | `sevenow_delivery` | `drivers`, `driver_actions` | `V1` drivers/driver_actions (`order_id` plain bigint), `V2` drivers 1 and 2 |

Build / migrate one service on its own: `./build-and-test-service.sh inventory-service` (starts MySQL, runs
`:<service>:flywayMigrate`, `build`, `integrationTest` against the real schema, then builds its image).
`./build-and-test-all.sh` migrates all three schemas and runs everything, including `ExtractedServicesEndToEndTests`.

### inventory-service contract

| Method | Path | Body → response |
|---|---|---|
| POST | `/stores` | same `CreateStoreRequest` as the monolith → `{"id"}` |
| GET | `/stores/{storeId}` | store + `products[]` with `id`, `name`, `price`, `onHandQuantity`, `substituteProductId`; 404 if unknown |
| GET | `/stores/{storeId}/products/{productId}` | one product (price/stock lookup); 404 if unknown |
| POST | `/reservations/resolve` | `{storeId, items:[{productId, quantity}]}` → `{storeId, items:[…resolved]}`; dry run, no stock change |
| POST | `/reservations` | `{storeId, items:[{productId, quantity}]}` → **201** `{id, storeId, status:"RESERVED", createdTime, items:[{productId, name, price, quantity, substitutedFromProductId}]}` |
| POST | `/reservations/{id}/release` | restores on-hand quantities, `status:"RELEASED"`; idempotent (second release is a no-op); 404 if unknown |
| GET | `/reservations/{id}` | the reservation; 404 if unknown |

Substitution rule (`SubstitutionRule`, moved from `OrderService.makeOrderLineItems`): unknown product id → **400**
`{"invalidProductId"}`; `onHandQuantity >= quantity` → as is; otherwise the configured `substituteProductId` if *it* has
`>= quantity` on hand (line item gets `substitutedFromProductId`); otherwise rejected. Any rejection → **409**
`{"rejectedProductIds":[…]}` (same body as the monolith's 400 on `POST /orders`). Unlike the monolith, the check is
quantity-aware rather than `> 0`.

Atomicity: `ReservationService.reserve` runs in one transaction; each line is a conditional
`update store_products set on_hand_quantity = on_hand_quantity - :q where store_id = :s and id = :p and on_hand_quantity >= :q`.
If any update hits 0 rows (a concurrent reservation won), the request fails with 409 and all decrements roll back, so
stock never goes negative (`ReservationIntegrationTest.concurrentReservationsNeverOversell`). `reservations.version`
is an optimistic lock guarding concurrent releases.

### delivery-service contract

| Method | Path | Body → response |
|---|---|---|
| POST | `/drivers` | `{name:{firstName,lastName}, address}` → `{"id"}` |
| POST | `/drivers/{driverId}/availability` | `{available}`; 404 if unknown |
| GET | `/drivers/{driverId}` | `{id, name, address, available, plan:{actions:[{type, time, orderId}]}}`; 404 if unknown |
| POST | `/deliveries` | `{orderId, readyBy, deliveryAddress?, eventType?:"OrderAccepted"}` → `{orderId, driverId, deliveryAddress, actions:[PICKUP, DROPOFF]}`; **409** if no driver is available |

Scheduling (`DeliveryService.schedule`, moved from `OrderService.scheduleDelivery`): picks a random driver from
`DriverRepository.findAllAvailable()`, appends `PICKUP` (no time) and `DROPOFF` at `readyBy + 30 min`, both carrying
`orderId`. Idempotent per `orderId`: if the order already has actions, the existing driver is returned unchanged.
`deliveryAddress` is echoed back but not persisted (`driver_actions` has no address columns).

### Monolith side

- `sevenow-store-service` is unchanged and still serves `/stores` from `sevenow.stores` so the monolith stays deployable;
  inventory-service is the owner of record and the only writer of stock. Moving order creation onto
  `POST /reservations` (and dropping the local copy) is the next step.
- `Order.assignedDriver` (`@ManyToOne Driver`) is now `Order.assignedDriverId` (`Long`, column `assigned_driver_id`);
  `sevenow-flyway` `V4` drops the `orders_assigned_driver_id` FK.
- `OrderService.accept` delegates to a `DeliveryScheduler`. `OrderConfiguration` picks `DeliveryServiceClient`
  (RestTemplate → `POST {url}/deliveries` with an `OrderAccepted` payload, and `GET {url}/drivers/{id}` for
  `GET /orders/{id}` `driverActions`) when `sevenow.delivery-service.url` is set (docker-compose sets
  `SEVENOW_DELIVERY_SERVICE_URL=http://delivery-service:8083`), else `LocalDeliveryScheduler` (the old in-process
  behaviour, used by `SevenowApplicationTest`). The HTTP call runs inside the accept transaction; if that transaction
  rolls back after delivery-service committed, retrying the accept is safe because `POST /deliveries` is idempotent.
- In the compose deployment, drivers are created and made available on delivery-service (`:8083/drivers`); the
  monolith's own `/drivers` endpoints and `sevenow.drivers` table are only used in local mode.

