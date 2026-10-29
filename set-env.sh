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
if [ -z "$SEVENOW_AUTH_JWT_SECRET" ] ; then
    echo SEVENOW_AUTH_JWT_SECRET not set: generating a random local signing secret
    export SEVENOW_AUTH_JWT_SECRET=$(openssl rand -hex 32)
fi
