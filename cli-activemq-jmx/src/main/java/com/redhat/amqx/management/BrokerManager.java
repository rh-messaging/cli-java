package com.redhat.amqx.management;

import com.redhat.amqx.main.BrokerType;

import javax.management.MalformedObjectNameException;

/**
 * Created by mtoth on 11/4/15.
 */
public interface BrokerManager {

    void reload() throws MalformedObjectNameException;

    /**
     * Get a list of transport connectors
     *
     * @throws Exception
     */
    void getTransportConnectors() throws Exception;

    /**
     * Get current broker topology list HA/cluster
     *
     * @throws Exception
     */
    void getNetworkTopology() throws Exception;

    BrokerType getBrokerType();
    /**
     * Return sessions for connections
     * or for given connection only, if provided.
     *
     * @throws Exception
     */
    void getSessions(String connectionId) throws Exception;

    /**
     * Get all the possible destinations on the broker.
     * It is a map of addresses, queues, jms-queues, jms-topics with their names.
     * @throws Exception
     */
    void getAllBrokerDestinations() throws Exception;

    /**
     * Get all the possible destinations on the broker with properties.
     * It is a map of addresses, queues, jms-queues, jms-topics with their names and their properties.
     * @throws Exception
     */
    void getAllBrokerDestinationsProperties() throws Exception;
}
