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

package com.redhat.mqe.lib;

import javax.jms.DeliveryMode;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.TextMessage;
import java.util.HashMap;
import java.util.Map;

/**
 * Message output formatter to python dict,
 * map, or any other object printable format.
 * Reusable from old client
 */
public class AMQPMessageFormatter extends MessageFormatter {
    @SuppressWarnings("unchecked")
    public Map<String, Object> formatMessageAsDict(Message msg) {
        Map<String, Object> result = new HashMap<>();
        try {
            // AMQP Header
            result.put("durable", msg.getJMSDeliveryMode() == DeliveryMode.PERSISTENT);
            result.put("priority", msg.getJMSPriority());
            result.put("ttl", Utils.getTtl(msg));
            result.put("first-acquirer", msg.getBooleanProperty(AMQP_FIRST_ACQUIRER));
            result.put("delivery-count", substractJMSDeliveryCount(msg.getIntProperty(JMSX_DELIVERY_COUNT)));
            // Delivery Annotations
            result.put("redelivered", msg.getJMSRedelivered());
//      JMS2.0 functionality, doesn't work with old clients
//      result.put("delivery-time", msg.getJMSDeliveryTime());
            // AMQP Properties
            result.put("id", msg.getJMSMessageID());
            result.put("user_id", msg.getStringProperty(JMSX_USER_ID));
            result.put("address", formatAddress(msg.getJMSDestination()));
            result.put("subject", msg.getJMSType());
            result.put("reply_to", formatAddress(msg.getJMSReplyTo()));
            result.put("correlation_id", msg.getJMSCorrelationID());
            result.put("content_type", msg.getStringProperty(AMQP_CONTENT_TYPE));
            result.put("content_encoding", msg.getStringProperty(AMQP_CONTENT_ENCODING));
            result.put("absolute-expiry-time", msg.getJMSExpiration());
            result.put("creation-time", msg.getJMSTimestamp());
            result.put("group-id", msg.getStringProperty(JMSX_GROUP_ID));
            result.put("group-sequence", msg.getStringProperty(AMQP_JMSX_GROUP_SEQ));
            result.put("reply-to-group-id", msg.getStringProperty(AMQP_REPLY_TO_GROUP_ID));
            // Application Properties
            result.put("properties", formatProperties(msg));
            // Application Data
            result.put("content", formatContent(msg));
        } catch (JMSException jmse) {
            LOG.error("Error while getting message properties!", jmse.getMessage());
            jmse.printStackTrace();
            System.exit(1);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> formatMessageAsInterop(Message msg) {
        Map<String, Object> result = new HashMap<>();
        try {
            result.put("durable", msg.getJMSDeliveryMode() == DeliveryMode.PERSISTENT);
            result.put("priority", msg.getJMSPriority());
            result.put("ttl", Utils.getTtl(msg));
            result.put("first-acquirer", msg.getBooleanProperty(AMQP_FIRST_ACQUIRER));
            result.put("delivery-count", substractJMSDeliveryCount(msg.getIntProperty(JMSX_DELIVERY_COUNT)));
            // Delivery Annotations
            // JMS specifics
//      result.put("redelivered", msg.getJMSRedelivered());
//      result.put("delivery-time", msg.getJMSDeliveryTime());
            // AMQP Properties
            result.put("id", removeIDprefix(msg.getJMSMessageID()));
            result.put("user-id", msg.getStringProperty(JMSX_USER_ID));
            result.put("address", formatAddress(msg.getJMSDestination()));
            result.put("subject", msg.getJMSType());
            result.put("reply-to", formatAddress(msg.getJMSReplyTo()));
            result.put("correlation-id", removeIDprefix(msg.getJMSCorrelationID()));
            result.put("content-type", msg.getStringProperty(AMQP_CONTENT_TYPE));
            result.put("content-encoding", msg.getStringProperty(AMQP_CONTENT_ENCODING));
            result.put("absolute-expiry-time", msg.getJMSExpiration());
            result.put("creation-time", msg.getJMSTimestamp());
            result.put("group-id", msg.getStringProperty(JMSX_GROUP_ID));
            result.put("group-sequence", msg.getStringProperty(AMQP_JMSX_GROUP_SEQ));
            result.put("reply-to-group-id", msg.getStringProperty(AMQP_REPLY_TO_GROUP_ID));
            // Application Properties
            result.put("properties", formatProperties(msg));
            // Application Data
            result.put("content", formatContent(msg));
        } catch (JMSException jmse) {
            LOG.error("Error while getting message properties!", jmse.getMessage());
            jmse.printStackTrace();
            System.exit(1);
        }
        return result;
    }

}
