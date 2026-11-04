#! /bin/bash -e

TTY_FLAG=
[ -t 0 ] || TTY_FLAG=-T

docker compose exec $TTY_FLAG mysql \
   sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" exec mysql -uroot sevenow'
