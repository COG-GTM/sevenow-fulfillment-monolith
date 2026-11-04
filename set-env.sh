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

# MySQL is published on loopback by default; a remote Docker daemon must publish it on its own address instead.
if [ -z "$MYSQL_PUBLISH_ADDRESS" ] && [ -n "$DOCKER_HOST" ] ; then
  export MYSQL_PUBLISH_ADDRESS=$(getent ahostsv4 "$DOCKER_HOST_IP" | awk 'NR==1 {print $1}')
  echo MYSQL_PUBLISH_ADDRESS is $MYSQL_PUBLISH_ADDRESS
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
