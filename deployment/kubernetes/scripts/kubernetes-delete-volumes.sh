#! /bin/bash -e

kubectl delete pvc sevenow-mysql-persistent-storage-sevenow-mysql-0

# MySQL users are created only on an empty volume, so the credentials secret is dropped together with it.
kubectl delete secret sevenow-db-secret --ignore-not-found
