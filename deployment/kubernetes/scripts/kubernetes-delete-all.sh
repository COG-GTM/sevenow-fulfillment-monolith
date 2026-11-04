#! /bin/bash -e

kubectl delete -f <(cat */src/deployment/kubernetes/*.yml deployment/kubernetes/*/*.yml)

kubectl delete secret sevenow-db-secret --ignore-not-found
kubectl delete configmap sevenow-mysql-init --ignore-not-found
