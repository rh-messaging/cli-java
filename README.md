# cli-java

[![Build Status](https://travis-ci.org/rh-messaging/cli-java.svg?branch=master)](https://travis-ci.org/rh-messaging/cli-java)
[![Code Coverage](https://codecov.io/gh/rh-messaging/cli-java/branch/master/graph/badge.svg)](https://codecov.io/gh/rh-messaging/cli-java)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/6af323f5f8804b659418013a719f3708)](https://www.codacy.com/app/jdanekrh/cli-java?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=rh-messaging/cli-java&amp;utm_campaign=Badge_Grade)
[![Code Climate](https://codeclimate.com/github/rh-messaging/cli-java/badges/gpa.svg)](https://codeclimate.com/github/rh-messaging/cli-java)

cli-java is a collection of commandline messaging clients suitable for interacting with Message Oriented Middleware.

## Getting started

    mvn package 
    or 
    mvn package -DskipTests=true (without executing tests)
    java -jar cli-qpid-jms/target/cli-qpid-jms-*.jar -b amqp://127.0.0.1:5672 -a myQ --log-msgs dict

### Update versions

    mvn versions:set -DgenerateBackupPoms=false -DnewVersion=2017.07

## List of Java clis

(Some of the clis below are not developed yet.)

* qpid-java (AMQP 0-9-1)
* qpid-jms (AMQP 1.0)
* Vert.x AMQP Bridge (AMQP 1.0)
* activemq-client (OpenWire)
* artemis-jms-client (Artemis Core)
* hornetq-jms-client (HornetQ)

## Related projects

* https://github.com/rh-messaging/cli-netlite
* https://github.com/rh-messaging/cli-rhea
* https://github.com/rh-messaging/cli-proton-python
