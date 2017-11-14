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
import com.redhat.mqe.lib.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Interface provides support for manipulating
 * with default options for clients.
 */
public abstract class AacClientOptions extends ClientOptions {
    protected static final Logger LOG = LoggerFactory.getLogger(AacClientOptions.class);
    private static final Map<String, String> translationDtestJmsMap = new HashMap<>();
    private final List<Option> defaultOptions = new ArrayList<>();
    private List<Option> updatedOptions = new ArrayList<>();
    static List<String> argsAcceptingMultipleValues = new ArrayList<>();
    static List<String> failoverProtocols = new ArrayList<>();
    static List<Option> numericArgumentValueOptionList = new ArrayList<>();

    /* Mapping of client option to jms client is done in CoreClient.CONNECTION_TRANSLATION_MAP */
    static final String BROKER = "broker";
    static final String BROKER_URI = "broker-uri";
    static final String TRANSACTED = "transacted";
    static final String MSG_DURABLE = "msg-durable";
    static final String ADDRESS = "address";
    static final String DURATION = "duration";
    static final String DURATION_MODE = "duration-mode";
    static final String LOG_LEVEL = "log-lib";
    static final String LOG_STATS = "log-stats";
    static final String USERNAME = "conn-username";                               // jms.username
    static final String PASSWORD = "conn-password";                               // jms.password
    static final String HELP = "help";
    static final String SSN_ACK_MODE = "ssn-ack-mode";
    static final String CLOSE_SLEEP = "close-sleep";

    static final String CON_HEARTBEAT = "conn-heartbeat";                         // amqp.idleTimeout=[ms] * 1000ms
    static final String CON_VHOST = "conn-vhost";                                 // amqp.vhost
    static final String CON_SASL_MECHS = "conn-auth-mechanisms";                  // amqp.saslMechanisms
    static final String CON_SASL_LAYER = "conn-auth-sasl";                        // amqp.saslLayer
    static final String CON_MAX_FRAME_SIZE = "conn-max-frame-size";               // amqp.maxFrameSize
    static final String CON_DRAIN_TIMEOUT = "conn-drain-timeout";                 // amqp.drainTimeout

    static final String CON_CLIENTID = "conn-clientid";                           // jms.clientID
    static final String CON_ASYNC_SEND = "conn-async-send";                       // jms.forceAsyncSend
    static final String CON_SYNC_SEND = "conn-sync-send";                         // jms.alwaysSyncSend
    static final String CON_ASYNC_ACKS = "conn-async-acks";                       // jms.sendAcksAsync
    static final String CON_LOC_MSG_PRIO = "conn-local-msg-priority";             // jms.localMessagePriority
    static final String CON_VALID_PROP_NAMES = "conn-valid-prop-names";           // jms.validatePropertyNames

    static final String CON_RECV_LOCAL_ONLY = "conn-recv-local-only";             // jms.receiveLocalOnly
    static final String CON_RECV_NOWAIT_LOCAL = "conn-recv-nowait-local";         // jms.receiveNoWaitLocalOnly

    static final String CON_QUEUE_PREFIX = "conn-queue-prefix";                   // jms.queuePrefix
    static final String CON_TOPIC_PREFIX = "conn-topic-prefix";                   // jms.topicPrefix
    static final String CON_CLOSE_TIMEOUT = "conn-close-timeout";                 // jms.closeTimeout
    static final String CON_CONN_TIMEOUT = "conn-conn-timeout";                   // jms.connectTimeout
    static final String CON_CLIENTID_PREFIX = "conn-clientid-prefix";             // jms.clientIDPrefix
    static final String CON_CONNID_PREFIX = "conn-connid-prefix";                 // jms.connectionIDPrefix
    static final String CON_POPULATE_JMSXUSERID = "conn-populate-user-id";        // jms.populateJMSXUserID

    static final String CON_PREFETCH_QUEUE = "conn-prefetch-queue";               // jms.prefetchPolicy.queuePrefetch
    static final String CON_PREFETCH_TOPIC = "conn-prefetch-topic";               // jms.prefetchPolicy.topicPrefetch
    static final String CON_PREFETCH_BROWSER = "conn-prefetch-browser";           // jms.prefetchPolicy.queueBrowserPrefetch
    static final String CON_PREFETCH_DUR_TOPIC = "conn-prefetch-topic-dur";       // jms.prefetchPolicy.durableTopicPrefetch
    static final String CON_PREFETCH_ALL = "conn-prefetch";                       // jms.prefetchPolicy.all

    static final String CON_MAX_REDELIVERIES = "conn-redeliveries-max";           // jms.redeliveryPolicy.maxRedeliveries

    static final String CON_TCP_SEND_BUF_SIZE = "conn-tcp-buf-size-send";         // transport.sendBufferSize
    static final String CON_TCP_RECV_BUF_SIZE = "conn-tcp-buf-size-recv";         // transport.receiveBufferSize
    static final String CON_TCP_TRAFFIC_CLASS = "conn-tcp-traffic-class";         // transport.trafficClass
    static final String CON_TCP_CON_TIMEOUT = "conn-tcp-conn-timeout";            // transport.connectTimeout
    static final String CON_TCP_SOCK_TIMEOUT = "conn-tcp-sock-timeout";           // transport.soTimeout
    static final String CON_TCP_SOCK_LINGER = "conn-tcp-sock-linger";             // transport.soLinger
    static final String CON_TCP_KEEP_ALIVE = "conn-tcp-keep-alive";               // transport.tcpKeepAlive
    static final String CON_TCP_NO_DELAY = "conn-tcp-no-delay";                   // transport.tcpNoDelay

    static final String CON_RECONNECT = "conn-reconnect";                         // enable reconnect options
    static final String CON_RECONNECT_INITIAL_DELAY = "conn-reconnect-initial-delay"; // failover.initialReconnectDelay (0)
    static final String CON_RECONNECT_TIMEOUT = "conn-reconnect-timeout";         // failover.reconnectDelay (10ms)
    static final String CON_RECONNECT_INTERVAL = "conn-reconnect-interval";       // failover.maxReconnectDelay (30sec)
    static final String CON_RECONNECT_BACKOFF = "conn-reconnect-backoff";         // failover.useReconnectBackOff (true)
    static final String CON_RECONNECT_BACKOFF_MULTIPLIER = "conn-reconnect-backoff-multiplier"; // failover.reconnectBackOffMultiplier
    static final String CON_RETRIES = "conn-reconnect-limit";                     // failover.maxReconnectAttempts (-1)
    static final String CON_RECONNECT_START_LIMIT = "conn-reconnect-start-limit"; // failover.startupMaxReconnectAttempts
    static final String CON_RECONNECT_WARN_ATTEMPTS = "conn-reconnect-warn-attempts"; // failover.warnAfterReconnectAttempts

    static final String CON_SSL_KEYSTORE_LOC = "conn-ssl-keystore-location";      // transport.keyStoreLocation
    static final String CON_SSL_KEYSTORE_PASS = "conn-ssl-keystore-password";     // transport.keyStorePassword
    static final String CON_SSL_TRUSTSTORE_LOC = "conn-ssl-truststore-location";  // transport.trustStoreLocation
    static final String CON_SSL_TRUSTSTORE_PASS = "conn-ssl-truststore-password"; // transport.trustStorePassword
    static final String CON_SSL_STORE_TYPE = "conn-ssl-store-type";               // transport.storeType
    static final String CON_SSL_CONTEXT_PROTOCOL = "conn-ssl-context-proto";      // transport.contextProtocol
    static final String CON_SSL_ENA_CIPHERED = "conn-ssl-ena-ciphered-suites";    // transport.enabledCipherSuites
    static final String CON_SSL_DIS_CIPHERED = "conn-ssl-dis-ciphered-suites";    // transport.disabledCipherSuites
    static final String CON_SSL_ENA_PROTOS = "conn-ssl-ena-protos";               // transport.enabledProtocols
    static final String CON_SSL_DIS_PROTOS = "conn-ssl-dis-protos";               // transport.disabledProtocols
    static final String CON_SSL_TRUST_ALL = "conn-ssl-trust-all";                 // transport.trustAll
    static final String CON_SSL_VERIFY_HOST = "conn-ssl-verify-host";             // transport.verifyHost
    static final String CON_SSL_KEYALIAS = "conn-ssl-key-alias";                  // transport.keyAlias


    // TODO Not implemented by client libraries
//  static final String CON_SSL_PROTOCOL = "conn-ssl-protocol";

    // These few options are not settable from outside. Client sets them up when setting parsed options.
    static final String PROTOCOL = "protocol";
    static final String BROKER_HOST = "broker_host";
    static final String BROKER_PORT = "broker_port";
    static final String BROKER_OPTIONS = "broker_options";
    static final String DESTINATION_TYPE = "destination_type";
    static final String FAILOVER_PROTO = "failover";
    static final String DISCOVERY_PROTO = "discovery";
    static final String FAILOVER_URL = "failover_url";

    /**
     * S+R+?
     */
    static final String TIMEOUT = "timeout";
    static final String COUNT = "count";
    static final String LOG_MSGS = "log-msgs";
    static final String TX_SIZE = "tx-size";
    static final String TX_ACTION = "tx-action";
    static final String TX_ENDLOOP_ACTION = "tx-endloop-action";
    static final String CAPACITY = "capacity";

    /**
     * RECEIVER Options
     */
    static final String ACTION = "action";
    static final String SYNC_MODE = "sync-mode";
    static final String MSG_LISTENER = "msg-listener-ena";
    static final String DURABLE_SUBSCRIBER = "durable-subscriber";
    static final String UNSUBSCRIBE = "subscriber-unsubscribe";
    static final String DURABLE_SUBSCRIBER_PREFIX = "durable-subscriber-prefix";
    static final String DURABLE_SUBSCRIBER_NAME = "durable-subscriber-name";
    static final String MSG_SELECTOR = "msg-selector";
    static final String BROWSER = "recv-browse";
    static final String PROCESS_REPLY_TO = "process-reply-to";
    static final String MSG_BINARY_CONTENT_TO_FILE = "msg-binary-content-to-file";

    /**
     * SENDER
     */
    static final String MSG_TTL = "msg-ttl";
    static final String MSG_PRIORITY = "msg-priority";
    static final String MSG_ID = "msg-id";
    static final String MSG_REPLY_TO = "msg-reply-to";
    static final String MSG_SUBJECT = "msg-subject";
    static final String MSG_USER_ID = "msg-user-id";
    static final String MSG_CORRELATION_ID = "msg-correlation-id";
    static final String MSG_NOTIMESTAMP = "msg-no-timestamp";

    static final String PROPERTY_TYPE = "property-type";
    static final String MSG_PROPERTY = "msg-property";
    static final String CONTENT_TYPE = "content-type";
    static final String MSG_CONTENT = "msg-content";
    public static final String MSG_CONTENT_BINARY = "msg-content-binary";
    static final String MSG_CONTENT_TYPE = "msg-content-type";
    static final String MSG_CONTENT_FROM_FILE = "msg-content-from-file";
    static final String MSG_CONTENT_MAP_ITEM = "msg-content-map-item";
    static final String MSG_CONTENT_LIST_ITEM = "msg-content-list-item";

    static final String MSG_GROUP_ID = "msg-group-id";
    static final String MSG_GROUP_SEQ = "msg-group-seq";
    static final String MSG_REPLY_TO_GROUP_ID = "msg-reply-to-group-id";


    /**
     * CONNECTOR
     */
    static final String OBJ_CTRL = "obj-ctrl";
    static final String Q_COUNT = "q-count";

    /**
     * QMF Options?
     * TODO qmf options
     */

    public AacClientOptions() {
        defaultOptions.addAll(Arrays.asList(
            // Options updated by parsing broker/broker-url
            new Option(PROTOCOL, "amqp"),
            new Option(BROKER_HOST, "localhost"),
            new Option(BROKER_PORT, "5672"),
            new Option(BROKER_OPTIONS, ""),
            new Option(DESTINATION_TYPE, AacConnectionManager.QUEUE_OBJECT),

            new Option(USERNAME, "", "USERNAME", "", "jms.username (not defined before host:port in qpid-jms-client)"),
            new Option(PASSWORD, "", "PASSWORD", "", "jms.password (not defined before host:port in qpid-jms-client)"),
            new Option(HELP, "h", "", "", "show this help"),
            new Option(BROKER, "b", "HOST:5672", "amqp://localhost:5672", "url broker to connect to, default"),
            new Option(BROKER_URI, "", "AmqpQpidJmsURL", "amqp://localhost:5672[[?conOpt=val]&conOpt=val]",
                "AMQP JMS QPID specific broker uri. NOTE: This options overrides everything related to broker & connection options. " +
                    "It is used as exactly as provided!"),
            new Option(LOG_LEVEL, "", "LEVEL", "info", "logging level of the client. trace/debug/info/warn/error"),
            new Option(LOG_STATS, "", "LEVEL", "INFO", "report various statistic/debug information"), // ?
            new Option(LOG_BYTES, "", "", "false", "report content of sent and received packets"), // ?
            new Option(SSN_ACK_MODE, "", "ACKMODE", "auto", "session acknowledge mode auto/client/dups_ok/(individual)"),
            new Option(CLOSE_SLEEP, "", "CSLEEP", "0", "sleep before publisher/subscriber/session/connection.close() in floating seconds"),

            new Option(CON_HEARTBEAT, "", "SECONDS", "60", "frequency of heartbeat messages (in seconds)"),
            new Option(CON_VHOST, "", "VHOST", "", "virtual hostname to connect to. (Default: main from URI)"),
            new Option(CON_SASL_LAYER, "", "ENABLED", "true", "choose whether SASL layer should be used"),
            new Option(CON_SASL_MECHS, "", "MECHS", "all", "comma separated list of SASL mechanisms allowed by client for authentication (plain/anonymous/external/cram-md5?/digest-md5?)"),
            new Option(CON_MAX_FRAME_SIZE, "", "BYTES", "1048576", "The max-frame-size value in bytes that is advertised to the peer. Default is 1048576"),
            new Option(CON_DRAIN_TIMEOUT, "", "MS", "60000", "The time in milliseconds that the client will wait for a response from the remote when a drain request is made. (Default 60000ms)"),

            new Option(CON_CLIENTID, "", "CLIENTID", "", "clientID value that is applied to the connection."),
            new Option(CON_ASYNC_SEND, "", "ENABLED", "false", "send all messages asynchronously (if false only non-persistent and transacted are send asynchronously"),
            new Option(CON_SYNC_SEND, "", "ENABLED", "false", "send all messages synchronously"),
            new Option(CON_ASYNC_ACKS, "", "ENABLED", "false", "causes all Message acknowledgments to be sent asynchronously"),
            new Option(CON_LOC_MSG_PRIO, "", "ENABLED", "false", "prefetched messages are reordered locally based on their given priority"),
            new Option(CON_VALID_PROP_NAMES, "", "ENABLED", "true", "message property names should be validated as valid Java identifiers"),

            new Option(CON_RECV_LOCAL_ONLY, "", "ENABLED", "false", "if enabled receive calls with a timeout will only check a consumers local message buffer"),
            new Option(CON_RECV_NOWAIT_LOCAL, "", "ENABLED", "false", "if enabled receiveNoWait calls will only check a consumers local message buffer"),

            new Option(CON_QUEUE_PREFIX, "", "PREFIX", "", "optional prefix value added to the name of any Queue created from a JMS Session"),
            new Option(CON_TOPIC_PREFIX, "", "PREFIX", "", "optional prefix value added to the name of any Topic created from a JMS Session."),
            new Option(CON_CLOSE_TIMEOUT, "", "TIMEOUT", "15", "timeout value that controls how long the client waits on Connection close before returning"),
            new Option(CON_CONN_TIMEOUT, "", "TIMEOUT", "15", "timeout value that controls how long the client waits on Connection establishment before returning with an error"),
            new Option(CON_CLIENTID_PREFIX, "", "PREFIX", "ID:", "client ID prefix for new connections"),
            new Option(CON_CONNID_PREFIX, "", "PREFIX", "ID:", "connection ID prefix used for a new connections. Usable for tracking connections in logs"),
            new Option(CON_POPULATE_JMSXUSERID, "", "ENABLED", "false", "populate the JMSXUserID for each sent message using authenticated username from connection"),

            new Option(CON_MAX_REDELIVERIES, "", "COUNT", "-1", "maximum number of allowed message redeliveries"),
            new Option(CON_PREFETCH_QUEUE, "", "COUNT", "1000", "number of messages which can be held in a prefetch buffer"),
            new Option(CON_PREFETCH_TOPIC, "", "COUNT", "1000", "number of messages which can be held in a prefetch buffer"),
            new Option(CON_PREFETCH_BROWSER, "", "COUNT", "1000", "number of messages which can be held in a prefetch buffer"),
            new Option(CON_PREFETCH_DUR_TOPIC, "", "COUNT", "1000", "number of messages which can be held in a prefetch buffer"),
            new Option(CON_PREFETCH_ALL, "", "COUNT", "1000", "set prefetch values to all prefetch options"),

            new Option(CON_RECONNECT, "", "ENABLED", "false", "enable default failover reconnect"),
            new Option(CON_RETRIES, "", "COUNT", "-1", "retry to connect to the broker that many times"),
            new Option(CON_RECONNECT_TIMEOUT, "", "MS", "10", "delay between successive reconnection attempts; constant if backoff is off"),
            new Option(CON_RECONNECT_INTERVAL, "", "SEC", "30", "maximum time that client will wait before next reconnect. Used only when backoff is on"),
            new Option(CON_RECONNECT_BACKOFF, "", "ENABLED", "true", "choose to use backoff multiplier or not"),
            new Option(CON_RECONNECT_BACKOFF_MULTIPLIER, "", "VALUE", "2.0", "backoff multiplier for reconnect intervals"),
            new Option(CON_RECONNECT_START_LIMIT, "", "INTERVAL", "-1", "For a client that has never connected to a remote peer before, this sets " +
                "the number of attempts made to connect before reporting the connection as failed. The default is value of maxReconnectAttempts"),
            new Option(CON_RECONNECT_INITIAL_DELAY, "", "DELAY", "0", "delay the client will wait before the first attempt to reconnect to a remote peer"),
            new Option(CON_RECONNECT_WARN_ATTEMPTS, "", "ATTEMPTS", "10", "that often the client will log a message indicating that failover reconnection is being attempted"),

            new Option(CON_SSL_KEYSTORE_LOC, "", "LOC", "", "default is to read from the system property \"javax.net.ssl.keyStore\""),
            new Option(CON_SSL_KEYSTORE_PASS, "", "PASS", "", "default is to read from the system property \"javax.net.ssl.keyStorePassword\""),
            new Option(CON_SSL_TRUSTSTORE_LOC, "", "LOC", "", "default is to read from the system property \"javax.net.ssl.trustStore\""),
            new Option(CON_SSL_TRUSTSTORE_PASS, "", "PASS", "", "default is to read from the system property \"javax.net.ssl.keyStorePassword\""),
            new Option(CON_SSL_STORE_TYPE, "", "TYPE", "JKS", "store type"),
            new Option(CON_SSL_CONTEXT_PROTOCOL, "", "PROTOCOL", "TLS", "protocol argument used when getting an SSLContext"),
            new Option(CON_SSL_ENA_CIPHERED, "", "SUITES", "", "enabled cipher suites (comma separated list); disabled ciphers are removed from this list."),
            new Option(CON_SSL_DIS_CIPHERED, "", "SUITES", "", "disabled cipher suites (comma separated list)"),
            new Option(CON_SSL_ENA_PROTOS, "", "PROTOCOLS", "", "enabled protocols (comma separated list). No default, meaning the context default protocols are used"),
            new Option(CON_SSL_DIS_PROTOS, "", "PROTOCOLS", "SSLv2Hello,SSLv3", "disabled protocols (comma separated list)"),
            new Option(CON_SSL_TRUST_ALL, "", "ENABLED", "false", ""),
            new Option(CON_SSL_VERIFY_HOST, "", "ENABLED", "true", ""),
            new Option(CON_SSL_KEYALIAS, "", "ALIAS", "", "alias to use when selecting a keypair from the keystore if required to send a client certificate to the server"),

            new Option(CON_TCP_SEND_BUF_SIZE, "", "SIZE", "64", "tcp send buffer size in kilobytes"),
            new Option(CON_TCP_RECV_BUF_SIZE, "", "SIZE", "64", "tcp receive buffer size in kilobytes"),
            new Option(CON_TCP_TRAFFIC_CLASS, "", "CLASS?", "0", "?tcp traffic class"),
            new Option(CON_TCP_CON_TIMEOUT, "", "TIMEOUT", "60", "tcp connection timeout in seconds"),
            new Option(CON_TCP_SOCK_TIMEOUT, "", "TIMEOUT", "-1", "?tcp socket timeout in (-1 disabled?)"),
            new Option(CON_TCP_SOCK_LINGER, "", "TIMEOUT", "-1", "?tcp socket linger timeout"),
            new Option(CON_TCP_KEEP_ALIVE, "", "ENABLED", "false", "send tcp keep alive packets"),
            new Option(CON_TCP_NO_DELAY, "", "ENABLED", "true", "use tcp_nodelay (automatic concatenation of small packets into bigger frames)"),

            new Option(TRANSACTED, "false"),
            new Option(MSG_DURABLE, "false"),
            new Option(DURATION, "0"),
            new Option(FAILOVER_URL, "")
        ));
        translationDtestJmsMap.put("", "");

        argsAcceptingMultipleValues.addAll(Arrays.asList(MSG_CONTENT_LIST_ITEM, MSG_CONTENT_MAP_ITEM, MSG_PROPERTY));
        failoverProtocols.addAll(Arrays.asList(FAILOVER_PROTO, DISCOVERY_PROTO));
    }

    /**
     * Check whether this option is valid for client.
     *
     * @param option option name to look up in optionsMap
     * @return true, if this option is valid for given client.
     */

    public boolean isValidOption(Option option) {
        if (option == null) {
            throw new IllegalArgumentException("Argument is null!");
        }
        return defaultOptions.contains(option);
    }

    /**
     * Get defaultValue for given option.
     *
     * @param name get defaultValue for this option
     * @return Object
     */
    public abstract Option getOption(String name);

    /**
     * Method for getting default values for given client.
     *
     * @return Map of default options for given client
     */
    public abstract List<Option> getClientDefaultOptions();

    /**
     * Get list of actual/updated client options.
     *
     * @return current list of updated client options
     */
    public abstract List<Option> getClientOptions();

    public List<Option> getDefaultOptions() {
        return defaultOptions;
    }

    /**
     * Method returns the map of parsed (updated) client Options, without
     * the default options.
     * Map contains the mapping as optionName:Option.
     * OptionName keys are all defined in ClientOptions class.
     *
     * @return map of option names and client Options which has been
     * modified by the user command line input.
     */
    public Map<String, Option> getUpdatedOptionsMap() {
        Map<String, Option> updatedOptionsMap = new HashMap<>();
        for (Option option : updatedOptions) {
            updatedOptionsMap.put(option.getName(), option);
        }
        return updatedOptionsMap;
    }

    public List<Option> getUpdatedOptions() {
        return updatedOptions;
    }

    public void setUpdatedOptions(List<Option> updatedOptions) {
        this.updatedOptions = updatedOptions;
    }

    public static void addNumericArgumentValueOptionList(Option option) {
        numericArgumentValueOptionList.add(option);
    }
}

