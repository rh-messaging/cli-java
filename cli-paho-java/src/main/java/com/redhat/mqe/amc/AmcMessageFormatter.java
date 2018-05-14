/*
 * Copyright (c) 2018 Red Hat, Inc.
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

import com.redhat.mqe.lib.MessageFormatter;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.HashMap;
import java.util.Map;

public class AmcMessageFormatter extends MessageFormatter {
    Map<String, Object> formatMessage(String topic, MqttMessage message) {
        HashMap<String, Object> format = new HashMap<>();
        format.put("qos", message.getQos());
        format.put("payload", message.getPayload());
        format.put("duplicate", message.isDuplicate());
        format.put("retained", message.isRetained());
        format.put("address", topic);
        return format;
    }

    public Map<String, Object> formatMessageBody(MqttMessage message) {
        Map<String, Object> messageData = new HashMap<>();
        messageData.put("content", message.getPayload());
        return messageData;
    }
}
