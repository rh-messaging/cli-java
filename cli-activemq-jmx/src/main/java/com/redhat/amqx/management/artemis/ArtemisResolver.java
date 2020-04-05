package com.redhat.amqx.management.artemis;

import com.redhat.amqx.management.Resolver;
import org.apache.activemq.artemis.api.core.SimpleString;
import org.apache.activemq.artemis.api.core.management.*;
import org.apache.activemq.artemis.api.core.RoutingType;

import javax.management.MBeanServerConnection;
import javax.management.MBeanServerInvocationHandler;
import javax.management.ObjectName;

/**
 * Apache Artemis 1.1 management MBeans resolver class.
 */
public class ArtemisResolver implements Resolver<ActiveMQServerControl, QueueControl, QueueControl, AddressControl, DivertControl> {
    private final String DEFAULT_DOMAIN = "org.apache.activemq.artemis";
    private MBeanServerConnection mBeanServerConnection;
    private String brokerName;

    public ArtemisResolver(MBeanServerConnection mBeanServerConnection, String brokerName) {
        this.mBeanServerConnection = mBeanServerConnection;
        this.brokerName = brokerName;
    }

    @Override
    public String getBrokerName() {
        return brokerName;
    }

    @Override
    public ActiveMQServerControl getBrokerView() throws Exception {
        ObjectName objectName = ObjectNameBuilder.create(DEFAULT_DOMAIN, brokerName, true).getActiveMQServerObjectName();
        // 1.0   org.apache.activemq.artemis:module=Core,type=Server
        // 1.2   org.apache.activemq.artemis:type=Broker,brokerName="<broker-name>",module=Core,ServerType=Server";
        // 1.5.1 org.apache.activemq.artemis:type=Broker,brokerName="amq",serviceType=Address,name="queue-anycast2"
        // 2.0   org.apache.activemq.artemis:broker="<brokerName>",component=addresses,address="<addressName>",subcomponent=queues,routing-type="<routingType>",queue="<queueName>"
        objectName = ObjectNameBuilder.create(DEFAULT_DOMAIN, brokerName, true).getActiveMQServerObjectName();
        return MBeanServerInvocationHandler.newProxyInstance(mBeanServerConnection, objectName, ActiveMQServerControl.class, false);
    }

    @Override
    public QueueControl getQueueView(String addressName, String queueName) throws Exception {
        ObjectName objectName = ObjectNameBuilder.create(DEFAULT_DOMAIN, brokerName, true).getQueueObjectName(new SimpleString(addressName), new SimpleString(queueName), RoutingType.ANYCAST);
        return MBeanServerInvocationHandler.newProxyInstance(mBeanServerConnection, objectName, QueueControl.class, false);
    }

    @Override
    public QueueControl getTopicView(String addressName, String topicName) throws Exception {
//        if address doesn't add RoutingType.MULTICAST, it does not have any type
        ObjectName objectName = ObjectNameBuilder.create(DEFAULT_DOMAIN, brokerName, true).getQueueObjectName(new SimpleString(addressName), new SimpleString(topicName), RoutingType.MULTICAST);
        return MBeanServerInvocationHandler.newProxyInstance(mBeanServerConnection, objectName, QueueControl.class, false);
    }

    public AcceptorControl getAcceptorView(String acceptorName) throws Exception {
        ObjectName objectname = ObjectNameBuilder.create(DEFAULT_DOMAIN, brokerName, true).getAcceptorObjectName(acceptorName);
        return MBeanServerInvocationHandler.newProxyInstance(mBeanServerConnection, objectname, AcceptorControl.class, false);
    }

    public AddressControl getAddressView(String addressName) throws Exception {
        ObjectName objectname = ObjectNameBuilder.create(DEFAULT_DOMAIN, brokerName, true).getAddressObjectName(new SimpleString(addressName));
        return MBeanServerInvocationHandler.newProxyInstance(mBeanServerConnection, objectname, AddressControl.class, false);
    }

    public DivertControl getDivertView(String addressName, String divertName) throws Exception {
        ObjectName objectname = ObjectNameBuilder.create(DEFAULT_DOMAIN, brokerName, true).getDivertObjectName(divertName, addressName);
        return MBeanServerInvocationHandler.newProxyInstance(mBeanServerConnection, objectname, DivertControl.class, false);
    }

}
