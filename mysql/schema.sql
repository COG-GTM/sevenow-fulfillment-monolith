create database sevenow;
GRANT ALL PRIVILEGES ON sevenow.* TO 'mysqluser'@'%' WITH GRANT OPTION;

-- Schemas owned by the extracted services (services/inventory-service, services/delivery-service).
create database sevenow_inventory;
GRANT ALL PRIVILEGES ON sevenow_inventory.* TO 'mysqluser'@'%' WITH GRANT OPTION;

create database sevenow_delivery;
GRANT ALL PRIVILEGES ON sevenow_delivery.* TO 'mysqluser'@'%' WITH GRANT OPTION;
