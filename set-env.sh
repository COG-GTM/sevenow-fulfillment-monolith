if [ -z "$DOCKER_HOST_IP" ] ; then
    if [ -z "$DOCKER_HOST" ] ; then
      export DOCKER_HOST_IP=`hostname`
    else
      echo using ${DOCKER_HOST?}
      XX=${DOCKER_HOST%\:*}
      export DOCKER_HOST_IP=${XX#tcp\:\/\/}
    fi
fi

echo DOCKER_HOST_IP is $DOCKER_HOST_IP
export COMPOSE_HTTP_TIMEOUT=240

# MySQL is published on the Docker host's loopback only. With a remote daemon, opt in explicitly to a reachable
# address (or tunnel 3306) rather than exposing the database on the network by default.
if [ -z "$MYSQL_PUBLISH_ADDRESS" ] && [ -n "$DOCKER_HOST" ] ; then
  echo "WARNING: DOCKER_HOST is remote but MYSQL_PUBLISH_ADDRESS is unset; MySQL is only reachable on that host's loopback." >&2
  echo "         Export MYSQL_PUBLISH_ADDRESS=<daemon IP> or tunnel port 3306 and set DOCKER_HOST_IP=localhost." >&2
fi

# MySQL credentials are never committed: they come from the environment or from a
# git-ignored .env file that is generated with random passwords on first use.
sevenow_random_secret() {
  od -An -N24 -tx1 /dev/urandom | tr -d ' \n'
}

if [ ! -f .env ] ; then
  (
    umask 077
    {
      echo "MYSQL_ROOT_PASSWORD=${MYSQL_ROOT_PASSWORD:-$(sevenow_random_secret)}"
      echo "SPRING_DATASOURCE_USERNAME=${SPRING_DATASOURCE_USERNAME:-sevenow_app}"
      echo "SPRING_DATASOURCE_PASSWORD=${SPRING_DATASOURCE_PASSWORD:-$(sevenow_random_secret)}"
      echo "FLYWAY_USER=${FLYWAY_USER:-sevenow_migrator}"
      echo "FLYWAY_PASSWORD=${FLYWAY_PASSWORD:-$(sevenow_random_secret)}"
    } > .env
  )
  echo "Generated MySQL credentials in .env"
fi

while IFS='=' read -r sevenow_name sevenow_value ; do
  case "$sevenow_name" in
    ''|\#*) continue ;;
  esac
  if [ -z "$(printenv "$sevenow_name")" ] ; then
    export "$sevenow_name=$sevenow_value"
  fi
done < .env
