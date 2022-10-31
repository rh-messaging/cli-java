package com.redhat.amqx.management.artemis;

import com.redhat.amqx.main.NodeType;
import com.redhat.amqx.management.Credentials;
import com.redhat.amqx.management.DestinationManager;
import com.redhat.amqx.management.exception.DestinationException;
import org.apache.activemq.artemis.api.core.ActiveMQAddressDoesNotExistException;
import org.apache.activemq.artemis.api.core.ActiveMQNonExistentQueueException;
import org.apache.activemq.artemis.api.core.RoutingType;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Map;

/**
 * JMS Topic object management class for Apache Artemis/AMQ7 broker.
 */
public class TopicArtemisManager extends AbstractArtemisManager implements DestinationManager {
    public TopicArtemisManager(String url, Credentials credentials, String brokerName, String host) throws IOException {
        super(url, credentials, brokerName, host);
    }

    @Override
    public boolean destinationExists(String name) throws Exception {
        if (name == null) {
            throw new IllegalArgumentException("Destination name not supplied!");
        }
        return getTopics().containsKey(name);
    }

    @Override
    public void listDestinations(boolean isVerbose) throws Exception {
        String reportJson;
        Map<String, String> topicNames = getTopics();
        if (isVerbose) {
            JSONObject jsonObject = new JSONObject();
            for (String topicName : topicNames.keySet()) {
                jsonObject.put(topicName, getDestinationProperties(topicNames.get(topicName), topicName, NodeType.TOPIC));
            }
            reportJson = jsonObject.toString();
        } else {
            reportJson = new JSONArray(topicNames.keySet()).toString();
        }
        formatter.printConvertedJson(reportJson);
    }

    @Override
    public void removeDestination(String destinationName, String addressName) throws Exception {
        if (addressName == null)  {
            addressName = destinationName;
        }
        if (!destinationExists(destinationName)) {
            throw new DestinationException("Destination '" + destinationName + "' does not exist!");
        } else {
            try {
                getServerControlMBean().destroyQueue(destinationName, true, false);
                if (addressName.equals(destinationName)) getServerControlMBean().deleteAddress(addressName);
            } catch (ActiveMQNonExistentQueueException e) {
                // try to remove address
                logger.warn("Unable to remove non-existing topic '" + destinationName + "' Trying to remove address.", e.getMessage());
                try {
                    if (addressName.equals(destinationName)) getServerControlMBean().deleteAddress(addressName);
                } catch (ActiveMQAddressDoesNotExistException existException) {
                    logger.error("Unable to remove non-existing address '" + destinationName + "'", existException.getMessage());
                }
            }
            logger.info("Topic '" + destinationName + "' removed");
        }
    }

    @Override
    public void removeMessages(String destinationName, String addressName) throws Exception {
        if (addressName == null)  {
            addressName = destinationName;
        }
        if (destinationExists(destinationName)) {
            getTopicControlMBean(addressName, destinationName).removeAllMessages();
            logger.info(String.format("Removed all messages from '%s' ", destinationName));
        } else {
            throw new DestinationException("Destination '" + destinationName + "' does not exist!");
        }
    }

    /**
     * Create JMS Topic (internally queue is created with binding /address name)
     *
     * @param destinationName create this destination
     * @param durable         not used with topics
     * @param addressName         not used with topics
     * @param selector        not used with topics
     * @throws Exception
     */
    @Override
    public void addDestination(String destinationName, boolean durable, String addressName, String selector) throws Exception {
        if (addressName == null)  {
            addressName = destinationName;
        }
        if (destinationExists(destinationName)) {
            throw new DestinationException("Destination '" + destinationName + "' already exists!");
        } else {
            getServerControlMBean().createQueue(addressName, destinationName, selector, durable, RoutingType.MULTICAST.toString());
            logger.info("topic '" + destinationName + "' created");
        }
    }


    @Override
    public void addDestination(String destinationName, boolean durable, String addressName, String selector, int maxConsumers, boolean deleteOnNoConsumers) throws Exception {
        if (addressName == null)  {
            addressName = destinationName;
        }
        if (destinationExists(destinationName)) {
            throw new DestinationException("Destination '" + destinationName + "' already exists!");
        } else {
            getServerControlMBean().createQueue(addressName, RoutingType.MULTICAST.toString(), destinationName, selector, durable, maxConsumers, deleteOnNoConsumers, true);
            logger.info(String.format("Topic '%s' created", destinationName));
        }
    }

    @Override
    public void getDestinationProperties(String addressName, String destinationName) throws Exception {
        if (addressName == null) {
            addressName = destinationName;
        }
        if (destinationExists(destinationName)) {
            formatter.printConvertedJson(new JSONObject(getDestinationProperties(addressName, destinationName, NodeType.TOPIC)).toString());
        } else {
            throw new DestinationException(String.format("Topic '%s' does not exist!", destinationName));
        }
    }

    /**
     *
     * @param destinationName
     * @param durable
     * @param addressName
     * @param selector
     * @param maxConsumers
     * @param purgeOnNoConsumers
     * @throws Exception
     */
    @Override
    public void updateDestination(String destinationName, boolean durable, String addressName, String selector, int maxConsumers, boolean purgeOnNoConsumers, String routingType) throws Exception {
        getServerControlMBean().updateQueue(destinationName, RoutingType.MULTICAST.toString(), maxConsumers, purgeOnNoConsumers);
    }
}
