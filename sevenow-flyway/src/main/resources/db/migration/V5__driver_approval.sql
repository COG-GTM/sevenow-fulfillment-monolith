-- Drivers must be approved by a dispatcher before OrderService.scheduleDelivery may assign them orders.
-- Only the vetted V3 seed drivers are approved here; any other existing driver row (e.g. one registered through the
-- previously unauthenticated POST /drivers) stays unapproved until a dispatcher calls POST /drivers/{driverId}/approve.
alter table drivers add column approved bit not null default 0;

update drivers set approved = 1
 where (id = 1 and first_name = 'Dana' and last_name = 'Driver')
    or (id = 2 and first_name = 'Marco' and last_name = 'Mendoza');
