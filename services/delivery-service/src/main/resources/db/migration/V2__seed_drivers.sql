-- Drivers 1 and 2 from the monolith's V3__seed_7now_data.sql (driver 1 available, driver 2 not).

insert into drivers (id, available, first_name, last_name, street1, street2, city, state, zip) values
  (1, 1, 'Dana',  'Driver',  '5601 N MacArthur Blvd', null, 'Irving', 'TX', '75038'),
  (2, 0, 'Marco', 'Mendoza', '7701 Las Colinas Ridge', null, 'Irving', 'TX', '75063');
