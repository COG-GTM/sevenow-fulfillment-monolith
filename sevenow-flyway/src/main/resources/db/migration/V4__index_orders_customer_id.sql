-- GET /orders?customerId= (OrderRepository.findAllByCustomerId) filters on customer_id, which had
-- no index, so every call scanned the whole orders table.
use sevenow;

create index orders_customer_id on orders (customer_id);
