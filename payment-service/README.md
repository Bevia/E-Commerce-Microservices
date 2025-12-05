RUN:

./mvnw spring-boot:run

CREATE ORDER:

curl -X POST http://localhost:8081/api/payments/process \
  -H "Content-Type: application/json" \
  -d '{"orderId": "ORD-001", "amount": 99.99, "currency": "USD", "paymentMethod": "Credit Card"}'

RETRIVE ALL ORDERS:

  curl http://localhost:8081/api/payments