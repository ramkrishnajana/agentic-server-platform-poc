#!/bin/bash
set -e

echo "Building Agentic Server Platform POC Docker Images..."

# Build all projects first
echo "Building Maven projects..."
./mvnw clean package -DskipTests

# Build plugin worker images (these need to be built separately as they are spawned dynamically)
echo "Building Java Add Plugin image..."
docker build -t java-plugin-add:latest -f java-plugin-add/Dockerfile .

echo "Building Java Multiply Plugin image..."
docker build -t java-plugin-multiply:latest -f java-plugin-multiply/Dockerfile .

echo "Building Python Subtract Plugin image..."
docker build -t python-plugin-subtract:latest -f python-plugin-subtract/Dockerfile .

echo "All plugin worker images built successfully!"
echo ""
echo "Now you can run: docker-compose up -d"

