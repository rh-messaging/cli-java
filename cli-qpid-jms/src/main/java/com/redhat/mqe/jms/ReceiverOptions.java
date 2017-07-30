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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Default options for ReceiverClient.
 * Constructed from CommonOptions and receiverDefaultOptions.
 */
public class ReceiverOptions extends ClientOptions {
  private List<com.redhat.mqe.lib.Option> options = null;
  private Logger LOG = LoggerFactory.getLogger(ReceiverOptions.class);
  private final List<com.redhat.mqe.lib.Option> receiverDefaultOptions = new ArrayList<com.redhat.mqe.lib.Option>();

  {
    receiverDefaultOptions.addAll(Arrays.asList(
            // 
        new com.redhat.mqe.lib.Option(ADDRESS, "a", "ADDRESS", "", "Queue/Topic destination"),
        new com.redhat.mqe.lib.Option(TIMEOUT, "t", "TIMEOUT", "0", "timeout in seconds to wait before exiting"),
        new com.redhat.mqe.lib.Option("forever", "f", "", "false", "DEPRECATED! use \"timeout -1\" ignore timeout and wait forever"),
        new com.redhat.mqe.lib.Option(ACTION, "", "ACTION", "acknowledge", "action on acquired message (default ack)"),
        new com.redhat.mqe.lib.Option(COUNT, "c", "MESSAGES", "0", "read c messages, then exit (default 0 for all messages)"),
        new com.redhat.mqe.lib.Option(DURATION, "d", "DURATION", "0", "message actions total duration in seconds (defines msg-rate together with count)"),
        new com.redhat.mqe.lib.Option(LOG_MSGS, "", "LOGMSGFMT", "upstream", "message[s] reporting style (dict|body|upstream|none)"),
        new com.redhat.mqe.lib.Option(LOG_STATS, "", "LEVEL", "upstream", "report various statistic/debug information"),
        new com.redhat.mqe.lib.Option(TX_SIZE, "", "TXBSIZE", "0", "transactional mode: batch message count size (negative skips tx-action before exit)"),
        new com.redhat.mqe.lib.Option(TX_ACTION, "", "TXACTION", "commit", "transactional action at the end of tx batch"),
        new com.redhat.mqe.lib.Option(TX_ENDLOOP_ACTION, "", "TXACTION", "None", "transactional action after sending all messages in loop (commit|rollback|recover|None)"),
        new com.redhat.mqe.lib.Option(DURATION_MODE, "", "VALUE", ReceiverClient.SLEEP_AFTER, "specifies where to wait (" + ReceiverClient.SLEEP_BEFORE
            + "/" + ReceiverClient.SLEEP_AFTER + "/" + ReceiverClient.SLEEP_AFTER_ACTION + "/" + ReceiverClient.SLEEP_AFTER_TX_ACTION + ")"),
        new com.redhat.mqe.lib.Option(SYNC_MODE, "", "SYNCMODE", "action", "synchronization mode: none/session/action/persistent/transient"),
        new com.redhat.mqe.lib.Option(MSG_LISTENER, "", "ENABLED", "false", "receive messages using a MessageListener"),
        new com.redhat.mqe.lib.Option(DURABLE_SUBSCRIBER, "", "ENABLED", "false", "create durable subscription to topic"),
        new com.redhat.mqe.lib.Option(UNSUBSCRIBE, "", "UNSUBSCRIBE", "false", "unsubscribe durable subscriptor with given name (provide " + DURABLE_SUBSCRIBER_NAME +")"),
        new com.redhat.mqe.lib.Option(DURABLE_SUBSCRIBER_PREFIX, "", "PREFIX", "", "prefix to use to identify this connection subscriber"),
        new com.redhat.mqe.lib.Option(DURABLE_SUBSCRIBER_NAME, "", "PREFIX", "", "name of the durable subscriber to be unsubscribe"),
//        new Option(DURABLE_CONSUMER, "", "ENABLED", "false", "create durable consumer from topic"), JMS 2.0
        new com.redhat.mqe.lib.Option(MSG_SELECTOR, "", "SELECT", "", "select messages based on the SQL92 subset"),
        new com.redhat.mqe.lib.Option("verbose", "", "", "false", "DEPRECATED? verbose AMQP message output"),
        new com.redhat.mqe.lib.Option(CAPACITY, "", "CAPACITY", "-1", "sender|receiver capacity (no effect in jms atm)"),
        new com.redhat.mqe.lib.Option(BROWSER, "", "ENABLED", "false", "if true, browse messages instead of reading"),
        new com.redhat.mqe.lib.Option(PROCESS_REPLY_TO, "", null, "", "whether to process reply to (true) or ignore it"),
        new com.redhat.mqe.lib.Option(MSG_BINARY_CONTENT_TO_FILE, "", "FILEPATH", "", "write binary data to provided file with prefix")
    ));
//    receiverDefaultOptions.put("forever", "false"); // drain only option
//    receiverDefaultOptions.put("action", "acknowledge"); // acknowledge, reject, release, noack

  }

  public ReceiverOptions() {
    this.options = ClientOptionManager.mergeOptionLists(super.getDefaultOptions(), receiverDefaultOptions);
    /**
     -h, --help                                            show this help message and exit
     -b USER/PASS@HOST:PORT, --broker USER/PASS@HOST:PORT  connect to specified broker (default guest/guest@localhost:5672)
     -t TIMEOUT, --timeout TIMEOUT                         timeout in seconds to wait before exiting (default 0)
     -f, --forever                                         ignore timeout and wait forever
     -c COUNT, --count COUNT                               read c messages, then exit (default 0)
     --duration DURATION                                   message actions total duration (defines msg-rate together with count) (default 0)
     --con-option NAME=VALUE                               JMS Connection URL options. Ex sync_ack=true sync_publish=all
     --broker-option NAME=VALUE                            JMS Broker URL options. Ex ssl=true sasl_mechs=GSSAPI
     --connection-options {NAME=VALUE,NAME=VALUE..}        QPID Connection URL options. (c++ style)
     --accept ACTION                                       action on acquired message (default ack)
     -log-msgs LOGMSGFMT                                  message[s] reporting style (dict|body|upstream|none) (default upstream)
     * --log-stats LEVEL                                     report various statistic/debug information (default )
     --tx-batch-size TXBSIZE                               transactional mode: batch message count size (negative skips tx-action before exit) (default 0)
     --tx-action TXACTION                                  transactional action at the end of tx batch (default commit)
     --sync-mode SMODE                                     synchronization mode: none/session/action/persistent/transient (default action)
     --msg-listener-ena                                    receive messages using a MessageListener
     --verbose                                             verbose AMQP message output
     --capacity CPCT                                       sender|receiver capacity (no effect in jms atm) (default -1)
     --close-sleep CSLEEP                                  sleep before publisher/subscriber/session/connection.close() (default 0)
     TODO *
     */
  }

  @Override
  public com.redhat.mqe.lib.Option getOption(String name) {
    if (name != null) {
      for (com.redhat.mqe.lib.Option option : options) {
        if (name.equals(option.getName()))
          return option;
      }
    } else {
      LOG.error("Accessing client options map with null key.");
      throw new IllegalArgumentException("Null name is not allowed!");
    }
    return null;
  }

  @Override
  public List<com.redhat.mqe.lib.Option> getClientDefaultOptions() {
    return receiverDefaultOptions;
  }

  @Override
  public List<com.redhat.mqe.lib.Option> getClientOptions() {
    return options;
  }

  @Override
  public String toString() {
    return "ReceiverOptions{" +
        "options=" + options;
  }

}
