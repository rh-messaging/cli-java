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

package com.redhat.mqe.acc;

import com.redhat.mqe.lib.ClientOptionManager;
import com.redhat.mqe.lib.ClientOptions;

import java.util.HashMap;
import java.util.Map;

class AccClientOptionManager extends ClientOptionManager {
    private Map<String, String> CTL_SSL_OPTIONS = new HashMap<>();
    {
        // Core
        // org/apache/activemq/artemis/core/remoting/impl/netty/TransportConstants.java
        // activemq-artemis/artemis-core-client/src/main/java/org/apache/activemq/artemis/api/core/client/ActiveMQClient.java
        // artemis-core-client/org/apache/activemq/artemis/core/client/impl/ServerLocatorImpl.java
        // artemis-core-client/org/apache/activemq/artemis/core/protocol/core/impl/RemotingConnectionImpl.java
        // tests/activemq5-unit-tests/src/main/java/org/apache/activemq/ActiveMQConnectionFactory.java
        // artemis-jms-client/src/test/java/org/apache/activemq/artemis/uri/ConnectionFactoryURITest.java
        CONNECTION_TRANSLATION_MAP.put(ClientOptions.USERNAME, "jms.userName");
        CONNECTION_TRANSLATION_MAP.put(ClientOptions.PASSWORD, "jms.password");
        CONNECTION_TRANSLATION_MAP.put(ClientOptions.CON_VHOST, "");
        CONNECTION_TRANSLATION_MAP.put(ClientOptions.CON_SASL_MECHS, "");
        CONNECTION_TRANSLATION_MAP.put(ClientOptions.CON_SASL_LAYER, "");

        CONNECTION_TRANSLATION_MAP.put(ClientOptions.CON_CLIENTID, "clientID");
        CONNECTION_TRANSLATION_MAP.put(ClientOptions.CON_ASYNC_SEND, "");
        CONNECTION_TRANSLATION_MAP.put(ClientOptions.CON_SYNC_SEND, "blockOnNonDurableSend;blockOnDurableSend");
        // TODO: there is option blockOnAcknowledge, which is a negation of the following
        CONNECTION_TRANSLATION_MAP.put(ClientOptions.CON_ASYNC_ACKS, "");
        CONNECTION_TRANSLATION_MAP.put(ClientOptions.CON_LOC_MSG_PRIO, "");
        CONNECTION_TRANSLATION_MAP.put(ClientOptions.CON_VALID_PROP_NAMES, "");
        CONNECTION_TRANSLATION_MAP.put(ClientOptions.CON_QUEUE_PREFIX, "");
        CONNECTION_TRANSLATION_MAP.put(ClientOptions.CON_TOPIC_PREFIX, "");
        CONNECTION_TRANSLATION_MAP.put(ClientOptions.CON_CLOSE_TIMEOUT, "");
        CONNECTION_TRANSLATION_MAP.put(ClientOptions.CON_CONN_TIMEOUT, "");
        CONNECTION_TRANSLATION_MAP.put(ClientOptions.CON_CLIENTID_PREFIX, "");
        CONNECTION_TRANSLATION_MAP.put(ClientOptions.CON_CONNID_PREFIX, "");

        CONNECTION_TRANSLATION_MAP.put(ClientOptions.CON_PREFETCH_QUEUE, "");
        CONNECTION_TRANSLATION_MAP.put(ClientOptions.CON_PREFETCH_TOPIC, "");
        CONNECTION_TRANSLATION_MAP.put(ClientOptions.CON_PREFETCH_BROWSER, "");
        CONNECTION_TRANSLATION_MAP.put(ClientOptions.CON_PREFETCH_DUR_TOPIC, "");
        // https://access.redhat.com/solutions/1398683
        CONNECTION_TRANSLATION_MAP.put(ClientOptions.CON_PREFETCH_ALL, "consumerWindowSize");
        CONNECTION_TRANSLATION_MAP.put(ClientOptions.CON_MAX_REDELIVERIES, "jms.redeliveryPolicy.maxRedeliveries");

        CONNECTION_TRANSLATION_MAP.put(ClientOptions.CON_RETRIES, "reconnectAttempts");
        CONNECTION_TRANSLATION_MAP.put(ClientOptions.CON_RECONNECT_TIMEOUT, "maxRetryInterval");
        CONNECTION_TRANSLATION_MAP.put(ClientOptions.CON_RECONNECT_INTERVAL, "");
        CONNECTION_TRANSLATION_MAP.put(ClientOptions.CON_RECONNECT_BACKOFF, "retryIntervalMultiplier");
        CONNECTION_TRANSLATION_MAP.put(ClientOptions.CON_RECONNECT_BACKOFF_MULTIPLIER, "retryIntervalMultiplier");
        CONNECTION_TRANSLATION_MAP.put(ClientOptions.CON_RECONNECT_START_LIMIT, "");
        CONNECTION_TRANSLATION_MAP.put(ClientOptions.CON_RECONNECT_INITIAL_DELAY, "");
        CONNECTION_TRANSLATION_MAP.put(ClientOptions.CON_RECONNECT_WARN_ATTEMPTS, "");

        CONNECTION_TRANSLATION_MAP.put(ClientOptions.CON_TCP_SEND_BUF_SIZE, "tcpSendBufferSize");
        CONNECTION_TRANSLATION_MAP.put(ClientOptions.CON_TCP_RECV_BUF_SIZE, "tcpReceiveBufferSize");
        CONNECTION_TRANSLATION_MAP.put(ClientOptions.CON_TCP_TRAFFIC_CLASS, "");
        CONNECTION_TRANSLATION_MAP.put(ClientOptions.CON_TCP_CON_TIMEOUT, "connect-timeout-millis");
        CONNECTION_TRANSLATION_MAP.put(ClientOptions.CON_TCP_SOCK_TIMEOUT, "callTimeout");
        CONNECTION_TRANSLATION_MAP.put(ClientOptions.CON_TCP_SOCK_LINGER, "");
        CONNECTION_TRANSLATION_MAP.put(ClientOptions.CON_TCP_KEEP_ALIVE, "");

        CONNECTION_TRANSLATION_MAP.put(ClientOptions.CONN_CACHE_ENA, "");
        CONNECTION_TRANSLATION_MAP.put(ClientOptions.CONN_CACHE_SIZE, "");
        CONNECTION_TRANSLATION_MAP.put(ClientOptions.CON_HEARTBEAT, "connectionTTL");
        CONNECTION_TRANSLATION_MAP.put(ClientOptions.CONN_MAX_INACTITVITY_DUR, "");
        CONNECTION_TRANSLATION_MAP.put(ClientOptions.CONN_MAX_FRAME_SIZE, "");
        CONNECTION_TRANSLATION_MAP.put(ClientOptions.CONN_PREFIX_PACKET_SIZE_ENA, "");
        CONNECTION_TRANSLATION_MAP.put(ClientOptions.CONN_SERVER_STACK_TRACE_ENA, "");
        CONNECTION_TRANSLATION_MAP.put(ClientOptions.CONN_TCP_NO_DELAY, "");
        CONNECTION_TRANSLATION_MAP.put(ClientOptions.CONN_TIGHT_ENCODING_ENA, "");

        // activemq-artemis/examples/features/standard/ssl-enabled-dual-authentication/src/main/resources/jndi.properties
        CTL_SSL_OPTIONS.put(ClientOptions.CON_SSL_KEYSTORE_LOC, "keyStorePath");
        CTL_SSL_OPTIONS.put(ClientOptions.CON_SSL_KEYSTORE_PASS, "keyStorePassword");
        CTL_SSL_OPTIONS.put(ClientOptions.CON_SSL_TRUSTSTORE_LOC, "trustStorePath");
        CTL_SSL_OPTIONS.put(ClientOptions.CON_SSL_TRUSTSTORE_PASS, "trustStorePassword");
        CTL_SSL_OPTIONS.put(ClientOptions.CON_SSL_STORE_TYPE, "keyStoreProvider");
        CTL_SSL_OPTIONS.put(ClientOptions.CON_SSL_CONTEXT_PROTOCOL, "protocols");
        CTL_SSL_OPTIONS.put(ClientOptions.CON_SSL_ENA_CIPHERED, "enabledCipherSuites");
        CTL_SSL_OPTIONS.put(ClientOptions.CON_SSL_DIS_CIPHERED, "");
        CTL_SSL_OPTIONS.put(ClientOptions.CON_SSL_ENA_PROTOS, "enabledProtocols");
        CTL_SSL_OPTIONS.put(ClientOptions.CON_SSL_DIS_PROTOS, "");
        CTL_SSL_OPTIONS.put(ClientOptions.CON_SSL_TRUST_ALL, "");
        CTL_SSL_OPTIONS.put(ClientOptions.CON_SSL_VERIFY_HOST, "verifyHost");
        CONNECTION_TRANSLATION_MAP.putAll(CTL_SSL_OPTIONS);
    }

    @Override
    protected void createConnectionOptions(ClientOptions clientOptions) {
        if (Boolean.parseBoolean(clientOptions.getOption(ClientOptions.CON_RECONNECT).getValue())) {
            // https://activemq.apache.org/artemis/docs/2.1.0/client-reconnection.html
            // https://activemq.apache.org/artemis/docs/2.1.0/ha.html
            connectionOptionsUrlMap.put("reconnectAttempts", "-1"); // unlimited
            connectionOptionsUrlMap.put("initialConnectAttempts", "-1"); // unlimited
        }
        if (clientOptions.getOption(ClientOptions.CON_HEARTBEAT).hasParsedValue()) {
            final float value = Float.parseFloat(clientOptions.getOption(ClientOptions.CON_HEARTBEAT).getValue());
            connectionOptionsUrlMap.put("clientFailureCheckPeriod", String.valueOf(Math.round(value / 2.0 * 1000)));
            connectionOptionsUrlMap.put("callTimeout", String.valueOf(Math.round(value * 2 * 1000)));
        }
        // set CON_SSL_ENA if at least one other ssl option is specified
        for (String sslOption : CTL_SSL_OPTIONS.keySet()) {
            if (clientOptions.getOption(sslOption).hasParsedValue()) {
                connectionOptionsUrlMap.put("sslEnabled", "true");
                break;
            }
        }
        super.createConnectionOptions(clientOptions);
    }
}
