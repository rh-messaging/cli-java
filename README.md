# cli-java

[![Build Status](https://travis-ci.org/rh-messaging/cli-java.svg?branch=master)](https://travis-ci.org/rh-messaging/cli-java)
[![Code Coverage](https://codecov.io/gh/rh-messaging/cli-java/branch/master/graph/badge.svg)](https://codecov.io/gh/rh-messaging/cli-java)
[![Coverity Scan Status](https://scan.coverity.com/projects/14128/badge.svg)](https://scan.coverity.com/projects/cli-java)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/6af323f5f8804b659418013a719f3708)](https://www.codacy.com/app/jdanekrh/cli-java?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=rh-messaging/cli-java&amp;utm_campaign=Badge_Grade)
[![Code Climate](https://codeclimate.com/github/rh-messaging/cli-java/badges/gpa.svg)](https://codeclimate.com/github/rh-messaging/cli-java)

cli-java is a collection of commandline messaging clients suitable for interacting with Message Oriented Middleware.

## Getting started

When using IntelliJ IDEA Ultimate Edition, select "Open" (not "Import Project") option to open project and delete OSGi facets in File >> Project Structure >> Project Settings >> Facets.

    mvn clean package  # compile without executing external tests (tests that require broker)
    java -jar cli-qpid-jms/target/cli-qpid-jms-*.jar sender -b amqp://127.0.0.1:5672 -a myQ --log-msgs dict

### Run tests

    mvn test -Ptests
    
    mvn test -Pcoverage,tests  # collect coverage using JaCoCo
    
    mvn clean test -Dmaven.test.failure.ignore
    find -wholename "*/surefire-reports/TEST-*.xml" | zip -j@ test_results.zip
    

### Update versions

    mvn versions:set -DgenerateBackupPoms=false -DnewVersion=2017.07

## List of Java clis

* qpid-jms (AMQP 1.0)
* activemq-client (OpenWire)
* artemis-jms-client (Artemis Core)

## Related projects

* https://github.com/rh-messaging/cli-netlite
* https://github.com/rh-messaging/cli-rhea
* https://github.com/rh-messaging/cli-proton-python
* https://github.com/rh-messaging/cli-proton-ruby
