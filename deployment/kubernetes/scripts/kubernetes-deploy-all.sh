#! /bin/bash -e

kubectl get secret sevenow-db-secret > /dev/null 2>&1 || ./deployment/kubernetes/misc/create-db-secret.sh

kubectl create configmap sevenow-mysql-init --from-file=init-sevenow.sh=mysql/init-sevenow.sh --dry-run -o yaml | kubectl apply -f -

kubectl apply -f <(cat deployment/kubernetes/stateful-services/*.yml)

./deployment/kubernetes/scripts/kubernetes-wait-for-ready-pods.sh sevenow-mysql-0

kubectl apply -f <(cat */src/deployment/kubernetes/*.yml)
