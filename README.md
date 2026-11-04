<p align="center">
  <img src="docs/assets/7now-tile.png" alt="7NOW delivery" width="420"/>
</p>

# 7NOW Fulfillment Monolith

<img src="docs/assets/7-eleven-logo.svg" alt="7-Eleven" height="48" align="right"/>

Demo back end for **7NOW**, 7-Eleven's convenience-store delivery service
([7-eleven.com/7now](https://www.7-eleven.com/7now/home)). Customers order cold drinks, snacks, pizza
and hot food from a nearby 7-Eleven store; store associates pick and pack the order; a driver delivers it.

It is a fork of the [FTGO monolith](https://github.com/microservices-patterns/ftgo-monolith) from
*Microservices Patterns*, rebranded to the 7NOW domain **without changing the architecture**: one Spring Boot
application (`sevenow-application`) on one MySQL schema (`sevenow`). It exists as the starting point for a
monolith-to-microservices refactoring exercise; see [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) for the module map,
table ownership per module and extraction notes.

## Domain

| Concept | Entity | Table | Notes |
|---|---|---|---|
| Store | `Store` | `stores`, `store_products` | `storeNumber`, `Address`, catalog of `Product`s |
| Product | `Product` (embeddable) | `store_products` | `price`, `onHandQuantity` per store, optional `substituteProductId` |
| Customer | `Customer` | `customers` | |
| Driver | `Driver` | `drivers`, `driver_actions` | availability + delivery plan (PICKUP / DROPOFF actions) |
| Order | `Order`, `OrderLineItem` | `orders`, `order_line_items` | line items keep `substitutedFromProductId` when a swap happened |

### Order lifecycle

```
APPROVED ──accept──▶ ACCEPTED ──picking──▶ PICKING ──packed──▶ PACKED ──ready──▶ READY_FOR_PICKUP ──pickedup──▶ PICKED_UP ──delivered──▶ DELIVERED
    │
    └──cancel──▶ CANCELLED
```

### Substitution rule

At order time, for each line item: if the product's `onHandQuantity` at the store is zero, the configured
`substituteProductId` is used instead (the line item is priced as the substitute and records
`substitutedFromProductId`). If no substitute is configured (or it is also out of stock), the line item is
rejected and `POST /orders` returns **400** with `rejectedProductIds`.

## Running

Requirements: JDK 8, Docker with Compose v2 (`docker compose`; the v1 `docker-compose` binary also works).

```bash
export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64   # or equivalent
./build-and-test-all.sh                               # build, migrate, unit + e2e tests, tear down
./build-and-test-all.sh --keep-running                # same, but leave MySQL + app up
```

`./build-and-run.sh` builds and starts MySQL + the app; `./start-infrastructure-services.sh` starts MySQL alone;
`./mysql-cli.sh` opens a MySQL shell. Set `DOCKER_HOST_IP` if Docker is not on localhost.

* Application: http://localhost:8081
* Swagger UI: http://localhost:8081/swagger-ui.html
* MySQL: `127.0.0.1:3306` (TLS required), schema `sevenow`

MySQL credentials are not committed. `. ./set-env.sh` (sourced by the scripts above) generates random passwords
into a git-ignored `.env` on first use; values already exported in the environment (e.g. from a CI secret store)
take precedence. `docker compose`, the app and the Gradle tasks fail fast when they are missing.

| Variable | Used by | Privileges |
|---|---|---|
| `SPRING_DATASOURCE_USERNAME` / `SPRING_DATASOURCE_PASSWORD` | app, `waitForMySql` | `SELECT, INSERT, UPDATE, DELETE` on `sevenow.*` |
| `FLYWAY_USER` / `FLYWAY_PASSWORD` | `:sevenow-flyway:flywayMigrate` | DDL + DML on `sevenow.*` |
| `MYSQL_ROOT_PASSWORD` | `./mysql-cli.sh` | root, `localhost` inside the container only |

Both accounts are created `REQUIRE SSL` by `mysql/init-sevenow.sh`; set `SEVENOW_DB_ALLOWED_HOST` on the
`mysql` service to restrict them to a host pattern. The default JDBC URLs use
`useSSL=true&requireSSL=true` with the container's self-signed certificate (`verifyServerCertificate=false`);
outside local Docker, override `SPRING_DATASOURCE_URL` and `FLYWAY_URL` with `verifyServerCertificate=true` and a
`trustCertificateKeyStoreUrl`. Port 3306 is published on `127.0.0.1` only (`MYSQL_PUBLISH_ADDRESS` overrides it;
`set-env.sh` sets it to the remote host's address when `DOCKER_HOST` points at a remote daemon).

On Kubernetes, `kubernetes-deploy-all.sh` creates `sevenow-db-secret` (random values unless exported) and mounts
`mysql/init-sevenow.sh` into the stock `mysql:5.7` StatefulSet; `kubernetes-delete-all.sh` removes both. The init
script only runs on an empty volume, so run `kubernetes-delete-volumes.sh` to drop a database created with the old
accounts.

Flyway (`sevenow-flyway`) runs `V1` (original FTGO schema), `V2__rebrand_7now.sql` (rename + new columns) and
`V3__seed_7now_data.sql` (store #38114 in Irving, TX with 18 products across cold drinks, snacks, pizza and hot
food, two drivers and one customer). Seeded out-of-stock examples: `PIZZA-PEPPERONI-WHOLE` → substitute
`PIZZA-CHEESE-WHOLE`; `WINGS-8PC` has no substitute and is rejected.
The migrations are safe to run on a populated FTGO database (`PREPARING` orders become `PICKING`, migrated products
start at `on_hand_quantity = 0`, seed rows are `insert ignore`); see `docs/ARCHITECTURE.md` §8.

## REST API

| Method | Path | Module | Purpose |
|---|---|---|---|
| POST | `/customers` | customer-service | create customer |
| GET | `/customers/{customerId}` | customer-service | get customer |
| POST | `/stores` | store-service | create store with `storeNumber`, `address`, `catalog.products[]` |
| GET | `/stores/{storeId}` | store-service | store incl. products and on-hand quantities |
| POST | `/drivers` | driver-service | create driver |
| POST | `/drivers/{driverId}/availability` | driver-service | set availability |
| GET | `/drivers/{driverId}` | driver-service | driver with delivery plan |
| POST | `/orders` | order-service | create order (`customerId`, `storeId`, `lineItems[{productId, quantity}]`); applies substitution rule |
| GET | `/orders/{orderId}` | order-service | order state, total, line items, assigned driver and actions |
| GET | `/orders?customerId=` | order-service | order history |
| POST | `/orders/{orderId}/cancel`, `/revise` | order-service | customer actions while `APPROVED` |
| POST | `/orders/{orderId}/accept` | order-service | store accepts (`readyBy`), driver scheduled |
| POST | `/orders/{orderId}/picking`, `/packed`, `/ready` | order-service | store fulfillment steps |
| POST | `/orders/{orderId}/pickedup`, `/delivered` | order-service | driver steps |

Example:

```bash
curl -s -X POST localhost:8081/orders -H 'Content-Type: application/json' -d '{
  "customerId": 1, "storeId": 1,
  "lineItems": [{"productId": "SLURPEE-CHERRY-M", "quantity": 2}, {"productId": "PIZZA-PEPPERONI-WHOLE", "quantity": 1}]
}'
```

## Repository layout

```
sevenow-application          Spring Boot main + Dockerfile (the one deployable)
sevenow-domain               JPA entities and repositories shared by every module
sevenow-order-service(-api)  orders, state machine, substitution, Swagger'd REST
sevenow-store-service(-api)  stores + product catalog
sevenow-customer-service(-api)
sevenow-driver-service(-api)
sevenow-common, sevenow-common-jpa, common-swagger, sevenow-test-util
sevenow-flyway               db/migration V1..V3
sevenow-end-to-end-tests(-common)
docs/                        ARCHITECTURE.md, brand assets
```

7NOW and 7-Eleven marks are trademarks of 7-Eleven, Inc.; used here for demo purposes only.
