-- 7NOW rebrand: Restaurant -> Store, MenuItem -> Product, Courier -> Driver, Consumer -> Customer.
-- Same single schema; tables and FK columns are renamed in place and the new
-- store-number / inventory / substitution / picking-packed columns are added.
use sevenow;

-- drop FKs that reference renamed tables/columns
alter table courier_actions drop foreign key courier_actions_order_id;
alter table courier_actions drop foreign key courier_actions_courier_id;
alter table orders drop foreign key orders_assigned_courier_id;
alter table orders drop foreign key orders_restaurant_id;
alter table restaurant_menu_items drop foreign key restaurant_menu_items_restaurant_id;

-- Consumer -> Customer
rename table consumers to customers;

-- Courier -> Driver
rename table courier to drivers;
rename table courier_actions to driver_actions;
alter table driver_actions change courier_id driver_id bigint not null;

-- Restaurant -> Store (+ store_number)
rename table restaurants to stores;
alter table stores add column store_number varchar(32) after id;

-- MenuItem -> Product (+ on_hand_quantity, substitute_product_id)
rename table restaurant_menu_items to store_products;
alter table store_products change restaurant_id store_id bigint not null;
-- Rows migrated from the FTGO schema carry no inventory, so they default to 0 = unavailable
-- until stock is loaded (the order-time substitution rule treats 0 as out of stock).
-- This is deliberate: inventing a positive quantity would let orders be placed against
-- products the store has never counted.
alter table store_products add column on_hand_quantity integer not null default 0;
alter table store_products add column substitute_product_id varchar(255);
alter table store_products add unique key store_products_store_id_product_id (store_id, id);

-- orders: renamed FK columns, PICKING/PACKED timestamps replace preparing_time
alter table orders change consumer_id customer_id bigint;
alter table orders change restaurant_id store_id bigint;
alter table orders change assigned_courier_id assigned_driver_id bigint;
alter table orders add column picking_time datetime after accept_time;
alter table orders add column packed_time datetime after picking_time;
-- PREPARING was the restaurant-era state between ACCEPTED and READY_FOR_PICKUP; it maps onto
-- PICKING (the first of the two new states) and its timestamp is carried over.
update orders set picking_time = preparing_time where preparing_time is not null;
update orders set order_state = 'PICKING' where order_state = 'PREPARING';
alter table orders drop column preparing_time;

-- order_line_items: product id + substitution provenance
alter table order_line_items change menu_item_id product_id varchar(255);
alter table order_line_items add column substituted_from_product_id varchar(255);

-- re-create FKs
alter table driver_actions
  add constraint driver_actions_order_id foreign key (order_id) references orders (id);
alter table driver_actions
  add constraint driver_actions_driver_id foreign key (driver_id) references drivers (id);
alter table orders
  add constraint orders_assigned_driver_id foreign key (assigned_driver_id) references drivers (id);
alter table orders
  add constraint orders_store_id foreign key (store_id) references stores (id);
alter table store_products
  add constraint store_products_store_id foreign key (store_id) references stores (id);
