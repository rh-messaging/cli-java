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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Default options for ReceiverClient.
 * Constructed from CommonOptions and receiverDefaultOptions.
 */
public class ReceiverOptions extends ClientOptions {
    private List<Option> options = null;
    private Logger LOG = LoggerFactory.getLogger(ReceiverOptions.class);
    private final List<Option> receiverDefaultOptions = new ArrayList<Option>();

    {
        receiverDefaultOptions.addAll(Arrays.asList(
            // 
            new Option(ADDRESS, "a", "ADDRESS", "", "Queue/Topic destination"),
            new Option(TIMEOUT, "t", "TIMEOUT", "0", "timeout in seconds to wait before exiting"),
            new Option("forever", "f", "", "false", "DEPRECATED! use \"timeout -1\" ignore timeout and wait forever"),
            new Option(ACTION, "", "ACTION", "acknowledge", "action on acquired message (default ack)"),
            new Option(COUNT, "c", "MESSAGES", "0", "read c messages, then exit (default 0 for all messages)"),
            new Option(DURATION, "d", "DURATION", "0", "message actions total duration in seconds (defines msg-rate together with count)"),
            new Option(LOG_MSGS, "", "LOGMSGFMT", "upstream", "message[s] reporting style (dict|body|upstream|none)"),
            new Option(LOG_STATS, "", "LEVEL", "upstream", "report various statistic/debug information"),
            new Option(OUT, "", "FORMAT", "repr", "message[s] reporting format (repr|json)"),
            new Option(TX_SIZE, "", "TXBSIZE", "0", "transactional mode: batch message count size (negative skips tx-action before exit)"),
            new Option(TX_ACTION, "", "TXACTION", "commit", "transactional action at the end of tx batch"),
            new Option(TX_ENDLOOP_ACTION, "", "TXACTION", "None", "transactional action after sending all messages in loop (commit|rollback|recover|None)"),
            new Option(DURATION_MODE, "", "VALUE", ReceiverClient.SLEEP_AFTER, "specifies where to wait (" + ReceiverClient.SLEEP_BEFORE
                + "/" + ReceiverClient.SLEEP_AFTER + "/" + ReceiverClient.SLEEP_AFTER_ACTION + "/" + ReceiverClient.SLEEP_AFTER_TX_ACTION + ")"),
            new Option(SYNC_MODE, "", "SYNCMODE", "action", "synchronization mode: none/session/action/persistent/transient"),
            new Option(MSG_LISTENER, "", "ENABLED", "false", "receive messages using a MessageListener"),
            new Option(DURABLE_SUBSCRIBER, "", "ENABLED", "false", "create durable subscription to topic"),
            new Option(UNSUBSCRIBE, "", "UNSUBSCRIBE", "false", "unsubscribe durable subscriptor with given name (provide " + DURABLE_SUBSCRIBER_NAME + ")"),
            new Option(DURABLE_SUBSCRIBER_PREFIX, "", "PREFIX", "", "prefix to use to identify this connection subscriber"),
            new Option(DURABLE_SUBSCRIBER_NAME, "", "PREFIX", "", "name of the durable subscriber to be unsubscribe"),
//        new Option(DURABLE_CONSUMER, "", "ENABLED", "false", "create durable consumer from topic"), JMS 2.0
            new Option(MSG_SELECTOR, "", "SELECT", "", "select messages based on the SQL92 subset"),
            new Option("verbose", "", "", "false", "DEPRECATED? verbose AMQP message output"),
            new Option(CAPACITY, "", "CAPACITY", "-1", "sender|receiver capacity (no effect in jms atm)"),
            new Option(BROWSER, "", "ENABLED", "false", "if true, browse messages instead of reading"),
            new Option(PROCESS_REPLY_TO, "", null, "", "whether to process reply to (true) or ignore it"),
            new Option(MSG_BINARY_CONTENT_TO_FILE, "", "FILEPATH", "", "write binary data to provided file with prefix"),
            new Option(MSG_CONTENT_TO_FILE, "", "FILEPATH", "", "write message content to provided file with prefix")
        ));
//    receiverDefaultOptions.put("forever", "false"); // drain only option
//    receiverDefaultOptions.put("action", "acknowledge"); // acknowledge, reject, release, noack

    }

    @Inject
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
    public Option getOption(String name) {
        if (name != null) {
            for (Option option : options) {
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
    public List<Option> getClientDefaultOptions() {
        return receiverDefaultOptions;
    }

    @Override
    public List<Option> getClientOptions() {
        return options;
    }

    @Override
    public String toString() {
        return "ReceiverOptions{" +
            "options=" + options;
    }

}
