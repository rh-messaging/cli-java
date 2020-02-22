package com.redhat.amqx.management.activemq;

import com.redhat.amqx.management.Resolver;
import org.apache.activemq.broker.jmx.BrokerViewMBean;
import org.apache.activemq.broker.jmx.QueueViewMBean;
import org.apache.activemq.broker.jmx.TopicViewMBean;

import javax.management.JMX;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;

/**
 * ActiveMQ (or A-MQ) name resolver
 */
public class ActiveMQResolver implements Resolver<BrokerViewMBean, QueueViewMBean, TopicViewMBean, QueueViewMBean, QueueViewMBean> {
    private static final String DEFAULT_DOMAIN = "org.apache.activemq";
    private String brokerDomain;
    private String brokerName;
    private MBeanServerConnection mBeanServerConnection;

    public ActiveMQResolver(MBeanServerConnection mBeanServerConnection, String brokerName) {
        this.mBeanServerConnection = mBeanServerConnection;
        this.brokerName = brokerName;
        this.brokerDomain = DEFAULT_DOMAIN + ":type=Broker,brokerName=" + brokerName;
    }

    @Override
    public String getBrokerName() {
        return brokerName;
    }


    @Override
    public BrokerViewMBean getBrokerView() throws Exception {
        ObjectName mbeanName = new ObjectName(brokerDomain);
        return JMX.newMBeanProxy(mBeanServerConnection, mbeanName, BrokerViewMBean.class, true);
    }


    @Override
    public QueueViewMBean getQueueView(String adressName, String destinationName) throws Exception {
        ObjectName mbeanName = new ObjectName(brokerDomain
                + ",destinationType=Queue,destinationName=" + destinationName);
        return JMX.newMBeanProxy(mBeanServerConnection, mbeanName, QueueViewMBean.class, true);
    }

    @Override
    public TopicViewMBean getTopicView(String adressName, String topicName) throws Exception {
        ObjectName mbeanName = new ObjectName(brokerDomain +
                ",destinationType=Topic,destinationName=" + topicName);
        return JMX.newMBeanProxy(mBeanServerConnection, mbeanName, TopicViewMBean.class, true);
    }

    @Override
    public QueueViewMBean getAddressView(String queueName) throws Exception {
        System.err.println("Does not exist for AMQ!");
        return null;
    }

    @Override
    public QueueViewMBean getDivertView(String addressName, String divertName) throws Exception {
        System.err.println("Does not exist for AMQ!");
        return null;
    }
}
