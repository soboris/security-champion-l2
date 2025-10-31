FROM openjdk:17-jdk-slim

WORKDIR /app

# Install curl, dos2unix, and Maven
RUN apt-get update && apt-get install -y curl dos2unix maven && rm -rf /var/lib/apt/lists/*

# Copy Maven wrapper and pom.xml
COPY mvnw .
COPY mvnw.cmd .
COPY .mvn .mvn
COPY pom.xml .

# Fix line endings and make Maven wrapper executable
RUN dos2unix ./mvnw && chmod +x ./mvnw

# Download dependencies using system Maven
RUN mvn dependency:go-offline -B

# Copy source code
COPY src src

# Copy startup script
COPY wait-for-openfga.sh .
RUN chmod +x wait-for-openfga.sh

# Build application using system Maven
RUN mvn clean package -DskipTests

# Run application
EXPOSE 8090
CMD ["./wait-for-openfga.sh"]