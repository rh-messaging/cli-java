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

import com.redhat.mqe.lib.JmsMessagingException;
import com.redhat.mqe.lib.MessageFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.*;
import java.util.ArrayList;
import java.util.List;

/**
 * ConnectorClient is an Messaging QE client, which is able to
 * create connections to provided brokers. Connector can create multiple
 * Connection, Session, MessageProducer, MessageConsumer and TemporaryQueue objects.
 * It can wait for given time
 */
public class ConnectorClient extends CoreClient {

    private ConnectorOptions connectorOptions;
    private static int connectionsOpened = 0;
    private static List<Throwable> exceptions = new ArrayList<>();
    private static Logger LOG_CLEAN = LoggerFactory.getLogger(MessageFormatter.class);

    ConnectorClient(String[] arguments) {
        connectorOptions = new ConnectorOptions();
        ClientOptionManager.applyClientArguments(connectorOptions, arguments);
    }


    @Override
    void startClient() {
        // start all connections
        createConnectionObjects(connectorOptions.getOption(ClientOptions.OBJ_CTRL).getDefaultValue());
        if (exceptions.isEmpty()) {
            startConnections();
        }
        int count = Integer.parseInt((this.getClientOptions().getOption(ClientOptions.COUNT).getValue()));
        LOG_CLEAN.info(connectionsOpened + " " + exceptions.size() + " " + count);
        for (Throwable t : exceptions) {
            LOG.error(t.getMessage(), t.getCause());
        }
        closeConnObjects(this,
            Double.parseDouble(this.getClientOptions().getOption(ClientOptions.CLOSE_SLEEP).getValue()));
        if (exceptions.size() > 0) {
            System.exit(exceptions.size());
        }
    }

    /**
     * Start connections created by ::createConnectionObject()
     */
    private void startConnections() {
        for (Connection connection : this.getConnections()) {
            try {
                connection.start();
                connectionsOpened++;
            } catch (JMSException e) {
                exceptions.add(new JmsMessagingException("Failed to start a connection.\n" + e.getMessage(), e.getCause()));
            }
        }
    }

    /**
     * Create given number of Connection, Session, MessageProducer,
     * MessageConsumer, TemporaryQueue objects.
     *
     * @param objCtrl specifies which objects are to be created
     */
    private void createConnectionObjects(String objCtrl) {
        if (connectorOptions.getOption(ClientOptions.ADDRESS).hasParsedValue()) {
            objCtrl = "CESR";
        } else if (connectorOptions.getOption(ClientOptions.OBJ_CTRL).hasParsedValue()) {
            objCtrl = connectorOptions.getOption(ClientOptions.OBJ_CTRL).getValue().toUpperCase();
        }
        int count = Integer.parseInt(connectorOptions.getOption(ClientOptions.COUNT).getValue());
        try {
            // create N Connections
            for (int i = 0; i < count; i++) {
                this.createConnection(connectorOptions);
            }

            // create N sEssions
            if (objCtrl.contains("E")) {
                for (Connection connection : getConnections()) {
                    createSession(connectorOptions, connection, false);
                }

                Destination destination = null;
                if (objCtrl.contains("S") || objCtrl.contains("R")) {
                    destination = this.getDestination();
                }
                // create N Senders (MessageProducers)
                if (objCtrl.contains("S")) {
                    for (Session session : getSessions()) {
                        MessageProducer producer = session.createProducer(destination);
                        addMessageProducer(producer);
                    }
                    // create N Receivers (MessageConsumers)
                }
                if (objCtrl.contains("R")) {
                    for (Session session : getSessions()) {
                        MessageConsumer consumer = session.createConsumer(destination);
                        addMessageConsumer(consumer);
                    }
                }
                // create temporary queue (the only queue we can create with JMSSession)
                if (objCtrl.contains("Q")) {
                    int qCount = Integer.parseInt(connectorOptions.getOption(ClientOptions.Q_COUNT).getValue());
                    if (qCount > count) {
                        qCount = count;
                    }
                    for (Session session : getSessions()) {
                        if (qCount > 0) {
                            Queue queue = session.createTemporaryQueue();
                            addQueue(queue);
                            qCount--;
                        } else {
                            break;
                        }
                    }
                }
            }

            if (LOG.isTraceEnabled()) {
                int conns = (getConnections() == null) ? 0 : getConnections().size();
                int sesss = (getSessions() == null) ? 0 : getSessions().size();
                int sends = (getProducers() == null) ? 0 : getProducers().size();
                int reces = (getConsumers() == null) ? 0 : getConsumers().size();
                int queues = (getQueues() == null) ? 0 : getQueues().size();
                LOG.trace("\tC={}\tE={}\tS={}\tR={}\tQ={}", conns, sesss, sends, reces, queues);
            }
        } catch (JMSException e) {
            exceptions.add(new JmsMessagingException("Failed to create 'obj-ctrl.\n" + e.getMessage(), e.getCause()));
        }
    }

    @Override
    ClientOptions getClientOptions() {
        return connectorOptions;
    }
}
