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
import java.io.UnsupportedEncodingException;
import java.util.*;

/**
 * MessageFormatter abstraction layer for all protocols.
 * <p>
 * Subclasses of MessageFormatter produce data structures containing message data. Use the Formatter classes to print as Python objects,
 * JSON, and so on.
 */
public abstract class MessageFormatter {
    String AMQP_CONTENT_TYPE = "JMS_AMQP_ContentType";
    String OWIRE_AMQP_FIRST_ACQUIRER = "JMS_AMQP_FirstAcquirer";
    String OWIRE_AMQP_SUBJECT = "JMS_AMQP_Subject";
    String OWIRE_AMQP_CONTENT_ENCODING = "JMS_AMQP_ContentEncoding";
    String OWIRE_AMQP_REPLY_TO_GROUP_ID = "JMS_AMQP_ReplyToGroupID";

    String AMQP_JMSX_GROUP_SEQ = "JMSXGroupSeq";
    String JMSX_DELIVERY_COUNT = "JMSXDeliveryCount";
    String JMSX_USER_ID = "JMSXUserID";
    String JMSX_GROUP_ID = "JMSXGroupID";

    static Logger LOG = LoggerFactory.getLogger(MessageFormatter.class);

    /**
     * Print message body as text.
     */
    public Map<String, Object> formatMessageBody(Message message) {
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
        messageData.put("content", content);
        return messageData;
    }

    /**
     * Returns a Map that is a common foundation for Dict and Interop outputs
     */
    public abstract Map<String, Object> formatMessage(Message msg) throws JMSException;

    public void addFormatInterop(Message msg, Map<String, Object> result) throws JMSException {
        result.put("delivery-count", substractJMSDeliveryCount(msg.getIntProperty(JMSX_DELIVERY_COUNT)));
        result.put("first-acquirer", msg.getBooleanProperty(OWIRE_AMQP_FIRST_ACQUIRER));
    }

    public void addFormatJMS11(Message msg, Map<String, Object> result) throws JMSException {
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
        result.put("content", formatContent(msg));
        result.put("type", msg.getJMSType()); // not everywhere, amqp does not have it
    }

    public void addFormatJMS20(Message msg, Map<String, Object> result) throws JMSException {
        // Delivery Annotations
        result.put("redelivered", msg.getJMSRedelivered());
        result.put("delivery-time", msg.getJMSDeliveryTime());
    }

    /**
     * Print message with as many as possible known client properties.
     */
    public Map<String, Object> formatMessageAsDict(Message msg) throws JMSException {
        Map<String, Object> result = formatMessage(msg);
        result.put("redelivered", msg.getJMSRedelivered());
        return result;
    }

    /**
     * Print message in interoperable way for comparing with other clients.
     */
    public Map<String, Object> formatMessageAsInterop(Message msg) throws JMSException {
        Map<String, Object> result = formatMessage(msg);
        addFormatInterop(msg, result);
        result.put("id", removeIDprefix((String) result.get("id")));
        result.put("user-id", removeIDprefix((String) result.get("user-id")));
        result.put("correlation-id", removeIDprefix((String) result.get("correlation-id")));
        return result;
    }

    protected String getGroupSequenceNumber(Message message, String propertyName) {
        try {
            if (message.getStringProperty(propertyName) == null) {
                return formatInt(0).toString();
            } else {
                return formatInt(message.getIntProperty(propertyName)).toString();
            }
        } catch (JMSException e) {
            e.printStackTrace();
        }
        return formatInt(0).toString();
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

    public void printStatistics(Hashtable<String, Object> msg) throws Exception {
        LOG.info("STATS {}", formatMap(msg));
    }

    protected StringBuilder formatBool(Boolean in_data) {
        StringBuilder int_res = new StringBuilder();
        if (in_data) {
            int_res.append("True");
        } else {
            int_res.append("False");
        }
        return int_res;
    }

    protected StringBuilder formatCharacter(Character in_data) {
        return formatString(in_data.toString());
    }

    protected StringBuilder formatString(String in_data) {
        StringBuilder int_res = new StringBuilder();
        if (in_data == null) {
            int_res.append("None");
        } else {
            int_res.append("'").append(quoteStringEscape(in_data)).append("'");
        }
        return int_res;
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

    /**
     * delivery-count in AMQP is 'failed delivery attempts'.
     * JMS the count is more like 'sequence of attempts'
     *
     * @param value JMSXDeliveryCount
     * @return JMSXDeliveryCount reduced by 1 (has to be 0+ number)
     */
    protected int substractJMSDeliveryCount(int value) {
        if (value > 0) {
            value--;
        }
        return value;
    }

    protected StringBuilder formatNumber(Number in_data) {
        StringBuilder int_res = new StringBuilder();
        if (in_data instanceof Byte || in_data instanceof Short || in_data instanceof Integer || in_data instanceof Long) {
            int_res.append(formatLong(in_data.longValue()));
        } else if (in_data instanceof Float || in_data instanceof Double) {
            int_res.append(formatDouble(in_data.doubleValue()));
        }
        return int_res;
    }

    protected StringBuilder formatInt(int in_data) {
        return formatLong((long) in_data);
    }

    protected StringBuilder formatLong(long in_data) {
        StringBuilder int_res = new StringBuilder();
        int_res.append(in_data);
        return int_res;
    }

    protected StringBuilder formatFloat(float in_data) {
        return formatDouble((double) in_data);
    }

    protected StringBuilder formatDouble(double in_data) {
        // NOTE We print everything as float because of unnecessary long double output
        // 3.14 -> 3.140000000908..
        StringBuilder int_res = new StringBuilder();
//    int_res.append(String.format("%f", in_data));
        int_res.append(((Double) in_data).floatValue());
        return int_res;
    }

    protected String formatAddress(Destination destination) {
        if (destination == null) {
            return null;
        }

        final String address = destination.toString();
        return dropDestinationPrefix(address);
    }

    @SuppressWarnings("unchecked")
    protected StringBuilder formatObject(Object in_data) {
        StringBuilder int_res = new StringBuilder();
        if (in_data == null) {
            int_res.append("None");
        } else if (in_data instanceof Boolean) {
            int_res.append(formatBool((Boolean) in_data));
        } else if (in_data instanceof Number) {
            int_res.append(formatNumber((Number) in_data));
        } else if (in_data instanceof Character) {
            int_res.append(formatCharacter((Character) in_data));
        } else if (in_data instanceof String) {
            int_res.append(formatString((String) in_data));
        } else if (in_data instanceof List) {
            List<Object> in_data_list = (List<Object>) in_data;
            int_res.append(formatList(in_data_list));
        } else if (in_data instanceof Map) {
            Map<String, Object> in_data_map = (Map<String, Object>) in_data;
            int_res.append(formatMap(in_data_map));
        } else if (in_data instanceof UUID) {
            int_res.append(formatString(in_data.toString()));
        } else if (in_data instanceof byte[]) {
            try {
                String value = new String((byte[]) in_data, "UTF-8");
                int_res.append(formatString(value));
            } catch (UnsupportedEncodingException uee) {
                LOG.error("Error while getting message properties!", uee.getMessage());
                uee.printStackTrace();
                System.exit(1);
            }
        } else {
            LOG.error("Unsupported object type {} {}", in_data.getClass().getCanonicalName(), in_data);
            int_res.append(in_data.toString());
        }
        return int_res;
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

    protected StringBuilder formatList(List<Object> objectsList) {
        StringBuilder listData = new StringBuilder();
        String delimiter = "";
        listData.append('[');
        for (Object o : objectsList) {
            listData.append(delimiter).append(formatObject(o));
            delimiter = ", ";
        }
        listData.append(']');
        return listData;
    }


    protected StringBuilder formatMap(Map<String, Object> map) {
        StringBuilder mapData = new StringBuilder();
        mapData.append('{');
        String delimiter = "";
        for (String key : map.keySet()) {
            mapData.append(delimiter).append(formatString(key)).append(": ").append(formatObject(map.get(key)));
            delimiter = ", ";
        }
        mapData.append('}');
        return mapData;

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


    /* ------ Support formatting functions ----- */
    protected StringBuilder quoteStringEscape(String a) {
        final char pattern = '\'';
        StringBuilder int_result = new StringBuilder();
        for (char c : a.toCharArray()) {
            if (c == pattern) {
                int_result.append('\\');
            }
            int_result.append(c);
        }
        return int_result;
    }

    public void printMessageAsPython(Map<String, Object> format) {
        StringBuilder msgString = new StringBuilder();
        msgString.append("{");

        boolean first = true;
        for (Map.Entry<String, Object> entry : format.entrySet()) {
            if (!first) {
                msgString.append(", ");
            } else {
                first = false;
            }
            msgString.append("'");
            msgString.append(entry.getKey());
            msgString.append("': ");
            msgString.append(formatObject(entry.getValue()));
        }
        msgString.append("}");
        LOG.info(msgString.toString());
    }
}
