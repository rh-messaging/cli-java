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

package com.redhat.mqe.lib;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * MessageFormatter abstraction layer for all protocols.
 * <p>
 * Subclasses of MessageFormatter produce data structures containing message data. Use the Formatter classes to print as Python objects,
 * JSON, and so on.
 */
public abstract class MessageFormatter {
    static Logger LOG = LoggerFactory.getLogger(MessageFormatter.class);
    private final ObjectMapper json = new ObjectMapper();

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
     * delivery-count in AMQP is 'failed delivery attempts'.
     * JMS the count is more like 'sequence of attempts'
     *
     * @param value JMSXDeliveryCount
     * @return JMSXDeliveryCount reduced by 1 (has to be 0+ number)
     */
    protected int subtractJMSDeliveryCount(int value) {
        if (value > 0) {
            value--;
        }
        return value;
    }

    protected StringBuilder formatNumber(Number number) {
        StringBuilder builder = new StringBuilder();
        if (number instanceof Byte || number instanceof Short || number instanceof Integer || number instanceof Long) {
            builder.append(formatLong(number.longValue()));
        } else if (number instanceof Float || number instanceof Double) {
            builder.append(formatDouble(number.doubleValue()));
        } else { // can be e.g. org.apache.qpid.proton.amqp;UnsignedInteger
            builder.append(number); // toString{} formats UnsignedIntegers just fine
        }
        return builder;
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
            handleUnsupportedObjectMessagePayloadType(int_res, in_data);
        }
        return int_res;
    }

    public void handleUnsupportedObjectMessagePayloadType(StringBuilder int_res, Object in_data) {
        LOG.error("Unsupported object type {} {}", in_data.getClass().getCanonicalName(), in_data);
        int_res.append(in_data.toString());
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

    /* ------ Support formatting functions ----- */
    protected StringBuilder quoteStringEscape(String a) {
        StringBuilder builder = new StringBuilder();
        for (char c : a.toCharArray()) {
            if (c == '\'') {
                builder.append("\\'");
            } else if (c == '\0') {
                builder.append("\\0");
            } else if (c == '\n') {
                builder.append("\\n");
            } else if (c == '\r') {
                builder.append("\\r");
            } else {
                builder.append(c);
            }
        }
        return builder;
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
        printMessage(msgString.toString());
    }

    public void printMessageAsJson(Map<String, Object> format) {
        try {
            printMessage(json.writeValueAsString(format));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public void printMessage(String message) {
        System.out.println(message);
    }

    public void printStatistics(Hashtable<String, Object> msg) {
        System.out.println("STATS " + formatMap(msg));
    }

    public void printConnectorStatistics(int connectionsOpened, int connectionsFailed, int connectionsTotal) {
        System.out.println(connectionsOpened + " " + connectionsFailed + " " + connectionsTotal);
    }

    public static String hash(Object o) {
        if (o == null) {
            return null; // no point in hashing this value
        }
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            throw new MessagingException("Unable to hash message", e);
        }
        String content = o.toString();
        return new BigInteger(1, md.digest(content.getBytes())).toString(16);
    }
}
