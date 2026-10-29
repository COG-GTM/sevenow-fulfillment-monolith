-- One row per scheduled order: the primary key makes POST /deliveries idempotent under concurrent retries
-- (a second insert for the same order_id blocks until the first transaction ends, then is ignored).
create table delivery_assignments (
  order_id  bigint not null primary key,
  driver_id bigint,
  constraint delivery_assignments_driver_id foreign key (driver_id) references drivers (id)
) engine = InnoDB;
