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

import org.eclipse.paho.client.mqttv3.*;

import java.util.logging.Logger;

/**
 * Created by mtoth on 4/26/16.
 */
public class Receiver extends CoreClient implements MqttCallback {
  Logger log;

  public Receiver(String cliBroker) {
    log = setUpLogger("Receiver");
    broker = cliBroker;
  }

  public void receive(String topic, String clientid, int timeout) {
    MqttClient receiver = null;
    timeout = timeout * 1000;
    try {
      receiver = new MqttClient(broker, clientid, null);
      log.fine("Connecting to the broker " + broker);
      receiver.connect(setConnectionOptions(new MqttConnectOptions()));
      receiver.setCallback(this);

      receiver.subscribe(topic);
      log.fine("Subscribed to " + topic);
      // wait for messages to arrive for some time
      long endTime = System.currentTimeMillis() + timeout;
      while (System.currentTimeMillis() < endTime) {
        Thread.sleep(200);
      }
      receiver.unsubscribe(topic);
    } catch (MqttException e) {
      log.severe("Error while subscribing!  " + e.getMessage());
      e.printStackTrace();
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

  public void messageArrived(String topic, MqttMessage message)
      throws Exception {
//    log.info("Message arrived from " + topic);
//    log.info("message=" + message.toString());
    System.out.println(message.toString());
  }

  public void deliveryComplete(IMqttDeliveryToken token) {
    log.info("Delivery of message OK. " + token.toString());
  }
}
