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

import com.redhat.mqe.lib.AMQPMessageFormatter;
import com.redhat.mqe.lib.MessageFormatter;
import com.redhat.mqe.lib.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Core implementation of creating various connections to brokers using clients.
 */
public abstract class CoreClient {
  static final String AMQ_CLIENT_TYPE = "amq";
  static final String QPID_CLIENT_TYPE = "qpid";
  static Logger LOG = LoggerFactory.getLogger(CoreClient.class);
  private ConnectionManager connectionManager;
  static Map<String, String> connectionOptionsUrlMap;
  private static final Map<String, Integer> SESSION_ACK_MAP = new HashMap<>(5);
  private static final Map<String, String> CONNECTION_TRANSLATION_MAP = new HashMap<>();

  static {
//    SESSION_ACK_MAP.put("transacted", Session.SESSION_TRANSACTED); // This is handled by TRANSACTED option
    SESSION_ACK_MAP.put("auto", Session.AUTO_ACKNOWLEDGE);
    SESSION_ACK_MAP.put("client", Session.CLIENT_ACKNOWLEDGE);
    SESSION_ACK_MAP.put("dups_ok", Session.DUPS_OK_ACKNOWLEDGE);
//    SESSION_ACK_MAP.put("individual", Session.INDIVIDUAL_ACKNOWLEDGE); // ActiveMQSpecific?

    CONNECTION_TRANSLATION_MAP.put(ClientOptions.CON_HEARTBEAT, "amqp.idleTimeout");
    CONNECTION_TRANSLATION_MAP.put(ClientOptions.USERNAME, "jms.username");
    CONNECTION_TRANSLATION_MAP.put(ClientOptions.PASSWORD, "jms.password");
    CONNECTION_TRANSLATION_MAP.put(ClientOptions.CON_VHOST, "amqp.vhost");
    CONNECTION_TRANSLATION_MAP.put(ClientOptions.CON_SASL_MECHS, "amqp.saslMechanisms");
    CONNECTION_TRANSLATION_MAP.put(ClientOptions.CON_SASL_LAYER, "amqp.saslLayer");
    CONNECTION_TRANSLATION_MAP.put(ClientOptions.CON_MAX_FRAME_SIZE, "amqp.maxFrameSize");
    CONNECTION_TRANSLATION_MAP.put(ClientOptions.CON_DRAIN_TIMEOUT, "amqp.drainTimeout");
    CONNECTION_TRANSLATION_MAP.put(ClientOptions.CON_CLIENTID, "jms.clientID");
    CONNECTION_TRANSLATION_MAP.put(ClientOptions.CON_ASYNC_SEND, "jms.forceAsyncSend");
    CONNECTION_TRANSLATION_MAP.put(ClientOptions.CON_SYNC_SEND, "jms.alwaysSyncSend");
    CONNECTION_TRANSLATION_MAP.put(ClientOptions.CON_ASYNC_ACKS, "jms.sendAcksAsync");
    CONNECTION_TRANSLATION_MAP.put(ClientOptions.CON_LOC_MSG_PRIO, "jms.localMessagePriority");
    CONNECTION_TRANSLATION_MAP.put(ClientOptions.CON_VALID_PROP_NAMES, "jms.validatePropertyNames");
    CONNECTION_TRANSLATION_MAP.put(ClientOptions.CON_RECV_LOCAL_ONLY, "jms.receiveLocalOnly");
    CONNECTION_TRANSLATION_MAP.put(ClientOptions.CON_RECV_NOWAIT_LOCAL, "jms.receiveNoWaitLocalOnly");
    CONNECTION_TRANSLATION_MAP.put(ClientOptions.CON_QUEUE_PREFIX, "jms.queuePrefix");
    CONNECTION_TRANSLATION_MAP.put(ClientOptions.CON_TOPIC_PREFIX, "jms.topicPrefix");
    CONNECTION_TRANSLATION_MAP.put(ClientOptions.CON_CLOSE_TIMEOUT, "jms.closeTimeout");
    CONNECTION_TRANSLATION_MAP.put(ClientOptions.CON_CONN_TIMEOUT, "jms.connectTimeout");
    CONNECTION_TRANSLATION_MAP.put(ClientOptions.CON_CLIENTID_PREFIX, "jms.clientIDPrefix");
    CONNECTION_TRANSLATION_MAP.put(ClientOptions.CON_CONNID_PREFIX, "jms.connectionIDPrefix");
    CONNECTION_TRANSLATION_MAP.put(ClientOptions.CON_POPULATE_JMSXUSERID, "jms.populateJMSXUserID");

    CONNECTION_TRANSLATION_MAP.put(ClientOptions.CON_PREFETCH_QUEUE, "jms.prefetchPolicy.queuePrefetch");
    CONNECTION_TRANSLATION_MAP.put(ClientOptions.CON_PREFETCH_TOPIC, "jms.prefetchPolicy.topicPrefetch");
    CONNECTION_TRANSLATION_MAP.put(ClientOptions.CON_PREFETCH_BROWSER, "jms.prefetchPolicy.queueBrowserPrefetch");
    CONNECTION_TRANSLATION_MAP.put(ClientOptions.CON_PREFETCH_DUR_TOPIC, "jms.prefetchPolicy.durableTopicPrefetch");
    CONNECTION_TRANSLATION_MAP.put(ClientOptions.CON_PREFETCH_ALL, "jms.prefetchPolicy.all");
    CONNECTION_TRANSLATION_MAP.put(ClientOptions.CON_MAX_REDELIVERIES, "jms.redeliveryPolicy.maxRedeliveries");

    CONNECTION_TRANSLATION_MAP.put(ClientOptions.CON_RETRIES, "failover.maxReconnectAttempts");
    CONNECTION_TRANSLATION_MAP.put(ClientOptions.CON_RECONNECT_TIMEOUT, "failover.reconnectDelay");
    CONNECTION_TRANSLATION_MAP.put(ClientOptions.CON_RECONNECT_INTERVAL, "failover.maxReconnectDelay");
    CONNECTION_TRANSLATION_MAP.put(ClientOptions.CON_RECONNECT_BACKOFF, "failover.useReconnectBackOff");
    CONNECTION_TRANSLATION_MAP.put(ClientOptions.CON_RECONNECT_BACKOFF_MULTIPLIER, "failover.reconnectBackOffMultiplier");
    CONNECTION_TRANSLATION_MAP.put(ClientOptions.CON_RECONNECT_START_LIMIT, "failover.startupMaxReconnectAttempts");
    CONNECTION_TRANSLATION_MAP.put(ClientOptions.CON_RECONNECT_INITIAL_DELAY, "failover.initialReconnectDelay");
    CONNECTION_TRANSLATION_MAP.put(ClientOptions.CON_RECONNECT_WARN_ATTEMPTS, "failover.warnAfterReconnectAttempts");

    CONNECTION_TRANSLATION_MAP.put(ClientOptions.CON_SSL_KEYSTORE_LOC, "transport.keyStoreLocation");
    CONNECTION_TRANSLATION_MAP.put(ClientOptions.CON_SSL_KEYSTORE_PASS, "transport.keyStorePassword");
    CONNECTION_TRANSLATION_MAP.put(ClientOptions.CON_SSL_TRUSTSTORE_LOC, "transport.trustStoreLocation");
    CONNECTION_TRANSLATION_MAP.put(ClientOptions.CON_SSL_TRUSTSTORE_PASS, "transport.trustStorePassword");
    CONNECTION_TRANSLATION_MAP.put(ClientOptions.CON_SSL_STORE_TYPE, "transport.storeType");
    CONNECTION_TRANSLATION_MAP.put(ClientOptions.CON_SSL_CONTEXT_PROTOCOL, "transport.contextProtocol");
    CONNECTION_TRANSLATION_MAP.put(ClientOptions.CON_SSL_ENA_CIPHERED, "transport.enabledCipherSuites");
    CONNECTION_TRANSLATION_MAP.put(ClientOptions.CON_SSL_DIS_CIPHERED, "transport.disabledCipherSuites");
    CONNECTION_TRANSLATION_MAP.put(ClientOptions.CON_SSL_ENA_PROTOS, "transport.enabledProtocols");
    CONNECTION_TRANSLATION_MAP.put(ClientOptions.CON_SSL_DIS_PROTOS, "transport.disabledProtocols");
    CONNECTION_TRANSLATION_MAP.put(ClientOptions.CON_SSL_TRUST_ALL, "transport.trustAll");
    CONNECTION_TRANSLATION_MAP.put(ClientOptions.CON_SSL_VERIFY_HOST, "transport.verifyHost");
    CONNECTION_TRANSLATION_MAP.put(ClientOptions.CON_SSL_KEYALIAS, "transport.keyAlias");

    CONNECTION_TRANSLATION_MAP.put(ClientOptions.CON_TCP_SEND_BUF_SIZE, "transport.sendBufferSize");
    CONNECTION_TRANSLATION_MAP.put(ClientOptions.CON_TCP_RECV_BUF_SIZE, "transport.receiveBufferSize");
    CONNECTION_TRANSLATION_MAP.put(ClientOptions.CON_TCP_TRAFFIC_CLASS, "transport.trafficClass");
    CONNECTION_TRANSLATION_MAP.put(ClientOptions.CON_TCP_CON_TIMEOUT, "transport.connectTimeout");
    CONNECTION_TRANSLATION_MAP.put(ClientOptions.CON_TCP_SOCK_TIMEOUT, "transport.soTimeout");
    CONNECTION_TRANSLATION_MAP.put(ClientOptions.CON_TCP_SOCK_LINGER, "transport.soLinger");
    CONNECTION_TRANSLATION_MAP.put(ClientOptions.CON_TCP_KEEP_ALIVE, "transport.tcpKeepAlive");
    CONNECTION_TRANSLATION_MAP.put(ClientOptions.CON_TCP_NO_DELAY, "transport.tcpNoDelay");
  }

  private List<Connection> connections;
  private List<Session> sessions;
  private List<MessageProducer> messageProducers;
  private List<MessageConsumer> messageConsumers;
  private List<Queue > queues;
  private static String clientType;

  /**
   * Method starts the given client. Serves as entry point.
   */
  abstract void startClient();

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
      if (clientOptions.getOption(ClientOptions.BROKER_OPTIONS).hasParsedValue()) {
        brokerUrl += "?" + clientOptions.getOption(ClientOptions.BROKER_OPTIONS).getValue();
      }
    }
    connectionManager = new ConnectionManager(clientOptions, brokerUrl);
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
  Session createSession(ClientOptions clientOptions, Connection connection, boolean transacted) {
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

  Destination getDestination() {
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
  static void closeConnObjects(CoreClient client, double closeSleep) {
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

  void close(Connection connection) {
    try {
      LOG.trace("Closing connection " + connection.toString());
      connection.close();
    } catch (JMSException e) {
      e.printStackTrace();
    }
  }

  void close(Session session) {
    try {
      LOG.trace("Closing session " + session.toString());
      session.close();
    } catch (JMSException e) {
      e.printStackTrace();
    }
  }

  void close(MessageProducer messageProducer) {
    try {
      LOG.trace("Closing sender " + messageProducer.toString());
      messageProducer.close();
    } catch (JMSException e) {
      e.printStackTrace();
    }
  }

  void close(MessageConsumer messageConsumer) {
    try {
      LOG.trace("Closing receiver " + messageConsumer.toString());
      messageConsumer.close();
    } catch (JMSException e) {
      e.printStackTrace();
    }
  }

  /**
   * Print message using MessageFormatter in given format.
   * Printing format is specified using LOG_MSGS value
   * as (dict|body|upstream|none).
   *
   * @param clientOptions options of the client
   * @param message       to be printed
   */
  static void printMessage(ClientOptions clientOptions, Message message) {
    MessageFormatter formatter = new AMQPMessageFormatter();
    switch (clientOptions.getOption(ClientOptions.LOG_MSGS).getValue()) {
      case "dict":
        formatter.printMessageAsDict(message);
        break;
      case "body":
        formatter.printMessageBodyAsText(message);
        break;
      case "interop":
        formatter.printMessageAsInterop(message);
        break;
      case "none":
      default:
        break;
    }
  }

  // TODO - make it better, easily extensible for future clients
  static boolean isAMQClient() {
    return clientType.equals(AMQ_CLIENT_TYPE);
  }

  static boolean isQpidClient() {
    return clientType.equals(QPID_CLIENT_TYPE);
  }

  /**
   * Supported client types are 'qpid' and 'amq'.
   * Specific broker-related data types are different among different
   * brokers.
   *
   * @param client to which broker will connect. Supported amq/qpid values.
   */
  public static void setClientType(String client) {
    clientType = client.toLowerCase();
  }

  /**
   * Do the given transaction.
   *
   * @param session     to do transaction on this session
   * @param transaction transaction action type to perform
   */
  static void doTransaction(Session session, String transaction) {
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
  static void setGlobalClientOptions(ClientOptions clientOptions) {
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
      if (isQpidClient()) {
        if (clientOptions.getOption(ClientOptions.USERNAME).hasParsedValue()) {
          brkCon.append(clientOptions.getOption(ClientOptions.USERNAME).getValue()).append(":");
        }
        if (clientOptions.getOption(ClientOptions.PASSWORD).hasParsedValue()) {
          brkCon.append(clientOptions.getOption(ClientOptions.PASSWORD).getValue());
        }
        if (clientOptions.getOption(ClientOptions.USERNAME).hasParsedValue()
            || clientOptions.getOption(ClientOptions.PASSWORD).hasParsedValue()) {
          brkCon.append("@");
        }
      }
      brkCon.append(clientOptions.getOption(ClientOptions.BROKER_HOST).getValue()).append(":")
          .append(clientOptions.getOption(ClientOptions.BROKER_PORT).getValue());
    }
    LOG.trace("BrokerUrl=" + brkCon.toString());
    return brkCon.toString();
  }

  /**
   * Create connection url from given options.
   *
   * @return string of options starting as "?<conOpts..>"
   */
  static String getConnectionOptionsAsUrl() {
    StringBuilder conOptUrl = new StringBuilder();
    if (connectionOptionsUrlMap != null) {
      for (String optionName : connectionOptionsUrlMap.keySet()) {
        String divider = (conOptUrl.length() == 0) ? "?" : "&";
        conOptUrl.append(divider);
        conOptUrl.append(optionName).append("=");
        appendSingleQuote(conOptUrl).append(connectionOptionsUrlMap.get(optionName));
        appendSingleQuote(conOptUrl);
      }
      return conOptUrl.toString();
    } else {
      return "";
    }
  }

  /**
   * Do not use quotes in connection options for Qpid JMS AMQP client,
   * codename (AMQ client).
   * @param stringBuilder string to have appended the singleQuote
   * @return same string with the appended quote
   */
  static StringBuilder appendSingleQuote(StringBuilder stringBuilder) {
    return (isAMQClient()) ? stringBuilder : stringBuilder.append("'");
  }

  /**
   * Fill connectionOptionsUrlMap with the data.
   * If needed convert the client input connectin option
   * to the appropriate jms connection option.
   * Also, if needed, change seconds to milliseconds.
   */
  static void addConnectionOptions(com.redhat.mqe.lib.Option option) {
    if (connectionOptionsUrlMap == null) {
      connectionOptionsUrlMap = new HashMap<>();
    }
    String jmsConOptionName = CONNECTION_TRANSLATION_MAP.get(option.getName());
    if (jmsConOptionName != null) {
      // TODO need of conversion map, if more options are neeed to be altered
      if (option.getName().equals(ClientOptions.CON_HEARTBEAT)) {
        Integer value = Math.round(Float.parseFloat(option.getValue()) * 1000);
        connectionOptionsUrlMap.put(jmsConOptionName, value.toString());
      } else {
        connectionOptionsUrlMap.put(jmsConOptionName, option.getValue());
      }
    } else {
      if (option.getName().equals(ClientOptions.CON_RECONNECT)) {
        // use failover mechanism, conn-reconnect does not perform nor add any other action to the client
        return;
      }
      LOG.error("Connection option {} is not recognized! ", option.getName());
      System.exit(2);
    }
  }
}
