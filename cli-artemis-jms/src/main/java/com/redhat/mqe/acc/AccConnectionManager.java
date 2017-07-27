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

package com.redhat.mqe.acc;

import com.redhat.mqe.lib.ClientOptions;
import com.redhat.mqe.lib.ConnectionManager;
import org.apache.activemq.artemis.api.core.ActiveMQDisconnectedException;
import org.apache.activemq.artemis.api.core.ActiveMQNotConnectedException;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.apache.activemq.artemis.jms.client.ActiveMQQueue;
import org.apache.activemq.artemis.jms.client.ActiveMQTopic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.Topic;

public class AccConnectionManager extends ConnectionManager {
    // TODO remove initialcontext from the superclass
    private Logger LOG = LoggerFactory.getLogger(AccConnectionManager.class.getName());

    AccConnectionManager(final ClientOptions clientOptions, String brokerUrl) {
        super();
        if (clientOptions.getOption(ClientOptions.USERNAME).hasParsedValue()) {
            username = clientOptions.getOption(ClientOptions.USERNAME).getValue();
        }
        if (clientOptions.getOption(ClientOptions.PASSWORD).hasParsedValue()) {
            password = clientOptions.getOption(ClientOptions.PASSWORD).getValue();
        }

        // GOTCHA: this is more or less unrelated to initialContext field
//        setInitialContext(brokerUrl);
        factory = new ActiveMQConnectionFactory(brokerUrl);

        try {
            if (clientOptions.getOption(ClientOptions.DESTINATION_TYPE).getValue().equals(TOPIC_OBJECT)) {
                destination = createTopic(clientOptions.getOption(ClientOptions.ADDRESS).getValue());
            } else if (clientOptions.getOption(ClientOptions.DESTINATION_TYPE).getValue().equals(QUEUE_OBJECT)) {
                destination = createQueue(clientOptions.getOption(ClientOptions.ADDRESS).getValue());
            } else {
                // reserved for future other destination types
                LOG.warn("Not sure what type of Destination to create. Falling back to Destination");
                throw new RuntimeException("WTF?");
////                destination = (Destination) initialContext.lookup(this.queueOrTopic);
            }

            LOG.debug("Connection=" + connectionFactory);
            LOG.trace("Destination=" + destination);
            if (clientOptions.getOption(ClientOptions.BROKER_URI).hasParsedValue()
                    || (username == null && password == null)) {
//          || CoreClient.isAMQClient()) { this will work for Qpid JMS AMQP Client as well, but we will be nicer
                connection = factory.createConnection();
            } else {
                LOG.trace("Using credentials " + username + ":" + password);
                connection = factory.createConnection(username, password);
            }

            final MessagingExceptionListener exceptionListener = new MessagingExceptionListener();
            connection.setExceptionListener(new ExceptionListener() {
                @Override
                public void onException(JMSException exception) {
                    if (clientOptions.getOption(ClientOptions.CON_RECONNECT).getValue().matches("[tT]rue") &&
                            (exception.getCause() instanceof ActiveMQDisconnectedException ||
                            exception.getCause() instanceof ActiveMQNotConnectedException)) {
                        return;
                    }
                    exceptionListener.onException(exception);
                }
            });
        } catch (JMSException e) {
            LOG.error(e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    @Override
    protected Queue createQueue(String queueName) {
        return new ActiveMQQueue(queueName);
    }

    @Override
    protected Topic createTopic(String topicName) {
        return new ActiveMQTopic(topicName);
    }
}
