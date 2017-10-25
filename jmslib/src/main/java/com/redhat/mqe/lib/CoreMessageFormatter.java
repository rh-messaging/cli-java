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
public class CoreMessageFormatter extends MessageFormatter {
    /**
     * Openwire -> AMQP mapping http://activemq.apache.org/amqp.html
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> formatMessageAsDict(Message msg) {
        Map<String, Object> format = new HashMap<>();
        try {
            // Header
            format.put("durable", msg.getJMSDeliveryMode() == DeliveryMode.PERSISTENT);
            format.put("priority", msg.getJMSPriority());
            format.put("ttl", Utils.getTtl(msg));
            format.put("first-acquirer", msg.getBooleanProperty(OWIRE_AMQP_FIRST_ACQUIRER));
            format.put("delivery-count", substractJMSDeliveryCount(msg.getIntProperty(JMSX_DELIVERY_COUNT)));

            // Delivery Annotations
            format.put("redelivered", msg.getJMSRedelivered());
//      format.put("delivery-time", msg.getJMSDeliveryTime());
            // Properties
            format.put("id", msg.getJMSMessageID());
            format.put("user_id", msg.getStringProperty(JMSX_GROUP_ID));
            format.put("address", formatAddress(msg.getJMSDestination()));
            format.put("subject", msg.getObjectProperty(OWIRE_AMQP_SUBJECT));
            format.put("reply_to", formatAddress(msg.getJMSReplyTo()));
            format.put("correlation_id", msg.getJMSCorrelationID());
            format.put("content_type", msg.getStringProperty(OWIRE_AMQP_CONTENT_TYPE));
            format.put("content_encoding", msg.getStringProperty(OWIRE_AMQP_CONTENT_ENCODING));
            format.put("absolute-expiry-time", msg.getJMSExpiration());
            format.put("creation-time", msg.getJMSTimestamp());
            format.put("group-id", msg.getStringProperty(JMSX_GROUP_ID));
            format.put("group-sequence", getGroupSequenceNumber(msg, OWIRE_GROUP_SEQ));
            format.put("reply-to-group-id", msg.getStringProperty(OWIRE_AMQP_REPLY_TO_GROUP_ID));
            // Application Properties
            format.put("properties", formatProperties(msg));
            // Application Data
            format.put("content", formatContent(msg));
            format.put("type", msg.getJMSType());
        } catch (JMSException jmse) {
            LOG.error("Error while getting message properties!", jmse.getMessage());
            jmse.printStackTrace();
            System.exit(1);
        }
        return format;
    }

    /**
     * Openwire -> AMQP mapping http://activemq.apache.org/amqp.html
     *
     * @param msg to be printed
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> formatMessageAsInterop(Message msg) {
        Map<String, Object> format = new HashMap<>();
        try {
            //
            // Header
            format.put("durable", msg.getJMSDeliveryMode() == DeliveryMode.PERSISTENT);
            format.put("priority", msg.getJMSPriority());
            format.put("ttl", Utils.getTtl(msg));
            format.put("first-acquirer", msg.getBooleanProperty(OWIRE_AMQP_FIRST_ACQUIRER));
            format.put("delivery-count", substractJMSDeliveryCount(msg.getIntProperty(JMSX_DELIVERY_COUNT)));

            // Delivery Annotations
            // JMS Specifics
//      format.put("redelivered", msg.getJMSRedelivered()));
//      format.put("delivery-time", msg.getJMSDeliveryTime()));
            // Properties
            format.put("id", removeIDprefix(msg.getJMSMessageID()));
            format.put("user-id", msg.getStringProperty(JMSX_GROUP_ID));
            format.put("address", formatAddress(msg.getJMSDestination()));
            format.put("subject", formatObject(msg.getObjectProperty(OWIRE_AMQP_SUBJECT)));
            format.put("reply-to", formatAddress(msg.getJMSReplyTo()));
            format.put("correlation-id", removeIDprefix(msg.getJMSCorrelationID()));
            format.put("content-type", msg.getStringProperty(OWIRE_AMQP_CONTENT_TYPE));
            format.put("content-encoding", msg.getStringProperty(OWIRE_AMQP_CONTENT_ENCODING));
            format.put("absolute-expiry-time", msg.getJMSExpiration());
            format.put("creation-time", msg.getJMSTimestamp());
            format.put("group-id", msg.getStringProperty(JMSX_GROUP_ID));
            format.put("group-sequence", getGroupSequenceNumber(msg, OWIRE_GROUP_SEQ));
            format.put("reply-to-group-id", msg.getStringProperty(OWIRE_AMQP_REPLY_TO_GROUP_ID));
            // Application Properties
            format.put("properties", formatProperties(msg));
            // Application Data
            format.put("content", formatContent(msg));
            format.put("type", msg.getJMSType());
        } catch (JMSException jmse) {
            LOG.error("Error while getting message properties!", jmse.getMessage());
            jmse.printStackTrace();
            System.exit(1);
        }
        return format;
    }
}
