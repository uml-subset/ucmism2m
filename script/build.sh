#!/bin/bash

# UCMIS M2M Build Script

set -e

echo "Building UCMIS M2M Transformation..."
echo "======================================"

# Check Java version
JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt 21 ]; then
    echo "Error: Java 21 or higher required (found Java $JAVA_VERSION)"
    exit 1
fi

echo "Java version: OK"

# Run Maven build
mvn clean verify

echo ""
echo "Build completed successfully!"
echo "Product location: ucmism2m.product/target/products/"
