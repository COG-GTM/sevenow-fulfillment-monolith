#! /bin/bash

set -e

./deployment/kubernetes/scripts/kubernetes-wait-for-ready-pods.sh $(kubectl get pod -l application=sevenow  -o=jsonpath='{.items[*].metadata.name}')

./deployment/kubernetes/scripts/port-forwards.sh

 DOCKER_HOST_IP=localhost ./gradlew :sevenow-end-to-end-tests:cleanTest :sevenow-end-to-end-tests:test
