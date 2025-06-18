FROM eclipse-temurin:17-jdk AS builder
WORKDIR /app
COPY . .
RUN ./gradlew clean build

# ---------------------------------------

FROM eclipse-temurin:17-jdk 
WORKDIR /app
RUN apt-get update && \
    apt-get install -y parallel && \
    rm -rf /var/lib/apt/lists/*
COPY --from=builder /app/build/libs/MalTapExtract-1.0-SNAPSHOT.jar app.jar
COPY entrypoint.sh entrypoint.sh
RUN chmod +x entrypoint.sh
ENTRYPOINT ["./entrypoint.sh", "/app/app.jar"]