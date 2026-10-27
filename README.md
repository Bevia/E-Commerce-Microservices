# E-Commerce Microservices

A local learning project that demonstrates authentication, order management, and payment processing with Java 17, Spring Boot 3.5, Docker, and Kubernetes.

The three services issue and validate the same JSON Web Token (JWT), but each service owns its own H2 in-memory database.

## Architecture

```mermaid
flowchart LR
    Client[Client or Postman]

    subgraph Docker[Docker Compose backend network]
        Auth[Auth service<br/>8083]
        Order[Order service<br/>8080]
        Payment[Payment service<br/>8081]

        AuthDB[(H2 authdb)]
        OrderDB[(H2 orderdb)]
        PaymentDB[(H2 paymentdb)]
    end

    Client -->|Register and sign in| Auth
    Client -->|Bearer token| Order
    Client -->|Bearer token| Payment

    Auth --> AuthDB
    Order --> OrderDB
    Payment --> PaymentDB
```

The services currently expose separate host ports:

| Service | Host address | Container port | Responsibility |
|---|---|---:|---|
| Authentication | `http://localhost:8083` | 8083 | Registration, sign-in, and token issuance |
| Orders | `http://localhost:8090` | 8080 | Order creation, lookup, status changes, and deletion |
| Payments | `http://localhost:8081` | 8081 | Simulated payment processing and payment lookup |

Port `8090` is used for orders on the host because port `8080` is already used by another local container.

## Quick start

### Prerequisites

- Docker Desktop
- Git
- Postman or another Hypertext Transfer Protocol (HTTP) client
- Java 17 only if you want to run Maven tests outside Docker

### Start the application

From the repository root:

```bash
docker compose up -d --build
docker compose ps
```

All three services should become `healthy`.

Check their readiness:

```bash
curl http://localhost:8083/actuator/health/readiness
curl http://localhost:8090/actuator/health/readiness
curl http://localhost:8081/actuator/health/readiness
```

<<<<<<< HEAD
| Endpoint           | Method | Description       | Auth Required |
| ------------------ | ------ | ----------------- | ------------- |
| `/api/auth/signup` | POST   | Register new user | No            |
| `/api/auth/signin` | POST   | Login & get JWT   | No            |

**Database Tables:**

- `USERS` - User accounts (id, username, email, password_hash)
- `ROLES` - Available roles (ROLE_USER, ROLE_MODERATOR, ROLE_ADMIN)
- `USER_ROLES` - User-role assignments

**Key Features:**

- BCrypt password hashing
- JWT token generation (1-hour expiry)
- Role-based user registration
=======
Stop the application:

```bash
docker compose down
```

For the complete Docker and Kubernetes workflow, see [DOCKER_KUBERNETES.md](DOCKER_KUBERNETES.md).
>>>>>>> 2fdf56d97d1ddb5412d4ae897a67dcec991d02b5

## Authentication

Register a user:

<<<<<<< HEAD
**Purpose:** Manage customer orders with user tracking.

| Endpoint                    | Method | Description         | Auth Required |
| --------------------------- | ------ | ------------------- | ------------- |
| `/api/orders`               | GET    | List all orders     | ✅ Yes        |
| `/api/orders`               | POST   | Create new order    | ✅ Yes        |
| `/api/orders/{id}`          | GET    | Get order by ID     | ✅ Yes        |
| `/api/orders/user/{userId}` | GET    | Get orders by user  | ✅ Yes        |
| `/api/orders/{id}/status`   | PUT    | Update order status | ✅ Yes        |
| `/api/orders/{id}`          | DELETE | Delete order        | ✅ Yes        |

**Database Tables:**

- `ORDERS` - Orders with username from JWT token
- `ORDER_ITEM` - Individual items in orders

**Key Features:**

- Extracts username from JWT automatically
- Links orders to authenticated user
- Stateless JWT validation

---

### Payment Service (Port 8081)

**Purpose:** Process and track payments for orders.

| Endpoint                | Method | Description       | Auth Required |
| ----------------------- | ------ | ----------------- | ------------- |
| `/api/payments`         | GET    | List all payments | ✅ Yes        |
| `/api/payments/process` | POST   | Process payment   | ✅ Yes        |
| `/api/payments/{id}`    | GET    | Get payment by ID | ✅ Yes        |

**Database Tables:**

- `PAYMENT` - Payment records with transaction IDs

**Key Features:**

- Generates unique transaction IDs
- Links payments to order IDs
- Supports multiple payment methods

---

## 🔑 JWT Token Structure

```json
{
  "header": {
    "alg": "HS256"
  },
  "payload": {
    "sub": "username", // Subject = username
    "iat": 1764937069, // Issued at
    "exp": 1764940669 // Expiration (1 hour)
  },
  "signature": "..." // HMAC-SHA256 signed
}
```

**Shared Secret (Base64):**

```
WW91clN1cGVyU2VjcmV0S2V5Rm9ySldUQXV0aGVudGljYXRpb25XaGljaFNob3VsZEJlVmVyeUxvbmdBbmRDb21wbGV4
```

All services share this secret to validate tokens independently (stateless).

---

## 🚀 Quick Start

### 1. Start All Services

**Terminal 1 - Auth Service:**

```bash
cd /Users/vincentbevia/IdeaProjects/AuthMicroservice && ./mvnw spring-boot:run
```

**Terminal 2 - Order Service:**

```bash
cd /Users/vincentbevia/IdeaProjects/OrderMicroservice && ./mvnw spring-boot:run
```

**Terminal 3 - Payment Service:**

```bash
cd /Users/vincentbevia/IdeaProjects/PaymentMicroservice && ./mvnw spring-boot:run
```

### 2. Register a User

```bash
curl -X POST http://localhost:8083/api/auth/signup \
  -H "Content-Type: application/json" \
  -d '{"username":"john","email":"john@example.com","password":"secret123","role":["user"]}'
```

### 3. Login & Get Token

```bash
curl -X POST http://localhost:8083/api/auth/signin \
  -H "Content-Type: application/json" \
  -d '{"username":"john","password":"secret123"}'
```

Response:

=======
```http
POST http://localhost:8083/api/auth/signup
Content-Type: application/json
```

```json
{
  "username": "testuser",
  "email": "testuser@example.com",
  "password": "password123"
}
```

Sign in:

```http
POST http://localhost:8083/api/auth/signin
Content-Type: application/json
```

>>>>>>> 2fdf56d97d1ddb5412d4ae897a67dcec991d02b5
```json
{
  "username": "testuser",
  "password": "password123"
}
```

<<<<<<< HEAD
### 4. Create an Order
=======
The response contains a `token`. Protected requests must send it as:

```http
Authorization: Bearer <token>
```

Tokens expire after one hour. Sign in again when a valid request begins returning `401 Unauthorized` or `403 Forbidden`.

## Complete business flow

Payment creation and order-status updates are deliberately separate in the current implementation:

```text
Register → Sign in → Create order → Process payment → Mark order PAID → Verify
```

### 1. Create an order

```http
POST http://localhost:8090/api/orders
Authorization: Bearer <token>
Content-Type: application/json
```

```json
{
  "userId": 5,
  "customerName": "Test Customer",
  "totalAmount": 150.00
}
```

Example response:

```json
{
  "id": 4,
  "userId": 5,
  "username": "testuser",
  "status": "PENDING",
  "totalAmount": 150.00,
  "paymentId": null,
  "orderItems": []
}
```

The response field `id` is the order identifier. Use that value as `orderId` in subsequent requests.

The current create-order request accepts `userId`, `customerName`, and `totalAmount`. It does not yet accept `orderItems`; unknown item data is ignored.

### 2. Read the order

```http
GET http://localhost:8090/api/orders/4
Authorization: Bearer <token>
```

Order reads use response Data Transfer Objects (DTOs), so lazily loaded persistence relationships are not serialized after their database transaction has closed.

### 3. Process the payment

```http
POST http://localhost:8081/api/payments/process
Authorization: Bearer <token>
Content-Type: application/json
```

```json
{
  "orderId": "4",
  "amount": 150.00,
  "currency": "EUR",
  "paymentMethod": "TEST_CARD"
}
```

The service simulates a successful payment and returns `COMPLETED`. If retries create multiple payments for one order, lookup by order identifier returns the newest attempt.

### 4. Mark the order as paid

Select `PUT` as the request method and enter only the Uniform Resource Locator (URL):

```http
PUT http://localhost:8090/api/orders/4/status?status=PAID
Authorization: Bearer <token>
```

There is no request body.

### 5. Verify the result

```http
GET http://localhost:8090/api/orders/4
Authorization: Bearer <token>
```

The order should now contain `"status": "PAID"`.

For a complete Postman collection sequence, assertions, cleanup, and troubleshooting, see [POSTMAN.md](POSTMAN.md).

## Endpoint reference

All order and payment endpoints require a valid Bearer token.

### Authentication service

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/auth/signup` | Register a user |
| `POST` | `/api/auth/signin` | Sign in and obtain a token |

### Order service

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/orders` | Create an order |
| `GET` | `/api/orders` | List all orders |
| `GET` | `/api/orders/{id}` | Get one order |
| `GET` | `/api/orders/user/{userId}` | List orders for a user |
| `PUT` | `/api/orders/{id}/status?status=PAID` | Change order status |
| `DELETE` | `/api/orders/{id}` | Delete an order |

### Payment service

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/payments/process` | Process a simulated payment |
| `GET` | `/api/payments` | List all payments |
| `GET` | `/api/payments/{id}` | Get one payment |
| `GET` | `/api/payments/order/{orderId}` | Get the newest payment attempt for an order |
| `PUT` | `/api/payments/{id}` | Update a payment |
| `DELETE` | `/api/payments/{id}` | Delete a payment |

## Running the tests

Each service has its own Maven wrapper:
>>>>>>> 2fdf56d97d1ddb5412d4ae897a67dcec991d02b5

```bash
cd auth-service && ./mvnw test
cd ../order-service && ./mvnw test
cd ../payment-service && ./mvnw test
```

<<<<<<< HEAD
### 5. Process Payment

```bash
curl -X POST http://localhost:8081/api/payments/process \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN_HERE" \
  -d '{"orderId":"1","amount":149.99,"paymentMethod":"CREDIT_CARD"}'
=======
The order and payment suites include repository regression tests for order serialization and repeated payment attempts.

## Project structure

```text
.
├── auth-service/             Authentication service and Dockerfile
├── order-service/            Order service and Dockerfile
├── payment-service/          Payment service and Dockerfile
├── k8s/                      Docker Desktop Kubernetes manifests
├── compose.yaml              Local three-service Docker environment
├── DOCKER_KUBERNETES.md      Container and Kubernetes guide
├── POSTMAN.md                End-to-end Application Programming Interface test guide
└── README.md
>>>>>>> 2fdf56d97d1ddb5412d4ae897a67dcec991d02b5
```

## Technology stack

<<<<<<< HEAD
## 🗄️ H2 Database Consoles

| Service | Console URL                      | JDBC URL                | User | Password |
| ------- | -------------------------------- | ----------------------- | ---- | -------- |
| Auth    | http://localhost:8083/h2-console | `jdbc:h2:mem:authdb`    | sa   | password |
| Order   | http://localhost:8080/h2-console | `jdbc:h2:mem:orderdb`   | sa   | password |
| Payment | http://localhost:8081/h2-console | `jdbc:h2:mem:paymentdb` | sa   | password |

**Useful Queries:**

```sql
-- Auth DB
SELECT * FROM USERS;
SELECT * FROM ROLES;
SELECT u.USERNAME, r.NAME as ROLE FROM USERS u
  JOIN USER_ROLES ur ON u.ID = ur.USER_ID
  JOIN ROLES r ON ur.ROLE_ID = r.ID;

-- Order DB
SELECT * FROM ORDERS;
SELECT ID, USERNAME, TOTAL_AMOUNT, STATUS FROM ORDERS;

-- Payment DB
SELECT * FROM PAYMENT;
```

---

## 🛠️ Technology Stack

| Component        | Technology            |
| ---------------- | --------------------- |
| Framework        | Spring Boot 3.5.0     |
| Language         | Java 17               |
| Security         | Spring Security + JWT |
| JWT Library      | JJWT 0.12.5           |
| Database         | H2 (in-memory)        |
| ORM              | Spring Data JPA       |
| Build Tool       | Maven                 |
| Password Hashing | BCrypt                |
=======
| Area | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3.5 |
| Security | Spring Security and JSON Web Tokens |
| Persistence | Spring Data Java Persistence API (JPA) |
| Development database | H2 in-memory database |
| Build | Maven wrapper |
| Containers | Docker and Docker Compose |
| Orchestration learning environment | Docker Desktop Kubernetes |
| Health checks | Spring Boot Actuator |
>>>>>>> 2fdf56d97d1ddb5412d4ae897a67dcec991d02b5

## Current limitations

- H2 data is held in memory. Recreating a container or Pod deletes that service's data.
- Restarting only one stateful service can leave order and payment identifiers out of sync.
- Creating a payment does not automatically update its order; the client must perform both calls.
- The create-order endpoint does not yet accept or calculate `orderItems`.
- There is no Application Programming Interface (API) gateway; clients address each service separately.
- The included signing key fallback is for local development only. Set `JWT_SECRET` to a secure Base64-encoded value outside local development.
- There is no centralized logging, distributed tracing, rate limiting, or persistent production database.

## Suggested next steps

1. Accept order items and calculate totals on the server.
2. Connect successful payments to order state changes using synchronous communication or events.
3. Replace H2 with one PostgreSQL database per service.
4. Add an API gateway and Transport Layer Security termination.
5. Add metrics, centralized logs, and distributed traces.
6. Add idempotency keys and database constraints for payment requests.

## License

<<<<<<< HEAD
---

## 🔒 Security Configuration

Each service has identical security setup:

```java
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http.csrf(csrf -> csrf.disable())
        .sessionManagement(session ->
            session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/h2-console/**").permitAll()
            .anyRequest().authenticated());

    http.addFilterBefore(jwtAuthFilter,
        UsernamePasswordAuthenticationFilter.class);

    return http.build();
}
```

**Key Points:**

- CSRF disabled (stateless JWT)
- No sessions (STATELESS policy)
- JWT filter validates every request
- H2 console public for debugging

---

## 🧪 Testing Checklist

- [ ] Register user → `POST /api/auth/signup`
- [ ] Login → `POST /api/auth/signin` → Get JWT
- [ ] Create order with JWT → `POST /api/orders`
- [ ] Verify username saved in order → H2 Console
- [ ] Process payment → `POST /api/payments/process`
- [ ] Verify payment linked to order → H2 Console
- [ ] Test expired token → Should return 401
- [ ] Test invalid token → Should return 401

---

## 🚧 Future Improvements

1. **API Gateway** - Add Spring Cloud Gateway for single entry point
2. **Service Discovery** - Add Eureka for dynamic service registration
3. **Persistent Database** - Replace H2 with PostgreSQL/MySQL
4. **Docker** - Containerize all services
5. **Kubernetes** - Deploy to K8s cluster
6. **Message Queue** - Add RabbitMQ/Kafka for async communication
7. **Rate Limiting** - Protect against abuse
8. **Refresh Tokens** - Implement token refresh mechanism
9. **Role-based Access** - Use `@PreAuthorize` for fine-grained control
10. **Distributed Tracing** - Add Zipkin/Jaeger for request tracing

---

## 📝 License

MIT License - Feel free to use and modify!

---

_Built with ❤️ using Spring Boot and JWT Authentication_

---

/anthropic-skills:docs @
=======
MIT
>>>>>>> 2fdf56d97d1ddb5412d4ae897a67dcec991d02b5
