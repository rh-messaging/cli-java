package com.redhat.amqx.management;

/**
 * Destination manager for queues and topics.
 */
public interface DestinationManager {

    /**
     * Check whether destination exists
     * @param name of the queue/topic to check for
     * @return true if destination exists, false otherwise
     * @throws Exception
     */
    boolean destinationExists(String name) throws Exception;

    /**
     * List all destinations with properties on broker.
     * Output is formatted in python dictionary.
     * @param isVerbose if true, list destinations with properties (default behavior)
     * @throws Exception
     */
    void listDestinations(boolean isVerbose) throws Exception;

    /**
     * Remove given destination from broker if exists.
     * Type of the destination is known from action caller
     * @param destinationName name of destination to be removed
     * @throws Exception
     */
    void removeDestination(final String destinationName, final String addressName) throws Exception;

    /**
     * Remove all messages from the queue
     * @param destinationName name of destination/queue to remove all messages from
     * @param addressName name of address to remove all messages from
     * @throws Exception
     */
    void removeMessages(String destinationName, String addressName) throws Exception;

    /**
     * Create a new destination on the broker.
     * @param destinationName create this destination
     * @param durable create durable queue
     * @throws Exception
     */
    void addDestination(final String destinationName, final boolean durable, final String addressName, final String selector) throws Exception;

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
    void addDestination(String destinationName, boolean durable, String addressName, String selector, int maxConsumers, boolean deleteOnNoConsumers) throws Exception;

    /**
     * Get properties of given destination (if exists) from the broker.
     * Output is python dictionary.
     * @param destinationName name of the destination
     * @throws Exception
     */
    void getDestinationProperties(final String addressName, final String destinationName) throws Exception;

    /**
     * Update onb given destination maxConsumers, purgeOnNoconsumers or routingType on address
     * @param destinationName
     * @param durable
     * @param addressName
     * @param selector
     * @param maxConsumers
     * @param purgeOnNoConsumers
     * @param routingType
     * @throws Exception
     */
    void updateDestination(String destinationName, boolean durable, String addressName, String selector, int maxConsumers, boolean purgeOnNoConsumers, String routingType) throws Exception;
}
