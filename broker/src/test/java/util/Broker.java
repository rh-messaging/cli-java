/*
 * Copyright (c) 2019 Red Hat, Inc.
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

import org.apache.activemq.artemis.api.core.SimpleString;
import org.apache.activemq.artemis.api.core.management.AddressControl;
import org.apache.activemq.artemis.api.core.management.ObjectNameBuilder;
import org.apache.activemq.artemis.core.config.Configuration;
import org.apache.activemq.artemis.core.config.ConfigurationUtils;
import org.apache.activemq.artemis.core.config.impl.ConfigurationImpl;
import org.apache.activemq.artemis.core.server.embedded.EmbeddedActiveMQ;
import org.apache.activemq.artemis.spi.core.remoting.Acceptor;
import org.junit.jupiter.api.extension.ExtensionContext;

import javax.management.MBeanServer;
import javax.management.MBeanServerInvocationHandler;
import javax.management.ObjectName;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;


// https://activemq.apache.org/artemis/docs/latest/embedding-activemq.html
public class Broker implements AutoCloseable, ExtensionContext.Store.CloseableResource {
    // Use same MBeanServer instance that broker is using (don't create new)
    private final MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
    public Path tempDir;
    public EmbeddedActiveMQ embeddedBroker = new EmbeddedActiveMQ();
    public Configuration configuration = new ConfigurationImpl();

    public Broker() {
        this(null);
    }

    public Broker(Path tempDir) {
        this.tempDir = tempDir;
        configureBroker(configuration);
    }

    /**
     * Set configuration for the test broker. Delta from broker's default configuration.
     *
     * @param configuration configuration to apply the delta to
     */
    private static void configureBroker(Configuration configuration) {
        configuration.setMaxDiskUsage(100); // my laptop is constantly running out of disk space
    }

    public void startBroker() {
        ConfigurationUtils.validateConfiguration(configuration);
        embeddedBroker.setConfiguration(configuration);
        try {
            embeddedBroker.start();
        } catch (Exception e) {
            throw new RuntimeException("Failed to start the embedded broker", e);
        }
        if (!embeddedBroker.getActiveMQServer().isStarted()) {
            throw new RuntimeException("Failed to start the embedded broker: it is not running");
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
     * Configures a log4j appender if there isn't any, so that log messages flood the stdout
     */
    public static void configureLogging() {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    /**
     * @return port where the acceptor listens
     */
    public int addAMQPAcceptor() {
        Exception lastException = null;
        for (int i = 0; i < 10; i++) {
            try {
                int port = findRandomAvailablePortOnAllLocalInterfaces();
                Acceptor acceptor = embeddedBroker.getActiveMQServer().getRemotingService().createAcceptor("amqp", "tcp://127.0.0.1:" + port + "?protocols=AMQP");
                acceptor.start();  // this will throw if the port is not available
                return port;
            } catch (Exception e) {
                lastException = e;
            }
        }
        throw new RuntimeException("Failed to bind to an available port", lastException);
    }

    int addCoreAcceptor() {
        Exception lastException = null;
        for (int i = 0; i < 10; i++) {
            try {
                int port = findRandomAvailablePortOnAllLocalInterfaces();
                Acceptor acceptor = embeddedBroker.getActiveMQServer().getRemotingService().createAcceptor("core", "tcp://127.0.0.1:" + port + "?protocols=CORE");
                acceptor.start();  // this will throw if the port is not available
                return port;
            } catch (Exception e) {
                lastException = e;
            }
        }
        throw new RuntimeException("Failed to bind to an available port", lastException);
    }

    /**
     * @return port where the acceptor listens
     */
    public int addAMQPSAcceptor(InputStream keyStore) {
        if (this.tempDir == null) {
            throw new IllegalStateException("Broker must be created with tempDir to use this");
        }

        Path keyStorePath = null;
        try {
            keyStorePath = Files.createTempFile(tempDir, "keyStore", "");
            Files.copy(keyStore, keyStorePath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Could not write keyStore to tempDir", e);
        }

        Exception lastException = null;
        for (int i = 0; i < 10; i++) {
            try {
                int port = findRandomAvailablePortOnAllLocalInterfaces();
                Acceptor acceptor = embeddedBroker.getActiveMQServer().getRemotingService().createAcceptor("amqps",
                    "tcp://0.0.0.0:" + port + "?sslEnabled=true;keyStorePath=" + keyStorePath + ";keyStorePassword=secureexample;tcpSendBufferSize=1048576;tcpReceiveBufferSize=1048576;protocols=AMQP;useEpoll=true;amqpCredits=1000;amqpMinCredits=300");
                acceptor.start();  // this will throw if the port is not available
                return port;
            } catch (Exception e) {
                lastException = e;
            }
        }
        throw new RuntimeException("Failed to bind to an available port", lastException);
    }

    public org.apache.activemq.artemis.core.server.Queue getProxyToQueue(String queueName) {
        return embeddedBroker.getActiveMQServer().locateQueue(SimpleString.toSimpleString(queueName));
    }

    /**
     * @return port number (there is a race so it may not be available anymore)
     * @throws IOException
     */
    // https://stackoverflow.com/questions/2675362/how-to-find-an-available-port
    private int findRandomAvailablePortOnAllLocalInterfaces() throws IOException {
        try (ServerSocket socket = new ServerSocket()) {
            socket.setReuseAddress(true);
            socket.bind(new InetSocketAddress(0));
            return socket.getLocalPort();
        }
    }

    protected Object createProxy(final ObjectName objectName,
                                 final Class mbeanInterface,
                                 final MBeanServer mbeanServer) {
        return MBeanServerInvocationHandler.newProxyInstance(mbeanServer, objectName, mbeanInterface, false);
    }

    /**
     * The resulting AddressControl operations will throw java.lang.reflect.UndeclaredThrowableException
     *  if the queried object does not exist yet. Use awaitilly to deal with that.
     */
    public AddressControl makeAddressControl(String queueName) {
        SimpleString address = SimpleString.toSimpleString(queueName);
        try {
            AddressControl addressControl = (AddressControl) createProxy(ObjectNameBuilder.DEFAULT.getAddressObjectName(address), AddressControl.class, mBeanServer);
            return addressControl;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
