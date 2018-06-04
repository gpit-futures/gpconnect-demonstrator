FROM java:8
EXPOSE 19191
ADD gpconnect-demonstrator-api/target/ /docker/
ENTRYPOINT ["java","-jar","/docker/gpconnect-demonstrator-api.war", "--spring.config.location=file:/docker/config/gpconnect-demonstrator-api.properties", "--server.port=19192", "--server.port.http=19191", "--config.path=/docker/config/", "--server.ssl.key-store=/docker/config/server.jks", "--server.ssl.key-store-password=password", "--server.ssl.trust-store=/docker/config/server.jks", "--server.ssl.trust-store-password=password", "--server.ssl.client-auth=want"]