
# Host DNS name doesn't resolve in Docker alpine images

export DOCKER_HOST_IP=$(hostname -I | sed -e 's/ .*//g')
export TERM=dumb


export MYSQL_PUBLISH_ADDRESS=$DOCKER_HOST_IP
