#!/bin/bash
echo "Building for all platforms (Linux, Windows, macOS)..."
mvn clean verify -P all-platforms
echo "Warning is normal: [WARNING] includeAllPlatforms='true' and includeMode='planner' are incompatible. Ignoring 'includeAllPlatforms' flag"
