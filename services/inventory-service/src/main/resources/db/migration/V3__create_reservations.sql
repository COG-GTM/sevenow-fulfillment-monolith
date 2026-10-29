-- Stock reservations: POST /reservations decrements store_products.on_hand_quantity and records
-- the resolved (possibly substituted) line items; POST /reservations/{id}/release puts them back.

create table reservations
(
  id            bigint      not null auto_increment,
  version       bigint,
  store_id      bigint      not null,
  status        varchar(32) not null,
  created_time  datetime,
  released_time datetime,
  primary key (id)
) engine = InnoDB;

create table reservation_items
(
  reservation_id              bigint  not null,
  product_id                  varchar(255),
  name                        varchar(255),
  price                       decimal(19, 2),
  quantity                    integer not null,
  substituted_from_product_id varchar(255)
) engine = InnoDB;

alter table reservations
  add constraint reservations_store_id foreign key (store_id) references stores (id);

alter table reservation_items
  add constraint reservation_items_reservation_id foreign key (reservation_id) references reservations (id);
