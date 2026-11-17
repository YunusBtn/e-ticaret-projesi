FROM maven:3.9-eclipse-temurin-21-noble AS build

WORKDIR /build

COPY pom.xml .

RUN --mount=type=cache,target=/root/.m2 mvn -B dependency:go-offline


COPY src ./src

RUN --mount=type=cache,target=/root/.m2 mvn -B package -Dmaven.test.skip=true \
 && cp target/*.jar app.jar \
 && java -Djarmode=tools -jar app.jar extract --layers --launcher --destination extracted



FROM eclipse-temurin:21-jre-noble AS runtime

RUN groupadd --system spring \
    && useradd --system --gid spring --no-create-home spring


WORKDIR /app

COPY --from=build /build/extracted/dependencies/ ./
COPY --from=build /build/extracted/spring-boot-loader/ ./
COPY --from=build /build/extracted/snapshot-dependencies/ ./
COPY --from=build /build/extracted/application/ ./

USER spring:spring

EXPOSE 8080


ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-XX:+ExitOnOutOfMemoryError", "org.springframework.boot.loader.launch.JarLauncher"]
