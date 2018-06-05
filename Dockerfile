FROM java:8
EXPOSE 19191
ARG INTERNAL_PATH=/docker/
ADD gpconnect-demonstrator-api/target/ $INTERNAL_PATH
ENV DATABASE_ADDRESS 10.100.100.61
ENV DATABASE_PORT 3306
ENTRYPOINT java -jar /docker/gpconnect-demonstrator-api.war \
--spring.config.location=file:/docker/config/gpconnect-demonstrator-api.properties --server.port=19192 \
--server.port.http=19191 --config.path=/docker/config/ --server.ssl.key-store=/docker/config/server.jks \
--server.ssl.key-store-password=password --server.ssl.trust-store=/docker/config/server.jks \
--server.ssl.trust-store-password=password --server.ssl.client-auth=want --datasource.host=$DATABASE_ADDRESS \
--datasource.port=$DATABASE_PORT