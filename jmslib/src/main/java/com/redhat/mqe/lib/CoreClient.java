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
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//import javax.ws.rs.core.UriBuilder;  // helpful class that would sadly require bringing in additional dependency


/**
 * Core implementation of creating various connections to brokers using clients.
 */
public abstract class CoreClient {
    protected static Logger LOG = LoggerFactory.getLogger(CoreClient.class);
    private ConnectionManager connectionManager;
    private static final Map<String, Integer> SESSION_ACK_MAP = new HashMap<>(5);

    static {
//    SESSION_ACK_MAP.put("transacted", Session.SESSION_TRANSACTED); // This is handled by TRANSACTED option
        SESSION_ACK_MAP.put("auto", Session.AUTO_ACKNOWLEDGE);
        SESSION_ACK_MAP.put("client", Session.CLIENT_ACKNOWLEDGE);
        SESSION_ACK_MAP.put("dups_ok", Session.DUPS_OK_ACKNOWLEDGE);
//    SESSION_ACK_MAP.put("individual", Session.INDIVIDUAL_ACKNOWLEDGE); // ActiveMQSpecific?
    }

    private List<Connection> connections;
    private List<Session> sessions;
    private List<MessageProducer> messageProducers;
    private List<MessageConsumer> messageConsumers;
    private List<Queue> queues;

    protected ConnectionManagerFactory connectionManagerFactory;
    protected JmsMessageFormatter jmsMessageFormatter;

    /**
     * Method starts the given client. Serves as entry point.
     */
    public abstract void startClient() throws Exception;

    /**
     * Create @Connection for given client from provided client options.
     * By default, we prefer to use brokerUrl to not set up anything.
     * Also, no brokerUrl can be provided, but we need all the other connection
     * options to create @Connection.
     *
     * @param clientOptions options of given client
     * @return newly created Connection from provided client options
     */
    public Connection createConnection(ClientOptions clientOptions) {
//    Map<String, Option> updatedOptions = clientOptions.getUpdatedOptions();
        String brokerUrl;
        if (clientOptions.getOption(ClientOptions.BROKER_URI).hasParsedValue()) {
            // Use the whole provided broker-url string with options
            brokerUrl = clientOptions.getOption(ClientOptions.BROKER_URI).getValue();
        } else {
            // Use only protocol,credentials,host and port
            brokerUrl = clientOptions.getOption(ClientOptions.BROKER).getValue();

            if (clientOptions.getOption(ClientOptions.CON_RECONNECT_URL).hasParsedValue()) {
                String reconnectBrokers = clientOptions.getOption(ClientOptions.CON_RECONNECT_URL).getValue();
                brokerUrl += "," + reconnectBrokers;
            }

            if (clientOptions.getOption(ClientOptions.BROKER_OPTIONS).hasParsedValue()) {
                brokerUrl += "?" + clientOptions.getOption(ClientOptions.BROKER_OPTIONS).getValue();
            }
        }
        connectionManager = connectionManagerFactory.make(clientOptions, brokerUrl);
        Connection connection = connectionManager.getConnection();
        addConnection(connection);
        return connection;
    }

    /**
     * Abstract method, returns the current client Options.
     *
     * @return the given ClientOption list
     */
    abstract ClientOptions getClientOptions();

    /**
     * Create session for  client on provided connection using clientOptions
     *
     * @param clientOptions options of the client
     * @param connection    to be created session on
     * @param transacted    defines whether create transacted session or not
     * @return created session object
     */
    protected Session createSession(ClientOptions clientOptions, Connection connection, boolean transacted) {
        Session session = null;
//    boolean transacted = Boolean.parseBoolean(clientOptions.getOption(ClientOptions.TRANSACTED).getValue());
        int acknowledgeMode = SESSION_ACK_MAP.get(clientOptions.getOption(ClientOptions.SSN_ACK_MODE).getValue());
        try {
            // if transacted is true, acknowledgeMode is ignored
            session = connection.createSession(transacted, acknowledgeMode);
        } catch (JMSException e) {
            LOG.error("Error while creating session! " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
        if (sessions == null) {
            sessions = new ArrayList<>();
        }
        sessions.add(session);
        return session;
    }

    protected Destination getDestination() {
        return this.connectionManager.getDestination();
    }

    String getDestinationType() {
        return getClientOptions().getOption(ClientOptions.DESTINATION_TYPE).getValue();
    }

    /**
     * Returns the list of connections for this client
     *
     * @return list of Connections
     */
    List<Connection> getConnections() {
        return connections;
    }

    /**
     * After creation of new connection, this connection
     * is automatically added to the list of connections.
     * No need to use it explicitly.
     *
     * @param connection to be added to the list
     */
    void addConnection(Connection connection) {
        if (connections == null) {
            connections = new ArrayList<>(getCount());
        }
        connections.add(connection);
    }

    /**
     * Returns the list of sessions for this client
     *
     * @return list of Sessions
     */
    List<Session> getSessions() {
        return sessions;
    }

    /**
     * Add this session to the list of sessions.
     *
     * @param session to be added to session list
     */
    void addSession(Session session) {
        if (sessions == null) {
            sessions = new ArrayList<>(getCount());
        }
        sessions.add(session);
    }

    /**
     * Returns the list of MessageProducers for this client
     *
     * @return list of MessageProducers
     */
    List<MessageProducer> getProducers() {
        return messageProducers;
    }

    /**
     * Add this producer to the list of messageProducers.
     *
     * @param messageProducer to be added to messageProducers list
     */
    void addMessageProducer(MessageProducer messageProducer) {
        if (messageProducers == null) {
            messageProducers = new ArrayList<>(getCount());
        }
        messageProducers.add(messageProducer);
    }

    /**
     * Returns the list of MessageConsumers for this client
     *
     * @return list of MessageConsumers
     */
    List<MessageConsumer> getConsumers() {
        return messageConsumers;
    }

    /**
     * Add this consumer to the list of messageConsumer.
     *
     * @param messageConsumer to be added to messageConsumers list
     */
    void addMessageConsumer(MessageConsumer messageConsumer) {
        if (messageConsumers == null) {
            messageConsumers = new ArrayList<>(getCount());
        }
        messageConsumers.add(messageConsumer);
    }

    /**
     * Add given queue to queues list
     *
     * @param queue to be added to queues list
     */
    void addQueue(Queue queue) {
        if (queues == null) {
            queues = new ArrayList<>(getCount());
        }
        queues.add(queue);
    }

    /**
     * Returns the list of queues added
     *
     * @return queues added
     */
    public List<Queue> getQueues() {
        return queues;
    }

    /**
     * Returns the number of "count" argument.
     *
     * @return number of messages/connections depending on client
     */
    int getCount() {
        return Integer.parseInt(getClientOptions().getOption(ClientOptions.COUNT).getValue());
    }

    /**
     * Close objects for given Client.
     * Sleep for given period of time before closing MessageProducer/Consumer,
     * Session and Connection.
     *
     * @param client     holding all the open connection objects
     * @param closeSleep sleep for given period of time between closings objects
     */
    protected static void closeConnObjects(CoreClient client, double closeSleep) {
        long sleepMs = Math.round(closeSleep * 1000);
        if (client.getProducers() != null) {
            if (closeSleep > 0) {
                LOG.debug("Sleeping before closing producers for " + closeSleep + " seconds.");
                Utils.sleep(sleepMs);
            }
            for (MessageProducer producer : client.getProducers()) {
                client.close(producer);
            }
        }

        if (client.getConsumers() != null) {
            if (closeSleep > 0) {
                LOG.debug("Sleeping before closing consumers for " + closeSleep + " seconds.");
                Utils.sleep(sleepMs);
            }
            for (MessageConsumer consumer : client.getConsumers()) {
                client.close(consumer);
            }
        }

        if (client.getSessions() != null) {
            if (closeSleep > 0) {
                LOG.debug("Sleeping before closing sessions for " + closeSleep + " seconds.");
                Utils.sleep(sleepMs);
            }
            for (Session session : client.getSessions()) {
                client.close(session);
            }
        }

        if (closeSleep > 0) {
            LOG.debug("Sleeping before closing connections for " + closeSleep + " seconds.");
            Utils.sleep(sleepMs);
        }
        for (Connection connection : client.getConnections()) {
            client.close(connection);
        }
    }

    protected void close(Connection connection) {
        try {
            if (connection != null) {
                LOG.trace("Closing connection " + connection.toString());
                connection.close();
            }
        } catch (JMSException e) {
            e.printStackTrace();
            if (e.getCause() instanceof SocketException
                && e.getCause().getMessage().equals("Connection closed by remote host")) {
                return;  // suppress error, explained at https://issues.apache.org/jira/browse/AMQ-6956
            }
            System.exit(1);
        }
    }

    protected void close(Session session) {
        try {
            LOG.trace("Closing session " + session.toString());
            session.close();
        } catch (JMSException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    protected void close(MessageProducer messageProducer) {
        try {
            LOG.trace("Closing sender " + messageProducer.toString());
            messageProducer.close();
        } catch (JMSException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    protected void close(MessageConsumer messageConsumer) {
        try {
            LOG.trace("Closing receiver " + messageConsumer.toString());
            messageConsumer.close();
        } catch (JMSException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Print message using JmsMessageFormatter in given format.
     * Printing format is specified using LOG_MSGS value
     * as (dict|body|upstream|none).
     *
     * @param clientOptions options of the client
     * @param message       to be printed
     */
    protected void printMessage(ClientOptions clientOptions, Message message) {
        final String logMsgs = clientOptions.getOption(ClientOptions.LOG_MSGS).getValue();
        final String out = clientOptions.getOption(ClientOptions.OUT).getValue();
        Map<String, Object> messageData = null;
        try {
            switch (logMsgs) {
                case "dict":
                    messageData = jmsMessageFormatter.formatMessageAsDict(message);
                    break;
                case "body":
                    messageData = jmsMessageFormatter.formatMessageBody(message);
                    break;
                case "interop":
                case "json":
                    messageData = jmsMessageFormatter.formatMessageAsInterop(message);
                    break;
                case "none":
                default:
                    break;
            }
        } catch (JMSException e) {
            LOG.error("Unable to retrieve text from message.\n" + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
        if (messageData != null) {
            if ("json".equals(logMsgs) || "json".equals(out)) {
                jmsMessageFormatter.printMessageAsJson(messageData);
            } else {
                jmsMessageFormatter.printMessageAsPython(messageData);
            }
        }
    }

    /**
     * Do the given transaction.
     *
     * @param session     to do transaction on this session
     * @param transaction transaction action type to perform
     */
    protected static void doTransaction(Session session, String transaction) {
        try {
            StringBuilder txLog = new StringBuilder("Performed ");
            switch (transaction.toLowerCase()) {
                case "commit":
                    session.commit();
                    txLog.append("Commit");
                    break;
                case "rollback":
                    session.rollback();
                    txLog.append("Rollback");
                    break;
                case "recover":
                    session.recover();
                    txLog.append("Recover");
                    break;
                case "none":
                    txLog.append("None");
                    break;
                default:
                    LOG.error("Unknown tx action: '" + transaction + "'! Exiting");
                    System.exit(2);
            }
            LOG.trace(txLog.append(" TX action").toString());
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }

    /**
     * Set global options applicable to all clients.
     * Only Logging for now.
     *
     * @param clientOptions options of the client
     */
    protected static void setGlobalClientOptions(ClientOptions clientOptions) {
        if (clientOptions.getOption(ClientOptions.LOG_LEVEL).hasParsedValue()) {
            Utils.setLogLevel(clientOptions.getOption(ClientOptions.LOG_LEVEL).getValue());
        }
    }

    /**
     * Format broker connection strictly for 'broker' argument.
     * Url consists of protocol, (username+password), hostname and port.
     *
     * @param clientOptions
     * @return
     */
    static String formBrokerUrl(ClientOptions clientOptions) {
        StringBuilder brkCon = new StringBuilder();
        brkCon.append(clientOptions.getOption(ClientOptions.PROTOCOL).getValue());
        if (clientOptions.getOption(ClientOptions.FAILOVER_URL).hasParsedValue()) {
            brkCon.append(":(").append(clientOptions.getOption(ClientOptions.FAILOVER_URL).getValue()).append(")");
        } else {
            brkCon.append("://");
            brkCon.append(clientOptions.getOption(ClientOptions.BROKER_HOST).getValue()).append(":")
                .append(clientOptions.getOption(ClientOptions.BROKER_PORT).getValue());
        }
        LOG.trace("BrokerUrl=" + brkCon.toString());
        return brkCon.toString();
    }
}
