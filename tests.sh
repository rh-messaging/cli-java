#!/usr/bin/env bash
set -Eeuo pipefail
set -x

#
# Copyright (c) 2021 Red Hat, Inc.
#
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements. See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License. You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

java -jar cli-activemq/target/cli-activemq-1.2.2-SNAPSHOT-*.jar sender --address cli-activemq --log-msgs json --count 1
java -jar cli-activemq/target/cli-activemq-1.2.2-SNAPSHOT-*.jar receiver --address cli-activemq --log-msgs json --count 1
java -jar cli-activemq/target/cli-activemq-1.2.2-SNAPSHOT-*.jar sender --conn-username test --conn-ssl-verify-host false --conn-password test --msg-content msg no. %d --broker ssl://127.0.0.1:61617 --conn-auth-mechanisms PLAIN --timeout 30 --log-msgs json --log-lib trace --address message-basiccli_jms --count 10 --conn-ssl-trust-all true

java -jar cli-artemis-jms/target/cli-artemis-jms-1.2.2-SNAPSHOT-*.jar sender --address cli-artemis-jms --log-msgs json --count 1
java -jar cli-artemis-jms/target/cli-artemis-jms-1.2.2-SNAPSHOT-*.jar receiver --address cli-artemis-jms --log-msgs json --count 1
java -jar cli-artemis-jms/target/cli-artemis-jms-1.2.2-SNAPSHOT-*.jar sender --conn-username test --conn-ssl-verify-host false --conn-password test --msg-content msg no. %d --broker tcp://127.0.0.1:61617 --conn-auth-mechanisms PLAIN --timeout 30 --log-msgs json --log-lib trace --address message-basiccli_jms --count 10 --conn-ssl-trust-all true

java -jar cli-paho-java/target/cli-paho-java-1.2.2-SNAPSHOT-*.jar sender --address cli-paho-java --log-msgs json --count 1

if [[ $TRAVIS_JDK_VERSION == "openjdk8" ]] || [[ $TRAVIS_JDK_VERSION == "oraclejdk8" ]]; then exit 0; fi

cli_qpid_jms_jar=$(find cli-qpid-jms/target -name 'cli-qpid-jms-1.2.2-SNAPSHOT-*.jar' -not -name '*-tests.jar')
java -jar ${cli_qpid_jms_jar} sender --address cli-qpid-jms --log-msgs json --count 1
java -jar ${cli_qpid_jms_jar} receiver --address cli-qpid-jms --log-msgs json --count 1
java -jar ${cli_qpid_jms_jar} sender --conn-username test --conn-ssl-verify-host false --conn-password test --msg-content msg no. %d --broker amqps://127.0.0.1:5673 --conn-auth-mechanisms PLAIN --timeout 30 --log-msgs json --log-lib trace --address message-basiccli_jms --count 10 --conn-ssl-trust-all true
