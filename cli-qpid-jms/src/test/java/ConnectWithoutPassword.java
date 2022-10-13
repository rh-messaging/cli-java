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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import util.Broker;

import jakarta.jms.Connection;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSSecurityException;
import java.time.Duration;

@SuppressWarnings("Duplicates")
class ConnectWithoutPassword {
    @Test
    void reconnectOneServerNoAuthGuestNotConfigured() {
        try (Broker broker = new Broker()) {
            broker.startBroker();

            ConnectionFactory f = new org.apache.qpid.jms.JmsConnectionFactory(
                "failover:(amqp://127.0.0.1:" + broker.addAMQPAcceptor() + ")");
            Assertions.assertTimeoutPreemptively(Duration.ofSeconds(5), () -> {
                Assertions.assertThrows(JMSSecurityException.class, () -> {
                    Connection c = f.createConnection();
                    c.start();
                });
            });
        }
    }

    @Test
    void reconnectOneServerWrongSaslCredentialsGuestNotConfigured() {
        try (Broker broker = new Broker()) {
            broker.startBroker();

            ConnectionFactory f = new org.apache.qpid.jms.JmsConnectionFactory(
                "failover:(amqp://127.0.0.1:" + broker.addAMQPAcceptor() + ")");
            Assertions.assertTimeoutPreemptively(Duration.ofSeconds(5), () -> {
                Assertions.assertThrows(JMSSecurityException.class, () -> {
                    Connection c = f.createConnection("someUser", "somePassword_wrong");
                    c.start();
                });
            });
        }
    }
}
