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
import org.eclipse.paho.client.mqttv3.*;

import java.util.logging.Logger;

public class Receiver extends Client implements MqttCallback {
    private Logger log = setUpLogger("Receiver");

    public Receiver(String[] args) {
        super(args);
    }

    @Override
    OptionParser populateOptionParser(OptionParser parser) {
        super.populateOptionParser(parser);
        return parser;
    }

    @Override
    public void startClient() throws MqttException {
        MqttClient receiver = null;
        cliTimeout = cliTimeout * 1000;
        try {
            receiver = new MqttClient(cliBroker, cliClientId, null);
            log.fine("Connecting to the broker " + cliBroker);

            MqttConnectOptions connectOptions = new MqttConnectOptions();

            if (cliWillFlag) {
                if (cliWillDestination.isEmpty()) {
                    log.severe("Will destination cannot be empty.");
                    System.exit(0);
                }

                if (cliWillMessage.isEmpty()) {
                    log.severe("Will message body cannot be empty.");
                    System.exit(0);
                }

                connectOptions.setWill(cliWillDestination, cliWillMessage.getBytes(), cliWillQos, cliWillRetained);
            }


            receiver.connect(setConnectionOptions(connectOptions, cliUsername, cliPassword));
            receiver.setCallback(this);

            receiver.subscribe(cliDestination);
            log.fine("Subscribed to " + cliDestination);
            // wait for messages to arrive for some time
            long endTime = System.currentTimeMillis() + cliTimeout;
            while (System.currentTimeMillis() < endTime) {
                Thread.sleep(200);
            }
            receiver.unsubscribe(cliDestination);
        } catch (MqttException e) {
            log.severe("Error while subscribing!  " + e.getMessage());
            throw e;
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            closeClient(receiver);
        }
    }

    public void connectionLost(Throwable cause) {
        log.severe("Connection lost! " + cause.getMessage());
        cause.printStackTrace();
    }

    public void messageArrived(String topic, MqttMessage message) {
        printMessage(topic, message);
    }

    public void deliveryComplete(IMqttDeliveryToken token) {
        log.info("Delivery of message OK. " + token.toString());
    }
}
