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

import org.apache.activemq.artemis.core.server.Queue;
import org.apache.qpid.jms.JmsConnectionFactory;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import util.Broker;
import util.BrokerFixture;

import javax.jms.Connection;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;
import java.util.concurrent.TimeUnit;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;
import static org.awaitility.Awaitility.await;

// Originally created by Andy Taylor,
// available from https://github.com/andytaylor/activemq-artemis/commits/ENTMQBR-2677
class ENTMQCL1860 {
    @BeforeAll
    static void configureLogging() {
        // turn on extra verbose logging
        //Broker.configureLogging();
    }

    @Test
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    @ExtendWith(BrokerFixture.class)
    void testAckWithSessionClose(@BrokerFixture.TempBroker Broker broker) throws Exception {
        broker.configuration.setSecurityEnabled(false);
        broker.configuration.setPersistenceEnabled(false); // this, or tmpdir, otherwise test runs interact
        broker.startBroker();

        String brokerUrl = "amqp://localhost:" + broker.addAMQPAcceptor();
        JmsConnectionFactory connectionFactory = new JmsConnectionFactory(brokerUrl);

        Connection connection = connectionFactory.createConnection();
        connection.start();

        try {
            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            javax.jms.Queue queue = session.createQueue(getQueueName());
            MessageProducer producer = session.createProducer(queue);
            producer.send(session.createMessage());
            connection.close();
            Queue queueView = broker.getProxyToQueue(getQueueName());

            await().untilAsserted(() -> assertThat(queueView.getMessageCount()).isEqualTo(1));

            // Now create a new connection and receive and acknowledge
            for (int i = 0; i < 10; i++) {
                connection = connectionFactory.createConnection();
                connection.start();
                session = connection.createSession(false, Session.CLIENT_ACKNOWLEDGE);
                MessageConsumer consumer = session.createConsumer(session.createQueue(getQueueName()));
                Message message = consumer.receive();
                assertWithMessage("Message is null during for loop iteration i = %s", i)
                    .that(message).isNotNull();
                connection.close();
                if (i > 0) {
                    Assert.assertTrue(message.getJMSRedelivered());
                }
            }
            connection = connectionFactory.createConnection();
            connection.start();
            session = connection.createSession(false, Session.CLIENT_ACKNOWLEDGE);
            MessageConsumer consumer = session.createConsumer(session.createQueue(getQueueName()));
            Message message = consumer.receiveNoWait();
            Assert.assertNull(message);
            connection.close();
        } finally {
            connection.close();
        }
    }

    private String getQueueName() {
        return "someQueue";
    }
}
