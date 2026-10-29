FROM openjdk:8u171-jre-alpine
RUN apk --no-cache add curl
CMD java ${JAVA_OPTS} -jar inventory-service.jar
HEALTHCHECK --start-period=30s --interval=5s CMD curl -f http://localhost:8082/actuator/health || exit 1
COPY build/libs/inventory-service.jar .
