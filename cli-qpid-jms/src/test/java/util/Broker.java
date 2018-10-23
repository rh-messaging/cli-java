/*
 * Copyright (c) 2018 Red Hat, Inc.
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

package util;

import org.apache.activemq.artemis.core.config.Configuration;
import org.apache.activemq.artemis.core.config.ConfigurationUtils;
import org.apache.activemq.artemis.core.config.impl.ConfigurationImpl;
import org.apache.activemq.artemis.core.server.embedded.EmbeddedActiveMQ;
import org.apache.activemq.artemis.spi.core.remoting.Acceptor;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;


// https://activemq.apache.org/artemis/docs/latest/embedding-activemq.html
public class Broker implements Closeable {
    public EmbeddedActiveMQ embeddedBroker = new EmbeddedActiveMQ();
    public Configuration configuration = new ConfigurationImpl();
    public int amqpPort = -1;

    public void startBroker() {
        ConfigurationUtils.validateConfiguration(configuration);
        embeddedBroker.setConfiguration(configuration);
        try {
            embeddedBroker.start();
            amqpPort = addAMQPAcceptor();
        } catch (Exception e) {
            throw new RuntimeException("Failed to start the embedded broker", e);
        }
    }

    @Override
    public void close() {
        try {
            embeddedBroker.stop();
        } catch (Exception e) {
            throw new RuntimeException("Failed to stop the embedded broker", e);
        }
    }

    /**
     * @return port where the acceptor listens
     */
    int addAMQPAcceptor() {
        Exception lastException = null;
        for (int i = 0; i < 10; i++) {
            try {
                int port = findRandomOpenPortOnAllLocalInterfaces();
                Acceptor acceptor = embeddedBroker.getActiveMQServer().getRemotingService().createAcceptor("amqp", "tcp://127.0.0.1:" + port + "?protocols=AMQP");
                acceptor.start();  // this will throw if the port is not available
                return port;
            } catch (Exception e) {
                lastException = e;
            }
        }
        throw new RuntimeException("Failed to bind to an available port", lastException);
    }

    /**
     * @return port number (there is a race so it may not be available anymore)
     * @throws IOException
     */
    // https://stackoverflow.com/questions/2675362/how-to-find-an-available-port
    private int findRandomOpenPortOnAllLocalInterfaces() throws IOException {
        try (ServerSocket socket = new ServerSocket()) {
            socket.setReuseAddress(true);
            socket.bind(new InetSocketAddress(0));
            return socket.getLocalPort();
        }
    }
}
