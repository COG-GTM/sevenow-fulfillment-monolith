#! /bin/bash -e

./wait-for-services.sh

echo for API - open http://${DOCKER_HOST_IP?}:8081/swagger-ui.html
echo for inventory-service - open http://${DOCKER_HOST_IP?}:8082/swagger-ui.html
echo for delivery-service - open http://${DOCKER_HOST_IP?}:8083/swagger-ui.html
