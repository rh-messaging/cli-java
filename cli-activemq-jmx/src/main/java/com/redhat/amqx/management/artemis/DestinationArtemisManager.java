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
 * Destination object management for Apache Artemis/AMQ7 broker.
 */
public class DestinationArtemisManager extends AbstractArtemisManager implements DestinationManager {

    public DestinationArtemisManager(String url, Credentials credentials, String brokerName, String host) throws IOException {
        super(url, credentials, brokerName, host);
    }

    @Override
    public boolean destinationExists(String name) throws Exception {
        if (name == null) {
            throw new IllegalArgumentException("Destination name not supplied!");
        }
        return getQueues().containsKey(name);
    }

    @Override
    public void listDestinations(boolean isVerbose) throws Exception {
        String reportJson;
        Map<String, String> queueNames = getQueues();
        if (isVerbose) {
            JSONObject jsonObject = new JSONObject();
            for (String topicName : queueNames.keySet()) {
                jsonObject.put(topicName, getDestinationProperties(queueNames.get(topicName), topicName, NodeType.QUEUE));
            }
            reportJson = jsonObject.toString();
        } else {
            reportJson = new JSONArray(queueNames.keySet()).toString();
        }
        formatter.printConvertedJson(reportJson);
    }

    @Override
    public void removeDestination(String destinationName, String addressName) throws Exception {
        if (addressName == null)  {
            addressName = destinationName;
        }
        if (destinationExists(destinationName)) {
            try {

                getServerControlMBean().destroyQueue(destinationName, true, false);
                if (addressName.equals(destinationName)) getServerControlMBean().deleteAddress(addressName);
            } catch (ActiveMQNonExistentQueueException qexc) {
                try {
                    if (addressName.equals(destinationName)) getServerControlMBean().deleteAddress(addressName);
                } catch (ActiveMQAddressDoesNotExistException existException) {
                    logger.error("Unable to remove non-existing address '" + destinationName + "'", existException.getMessage());
                }
            }
            logger.info(String.format("Destination '%s' removed", destinationName));
        } else {
            throw new DestinationException("Destination '" + destinationName + "' does not exist!");
        }
    }

    @Override
    public void removeMessages(String destinationName, String addressName) throws Exception {
        if (addressName == null)  {
            addressName = destinationName;
        }
        if (destinationExists(destinationName)) {
            getQueueControlMBean(addressName, destinationName).removeAllMessages();
            logger.info(String.format("Removed all messages from '%s' ", destinationName));
        } else {
            throw new DestinationException("Destination '" + destinationName + "' does not exist!");
        }
    }


    /**
     * Create durable/non-durable destination (jms queue) on artemis broker.
     *
     * @param destinationName create this destination
     * @param durable         create durable queue
     * @param addressName         not used
     * @param selector        seems like "filter"
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
//            getServerControlMBean().createAddress(addressName, RoutingType.ANYCAST.toString());
            getServerControlMBean().createQueue(addressName, destinationName, selector, durable, RoutingType.ANYCAST.toString());
            logger.info(String.format("Queue '%s' created", destinationName));
        }
    }

    /**
     * Create a new destination on the broker. with extra parameters
     * @param destinationName create this destination
     * @param durable create durable destination/queue
     * @param addressName address name for this queue
     * @param selector filter to be used on this address/queue
     * @param maxConsumers maximum numbers of consumers
     * @param deleteOnNoConsumers whether to delete this destination on no consumers
     * @throws Exception
     */
    @Override
    public void addDestination(String destinationName, boolean durable, String addressName, String selector, int maxConsumers, boolean deleteOnNoConsumers) throws Exception {
        if (addressName == null)  {
            addressName = destinationName;
        }
        if (destinationExists(destinationName)) {
            throw new DestinationException("Destination '" + destinationName + "' already exists!");
        } else {
            getServerControlMBean().createQueue(addressName, RoutingType.ANYCAST.toString(), destinationName, selector, durable, maxConsumers, deleteOnNoConsumers, true);
            logger.info(String.format("Queue '%s' created", destinationName));
        }
    }

    @Override
    public void getDestinationProperties(String addressName, String destinationName) throws Exception {
        if (addressName == null) {
            addressName = destinationName;
        }
        if (destinationExists(destinationName)) {
            formatter.printConvertedJson(new JSONObject(getDestinationProperties(addressName, destinationName, NodeType.QUEUE)).toString());
        } else {
            throw new DestinationException(String.format("Queue '%s' does not exist!", destinationName));
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
        getServerControlMBean().updateQueue(destinationName, RoutingType.ANYCAST.toString(), maxConsumers, purgeOnNoConsumers);
    }

}
