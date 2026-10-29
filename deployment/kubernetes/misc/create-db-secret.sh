kubectl create secret generic sevenow-db-secret \
  --from-literal=username=mysqluser \
  --from-literal=password=mysqlpw