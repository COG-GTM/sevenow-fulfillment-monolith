-- Same store #38114 / 18-product catalog as the monolith's V3__seed_7now_data.sql
-- (stores + store_products only; drivers, customers and orders are not owned here).

insert into stores (id, store_number, name, street1, street2, city, state, zip) values
  (1, '38114', '7-Eleven #38114', '3200 Hackberry Rd', null, 'Irving', 'TX', '75063');

insert into store_products (store_id, id, name, price, on_hand_quantity, substitute_product_id) values
  -- Cold drinks
  (1, 'SLURPEE-CHERRY-M',      'Cherry Slurpee (Medium)',           2.19, 40, null),
  (1, 'SLURPEE-COKE-M',        'Coca-Cola Slurpee (Medium)',        2.19, 35, null),
  (1, 'BIG-GULP-COKE',         'Big Gulp Coca-Cola (30 oz)',        1.99, 50, null),
  (1, 'RED-BULL-12OZ',         'Red Bull Energy Drink (12 oz)',     3.49,  0, 'MONSTER-16OZ'),
  (1, 'MONSTER-16OZ',          'Monster Energy (16 oz)',            2.99, 24, null),
  (1, 'WATER-7SELECT-1L',      '7-Select Purified Water (1 L)',     1.49, 60, null),
  -- Snacks
  (1, 'TAKIS-FUEGO-9OZ',       'Takis Fuego (9.9 oz)',              4.79, 18, null),
  (1, 'DORITOS-NACHO-9OZ',     'Doritos Nacho Cheese (9.25 oz)',    4.99,  0, 'DORITOS-COOLRANCH-9OZ'),
  (1, 'DORITOS-COOLRANCH-9OZ', 'Doritos Cool Ranch (9.25 oz)',      4.99, 12, null),
  (1, 'TROLLI-SOUR-BRITE',     'Trolli Sour Brite Crawlers (5 oz)', 2.49, 30, null),
  (1, 'SNICKERS-KING',         'Snickers King Size',                2.29, 45, null),
  -- Pizza
  (1, 'PIZZA-PEPPERONI-WHOLE', 'Pepperoni Pizza (Whole)',           7.99,  0, 'PIZZA-CHEESE-WHOLE'),
  (1, 'PIZZA-CHEESE-WHOLE',    'Cheese Pizza (Whole)',              6.99,  6, null),
  (1, 'PIZZA-PEPPERONI-SLICE', 'Pepperoni Pizza Slice',             2.49, 16, null),
  -- Hot food
  (1, 'BIG-BITE-HOTDOG',       'Big Bite Hot Dog (1/4 lb)',         2.59, 20, null),
  (1, 'TAQUITO-BUFFALO',       'Buffalo Chicken Taquito',           1.99, 14, null),
  (1, 'WINGS-8PC',             'Chicken Wings (8 pc)',              6.49,  0, null),
  (1, 'CHICKEN-TENDERS-4PC',   'Chicken Tenders (4 pc)',            5.49,  9, null);
