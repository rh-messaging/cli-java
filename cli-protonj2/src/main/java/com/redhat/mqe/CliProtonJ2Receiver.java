/*
 * Copyright (c) 2021 Red Hat, Inc.
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

package com.redhat.mqe;

import com.redhat.mqe.lib.Utils;
import org.apache.qpid.protonj2.client.Client;
import org.apache.qpid.protonj2.client.ClientOptions;
import org.apache.qpid.protonj2.client.Connection;
import org.apache.qpid.protonj2.client.ConnectionOptions;
import org.apache.qpid.protonj2.client.Delivery;
import org.apache.qpid.protonj2.client.DistributionMode;
import org.apache.qpid.protonj2.client.Message;
import org.apache.qpid.protonj2.client.Receiver;
import org.apache.qpid.protonj2.client.ReceiverOptions;
import org.apache.qpid.protonj2.client.Sender;
import org.apache.qpid.protonj2.client.Session;
import org.apache.qpid.protonj2.client.exceptions.ClientException;
import picocli.CommandLine;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import static com.redhat.mqe.lib.ClientOptionManager.QUEUE_PREFIX;
import static com.redhat.mqe.lib.ClientOptionManager.TOPIC_PREFIX;

@CommandLine.Command(
    name = "receiver",
    mixinStandardHelpOptions = true,
    version = "1.0.0",
    description = "Opens AMQP connections"
)
public class CliProtonJ2Receiver extends CliProtonJ2SenderReceiver implements Callable<Integer> {

    private final ProtonJ2MessageFormatter messageFormatter;

    @CommandLine.Option(names = {"--log-msgs"}, description = "MD5, SHA-1, SHA-256, ...")
    private LogMsgs logMsgs = LogMsgs.dict;

    @CommandLine.Option(names = {"--out"}, description = "MD5, SHA-1, SHA-256, ...")
    private Out out = Out.python;

    @CommandLine.Option(names = {"--msg-content-hashed"})
    private String msgContentHashedString = "false";

    @CommandLine.Option(names = {"-b", "--broker"}, description = "MD5, SHA-1, SHA-256, ...")
    private String broker = "MD5";

    @CommandLine.Option(names = {"--conn-username"}, description = "MD5, SHA-1, SHA-256, ...")
    private String connUsername = "MD5";

    @CommandLine.Option(names = {"--conn-password"}, description = "MD5, SHA-1, SHA-256, ...")
    private String connPassword = "MD5";

    @CommandLine.Option(names = {"--conn-clientid"})
    private String connClientId;

    @CommandLine.Option(names = {"--durable-subscriber"})
    private String durableSubscriberString = "false";

    @CommandLine.Option(names = {"--durable-subscriber-name"})
    private String durableSubscriberName;

    @CommandLine.Option(names = {"--subscriber-unsubscribe"})
    private String subscriberUnsubscribeString = "false";

    @CommandLine.Option(names = {"-a", "--address"}, description = "MD5, SHA-1, SHA-256, ...")
    private String address = "MD5";

    @CommandLine.Option(names = {"--recv-browse"}, description = "browse queued messages instead of receiving them")
    private String recvBrowseString = "false";

    @CommandLine.Option(names = {"--count"}, description = "MD5, SHA-1, SHA-256, ...")
    private int count = 1;

    @CommandLine.Option(names = {"--timeout"}, description = "MD5, SHA-1, SHA-256, ...")
    private int timeout;

    @CommandLine.Option(names = {"--conn-auth-mechanisms"}, description = "MD5, SHA-1, SHA-256, ...")
    // todo, want to accept comma-separated lists; there is https://picocli.info/#_split_regex
    private List<AuthMechanism> connAuthMechanisms = new ArrayList<>();

    @CommandLine.Option(names = {"--process-reply-to"})
    private boolean processReplyTo = false;

    @CommandLine.Option(names = {"--duration"})
    private Float duration = 0.0f;

    @CommandLine.Option(names = {"--duration-mode"})
    private DurationModeReceiver durationMode = DurationModeReceiver.afterReceive;

    @CommandLine.Option(names = {"--ssn-ack-mode"})
    private SsnAckMode ssnAckMode;

    @CommandLine.Option(names = {"--tx-size"})
    private Integer txSize;

    @CommandLine.Option(names = {"--tx-endloop-action"})
    private TxAction txEndloopAction;

    @CommandLine.Option(names = {"--tx-action"})
    private TxAction txAction;

    @CommandLine.Option(names = {"--msg-content-to-file"})
    private String msgContentToFile;

    @CommandLine.Option(names = {"--conn-reconnect"})
    private String reconnectString = "false";

    @CommandLine.Option(names = {"--conn-prefetch"})
    private Integer connPrefetch;

    @CommandLine.Option(names = {"--conn-heartbeat"})
    private Long connHeartbeat;

    public CliProtonJ2Receiver() {
        this.messageFormatter = new ProtonJ2MessageFormatter();
    }

    public CliProtonJ2Receiver(ProtonJ2MessageFormatter messageFormatter) {
        this.messageFormatter = messageFormatter;
    }

    /**
     * This is the main function of the client, as called by the cli options handling library.
     */
    @Override
    public Integer call() throws Exception {
        duration *= 1000;  // convert to milliseconds

        String prefix = "";
        if (!broker.startsWith("amqp://") && !broker.startsWith("amqps://")) {
            prefix = "amqp://";
        }
        final URI url = new URI(prefix + broker);
        final String serverHost = url.getHost();
        int serverPort = url.getPort();
        serverPort = (serverPort == -1) ? 5672 : serverPort;

        String destinationCapability = "queue";
        if (address.startsWith(TOPIC_PREFIX)) {
            address = address.substring((TOPIC_PREFIX.length()));
            destinationCapability = "topic";
        }
        if (address.startsWith(QUEUE_PREFIX)) {
            address = address.substring((QUEUE_PREFIX.length()));
        }

        ClientOptions clientOptions = new ClientOptions();
        // TODO api usability I had to hunt for this a bit; the idea is to have durable subscription: need specify connection id and subscriber name
        if (connClientId != null) {
            clientOptions.id(connClientId);
        }

        // TODO api usability; If I use the w/ clientOptions variant of Client.create, then .id defaults to null, and I get exception;
        //  ok, that just cannot be true ^^^; but it looks to be true; what!?!
        // aha, right; constructor does not check, factory method does check for null
        // proposed solution: allow null there, and let it mean autoassign; or tell us method to generate ID ourselves if we don't care
        Client client;
        if (clientOptions.id() != null) {
            client = Client.create(clientOptions);
        } else {
            client = Client.create();
        }

        final ConnectionOptions options = new ConnectionOptions();
        if (stringToBool(reconnectString)) {
            options.reconnectEnabled(true);
        }
        if (connHeartbeat != null) {
            options.idleTimeout(2 * connHeartbeat, TimeUnit.SECONDS);
        }
        options.user(connUsername);
        options.password(connPassword);
        for (AuthMechanism mech : connAuthMechanisms) {
            options.saslOptions().addAllowedMechanism(mech.name());
        }

        /*
        TODO API usability, hard to ask for queue when dealing with broker that likes to autocreate topics
         */
        ReceiverOptions receiverOptions = new ReceiverOptions();
        // is it target or source? target.
        receiverOptions.sourceOptions().capabilities(destinationCapability);

        // todo: another usability, little hard to figure out this is analogue of jms to browse queues
        if (stringToBool(recvBrowseString)) {
            receiverOptions.sourceOptions().distributionMode(DistributionMode.COPY);
        }

        // In AMQP, it is one credit means one message, so this matches the semantics
        if (connPrefetch != null) {
            receiverOptions.creditWindow(connPrefetch);
        }

        // TODO: API question: what is difference between autoSettle and autoAccept? why I want one but not the other?
        if (ssnAckMode != null) {
            if (ssnAckMode == SsnAckMode.client) {
                receiverOptions.autoAccept(false);
                receiverOptions.autoSettle(false);
            }
        }

        boolean transacted = txSize != null || txAction != null || txEndloopAction != null;

        try (Connection connection = client.connect(serverHost, serverPort, options);
             Session session = connection.openSession()) {
            Receiver receiver;
            if (stringToBool(durableSubscriberString)) {
                receiver = session.openDurableReceiver(address, durableSubscriberName, receiverOptions);
            } else {
                receiver = session.openReceiver(address, receiverOptions);
            }

            if (stringToBool(subscriberUnsubscribeString)) {
                receiver.close();
                return 0;
            }

            if (transacted) {
                session.beginTransaction();
            }

            int i = 0;
            double initialTimestamp = Utils.getTime();
            while (true) {

                if (durationMode == DurationModeReceiver.beforeReceive) {
                    Utils.sleepUntilNextIteration(initialTimestamp, count, duration, i + 1);
                }

                final Delivery delivery;
                if (timeout == 0) {
                    delivery = receiver.receive();  // todo: can default it to -1
                } else {
                    delivery = receiver.receive(timeout, TimeUnit.SECONDS);
                }

                if (delivery == null) {
                    break;
                }

                if (durationMode == DurationModeReceiver.afterReceive) {
                    Utils.sleepUntilNextIteration(initialTimestamp, count, duration, i + 1);
                }

                if (processReplyTo && delivery.message().replyTo() != null) {
                    String replyTo = delivery.message().replyTo();
                    Message<Object> message = delivery.message();
                    message.replyTo(null);
                    try (Sender sender = connection.openSender(replyTo)) {
                        sender.send(message);
                    }
                }

                // todo, is this what we mean?
                if (ssnAckMode != null && ssnAckMode == SsnAckMode.client) {
                    delivery.accept();
                }

                outputReceivedMessage(i, delivery);
                i++;

                if (txSize != null && txSize != 0) {
                    if (i % txSize == 0) {
                        if (txAction != null) {
                            switch (txAction) {
                                case commit:
                                    session.commitTransaction();
                                    break;
                                case rollback:
                                    session.rollbackTransaction();
                                    break;
                            }

                            session.beginTransaction();

                            if (durationMode == DurationModeReceiver.afterReceiveTxAction) {
                                Utils.sleepUntilNextIteration(initialTimestamp, i, duration, i + 1);
                            }
                        }
                    }
                }

                if (i == count) { // not i > count; --count=0 needs to disable the break
                    break;
                }
            }

            if (txEndloopAction != null) {
                switch (txEndloopAction) {
                    case commit:
                        session.commitTransaction();
                        break;
                    case rollback:
                        session.rollbackTransaction();
                        break;
                }
            } else if (transacted) {
                session.rollbackTransaction();
            }

            if (stringToBool(durableSubscriberString)) {
                receiver.detach();
            } else {
                receiver.close(); // TODO want to do autoclosable, need helper func, that's all
            }
        }

        return 0;
    }

    private void outputReceivedMessage(int i, Delivery delivery) throws ClientException, IOException {
        Message<Object> message = delivery.message();
        int messageFormat = delivery.messageFormat();
        Map<String, Object> messageDict = messageFormatter.formatMessage(address, message, stringToBool(msgContentHashedString));
        if (msgContentToFile != null) {
            // todo?
            Path file = Paths.get(msgContentToFile + "_" + i);
            Files.write(file, message.body().toString().getBytes(StandardCharsets.UTF_8));
        }
        switch (out) {
            case python:
                switch (logMsgs) {
                    case dict:
                        messageFormatter.printMessageAsPython(messageDict);
                        break;
                    case interop:
                        messageFormatter.printMessageAsPython(messageDict);
                        break;
                }
                break;
            case json:
                switch (logMsgs) {
                    case dict:
                        messageFormatter.printMessageAsJson(messageDict);
                        break;
                    case interop:
                        messageFormatter.printMessageAsJson(messageDict);
                        break;
                }
                break;
        }
    }
}
