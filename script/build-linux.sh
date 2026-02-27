#!/bin/bash
echo "Building for Linux only..."
mvn clean verify -P linux-only
echo "Warning is normal: [WARNING] includeAllPlatforms='true' and includeMode='planner' are incompatible. Ignoring 'includeAllPlatforms' flag"
