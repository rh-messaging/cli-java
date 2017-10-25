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
import com.redhat.mqe.lib.CoreClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.*;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class AacConnectionManager extends ConnectionManager {
    private Context context;
    private String customConnectionFactory = "connectionfactory.amqFactory";
    private String customQueue = "queue.amqQueue";
    private String customTopic = "topic.amqTopic";
    private String destinationQueue = "amqQueue";
    private String destinationTopic = "amqTopic";
    String connectionFactory = "amqFactory"; // default amqp://localhost:5672
    String password;
    String queueOrTopic = "amqQueue";
    String username;
    static final String QUEUE_OBJECT = "javax.jms.Queue";
    static final String TOPIC_OBJECT = "javax.jms.Topic";
    private static final String AMQ_INITIAL_CONTEXT = "org.apache.qpid.jms.jndi.JmsInitialContextFactory";
    private static final String QPID_INITIAL_CONTEXT = "org.apache.qpid.jndi.PropertiesFileInitialContextFactory";

    private static final String EXTERNAL_JNDI_PROPERTY = "aac1.jndi";
    private Logger LOG = LoggerFactory.getLogger(AacConnectionManager.class.getName());

    AacConnectionManager(ClientOptions clientOptions, String connectionFactory) {
        if (clientOptions.getOption(AacClientOptions.USERNAME).hasParsedValue()) {
            username = clientOptions.getOption(AacClientOptions.USERNAME).getValue();
        }
        if (clientOptions.getOption(AacClientOptions.PASSWORD).hasParsedValue()) {
            password = clientOptions.getOption(AacClientOptions.PASSWORD).getValue();
        }
        try {
            Properties props = new Properties();
            String jndiFilePath;
            if ((jndiFilePath = System.getProperty(EXTERNAL_JNDI_PROPERTY)) != null) {
                // load property file from an absolute path to the file
                try (FileInputStream fileInputStream = new FileInputStream(new File(jndiFilePath))) {
                    props.load(fileInputStream);
                }
            } else {
                // fallback to use resources/jndi.properties file
                jndiFilePath = "/jndi.properties";
                try (InputStream inputStream = this.getClass().getResourceAsStream(jndiFilePath)) {
                    props.load(inputStream);
                }
            }
            if (connectionFactory.contains("://")) {
                // override connectionFactory by this option in jndi/properties
                props.setProperty(customConnectionFactory, connectionFactory);
            }
      /* TODO if external JNDI is supported, how to read/create Provider objects from it?
        if (externalJNDI) {
          load properties, search for queue/topic property & use it
        } else { */
            context = new InitialContext(props);
            factory = (ConnectionFactory) context.lookup(this.connectionFactory);

            if (clientOptions.getOption(AacClientOptions.DESTINATION_TYPE).getValue().equals(TOPIC_OBJECT)) {
                destination = createTopic(clientOptions.getOption(AacClientOptions.ADDRESS).getValue());
            } else if (clientOptions.getOption(AacClientOptions.DESTINATION_TYPE).getValue().equals(QUEUE_OBJECT)) {
                destination = createQueue(clientOptions.getOption(AacClientOptions.ADDRESS).getValue());
            } else {
                // reserved for future other destination types
                LOG.warn("Not sure what type of Destination to create. Falling back to Destination");
                destination = (Destination) context.lookup(this.queueOrTopic);
            }

            LOG.debug("Connection=" + connectionFactory);
            LOG.trace("Destination=" + destination);
            if (clientOptions.getOption(AacClientOptions.BROKER_URI).hasParsedValue()
                || (username == null && password == null)) {
//          || CoreClient.isAMQClient()) { this will work for Qpid JMS AMQP Client as well, but we will be nicer
                connection = factory.createConnection();
            } else {
                LOG.trace("Using credentials " + username + ":" + password);
                connection = factory.createConnection(username, password);
            }

            connection.setExceptionListener(new MessagingExceptionListener());
        } catch (IOException | NamingException | JMSException e) {
            LOG.error(e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
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
    class MessagingExceptionListener implements ExceptionListener {
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
    /* Eventually maybe needed to be redefined */
        final String initialContext;
        if (CoreClient.isQpidClient()) {
            initialContext = QPID_INITIAL_CONTEXT;
        } else {
            initialContext = AMQ_INITIAL_CONTEXT;
        }
        Properties properties = new Properties();
    /* Name of the object is the same as class of the object */
        String name = className;
        properties.setProperty("java.naming.factory.initial", initialContext);
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
