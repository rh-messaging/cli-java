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

package com.redhat.mqe.lib;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.*;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Subclasses are responsible for assigning to attributes
 * factory, initialContext
 * <p>
 * TODO: clarify purpose of this class, now it looks like a factory for factory to me
 * This class initializes attributes connection and destination,
 * which will be pulled by the client.
 * <p>
 * session, creates queues and topics on that session and ?? sends messages?
 */
abstract public class ConnectionManager {
    protected ConnectionFactory factory;
    protected Connection connection;
    protected Destination destination;
    protected String password;
    protected String username;
    protected static final String QUEUE_OBJECT = "javax.jms.Queue";
    protected static final String TOPIC_OBJECT = "javax.jms.Topic";
    /**
     * private static final String AMQ_INITIAL_CONTEXT = "org.apache.qpid.jms.jndi.JmsInitialContextFactory";
     * private static final String QPID_INITIAL_CONTEXT = "org.apache.qpid.jndi.PropertiesFileInitialContextFactory";
     * private static final String WIRE_INITIAL_CONTEXT = "org.apache.activemq.jndi.ActiveMQInitialContextFactory";
     * private static final String ARTEMIS_INITIAL_CONTEXT = "org.apache.activemq.artemis.jndi.ActiveMQInitialContextFactory";
     */

    private Logger LOG = LoggerFactory.getLogger(ConnectionManager.class.getName());

    public ConnectionManager() {
        // FIXME(jdanek): empty constructor, before I change the hierarchy
    }

    Connection getConnection() {
        return this.connection;
    }

    Destination getDestination() {
        return destination;
    }

    protected abstract Queue createQueue(String queueName);

    protected abstract Topic createTopic(String topicName);

    /**
     * MessagingExceptionListener is created for each connection made.
     */
    public class MessagingExceptionListener implements ExceptionListener {
        @Override
        public void onException(JMSException e) {
            LOG.error("ExceptionListener error detected! \n{}\n{}", e.getMessage(), e.getCause());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
