package com.redhat.amqx.management;

import com.redhat.amqx.main.BrokerType;
import com.redhat.amqx.management.activemq.BrokerActiveMQManager;
import com.redhat.amqx.management.activemq.QueueActiveMQManager;
import com.redhat.amqx.management.activemq.TopicActiveMQManager;
import com.redhat.amqx.management.artemis.*;
import com.redhat.amqx.management.exception.DestinationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Factory for creation of Broker, Queue, Topic Managers
 */
public class ManagerFactory {
    private static final Logger logger = LoggerFactory.getLogger(ManagerFactory.class);

    public ManagerFactory() {
    }

    /**
     * Create BrokerManager object based on given broker type and other input parameters.
     * ActiveMQ broker type is Apache ActiveMQ6 / A-MQ 6
     * Artemis broker type is Apache Artemis / A-MQ 7
     *
     * @param jmxURL
     * @param credentials
     * @param brokerName
     * @param brokerTypeName
     * @return
     * @throws IOException
     */
    public BrokerManager createBrokerManager(String jmxURL, Credentials credentials, String brokerName,
                                             String brokerTypeName, String host) throws IOException {
        BrokerManager brokerManager = null;
        BrokerType brokerType = resolveBrokerType(brokerTypeName);
        if (brokerType.equals(BrokerType.ARTEMIS)) {
            brokerName = checkBrokerName(brokerName, BrokerType.ARTEMIS);
            brokerManager = new BrokerArtemisManager(jmxURL, credentials, brokerName, host);
        } else if (brokerType.equals(BrokerType.ACTIVEMQ)) {
            brokerName = checkBrokerName(brokerName, BrokerType.ACTIVEMQ);
            brokerManager = new BrokerActiveMQManager(jmxURL, credentials, brokerName, host);
        } else {
            logger.error("Could not create BrokerManager!" + brokerTypeName);
        }
        return brokerManager;
    }

    private String checkBrokerName(String brokerName, BrokerType brokerType) {
        if (brokerName == null || brokerName.equals("")) {
            brokerName = brokerType.getDefaultBrokerName();
        }
        return brokerName;
    }

    /**
     * Figure out the broker type based on provided parameters
     * and return the given Broker type.
     * @param brokerTypeName
     * @return brokerType to be used from parameters
     */
    private BrokerType resolveBrokerType(String brokerTypeName) {
        if (brokerTypeName == null || brokerTypeName.equals("")) {
            return BrokerType.ARTEMIS;
        } else {
            brokerTypeName = brokerTypeName.toUpperCase().trim();
            if (brokerTypeName.equals(BrokerType.ACTIVEMQ.toString())) {
                return BrokerType.ACTIVEMQ;
            } else if (brokerTypeName.equals(BrokerType.ARTEMIS.toString())) {
                return BrokerType.ARTEMIS;
            } else {
                logger.error("Unknown broker type '" + brokerTypeName + "'!");
                throw new IllegalArgumentException("Unknown broker type");
                // print help?
            }
        }
    }

    /**
     * Create queue manager for given broker
     * @param jmxURL
     * @param credentials
     * @param brokerName
     * @param brokerTypeName
     * @return DestinationManager of given subclass Queue{AMQ,Artemis}Manager based on brokerType
     * @throws IOException
     */
    public DestinationManager createQueueManager(String jmxURL, Credentials credentials, String brokerName, String brokerTypeName, String host) throws IOException, DestinationException {
        DestinationManager queueManager;
        BrokerType brokerType = resolveBrokerType(brokerTypeName);
        if (brokerType.equals(BrokerType.ACTIVEMQ)) {
            throw new DestinationException("ActiveMQ broker does not support destination command. Use 'queue' or 'topic'");
        } else if (brokerType.equals(BrokerType.ARTEMIS)) {
            brokerName = checkBrokerName(brokerName, brokerType);
            queueManager = new AddressArtemisManager(jmxURL, credentials, brokerName, host);
        } else {
            logger.error("Unknown brokerType " + brokerTypeName);
            throw new IllegalArgumentException("Unknown broker type" + brokerTypeName);
        }
        return queueManager;
    }

    /**
     * Create topic manager for given broker
     * @param jmxURL
     * @param credentials
     * @param brokerName
     * @param brokerTypeName
     * @return DestinationManager of given subclass Topic{AMQ,Artemis}Manager based on brokerType
     * @throws IOException
     */
    public DestinationManager createTopicManager(String jmxURL, Credentials credentials, String brokerName, String brokerTypeName, String host) throws IOException {
        DestinationManager topicManager;
        BrokerType brokerType = resolveBrokerType(brokerTypeName);
        if (brokerType.equals(BrokerType.ACTIVEMQ)) {
            brokerName = checkBrokerName(brokerName, brokerType);
            topicManager = new TopicActiveMQManager(jmxURL, credentials, brokerName, host);
        } else if (brokerType.equals(BrokerType.ARTEMIS)) {
            brokerName = checkBrokerName(brokerName, brokerType);
            topicManager = new TopicArtemisManager(jmxURL, credentials, brokerName, host);
        } else {
            logger.error("Unknown brokerType " + brokerTypeName);
            throw new IllegalArgumentException("Unknown broker type" + brokerTypeName);
        }
        return topicManager;
    }

    /**
     * Method used mainly for AMQ7/Artemis,
     * which distinguishes JMSQueue, JMSTopic and Destination/Address.
     *
     * @return DestinationManger object purely for Artemis usage
     */
    public DestinationManager createDestinationManager(String jmxURL, Credentials credentials,
                                                       String brokerName, String brokerTypeName, String host) throws IOException {
        DestinationManager destinationManager;
        BrokerType brokerType = resolveBrokerType(brokerTypeName);
        if (brokerType.equals(BrokerType.ACTIVEMQ)) {
            brokerName = checkBrokerName(brokerName, brokerType);
            destinationManager = new QueueActiveMQManager(jmxURL, credentials, brokerName, host);
        } else if (brokerType.equals(BrokerType.ARTEMIS)) {
            brokerName = checkBrokerName(brokerName, brokerType);
            destinationManager = new DestinationArtemisManager(jmxURL, credentials, brokerName, host);
        } else {
            logger.error("Unknown brokerType " + brokerTypeName);
            throw new IllegalArgumentException("Unknown broker type" + brokerTypeName);
        }
        return destinationManager;
    }

    public DivertArtemisManager createDivertManager(String jmxURL, Credentials credentials,
                                                    String brokerName, String brokerTypeName, String host) throws IOException {
        DivertArtemisManager divertManager = new DivertArtemisManager(jmxURL, credentials, brokerName, host);
        return divertManager;
    }
}
