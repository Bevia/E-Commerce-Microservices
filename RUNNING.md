# Running the project

## Two ways to run it: pick one

You can run the services **with Docker** (Compose) or **without Docker** (`./mvnw` in each folder). Don't mix them: both use ports 8083 and 8081, so whichever starts second fails with "Port … already in use".

| | With Docker (Compose) | Without Docker (`./mvnw`) |
|---|---|---|
| Start | `docker compose up -d --build` from the repo root | `./mvnw spring-boot:run` in each service folder, one terminal tab each |
| Stop | `docker compose down` | Ctrl+C in each tab |
| Auth | http://localhost:8083 | http://localhost:8083 |
| Order | http://localhost:**8090** | http://localhost:**8080** |
| Payment | http://localhost:8081 | http://localhost:8081 |
| Java | 17, inside the image | whatever JDK is on your Mac |

Both use the same default JWT secret, so tokens work the same either way. Full Docker and Kubernetes guide: [DOCKER_KUBERNETES.md](DOCKER_KUBERNETES.md).

### With Docker

```bash
cd /Users/vincentbevia/IdeaProjects/E-Commerce-Microservices
docker compose up -d --build
docker compose ps        # all three should become "healthy"
```

In Docker Desktop they appear under **Containers → ecommerce-microservices**.

### Without Docker

Stop the Compose stack first (`docker compose down`), then run each service in its own tab (see below).

## Troubleshooting

**`zsh: command not found: docker`**
Docker Desktop puts the CLI in `~/.docker/bin` and adds it to PATH in `~/.zprofile`. That file is only read by *login* shells, and IDE terminals usually aren't, so `docker` is missing there. Add it to `~/.zshrc` once and open a new terminal:

```bash
echo 'export PATH="$PATH:$HOME/.docker/bin"' >> ~/.zshrc
```

**"Port 8083 (or 8081/8080) was already in use"**
Something else holds the port, usually the other way of running (a Docker container, or a leftover `./mvnw` process). See who owns it:

```bash
lsof -nP -iTCP:8083 -sTCP:LISTEN
```

- `com.docker…` → the Compose stack has it: `docker compose down` (or `docker compose stop auth-service`).
- `java` → a local `./mvnw` run: Ctrl+C in its tab, or `kill $(lsof -t -iTCP:8083 -sTCP:LISTEN)`.

Always keep `-sTCP:LISTEN` in that kill. A bare `kill $(lsof -t -i:8083)` also matches Docker Desktop's backend when Docker publishes the port, and kills Docker Desktop with every container in it.

**Containers don't show as running in Docker Desktop**
Check `docker compose ps -a`. `Exited (143)` means they were stopped cleanly (Stop button, `docker compose stop`, or quitting Docker). Containers stopped on purpose stay stopped after Docker restarts, even with `restart: unless-stopped`. Start them again with `docker compose up -d`. Also check that the Containers view isn't filtered to "Only show running containers".

**Orders fail at the payment step in Docker but work locally**
The order service calls payment at `payment.service.base-url`, which defaults to `http://localhost:8081`. Inside a container, `localhost` is the order container itself. If this happens, point it at the payment container in `compose.yaml` under `order-service`:

```yaml
    environment:
      PAYMENT_SERVICE_BASEURL: http://payment-service:8081
```

---

## Without Docker: step by step

Please run these in 3 separate terminal tabs:

Tab 1 - Auth Service:
cd /Users/vincentbevia/IdeaProjects/E-Commerce-Microservices/auth-service && ./mvnw spring-boot:run

Tab 2 - Order Service:
cd /Users/vincentbevia/IdeaProjects/E-Commerce-Microservices/order-service && ./mvnw spring-boot:run

Tab 3 - Test (wait until both show "Started" in their tabs):

curl -X POST http://localhost:8083/api/auth/signup -H "Content-Type: application/json" -d '{"username":"Vicent","email":"u2@test.com","password":"pass123","role":["user"]}'

then to get the token from Vicent:
curl -X POST http://localhost:8083/api/auth/signin -H "Content-Type: application/json" -d '{"username":"Vicent","password":"pass123"}'

then:
curl http://localhost:8080/api/orders -H "Authorization: Bearer <PASTE_YOUR_TOKEN>"


**Get all payments:**
```bash
curl http://localhost:8081/api/payments -H "Authorization: Bearer <YOUR_TOKEN>"
```

**Create a payment:**
```bash
curl -X POST http://localhost:8081/api/payments/process \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <YOUR_TOKEN>" \
  -d '{"orderId":"ORD-001","amount":99.99,"paymentMethod":"CREDIT_CARD","status":"COMPLETED"}'
```

**H2 Console for Payment DB:**
- URL: http://localhost:8081/h2-console
- JDBC URL: `jdbc:h2:mem:paymentdb`
- Username: `sa`
- Password: `password`

---

### Summary of all 3 services:

| Service | Port | Endpoints | H2 Console |
|---------|------|-----------|------------|
| Auth | 8083 | `/api/auth/signup`, `/api/auth/signin` | `jdbc:h2:mem:authdb` |
| Order | 8080 | `/api/orders` | `jdbc:h2:mem:orderdb` |
| Payment | 8081 | `/api/payments`, `/api/payments/process` | `jdbc:h2:mem:paymentdb` |



-- View all orders
SELECT * FROM orders;

-- View order items (if exists)
SELECT * FROM order_item;



You're connected to the **Auth database** (`authdb`), not the Order database!

For the **Order service H2 Console**, you need to:

1. Go to http://localhost:8080/h2-console (port **8080**, not 8083)
2. Change JDBC URL to: `jdbc:h2:mem:orderdb`
3. Username: `sa`
4. Password: `password`
5. Click Connect

Then run:
```sql
SHOW TABLES;
SELECT * FROM ORDERS;
```

---

**Quick reference:**

| Service | H2 Console URL | JDBC URL |
|---------|----------------|----------|
| Auth | http://localhost:8083/h2-console | `jdbc:h2:mem:authdb` |
| Order | http://localhost:8080/h2-console | `jdbc:h2:mem:orderdb` |
| Payment | http://localhost:8081/h2-console | `jdbc:h2:mem:paymentdb` |


## To create an order, start services:

cd /Users/vincentbevia/IdeaProjects/E-Commerce-Microservices/auth-service && ./mvnw spring-boot:run

cd /Users/vincentbevia/IdeaProjects/E-Commerce-Microservices/order-service && ./mvnw spring-boot:run

# 1. Register user
curl -X POST http://localhost:8083/api/auth/signup -H "Content-Type: application/json" -d '{"username":"user1","email":"u1@test.com","password":"pass123","role":["user"]}'

# 2. Login and get token
curl -X POST http://localhost:8083/api/auth/signin -H "Content-Type: application/json" -d '{"username":"user1","password":"pass123"}'

that will give you the TOKEN

# 3. Copy the token from step 2, then create an order:
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJWaWNlbnQiLCJpYXQiOjE3NjQ5MzcwNjksImV4cCI6MTc2NDk0MDY2OX0.sRDbNdRueL7PXEbbq93dq6eteWSewDscmLHAOzQ_-vk" \
  -d '{"userId":1,"customerName":"Vicens","totalAmount":99.99}'

  curl -X POST http://localhost:8080/api/orders -H "Content-Type: application/json" -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ1c2VyMSIsImlhdCI6MTc2NDkzNjI1NiwiZXhwIjoxNzY0OTM5ODU2fQ.8j2AIAtDK-U6UYyrIhQA1R7oBJlXyNZeuCHgmyktMWY" -d '{"customerName":"Josh","totalAmount":15.00}'

then:

Then go to http://localhost:8080/h2-console and use:

JDBC URL: jdbc:h2:mem:orderdb
User: sa
Password: password
Run SELECT * FROM ORDERS; to see your order.

## if you need to kill the service:
kill $(lsof -t -iTCP:8080 -sTCP:LISTEN) && cd /Users/vincentbevia/IdeaProjects/E-Commerce-Microservices/order-service && ./mvnw spring-boot:run



SELECT ID, USERNAME, TOTAL_AMOUNT, STATUS FROM ORDERS;

SELECT * FROM ORDERS WHERE USERNAME = 'Vicent';


## Payment Service:

Start the Payment Service in a new terminal:

cd /Users/vincentbevia/IdeaProjects/E-Commerce-Microservices/payment-service && ./mvnw spring-boot:run

curl http://localhost:8081/api/payments -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJWaWNlbnQiLCJpYXQiOjE3NjQ5MzcwNjksImV4cCI6MTc2NDk0MDY2OX0.sRDbNdRueL7PXEbbq93dq6eteWSewDscmLHAOzQ_-vk"

curl -X POST http://localhost:8081/api/payments/process \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJWaWNlbnQiLCJpYXQiOjE3NjQ5MzcwNjksImV4cCI6MTc2NDk0MDY2OX0.sRDbNdRueL7PXEbbq93dq6eteWSewDscmLHAOzQ_-vk" \
  -d '{"orderId":"3","amount":99.99,"paymentMethod":"CREDIT_CARD"}'

Check H2 Console:

URL: http://localhost:8081/h2-console
JDBC URL: jdbc:h2:mem:paymentdb
User: sa
Password: password

SELECT * FROM PAYMENT;


Flow verified:

✅ User registers → Auth service
✅ User logs in → Gets JWT token
✅ User creates order → Order service validates JWT, saves order with username
✅ User makes payment → Payment service validates JWT, processes payment

All services share the same JWT secret and validate tokens independently - true stateless microservices authentication! 

