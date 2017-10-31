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

import com.redhat.mqe.lib.ClientOptionManager;
import com.redhat.mqe.lib.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * Default options for ConnectorClient.
 */
public class AacConnectorOptions extends AacClientOptions {
    private List<Option> options = new LinkedList<>();
    private Logger LOG = LoggerFactory.getLogger(AacReceiverOptions.class);
    private final List<Option> connectorDefaultOptions = new LinkedList<>();

    {
        connectorDefaultOptions.addAll(Arrays.asList(
            new Option(ADDRESS, "a", "CCADDRESS", "?", "If specified the C senders and receivers are created for this address"),
            new Option(OBJ_CTRL, "", "OBJCTRL", "C", "Optional creation object control (syntax C/E/S/R/Q stands for Connection, sEssion, Sender, Receiver, Queue)"),
            new Option(COUNT, "c", "CONNCOUNT", "1", "Specify how many connections will make"),
            new Option(Q_COUNT, "", "QCOUNT", "1", "Specify amount of queues created"),
            // TODO JMS+SYNC_MODE?
            new Option(SYNC_MODE, "", "SMODE", "action", "Optional action synchronization mode: none/session/action (JMS does not support none & session modes)")
        ));
    }

    AacConnectorOptions() {
        this.options = ClientOptionManager.mergeOptionLists(super.getDefaultOptions(), connectorDefaultOptions);
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
        return connectorDefaultOptions;
    }

    @Override
    public List<Option> getClientOptions() {
        return options;
    }

    @Override
    public String toString() {
        return "ConnectorOptions{" +
            "options=" + options +
            '}';
    }
    /**
     -h, --help                                            show this help message and exit
     -b USER/PASS@HOST:PORT, --broker USER/PASS@HOST:PORT  connect to specified broker (default guest/guest@localhost:5672)
     --duration DURATION                                   Opened objects will be held until duration passes by, Also the sessions if exists will be synced every T=1s (default 0)
     -a CCADDRESS, --address CCADDRESS                     If specified the C senders and receivers are created for this address (default )
     --obj-ctrl OBJCTRL                                    Optional creation object control (syntax C/E/S/R stands for Connection, sEssion, Sender, Receiver) (default C)
     --sync-mode SMODE                                     Optional action synchronization mode: none/session/action (JMS does not support none & session modes) (default action)
     -c CONN_CNT, --conn-cnt CONN_CNT                      Specify how many connections will make (default 1)
     --con-option NAME=VALUE                               JMS Connection URL options. Ex sync_ack=true sync_publish=all
     --broker-option NAME=VALUE                            JMS Broker URL options. Ex ssl=true sasl_mechs=GSSAPI
     --connection-options {NAME=VALUE,NAME=VALUE..}        QPID Connection URL options. (c++ style)
     */
}
