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

package com.redhat.mqe.aoc;

import com.redhat.mqe.lib.ClientFactory;
import com.redhat.mqe.lib.ClientOptionManager;
import com.redhat.mqe.lib.ClientOptions;
import com.redhat.mqe.lib.ConnectionManagerFactory;
import com.redhat.mqe.lib.ConnectorClient;
import com.redhat.mqe.lib.ConnectorOptions;
import com.redhat.mqe.lib.CoreClient;
import com.redhat.mqe.lib.MessageBrowser;
import com.redhat.mqe.lib.MessageFormatter;
import com.redhat.mqe.lib.OpenwireMessageFormatter;
import com.redhat.mqe.lib.ReceiverClient;
import com.redhat.mqe.lib.ReceiverOptions;
import com.redhat.mqe.lib.SenderClient;
import com.redhat.mqe.lib.SenderOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AocClientFactory extends ClientFactory {
    private Logger LOG = LoggerFactory.getLogger(AocClientFactory.class);
    MessageFormatter messageFormatter = new OpenwireMessageFormatter();
    ClientOptionManager clientOptionManager = new AocClientOptionManager();

    @Override
    public CoreClient makeSenderClient(String[] args, ConnectionManagerFactory connectionManagerFactory) {
        ClientOptions options = new SenderOptions();
        clientOptionManager.applyClientArguments(options, args);

        SenderClient senderClient = new SenderClient(args, connectionManagerFactory, messageFormatter, options);
        return senderClient;
    }

    @Override
    public CoreClient makeReceiverClient(String[] args, ConnectionManagerFactory connectionManagerFactory) {
        ClientOptions options = new ReceiverOptions();
        clientOptionManager.applyClientArguments(options, args);

        ReceiverClient receiverClient = new ReceiverClient(args, connectionManagerFactory, messageFormatter, options);
        String browsingMode = receiverClient.getClientOptions().getOption(ClientOptions.BROWSER).getValue();
        try {
            if (Boolean.parseBoolean(browsingMode)) {
                LOG.debug("Browsing mode");
                MessageBrowser msgBrowser = new MessageBrowser(receiverClient.getClientOptions(), connectionManagerFactory, messageFormatter);
                return msgBrowser;
            } else {
                return receiverClient;
            }
        } catch (Exception e) {
            LOG.error("Error while consuming messages!", e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
        return null;
    }

    @Override
    public CoreClient makeConnectorClient(String[] args, ConnectionManagerFactory connectionManagerFactory) {
        ClientOptions options = new ConnectorOptions();
        clientOptionManager.applyClientArguments(options, args);

        ConnectorClient connectorClient = new ConnectorClient(args, connectionManagerFactory, messageFormatter, options);
        return connectorClient;
    }
}
