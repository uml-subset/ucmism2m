#!/bin/bash

# UCMIS M2M Transformation Runner Script

set -e

# Default paths
PRODUCT_DIR="ucmism2m.product/target/products/ucmism2m"
EXECUTABLE="ucmism2m"

# Detect OS
if [[ "$OSTYPE" == "linux-gnu"* ]]; then
    PLATFORM="linux/gtk/x86_64"
elif [[ "$OSTYPE" == "darwin"* ]]; then
    PLATFORM="macosx/cocoa/x86_64"
elif [[ "$OSTYPE" == "msys" ]] || [[ "$OSTYPE" == "win32" ]]; then
    PLATFORM="win32/win32/x86_64"
    EXECUTABLE="ucmism2m.exe"
else
    echo "Unsupported OS: $OSTYPE"
    exit 1
fi

EXEC_PATH="${PRODUCT_DIR}/${PLATFORM}/${EXECUTABLE}"

# Check if executable exists
if [ ! -f "$EXEC_PATH" ]; then
    echo "Error: Executable not found at $EXEC_PATH"
    echo "Please run 'mvn clean verify' first to build the product."
    exit 1
fi

# Check arguments
if [ "$#" -ne 6 ]; then
    echo "Usage: $0 -input <input.uml> -output <output.uml> -config <config.json>"
    exit 1
fi

# Run transformation
echo "Running UCMIS M2M Transformation..."
echo "===================================="
"$EXEC_PATH" "$@"
