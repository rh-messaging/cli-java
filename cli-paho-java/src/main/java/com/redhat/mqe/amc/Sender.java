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

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.logging.Logger;


/**
 * Sender client to send messages to given topic.
 * Persistence is not neccessary if cleanSession is used. (default client yes)
 * Quality of Service 0: at most once, 1: at least once, 2: exactly once
 */
public class Sender extends CoreClient {
  Logger log;

  public Sender(String cliBroker) {
    System.out.println(cliBroker);
    broker = cliBroker;
    log = setUpLogger("Sender");
  }

  /**
   * Send a message to the topic
   * @param topic
   * @param qos     At most once (0)
                    At least once (1)
                    Exactly once (2)
   * @param content
   * @param clientId
   * @param msgCount
   */
  public void send(String topic, int qos, String content, String clientId, int msgCount) {
    MqttClient sender = null;
    try {
      sender = new MqttClient(broker, clientId, persistence);
      log.fine("Connecting to broker: " + broker);
      sender.connect(setConnectionOptions(new MqttConnectOptions()));
      MqttMessage message = new MqttMessage(content.getBytes());
      message.setQos(qos);
      for (int i = 0; i < msgCount; i++) {
        sender.publish(topic, message);
      }
    } catch (MqttException me) {
      log.severe("reason " + me.getReasonCode());
      log.severe("msg " + me.getMessage());
      log.severe("loc " + me.getLocalizedMessage());
      log.severe("cause " + me.getCause());
      log.severe("excep " + me);
      me.printStackTrace();
    } finally {
      closeClient(sender);
      log.fine("Disconnected");
    }
  }
}
