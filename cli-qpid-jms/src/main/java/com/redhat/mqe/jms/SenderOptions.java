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

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * Default options for SenderClient.
 */
public class SenderOptions extends ClientOptions {

  private List<com.redhat.mqe.lib.Option> options = new LinkedList<com.redhat.mqe.lib.Option>();
  private Logger LOG = LoggerFactory.getLogger(ReceiverOptions.class);
  private final List<com.redhat.mqe.lib.Option> senderDefaultOptions = new LinkedList<com.redhat.mqe.lib.Option>();

  {
    senderDefaultOptions.addAll(Arrays.asList(
        new com.redhat.mqe.lib.Option(ADDRESS, "a", "ADDRESS", "", "Queue/Topic destination"),
        new com.redhat.mqe.lib.Option(TIMEOUT, "t", "TIMEOUT", "0", "timeout in seconds to wait before exiting. Use -1 to wait forever."),
        new com.redhat.mqe.lib.Option(COUNT, "c", "MESSAGES", "1", "stop after count messages have been sent, zero disables"),
        new com.redhat.mqe.lib.Option(DURATION, "d", "DURATION", "0", "message actions total duration in seconds (defines msg-rate together with count)"),
        new com.redhat.mqe.lib.Option(DURATION_MODE, "", "VALUE", "after-send", "specifies where to wait (before-send/after-send/after-send-tx-action"),
        new com.redhat.mqe.lib.Option(MSG_ID, "i", "MSG_ID", "", "use the supplied id instead of generating one. use \'noid\' to not generate IDs"),
        new com.redhat.mqe.lib.Option(PROPERTY_TYPE, "", "PTYPE", "String", "specify the type of message property"),
        new com.redhat.mqe.lib.Option(MSG_PROPERTY, "", "KEY=PVALUE", "", "specify message property as KEY=VALUE (use '~' instead of '=' for auto-casting)"),
        new com.redhat.mqe.lib.Option(CONTENT_TYPE, "", "CTYPE", "String", "specify type of the actual content type"),
        new com.redhat.mqe.lib.Option(MSG_CONTENT_TYPE, "", "MSGTYPE", "", "type of message body to use in header (JMSType)"),
        new com.redhat.mqe.lib.Option(MSG_CONTENT_FROM_FILE, "", "PATH", "", "specify filename to load content from"),
        new com.redhat.mqe.lib.Option(MSG_CONTENT, "", "CONTENT", "", "actual content fed to message body"),
        new com.redhat.mqe.lib.Option(MSG_CONTENT_BINARY, "false", "BIN_CONTENT", "", "is message content binary"),
        new com.redhat.mqe.lib.Option(MSG_CONTENT_LIST_ITEM, "L", "VALUE", "", "item from list"),
        new com.redhat.mqe.lib.Option(MSG_CONTENT_MAP_ITEM, "M", "KEY=VALUE", "", "Map item specified as KEY=VALUE (use '~' instead of '=' for auto-casting)"),
        new com.redhat.mqe.lib.Option(MSG_NOTIMESTAMP, "", "TIMESTAMP", "false", "producer do not create timestamps for messages"),
        new com.redhat.mqe.lib.Option(MSG_REPLY_TO, "", "QUEUE", "", "reply to provided queue"),
        new com.redhat.mqe.lib.Option(MSG_SUBJECT, "", "SUBJECT", "", "specify message subject"),
        new com.redhat.mqe.lib.Option(MSG_DURABLE, "", "MSG_DURABLE", "yes", "send durable messages: yes/no|true/false"),
        new com.redhat.mqe.lib.Option(LOG_MSGS, "", "LOGMSGFMT", "upstream", "message[s] reporting style (dict|body|upstream|none)"),
        new com.redhat.mqe.lib.Option(LOG_STATS, "", "LEVEL", "upstream", "report various statistic/debug information"),
        new com.redhat.mqe.lib.Option(MSG_TTL, "", "TTL", "0", "message time-to-live (ms)"),
        new com.redhat.mqe.lib.Option(MSG_PRIORITY, "", "MSG_PRIORITY", "4", "message priority"),
        new com.redhat.mqe.lib.Option(MSG_CORRELATION_ID, "", "MSGCORRID", "", "message correlation id"),
        new com.redhat.mqe.lib.Option(MSG_USER_ID, "", "USER", "", "obsolete! use '" + CON_POPULATE_JMSXUSERID + "'"),
        new com.redhat.mqe.lib.Option(MSG_GROUP_ID, "", "GROUPID", "", "message group id - JMSXGroupID"),
        new com.redhat.mqe.lib.Option(MSG_GROUP_SEQ, "", "SEQUENCE", "", "message group sequence - JMSXGroupSeq"),
        new com.redhat.mqe.lib.Option(MSG_REPLY_TO_GROUP_ID, "", "GROUPID", "", "reply to message group id"),
        new com.redhat.mqe.lib.Option(TX_SIZE, "", "TXSIZE", "0", "transactional mode: batch message count size"),
        new com.redhat.mqe.lib.Option(TX_ACTION, "", "TXACTION", "commit", "transactional action at the end of tx batch (commit|rollback|recover|None)"),
        new com.redhat.mqe.lib.Option(TX_ENDLOOP_ACTION, "", "TXACTION", "None", "transactional action after sending all messages in loop (commit|rollback|recover|None)"),
        // TODO
        new com.redhat.mqe.lib.Option(SYNC_MODE, "", "SYNCMODE", "action", "synchronization mode: none/session/action/persistent/transient"),
        new com.redhat.mqe.lib.Option(CAPACITY, "", "CAPACITY", "-1", "sender|receiver capacity (no effect in jms atm)")
    ));
  }

  public SenderOptions() {
    this.options = ClientOptionManager.mergeOptionLists(super.getDefaultOptions(), senderDefaultOptions);
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
    // TODO fix this!?
    return null;
  }

  @Override
  public List<com.redhat.mqe.lib.Option> getClientDefaultOptions() {
    return senderDefaultOptions;
  }

  public List<com.redhat.mqe.lib.Option> getClientOptions() {
    return options;
  }

  @Override
  public String toString() {
    return "SenderOptions{" +
        "options=" + options +
        '}';
  }

  /**
   -h, --help                                            show this help message and exit
   -b USER/PASS@HOST:PORT, --broker USER/PASS@HOST:PORT  connect to specified broker (default guest/guest@localhost:5672)
   -t TIMEOUT, --timeout TIMEOUT                         timeout in seconds to wait before exiting (default 0)
   -c COUNT, --count COUNT                               stop after count messages have been sent, zero disables (default 1)
   -i, --id                                              use the supplied id instead of generating one
   --duration DURATION                                   message actions total duration (defines msg-rate together with count) (default 0)
   -P NAME=VALUE, --property NAME=VALUE                  specify message property
   -M KEY=VALUE, --map KEY=VALUE                         specify entry for map content
   --content TEXT                                        specify textual content
   --content-from-file TEXT                              specify filename to load content from
   --con-option NAME=VALUE                               JMS Connection URL options. Ex sync_ack=true sync_publish=all
   --broker-option NAME=VALUE                            JMS Broker URL options. Ex ssl=true sasl_mechs=GSSAPI
   --connection-options {NAME=VALUE,NAME=VALUE..}        QPID Connection URL options. (c++ style)
   --log-msgs LOGMSGFMT                                  message[s] reporting style (dict|body|upstream|none) (default )
   --log-stats LEVEL                                     report various statistic/debug information (default )
   --tx-batch-size TXBSIZE                               transactional mode: batch message count size (negative skips tx-action before exit) (default 0)
   --tx-action TXACTION                                  transactional action at the end of tx batch (default commit)
   --sync-mode SMODE                                     synchronization mode: none/session/action/persistent/transient (default action)

   --durable MSG_DURABLE                                     send durable messages: yes/no (default )
   --ttl TTL                                             message time-to-live (ms) (default 0)
   --priority MSG_PRIORITY                                   message time-to-live (ms) (default -1)
   --capacity CPCT                                       sender|receiver capacity (no effect in jms atm) (default -1)
   --close-sleep CSLEEP                                  sleep before publisher/subscriber/session/connection.close() (default 0)
   */
}
