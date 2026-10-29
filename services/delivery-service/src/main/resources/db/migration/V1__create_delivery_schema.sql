-- delivery-service owns drivers and their plans. Column shapes match the monolith's
-- sevenow.drivers / sevenow.driver_actions after sevenow-flyway V1 (FTGO courier / courier_actions)
-- + V2__rebrand_7now.sql, except that driver_actions.order_id is a plain bigint: orders live in the
-- monolith's schema, so there is no foreign key to them (ARCHITECTURE.md seam #2).

create table drivers
(
  id         bigint not null auto_increment,
  available  bit,
  first_name varchar(255),
  last_name  varchar(255),
  street1    varchar(255),
  street2    varchar(255),
  city       varchar(255),
  state      varchar(255),
  zip        varchar(255),
  primary key (id)
) engine = InnoDB;

create table driver_actions
(
  driver_id bigint not null,
  order_id  bigint,
  time      datetime,
  type      varchar(255)
) engine = InnoDB;

alter table driver_actions
  add constraint driver_actions_driver_id foreign key (driver_id) references drivers (id);

create index driver_actions_order_id on driver_actions (order_id);
