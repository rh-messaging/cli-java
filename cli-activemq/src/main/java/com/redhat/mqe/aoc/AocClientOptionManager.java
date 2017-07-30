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

import com.redhat.mqe.lib.ClientOptionManager;
import com.redhat.mqe.lib.ClientOptions;

import java.nio.file.Paths;

class AocClientOptionManager extends ClientOptionManager {
    {
        // OpenWire
        // http://activemq.apache.org/activemq-connection-uris.html
        // http://activemq.apache.org/configuring-wire-formats.html
        CONNECTION_TRANSLATION_MAP.put(ClientOptions.USERNAME, "jms.userName");
        CONNECTION_TRANSLATION_MAP.put(ClientOptions.PASSWORD, "jms.password");
        CONNECTION_TRANSLATION_MAP.put(ClientOptions.CON_VHOST, "");
        CONNECTION_TRANSLATION_MAP.put(ClientOptions.CON_SASL_MECHS, "");
        CONNECTION_TRANSLATION_MAP.put(ClientOptions.CON_SASL_LAYER, "");

        // http://activemq.apache.org/connection-configuration-uri.html
        CONNECTION_TRANSLATION_MAP.put(ClientOptions.CON_CLIENTID, "jms.clientID");
        CONNECTION_TRANSLATION_MAP.put(ClientOptions.CON_ASYNC_SEND, "jms.useAsyncSend");
        CONNECTION_TRANSLATION_MAP.put(ClientOptions.CON_SYNC_SEND, "jms.alwaysSyncSend");
        CONNECTION_TRANSLATION_MAP.put(ClientOptions.CON_ASYNC_ACKS, "");
        CONNECTION_TRANSLATION_MAP.put(ClientOptions.CON_LOC_MSG_PRIO, "");
        CONNECTION_TRANSLATION_MAP.put(ClientOptions.CON_VALID_PROP_NAMES, "");
        CONNECTION_TRANSLATION_MAP.put(ClientOptions.CON_QUEUE_PREFIX, "");
        CONNECTION_TRANSLATION_MAP.put(ClientOptions.CON_TOPIC_PREFIX, "");
        CONNECTION_TRANSLATION_MAP.put(ClientOptions.CON_CLOSE_TIMEOUT, "jms.closeTimeout");
        CONNECTION_TRANSLATION_MAP.put(ClientOptions.CON_CONN_TIMEOUT, "");
        CONNECTION_TRANSLATION_MAP.put(ClientOptions.CON_CLIENTID_PREFIX, "");
        CONNECTION_TRANSLATION_MAP.put(ClientOptions.CON_CONNID_PREFIX, "");
        // http://activemq.apache.org/what-is-the-prefetch-limit-for.html
        CONNECTION_TRANSLATION_MAP.put(ClientOptions.CON_PREFETCH_QUEUE, "jms.prefetchPolicy.queuePrefetch");
        CONNECTION_TRANSLATION_MAP.put(ClientOptions.CON_PREFETCH_TOPIC, "jms.prefetchPolicy.topicPrefetch");
        CONNECTION_TRANSLATION_MAP.put(ClientOptions.CON_PREFETCH_BROWSER, "jms.prefetchPolicy.queueBrowserPrefetch");
        CONNECTION_TRANSLATION_MAP.put(ClientOptions.CON_PREFETCH_DUR_TOPIC, "jms.prefetchPolicy.durableTopicPrefetch");
        CONNECTION_TRANSLATION_MAP.put(ClientOptions.CON_PREFETCH_ALL, "jms.prefetchPolicy.all");
        // http://activemq.apache.org/redelivery-policy.html
        CONNECTION_TRANSLATION_MAP.put(ClientOptions.CON_MAX_REDELIVERIES, "jms.redeliveryPolicy.maximumRedeliveries");

        // http://activemq.apache.org/failover-transport-reference.html
        CONNECTION_TRANSLATION_MAP.put(ClientOptions.CON_RETRIES, "maxReconnectAttempts");
        CONNECTION_TRANSLATION_MAP.put(ClientOptions.CON_RECONNECT_TIMEOUT, "timeout");
        CONNECTION_TRANSLATION_MAP.put(ClientOptions.CON_RECONNECT_INTERVAL, "maxReconnectDelay");
        CONNECTION_TRANSLATION_MAP.put(ClientOptions.CON_RECONNECT_BACKOFF, "useExponentialBackOff");
        CONNECTION_TRANSLATION_MAP.put(ClientOptions.CON_RECONNECT_BACKOFF_MULTIPLIER, "reconnectDelayExponent");
        CONNECTION_TRANSLATION_MAP.put(ClientOptions.CON_RECONNECT_START_LIMIT, "startupMaxReconnectAttempts");
        CONNECTION_TRANSLATION_MAP.put(ClientOptions.CON_RECONNECT_INITIAL_DELAY, "initialReconnectDelay");
        CONNECTION_TRANSLATION_MAP.put(ClientOptions.CON_RECONNECT_WARN_ATTEMPTS, "warnAfterReconnectAttempts");

        CONNECTION_TRANSLATION_MAP.put(ClientOptions.CON_SSL_KEYSTORE_LOC, null);
        CONNECTION_TRANSLATION_MAP.put(ClientOptions.CON_SSL_KEYSTORE_PASS, null);
        CONNECTION_TRANSLATION_MAP.put(ClientOptions.CON_SSL_TRUSTSTORE_LOC, null);
        CONNECTION_TRANSLATION_MAP.put(ClientOptions.CON_SSL_TRUSTSTORE_PASS, null);
        CONNECTION_TRANSLATION_MAP.put(ClientOptions.CON_SSL_STORE_TYPE, null);
        CONNECTION_TRANSLATION_MAP.put(ClientOptions.CON_SSL_CONTEXT_PROTOCOL, "");
        CONNECTION_TRANSLATION_MAP.put(ClientOptions.CON_SSL_ENA_CIPHERED, "");
        CONNECTION_TRANSLATION_MAP.put(ClientOptions.CON_SSL_DIS_CIPHERED, "");
        CONNECTION_TRANSLATION_MAP.put(ClientOptions.CON_SSL_ENA_PROTOS, "");
        CONNECTION_TRANSLATION_MAP.put(ClientOptions.CON_SSL_DIS_PROTOS, "");
        CONNECTION_TRANSLATION_MAP.put(ClientOptions.CON_SSL_TRUST_ALL, "");
        CONNECTION_TRANSLATION_MAP.put(ClientOptions.CON_SSL_VERIFY_HOST, "");
        CONNECTION_TRANSLATION_MAP.put(ClientOptions.CON_SSL_KEYALIAS, "");

        // http://activemq.apache.org/tcp-transport-reference.html
        CONNECTION_TRANSLATION_MAP.put(ClientOptions.CON_TCP_SEND_BUF_SIZE, "socketBufferSize");
        CONNECTION_TRANSLATION_MAP.put(ClientOptions.CON_TCP_RECV_BUF_SIZE, "socketBufferSize");
        CONNECTION_TRANSLATION_MAP.put(ClientOptions.CON_TCP_TRAFFIC_CLASS, "trafficClass");
        CONNECTION_TRANSLATION_MAP.put(ClientOptions.CON_TCP_CON_TIMEOUT, "connectionTimeout");
        CONNECTION_TRANSLATION_MAP.put(ClientOptions.CON_TCP_SOCK_TIMEOUT, "soTimeout");
        CONNECTION_TRANSLATION_MAP.put(ClientOptions.CON_TCP_SOCK_LINGER, "soLinger");
        CONNECTION_TRANSLATION_MAP.put(ClientOptions.CON_TCP_KEEP_ALIVE, "keepAlive");

        // http://activemq.apache.org/configuring-wire-formats.html
        CONNECTION_TRANSLATION_MAP.put(ClientOptions.CONN_CACHE_ENA, "wireFormat.cacheEnabled");
        CONNECTION_TRANSLATION_MAP.put(ClientOptions.CONN_CACHE_SIZE, "wireFormat.cacheSize");
        CONNECTION_TRANSLATION_MAP.put(ClientOptions.CON_HEARTBEAT, "wireFormat.maxInactivityDuration");
        CONNECTION_TRANSLATION_MAP.put(ClientOptions.CONN_MAX_INACTITVITY_DUR, "wireFormat.maxInactivityDuration");
        CONNECTION_TRANSLATION_MAP.put(ClientOptions.CONN_MAX_INACTITVITY_DUR_INIT_DELAY, "wireFormat.maxInactivityDurationInitalDelay");
        CONNECTION_TRANSLATION_MAP.put(ClientOptions.CONN_MAX_FRAME_SIZE, "wireFormat.maxFrameSize");
        CONNECTION_TRANSLATION_MAP.put(ClientOptions.CONN_PREFIX_PACKET_SIZE_ENA, "wireFormat.prefixPacketSize");
        CONNECTION_TRANSLATION_MAP.put(ClientOptions.CONN_SERVER_STACK_TRACE_ENA, "wireFormat.stackTraceEnabled");
        CONNECTION_TRANSLATION_MAP.put(ClientOptions.CONN_TCP_NO_DELAY, "wireFormat.tcpNoDelayEnabled");
        CONNECTION_TRANSLATION_MAP.put(ClientOptions.CONN_TIGHT_ENCODING_ENA, "wireFormat.tightEncodingEnabled");
    }

    protected void setBrokerOptions(ClientOptions clientOptions, String brokerUrl) {
        // Intercept setBrokerOptions and configure reconnect for the Aoc client

        String uriProtocol = null;
        if (Boolean.parseBoolean(clientOptions.getOption(ClientOptions.CON_RECONNECT).getValue())) {
            // use failover mechanism by default, discovery otherwise
            uriProtocol = ClientOptions.FAILOVER_PROTO;
        }
        if (uriProtocol != null) {
            checkAndSetOption(ClientOptions.PROTOCOL, uriProtocol, clientOptions);
            // Set the whole url as failoverUrl. Do not parse it. connection options should come as input "conn-*"
            //TODO if missing protocol, add amqp by default
            // failover:(dhcp-75-212.lab.eng.brq.redhat.com:5672,dhcp-75-219.lab.eng.brq.redhat.com:5672) -->
            // failover:(tcp://dhcp-75-212.lab.eng.brq.redhat.com:5672,tcp://dhcp-75-219.lab.eng.brq.redhat.com:5672) -->
            brokerUrl = appendMissingProtocol(brokerUrl);
            checkAndSetOption(ClientOptions.FAILOVER_URL, brokerUrl, clientOptions);
        } else {
            super.setBrokerOptions(clientOptions, brokerUrl);
        }
    }

    @Override
    protected void createConnectionOptions(ClientOptions clientOptions) {
        // Configure SSL options, which in case of activemq-client are set as Java properties
        // http://activemq.apache.org/how-do-i-use-ssl.html
        // https://docs.oracle.com/javase/8/docs/technotes/guides/security/jsse/JSSERefGuide.html#CustomizingStores

        if (clientOptions.getOption(ClientOptions.CON_SSL_KEYSTORE_LOC).hasParsedValue()) {
            System.setProperty("javax.net.ssl.keyStore", relativize(clientOptions.getOption(ClientOptions.CON_SSL_KEYSTORE_LOC).getValue()));
        }
        if (clientOptions.getOption(ClientOptions.CON_SSL_KEYSTORE_PASS).hasParsedValue()) {
            System.setProperty("javax.net.ssl.keyStorePassword", clientOptions.getOption(ClientOptions.CON_SSL_KEYSTORE_PASS).getValue());
        }
//        System.setProperty("javax.net.ssl.keyStorePassword", "secureexample");
        if (clientOptions.getOption(ClientOptions.CON_SSL_TRUSTSTORE_LOC).hasParsedValue()) {
            System.setProperty("javax.net.ssl.trustStore", relativize(clientOptions.getOption(ClientOptions.CON_SSL_TRUSTSTORE_LOC).getValue()));
        }
        if (clientOptions.getOption(ClientOptions.CON_SSL_TRUSTSTORE_PASS).hasParsedValue()) {
            System.setProperty("javax.net.ssl.trustStorePassword", clientOptions.getOption(ClientOptions.CON_SSL_TRUSTSTORE_PASS).getValue());
        }
        if (clientOptions.getOption(ClientOptions.CON_SSL_STORE_TYPE).hasParsedValue()) {
            System.setProperty("javax.net.ssl.keyStoreType", clientOptions.getOption(ClientOptions.CON_SSL_STORE_TYPE).getValue());
            System.setProperty("javax.net.ssl.trustStoreType", clientOptions.getOption(ClientOptions.CON_SSL_STORE_TYPE).getValue());
        }

        super.createConnectionOptions(clientOptions);
    }

    private String relativize(String p) {
        // this may not be necessary step, although ActiveMQ doc says it is
        return Paths.get("").toAbsolutePath().relativize(Paths.get(p).toAbsolutePath()).toString();
    }
}
