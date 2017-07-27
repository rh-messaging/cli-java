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

package com.redhat.mqe.jms;

import com.redhat.mqe.lib.Utils;

import javax.jms.*;

import java.util.UUID;

import static com.redhat.mqe.jms.ClientOptions.*;

/**
 * Class for receiving, consuming and interpreting messages.
 */
public class ReceiverClient extends CoreClient {

  protected static final String SLEEP_AFTER = "after-receive";
  protected static final String SLEEP_AFTER_ACTION = "after-receive-action"; // TODO not implemented
  protected static final String SLEEP_AFTER_TX_ACTION = "after-receive-action-tx-action";
  protected static final String SLEEP_BEFORE = "before-receive";

  private boolean msgListener;
  private boolean durableSubscriber;
  private String durableSubscriberPrefix = null;
  private boolean unsubscribe = false;
  private String durableSubscriberName = null; // passed from user

  /**
   * If false, the client(s) consume(s) own message(s). The behavior is undefined for Queue(s).
   */
  private boolean noLocal; // TODO implement in future
  private boolean transacted;

  private int msgCount;

  /**
   * Session.SESSION_TRANSACTED = 0, Session.AUTO_ACKNOWLEDGE = 1, Session.CLIENT_ACKNOWLEDGE = 2,
   * Session.DUPS_OK_ACKNOWLEDGE = 3. The last three are non-transactional.
   */
  private int txSize;
  private String txEndloopAction;
  private double closeSleep;
  private float duration;
  private long timeout; // milliseconds, needs to be greater than 0
  private String durationMode;
  private boolean processReplyTo;

  /**
   * Selects messages based on the SQL92 syntax subset. Invalid selector causes the client to fail. Example:
   * "JMSXDeliveryCount is not null"
   */
  private String msgSelector;
  private String txAction;

  ReceiverOptions rcvrOpts;
  private String writeBinaryMessageFile;

  ReceiverClient(String[] arguments) {
    rcvrOpts = new ReceiverOptions();
    ClientOptionManager.applyClientArguments(rcvrOpts, arguments);
    String destinationType = rcvrOpts.getOption(DESTINATION_TYPE).getValue();
    LOG.debug("Using destination type:" + destinationType);
  }

  @Override
  ClientOptions getClientOptions() {
    return rcvrOpts;
  }

  void setReceiverClient(ClientOptions options) {
    // TODO add support for noLocal to ClientOptions
    if (options != null) {
      msgCount = Integer.parseInt(options.getOption(COUNT).getValue()) > 0 ? Integer.parseInt(options.getOption(COUNT).getValue()) : 0;
      msgListener = Boolean.parseBoolean(options.getOption(MSG_LISTENER).getValue());
      durableSubscriber = Boolean.parseBoolean(options.getOption(DURABLE_SUBSCRIBER).getValue());
      durableSubscriberPrefix = options.getOption(DURABLE_SUBSCRIBER_PREFIX).getValue();
      unsubscribe = Boolean.parseBoolean(options.getOption(UNSUBSCRIBE).getValue());
      durableSubscriberName = options.getOption(DURABLE_SUBSCRIBER_NAME).getValue();
//      durableConsumer = Boolean.parseBoolean(options.getOption(DURABLE_CONSUMER).getValue()); JMS 2.0
      msgSelector = options.getOption(MSG_SELECTOR).getValue();

      closeSleep = Double.parseDouble(options.getOption(CLOSE_SLEEP).getValue());
      closeSleep *= 1000;
      duration = Float.parseFloat(options.getOption(DURATION).getValue());
      duration *= 1000;
      durationMode = options.getOption(ClientOptions.DURATION_MODE).getValue().toLowerCase();

      timeout = Long.parseLong(options.getOption(TIMEOUT).getValue());
      if (timeout > 0) timeout *= 1000;

      transacted = Boolean.parseBoolean(options.getOption(TRANSACTED).getValue());
      txAction = options.getOption(TX_ACTION).getValue();
      txSize = Integer.parseInt(options.getOption(TX_SIZE).getValue());
      txEndloopAction = options.getOption(TX_ENDLOOP_ACTION).getValue();
      processReplyTo = Boolean.parseBoolean(options.getOption(PROCESS_REPLY_TO).getValue());
      writeBinaryMessageFile = options.getOption(MSG_BINARY_CONTENT_TO_FILE).getValue();
    }
  }

  boolean isAsync() {
    return this.msgListener;
  }

  @Override
  void startClient() {
    this.setReceiverClient(rcvrOpts);
    // Unsubscribe given durable topic subscriber
    if (unsubscribe && durableSubscriberName != null) {
      this.unsubscribe();
    } else {
      this.consumeMessage();
    }
  }

  private void unsubscribe() {
    Connection connection = createConnection(rcvrOpts);
    Session session = createSession(rcvrOpts, connection, transacted);
    try {
      session.unsubscribe(durableSubscriberName);
    } catch (JMSException e) {
      LOG.error("Error while unsubscribing durable subscriptor " + durableSubscriberName);
      e.printStackTrace();
    } finally {
      close(session);
      close(connection);
    }
  }

  /**
   * This method contains logic for consuming messages: - creates Connection - creates Session (transacted vs
   * non-transacted) - creates MessageConsumer with Destination (topic vs Queue), message selector and support for local
   * vs non-local transactions - supports synchronous vs asynchronous (message listener) mode - supports transactions
   */
  void consumeMessage() {
    Connection conn = createConnection(rcvrOpts);
    Session ssn = createSession(rcvrOpts, conn, transacted);
    try {
      MessageConsumer msgConsumer;
      if (durableSubscriber && getDestinationType().equals(ConnectionManager.TOPIC_OBJECT)) {
        createSubscriptionName(durableSubscriberPrefix);
        msgConsumer = ssn.createDurableSubscriber((Topic) getDestination(), durableSubscriberName, msgSelector, noLocal);
      } else {
        msgConsumer = ssn.createConsumer(getDestination(), msgSelector, noLocal);
      }
      MessageListener msgLsnr = new MessageListenerImpl(this);

      if (msgListener) {
        msgConsumer.setMessageListener(msgLsnr);
      }

      conn.start();

      //===  ASYNC ===
      while (msgListener) {
        Utils.sleep((int) duration);
      }

      //=== SYNC ===
      int i = 0;
      Message msg;

      double initialTimestamp = Utils.getTime();
      do {
        if (durationMode.equals(SLEEP_BEFORE)) {
          LOG.trace("Sleeping before receive");
          Utils.sleepUntilNextIteration(initialTimestamp, msgCount, duration, i + 1);
        }

        if (timeout == 0) {
          // TODO JMS SPEC BUG https://java.net/jira/browse/JMS_SPEC-85
          // msg = msgConsumer.receiveNoWait();
          msg = msgConsumer.receive(200); // the lowest number of ms to receive a message was 36ms
        } else if (timeout == -1) {
          msg = msgConsumer.receive(); // == msgConsumer.receive(0)
        } else {
          msg = msgConsumer.receive(timeout);
        }

        if (durationMode.equals(SLEEP_AFTER)) {
          LOG.trace("Sleeping after receive");
          Utils.sleepUntilNextIteration(initialTimestamp, msgCount, duration, i + 1);
        }

        if (ssn.getAcknowledgeMode() == Session.CLIENT_ACKNOWLEDGE && msg != null) {
          msg.acknowledge();
        }

        if (msg != null) {
          if (!writeBinaryMessageFile.isEmpty()) {
            Utils.writeBinaryContentToFile(writeBinaryMessageFile, msg, i);
          }
          i++;
          printMessage(rcvrOpts, msg);
        } else {
          LOG.trace("Did not receive any message!");
        }

        //=== TRANSACTION ===
        if (ssn.getTransacted() && txSize != 0) {
          if (i % txSize == 0) {
            CoreClient.doTransaction(ssn, txAction);

            if (durationMode.equals(SLEEP_AFTER_TX_ACTION)) {
              LOG.trace("Sleeping after transaction");
              Utils.sleepUntilNextIteration(initialTimestamp, msgCount, duration, i + 1);
            }
          }
        }

        //=== REPLY TO ===
        if (processReplyTo && msg != null && msg.getJMSReplyTo() != null) {
          MessageProducer msgProducer = ssn.createProducer(msg.getJMSReplyTo());
          msg.setJMSReplyTo(null);
          msgProducer.send(msg);
          close(msgProducer);
        }

        if (i == msgCount) {
          close(msgConsumer);
          break; // or timeout
        }
      } while (msg != null);

      if (ssn.getTransacted()) {
        LOG.trace("Performing tx-endloop-action " + txEndloopAction);
        CoreClient.doTransaction(ssn, txEndloopAction);
      }
    } catch (InvalidSelectorException se) {
      LOG.error("Invalid selector \"{}\" has been specified.", msgSelector);
      se.printStackTrace();
      System.exit(2);
    } catch (JMSException jmse) {
      LOG.error("Exception while consuming message!");
      jmse.printStackTrace();
      System.exit(1);
    } finally {
      if (closeSleep > 0) {
        Utils.sleep((int) closeSleep);
      }
      close(ssn);
      close(conn);
    }
  }

  private void createSubscriptionName(String customPrefix) {
    if (durableSubscriberName == null) {
      UUID uuid = UUID.randomUUID();
      if (customPrefix == null || customPrefix.equals(""))
        durableSubscriberName = "qpid-jms-" + uuid.toString();
      else
        durableSubscriberName = customPrefix + uuid.toString();
    }
    LOG.debug("DurableSubscriptionName=" + durableSubscriberName);
  }
}
