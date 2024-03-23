/*
 * Copyright (c) 2022 Red Hat, Inc.
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

import org.apache.qpid.protonj2.client.Client;
import org.apache.qpid.protonj2.client.Connection;
import org.apache.qpid.protonj2.client.ConnectionOptions;
import picocli.CommandLine;

import java.net.URI;
import java.util.concurrent.Callable;

@CommandLine.Command(
    name = "connector",
    mixinStandardHelpOptions = true,
    version = "1.0.0",
    description = "Opens AMQP connections"
)
class CliProtonJ2Connector extends CliProtonJ2SenderReceiverConnector implements Callable<Integer> {
    @CommandLine.Option(names = {"-b", "--broker"}, description = "")
    private final String broker = "";

    @CommandLine.Option(names = {"-a", "--address"}, description = "")
    private final String address = "";

    @CommandLine.Option(names = {"--count"}, description = "")
    private final int count = 1;

    @Override
    public Integer call() throws Exception {
        configureLogging();

        String prefix = "";
        if (!broker.startsWith("amqp://") && !broker.startsWith("amqps://")) {
            prefix = "amqp://";
        }
        final URI url = new URI(prefix + broker);
        final String serverHost = url.getHost();
        int serverPort = url.getPort();
        serverPort = (serverPort == -1) ? 5672 : serverPort;

        final Client client = Client.create();

        final ConnectionOptions options = getConnectionOptions();

        try (Connection connection = client.connect(serverHost, serverPort, options)) {
        }

        client.close();

        return 0;
    }
}
