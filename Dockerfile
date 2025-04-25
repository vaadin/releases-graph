# align with CC app to reuse hashes
FROM eclipse-temurin:17-jre
COPY target/*.jar ./app.jar
ENTRYPOINT java -jar ./app.jar

