# cli-java

cli-java is a collection of commandline messaging clients suitable for interacting with Message Oriented Middleware.

## Getting started

    mvn package
    java -jar cli-qpid-jms/target/cli-qpid-jms-*.jar -b amqp://127.0.0.1:5672 -a myQ --log-msgs dict

### Update versions

    mvn versions:set -DgenerateBackupPoms=false -DnewVersion=2017.07

## List of clis

(Some of the clis below are not developed yet.)

* qpid-java (AMQP 0-9-1)
* qpid-jms (AMQP 1.0)
* Vert.x AMQP Bridge (AMQP 1.0)
* activemq-client (OpenWire)
* artemis-jms-client (Artemis Core)
* hornetq-jms-client (HornetQ)

## Related projects

* https://bitbucket.org/david_kornel/cli-netlite
* https://bitbucket.org/david_kornel/cli-rhea
