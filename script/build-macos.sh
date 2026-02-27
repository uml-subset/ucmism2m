#!/bin/bash
echo "Building for macOS only..."
mvn clean verify -P macos-only
