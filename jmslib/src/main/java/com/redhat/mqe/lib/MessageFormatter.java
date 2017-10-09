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
 */
public abstract class MessageFormatter {

    String AMQP_FIRST_ACQUIRER = "JMS_AMQP_FIRST_ACQUIRER";
    String AMQP_CONTENT_TYPE = "JMS_AMQP_CONTENT_TYPE";
    String AMQP_CONTENT_ENCODING = "JMS_AMQP_CONTENT_ENCODING";
    String AMQP_JMSX_GROUP_SEQ = "JMSXGroupSeq";
    String AMQP_REPLY_TO_GROUP_ID = "JMS_AMQP_REPLY_TO_GROUP_ID";

    String OWIRE_AMQP_FIRST_ACQUIRER = "JMS_AMQP_FirstAcquirer";
    String OWIRE_AMQP_SUBJECT = "JMS_AMQP_Subject";
    String OWIRE_AMQP_CONTENT_TYPE = "JMS_AMQP_ContentType";
    String OWIRE_AMQP_CONTENT_ENCODING = "JMS_AMQP_ContentEncoding";
    String OWIRE_AMQP_REPLY_TO_GROUP_ID = "JMS_AMQP_ReplyToGroupID";
    String OWIRE_GROUP_SEQ = "JMSXGroupSequence";


    String JMSX_DELIVERY_COUNT = "JMSXDeliveryCount";
    String JMSX_USER_ID = "JMSXUserID";
    String JMSX_GROUP_ID = "JMSXGroupID";

    static Logger LOG = LoggerFactory.getLogger(MessageFormatter.class);

    /**
     * Print message body as text.
     *
     * @param message message to print
     */
    public abstract void printMessageBodyAsText(Message message);

    /**
     * Print message with as many as possible known client properties.
     *
     * @param msg message to print
     */
    public abstract void printMessageAsDict(Message msg);

    /**
     * Print message in interoperable way for comparing with other clients.
     *
     * @param msg message to print
     */
    public abstract void printMessageAsInterop(Message msg);

    protected String getGroupSequenceNunmber(Message message, String propertyName) {
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

    protected StringBuilder formatAddress(Destination in_data) {
        if (in_data == null) {
            return new StringBuilder("None");
        }
        String address = dropDestinationPrefix(in_data.toString());
        if (address == null) {
            return new StringBuilder("None");
        }
        return formatString(address);
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
    protected StringBuilder formatProperties(Message msg) {
        StringBuilder int_res = new StringBuilder();
        try {
            Enumeration<String> props = msg.getPropertyNames();
            String pVal;
            int_res.append('{');

            while (props.hasMoreElements()) {
                pVal = props.nextElement();
                int_res.append("'").append(pVal).append("': ").append(formatObject(msg.getObjectProperty(pVal)));
                if (props.hasMoreElements()) {
                    int_res.append(", ");
                }
            }
        } catch (JMSException jmse) {
            LOG.error("Error while getting message properties!", jmse.getMessage());
            jmse.printStackTrace();
            System.exit(1);
        }
        int_res.append('}');
        return int_res;
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
    protected StringBuilder formatContent(Message msg) {
        StringBuilder int_res = new StringBuilder();
        try {
            if (msg instanceof TextMessage) {
                int_res.append(formatString(((TextMessage) msg).getText()));
                // TODO remove dependency on qpid.ListMessage
//    } else if (msg instanceof ListMessage) {
//      return formatList(((ListMessage) msg).asList());
            } else if (msg instanceof MapMessage) {
                return formatMap(extractMap((MapMessage) msg));
            } else if (msg instanceof ObjectMessage) {
                Object obj = ((ObjectMessage) msg).getObject();
                if (obj instanceof List) {
                    return formatList((List) obj);
                } else if (obj instanceof Map) {
                    // TODO this might not be necessary as Object should not have MapMessage
                    return formatMap((Map) obj);
                } else {
                    return formatObject(obj);
                }
            } else if (msg instanceof BytesMessage) {
                BytesMessage bmsg = (BytesMessage) msg;
                if (bmsg.getBodyLength() > 0) {
                    byte[] readBytes = new byte[(int) bmsg.getBodyLength()];
                    bmsg.readBytes(readBytes);
                    return new StringBuilder(readBytes.toString());
//          return new StringBuilder(bmsg.readUTF());
//          return new StringBuilder(new String(readBytes));
                } else {
                    return new StringBuilder("None");
                }
            } else if (msg instanceof StreamMessage) {
                StreamMessage streamMessage = (StreamMessage) msg;
                List<Object> list = new ArrayList<>();
                while (true) {
                    try {
                        Object o = streamMessage.readObject();
                        list.add(o);
                    } catch (MessageEOFException e) {
                        return formatList(list);
                    }
                }
            } else if (msg instanceof Message) {
                return new StringBuilder("None");
            } else {
                return new StringBuilder("UnknownMsgType");
            }
        } catch (JMSException ex) {
            LOG.error("Error while printing content from message");
            ex.printStackTrace();
            System.exit(1);
        }
        return int_res;
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

}
