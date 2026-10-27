# Database access guide

Each of the three Spring Boot services owns its own **H2 in-memory database**. There is no shared database — the H2 web console is how you inspect each service's data directly.

Because the data lives in memory, it only exists while that service's process is running, and it is wiped whenever the container restarts or is rebuilt (`docker compose up -d --build`).

## 1. Prerequisites

- Docker Compose is running (`docker compose ps` shows all three services healthy).
- `spring.h2.console.settings.web-allow-others=true` is set in each service's `application.properties`. Without it, the H2 console rejects connections made through Docker's port mapping with `Sorry, remote connections ('webAllowOthers') are disabled on this server.`, because the request doesn't appear to originate from `127.0.0.1` once it passes through Docker's NAT. This is a local-dev-only setting — don't enable it in a production configuration.

## 2. Console URLs and credentials

| Service | H2 Console URL | JDBC URL | User | Password |
|---|---|---|---|---|
| Authentication | http://localhost:8083/h2-console | `jdbc:h2:mem:authdb` | `sa` | `password` |
| Order | http://localhost:8090/h2-console | `jdbc:h2:mem:orderdb` | `sa` | `password` |
| Payment | http://localhost:8081/h2-console | `jdbc:h2:mem:paymentdb` | `sa` | `password` |

On the H2 login screen, replace the default `jdbc:h2:mem:testdb` with the JDBC URL for that service exactly as shown above, then click **Connect**. A mismatched URL silently connects to a new, empty database instead of erroring.

## 3. Schema reference

### Authentication service (`authdb`)

| Table | Notable columns |
|---|---|
| `users` | `id`, `username` (unique), `email` (unique), `password` (BCrypt hash) |
| `roles` | `id`, `name` (`ROLE_USER`, `ROLE_ADMIN`, ...) |
| `user_roles` | join table: `user_id`, `role_id` |

### Order service (`orderdb`)

| Table | Notable columns |
|---|---|
| `orders` | `id`, `user_id`, `username`, `order_date`, `status` (`PENDING`, `PAID`, `SHIPPED`, `CANCELLED`), `total_amount`, `payment_id` |
| `order_item` | `id`, `order_id` (FK), `product_id`, `product_name`, `quantity`, `price_at_time_of_purchase` |

Note: `POST /api/orders` only accepts `userId`, `customerName`, `totalAmount` — `customerName` isn't persisted, and order items aren't created through that endpoint today, so `order_item` stays empty unless rows are inserted another way.

### Payment service (`paymentdb`)

| Table | Notable columns |
|---|---|
| `payment` | `id`, `order_id` (string reference to an Order service order, not a FK), `amount`, `currency`, `payment_method`, `status` (`PENDING`, `COMPLETED`, `FAILED`), `payment_date`, `transaction_id` |

## 4. Example queries

```sql
-- Auth: list users with their roles
SELECT u.id, u.username, u.email, r.name
FROM users u
JOIN user_roles ur ON ur.user_id = u.id
JOIN roles r ON r.id = ur.role_id;

-- Order: list orders with item counts
SELECT o.id, o.user_id, o.status, o.total_amount, COUNT(oi.id) AS item_count
FROM orders o
LEFT JOIN order_item oi ON oi.order_id = o.id
GROUP BY o.id, o.user_id, o.status, o.total_amount;

-- Payment: latest payment per order
SELECT order_id, id, status, amount, payment_date
FROM payment
ORDER BY payment_date DESC;
```

## 5. Troubleshooting

### "Sorry, remote connections ('webAllowOthers') are disabled on this server."

`spring.h2.console.settings.web-allow-others=true` is missing or the image hasn't been rebuilt since it was added. Run `docker compose up -d --build` to pick up the config change (this wipes existing data, per the note above).

### Table not found / database is empty

You likely connected with the default `jdbc:h2:mem:testdb` instead of the service-specific URL (`authdb`, `orderdb`, or `paymentdb`). H2 silently creates a new empty in-memory database for any URL that doesn't already exist.

### Data disappeared

Expected — H2 is in-memory. A container restart or rebuild resets that service's data. Re-run the [POSTMAN.md](POSTMAN.md) flow (signup → sign in → create order → process payment) to repopulate it.
