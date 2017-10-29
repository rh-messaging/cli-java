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

import javax.jms.*;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

class AocConnectionManager extends ConnectionManager {
    private static final String EXTERNAL_JNDI_PROPERTY = "aoc7.jndi";
    protected InitialContext initialContext;
    private String queueOrTopic = "amqQueue";
    private String amqConnectionFactoryJNDI = "amqFactory";
    private Properties properties;
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
            if (clientOptions.getOption(ClientOptions.BROKER_URI).hasParsedValue() || (username == null && password == null)) {
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

    protected void setInitialContext(String userConnectionFactory) {
        properties = new Properties();
        try {
            String jndiFilePath;
            if ((jndiFilePath = System.getProperty(EXTERNAL_JNDI_PROPERTY)) != null) {
                // load property file from an absolute path to the file
                try (FileInputStream stream = new FileInputStream(jndiFilePath)) {
                    properties.load(stream);
                }
            } else {
                ClassLoader classloader = Thread.currentThread().getContextClassLoader();
                String JNDIFile = "JAMQ6JNDI.properties";
                LOG.trace("Using " + JNDIFile);
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

    /**
     * Create queue object
     *
     * @param queueName name of the queue to be created
     * @return created Queue object
     */
    @Override
    protected Queue createQueue(String queueName) {
        return (Queue) createJMSProviderObject("queue", queueName);
    }

    /**
     * Create topic object
     *
     * @param topicName name of the topic to be created
     * @return created Topic object
     */
    @Override
    protected Topic createTopic(String topicName) {
        return (Topic) createJMSProviderObject("topic", topicName);
    }

    /**
     * Creates an object using qpid/amq initial context factory.
     *
     * @param className can be any of the qpid/amq supported JNDI properties:
     *                  connectionFactory, queue, topic, destination.
     * @param address   of the connection or node to create.
     */
    protected Object createJMSProviderObject(String className, String address) {
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
}
