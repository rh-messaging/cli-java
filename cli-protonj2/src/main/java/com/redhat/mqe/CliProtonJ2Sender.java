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

import com.redhat.mqe.lib.Content;
import com.redhat.mqe.lib.Utils;
import org.apache.qpid.protonj2.client.*;
import org.apache.qpid.protonj2.client.exceptions.ClientException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import picocli.CommandLine;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import static com.redhat.mqe.lib.ClientOptionManager.QUEUE_PREFIX;
import static com.redhat.mqe.lib.ClientOptionManager.TOPIC_PREFIX;

@CommandLine.Command(
    name = "sender",
    mixinStandardHelpOptions = true,
    version = "1.0.0",
    description = "Opens AMQP connections"
)
public class CliProtonJ2Sender extends CliProtonJ2SenderReceiver implements Callable<Integer> {

    private final ProtonJ2MessageFormatter messageFormatter;

    @CommandLine.Option(names = {"--log-msgs"}, description = "message reporting style")
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

    @CommandLine.Option(names = {"-a", "--address"}, description = "MD5, SHA-1, SHA-256, ...")
    private String address = "MD5";

    @CommandLine.Option(names = {"--count"}, description = "MD5, SHA-1, SHA-256, ...")
    private int count = 1;

    @CommandLine.Option(names = {"--timeout"}, description = "MD5, SHA-1, SHA-256, ...")
    private int timeout;

    @CommandLine.Option(names = {"--duration"})
    private Float duration = 0.0f;

    @CommandLine.Option(names = {"--conn-auth-mechanisms"}, description = "MD5, SHA-1, SHA-256, ...")
    // todo, want to accept comma-separated lists; there is https://picocli.info/#_split_regex
    private List<AuthMechanism> connAuthMechanisms = new ArrayList<>();

    @CommandLine.Option(names = {"--msg-property"})  // picocli Map options works for this, sounds like
    private List<String> msgProperties = new ArrayList<>();

    @CommandLine.Option(names = {"--msg-content"})
    private String msgContent;

    @CommandLine.Option(names = {"--msg-content-from-file"})
    private String msgContentFromFile;

    @CommandLine.Option(names = {"--content-type"})
    private ContentType contentType = ContentType.STRING;

    @CommandLine.Option(names = {"--property-type"})
    private PropertyType propertyType = PropertyType.String;

    @CommandLine.Option(names = {"--msg-durable"})
    private String msgDurableString = "false";

    @CommandLine.Option(names = {"--msg-ttl"})
    private Long msgTtl;

    @CommandLine.Option(names = {"--msg-content-list-item"})
    private List<String> msgContentListItem;

    @CommandLine.Option(names = {"--msg-content-map-item"})
    private List<String> msgContentMapItems;

    @CommandLine.Option(names = {"--msg-content-binary"})
    private String msgContentBinaryString = "false";

    @CommandLine.Option(names = {"--msg-correlation-id"})
    private String msgCorrelationId;

    @CommandLine.Option(names = {"--msg-group-id"})
    private String msgGroupId;

    @CommandLine.Option(names = {"--msg-id"})
    private String msgId;  // todo, not just string is an option

    @CommandLine.Option(names = {"--msg-reply-to"})
    private String msgReplyTo;

    @CommandLine.Option(names = {"--msg-subject"})
    private String msgSubject;

    @CommandLine.Option(names = {"--msg-user-id"})
    private String msgUserId;

    @CommandLine.Option(names = {"--msg-priority"})
    private Short msgPriority;  // TODO unsigned byte, actually

    // jms.populateJMSXUserID opt in qpid-jms
    // TODO: does not seem to have equivalent; what is the threat model for "prevent spoofing" in JMS docs?
    @CommandLine.Option(names = {"--conn-populate-user-id"})
    private String connPopulateUserIdString = "false";

    @CommandLine.Option(names = {"--msg-group-seq"})
    private Integer msgGroupSeq;

    @CommandLine.Option(names = {"--msg-reply-to-group-id"})
    private String msgReplyToGroupId;

    @CommandLine.Option(names = {"--ssn-ack-mode"})
    private SsnAckMode ssnAckMode;

    @CommandLine.Option(names = {"--tx-size"})
    private Integer txSize;

    @CommandLine.Option(names = {"--tx-endloop-action"})
    private TxAction txEndloopAction;

    @CommandLine.Option(names = {"--tx-action"})
    private TxAction txAction;

    @CommandLine.Option(names = {"--sync-mode"})
    private SyncMode syncMode;

    @CommandLine.Option(names = {"--log-lib"})
    private LogLib logLib;

    @CommandLine.Option(names = {"--duration-mode"})
    private DurationModeSender durationMode = DurationModeSender.afterSend;

    @CommandLine.Option(names = {"--conn-reconnect"})
    private String reconnectString = "false";

    @CommandLine.Option(names = {"--conn-heartbeat"})
    private Long connHeartbeat;

    public CliProtonJ2Sender() {
        this.messageFormatter = new ProtonJ2MessageFormatter();
    }

    public CliProtonJ2Sender(ProtonJ2MessageFormatter messageFormatter) {
        this.messageFormatter = messageFormatter;
    }

    /**
     * This is the main function of the client, as called by the cli options handling library.
     */
    @Override
    public Integer call() throws Exception { // your business logic goes here...
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

        final Client client = Client.create();

        final ConnectionOptions options = new ConnectionOptions();
        // TODO typo in javadoc: This option enables or disables reconnection to a remote remote peer after IO errors. To control
        // TODO API: unclear if reconnect is on or off by default (public static final boolean DEFAULT_RECONNECT_ENABLED = false;)
        if (stringToBool(reconnectString)) {
            options.reconnectEnabled(true);
        }
        if (connHeartbeat != null) {
            // TODO finish that 2x investigation for heartbeats and document it somewhere (jira?)
            options.idleTimeout(2 * connHeartbeat, TimeUnit.SECONDS);
        }
        options.user(connUsername);
        options.password(connPassword);
        for (AuthMechanism mech : connAuthMechanisms) {
            options.saslOptions().addAllowedMechanism(mech.name());
        }

        /*
        TODO API usablility, hard to ask for queue when dealing with broker that likes to autocreate topics
         */
        SenderOptions senderOptions = new SenderOptions();
        // is it target or source? target.  // TODO API explain which is which
        senderOptions.targetOptions().capabilities(destinationCapability);

        boolean transacted = txSize != null || txAction != null || txEndloopAction != null;

        // do simple and also complex (with session) loop, depending on if we have transactions
        if (transacted) {
            // TODO API, when I use session and when not? Add note to session that it is optional. and that it provides transactions?
            //  "Session object used to create Sender and Receiver instances."
            try (Connection connection = client.connect(serverHost, serverPort, options);
                 Session session = connection.openSession();
                 Sender sender = session.openSender(address, senderOptions)) {
                performMessageSending(transacted, sender, session);
            }
        } else {
            try (Connection connection = client.connect(serverHost, serverPort, options);
                 Sender sender = connection.openSender(address, senderOptions)) {
                performMessageSending(transacted, sender, null);
            }
        }
        return 0;
    }

    private void performMessageSending(boolean transacted, @NotNull Sender sender, @Nullable Session session) throws IOException, ClientException {
        // ensure we have a transaction; JMS begins a transaction automatically
        if (transacted) {
            assert session != null;
            // TODO: Typo in javadoc: transaction they user must commit
            //  also, thought beginning transaction twice is noop, but got
            //  ClientIllegalStateException("A transaction is already active in this Session");
            session.beginTransaction();
        }

        int i = 0;
        double initialTimestamp = Utils.getTime();
        while (true) {

            if (durationMode == DurationModeSender.beforeSend) {
                Utils.sleepUntilNextIteration(initialTimestamp, count, duration, i + 1);
            }

            Message<?> message = createNewMessage();
            // TODO what's timeout for in a sender?
            Tracker tracker = sender.send(message);
            tracker.awaitSettlement();
            // NB: This is not a busy loop involving the network, because sooner or later the peer will drain credit,
            //  if it intends to keep blocking. And sender.send() blocks upon running out of credit.
            while (!tracker.remoteState().isAccepted()) {
                // NB: Transacted session gives a special state that is not considered "accepted" even though it is e.g.
                //  DeliveryState.ClientTransactional{
                //   TransactionalState{txnId=ea51ffc4-4896-11ed-924a-d6bdd75e6e2e, outcome=Accepted{}}
                //  Since we don't test reconnect with transactions (GAP! :shocked face:) let's bail out
                if (tracker.remoteState().getType() == DeliveryState.Type.TRANSACTIONAL) {
                    break;
                }
                // TODO: am I supposed to increment `delivery-count` of the message if I got rejected before?
                //  as per http://docs.oasis-open.org/amqp/core/v1.0/os/amqp-core-messaging-v1.0-os.html#type-rejected
                tracker = sender.send(message);  // resend the message
                tracker.awaitSettlement();
            }

            printMessage((Message<Object>) message);
            i++; // TODO: looks like all have the sleeps wrong, then (the + 1 in the calls)

            if (durationMode == DurationModeSender.afterSend) {
                Utils.sleepUntilNextIteration(initialTimestamp, count, duration, i + 1);
            }

            if (txSize != null && txSize != 0) {
                if (i % txSize == 0) {
                    // Do transaction action
                    if (txAction != null) {
                        assert transacted && session != null;
                        switch (txAction) {
                            case commit:
                                session.commitTransaction();
                                break;
                            case rollback:
                                session.rollbackTransaction();
                                break;
                        }

                        session.beginTransaction();

                        if (durationMode == DurationModeSender.afterSendTxAction) {
                            Utils.sleepUntilNextIteration(initialTimestamp, count, duration, i + 1);
                        }
                    }
                }
            }
            if (count == 0) continue;
            if (i == count) break;
        }

        if (txEndloopAction != null) {
            assert transacted && session != null;
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
    }

    private void printMessage(Message<Object> message) throws ClientException {
        Map<String, Object> messageDict = messageFormatter.formatMessage(address, message, stringToBool(msgContentHashedString));
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

    @NotNull
    private Message<?> createNewMessage() throws IOException, ClientException {
        Message<?> message;
        if (msgContentListItem != null && !msgContentListItem.isEmpty()) {  // TODO check only one of these is specified
            List<Object> list = new ArrayList<>();
            for (String item : msgContentListItem) {
                Content content = new Content(contentType.toString(), item, false);  // TODO do this in args parsing?
                list.add(content.getValue());
            }
            message = Message.create((Object) list);
        } else if (msgContentMapItems != null) {
            Map<String, Object> map = new HashMap<>();
            for (String item : msgContentMapItems) {
                Content content = new Content(contentType.toString(), item, true);  // TODO do this in args parsing?
                map.put(content.getKey(), content.getValue());
            }
            message = Message.create((Object) map);
        } else if (msgContentFromFile != null) {
            if (stringToBool(msgContentBinaryString)) {
                message = Message.create(Files.readAllBytes(Paths.get(msgContentFromFile)));  // todo maybe param type as Path? check exists
            } else {
                message = Message.create(Files.readString(Paths.get(msgContentFromFile)));  // todo maybe param type as Path? check exists
            }
        } else {
            message = Message.create(msgContent);
        }
        if (msgProperties != null) {
            for (String item : msgProperties) {
                Content content = new Content(propertyType.toString(), item, true);  // TODO do this in args parsing?
                message.property(content.getKey(), content.getValue());
            }
        }
        if (msgId != null) {
            message.messageId(msgId);
        }
        if (msgCorrelationId != null) {
            message.correlationId(msgCorrelationId);
        }
        if (msgTtl != null) {
            message.timeToLive(msgTtl);
        }
        if (stringToBool(msgDurableString)) {
            message.durable(true);
        }
        if (msgGroupId != null) {
            message.groupId(msgGroupId);
        }
        if (msgGroupSeq != null) {
            message.groupSequence(msgGroupSeq);
        }
        if (msgReplyTo != null) {
            message.replyTo(msgReplyTo);
        }
        if (msgReplyToGroupId != null) {
            message.replyToGroupId(msgReplyToGroupId);
        }
        if (contentType != null) {
            message.contentType(contentType.toString()); // TODO: maybe should do more with it? don't bother with enum?
        }
        if (stringToBool(connPopulateUserIdString)) {
            message.userId(msgUserId.getBytes());
        }
        if (msgSubject != null) {
            message.subject(msgSubject);
        }
        if (msgPriority != null) {
            message.priority((byte) (int) msgPriority);
        }
        return message;
    }
}
