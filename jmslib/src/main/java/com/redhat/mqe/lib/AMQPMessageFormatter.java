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

/**
 * Message output formatter to python dict,
 * map, or any other object printable format.
 * Reusable from old client
 */
public class AMQPMessageFormatter extends MessageFormatter {

  public void printMessageBodyAsText(Message message) {
    if (message instanceof TextMessage) {
      TextMessage textMessage = (TextMessage) message;
      try {
        LOG.info(textMessage.getText());
      } catch (JMSException e) {
        LOG.error("Unable to retrieve text from message.\n" + e.getMessage());
        e.printStackTrace();
        System.exit(1);
      }
    }
  }

  @SuppressWarnings("unchecked")
  public void printMessageAsDict(Message msg) {
    StringBuilder msgString = new StringBuilder();
    try {
      msgString.append("{");
      // AMQP Header
      msgString.append("'durable': ").append(formatBool(msg.getJMSDeliveryMode() == DeliveryMode.PERSISTENT));
      msgString.append(", 'priority': ").append(formatInt(msg.getJMSPriority()));
      msgString.append(", 'ttl': ").append(formatLong(Utils.getTtl(msg)));
      msgString.append(", 'first-acquirer': ").append(formatBool(msg.getBooleanProperty(AMQP_FIRST_ACQUIRER)));
      msgString.append(", 'delivery-count': ").append(formatInt(substractJMSDeliveryCount(msg.getIntProperty(JMSX_DELIVERY_COUNT))));
      // Delivery Annotations
      msgString.append(", 'redelivered': ").append(formatBool(msg.getJMSRedelivered()));
//      JMS2.0 functionality, doesn't work with old clients
//      msgString.append(", 'delivery-time': ").append(formatLong(msg.getJMSDeliveryTime()));
      // AMQP Properties
      msgString.append(", 'id': ").append(formatString(msg.getJMSMessageID()));
      msgString.append(", 'user_id': ").append(formatString(msg.getStringProperty(JMSX_USER_ID)));
      msgString.append(", 'address': ").append(formatAddress(msg.getJMSDestination()));
      msgString.append(", 'subject': ").append(formatObject(msg.getJMSType()));
      msgString.append(", 'reply_to': ").append(formatAddress(msg.getJMSReplyTo()));
      msgString.append(", 'correlation_id': ").append(formatString(msg.getJMSCorrelationID()));
      msgString.append(", 'content_type': ").append(formatString(msg.getStringProperty(AMQP_CONTENT_TYPE)));
      msgString.append(", 'content_encoding': ").append(formatString(msg.getStringProperty(AMQP_CONTENT_ENCODING)));
      msgString.append(", 'absolute-expiry-time': ").append(formatLong(msg.getJMSExpiration()));
      msgString.append(", 'creation-time': ").append(formatLong(msg.getJMSTimestamp()));
      msgString.append(", 'group-id': ").append(formatString(msg.getStringProperty(JMSX_GROUP_ID)));
      msgString.append(", 'group-sequence': ").append(getGroupSequenceNunmber(msg, AMQP_JMSX_GROUP_SEQ));
      msgString.append(", 'reply-to-group-id': ").append(formatString(msg.getStringProperty(AMQP_REPLY_TO_GROUP_ID)));
      // Application Properties
      msgString.append(", 'properties': ").append(formatProperties(msg));
      // Application Data
      msgString.append(", 'content': ").append(formatContent(msg)); //
      msgString.append("}");
    } catch (JMSException jmse) {
      LOG.error("Error while getting message properties!", jmse.getMessage());
      jmse.printStackTrace();
      System.exit(1);
    }
    LOG.info(msgString.toString());
  }

  @SuppressWarnings("unchecked")
  public void printMessageAsInterop(Message msg) {
    StringBuilder msgString = new StringBuilder();
    try {
      msgString.append("{");
      // AMQP Header
      msgString.append("'durable': ").append(formatBool(msg.getJMSDeliveryMode() == DeliveryMode.PERSISTENT));
      msgString.append(", 'priority': ").append(formatInt(msg.getJMSPriority()));
      msgString.append(", 'ttl': ").append(formatLong(Utils.getTtl(msg)));
      msgString.append(", 'first-acquirer': ").append(formatBool(msg.getBooleanProperty(AMQP_FIRST_ACQUIRER)));
      msgString.append(", 'delivery-count': ").append(formatInt(substractJMSDeliveryCount(msg.getIntProperty(JMSX_DELIVERY_COUNT))));
      // Delivery Annotations
      // JMS specifics
//      msgString.append(", 'redelivered': ").append(formatBool(msg.getJMSRedelivered()));
//      msgString.append(", 'delivery-time': ").append(formatLong(msg.getJMSDeliveryTime()));
      // AMQP Properties
      msgString.append(", 'id': ").append(formatString(removeIDprefix(msg.getJMSMessageID())));
      msgString.append(", 'user-id': ").append(formatString(msg.getStringProperty(JMSX_USER_ID)));
      msgString.append(", 'address': ").append(formatAddress(msg.getJMSDestination()));
      msgString.append(", 'subject': ").append(formatObject(msg.getJMSType()));
      msgString.append(", 'reply-to': ").append(formatAddress(msg.getJMSReplyTo()));
      msgString.append(", 'correlation-id': ").append(formatString(removeIDprefix(msg.getJMSCorrelationID())));
      msgString.append(", 'content-type': ").append(formatString(msg.getStringProperty(AMQP_CONTENT_TYPE)));
      msgString.append(", 'content-encoding': ").append(formatString(msg.getStringProperty(AMQP_CONTENT_ENCODING)));
      msgString.append(", 'absolute-expiry-time': ").append(formatLong(msg.getJMSExpiration()));
      msgString.append(", 'creation-time': ").append(formatLong(msg.getJMSTimestamp()));
      msgString.append(", 'group-id': ").append(formatString(msg.getStringProperty(JMSX_GROUP_ID)));
      msgString.append(", 'group-sequence': ").append(getGroupSequenceNunmber(msg, AMQP_JMSX_GROUP_SEQ));
      msgString.append(", 'reply-to-group-id': ").append(formatString(msg.getStringProperty(AMQP_REPLY_TO_GROUP_ID)));
      // Application Properties
      msgString.append(", 'properties': ").append(formatProperties(msg));
      // Application Data
      msgString.append(", 'content': ").append(formatContent(msg)); //
      msgString.append("}");
    } catch (JMSException jmse) {
      LOG.error("Error while getting message properties!", jmse.getMessage());
      jmse.printStackTrace();
      System.exit(1);
    }
    LOG.info(msgString.toString());
  }

}
