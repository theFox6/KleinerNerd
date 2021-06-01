#!/bin/bash
PROJECT_DIR="$(dirname "$(readlink -f $0)")" #resolve directory the script is in
cd PROJECT_DIR
./gradlew update & #run gradle as detached process