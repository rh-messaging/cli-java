package com.redhat.amqx.management.artemis;

import com.redhat.amqx.formatters.Formatter;
import com.redhat.amqx.formatters.PythonFormatter;
import com.redhat.amqx.main.BrokerType;
import com.redhat.amqx.main.NodeType;
import com.redhat.amqx.management.ObjectReader;
import com.redhat.amqx.management.AbstractConnectionManager;
import com.redhat.amqx.management.Credentials;
import com.redhat.amqx.management.Resolver;
import org.apache.activemq.artemis.api.core.management.ActiveMQServerControl;
import org.apache.activemq.artemis.api.core.management.AddressControl;
import org.apache.activemq.artemis.api.core.management.DivertControl;
import org.apache.activemq.artemis.api.core.management.QueueControl;
//import org.apache.activemq.artemis.api.jms.management.JMSServerControl;
//import org.apache.activemq.artemis.api.jms.management.TopicControl;
import org.apache.activemq.artemis.api.core.RoutingType;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.ObjectName;
import java.io.IOException;
import java.util.*;

/**
 * Shared functioned of address/queue/topic/broker for Artemis Manager.
 */
// TODO add here annotations instead of using of BrokerType
public class AbstractArtemisManager extends AbstractConnectionManager {
    protected static final Logger logger = LoggerFactory.getLogger(AbstractArtemisManager.class);
    private static final ObjectReader objectReader = new ArtemisObjectReader();
    protected final Formatter formatter = new PythonFormatter();

    public AbstractArtemisManager(String url, Credentials credentials, String brokerName, String hostname) throws IOException {
        super(url, credentials, brokerName, BrokerType.ARTEMIS, hostname);
    }

    @Override
    protected Resolver<?, ?, ?, ?, ?> initializeResolver() {
        return new ArtemisResolver(mBeanServerConnection, getBrokerName());
    }

    /**
     * Get Artemis ActiveMQServerControl MBean.
     *
     * @return ActiveMQServerControl MBean
     * @throws Exception
     */
    protected ActiveMQServerControl getServerControlMBean() throws Exception {
        return getResolver(ArtemisResolver.class).getBrokerView();
    }

    protected QueueControl getQueueControlMBean(String addressName, String queueName) throws Exception {
        return getResolver(ArtemisResolver.class).getQueueView(addressName, queueName);
    }

    protected QueueControl getTopicControlMBean(String addressName, String queueName) throws Exception {
        return getResolver(ArtemisResolver.class).getTopicView(addressName, queueName);
    }

//    protected JMSServerControl getJMSServerControlMBean() throws Exception {
//        return getResolver(ArtemisResolver.class).getJMSServerControlView();
//    }

    /**
     * Get topics MULTICAST routing type "queues" from Artemis broker
     *
     * @return Map of multicast addresses
     * @throws Exception
     */
    protected Map<String, String> getTopics() throws Exception {
        Map<String, String> topics = new HashMap<>();

        for (String addr : getAddresses()) {
            AddressControl ac = (AddressControl) getResolver().getAddressView(addr);
            for (String queue : ac.getQueueNames()) {
                if (getServerControlMBean().getAddressInfo(addr).contains(RoutingType.MULTICAST.toString())) {
                    topics.put(queue, addr);
                }
            }
        }
        return topics;
    }

    /**
     * Get queues ANYCAST routing type "queues" from Artemis broker
     *
     * @return Map of anycast addresses
     * @throws Exception
     */
    protected Map<String, String> getQueues() throws Exception {
        Map<String, String> queues = new HashMap<>();

        for (String addr : getAddresses()) {
            AddressControl ac = (AddressControl) getResolver().getAddressView(addr);
            for (String queue : ac.getQueueNames()) {
                if (getServerControlMBean().getAddressInfo(addr).contains(RoutingType.ANYCAST.toString())) {
                    queues.put(queue, addr);
                }
            }
        }
        return queues;
    }

    /**
     * Get addresses from Artemis broker
     *
     * @return List of addresses
     * @throws Exception
     */
    protected List<String> getAddresses() throws Exception {
        return new LinkedList<String>(Arrays.asList(getServerControlMBean().getAddressNames()));
    }

    /**
     * Get addresses from Artemis broker which are not bound to any queue.
     *
     * @return List of addresses
     * @throws Exception
     */
    protected List<String> getUnboundAddresses() throws Exception {
        List<String> addresses = getAddresses();

        logger.debug(addresses.toString());
        Set<Map.Entry<String,String>> allQueues = getQueues().entrySet();
        allQueues.addAll(getTopics().entrySet());
        logger.debug(allQueues.toString());  // queue=address

        for (Map.Entry<String, String> entry : allQueues) {
            addresses.remove(entry.getValue());
        }

        return addresses;
    }

    private String getAddressNameFromQueue(String queueAdddressInfo) {
        int nameIndex = queueAdddressInfo.indexOf("name=");
        int rtIndex = queueAdddressInfo.indexOf(", routingTypes=");
        return queueAdddressInfo.substring(nameIndex + 5, rtIndex);
    }


    /**
     * Get a mapping of properties for queue or topic (and Address object if names are same).
     * JMS Topic object has properties from Address (queue) and JMSTopic objects.
     * JMS Queue object has properties from Address (queue) and Queue objects.
     * Note: JMSQueueControl has very similar 'get-abilities' as core.QueueControl. Let's keep it simple for now
     * and use QueueControl only.
     *
     * @param destinationName name of the destination
     * @param nodeType        queue or topic
     * @return map of property-value of all possible getters and isBooleans.
     */
    public Map<String, Object> getDestinationProperties(String addressName, String destinationName, NodeType nodeType) throws Exception {
        Map<String, Object> propertiesMap = new LinkedHashMap<>();
        List<String> excludeMethods = new ArrayList<>();
        // excluding JSON outputs for now
        excludeMethods.add("isProxyClass");



        if (!nodeType.equals(NodeType.ADDRESS)) {
            if (nodeType.equals(NodeType.TOPIC)) {
                QueueControl topicControl = (QueueControl) getResolver().getTopicView(addressName, destinationName);
                propertiesMap.putAll(objectReader.getObjectProperties(topicControl, excludeMethods));
                //            destinationName = "jms.topic." + destinationName;
            }
            if (nodeType.equals(NodeType.QUEUE)) {
                QueueControl queueControl = (QueueControl) getResolver().getQueueView(addressName, destinationName);
                propertiesMap.putAll(objectReader.getObjectProperties(queueControl, excludeMethods));
            }
            if (nodeType.equals(NodeType.DIVERT)) {
                DivertControl divertControl = (DivertControl) getResolver().getDivertView(addressName, destinationName);
                propertiesMap.putAll(objectReader.getObjectProperties(divertControl, excludeMethods));
            }
        }

        ObjectName addressObjectName = getResolver().getAddressObjectName(addressName);
        propertiesMap.putAll(objectReader.getRawObjectProperties(mBeanServerConnection, addressObjectName, excludeMethods));
        propertiesMap.put("address-settings", new JSONObject(getServerControlMBean().getAddressSettingsAsJSON(addressName)));
        return propertiesMap;
    }
}
