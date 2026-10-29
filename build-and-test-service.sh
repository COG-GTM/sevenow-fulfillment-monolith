#! /bin/bash -e

# Builds, migrates and tests one extracted service against its own schema, independently of the monolith.
# Usage: ./build-and-test-service.sh inventory-service|delivery-service

SERVICE=${1?usage: $0 inventory-service|delivery-service}
shift

if [ -z "$DOCKER_COMPOSE" ] ; then
    if command -v docker-compose > /dev/null 2>&1 ; then
        DOCKER_COMPOSE=docker-compose
    else
        DOCKER_COMPOSE="docker compose"
    fi
fi

. ./set-env.sh

${DOCKER_COMPOSE?} up -d --build mysql

./gradlew waitForMySql

./gradlew :${SERVICE}:flywayMigrate :${SERVICE}:build :${SERVICE}:integrationTest $*

${DOCKER_COMPOSE?} build ${SERVICE}
