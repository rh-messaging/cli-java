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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.*;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * JmsMessageFormatter abstraction layer for all protocols.
 * <p>
 * Subclasses of JmsMessageFormatter produce data structures containing message data. Use the Formatter classes to print as Python objects,
 * JSON, and so on.
 */
public abstract class JmsMessageFormatter extends MessageFormatter {
    static Logger LOG = LoggerFactory.getLogger(JmsMessageFormatter.class);

    String AMQP_CONTENT_TYPE = "JMS_AMQP_ContentType";
    String OWIRE_AMQP_FIRST_ACQUIRER = "JMS_AMQP_FirstAcquirer";
    String OWIRE_AMQP_SUBJECT = "JMS_AMQP_Subject";
    String OWIRE_AMQP_CONTENT_ENCODING = "JMS_AMQP_ContentEncoding";
    String OWIRE_AMQP_REPLY_TO_GROUP_ID = "JMS_AMQP_ReplyToGroupID";
    String AMQP_JMSX_GROUP_SEQ = "JMSXGroupSeq";
    String JMSX_DELIVERY_COUNT = "JMSXDeliveryCount";
    String JMSX_USER_ID = "JMSXUserID";
    String JMSX_GROUP_ID = "JMSXGroupID";

    /**
     * Print message body as text.
     */
    public Map<String, Object> formatMessageBody(Message message, boolean hashContent) {
        String content = null;
        if (message instanceof TextMessage) {
            TextMessage textMessage = (TextMessage) message;
            try {
                content = textMessage.getText();
            } catch (JMSException e) {
                LOG.error("Unable to retrieve text from message.\n" + e.getMessage());
                e.printStackTrace();
                System.exit(1);
            }
        }

        Map<String, Object> messageData = new HashMap<>();
        messageData.put("content", hashContent ? hash(content) : content);
        return messageData;
    }

    /**
     * Returns a Map that is a common foundation for Dict and Interop outputs
     */
    public abstract Map<String, Object> formatMessage(Message msg, boolean hashContent) throws JMSException;

    public void addFormatInterop(Message msg, Map<String, Object> result) throws JMSException {
        result.put("delivery-count", subtractJMSDeliveryCount(msg.getIntProperty(JMSX_DELIVERY_COUNT)));
        result.put("first-acquirer", msg.getBooleanProperty(OWIRE_AMQP_FIRST_ACQUIRER));
    }

    public void addFormatJMS11(Message msg, Map<String, Object> result, boolean hashContent) throws JMSException {
        // Header
        result.put("durable", msg.getJMSDeliveryMode() == DeliveryMode.PERSISTENT);
        result.put("priority", msg.getJMSPriority());
        result.put("ttl", Utils.getTtl(msg));

        // Delivery Annotations

        // Properties
        result.put("id", removeIDprefix(msg.getJMSMessageID()));
        result.put("user-id", msg.getStringProperty(JMSX_USER_ID));
        result.put("address", formatAddress(msg.getJMSDestination()));
        result.put("subject", msg.getObjectProperty(OWIRE_AMQP_SUBJECT));
        result.put("reply-to", formatAddress(msg.getJMSReplyTo()));
        result.put("correlation-id", removeIDprefix(msg.getJMSCorrelationID()));
        result.put("content-type", msg.getStringProperty(AMQP_CONTENT_TYPE));
        result.put("content-encoding", msg.getStringProperty(OWIRE_AMQP_CONTENT_ENCODING));
        result.put("absolute-expiry-time", msg.getJMSExpiration());
        result.put("creation-time", msg.getJMSTimestamp());
        result.put("group-id", msg.getStringProperty(JMSX_GROUP_ID));
        result.put("group-sequence", getGroupSequenceNumber(msg, AMQP_JMSX_GROUP_SEQ));
        result.put("reply-to-group-id", msg.getStringProperty(OWIRE_AMQP_REPLY_TO_GROUP_ID));

        // Application Properties
        result.put("properties", formatProperties(msg));

        // Application Data
        result.put("content", hashContent ? hash(formatContent(msg)) : formatContent(msg));
        result.put("type", msg.getJMSType()); // not everywhere, amqp does not have it
    }

    private Object hash(Object o) {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            throw new JmsMessagingException("Unable to hash message", e);
        }
        String content = o.toString();
        return new BigInteger(1, md.digest(content.getBytes())).toString(16);
    }

    public void addFormatJMS20(Message msg, Map<String, Object> result) throws JMSException {
        // Delivery Annotations
        result.put("redelivered", msg.getJMSRedelivered());
        result.put("delivery-time", msg.getJMSDeliveryTime());
    }

    /**
     * Print message with as many as possible known client properties.
     */
    public Map<String, Object> formatMessageAsDict(Message msg, boolean hashContent) throws JMSException {
        Map<String, Object> result = formatMessage(msg, hashContent);
        result.put("redelivered", msg.getJMSRedelivered());
        return result;
    }

    /**
     * Print message in interoperable way for comparing with other clients.
     */
    public Map<String, Object> formatMessageAsInterop(Message msg, boolean hashContent) throws JMSException {
        Map<String, Object> result = formatMessage(msg, hashContent);
        addFormatInterop(msg, result);
        result.put("id", removeIDprefix((String) result.get("id")));
        result.put("user-id", removeIDprefix((String) result.get("user-id")));
        result.put("correlation-id", removeIDprefix((String) result.get("correlation-id")));
        result.put("group-sequence", changeMinusOneToZero((long) result.get("group-sequence")));
        return result;
    }

    protected long getGroupSequenceNumber(Message message, String propertyName) {
        try {
            if (message.getStringProperty(propertyName) == null) {
                return 0;
            } else {
                return message.getLongProperty(propertyName);  // it is UnsignedInteger in qpid-jms; ActiveMQ uses -1
            }
        } catch (JMSException e) {
            e.printStackTrace();
        }
        return 0;
    }

    protected String dropDestinationPrefix(String destination) {
        if (destination == null) {
            return null;
        }
        if (destination.startsWith(ClientOptionManager.TOPIC_PREFIX)
            || destination.startsWith(ClientOptionManager.QUEUE_PREFIX)) {
            return destination.substring(ClientOptionManager.TOPIC_PREFIX.length());
        }
        return destination;
    }

    /**
     * Method removes "ID:" prefix from various JMS message IDs.
     * This should be used only with interoperability formatting.
     */
    protected String removeIDprefix(String in_data) {
        if (in_data != null) {
            if (in_data.startsWith("ID:")) {
                in_data = in_data.substring(3);
            }
        }
        return in_data;
    }

    protected long changeMinusOneToZero(long n) {
        if (n == -1) {
            return 0;
        }
        return n;
    }

    protected String formatAddress(Destination destination) {
        if (destination == null) {
            return null;
        }

        final String address = destination.toString();
        return dropDestinationPrefix(address);
    }

    @SuppressWarnings("unchecked")
    protected Map<String, Object> formatProperties(Message msg) {
        Map<String, Object> format = new HashMap<>();
        try {
            Enumeration<String> props = msg.getPropertyNames();
            String pVal;

            while (props.hasMoreElements()) {
                pVal = props.nextElement();
                format.put(pVal, msg.getObjectProperty(pVal));
            }
        } catch (JMSException jmse) {
            LOG.error("Error while getting message properties!", jmse.getMessage());
            jmse.printStackTrace();
            System.exit(1);
        }
        return format;
    }

    @SuppressWarnings("unchecked")
    protected Map<String, Object> extractMap(MapMessage msg) {
        Map<String, Object> map = new HashMap<String, Object>();
        try {
            Enumeration<String> e = msg.getMapNames();
            while (e.hasMoreElements()) {
                String name = e.nextElement();
                Object obj;
                try {
                    if (msg instanceof BytesMessage) {
                        obj = new String(msg.getBytes(name));
                    } else {
                        obj = msg.getObject(name);
                    }
                } catch (MessageFormatException exc) {
                    obj = msg.getObject(name);
                }
                map.put(name, obj);
            }
        } catch (JMSException jmse) {
            LOG.error("Error while extracting MapMessage!");
            jmse.printStackTrace();
            System.exit(1);
        }
        return map;
    }

    @SuppressWarnings("unchecked")
    protected Object formatContent(Message msg) {
        try {
            if (msg instanceof TextMessage) {
                return ((TextMessage) msg).getText();
            } else if (msg instanceof MapMessage) {
                return extractMap((MapMessage) msg);
            } else if (msg instanceof ObjectMessage) {
                return ((ObjectMessage) msg).getObject();
            } else if (msg instanceof BytesMessage) {
                BytesMessage bmsg = (BytesMessage) msg;
                if (bmsg.getBodyLength() > 0) {
                    byte[] readBytes = new byte[(int) bmsg.getBodyLength()];
                    bmsg.readBytes(readBytes);
                    return readBytes.toString();
//          return new StringBuilder(bmsg.readUTF());
//          return new StringBuilder(new String(readBytes));
                }
            } else if (msg instanceof StreamMessage) {
                StreamMessage streamMessage = (StreamMessage) msg;
                List<Object> list = new ArrayList<>();
                while (true) {
                    try {
                        Object o = streamMessage.readObject();
                        list.add(o);
                    } catch (MessageEOFException e) {
                        return list;
                    }
                }
            }
        } catch (JMSException ex) {
            LOG.error("Error while printing content from message");
            ex.printStackTrace();
            System.exit(1);
        }
        return null;
    }
}
