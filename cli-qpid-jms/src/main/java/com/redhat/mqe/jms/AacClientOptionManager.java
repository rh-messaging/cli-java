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

import com.redhat.mqe.lib.ClientOptionManager;
import com.redhat.mqe.lib.ClientOptions;

import javax.inject.Inject;

public class AacClientOptionManager extends ClientOptionManager {
    {
        CONNECTION_TRANSLATION_MAP.put(AacClientOptions.CON_HEARTBEAT, "amqp.idleTimeout");
        CONNECTION_TRANSLATION_MAP.put(AacClientOptions.USERNAME, "jms.username");
        CONNECTION_TRANSLATION_MAP.put(AacClientOptions.PASSWORD, "jms.password");
        CONNECTION_TRANSLATION_MAP.put(AacClientOptions.CON_VHOST, "amqp.vhost");
        CONNECTION_TRANSLATION_MAP.put(AacClientOptions.CON_SASL_MECHS, "amqp.saslMechanisms");
        CONNECTION_TRANSLATION_MAP.put(AacClientOptions.CON_SASL_LAYER, "amqp.saslLayer");
        CONNECTION_TRANSLATION_MAP.put(AacClientOptions.CON_MAX_FRAME_SIZE, "amqp.maxFrameSize");
        CONNECTION_TRANSLATION_MAP.put(AacClientOptions.CON_DRAIN_TIMEOUT, "amqp.drainTimeout");
        CONNECTION_TRANSLATION_MAP.put(AacClientOptions.CON_CLIENTID, "jms.clientID");
        CONNECTION_TRANSLATION_MAP.put(AacClientOptions.CON_ASYNC_SEND, "jms.forceAsyncSend");
        CONNECTION_TRANSLATION_MAP.put(AacClientOptions.CON_SYNC_SEND, "jms.alwaysSyncSend");
        CONNECTION_TRANSLATION_MAP.put(AacClientOptions.CON_ASYNC_ACKS, "jms.sendAcksAsync");
        CONNECTION_TRANSLATION_MAP.put(AacClientOptions.CON_LOC_MSG_PRIO, "jms.localMessagePriority");
        CONNECTION_TRANSLATION_MAP.put(AacClientOptions.CON_VALID_PROP_NAMES, "jms.validatePropertyNames");
        CONNECTION_TRANSLATION_MAP.put(AacClientOptions.CON_RECV_LOCAL_ONLY, "jms.receiveLocalOnly");
        CONNECTION_TRANSLATION_MAP.put(AacClientOptions.CON_RECV_NOWAIT_LOCAL, "jms.receiveNoWaitLocalOnly");
        CONNECTION_TRANSLATION_MAP.put(AacClientOptions.CON_QUEUE_PREFIX, "jms.queuePrefix");
        CONNECTION_TRANSLATION_MAP.put(AacClientOptions.CON_TOPIC_PREFIX, "jms.topicPrefix");
        CONNECTION_TRANSLATION_MAP.put(AacClientOptions.CON_CLOSE_TIMEOUT, "jms.closeTimeout");
        CONNECTION_TRANSLATION_MAP.put(AacClientOptions.CON_CONN_TIMEOUT, "jms.connectTimeout");
        CONNECTION_TRANSLATION_MAP.put(AacClientOptions.CON_CLIENTID_PREFIX, "jms.clientIDPrefix");
        CONNECTION_TRANSLATION_MAP.put(AacClientOptions.CON_CONNID_PREFIX, "jms.connectionIDPrefix");
        CONNECTION_TRANSLATION_MAP.put(AacClientOptions.CON_POPULATE_JMSXUSERID, "jms.populateJMSXUserID");

        CONNECTION_TRANSLATION_MAP.put(AacClientOptions.CON_PREFETCH_QUEUE, "jms.prefetchPolicy.queuePrefetch");
        CONNECTION_TRANSLATION_MAP.put(AacClientOptions.CON_PREFETCH_TOPIC, "jms.prefetchPolicy.topicPrefetch");
        CONNECTION_TRANSLATION_MAP.put(AacClientOptions.CON_PREFETCH_BROWSER, "jms.prefetchPolicy.queueBrowserPrefetch");
        CONNECTION_TRANSLATION_MAP.put(AacClientOptions.CON_PREFETCH_DUR_TOPIC, "jms.prefetchPolicy.durableTopicPrefetch");
        CONNECTION_TRANSLATION_MAP.put(AacClientOptions.CON_PREFETCH_ALL, "jms.prefetchPolicy.all");
        CONNECTION_TRANSLATION_MAP.put(AacClientOptions.CON_MAX_REDELIVERIES, "jms.redeliveryPolicy.maxRedeliveries");

        CONNECTION_TRANSLATION_MAP.put(AacClientOptions.CON_RETRIES, "failover.maxReconnectAttempts");
        CONNECTION_TRANSLATION_MAP.put(AacClientOptions.CON_RECONNECT_TIMEOUT, "failover.reconnectDelay");
        CONNECTION_TRANSLATION_MAP.put(AacClientOptions.CON_RECONNECT_INTERVAL, "failover.maxReconnectDelay");
        CONNECTION_TRANSLATION_MAP.put(AacClientOptions.CON_RECONNECT_BACKOFF, "failover.useReconnectBackOff");
        CONNECTION_TRANSLATION_MAP.put(AacClientOptions.CON_RECONNECT_BACKOFF_MULTIPLIER, "failover.reconnectBackOffMultiplier");
        CONNECTION_TRANSLATION_MAP.put(AacClientOptions.CON_RECONNECT_START_LIMIT, "failover.startupMaxReconnectAttempts");
        CONNECTION_TRANSLATION_MAP.put(AacClientOptions.CON_RECONNECT_INITIAL_DELAY, "failover.initialReconnectDelay");
        CONNECTION_TRANSLATION_MAP.put(AacClientOptions.CON_RECONNECT_WARN_ATTEMPTS, "failover.warnAfterReconnectAttempts");

        CONNECTION_TRANSLATION_MAP.put(AacClientOptions.CON_SSL_KEYSTORE_LOC, "transport.keyStoreLocation");
        CONNECTION_TRANSLATION_MAP.put(AacClientOptions.CON_SSL_KEYSTORE_PASS, "transport.keyStorePassword");
        CONNECTION_TRANSLATION_MAP.put(AacClientOptions.CON_SSL_TRUSTSTORE_LOC, "transport.trustStoreLocation");
        CONNECTION_TRANSLATION_MAP.put(AacClientOptions.CON_SSL_TRUSTSTORE_PASS, "transport.trustStorePassword");
        CONNECTION_TRANSLATION_MAP.put(AacClientOptions.CON_SSL_STORE_TYPE, "transport.storeType");
        CONNECTION_TRANSLATION_MAP.put(AacClientOptions.CON_SSL_CONTEXT_PROTOCOL, "transport.contextProtocol");
        CONNECTION_TRANSLATION_MAP.put(AacClientOptions.CON_SSL_ENA_CIPHERED, "transport.enabledCipherSuites");
        CONNECTION_TRANSLATION_MAP.put(AacClientOptions.CON_SSL_DIS_CIPHERED, "transport.disabledCipherSuites");
        CONNECTION_TRANSLATION_MAP.put(AacClientOptions.CON_SSL_ENA_PROTOS, "transport.enabledProtocols");
        CONNECTION_TRANSLATION_MAP.put(AacClientOptions.CON_SSL_DIS_PROTOS, "transport.disabledProtocols");
        CONNECTION_TRANSLATION_MAP.put(AacClientOptions.CON_SSL_TRUST_ALL, "transport.trustAll");
        CONNECTION_TRANSLATION_MAP.put(AacClientOptions.CON_SSL_VERIFY_HOST, "transport.verifyHost");
        CONNECTION_TRANSLATION_MAP.put(AacClientOptions.CON_SSL_KEYALIAS, "transport.keyAlias");

        CONNECTION_TRANSLATION_MAP.put(AacClientOptions.CON_TCP_SEND_BUF_SIZE, "transport.sendBufferSize");
        CONNECTION_TRANSLATION_MAP.put(AacClientOptions.CON_TCP_RECV_BUF_SIZE, "transport.receiveBufferSize");
        CONNECTION_TRANSLATION_MAP.put(AacClientOptions.CON_TCP_TRAFFIC_CLASS, "transport.trafficClass");
        CONNECTION_TRANSLATION_MAP.put(AacClientOptions.CON_TCP_CON_TIMEOUT, "transport.connectTimeout");
        CONNECTION_TRANSLATION_MAP.put(AacClientOptions.CON_TCP_SOCK_TIMEOUT, "transport.soTimeout");
        CONNECTION_TRANSLATION_MAP.put(AacClientOptions.CON_TCP_SOCK_LINGER, "transport.soLinger");
        CONNECTION_TRANSLATION_MAP.put(AacClientOptions.CON_TCP_KEEP_ALIVE, "transport.tcpKeepAlive");
        CONNECTION_TRANSLATION_MAP.put(AacClientOptions.CON_TCP_NO_DELAY, "transport.tcpNoDelay");
    }

    @Inject
    AacClientOptionManager() {
    }

    @Override
    protected void createConnectionOptions(ClientOptions clientOptions) {
        if (Boolean.parseBoolean(clientOptions.getOption(ClientOptions.LOG_BYTES).getValue())) {
            connectionOptionsUrlMap.put("transport.traceBytes", "true");
        }
        super.createConnectionOptions(clientOptions);
    }

    @Override
    protected String getUrlProtocol() {
        return "amqp";
    }
}
