FROM eclipse-temurin:24-jdk AS builder

# Install Maven
RUN apt-get update && \
    apt-get install -y maven && \
    rm -rf /var/lib/apt/lists/*

WORKDIR /build

# Copy only the tcc directory (containing pom.xml and src/)
COPY tcc/ /build/tcc/

# Compile the project
WORKDIR /build/tcc
RUN mvn clean package

FROM eclipse-temurin:24-jdk

# Install GNU time and wget for benchmarking and downloading datasets
RUN apt-get update && \
    apt-get install -y time wget tar && \
    rm -rf /var/lib/apt/lists/*

WORKDIR /ws

# Set up the expected directory structure
RUN mkdir -p jars datasets output

# Copy the compiled JARs
COPY --from=builder /build/tcc/target/tcc-1.0-two-tbfs-app.jar jars/
COPY --from=builder /build/tcc/target/tcc-1.0-reverse-scan-app.jar jars/
COPY --from=builder /build/tcc/target/tcc-1.0-tc-app.jar jars/

# Drop the reviewer into an interactive shell by default
CMD ["/bin/bash"]