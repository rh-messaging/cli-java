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
 *
 * TODO: clarify purpose of this class, now it looks like a factory for factory to me
 * This class initializes attributes connection and destination,
 * which will be pulled by the client.
 *
 * session, creates queues and topics on that session and ?? sends messages?
 */
abstract public class ConnectionManager {
    protected ConnectionFactory factory;
    protected InitialContext initialContext;
    private Properties properties;
    protected Destination destination;
    protected Connection connection;
    protected String amqConnectionFactoryJNDI = "amqFactory";
    protected String connectionFactory = "amqFactory"; // default amqp://localhost:5672
    protected String password;
    protected String queueOrTopic = "amqQueue";
    protected String username;
    protected static final String QUEUE_OBJECT = "javax.jms.Queue";
    protected static final String TOPIC_OBJECT = "javax.jms.Topic";
    /**
        private static final String AMQ_INITIAL_CONTEXT = "org.apache.qpid.jms.jndi.JmsInitialContextFactory";
        private static final String QPID_INITIAL_CONTEXT = "org.apache.qpid.jndi.PropertiesFileInitialContextFactory";
        private static final String WIRE_INITIAL_CONTEXT = "org.apache.activemq.jndi.ActiveMQInitialContextFactory";
        private static final String ARTEMIS_INITIAL_CONTEXT = "org.apache.activemq.artemis.jndi.ActiveMQInitialContextFactory";
     */

    private static final String EXTERNAL_JNDI_PROPERTY = "aoc7.jndi";
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

    /**
     * @param connectionFactory - often referred to as broker url
     */
    void setConnectionFactory(String connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    /**
     * @param queueOrTopic - destination can be either queue or topic
     */
    void setDestinationName(String queueOrTopic) {
        this.queueOrTopic = queueOrTopic;
    }

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

    /**
     * Creates a destination for a specified name.
     *
     * @param destination for which destination is to be created.
     * @return created Destination object
     */
    Destination createDestination(String destination) {
        return (Destination) createJMSProviderObject("destination", destination);
    }

    /**
     * Create queue object
     *
     * @param queueName name of the queue to be created
     * @return created Queue object
     */
    protected Queue createQueue(String queueName) {
        return (Queue) createJMSProviderObject("queue", queueName);
    }

    /**
     * Create topic object
     *
     * @param topicName name of the topic to be created
     * @return created Topic object
     */
    protected Topic createTopic(String topicName) {
        return (Topic) createJMSProviderObject("topic", topicName);
    }

    /**
     * Creates an object using qpid/amq initial context factory.
     *
     * @param className can be any of the qpid/amq supported JNDI properties:
     *                  connectionfactory, queue, topic, destination.
     * @param address   of the connection or node to create.
     */
    private Object createJMSProviderObject(String className, String address) {
    /* Name of the object is the same as class of the object */
        String name = className;
        properties.setProperty(className + "." + name, address);

        Object jmsProviderObject = null;
        try {
            Context context = new InitialContext(properties);
            jmsProviderObject = context.lookup(name);
            context.close();
        } catch (NamingException e) {
            e.printStackTrace();
        }
        return jmsProviderObject;
    }

    protected void setInitialContext(String userConnectionFactory) {
        properties = new Properties();
        try {
            String jndiFilePath;
            if ((jndiFilePath = System.getProperty(EXTERNAL_JNDI_PROPERTY)) != null) {
                // load property file from an absolute path to the file
                properties.load(new FileInputStream(jndiFilePath));
            } else {
                ClassLoader classloader = Thread.currentThread().getContextClassLoader();
                String JNDIFile;
                if (CoreClient.isAMQClient()) {
                    JNDIFile = "QpidJmsJNDI.properties";
                } else if (CoreClient.isQpidClient()) {
                    JNDIFile = "QpidLegacyJNDI.properties";
                } else {
                    JNDIFile = "JAMQ6JNDI.properties";
                }
                LOG.trace("Using "+ JNDIFile);
                // fallback to use resources/ArtemisJNDI.properties file
                properties.load(classloader.getResourceAsStream(JNDIFile));
                LOG.trace(properties.toString());
            }
            LOG.trace(userConnectionFactory);
            if (userConnectionFactory.contains("://")) {
                // override connectionFactory by this option in jndi/properties
                properties.setProperty(Context.PROVIDER_URL, userConnectionFactory);
                properties.setProperty("connectionFactory." + amqConnectionFactoryJNDI, userConnectionFactory);
            }
            initialContext = new InitialContext(properties);
        } catch (NamingException | IOException e) {
            e.printStackTrace();
        }
    }
}
