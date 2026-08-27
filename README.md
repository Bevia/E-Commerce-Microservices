# E-Commerce Microservices Architecture

A complete JWT-authenticated microservices system for e-commerce, built with Spring Boot 3.5 and Java 17.

## Container deployment

The repository now includes one multi-stage Dockerfile per service, a root `compose.yaml`, and Kubernetes manifests under `k8s/`. See [Docker and Kubernetes guide](DOCKER_KUBERNETES.md) for the complete build, run, deployment, and troubleshooting workflow.

## 🏗️ Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              CLIENT APPLICATION                             │
│                        (Web App, Mobile App, etc.)                          │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      │ HTTP + JWT Token
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                                                                             │
│  ┌───────────────┐    ┌───────────────┐    ┌───────────────┐                │
│  │               │    │               │    │               │                │
│  │  AUTH SERVICE │    │ ORDER SERVICE │    │PAYMENT SERVICE│                │
│  │    :8083      │    │    :8080      │    │    :8081      │                │
│  │               │    │               │    │               │                │
│  │  • Signup     │    │  • Create     │    │  • Process    │                │
│  │  • Signin     │    │  • List       │    │  • List       │                │
│  │  • JWT Issue  │    │  • Update     │    │  • Status     │                │
│  │               │    │               │    │               │                │
│  └───────┬───────┘    └───────┬───────┘    └───────┬───────┘                │
│          │                    │                    │                        │
│          ▼                    ▼                    ▼                        │
│  ┌───────────────┐    ┌───────────────┐    ┌───────────────┐                │
│  │   H2: authdb  │    │  H2: orderdb  │    │ H2: paymentdb │                │
│  │               │    │               │    │               │                │
│  │  • Users      │    │  • Orders     │    │  • Payments   │                │
│  │  • Roles      │    │  • OrderItems │    │               │                │
│  └───────────────┘    └───────────────┘    └───────────────┘                │
│                                                                             │
│                    SHARED JWT SECRET (Base64 Encoded)                       │
└─────────────────────────────────────────────────────────────────────────────┘
```

## 🔐 Authentication Flow

```
1. REGISTRATION
   Client ──POST /api/auth/signup──▶ Auth Service ──▶ Store User + Hash Password
                                                            │
                                                            ▼
                                                   {"message": "User registered!"}

2. LOGIN
   Client ──POST /api/auth/signin──▶ Auth Service ──▶ Validate Credentials
                                                            │
                                                            ▼
                                                   Generate JWT Token
                                                            │
                                                            ▼
                                                   {"token": "eyJhbG..."}

3. PROTECTED REQUESTS
   Client ──GET /api/orders──▶ Order Service ──▶ Validate JWT ──▶ Return Data
           + Authorization:                            │
             Bearer <token>                   Extract username
                                              from token subject
```

## 📦 Services

### Auth Service (Port 8083)

**Purpose:** User registration, authentication, and JWT token issuance.

| Endpoint | Method | Description | Auth Required |
|----------|--------|-------------|---------------|
| `/api/auth/signup` | POST | Register new user | No |
| `/api/auth/signin` | POST | Login & get JWT | No |

**Database Tables:**
- `USERS` - User accounts (id, username, email, password_hash)
- `ROLES` - Available roles (ROLE_USER, ROLE_MODERATOR, ROLE_ADMIN)
- `USER_ROLES` - User-role assignments

**Key Features:**
- BCrypt password hashing
- JWT token generation (1-hour expiry)
- Role-based user registration

---

### Order Service (Port 8080)

**Purpose:** Manage customer orders with user tracking.

| Endpoint | Method | Description | Auth Required |
|----------|--------|-------------|---------------|
| `/api/orders` | GET | List all orders | ✅ Yes |
| `/api/orders` | POST | Create new order | ✅ Yes |
| `/api/orders/{id}` | GET | Get order by ID | ✅ Yes |
| `/api/orders/user/{userId}` | GET | Get orders by user | ✅ Yes |
| `/api/orders/{id}/status` | PUT | Update order status | ✅ Yes |
| `/api/orders/{id}` | DELETE | Delete order | ✅ Yes |

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

| Endpoint | Method | Description | Auth Required |
|----------|--------|-------------|---------------|
| `/api/payments` | GET | List all payments | ✅ Yes |
| `/api/payments/process` | POST | Process payment | ✅ Yes |
| `/api/payments/{id}` | GET | Get payment by ID | ✅ Yes |

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
    "sub": "username",     // Subject = username
    "iat": 1764937069,     // Issued at
    "exp": 1764940669      // Expiration (1 hour)
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
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "type": "Bearer",
  "username": "john",
  "roles": ["ROLE_USER"]
}
```

### 4. Create an Order
```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN_HERE" \
  -d '{"totalAmount":149.99}'
```

### 5. Process Payment
```bash
curl -X POST http://localhost:8081/api/payments/process \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN_HERE" \
  -d '{"orderId":"1","amount":149.99,"paymentMethod":"CREDIT_CARD"}'
```

---

## 🗄️ H2 Database Consoles

| Service | Console URL | JDBC URL | User | Password |
|---------|-------------|----------|------|----------|
| Auth | http://localhost:8083/h2-console | `jdbc:h2:mem:authdb` | sa | password |
| Order | http://localhost:8080/h2-console | `jdbc:h2:mem:orderdb` | sa | password |
| Payment | http://localhost:8081/h2-console | `jdbc:h2:mem:paymentdb` | sa | password |

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

| Component | Technology |
|-----------|------------|
| Framework | Spring Boot 3.5.0 |
| Language | Java 17 |
| Security | Spring Security + JWT |
| JWT Library | JJWT 0.12.5 |
| Database | H2 (in-memory) |
| ORM | Spring Data JPA |
| Build Tool | Maven |
| Password Hashing | BCrypt |

---

## 📁 Project Structure

```
AuthMicroservice/
├── src/main/java/org/example/authmicroservice/
│   ├── controller/AuthController.java    # Auth endpoints
│   ├── jwt/JwtUtils.java                 # Token generation
│   ├── model/User.java, Role.java        # JPA entities
│   ├── security/WebSecurityConfig.java   # Security config
│   └── service/UserDetailsServiceImpl.java
└── src/main/resources/application.properties

OrderMicroservice/
├── src/main/java/org/example/ordermicroservice/
│   ├── controller/OrderController.java   # Order endpoints
│   ├── jwt/JwtUtils.java, JwtAuthFilter.java  # Token validation
│   ├── model/Order.java, OrderItem.java  # JPA entities
│   ├── security/WebSecurityConfig.java   # Security config
│   └── service/OrderService.java
└── src/main/resources/application.properties

PaymentMicroservice/
├── src/main/java/org/example/paymentmicroservice/
│   ├── controller/PaymentController.java # Payment endpoints
│   ├── jwt/JwtUtils.java, JwtAuthFilter.java  # Token validation
│   ├── model/Payment.java                # JPA entity
│   ├── security/WebSecurityConfig.java   # Security config
│   └── service/PaymentService.java
└── src/main/resources/application.properties
```

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

*Built with ❤️ using Spring Boot and JWT Authentication*
