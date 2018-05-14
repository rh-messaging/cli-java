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

package com.redhat.mqe.amc;

import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;

import java.io.IOException;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.util.Arrays.asList;

abstract class Client {
    OptionParser parser = new OptionParser();

    OptionSpec<String> destination;
    OptionSpec<String> clientId;
    OptionSpec<String> broker;
    OptionSpec<Integer> qos;
    OptionSpec<Integer> timeout;
    OptionSpec<Integer> msgCount;
    OptionSpec<Void> help;

    String cliDestination;
    String cliClientId;
    int cliQos;
    String cliContent;
    String cliBroker;
    int cliTimeout;
    int cliMsgCount;
    String cliLogMsgs;

    Client(String[] args) {
        populateOptionParser(parser);
        OptionSet optionSet;
        try {
            optionSet = parser.parse(args);
        } catch (OptionException e) {
            System.err.println(e.getMessage());
            printHelp(parser);
            throw e;
        }
        setOptionValues(optionSet);
    }

    /**
     * Method starts the given client. Serves as entry point.
     */
    public abstract void startClient() throws Exception;

    /**
     * Create accepting command line options for parser.
     *
     * @return option parser to be used
     */
    OptionParser populateOptionParser(OptionParser parser) {
        broker = parser.acceptsAll(asList("b", "broker"), "broker address").withRequiredArg()
            .ofType(String.class).defaultsTo("tcp://localhost:1883").describedAs("default mqtt broker address");

        destination = parser.acceptsAll(asList("a", "address"), "mqtt topic name").withRequiredArg()
            .ofType(String.class).defaultsTo("mqttTopic").describedAs("default mqtt topic destination");

        clientId = parser.accepts("client-id", "client id").withRequiredArg()
            .ofType(String.class).defaultsTo("");

        qos = parser.accepts("msg-qos", "message QoS (0,1,2)").withRequiredArg().ofType(Integer.class).defaultsTo(1);
        timeout = parser.accepts("timeout", "receiver timeout in seconds").withRequiredArg().ofType(Integer.class).defaultsTo(10);
        msgCount = parser.acceptsAll(asList("c", "count"), "number of messages").withRequiredArg().ofType(Integer.class).defaultsTo(1);

        help = parser.accepts("help", "This help").forHelp();

        return parser;
    }

    void setOptionValues(OptionSet optionSet) {
        if (optionSet.has(help)) {
            printHelp(parser);
            System.exit(0);
        } else {
            cliBroker = optionSet.valueOf(broker);
            cliDestination = optionSet.valueOf(destination);
            cliClientId = optionSet.valueOf(clientId);
            cliQos = optionSet.valueOf(qos);
            cliTimeout = optionSet.valueOf(timeout);
            cliMsgCount = optionSet.valueOf(msgCount);
        }
    }

    private void printHelp(OptionParser parser) {
        try {
            System.out.println("Usage: (default credentials admin/admin)");
            System.out.println("<sender|receiver> <Option> [<Option> <Option>...]");
            parser.printHelpOn(System.out);
        } catch (IOException e) {
            throw new RuntimeException("Unable to print help onto stdout", e);
        }
    }

    static MqttConnectOptions setConnectionOptions(MqttConnectOptions connectOptions) {
        connectOptions.setUserName("admin");
        connectOptions.setPassword("admin".toCharArray());
        connectOptions.setCleanSession(true);

        return connectOptions;
    }

    void closeClient(MqttClient client) throws MqttException {
        if (client != null) {
            client.disconnect();
            client.close();
        }
    }

    protected Logger setUpLogger(String name) {
        Logger log = Logger.getLogger(name);
        ConsoleHandler handler = new ConsoleHandler();
        log.setLevel(Level.FINE);
        handler.setLevel(Level.FINE);
        log.addHandler(handler);
        return log;
    }
}
