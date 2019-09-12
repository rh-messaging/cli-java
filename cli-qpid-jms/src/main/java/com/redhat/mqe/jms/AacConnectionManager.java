/*
 * Copyright (c) 2017 Red Hat, Inc.
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.redhat.mqe.jms;

import com.redhat.mqe.lib.ClientOptions;
import com.redhat.mqe.lib.ConnectionManager;
import com.redhat.mqe.lib.MessagingExceptionListener;
import org.apache.qpid.jms.JmsConnectionFactory;
import org.apache.qpid.jms.JmsQueue;
import org.apache.qpid.jms.JmsTopic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.Topic;

public class AacConnectionManager extends ConnectionManager {
    static final String QUEUE_OBJECT = "javax.jms.Queue";
    static final String TOPIC_OBJECT = "javax.jms.Topic";
    private Logger LOG = LoggerFactory.getLogger(AacConnectionManager.class.getName());

    AacConnectionManager(String serviceName, ClientOptions clientOptions, String brokerUrl) {
        if (Boolean.parseBoolean(clientOptions.getOption(ClientOptions.TRACE_MESSAGES).getValue())) {
            Tracing.register(serviceName);
        }

        String username = null;
        String password = null;

        if (clientOptions.getOption(AacClientOptions.USERNAME).hasParsedValue()) {
            username = clientOptions.getOption(AacClientOptions.USERNAME).getValue();
        }
        if (clientOptions.getOption(AacClientOptions.PASSWORD).hasParsedValue()) {
            password = clientOptions.getOption(AacClientOptions.PASSWORD).getValue();
        }

        try {
            factory = new JmsConnectionFactory(brokerUrl);

            if (clientOptions.getOption(AacClientOptions.DESTINATION_TYPE).getValue().equals(TOPIC_OBJECT)) {
                destination = createTopic(clientOptions.getOption(AacClientOptions.ADDRESS).getValue());
            } else if (clientOptions.getOption(AacClientOptions.DESTINATION_TYPE).getValue().equals(QUEUE_OBJECT)) {
                destination = createQueue(clientOptions.getOption(AacClientOptions.ADDRESS).getValue());
            } else {
                throw new RuntimeException("Not sure what type of Destination to create.");
            }

            LOG.debug("Connection=" + brokerUrl);
            LOG.trace("Destination=" + destination);
            if (clientOptions.getOption(AacClientOptions.BROKER_URI).hasParsedValue()
                || (username == null && password == null)) {
                connection = factory.createConnection();
            } else {
                LOG.trace("Using credentials " + username + ":" + password);
                connection = factory.createConnection(username, password);
            }

            connection.setExceptionListener(new MessagingExceptionListener());
        } catch (JMSException e) {
            LOG.error(e.getMessage());
            e.printStackTrace();
        }
    }

    Connection getConnection() {
        return this.connection;
    }

    /**
     * Create queue object
     *
     * @param queueName name of the queue to be created
     * @return created Queue object
     */
    @Override
    protected Queue createQueue(String queueName) {
        return new JmsQueue(queueName);
    }

    /**
     * Create topic object
     *
     * @param topicName name of the topic to be created
     * @return created Topic object
     */
    @Override
    protected Topic createTopic(String topicName) {
        return new JmsTopic(topicName);
    }
}
