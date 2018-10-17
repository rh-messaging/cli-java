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
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.io.IOException;
import java.util.Map;

import static java.util.Arrays.asList;

abstract class Client {
    private Logger log = setUpLogger("Client");

    OptionParser parser = new OptionParser();

    OptionSpec<String> destination;
    OptionSpec<String> clientId;
    OptionSpec<String> broker;
    OptionSpec<Integer> qos;
    OptionSpec<Integer> timeout;
    OptionSpec<Integer> msgCount;
    OptionSpec<Void> help;
    OptionSpec<String> logMsgs;
    OptionSpec<Boolean> willFlag;
    OptionSpec<String> willMessage;
    OptionSpec<Integer> willQos;
    OptionSpec<Boolean> willRetained;
    OptionSpec<String> willDestination;
    OptionSpec<String> username;
    OptionSpec<String> password;
    OptionSpec<Integer> keepAlive;
    OptionSpec<Boolean> reconnect;

    String cliDestination;
    String cliClientId;
    int cliQos;
    String cliContent;
    String cliBroker;
    int cliTimeout;
    int cliMsgCount;
    String cliLogMsgs;
    Boolean cliWillFlag;
    String cliWillMessage;
    int cliWillQos;
    Boolean cliWillRetained;
    String cliWillDestination;
    String cliUsername;
    String cliPassword;
    Integer cliKeepAlive;
    Boolean cliReconnect;

    AmcMessageFormatter messageFormatter = new AmcMessageFormatter();

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

        clientId = parser.accepts("conn-clientid", "client id").withRequiredArg()
            .ofType(String.class).defaultsTo("");

        qos = parser.accepts("msg-qos", "message QoS (0,1,2)").withRequiredArg().ofType(Integer.class).defaultsTo(1);
        timeout = parser.accepts("timeout", "receiver timeout in seconds").withRequiredArg().ofType(Integer.class).defaultsTo(10);
        msgCount = parser.acceptsAll(asList("c", "count"), "number of messages").withRequiredArg().ofType(Integer.class).defaultsTo(1);

        logMsgs = parser.accepts("log-msgs", "print messages").withRequiredArg()
            .ofType(String.class).defaultsTo("none");

        willFlag = parser.accepts("conn-will-flag", "will flag (true, false)").withRequiredArg().ofType(Boolean.class).defaultsTo(false);

        willMessage = parser.accepts("conn-will-message", "will message body").withRequiredArg().ofType(String.class).defaultsTo("");

        willQos = parser.accepts("conn-will-qos", "will QoS (0,1,2) ").withRequiredArg().ofType(Integer.class).defaultsTo(0);

        willRetained = parser.accepts("conn-will-retained", "is will retained (true, false)").withRequiredArg().ofType(Boolean.class).defaultsTo(false);

        willDestination = parser.accepts("conn-will-destination", "will topic name").withRequiredArg().ofType(String.class);

        username = parser.accepts("conn-username", "username").withRequiredArg().ofType(String.class).defaultsTo("");
        password = parser.accepts("conn-password", "password").withRequiredArg().ofType(String.class);

        /*If the Keep Alive value is non-zero and the Server does not receive a Control Packet from the Client within one and a half times the Keep Alive time period,
         it MUST disconnect the Network Connection to the Client as if the network had failed */
        keepAlive = parser.accepts("conn-heartbeat", "keep alive interval").withRequiredArg().ofType(Integer.class);

        reconnect = parser.accepts("conn-reconnect", "automatic reconnect (true, false)").withRequiredArg().ofType(Boolean.class).defaultsTo(true);

        help = parser.accepts("help", "This help").forHelp();

        return parser;
    }

    void setOptionValues(OptionSet optionSet) {
        if (optionSet.has(help)) {
            printHelp(parser);
            System.exit(0);
        } else {
            cliBroker = optionSet.valueOf(broker);
            if (!cliBroker.startsWith("tcp://")) {
                cliBroker = "tcp://" + cliBroker;
            }
            cliDestination = optionSet.valueOf(destination);
            cliClientId = optionSet.valueOf(clientId);
            cliQos = optionSet.valueOf(qos);
            cliTimeout = optionSet.valueOf(timeout);
            cliMsgCount = optionSet.valueOf(msgCount);
            cliLogMsgs = optionSet.valueOf(logMsgs);
            cliWillFlag = optionSet.valueOf(willFlag);
            cliWillMessage = optionSet.valueOf(willMessage);
            cliWillQos = optionSet.valueOf(willQos);
            cliWillRetained = optionSet.valueOf(willRetained);
            cliWillDestination = optionSet.valueOf(willDestination);
            cliUsername = optionSet.valueOf(username);
            cliPassword = optionSet.valueOf(password);
            cliKeepAlive = optionSet.valueOf(keepAlive);
            cliReconnect = optionSet.valueOf(reconnect);
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

    void printMessage(String topic, MqttMessage message) {
        Map<String, Object> format;
        switch (cliLogMsgs) {
            case "none":
                return;
            case "body":
                format = messageFormatter.formatMessageBody(message);
                break;
            default:
                format = messageFormatter.formatMessage(topic, message);
        }

        if ("json".equals(cliLogMsgs)) {
            messageFormatter.printMessageAsJson(format);
        } else {
            messageFormatter.printMessageAsPython(format);
        }
    }

    protected MqttConnectOptions setConnectionOptions(MqttConnectOptions connectOptions) {
        if (cliWillFlag) {
            try {
                connectOptions.setWill(cliWillDestination, cliWillMessage.getBytes(), cliWillQos, cliWillRetained);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Will destination cannot be empty.", e);
            }
        }
        if (!cliUsername.isEmpty()) {
            connectOptions.setUserName(cliUsername);
            if (cliPassword != null) {
                connectOptions.setPassword(cliPassword.toCharArray());
            }
        }
         if (cliKeepAlive != null) {
            try {
                connectOptions.setKeepAliveInterval(cliKeepAlive);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Keep alive interval cannot be a negative number.", e);
            }
        }
        connectOptions.setAutomaticReconnect(cliReconnect);
        connectOptions.setCleanSession(true);

        return connectOptions;
    }

    void closeClient(MqttClient client) throws MqttException {
        if (client != null) {
            try {
                client.disconnect();
                client.close();
            } catch (MqttException e) {
                client.close();
                throw e;
            }
        }
    }

    protected Logger setUpLogger(String name) {
        Logger log = Logger.getLogger(name);
        log.setLevel(Level.WARN);
        return log;
    }

}
