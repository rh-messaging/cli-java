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

import org.apache.activemq.artemis.api.core.SimpleString;
import org.apache.activemq.artemis.core.config.CoreQueueConfiguration;
import org.apache.activemq.artemis.core.server.MessageReference;
import org.apache.activemq.artemis.core.server.Queue;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.LogManager;
import org.apache.log4j.SimpleLayout;
import org.apache.qpid.jms.JmsConnectionFactory;
import org.apache.qpid.jms.JmsQueue;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junitpioneer.jupiter.TempDirectory;
import util.Broker;
import util.BrokerFixture;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.Session;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static com.google.common.truth.Truth.assertThat;
import static org.awaitility.Awaitility.await;

@Tag("issue")
public class QPIDJMS484Test {
    @BeforeAll
    static void configureLogging() {
        ConsoleAppender consoleAppender = new ConsoleAppender(new SimpleLayout(), ConsoleAppender.SYSTEM_OUT);
        LogManager.getRootLogger().addAppender(consoleAppender);
    }

    @Test
    @ExtendWith(BrokerFixture.class)
    @ExtendWith(TempDirectory.class)
    void testSendDispositionsAfterRecoverForUnacknowledgedMessages(@BrokerFixture.TempBroker Broker broker, @TempDirectory.TempDir Path tempDir) throws Exception {
        configureBroker(broker, tempDir);
        String someQueue = "someQueue";
        broker.configuration.addQueueConfiguration(new CoreQueueConfiguration().setAddress(someQueue));
        broker.startBroker();

        String brokerUrl = "amqp://localhost:" + broker.addAMQPAcceptor();
        SimpleString someQueueSS = SimpleString.toSimpleString(someQueue);
        final Destination destination = new JmsQueue(someQueue);

        com.redhat.mqe.jms.Main.main(new String[]{"sender", "--broker", brokerUrl, "--address", someQueue});

        JmsConnectionFactory factory = new JmsConnectionFactory(brokerUrl);
        try (Connection connection = factory.createConnection()) {
            connection.start();
            try (Session session = connection.createSession(Session.CLIENT_ACKNOWLEDGE)) {
                // this will use up default delivery retry count, message gets discarded by the broker
                for (int i = 0; i < 10; i++) {
                    try (MessageConsumer consumer = session.createConsumer(destination)) {
                        // checking messageReference.getDeliveryCount() does not reliably work, /me thinks that
                        // the internal consumer on broker is dequeueing the message, so it is sometimes not found
                        // by getSingleMessageReference

//                        {
//                            final MessageReference messageReference = getSingleMessageReference(broker, someQueue);
//                            if (i == 0) {
//                                assertThat(messageReference.getDeliveryCount()).isEqualTo(i + 1);
//                            } else {
//                                assertThat(messageReference.getDeliveryCount()).isEqualTo(i);
//                            }
//                        }

                        Message message = consumer.receive(1000);
                        assertThat(message).isNotNull();

                        if (i != 0) {
                            assertThat(message.getJMSRedelivered()).isTrue();
                        }
                        long deliveryCount = message.getLongProperty("JMSXDeliveryCount");
                        assertThat(deliveryCount).isEqualTo(i + 1);

                        session.recover();
                    } // close consumer

//                    {
//                        if (i != 9) {
//                            final MessageReference messageReference = getSingleMessageReference(broker, someQueue);
//                            assertThat(messageReference.getDeliveryCount()).isEqualTo(i + 1);
//                        } else {
//                            // broker auto-deleted the now empty queue
//                            assertThat(broker.getProxyToQueue(someQueue)).isNull();
//                        }
//                    }
                }
            }
        }
    }

    private void configureBroker(Broker broker, Path tempDir) {
        broker.configuration.setSecurityEnabled(false);
        broker.configuration.setBindingsDirectory(tempDir.resolve("data/bindings").toString());
        broker.configuration.setJournalDirectory(tempDir.resolve("data/journal").toString());
        broker.configuration.setLargeMessagesDirectory(tempDir.resolve("data/large").toString());
        broker.configuration.setPagingDirectory(tempDir.resolve("data/paging").toString());
    }

    private MessageReference getSingleMessageReference(Broker broker, String someQueue) {
        AtomicReference<MessageReference> result = new AtomicReference<>();
        await().untilAsserted(() -> {
            List<MessageReference> messageReferences = new ArrayList<>();
            Queue proxyToQueue = broker.getProxyToQueue(someQueue);
            assertThat(proxyToQueue).isNotNull();
            for (Iterator<MessageReference> it = proxyToQueue.iterator(); it.hasNext(); ) {
                messageReferences.add(it.next());
            }
            assertThat(messageReferences).hasSize(1);
            result.set(messageReferences.get(0));
        });
        return result.get();
    }


}
