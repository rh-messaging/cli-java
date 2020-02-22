ActiveMQ / JBoss Active MQ command-line JMX utility
============

Introduction
----

This is an utility that access Apache ActiveMQ, Apache Artemis and Red Hat A-MQ6 and A-MQ7 remotely.
It can be used to manage queues and topics, destinations and obtain various information.

Building and Installing: with ActiveMQ and Artemis libraries
----
Note: Maven commands are always accompanied with 'pom.xml' file. If you do not execute mvn within directory where pom.xml resides,
please supply -f <path/to/pom.xml> file. pom.xml file is a main building file for any maven project.
To compile and package the code using upstream libraries (using default upstream profile)
```
mvn -P Delivery clean package
```
or
```
mvn -P Delivery clean package -f <path/to/amqx/pom.xml>
```

To compile and package the code using JBoss libraries
```
mvn -P Downstream,Delivery clean package
```

To compile and package the code using Apache ActiveMQ/Artemis libraries (profile is enabled by default)
```
mvn -P Upstream,Delivery clean package
```

There are few options for manually overriding default options for AMQ6 and AMQ7 brokers.
Those are: 'jboss.amq{6,7}.{version,shortversion,build,base}' and 'amq{6,7}.path'. 

JBoss profile will use build 133 by default. It is possible to override this and use newer or older versions using the 
jboss.amq6.build property:
```
mvn -PJBoss,Delivery -Djboss.amq6.build=103 clean package
```

Additional properties such as the amq6.path, for the A-MQ6 installation, as well as the jboss.amq6.base, for the A-MQ6, 
upstream base version can be set if needed:

```
mvn -PJBoss,Delivery -Damq6.path=$HOME/tools/messaging/jboss-a-mq-6.2.0.redhat-102 -Djboss.amq6.build=103 -Djboss.amq6.base=5.13.0 clean package
```

It's important to note, though, that JBoss profile does not include org.apache.activemq:activemq-osgi:5.13.0 in the
tarball generation, therefore it must be manually added to the endorsed directory.


Building and Installing: manual compilation
----

Common dependency jars:

* org.slf4j:slf4j-api:jar:1.6.6
* org.slf4j:slf4j-log4j12:jar:1.6.6
* log4j:log4j:jar:1.2.17
* commons-cli:commons-cli:jar:1.2

Artemis (default) dependencies:
* org.apache.activemq:artemis-core-client:1.2.0
* org.apache.activemq:artemis-jms-client:1.2.0

Plus, either ActiveMQ core:

* org.apache.activemq:activemq-core:jar:5.13.0
* org.apache.activemq:activemq-runtime-config:5.13.0

Or JBoss:
* org.apache.activemq:activemq-osgi:<activemq-base-version>.redhat-<build>
    
Obs.: the dependency list is in maven/gradle format to simplify searching for dependencies in libraries repositories. The format is <group id>:<artifact id>:<version>

Setting up the Broker: with authentication support (the new/correct way)
----
Apache ActiveMQ / Red Hat A-MQ 6:

This should be the preferred JMX access method, since it works out of the box, supports authentication and does not 
require changes to A-MQ's own files.
  
The first step is to add a username and password in the etc/users.properties file. For most purposes, it is ok to just 
use the default settings provided out of the box. For this, just uncomment the following line:

```
admin=admin,admin,manager,viewer,Operator, Maintainer, Deployer, Auditor, Administrator, SuperUser
```

AMQX defaults to admin/admin as the username and password when accessing the broker. Please use --no-auth to completely 
turn off authentication and revert to the previous behavior.

Then, you must bypass credential checks on BrokeViewMBean by adding it to the whitelist ACL configuration. You can do
so by replacing this line:

```
org.apache.activemq.Broker;getBrokerVersion=bypass
```

With this:

```
org.apache.activemq.Broker=bypass
```

In addition to being the correct way, it also enables several different configuration options (eg: port, listen address,
 etc) by just changing the file org.apache.karaf.management.cfg on broker's etc directory. 
 
Please keep in mind that JMX access is made through a different JMX connector root in this case: it uses karaf-root 
instead of jmxrmi, which was previously used in the older method 
(eg.: service:jmx:rmi:///jndi/rmi://<host>:<port>/karaf-root). It also uses port 1099 by default, instead of 1616.

For artemis use following uri (by default port 3000 is used, we prefer 1099):
```
service:jmx:rmi:///jndi/rmi://<host>:<port>/jmxrmi"
```

Setting up the Broker: authentication-less (aka the old way)
----
Apache ActiveMQ:
-----

The first step is to configure the broker to accept remote JMX connections. You can do so by editing the file setenv
within the bin directory. You must uncomment the line that sets KARAF_OPTS and leave it like this:

```
export KARAF_OPTS="-Dcom.sun.management.jmxremote.port=1616 -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false"
```

Then, you must bypass credential checks on BrokeViewMBean by adding it to the whitelist ACL configuration. You can do
so by replacing this line:

```
org.apache.activemq.Broker;getBrokerVersion=bypass
```

With this:

```
org.apache.activemq.Broker=bypass
```

Note that the JMX connector root for this method is jmxrmi.


Setting up the Broker: authentication-less
----
Apache Artemis / Red Hat A-MQ 7:
-----


For now, use only unsecured method (no authentication):
Add following lines to the JAVA_ARGS variable located in ${BROKER_START_LOCATION}/etc/artemis.profile:
```
JAVA_ARGS+="-Dcom.sun.management.jmxremote=true -Dcom.sun.management.jmxremote.port=1099 -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.authenticate=false "
```

Setting up the Broker: authentication
----
Apache Artemis / Red Hat A-MQ 7:
-----
Add following lines to the JAVA_ARGS variable located in ${BROKER_START_LOCATION}/etc/artemis.profile:
```
JAVA_ARGS+="-Dcom.sun.management.jmxremote=true -Dcom.sun.management.jmxremote.port=1099 -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.authenticate=true "
```
TODO!

Setting up the Broker: configuration reload support
----

Reloading broker configuration relies on the RunTimeConfiguration plugin. At the very minimum,  the following 
configuration must be added to the activemq.xml configuration file:

```
 <plugins>
  <!-- other plugins, if any -->
  <runtimeConfigurationPlugin checkPeriod="1000" />
 </plugins>
```

Setting up for JBoss usage with libraries
----
If you're planning to use amqx with JBoss libraries, you have add the appropriate activemq core library in the endorsed
directory. To do so, go to the endorsed directory within amqx library dir and run:

```
ln -s /path/to/jboss/system/org/apache/activemq/activemq-osgi/5.11.0.redhat-620103/activemq-osgi-5.11.0.redhat-620103.jar
```

General usage of AMQX client:
---

General usage follow this pattern:
<> amqx topic -u service:jmx:rmi:///jndi/rmi://<host>:<port>/karaf-root -b vm-r6x --action remove -t amqx.test.topic
```
amqx <object> <action> <parameters> [<broker-type> <broker-jmx-url>]
```

Default options to use with AMQX client:

* url/u         - JMS url to the RMI service of broker (default: service:jmx:rmi:///jndi/rmi://localhost:1099/jmxrmi)
* broker-type/t - broker to contact [activemq, artemis] (default: artemis)
* broker-name/b - name of the broker to be contacted (default: amq)
* help/h        - print help about commands
* action/a      - action to be performed on given object
* username      - username to be used
* password      - password to be used
* no-auth       - do not use authentication

In general, AMQX client distinguishes 3 main objects on which you can call other actions:

* broker - broker actions
* queue  - queue (add, remove, list, properties) actions
* topic  - topic (add, remove, list, properties) actions
* address - Artemis only, destination is address/queue support using core protocol (not JMS protocol)
* divert - (add, remove, list) actions. Support for Artemis only


On **broker** object, you can call following actions:

* reload     - should reload the broker (not working correctly)
* connectors - should list all available connectors on the broker
* list-destinations - lists all known destinations on the broker (Artemis only)
* topology  - list network topology known to given broker (Artemis only)

On **queue** and **destination** objects, supported actions are:

* add    - create an queue with (--name/n <queuename>, --durable/d [True/False], --address <addrname>, --selector <expression>)
* remove - delete an queue with given name (--name <queuename>) if address is not specified or is same as queue name, address is removed as well
* list   - list queues with properties (by default properties are displayed) (--list-properties/p [True/False])
* properties - print properties of given queue only (--name <queuename>)

On **topic** objects, supported actions are:

* add    - create an topic with (--name <topicname>, --address <addrname> --jndiBindings* <bind1,bind2,..> (jndiBindings not impl yet) )
* remove - delete an topic with given name (--name <topicname>) if address is not specified or is same as queue name, address is removed as well
* list   - list topics with properties (by default properties are displayed) (--list-properties [True/False])
* properties - print properties of given topic only (--name <topicname>)

On **divert** objects, supported actions are:

* add    - create a divert with (-a add --name <divertName> -o <routingName> --address <addressName> -f <forwardAddress> --exclusive true)
* remove - delete a divert with given name (--name <divertName> --address <addressName> )
* list   - list diverts with properties # Not implemented yet (by default properties are displayed) (--list-properties [True/False])


Examples of usage
---
* Create a durable queue on Artemis
```
queue --action add --name myDurableQ -u service:jmx:rmi:///jndi/rmi://<host>:1099/jmxrmi
```

* Create a non-durable queue on Artemis with selector
```
queue -a add -n myDurableQ -d false -s "JMSMessageID > 4" -u service:jmx:rmi:///jndi/rmi://<host>:1099/jmxrmi
```

* Create a topic on Artemis broker on localhost
```
topic -a add -n myTopic
```

* Create a topic with specified address on Artemis broker on localhost
```
topic -a add -n myTopic --address myTopicAddress
```

* Create a topic on ActiveMQ broker on localhost
```
topic --action add --name myTopic --broker-type activemq --url service:jmx:rmi:///jndi/rmi://localhost:1099/karaf-root
topic -a add -n myTopic -t activemq
```

* Create a destination (core protol queue) on local Artemis broker
```
destination --action add --name myArtemisQueue
```

* Delete a topic/queue/address (X) from broker
```
X --action remove --name myX --url service:jmx:rmi:///jndi/rmi://<host>:1099/jmxrmi
```

* Delete a topic/queue (X) with different address from broker (*address supported on Artemis only)
```
X --action remove --name myX --address myXaddress --url service:jmx:rmi:///jndi/rmi://<host>:1099/jmxrmi
```

* List all jms queues from artemis broker without properties
```
queue --action list --list-properties false -t artemis -u service:jmx:rmi:///jndi/rmi://<host>:1099/jmxrmi
queue -a list --list-properties false -u service:jmx:rmi:///jndi/rmi://<host>:1099/jmxrmi
```

* List all jms topics from artemis broker with properties
```
topic --action list -u service:jmx:rmi:///jndi/rmi://<host>:1099/jmxrmi
```

* List all queues from activemq broker with properties
```
queue --broker-name amq --action list --broker-type activemq --list-properties true -u service:jmx:rmi:///jndi/rmi://<host>:1099/karaf-root
queue -a list -t activemq -u service:jmx:rmi:///jndi/rmi://<host>:1099/karaf-root
```

* List all topics from activemq broker with properties
```
topic -b amq -a list -t activemq -u service:jmx:rmi:///jndi/rmi://<host>:1099/karaf-root
```

* List all destinations, queues, topics and addresses from Artemis broker
```
broker --action list-destinations
```

* Show currently known topology for given Artemis broker
```
broker --action topology
```

* Create new divert
```
divert -a add --name myDivert -o routingName --address lalaQ -f forwardingAddress -e true
```

* Delete divert
```
divert -a remove --address lalaQ --name myDivert
```

* List divert
```
divert -a list
```


Running: authentication support
----

```
amqx topic -u service:jmx:rmi:///jndi/rmi://<host>:<port>/karaf-root --username admin --password something -b vm-r6x --action list
```

Note: please note that admin/admin username and password are implied by default and therefore, are not required command 
line parameters unless some other username/password are setup. 

Running: completely disabling authentication support
----

```
amqx topic -u service:jmx:rmi:///jndi/rmi://<host>:<port>/karaf-root --no-auth -b vm-r6x --action list
```

Running: reloading broker configuration
----
```
amqx broker -u service:jmx:rmi:///jndi/rmi://<host>:<port>/karaf-root -b amq --reload
```

Please keep in mind that reloading broker configuration relies on the RunTimeConfiguration plugin. This is documented in 
the item "Setting up the Broker: configuration reload support".

References
----

* [Karaf Monitoring and Management using JMX] (http://karaf.apache.org/manual/latest/users-guide/monitoring.html)
* [BrokerViewMBean bypass](https://github.com/hawtio/hawtio/issues/1747)
* [The JMX Service URL] (http://docs.oracle.com/cd/E19717-01/819-7758/gcnqf/index.html)
* [Runtime Configuration] (http://activemq.apache.org/runtime-configuration.html)

References (Red Hat Internal)
----

* [This Guide on Mojo] (https://mojo.redhat.com/docs/DOC-1034320)


Internal metadata for object names
----


In terms of the actual ObjectName syntax, top-level components like acceptors, bridges, broadcast-groups, and cluster-connections use this format:
```
<domain>:broker="<brokerName>",component=[acceptors|bridges|broadcast-groups|cluster-connections],name="<componentName>"
```
Addresses follow this format:
```
<domain>:broker="<brokerName>",component=addresses,address="<addressName>"
```
Then there's sub-components of an address like queues and diverts.

Queues follow this format:
```
<domain>:broker="<brokerName>",component=addresses,address="<addressName>",subcomponent=queues,routing-type="<routingType>",queue="<queueName>"
```
Diverts follow this format:
```
<domain>:broker="<brokerName>",component=addresses,address="<addressName>",subcomponent=diverts,divert="<divertName>"
```
