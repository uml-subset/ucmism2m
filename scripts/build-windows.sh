#!/bin/bash
echo "Building for Windows only..."
mvn clean verify -P windows-only
