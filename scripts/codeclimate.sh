#!/usr/bin/env bash

curl -L https://codeclimate.com/downloads/test-reporter/test-reporter-latest-linux-amd64 > ./cc-test-reporter
chmod +x ./cc-test-reporter
./cc-test-reporter before-build

mvn cobertura:cobertura -Dcobertura.report.format=xml -Dcobertura.aggregate=true

./cc-test-reporter after-build --coverage-input-type cobertura --exit-code $?
