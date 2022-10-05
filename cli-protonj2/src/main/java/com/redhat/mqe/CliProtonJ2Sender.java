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
import org.apache.qpid.protonj2.client.Client;
import org.apache.qpid.protonj2.client.Connection;
import org.apache.qpid.protonj2.client.ConnectionOptions;
import org.apache.qpid.protonj2.client.Message;
import org.apache.qpid.protonj2.client.Sender;
import org.apache.qpid.protonj2.client.SenderOptions;
import picocli.CommandLine;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

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
    private Float duration;  // TODO do something with it

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

    @CommandLine.Option(names = {"--tx-endloop-action"})
    private TxEndloopAction txEndloopAction;

    @CommandLine.Option(names = {"--tx-action"})
    private TxAction txAction;

    @CommandLine.Option(names = {"--sync-mode"})
    private SyncMode syncMode;

    @CommandLine.Option(names = {"--log-lib"})
    private LogLib logLib;

    @CommandLine.Option(names = {"--duration-mode"})
    private DurationModeSender durationMode;

    public CliProtonJ2Sender() {
        this.messageFormatter = new ProtonJ2MessageFormatter();
    }

    public CliProtonJ2Sender(ProtonJ2MessageFormatter messageFormatter) {
        this.messageFormatter = messageFormatter;
    }

    @Override
    public Integer call() throws Exception { // your business logic goes here...

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
        options.user(connUsername);
        options.password(connPassword);
        for (AuthMechanism mech : connAuthMechanisms) {
            options.saslOptions().addAllowedMechanism(mech.name());
        }

        /*
        TODO API usablility, hard to ask for queue when dealing with broker that likes to autocreate topics
         */
        SenderOptions senderOptions = new SenderOptions();
        // is it target or source? target.
        senderOptions.targetOptions().capabilities(destinationCapability);
        try (Connection connection = client.connect(serverHost, serverPort, options);
             Sender sender = connection.openSender(address, senderOptions)) {

            double initialTimestamp = Utils.getTime();
            for (int i = 0; i < count; i++) {

                if (durationMode == DurationModeSender.beforeSend) {
                    Utils.sleepUntilNextIteration(initialTimestamp, count, duration, i + 1);
                }

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
                sender.send(message);  // TODO what's timeout for in a sender?

                Map<String, Object> messageDict = messageFormatter.formatMessage(address, (Message<Object>) message, stringToBool(msgContentHashedString));
                switch(out) {
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

                if (durationMode == DurationModeSender.afterSend) {
                    Utils.sleepUntilNextIteration(initialTimestamp, count, duration, i + 1);
                }
            }
        }
        return 0;
    }
}
