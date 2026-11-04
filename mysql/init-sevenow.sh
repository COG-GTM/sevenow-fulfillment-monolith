# Sourced by the mysql image entrypoint on first start (empty data dir).
: "${SEVENOW_APP_DB_USER:?SEVENOW_APP_DB_USER must be set}"
: "${SEVENOW_APP_DB_PASSWORD:?SEVENOW_APP_DB_PASSWORD must be set}"
: "${SEVENOW_MIGRATOR_DB_USER:?SEVENOW_MIGRATOR_DB_USER must be set}"
: "${SEVENOW_MIGRATOR_DB_PASSWORD:?SEVENOW_MIGRATOR_DB_PASSWORD must be set}"
SEVENOW_DB_ALLOWED_HOST="${SEVENOW_DB_ALLOWED_HOST:-%}"

sevenow_sql_quote() {
  printf "'%s'" "$(printf '%s' "$1" | sed -e "s/\\\\/\\\\\\\\/g" -e "s/'/''/g")"
}

sevenow_host=$(sevenow_sql_quote "$SEVENOW_DB_ALLOWED_HOST")
sevenow_app=$(sevenow_sql_quote "$SEVENOW_APP_DB_USER")@$sevenow_host
sevenow_migrator=$(sevenow_sql_quote "$SEVENOW_MIGRATOR_DB_USER")@$sevenow_host

docker_process_sql --database=mysql <<-EOSQL
	CREATE DATABASE IF NOT EXISTS sevenow;
	CREATE USER $sevenow_app IDENTIFIED BY $(sevenow_sql_quote "$SEVENOW_APP_DB_PASSWORD") REQUIRE SSL;
	GRANT SELECT, INSERT, UPDATE, DELETE ON sevenow.* TO $sevenow_app;
	CREATE USER $sevenow_migrator IDENTIFIED BY $(sevenow_sql_quote "$SEVENOW_MIGRATOR_DB_PASSWORD") REQUIRE SSL;
	GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, DROP, INDEX, REFERENCES ON sevenow.* TO $sevenow_migrator;
	FLUSH PRIVILEGES;
EOSQL
