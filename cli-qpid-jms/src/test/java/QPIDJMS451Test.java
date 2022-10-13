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

import org.apache.qpid.jms.JmsConnectionFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import util.Broker;
import util.BrokerFixture;

import jakarta.jms.Session;
import jakarta.jms.TextMessage;
import jakarta.jms.Topic;
import jakarta.jms.TopicConnection;
import jakarta.jms.TopicConnectionFactory;
import jakarta.jms.TopicPublisher;
import jakarta.jms.TopicSession;
import jakarta.jms.TopicSubscriber;
import java.nio.file.Path;

class QPIDJMS451Test {
    @Test
    @ExtendWith(BrokerFixture.class)
    void testSessionRecoverWithDurableSub(@BrokerFixture.TempBroker Broker broker, @TempDir Path tempDir) throws Exception {
        configureBroker(broker, tempDir);
        broker.startBroker();

        String brokerUrl = "amqp://localhost:" + broker.addAMQPAcceptor();
//            String brokerUrl = "amqp://localhost:5672";

        String subscriptionName = "QPIDJMS451TestSub";

        TopicConnectionFactory topicConnectionFactory = new JmsConnectionFactory(brokerUrl);
        TopicConnection topicConnection = topicConnectionFactory.createTopicConnection();
        topicConnection.setClientID("QPIDJMS451TestId");
        TopicSession topicSession = topicConnection.createTopicSession(false, Session.CLIENT_ACKNOWLEDGE);
        Topic topic = topicSession.createTopic("QPIDJMS451TestTopic");
        TopicPublisher topicPublisher = topicSession.createPublisher(topic);
        TopicSubscriber topicSubscriber = topicSession.createDurableSubscriber(topic, subscriptionName);

        TextMessage textMessage = topicSession.createTextMessage("QPIDJMS451TestMessage");
        topicPublisher.publish(textMessage);
        topicConnection.start();

        System.out.println("1");

        TextMessage receivedTextMessage = (TextMessage) topicSubscriber.receive(3000);
        // NOTE: don't ack now, important for the reproducer

        System.out.println("2");

        // NOTE: call recover, that is where the bug was
        topicSession.recover();

        receivedTextMessage = (TextMessage) topicSubscriber.receive(3000);

        if (receivedTextMessage != null) {
            receivedTextMessage.acknowledge();
        }

        System.out.println("6");

        topicConnection.stop();

        System.out.println("7");

        topicSubscriber.close();

        System.out.println("8");

        topicSession.unsubscribe(subscriptionName);
        topicSession.close();

        topicConnection.close();
    }

    private void configureBroker(Broker broker, Path tempDir) {
        broker.configuration.setSecurityEnabled(false);
        broker.configuration.setBindingsDirectory(tempDir.resolve("data/bindings").toString());
        broker.configuration.setJournalDirectory(tempDir.resolve("data/journal").toString());
        broker.configuration.setLargeMessagesDirectory(tempDir.resolve("data/large").toString());
        broker.configuration.setPagingDirectory(tempDir.resolve("data/paging").toString());
    }
}
