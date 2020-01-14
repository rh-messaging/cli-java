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

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Interface provides support for manipulating
 * with default options for clients.
 */
public abstract class ClientOptions {

    @Inject
        // TODO(jdanek): this initialization is confusing, admittedly
    void setOptions(ClientOptionManager clientOptionManager, @Args String[] args) {
        clientOptionManager.applyClientArguments(this, args);
    }

    protected static final Logger LOG = LoggerFactory.getLogger(ReceiverOptions.class);
    protected static final String OUT = "out";
    private static final Map<String, String> translationDtestJmsMap = new HashMap<String, String>();
    public static final String CON_SSL_ENA = "conn-ssl-ena";
    private final List<Option> defaultOptions = new ArrayList<>();
    private List<Option> updatedOptions = new ArrayList<>();
    static List<String> argsAcceptingMultipleValues = new ArrayList<>();
    static List<String> failoverProtocols = new ArrayList<>();
    static List<Option> numericArgumentValueOptionList = new ArrayList<>();

    /* Mapping of client option to jms client is done in CoreClient.CONNECTION_TRANSLATION_MAP */
    public static final String BROKER = "broker";
    public static final String BROKER_URI = "broker-uri";
    static final String TRANSACTED = "transacted";
    public static final String MSG_DURABLE = "msg-durable";
    public static final String ADDRESS = "address";
    public static final String DURATION = "duration";
    public static final String DURATION_MODE = "duration-mode";
    static final String LOG_LEVEL = "log-lib";
    static final String LOG_STATS = "log-stats";
    public static final String LOG_BYTES = "log-bytes";
    public static final String USERNAME = "conn-username";                               // jms.username
    public static final String PASSWORD = "conn-password";                               // jms.password
    static final String HELP = "help";
    static final String SSN_ACK_MODE = "ssn-ack-mode";
    public static final String CLOSE_SLEEP = "close-sleep";

    public static final String CON_HEARTBEAT = "conn-heartbeat";                         // amqp.idleTimeout=[ms] * 1000ms
    public static final String CON_VHOST = "conn-vhost";                                 // amqp.vhost
    public static final String CON_SASL_MECHS = "conn-auth-mechanisms";                  // amqp.saslMechanisms
    public static final String CON_SASL_LAYER = "conn-auth-sasl";                        // amqp.saslLayer

    public static final String CON_CLIENTID = "conn-clientid";                           // jms.clientID
    public static final String CON_ASYNC_SEND = "conn-async-send";                       // jms.forceAsyncSend
    public static final String CON_SYNC_SEND = "conn-sync-send";                         // jms.alwaysSyncSend
    public static final String CON_ASYNC_ACKS = "conn-async-acks";                       // jms.sendAcksAsync
    public static final String CON_LOC_MSG_PRIO = "conn-local-msg-priority";             // jms.localMessagePriority
    public static final String CON_VALID_PROP_NAMES = "conn-valid-prop-names";           // jms.validatePropertyNames
    public static final String CON_QUEUE_PREFIX = "conn-queue-prefix";                   // jms.queuePrefix
    public static final String CON_TOPIC_PREFIX = "conn-topic-prefix";                   // jms.topicPrefix
    public static final String CON_CLOSE_TIMEOUT = "conn-close-timeout";                 // jms.closeTimeout
    public static final String CON_CONN_TIMEOUT = "conn-conn-timeout";                   // jms.connectTimeout
    public static final String CON_CLIENTID_PREFIX = "conn-clientid-prefix";             // jms.clientIDPrefix
    public static final String CON_CONNID_PREFIX = "conn-connid-prefix";                 // jms.connectionIDPrefix

    public static final String CON_PREFETCH_QUEUE = "conn-prefetch-queue";               // jms.prefetchPolicy.queuePrefetch
    public static final String CON_PREFETCH_TOPIC = "conn-prefetch-topic";               // jms.prefetchPolicy.topicPrefetch
    public static final String CON_PREFETCH_BROWSER = "conn-prefetch-browser";           // jms.prefetchPolicy.queueBrowserPrefetch
    public static final String CON_PREFETCH_DUR_TOPIC = "conn-prefetch-topic-dur";       // jms.prefetchPolicy.durableTopicPrefetch
    public static final String CON_PREFETCH_ALL = "conn-prefetch";                       // jms.prefetchPolicy.all

    public static final String CON_MAX_REDELIVERIES = "conn-redeliveries-max";           // jms.redeliveryPolicy.maxRedeliveries

    public static final String CON_TCP_SEND_BUF_SIZE = "conn-tcp-buf-size-send";         // transport.sendBufferSize
    public static final String CON_TCP_RECV_BUF_SIZE = "conn-tcp-buf-size-recv";         // transport.receiveBufferSize
    public static final String CON_TCP_TRAFFIC_CLASS = "conn-tcp-traffic-class";         // transport.trafficClass
    public static final String CON_TCP_CON_TIMEOUT = "conn-tcp-conn-timeout";            // transport.connectTimeout
    public static final String CON_TCP_SOCK_TIMEOUT = "conn-tcp-sock-timeout";           // transport.soTimeout
    public static final String CON_TCP_SOCK_LINGER = "conn-tcp-sock-linger";             // transport.soLinger
    public static final String CON_TCP_KEEP_ALIVE = "conn-tcp-keep-alive";               // transport.tcpKeepAlive

    public static final String CON_RECONNECT = "conn-reconnect";                         // enable reconnect options
    public static final String CON_RECONNECT_INITIAL_DELAY = "conn-reconnect-initial-delay"; // failover.initialReconnectDelay (0)
    public static final String CON_RECONNECT_TIMEOUT = "conn-reconnect-timeout";         // failover.reconnectDelay (10ms)
    public static final String CON_RECONNECT_INTERVAL = "conn-reconnect-interval";       // failover.maxReconnectDelay (30sec)
    public static final String CON_RECONNECT_BACKOFF = "conn-reconnect-backoff";         // failover.useReconnectBackOff (true)
    public static final String CON_RECONNECT_BACKOFF_MULTIPLIER = "conn-reconnect-backoff-multiplier"; // failover.reconnectBackOffMultiplier
    public static final String CON_RETRIES = "conn-reconnect-limit";                     // failover.maxReconnectAttempts (-1)
    public static final String CON_RECONNECT_START_LIMIT = "conn-reconnect-start-limit"; // failover.startupMaxReconnectAttempts
    public static final String CON_RECONNECT_WARN_ATTEMPTS = "conn-reconnect-warn-attempts"; // failover.warnAfterReconnectAttempts
    public static final String CON_FAILOVER_URLS = "conn-urls";                         // additional brokers in broker_list url

    // acc Core JMS specific options
    public static final String CON_HA = "conn-ha";                                       // ha
    public static final String CON_RECONNECT_ON_SHUTDOWN = "conn-reconnect-on-shutdown"; // failoverOnServerShutdown


    public static final String CON_SSL_KEYSTORE_LOC = "conn-ssl-keystore-location";      // transport.keyStoreLocation
    public static final String CON_SSL_KEYSTORE_PASS = "conn-ssl-keystore-password";     // transport.keyStorePassword
    public static final String CON_SSL_TRUSTSTORE_LOC = "conn-ssl-truststore-location";  // transport.trustStoreLocation
    public static final String CON_SSL_TRUSTSTORE_PASS = "conn-ssl-truststore-password"; // transport.trustStorePassword
    public static final String CON_SSL_STORE_TYPE = "conn-ssl-store-type";               // transport.storeType
    public static final String CON_SSL_CONTEXT_PROTOCOL = "conn-ssl-context-proto";      // transport.contextProtocol
    public static final String CON_SSL_ENA_CIPHERED = "conn-ssl-ena-ciphered-suites";    // transport.enabledCipherSuites
    public static final String CON_SSL_DIS_CIPHERED = "conn-ssl-dis-ciphered-suites";    // transport.disabledCipherSuites
    public static final String CON_SSL_ENA_PROTOS = "conn-ssl-ena-protos";               // transport.enabledProtocols
    public static final String CON_SSL_DIS_PROTOS = "conn-ssl-dis-protos";               // transport.disabledProtocols
    public static final String CON_SSL_TRUST_ALL = "conn-ssl-trust-all";                 // transport.trustAll
    public static final String CON_SSL_VERIFY_HOST = "conn-ssl-verify-host";             // transport.verifyHost
    public static final String CON_SSL_KEYALIAS = "conn-ssl-key-alias";                  // transport.keyAlias


    // OpenWire
    public static final String CONN_CACHE_ENA = "conn-cache-ena";                        // wireFormat.cacheEnabled
    public static final String CONN_CACHE_SIZE = "conn-cache-size";                      // wireFormat.cacheSize
    public static final String CONN_MAX_INACTITVITY_DUR = "conn-max-inactivity-dur";     // wireFormat.maxInactivityDuration
    public static final String CONN_MAX_INACTITVITY_DUR_INIT_DELAY = "conn-max-inactivity-dur-init-delay";  // wireFormat.maxInactivityDurationInitalDelay
    public static final String CONN_MAX_FRAME_SIZE = "conn-max-frame-size";               // wireFormat.maxFrameSize
    public static final String CONN_PREFIX_PACKET_SIZE_ENA = "conn-prefix-packet-size-ena";    // wireFormat.prefixPacketSize
    public static final String CONN_SERVER_STACK_TRACE_ENA = "conn-server-stack-trace-ena";    // wireFormat.stackTraceEnabled
    public static final String CONN_TCP_NO_DELAY = "conn-tcp-no-delay";                   // wireFormat.tcpNoDelayEnabled
    public static final String CONN_TIGHT_ENCODING_ENA = "conn-tight-encoding-ena";      // wireFormat.tightEncodingEnabled
    public static final String CONN_WATCH_TOPIC_ADVISORIES = "conn-watch-topic-advisories";

    // TODO Not implemented by client libraries
//  static final String CON_SSL_PROTOCOL = "conn-ssl-protocol";

    // These few options are not settable from outside. Client sets them up when setting parsed options.
    public static final String PROTOCOL = "protocol";
    static final String BROKER_HOST = "broker_host";
    static final String BROKER_PORT = "broker_port";
    public static final String BROKER_OPTIONS = "broker_options";
    public static final String DESTINATION_TYPE = "destination_type";
    public static final String FAILOVER_PROTO = "failover";
    static final String DISCOVERY_PROTO = "discovery";
    public static final String FAILOVER_URL = "failover_url";

    /**
     * S+R+?
     */
    static final String TIMEOUT = "timeout";
    public static final String COUNT = "count";
    static final String LOG_MSGS = "log-msgs";
    public static final String TX_SIZE = "tx-size";
    public static final String TX_ACTION = "tx-action";
    public static final String TX_ENDLOOP_ACTION = "tx-endloop-action";
    static final String CAPACITY = "capacity";
    public static final String TRACE_MESSAGES = "trace-messages";

    public static final String MSG_CONTENT_HASHED = "msg-content-hashed";
    public static final String MSG_CONTENT_STREAM = "msg-content-stream";

    public static final String CONN_USE_CONFIG_FILE = "conn-use-config-file";

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
    public static final String BROWSER = "recv-browse";
    static final String PROCESS_REPLY_TO = "process-reply-to";
    static final String MSG_BINARY_CONTENT_TO_FILE = "msg-binary-content-to-file";
    public static final String MSG_CONTENT_TO_FILE = "msg-content-to-file";

    /**
     * SENDER
     */
    public static final String MSG_TTL = "msg-ttl";
    public static final String MSG_PRIORITY = "msg-priority";
    public static final String MSG_ID = "msg-id";
    public static final String MSG_REPLY_TO = "msg-reply-to";
    public static final String MSG_SUBJECT = "msg-subject";
    public static final String MSG_USER_ID = "msg-user-id";
    public static final String MSG_CORRELATION_ID = "msg-correlation-id";
    static final String MSG_NOTIMESTAMP = "msg-no-timestamp";

    public static final String PROPERTY_TYPE = "property-type";
    public static final String MSG_PROPERTY = "msg-property";
    public static final String CONTENT_TYPE = "content-type";
    public static final String MSG_CONTENT = "msg-content";
    public static final String MSG_CONTENT_BINARY = "msg-content-binary";
    public static final String MSG_CONTENT_TYPE = "msg-content-type";
    public static final String MSG_CONTENT_FROM_FILE = "msg-content-from-file";
    public static final String MSG_CONTENT_MAP_ITEM = "msg-content-map-item";
    public static final String MSG_CONTENT_LIST_ITEM = "msg-content-list-item";

    public static final String MSG_GROUP_ID = "msg-group-id";
    public static final String MSG_GROUP_SEQ = "msg-group-seq";
    public static final String MSG_REPLY_TO_GROUP_ID = "msg-reply-to-group-id";

    public static final String ON_RELEASE = "on-release";

    /**
     * CONNECTOR
     */
    static final String OBJ_CTRL = "obj-ctrl";
    static final String Q_COUNT = "q-count";

    /**
     * QMF Options?
     * TODO qmf options
     */

    static {
        translationDtestJmsMap.put("", "");
        argsAcceptingMultipleValues.addAll(Arrays.asList(MSG_CONTENT_LIST_ITEM, MSG_CONTENT_MAP_ITEM, MSG_PROPERTY));
        failoverProtocols.addAll(Arrays.asList(FAILOVER_PROTO, DISCOVERY_PROTO));
    }

    public ClientOptions() {
        defaultOptions.addAll(Arrays.asList(// Options updated by parsing broker/broker-url
            new Option(PROTOCOL, "tcp"),
            new Option(BROKER_HOST, "localhost"),
            new Option(BROKER_PORT, "61616"),
            new Option(BROKER_OPTIONS, ""),
            new Option(DESTINATION_TYPE, ConnectionManager.QUEUE_OBJECT),

            new Option(USERNAME, "", "USERNAME", "", "jms.username (not defined before host:port in qpid-jms-client)"),
            new Option(PASSWORD, "", "PASSWORD", "", "jms.password (not defined before host:port in qpid-jms-client)"),
            new Option(HELP, "h", "", "", "show this help"),
            new Option(BROKER, "b", "HOST:61616", "tcp://localhost:61616", "url broker to connect to, default"),
            new Option(BROKER_URI, "", "OpenwireJmsURL", "tcp://localhost:61616[[?conOpt=val]&conOpt=val]",
                "AMQP JMS QPID specific broker uri. NOTE: This options overrides everything related to broker & connection options. " +
                    "It is used as exactly as provided!"),
            new Option(LOG_LEVEL, "", "LEVEL", "info", "logging level of the client. trace/debug/info/warn/error"),
            new Option(LOG_STATS, "", "LEVEL", "INFO", "report various statistic/debug information"), // ?
            new Option(SSN_ACK_MODE, "", "ACKMODE", "auto", "session acknowledge mode auto/client/dups_ok/(individual)"),
            new Option(CLOSE_SLEEP, "", "CSLEEP", "0", "sleep before publisher/subscriber/session/connection.close() in floating seconds"),

            new Option(CON_HEARTBEAT, "", "SECONDS", "", "mapped to " + CONN_MAX_INACTITVITY_DUR + " option"),
            new Option(CON_VHOST, "", "VHOST", "", "virtual hostname to connect to. (Default: main from URI)"),
            new Option(CON_SASL_LAYER, "", "ENABLED", "true", "choose whether SASL layer should be used"),
            new Option(CON_SASL_MECHS, "", "MECHS", "all", "comma separated list of SASL mechanisms allowed by client for authentication (plain/anonymous/external/cram-md5?/digest-md5?)"),

            new Option(CON_CLIENTID, "", "CLIENTID", "", "clientID value that is applied to the connection."),
            new Option(CON_ASYNC_SEND, "", "ENABLED", "false", "send all messages asynchronously (if false only non-persistent and transacted are send asynchronously"),
            new Option(CON_SYNC_SEND, "", "ENABLED", "false", "send all messages synchronously"),
            new Option(CON_ASYNC_ACKS, "", "ENABLED", "false", "causes all Message acknowledgments to be sent asynchronously"),
            new Option(CON_LOC_MSG_PRIO, "", "ENABLED", "false", "prefetched messages are reordered locally based on their given priority"),
            new Option(CON_VALID_PROP_NAMES, "", "ENABLED", "true", "message property names should be validated as valid Java identifiers"),
            new Option(CON_QUEUE_PREFIX, "", "PREFIX", "", "optional prefix value added to the name of any Queue created from a JMS Session"),
            new Option(CON_TOPIC_PREFIX, "", "PREFIX", "", "optional prefix value added to the name of any Topic created from a JMS Session."),
            new Option(CON_CLOSE_TIMEOUT, "", "TIMEOUT", "15", "timeout value that controls how long the client waits on Connection close before returning"),
            new Option(CON_CONN_TIMEOUT, "", "TIMEOUT", "15", "timeout value that controls how long the client waits on Connection establishment before returning with an error"),
            new Option(CON_CLIENTID_PREFIX, "", "PREFIX", "ID:", "client ID prefix for new connections"),
            new Option(CON_CONNID_PREFIX, "", "PREFIX", "ID:", "connection ID prefix used for a new connections. Usable for tracking connections in logs"),

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
            new Option(CON_HA, "", "ENABLED", "false", "whether connecting broker is in HA topology or not"),
            new Option(CON_RECONNECT_ON_SHUTDOWN, "", "ENABLED", "true", "whether to failover on live broker shutdown in HA"),
            new Option(CON_FAILOVER_URLS, "", "BROKER_URL", "", "additional brokers to add to broker-url as CSV"),

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
            new Option(CONN_TCP_NO_DELAY, "", "ENABLED", "true", "use tcp_nodelay (automatic concatenation of small packets into bigger frames)"),

            new Option(TRANSACTED, "", "ENABLED", "false", "whether the session is transacted or not"),
            new Option(MSG_DURABLE, "false"),
            new Option(DURATION, "0"),
            new Option(FAILOVER_URL, ""),

            new Option(MSG_CONTENT_HASHED, "", "", "false", "print the message content as a hash (SHA1)"),
            new Option(MSG_CONTENT_STREAM, "", "", "false", "should the message be streamed, must be used with binary content on sender"),

            new Option(CONN_USE_CONFIG_FILE, "", "jndi.properties", "false", "configure connection from JNDI .properties file"),

            new Option(TRACE_MESSAGES, "", "false", "false", "sender|receiver will send traces to Jaeger agent on localhost"),

            // OpenWire
            new Option(CONN_SERVER_STACK_TRACE_ENA, "", "ENABLED", "true", "should the stack trace of exception that occur on the broker be sent to the client?"),
            // tcpNoDelayEnabled (default true) is CONN_TCP_NO_DELAY
            new Option(CONN_CACHE_ENA, "", "ENABLED", "true", "should commonly repeated values be cached so that less marshalling occurs?"),
            new Option(CONN_TIGHT_ENCODING_ENA, "", "ENABLED", "true", "should wire size be optimized over CPU usage ?"),
            new Option(CONN_PREFIX_PACKET_SIZE_ENA, "", "ENABLED", "true", "Should the size of the packet be prefixed before each packet is marshalled?"),
            // maxInactivityDuration (ACtiveMQ default 30000ms) is CON_TCP_SOCK_TIMEOUT
            new Option(CONN_MAX_INACTITVITY_DUR, "", "MS", "30000", "The maximum inactivity duration (after which the connection is considered dead) in seconds"),
            new Option(CONN_MAX_INACTITVITY_DUR_INIT_DELAY, "", "MS", "10000", "initial delay before starting the maximum inactivity checks"),
            new Option(CONN_CACHE_SIZE, "", "if CM_ENA=true", "1024", "if cacheEnabled is true, then this specifies the maximum number of values to cached. This property was added in ActiveMQ 4.1"),
            new Option(CONN_MAX_FRAME_SIZE, "", "MAX_LONG", String.valueOf(Long.MAX_VALUE), "Maximum frame size that can be sent. Can help help prevent OOM DOS attacks"),
            new Option(CONN_WATCH_TOPIC_ADVISORIES, "", "ENABLED", String.valueOf(true), "Whether to attach to OpenWire topic advisory addresses")
        ));
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

