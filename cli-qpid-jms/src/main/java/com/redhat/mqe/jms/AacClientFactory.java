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

import com.redhat.mqe.lib.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AacClientFactory extends ClientFactory {
    private Logger LOG = LoggerFactory.getLogger(AacClientFactory.class);
    MessageFormatter messageFormatter = new AMQPMessageFormatter();
    ClientOptionManager clientOptionManager = new AacClientOptionManager();

    public CoreClient makeSenderClient(String[] args, ConnectionManagerFactory connectionManagerFactory) {
        ClientOptions options = new AacSenderOptions();
        clientOptionManager.applyClientArguments(options, args);

        SenderClient senderClient = new SenderClient(args, connectionManagerFactory, messageFormatter, options);
        return senderClient;
    }

    public CoreClient makeReceiverClient(String[] args, ConnectionManagerFactory connectionManagerFactory) {
        ClientOptions options = new AacReceiverOptions();
        clientOptionManager.applyClientArguments(options, args);

        ReceiverClient receiverClient = new ReceiverClient(args, connectionManagerFactory, messageFormatter, options);
        String browsingMode = receiverClient.getClientOptions().getOption(ClientOptions.BROWSER).getValue();
        if (Boolean.parseBoolean(browsingMode)) {
            LOG.debug("Browsing mode");
            MessageBrowser msgBrowser = new MessageBrowser(receiverClient.getClientOptions(), connectionManagerFactory, messageFormatter);
            return msgBrowser;
        } else {
            return receiverClient;
        }
    }

    public CoreClient makeConnectorClient(String[] args, ConnectionManagerFactory connectionManagerFactory) {
        ClientOptions options = new AacConnectorOptions();
        clientOptionManager.applyClientArguments(options, args);

        ConnectorClient connectorClient = new ConnectorClient(args, connectionManagerFactory, messageFormatter, options);
        return connectorClient;
    }
}
