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

package com.redhat.mqe.aoc;

import com.redhat.mqe.lib.ClientOptions;
import com.redhat.mqe.lib.ConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.naming.NamingException;

class AocConnectionManager extends ConnectionManager {
    private Logger LOG = LoggerFactory.getLogger(AocConnectionManager.class.getName());

    AocConnectionManager(ClientOptions clientOptions, String connectionFactory) {
        if (clientOptions.getOption(ClientOptions.USERNAME).hasParsedValue()) {
            username = clientOptions.getOption(ClientOptions.USERNAME).getValue();
        }
        if (clientOptions.getOption(ClientOptions.PASSWORD).hasParsedValue()) {
            password = clientOptions.getOption(ClientOptions.PASSWORD).getValue();
        }
        try {
            setInitialContext(connectionFactory);
            factory = (ConnectionFactory) initialContext.lookup(amqConnectionFactoryJNDI);

            if (clientOptions.getOption(ClientOptions.DESTINATION_TYPE).getValue().equals(TOPIC_OBJECT)) {
                destination = createTopic(clientOptions.getOption(ClientOptions.ADDRESS).getValue());
            } else if (clientOptions.getOption(ClientOptions.DESTINATION_TYPE).getValue().equals(QUEUE_OBJECT)) {
                destination = createQueue(clientOptions.getOption(ClientOptions.ADDRESS).getValue());
            } else {
                // reserved for future other destination types
                LOG.warn("Not sure what type of Destination to create. Falling back to Destination");
                destination = (Destination) initialContext.lookup(this.queueOrTopic);
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

            connection.setExceptionListener(new MessagingExceptionListener());
        } catch (NamingException | JMSException e) {
            LOG.error(e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
