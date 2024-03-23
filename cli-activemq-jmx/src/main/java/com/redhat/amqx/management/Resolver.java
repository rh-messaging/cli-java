package com.redhat.amqx.management;

import javax.management.ObjectName;

/**
 * Interface for resolving view management
 * objects like queues, topics, broker information.
 * For A-MQ 6.2 XViewMBeans are used. {Queue,Topic,Broker,Destination}ViewMBean.
 * For Artemis (A-MQ 7) ? are used.
 */
public interface Resolver<T, R, S, U, V> {

    String getBrokerName();

    T getBrokerView() throws Exception;

    R getQueueView(String addressName, String queueName) throws Exception;

    S getTopicView(String addressName, String topicName) throws Exception;

    U getAddressView(String addressName) throws Exception;

    ObjectName getAddressObjectName(String addressName) throws Exception;

    V getDivertView(String addressName, String divertName) throws Exception;

}
