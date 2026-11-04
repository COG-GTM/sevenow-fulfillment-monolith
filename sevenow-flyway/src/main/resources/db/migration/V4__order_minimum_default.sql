-- Orders used to persist Integer.MAX_VALUE as order_minimum (the old inverted revise check ignored it).
-- Now that revise enforces the minimum, reset those placeholder values so existing orders stay revisable.
use sevenow;

update orders set order_minimum = 0 where order_minimum is null or order_minimum >= 2147483647;
