FROM eclipse-temurin:21-jdk-alpine
LABEL authors="ankit"

WORKDIR /springboot-devops-app

COPY build/libs/springboot-devops-app-0.0.1-SNAPSHOT.jar springboot-devops-app-0.0.1-SNAPSHOT.jar

ENTRYPOINT ["java", "-jar" , "/springboot-devops-app/springboot-devops-app-0.0.1-SNAPSHOT.jar"]
