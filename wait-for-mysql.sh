#! /bin/sh

until (echo select 1 from dual | ./mysql-cli.sh > /dev/null)
do
 echo sleeping for mysql
 sleep 5
done
