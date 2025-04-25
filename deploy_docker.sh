

## Build the application 
mvn clean package -DskipTests -P production 

## You need to be logged in with your user, otherwise run `docker login`
USER=k8sdemos
docker build --tag $USER/releases-graph:latest .
docker push $USER/releases-graph:latest 
