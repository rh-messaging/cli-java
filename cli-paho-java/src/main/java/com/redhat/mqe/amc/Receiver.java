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
import org.apache.logging.log4j.Logger;
import org.eclipse.paho.client.mqttv3.*;


public class Receiver extends Client implements MqttCallback {
    private final Logger log = setUpLogger("Receiver");

    private MqttClient receiver = null;

    public Receiver(String[] args) {
        super(args);
    }

    @Override
    OptionParser populateOptionParser(OptionParser parser) {
        super.populateOptionParser(parser);
        return parser;
    }

    @Override
    void closeClient() throws MqttException {
        closeClient(receiver);
    }

    @Override
    public void startClient() throws MqttException {
        cliTimeout = cliTimeout * 1000;
        try {
            receiver = new MqttClient(cliBroker, cliClientId, null);
            log.info("Connecting to the broker " + cliBroker);

            MqttConnectOptions connectOptions = new MqttConnectOptions();
            receiver.connect(setConnectionOptions(connectOptions));

            receiver.setCallback(this);
            receiver.subscribe(cliDestination);
            log.info("Subscribed to " + cliDestination);
            // wait for messages to arrive for some time
            long endTime = System.currentTimeMillis() + cliTimeout;
            while (System.currentTimeMillis() < endTime) {
                Thread.sleep(200);
            }
            receiver.unsubscribe(cliDestination);
        } catch (MqttException e) {
            log.error("Error while subscribing!  " + e.getMessage());
            throw e;
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(1);
        } finally {
            closeClient();
        }
    }

    public void connectionLost(Throwable cause) {
        log.warn("Connection lost! " + cause.getMessage());
        cause.printStackTrace();
    }

    public void messageArrived(String topic, MqttMessage message) {
        printMessage(topic, message);
    }

    public void deliveryComplete(IMqttDeliveryToken token) {
        log.info("Delivery of message OK. " + token.toString());
    }
}
