# 7NOW Fulfillment Monolith — Threat Model

Repository: `COG-GTM/sevenow-fulfillment-monolith`. Built from the code, the in-repo infrastructure (`deployment/kubernetes`, `docker-compose.yml`, `mysql/`, `.circleci`) and the repository owner's answers. Any fact marked **(user)** came from the owner. Everything else is read from the code.

## 1. System overview and trust boundaries

- **One deployable:** a Spring Boot 2.0.3 / Java 8 app (`sevenow-application`, `SevenowApplicationMain`). It runs on port 8080 in the container (8081 on the host) and serves every REST endpoint.
- **One database:** a MySQL 5.7 schema `sevenow`, owned by Flyway (`sevenow-flyway`). The app connects as `mysqluser`.
- **Gateway (user):** in a real deployment the app sits behind an API gateway. The gateway authenticates customers, drivers and store staff. The app trusts any caller the gateway lets through. The app itself has no Spring Security, no identity, and no per-caller authorization. Any authenticated caller can do anything the API allows, on any record.
- **Trust boundaries:**
  1. Internet → gateway (authentication).
  2. Gateway → app (no further checks; any authenticated identity is treated alike).
  3. App → MySQL (shared static credentials).
  4. Cluster/CI operators → k8s Secrets, manifests, images.

## 2. Components

| Component | Code |
|---|---|
| **Order service** | `sevenow-order-service`, `sevenow-order-service-api` — `OrderController` (`/orders`, cancel/revise, the state transitions accept/picking/packed/ready/pickedup/delivered), `OrderService` (substitution rule, totals, driver scheduling) |
| **Customer service** | `sevenow-customer-service`, `sevenow-customer-service-api` — `CustomerController` (`/customers`), `CustomerService.validateOrderForCustomer` |
| **Store & catalog service** | `sevenow-store-service`, `sevenow-store-service-api` — `StoreController` (`/stores`), catalog, prices, stock |
| **Driver service** | `sevenow-driver-service`, `sevenow-driver-service-api` — `DriverController` (`/drivers`, availability, delivery plans) |
| **Application runtime & shared domain** | `sevenow-application` (main, Dockerfile, `application.properties`), `sevenow-domain` (all JPA entities and repositories), `sevenow-common`, `sevenow-common-jpa`, `common-swagger` (Swagger UI), Actuator/Micrometer |
| **Database & migrations** | `sevenow-flyway` (V1–V3), `mysql/` (image, `schema.sql` grants) |
| **Deployment & build** | `deployment/kubernetes` (MySQL StatefulSet, `sevenow-db-secret`, scripts), `docker-compose.yml`, `.circleci`, `buildSrc`, Gradle build, `sevenow-end-to-end-tests*`, `sevenow-test-util` |

## 3. Entry points and who can reach them

Every caller the gateway has authenticated (customer, driver, store staff) can reach all of these:

- `POST /orders` and `GET /orders/{id}`, `GET /orders?customerId=` — create, read and list orders for **any** customer ID.
- `POST /orders/{id}/cancel|revise|accept|picking|packed|ready|pickedup|delivered` — any caller can move any order through its lifecycle.
- `POST /customers`, `GET /customers/{id}` — customer names.
- `POST /stores`, `GET /stores/{id}` — create stores and catalogs (prices, stock).
- `POST /drivers`, `POST /drivers/{id}/availability`, `GET /drivers/{id}` — driver records and delivery plans.
- `/swagger-ui.html`, `/v2/api-docs`, `/actuator/*` — reachable through the gateway unless it filters them.
- MySQL port 3306: published on the host by docker-compose, and a headless Service in k8s.

## 4. Assets (user: all equally important)

- Customer PII: names, delivery addresses on orders.
- Order integrity: state machine, totals (`Money`), substitutions, driver assignment.
- Store catalog: prices, stock (`on_hand_quantity`).
- Driver data and delivery plans.
- DB credentials and infrastructure: `mysqluser`/`mysqlpw`, root `rootpassword`, the `sevenow-db-secret` k8s Secret.
- Availability of the single app and DB.

## 5. Attacker profiles (user: all matter)

1. **Anonymous internet user.** Reaches the app only if the gateway lets them through. That includes self-registering as a customer, or reaching the app or MySQL directly if they are exposed.
2. **Authenticated customer** going after other customers' orders and data (IDOR/BOLA), or tampering with prices, totals and states.
3. **Driver or store staff** changing orders, stores or drivers they don't own.
4. **Insider with cluster, DB or CI access.** Can read secrets and manifests, or connect to MySQL with the shared credentials.

## 6. Existing controls

- The gateway authenticates callers **(user)**. The app has no authN or authZ of its own.
- JPA/Spring Data repositories, so data access is parameterized by default.
- Order state transitions are enforced in the domain (`UnsupportedStateTransitionException`). That checks legality, not who is allowed to make the change.
- `CustomerService.validateOrderForCustomer` checks the order total.
- Flyway owns the schema (`spring.jpa.generate-ddl=false` in the app).

## 7. Out of scope / accepted risk

- **None (user): report everything.** Hardcoded dev credentials, missing authN/authZ, Swagger/Actuator exposure, and test/build code are all in scope.

## 8. How findings are judged

- Start from the scanner's severity, which already weighs how exploitable each finding is.
- Missing **authentication** in the app is partly covered by the gateway **(user)**. A finding whose impact depends *only* on an unauthenticated caller reaching the app is still in scope: anonymous users matter, and so does gateway bypass or direct exposure. It rarely moves.
- Missing **authorization or ownership** checks are exploitable by every authenticated customer, driver and staff member **(user)**. Nothing rules them out.
- Hardcoded or shared DB credentials are in scope: the insider attacker matters **(user)**.

## 9. Open questions

- Which routes does the gateway expose, and does it block `/actuator/*` and Swagger?
- Does the gateway pass caller identity (headers/JWT) to the app? Does any downstream code trust it?
- Is MySQL ever reachable from outside the cluster in a real deployment?
- Are there real secret-management and TLS settings for the production DB (the repo uses `useSSL=false`)?
