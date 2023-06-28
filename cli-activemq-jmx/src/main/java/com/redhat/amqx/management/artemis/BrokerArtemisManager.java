package com.redhat.amqx.management.artemis;

import com.redhat.amqx.main.BrokerType;
import com.redhat.amqx.main.NodeType;
import com.redhat.amqx.management.BrokerManager;
import com.redhat.amqx.management.Credentials;
import com.redhat.amqx.management.Resolver;
import org.apache.activemq.artemis.api.core.management.ActiveMQServerControl;
import org.apache.activemq.artemis.api.core.management.AddressControl;
import org.apache.activemq.artemis.api.core.management.DivertControl;
import org.apache.activemq.artemis.api.core.management.QueueControl;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.management.MalformedObjectNameException;
import java.io.IOException;
import java.util.*;


/**
 * Broker management options for Artemis/AMQ7 brokers.
 */

public class BrokerArtemisManager extends AbstractArtemisManager implements BrokerManager {
    // TODO update this class if possible
    public BrokerArtemisManager(String url, Credentials credentials, String brokerName, String host) throws IOException {
        super(url, credentials, brokerName, host);
    }


    @Override
    protected Resolver<ActiveMQServerControl, QueueControl, QueueControl, AddressControl, DivertControl> initializeResolver() {
        return new ArtemisResolver(mBeanServerConnection, getBrokerName());
    }

    @Override
    public void reload() throws MalformedObjectNameException {
        // TODO !?
//        ActiveMQServerControl serverControl = getResolver(ArtemisResolver.class).getBrokerView();
//        serverControl.resetAllMessageCounters();
    }

    @Override
    public void getTransportConnectors() throws Exception {
        ActiveMQServerControl serverControl = getResolver(ArtemisResolver.class).getBrokerView();
        formatter.printConvertedJson(serverControl.getConnectorsAsJSON());
        formatter.printConvertedJson(new JSONObject(serverControl.getConnectors()).toString());
        formatter.printConvertedJson(new JSONArray(serverControl.getAddressNames()).toString());
    }

    @Override
    public void getNetworkTopology() throws Exception {
        ActiveMQServerControl serverControl = getResolver(ArtemisResolver.class).getBrokerView();
        formatter.printConvertedJson(serverControl.listNetworkTopology());
    }

    @Override
    public void getSessions(String connectionId) throws Exception {
        ActiveMQServerControl serverControl = getResolver(ArtemisResolver.class).getBrokerView();
        if (connectionId == null) {
            formatter.printConvertedJson(serverControl.listAllSessionsAsJSON());
        } else {
            formatter.printConvertedJson(serverControl.listSessionsAsJSON(connectionId));
        }
    }

    @Override
    public BrokerType getBrokerType() {
        return BrokerType.ARTEMIS;
    }

    public void getAllBrokerDestinations() throws Exception {
        Map<String, Object> allDestinationsMap = new LinkedHashMap<>();

        allDestinationsMap.put("address", new JSONArray(getAddresses()));
        allDestinationsMap.put("queue", new JSONArray(getQueues().keySet()));
        allDestinationsMap.put("topic", new JSONArray(getTopics().keySet()));
        formatter.printConvertedJson(new JSONObject(allDestinationsMap).toString());
    }

    public void getAllBrokerDestinationsProperties() throws Exception {
        Map<String, Object> allDestinationsMap = new LinkedHashMap<>();
        Map<String, String> queues = getQueues();
        for (String queue : queues.keySet()) {
            allDestinationsMap.put(queue, getDestinationProperties(queues.get(queue), queue, NodeType.QUEUE));
        }

        Map<String, String> topics = getTopics();
        for (String topic : topics.keySet()) {
            allDestinationsMap.put(topic, getDestinationProperties(topics.get(topic), topic, NodeType.TOPIC));
        }

        for (String address : getUnboundAddresses()) {
            allDestinationsMap.put(address, getDestinationProperties(address, null, NodeType.ADDRESS));
        }

        formatter.printConvertedJson(new JSONObject(allDestinationsMap).toString());
    }

}
