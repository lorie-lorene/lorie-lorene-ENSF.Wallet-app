FROM maven:3.9-openjdk-17-slim AS builder

WORKDIR /app

# Copy pom.xml and download dependencies (for better caching)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build
COPY src ./src
RUN mvn clean package -DskipTests

# Production Stage
FROM openjdk:17-jre-slim

# Install security updates and tools
RUN apt-get update && \
    apt-get upgrade -y && \
    apt-get install -y --no-install-recommends \
    curl \
    netcat-openbsd && \
    rm -rf /var/lib/apt/lists/*

# Create non-root user for security
RUN groupadd -r userservice && \
    useradd -r -g userservice -d /app -s /bin/bash userservice

WORKDIR /app

# Copy JAR from builder stage
COPY --from=builder /app/target/serviceUser-*.jar app.jar

# Create directories and set permissions
RUN mkdir -p /app/logs /app/uploads && \
    chown -R userservice:userservice /app

# Switch to non-root user
USER userservice

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8081/actuator/health || exit 1

# Expose port
EXPOSE 8081

# JVM optimization for containers
ENV JAVA_OPTS="-XX:+UseContainerSupport \
               -XX:MaxRAMPercentage=75 \
               -XX:+UseG1GC \
               -XX:+UseStringDeduplication \
               -Djava.security.egd=file:/dev/./urandom"

# Application configuration
ENV SPRING_PROFILES_ACTIVE=prod

# Start application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]