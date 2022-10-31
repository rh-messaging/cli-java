package com.redhat.amqx.management.artemis;

import com.redhat.amqx.main.NodeType;
import com.redhat.amqx.management.Credentials;
import com.redhat.amqx.management.DestinationManager;
import com.redhat.amqx.management.exception.DestinationException;
import org.apache.activemq.artemis.api.core.ActiveMQAddressDoesNotExistException;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.management.InvalidApplicationException;
import java.io.IOException;
import java.util.List;

/**
 * JMS Queue object management for Apache Artemis/AMQ7 broker.
 */
public class AddressArtemisManager extends AbstractArtemisManager implements DestinationManager {

    public AddressArtemisManager(String url, Credentials credentials, String brokerName, String host) throws IOException {
        super(url, credentials, brokerName, host);
    }

    @Override
    public boolean destinationExists(String name) throws Exception {
        if (name == null) {
            throw new IllegalArgumentException("Address name not supplied!");
        }
        return getAddresses().contains(name);
    }

    @Override
    public void listDestinations(boolean isVerbose) throws Exception {
        String reportJson;
        List<String> addressNames = getAddresses();
        if (isVerbose) {
            JSONObject jsonObject = new JSONObject();
            for (String addressName : addressNames) {
                jsonObject.put(addressName, getDestinationProperties(addressName, null, NodeType.ADDRESS));
            }
            reportJson = jsonObject.toString();
        } else {
            reportJson = new JSONArray(addressNames).toString();
        }
        formatter.printConvertedJson(reportJson);
    }

    /**
     * Remove address of given destinationName. AddressName is completely ignored for this method.
     * @param destinationName name of destination to be removed
     * @param addressName
     * @throws Exception
     */
    @Override
    public void removeDestination(String destinationName, String addressName) throws Exception {
        if (!destinationExists(destinationName)) {
            throw new DestinationException("Address '" + destinationName + "' does not exist!");
        } else {
            try {
                getServerControlMBean().deleteAddress(destinationName);
            } catch (ActiveMQAddressDoesNotExistException existException) {
                logger.error("Unable to remove non-existing address '" + destinationName + "'", existException.getMessage());
            }
        }
        logger.info("Address '" + destinationName + "' removed");
    }

    @Override
    public void removeMessages(String destinationName, String addressName) throws Exception {
        throw new InvalidApplicationException("Unable to remove messages from Address!");
    }


    @Override
    /**
     * Create address. None of the parameters other then destinationName are valid.
     */
    public void addDestination(String destinationName, boolean durable, String routingType, String selector) throws Exception {
        if (destinationExists(destinationName)) {
            throw new DestinationException("Address '" + destinationName + "' already exists!");
        } else {
            getServerControlMBean().createAddress(destinationName, routingType);
            logger.info("Address '" + destinationName + "' created");
        }
    }

    /**
     * This method is not used on addresses
     * @param destinationName create this destination
     * @param durable create durable destination/queue
     * @param addressName address name for this queue
     * @param selector filter to be used on this address/queue
     * @param maxConsumers maximum numbers of consumers
     * @param deleteOnNoConsumers whether to delete this destination on no consumers
     * @throws Exception
     */
    public void addDestination(String destinationName, boolean durable, String addressName, String selector, int maxConsumers, boolean deleteOnNoConsumers) throws Exception {
        addDestination(destinationName, durable, null, selector);
    }

    @Override
    public void getDestinationProperties(String addressName, String unused) throws Exception {
        if (destinationExists(addressName)) {
            formatter.printConvertedJson(new JSONObject(getDestinationProperties(addressName, null, NodeType.ADDRESS)).toString());
        } else {
            throw new DestinationException(String.format("Address '%s' does not exist!", addressName));
        }
    }

    @Override
    public void updateDestination(String destinationName, boolean durable, String addressName, String selector, int maxConsumers, boolean deleteOnNoConsumers, String routingType) throws Exception {
        getServerControlMBean().updateAddress(addressName, routingType);
    }
}
