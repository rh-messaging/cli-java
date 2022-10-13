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

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import org.apache.logging.log4j.Logger;


/**
 * Sender client to send messages to given topic.
 * Persistence is not necessary if cleanSession is used. (default client yes)
 * Quality of Service 0: at most once, 1: at least once, 2: exactly once
 */
public class Sender extends Client {
    OptionSpec<String> content;
    MemoryPersistence persistence = new MemoryPersistence();
    private Logger log = setUpLogger("Sender");

    private MqttClient sender = null;

    public Sender(String[] args) {
        super(args);
    }

    @Override
    OptionParser populateOptionParser(OptionParser parser) {
        super.populateOptionParser(parser);
        content = parser.accepts("msg-content", "message content").withRequiredArg()
            .ofType(String.class).defaultsTo("");
        return parser;
    }

    @Override
    void setOptionValues(OptionSet optionSet) {
        super.setOptionValues(optionSet);
        cliContent = optionSet.valueOf(content);
    }
    /**
     * Send a message to the topic
     */
    @Override
    public void startClient() throws MqttException {
        try {
            sender = new MqttClient(cliBroker, cliClientId, persistence);
            log.info("Connecting to broker: " + cliBroker);

            MqttConnectOptions connectOptions = new MqttConnectOptions();
            sender.connect(setConnectionOptions(connectOptions));

            MqttMessage message = new MqttMessage(cliContent.getBytes());
            message.setQos(cliQos);
            for (int i = 0; i < cliMsgCount; i++) {
                sender.publish(cliDestination, message);
                printMessage(cliDestination, message);
            }
        } catch (MqttException me) {
            log.error("reason " + me.getReasonCode());
            log.error("msg " + me.getMessage());
            log.error("loc " + me.getLocalizedMessage());
            log.error("cause " + me.getCause());
            log.error("excep " + me);
            me.printStackTrace();
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(1);
        } finally {
            closeClient();
            log.info("Disconnected");
        }
    }

    @Override
    void closeClient() throws MqttException {
        closeClient(sender);
    }
}
