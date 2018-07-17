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

import org.eclipse.paho.client.mqttv3.MqttException;

import java.util.Arrays;

public class Main {
    public static void main(String[] args) throws Exception {
        Client client = null;
        if (args.length >= 1) {
            String[] subArgs = Arrays.copyOfRange(args, 1, args.length);
            switch (args[0]) {
                case "sender": {
                    client = new Sender(subArgs);
                    break;
                }
                case "receiver": {
                    client = new Receiver(subArgs);
                    break;
                }
            }
        }
        if (client != null) {
            final Client finalClient = client;
            Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override
                public void run() {
                        try {
                            finalClient.closeClient();
                        } catch (MqttException e) {
                            e.printStackTrace();
                        }
                    }
            });
            client.startClient();
        } else {
            printHelpAndExit();
        }
    }

    private static void printHelpAndExit() {
        System.out.println("first argument must be sender, receiver OR connector");
        System.exit(1);
    }
}
