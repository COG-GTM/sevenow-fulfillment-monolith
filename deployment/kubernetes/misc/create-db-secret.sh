#! /bin/bash -e

# Creates sevenow-db-secret from the environment, generating random passwords for unset values.
# Values are passed to kubectl via mode-600 files so they never appear in process arguments.
random_secret() {
  od -An -N24 -tx1 /dev/urandom | tr -d ' \n'
}

secret_dir=$(mktemp -d)
trap 'rm -rf "$secret_dir"' EXIT
umask 077

printf '%s' "${MYSQL_ROOT_PASSWORD:-$(random_secret)}" > "$secret_dir/root-password"
printf '%s' "${SPRING_DATASOURCE_USERNAME:-sevenow_app}" > "$secret_dir/username"
printf '%s' "${SPRING_DATASOURCE_PASSWORD:-$(random_secret)}" > "$secret_dir/password"
printf '%s' "${FLYWAY_USER:-sevenow_migrator}" > "$secret_dir/migrator-username"
printf '%s' "${FLYWAY_PASSWORD:-$(random_secret)}" > "$secret_dir/migrator-password"

kubectl create secret generic sevenow-db-secret --from-file="$secret_dir"
