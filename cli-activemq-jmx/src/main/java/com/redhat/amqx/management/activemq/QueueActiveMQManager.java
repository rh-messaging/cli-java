package com.redhat.amqx.management.activemq;

import com.redhat.amqx.management.Credentials;
import com.redhat.amqx.management.DestinationManager;
import com.redhat.amqx.management.exception.DestinationException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.apache.activemq.broker.jmx.BrokerViewMBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.ObjectName;
import java.io.IOException;
import java.util.Map;


/**
 * This class uses JMX to manage queues
 */
public class QueueActiveMQManager extends AbstractActiveMQManager implements DestinationManager {
    private static final Logger logger = LoggerFactory.getLogger(QueueActiveMQManager.class);

    public QueueActiveMQManager(final String url, final Credentials credentials, String brokerName, String host) throws IOException {
        super(url, credentials, brokerName, host);
    }

    @Override
    public boolean destinationExists(String name) throws Exception {
        BrokerViewMBean brokerViewMBean = getResolver(ActiveMQResolver.class).getBrokerView();

        ObjectName[] objectNames = brokerViewMBean.getQueues();

        return destinationExists(objectNames, name);
    }

    @Override
    public void listDestinations(boolean isVerbose) throws Exception {
        if (isVerbose) {
            logger.info(formatter.convertJSON(new JSONObject(listDestinationsWithProperties()).toString()));
        } else {
            logger.info(formatter.convertJSON(new JSONArray(listDestinationsWithoutProperties()).toString()));
        }

    }

    @Override
    protected ObjectName[] getObjectNames() throws Exception {
        return getResolver(ActiveMQResolver.class).getBrokerView().getQueues();
    }

    @Override
    public void removeDestination(final String destinationName, String addressName) throws Exception {
        // TODo addressName is not used now
        BrokerViewMBean brokerViewMBean = getResolver(ActiveMQResolver.class).getBrokerView();

        if (!destinationExists(destinationName)) {
            throw new DestinationException("Queue '" + destinationName + "' does not exist");
        }

        brokerViewMBean.removeQueue(destinationName);
    }

    @Override
    public void removeMessages(String destinationName, String addressName) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public void addDestination(final String destinationName, boolean durable, String addressName, String selector) throws Exception {
        BrokerViewMBean brokerViewMBean = getResolver(ActiveMQResolver.class).getBrokerView();

        if (destinationExists(destinationName)) {
            throw new DestinationException("Queue '" + destinationName + "' already exists");
        } else {
            brokerViewMBean.addQueue(destinationName);
        }
    }

    @Override
    public void addDestination(String destinationName, boolean durable, String addressName, String selector, int maxConsumers, boolean deleteOnNoConsumers) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    protected String getFormattedDestinationProperties(String destinationName) throws Exception {
        Map<String, Object> tmpProps =
                getDestinationProperties(getResolver(ActiveMQResolver.class).getQueueView(null, destinationName));
        return (new JSONObject(tmpProps)).toString();
    }


    @Override
    public void getDestinationProperties(final String addressName, final String queueName) throws Exception {
        if (destinationExists(queueName)) {
            logger.info(formatter.convertJSON(getFormattedDestinationProperties(queueName)));
        } else {
            throw new DestinationException("Queue '" + queueName + "' does not exist");
        }
    }

    @Override
    public void updateDestination(String destinationName, boolean durable, String addressName, String selector, int maxConsumers, boolean purgeOnNoConsumers, String routingType) throws Exception {
        throw new UnsupportedOperationException("Not Supported by AMQ6");
    }

}
