-- 7NOW demo seed data: one store with a 7-Eleven style catalog, two drivers and one customer.
-- Uses fixed ids so the README / e2e docs can refer to store 1, drivers 1-2 and customer 1.
-- "insert ignore" keeps the migration safe on a database that already holds rows with those
-- ids (e.g. a migrated FTGO instance): existing rows are left untouched and the seed is skipped.
-- The demo catalog is only loaded when store 1 really is the seeded 7-Eleven #38114, and only for
-- product ids that store does not already have (store_products has no primary key).
use sevenow;

insert ignore into stores (id, store_number, name, street1, street2, city, state, zip) values
  (1, '38114', '7-Eleven #38114', '3200 Hackberry Rd', null, 'Irving', 'TX', '75063');

-- Cold drinks
insert into store_products (store_id, id, name, price, on_hand_quantity, substitute_product_id)
select seed.* from (
  select 1 as store_id, 'SLURPEE-CHERRY-M' as id, 'Cherry Slurpee (Medium)' as name, 2.19 as price, 40 as on_hand_quantity, null as substitute_product_id
  union all
  select 1, 'SLURPEE-COKE-M',         'Coca-Cola Slurpee (Medium)',           2.19, 35, null
  union all
  select 1, 'BIG-GULP-COKE',          'Big Gulp Coca-Cola (30 oz)',           1.99, 50, null
  union all
  select 1, 'RED-BULL-12OZ',          'Red Bull Energy Drink (12 oz)',        3.49,  0, 'MONSTER-16OZ'
  union all
  select 1, 'MONSTER-16OZ',           'Monster Energy (16 oz)',               2.99, 24, null
  union all
  select 1, 'WATER-7SELECT-1L',       '7-Select Purified Water (1 L)',        1.49, 60, null
) seed
 where exists (select 1 from stores where id = 1 and store_number = '38114')
   and not exists (select 1 from store_products sp where sp.store_id = seed.store_id and sp.id = seed.id);

-- Snacks
insert into store_products (store_id, id, name, price, on_hand_quantity, substitute_product_id)
select seed.* from (
  select 1 as store_id, 'TAKIS-FUEGO-9OZ' as id, 'Takis Fuego (9.9 oz)' as name, 4.79 as price, 18 as on_hand_quantity, null as substitute_product_id
  union all
  select 1, 'DORITOS-NACHO-9OZ',      'Doritos Nacho Cheese (9.25 oz)',       4.99,  0, 'DORITOS-COOLRANCH-9OZ'
  union all
  select 1, 'DORITOS-COOLRANCH-9OZ',  'Doritos Cool Ranch (9.25 oz)',         4.99, 12, null
  union all
  select 1, 'TROLLI-SOUR-BRITE',      'Trolli Sour Brite Crawlers (5 oz)',    2.49, 30, null
  union all
  select 1, 'SNICKERS-KING',          'Snickers King Size',                   2.29, 45, null
) seed
 where exists (select 1 from stores where id = 1 and store_number = '38114')
   and not exists (select 1 from store_products sp where sp.store_id = seed.store_id and sp.id = seed.id);

-- Pizza
insert into store_products (store_id, id, name, price, on_hand_quantity, substitute_product_id)
select seed.* from (
  select 1 as store_id, 'PIZZA-PEPPERONI-WHOLE' as id, 'Pepperoni Pizza (Whole)' as name, 7.99 as price, 0 as on_hand_quantity, 'PIZZA-CHEESE-WHOLE' as substitute_product_id
  union all
  select 1, 'PIZZA-CHEESE-WHOLE',     'Cheese Pizza (Whole)',                 6.99,  6, null
  union all
  select 1, 'PIZZA-PEPPERONI-SLICE',  'Pepperoni Pizza Slice',                2.49, 16, null
) seed
 where exists (select 1 from stores where id = 1 and store_number = '38114')
   and not exists (select 1 from store_products sp where sp.store_id = seed.store_id and sp.id = seed.id);

-- Hot food
insert into store_products (store_id, id, name, price, on_hand_quantity, substitute_product_id)
select seed.* from (
  select 1 as store_id, 'BIG-BITE-HOTDOG' as id, 'Big Bite Hot Dog (1/4 lb)' as name, 2.59 as price, 20 as on_hand_quantity, null as substitute_product_id
  union all
  select 1, 'TAQUITO-BUFFALO',        'Buffalo Chicken Taquito',              1.99, 14, null
  union all
  select 1, 'WINGS-8PC',              'Chicken Wings (8 pc)',                 6.49,  0, null
  union all
  select 1, 'CHICKEN-TENDERS-4PC',    'Chicken Tenders (4 pc)',               5.49,  9, null
) seed
 where exists (select 1 from stores where id = 1 and store_number = '38114')
   and not exists (select 1 from store_products sp where sp.store_id = seed.store_id and sp.id = seed.id);

insert ignore into drivers (id, available, first_name, last_name, street1, street2, city, state, zip) values
  (1, 1, 'Dana',  'Driver',  '5601 N MacArthur Blvd', null, 'Irving', 'TX', '75038'),
  (2, 0, 'Marco', 'Mendoza', '7701 Las Colinas Ridge', null, 'Irving', 'TX', '75063');

insert ignore into customers (id, first_name, last_name) values
  (1, 'Jordan', 'Nguyen');

-- customers use hibernate_sequence; keep generated ids clear of the seeded row and of any
-- existing customers, without ever moving the sequence backwards.
update hibernate_sequence
   set next_val = greatest(next_val, 1000, (select coalesce(max(id), 0) + 1 from customers));
