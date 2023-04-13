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

package com.redhat.mqe.lib;

import com.redhat.mqe.lib.message.MessageProvider;

import javax.inject.Inject;
import javax.inject.Named;
import jakarta.jms.*;
import java.util.List;

/**
 * SenderClient is able to send various messages with wide options
 * of settings of these messages.
 */
public class SenderClient extends CoreClient {
    protected ClientOptions senderOptions;
    protected List<Content> content;
    static final String AMQ_SUBJECT = "JMS_AMQP_Subject";
    static final String AMQ_USERID = "JMSXUserID";
    public static final String BEFORE_SEND = "before-send";
    public static final String AFTER_SEND = "after-send";
    public static final String AFTER_SEND_TX_ACTION = "after-send-tx-action";


    @Inject
    public SenderClient(ConnectionManagerFactory connectionManagerFactory, JmsMessageFormatter jmsMessageFormatter, @Named("Sender") ClientOptions options) {
        this.connectionManagerFactory = connectionManagerFactory;
        this.jmsMessageFormatter = jmsMessageFormatter;
        this.senderOptions = options;
    }

    public SenderClient() {

    }

    /**
     * Initial method to start the client.
     * Initialization of content, properties and everything about
     * how to send message is done/initiated by this method.
     * Contains the main sending loop method.
     */
    public void startClient() {
        ClientOptions senderOptions = this.getClientOptions();
        setGlobalClientOptions(senderOptions);
        Connection connection = this.createConnection(senderOptions);

        // Transactions support
        int transactionSize = 0;
        String transaction = null;
        if (senderOptions.getOption(ClientOptions.TX_SIZE).hasParsedValue()
            || senderOptions.getOption(ClientOptions.TX_ENDLOOP_ACTION).hasParsedValue()) {
            transactionSize = Integer.parseInt(senderOptions.getOption(ClientOptions.TX_SIZE).getValue());
            if (senderOptions.getOption(ClientOptions.TX_ACTION).hasParsedValue()) {
                transaction = senderOptions.getOption(ClientOptions.TX_ACTION).getValue().toLowerCase();
            } else {
                transaction = senderOptions.getOption(ClientOptions.TX_ENDLOOP_ACTION).getValue().toLowerCase();
            }
        }

        try {
            Session session = (transaction == null || transaction.equals("none")) ?
                this.createSession(senderOptions, connection, false) : this.createSession(senderOptions, connection, true);
            connection.start();

            boolean anonymousProducer = Utils.convertOptionToBoolean(senderOptions.getOption(ClientOptions.CONN_ANONYMOUS_PRODUCER).getValue());
            MessageProducer msgProducer = anonymousProducer ? session.createProducer(null) : session.createProducer(this.getDestination());
            setMessageProducer(senderOptions, msgProducer);

            // Calculate msg-rate from COUNT & DURATION
            double initialTimestamp = Utils.getTime();
            int count = Integer.parseInt(senderOptions.getOption(ClientOptions.COUNT).getValue());
            double duration = Double.parseDouble(senderOptions.getOption(ClientOptions.DURATION).getValue());
            duration *= 1000;  // convert to milliseconds

            final MessageProvider messageProvider = new MessageProvider(senderOptions, session).newInstance();

            int msgCounter = 0;
            String durationMode = senderOptions.getOption(ClientOptions.DURATION_MODE).getValue();
            while (true) {
                // Create message and fill body with data (content)
                Message message = messageProvider.provideMessage(msgCounter);

                // sleep for given amount of time, defined by msg-rate "before-send"
                if (durationMode.equals(BEFORE_SEND)) {
                    LOG.trace("Sleeping before send");
                    Utils.sleepUntilNextIteration(initialTimestamp, count, duration, msgCounter + 1);
                }

                // Send messages
                try {
                    if (anonymousProducer) {
                        msgProducer.send(getDestination(), message);
                    } else {
                        msgProducer.send(message);
                    }
                } catch (Exception e) {
                    switch (e.getCause().getClass().getName()) {
                        case "org.apache.qpid.jms.provider.exceptions.ProviderDeliveryReleasedException":
                            String onRelease = senderOptions.getOption(ClientOptions.ON_RELEASE).getValue();
                            LOG.trace(String.format("Message released [action: %s]", onRelease));
                            switch (onRelease) {
                                case "fail":
                                    throw e;
                                case "retry":
                                    continue;
                            }
                        default:
                            // Preserve original behavior
                            throw e;
                    }
                }
                msgCounter++;
                // Makes message body read only from write only mode
                if (message instanceof StreamMessage) {
                    ((StreamMessage) message).reset();
                }
                if (message instanceof BytesMessage) {
                    ((BytesMessage) message).reset();
                }
                printMessage(senderOptions, message);

                // close streaming message source if that is what we are doing
                try {
                    messageProvider.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                // sleep for given amount of time, defined by msg-rate "after-send-before-tx-action"
                if (durationMode.equals(AFTER_SEND)) {
                    LOG.trace("Sleeping after send");
                    Utils.sleepUntilNextIteration(initialTimestamp, count, duration, msgCounter + 1);
                }

                // TX support
                if (transaction != null && transactionSize != 0) {
                    if (msgCounter % transactionSize == 0) {
                        // Do transaction action
                        doTransaction(session, transaction);
                    }
                }
                // sleep for given amount of time, defined by msg-rate "after-send-after-tx-action"
                if (durationMode.equals(AFTER_SEND_TX_ACTION)) {
                    LOG.trace("Sleeping after send & tx action");
                    Utils.sleepUntilNextIteration(initialTimestamp, count, duration, msgCounter + 1);
                }
                if (count == 0) continue;
                if (msgCounter == count) break;
            }

            // Finish transaction with sending of the rest messages
            if (transaction != null) {
                doTransaction(session, senderOptions.getOption(ClientOptions.TX_ENDLOOP_ACTION).getValue());
            }
        } catch (JMSException | IllegalArgumentException jmse) {
            LOG.error("Error while sending a message! {}", jmse.getMessage());
            jmse.printStackTrace();
            System.exit(1);
        } finally {
            double closeSleep = Double.parseDouble(this.getClientOptions().getOption(ClientOptions.CLOSE_SLEEP).getValue());
            closeConnObjects(this, closeSleep);
            this.close(connection);
        }
    }

    /**
     * Set default priority, ttl, durability and creating of id,
     * timestamps for messages of this message producer.
     *
     * @param senderOptions specify defined options for messages & messageProducers
     * @param producer      set this message producer
     */
    protected static void setMessageProducer(ClientOptions senderOptions, MessageProducer producer) {
        try {
            // set delivery mode - durable/non-durable
            int deliveryMode = Utils.convertOptionToBoolean(senderOptions.getOption(ClientOptions.MSG_DURABLE).getValue())
                ? DeliveryMode.PERSISTENT : DeliveryMode.NON_PERSISTENT;
            producer.setDeliveryMode(deliveryMode);
            // set time to live of message if provided
            if (senderOptions.getOption(ClientOptions.MSG_TTL).hasParsedValue()) {
                producer.setTimeToLive(Long.parseLong(senderOptions.getOption(ClientOptions.MSG_TTL).getValue()));
            }
            // set message priority if provided
            if (senderOptions.getOption(ClientOptions.MSG_PRIORITY).hasParsedValue()) {
                int priority = Integer.parseInt(senderOptions.getOption(ClientOptions.MSG_PRIORITY).getValue());
                if (priority < 0 || priority > 10) {
                    LOG.warn("Message priority is not in JMS interval <0, 10>.");
                }
                producer.setPriority(priority);
            }
            // Set Message ID or disable it completely
            if (senderOptions.getOption(ClientOptions.MSG_ID).hasParsedValue()) {
                if (senderOptions.getOption(ClientOptions.MSG_ID).getValue().equals("noid")) {
                    producer.setDisableMessageID(true);
                }
            }
            // Producer does not generate timestamps - for performance only
            producer.setDisableMessageTimestamp(
                Boolean.parseBoolean(senderOptions.getOption(ClientOptions.MSG_NOTIMESTAMP).getValue()));
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected ClientOptions getClientOptions() {
        return senderOptions;
    }
}
