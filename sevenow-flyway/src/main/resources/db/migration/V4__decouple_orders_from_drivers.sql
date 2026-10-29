-- delivery-service (services/delivery-service, schema sevenow_delivery) now owns drivers.
-- orders.assigned_driver_id stays as a plain column holding a delivery-service driver id, so the
-- FK to the monolith's local drivers table is dropped (Order no longer has a @ManyToOne Driver).
alter table orders drop foreign key orders_assigned_driver_id;
