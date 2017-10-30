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

import javax.inject.Inject;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

public class AocClientOptionManager extends ClientOptionManager {
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
        CONNECTION_TRANSLATION_MAP.put(ClientOptions.CON_SSL_TRUST_ALL, null);
        CONNECTION_TRANSLATION_MAP.put(ClientOptions.CON_SSL_VERIFY_HOST, "verifyHostName");
        CONNECTION_TRANSLATION_MAP.put(ClientOptions.CON_SSL_KEYALIAS, "");

        // http://activemq.apache.org/tcp-transport-reference.html
        CONNECTION_TRANSLATION_MAP.put(ClientOptions.CON_TCP_SEND_BUF_SIZE, "socketBufferSize");
        CONNECTION_TRANSLATION_MAP.put(ClientOptions.CON_TCP_RECV_BUF_SIZE, "socketBufferSize");
        CONNECTION_TRANSLATION_MAP.put(ClientOptions.CON_TCP_TRAFFIC_CLASS, "diffServ");
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
        CONNECTION_TRANSLATION_MAP.put(ClientOptions.CONN_PREFIX_PACKET_SIZE_ENA, "wireFormat.sizePrefixDisabled");
        CONNECTION_TRANSLATION_MAP.put(ClientOptions.CONN_SERVER_STACK_TRACE_ENA, "wireFormat.stackTraceEnabled");
        CONNECTION_TRANSLATION_MAP.put(ClientOptions.CONN_TCP_NO_DELAY, "wireFormat.tcpNoDelayEnabled");
        CONNECTION_TRANSLATION_MAP.put(ClientOptions.CONN_TIGHT_ENCODING_ENA, "wireFormat.tightEncodingEnabled");
        CONNECTION_TRANSLATION_MAP.put(ClientOptions.CONN_WATCH_TOPIC_ADVISORIES, "jms.watchTopicAdvisories");
    }

    @Inject
    public AocClientOptionManager() {
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

            // failover:(dhcp-75-212.lab.eng.brq.redhat.com:5672,dhcp-75-219.lab.eng.brq.redhat.com:5672) -->
            // failover:(tcp://dhcp-75-212.lab.eng.brq.redhat.com:5672,tcp://dhcp-75-219.lab.eng.brq.redhat.com:5672) -->
            // TODO discovery..
            // discovery:(tcp://dhcp-75-212.lab.eng.brq.redhat.com:5672,tcp://dhcp-75-219.lab.eng.brq.redhat.com:5672) -->
            brokerUrl = appendMissingProtocol(brokerUrl);

            // If Failover-url list contains a broker value, add it here
            if (clientOptions.getOption(ClientOptions.CON_FAILOVER_URLS).hasParsedValue()) {
                StringBuilder failoverBrokers = new StringBuilder(",");
                String reconnectBrokers = clientOptions.getOption(ClientOptions.CON_FAILOVER_URLS).getValue();

                for (String brokerFailover : reconnectBrokers.split(",")) {
                    failoverBrokers.append(appendMissingProtocol(brokerFailover)).append(",");
                }
                failoverBrokers.deleteCharAt(failoverBrokers.length() - 1);
                brokerUrl += failoverBrokers;
            }

            checkAndSetOption(ClientOptions.FAILOVER_URL, brokerUrl, clientOptions);
        } else {
            super.setBrokerOptions(clientOptions, brokerUrl);
        }
    }

    @Override
    protected String getUrlProtocol() {
        return "tcp";
    }

    @Override
    protected void createConnectionOptions(ClientOptions clientOptions) {
        // see the link for source of inspiration. NOTE: the TrustingTrustManager is never unset!
        // http://activemq.2283324.n4.nabble.com/Configure-activemq-client-to-trust-any-SSL-certificate-from-the-broker-without-verifying-it-td4733309.html
        if (clientOptions.getOption(ClientOptions.CON_SSL_TRUST_ALL).hasParsedValue()) {
            try {
                SSLContext ctx = SSLContext.getInstance("TLS");
                ctx.init(new KeyManager[0], new TrustManager[]{new TrustingTrustManager()}, null);
                SSLContext.setDefault(ctx);
            } catch (NoSuchAlgorithmException | KeyManagementException e) {
                throw new RuntimeException("Could not set up the all-trusting TrustManager", e);
            }
        }

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

    /**
     * Does not do any checking. Trusts all certificates.
     */
    private class TrustingTrustManager implements X509TrustManager {
        @Override
        public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
            // trust anything
        }

        @Override
        public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
            // trust anything
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    }
}
