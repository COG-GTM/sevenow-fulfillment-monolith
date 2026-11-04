#! /bin/bash -e

# Creates sevenow-db-secret from the environment, generating random passwords for unset values.
random_secret() {
  od -An -N24 -tx1 /dev/urandom | tr -d ' \n'
}

kubectl create secret generic sevenow-db-secret \
  --from-literal=root-password="${MYSQL_ROOT_PASSWORD:-$(random_secret)}" \
  --from-literal=username="${SPRING_DATASOURCE_USERNAME:-sevenow_app}" \
  --from-literal=password="${SPRING_DATASOURCE_PASSWORD:-$(random_secret)}" \
  --from-literal=migrator-username="${FLYWAY_USER:-sevenow_migrator}" \
  --from-literal=migrator-password="${FLYWAY_PASSWORD:-$(random_secret)}"
