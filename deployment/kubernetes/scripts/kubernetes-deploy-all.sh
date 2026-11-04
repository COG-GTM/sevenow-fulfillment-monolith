#! /bin/bash -e

kubectl get secret sevenow-db-secret > /dev/null 2>&1 || ./deployment/kubernetes/misc/create-db-secret.sh

kubectl apply -f <(cat deployment/kubernetes/stateful-services/*.yml)

./deployment/kubernetes/scripts/kubernetes-wait-for-ready-pods.sh sevenow-mysql-0

kubectl apply -f <(cat */src/deployment/kubernetes/*.yml)
