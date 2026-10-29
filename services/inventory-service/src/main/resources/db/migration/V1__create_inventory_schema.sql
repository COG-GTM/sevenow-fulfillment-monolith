-- inventory-service owns stores and their product catalog / on-hand stock.
-- Column shapes match the monolith's sevenow.stores / sevenow.store_products after
-- sevenow-flyway V1 (FTGO restaurants / restaurant_menu_items) + V2__rebrand_7now.sql.

create table stores
(
  id           bigint not null auto_increment,
  store_number varchar(32),
  name         varchar(255),
  street1      varchar(255),
  street2      varchar(255),
  city         varchar(255),
  state        varchar(255),
  zip          varchar(255),
  primary key (id)
) engine = InnoDB;

create table store_products
(
  store_id              bigint  not null,
  id                    varchar(255),
  name                  varchar(255),
  price                 decimal(19, 2),
  on_hand_quantity      integer not null default 0,
  substitute_product_id varchar(255)
) engine = InnoDB;

alter table store_products
  add constraint store_products_store_id foreign key (store_id) references stores (id);

-- Unlike the monolith (which inherited duplicate-tolerant FTGO data), a product id is unique per
-- store here: reservations decrement stock with a conditional update keyed on (store_id, id).
alter table store_products
  add constraint store_products_store_id_id unique (store_id, id);
