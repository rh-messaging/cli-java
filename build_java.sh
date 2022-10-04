#!/usr/bin/env bash
set -Eeuo pipefail
set -x

mvn -B package --file pom.xml -DskipTests
