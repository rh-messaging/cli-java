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

package com.redhat.mqe.lib.message;

import com.redhat.mqe.lib.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.*;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.InvalidParameterException;
import java.util.List;

public class MessageProvider implements AutoCloseable {
    static Logger LOG = LoggerFactory.getLogger(MessageProvider.class);
    private final ClientOptions senderOptions;
    private final Session session;

    public MessageProvider(ClientOptions senderOptions, Session session) {
        this.senderOptions = senderOptions;
        this.session = session;
    }

    /**
     * Read binary content from file
     *
     * @param binaryFileName binary file to be read from
     * @return read Byte array from file
     */
    protected static byte[] readBinaryContentFromFile(String binaryFileName) {
        File binaryFile = new File(binaryFileName);
        byte[] data = new byte[0];
        if (binaryFile.canRead()) {
            try {
                data = Files.readAllBytes(binaryFile.toPath());
            } catch (IOException e) {
                e.printStackTrace();
                System.exit(2);
            }
        } else {
            LOG.error("Unable to access file " + binaryFileName);
            System.exit(2);
        }
        LOG.debug("ToSend=" + data.length);
        return data;
    }

    public Message provideMessage(long msgCounter) throws JMSException {
        throw new UnsupportedOperationException("Subclass responsibility");
    }

    Message createTypedMessage() throws JMSException {
        throw new UnsupportedOperationException("Subclass responsibility");
    }

    Message createMessage() throws JMSException {
        Message message = createTypedMessage();
        setMessageProperties(message);
        setCustomMessageProperties(message);
        return message;
    }

    /**
     * Method creates message provider based on provided content on given session.
     *
     * @return newly created and set up message to be sent
     */
    public MessageProvider newInstance() {
        MessageProvider provider;
        if (hasParsedValue(ClientOptions.MSG_CONTENT_BINARY)) {
            provider = new BytesMessageProvider(senderOptions, session);
        } else if (hasParsedValue(ClientOptions.MSG_CONTENT, ClientOptions.MSG_CONTENT_FROM_FILE)) {
            final String contentType = senderOptions.getOption(ClientOptions.CONTENT_TYPE).getValue().toLowerCase();
            switch (contentType) {
                case "string":
                    provider = new TextMessageProvider(senderOptions, session);
                    break;
                case "object":
                case "int":
                case "integer":
                case "long":
                case "float":
                case "double":
                case "bool":
                    provider = new ObjectMessageProvider(senderOptions, session);
                    break;
                default:
                    throw new InvalidParameterException("Message content type of " + contentType + " is not supported.");
            }
        } else if (hasParsedValue(ClientOptions.MSG_CONTENT_LIST_ITEM)) {
            provider = new StreamMessageProvider(senderOptions, session);
        } else if (hasParsedValue(ClientOptions.MSG_CONTENT_MAP_ITEM)) {
            provider = new MapMessageProvider(senderOptions, session);
        } else {
            LOG.trace("Unknown type of message should be created! Sending empty Message");
            provider = new EmptyMessageProvider(senderOptions, session);
        }
        return provider;
    }

    /**
     * Checks if each option hasParsedValue and ||s the results together
     */
    private boolean hasParsedValue(String... options) {
        boolean result = false;
        for (String option : options) {
            result |= senderOptions.getOption(option).hasParsedValue();
        }
        return result;
    }

    String globalContentType() {
        if (senderOptions.getOption(ClientOptions.CONTENT_TYPE).hasParsedValue()) {
            return senderOptions.getOption(ClientOptions.CONTENT_TYPE).getValue().toLowerCase();
        }
        return null;
    }

    /**
     * Set various JMS (header) properties for this message.
     *
     * @param message to set properties for
     * @return same message with updated properties
     */
    Message setMessageProperties(Message message) {
        try {
            // Set message ID if provided or use default one
            if (senderOptions.getOption(ClientOptions.MSG_ID).hasParsedValue()) {
                final String id = senderOptions.getOption(ClientOptions.MSG_ID).getValue();
                // producer.setDisableMessageID(true) is called elsewhere
                if (!id.equals("noid")) {
                    message.setJMSMessageID(id);
                }
            }
            // Set message Correlation ID
            if (senderOptions.getOption(ClientOptions.MSG_CORRELATION_ID).hasParsedValue()) {
                message.setJMSCorrelationID(senderOptions.getOption(ClientOptions.MSG_CORRELATION_ID).getValue());
            }
            // Set message User ID
            if (senderOptions.getOption(ClientOptions.MSG_USER_ID).hasParsedValue()) {
                message.setStringProperty("JMSXUserID", senderOptions.getOption(ClientOptions.MSG_USER_ID).getValue());
            }
            // Set message Subject
            if (senderOptions.getOption(ClientOptions.MSG_SUBJECT).hasParsedValue()) {
                // FIXME? message.setJMSType(senderOptions.getOption(ClientOptions.MSG_SUBJECT).getValue());
                message.setStringProperty("JMS_AMQP_Subject", senderOptions.getOption(ClientOptions.MSG_SUBJECT).getValue());
            }
            // Set message reply to destination
            if (senderOptions.getOption(ClientOptions.MSG_REPLY_TO).hasParsedValue()) {
                String name = senderOptions.getOption(ClientOptions.MSG_REPLY_TO).getValue();
                Destination destination;
                if (name.startsWith(ClientOptionManager.QUEUE_PREFIX)) {
                    destination = session.createQueue(name.substring(ClientOptionManager.QUEUE_PREFIX.length()));
                } else if (name.startsWith(ClientOptionManager.TOPIC_PREFIX)) {
                    destination = session.createTopic(name.substring(ClientOptionManager.TOPIC_PREFIX.length()));
                } else {
                    destination = session.createQueue(name);
                }
                message.setJMSReplyTo(destination);
            }

            // Set message type to message content type (some JMS vendors use this internally)
            if (senderOptions.getOption(ClientOptions.MSG_CONTENT_TYPE).hasParsedValue()) {
                message.setStringProperty("JMS_AMQP_ContentType", senderOptions.getOption(ClientOptions.MSG_CONTENT_TYPE).getValue());
            }
            // Set message priority (4 by default)
            if (senderOptions.getOption(ClientOptions.MSG_PRIORITY).hasParsedValue()) {
                message.setJMSPriority(Integer.parseInt(senderOptions.getOption(ClientOptions.MSG_PRIORITY).getValue()));
            }

            // Set the group the message belongs to
            if (senderOptions.getOption(ClientOptions.MSG_GROUP_ID).hasParsedValue()) {
                message.setStringProperty("JMSXGroupID", senderOptions.getOption(ClientOptions.MSG_GROUP_ID).getValue());
            }
            // Set relative position of this message within its group
            if (senderOptions.getOption(ClientOptions.MSG_GROUP_SEQ).hasParsedValue()) {
                message.setIntProperty("JMSXGroupSeq", Integer.parseInt(senderOptions.getOption(ClientOptions.MSG_GROUP_SEQ).getValue()));
            }

            // JMS AMQP specific reply-to-group-id mapping
            if (senderOptions.getOption(ClientOptions.MSG_REPLY_TO_GROUP_ID).hasParsedValue()) {
                message.setStringProperty("JMS_AMQP_ReplyToGroupID", senderOptions.getOption(ClientOptions.MSG_REPLY_TO_GROUP_ID).getValue());
            }

        } catch (JMSException e) {
            e.printStackTrace();
        }
        return message;
    }

    /**
     * Set custom property values in the JMS header/body.
     * Setting is done using reflection on Message object and invoking
     * appropriate setXProperty(String, <primitive-type>).
     *
     * @param message message to have properties updated
     */
    void setCustomMessageProperties(Message message) {
        String globalPropertyType = null;
        if (senderOptions.getOption(ClientOptions.PROPERTY_TYPE).hasParsedValue()) {
            globalPropertyType = senderOptions.getOption(ClientOptions.PROPERTY_TYPE).getValue();
        }
        if (senderOptions.getOption(ClientOptions.MSG_PROPERTY).hasParsedValue()) {
            List<String> customProperties = senderOptions.getOption(ClientOptions.MSG_PROPERTY).getParsedValuesList();
            for (String property : customProperties) {
                // Create new 'content' object for property key=value mapping. It is same as Message Content Map, so we can safely reuse
                Content propertyContent = new Content(globalPropertyType, property, true);
                try {
                    String simpleName = propertyContent.getType().getSimpleName();
                    if (simpleName.equals("Integer")) {
                        simpleName = "Int";
                    }
                    // Call the appropriate setXProperty(String, <primitive-type>) - complicated with primitive types..
                    LOG.trace("calling method \"set" + simpleName + "Property()\" for " + property);
                    Method messageSetXPropertyMethod = Message.class.getMethod("set" + simpleName + "Property",
                        String.class, Utils.getPrimitiveClass(propertyContent.getType()));
                    messageSetXPropertyMethod.invoke(message, propertyContent.getKey(), propertyContent.getValue());
                } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
                    LOG.error("Unable to set message property from provided input. Exiting.");
                    e.printStackTrace();
                    System.exit(2);
                }
            }
        }
    }

    /**
     * Create message Content based on provided input.
     * This method does not care about types. Creation of object Content
     * takes care of autotype-casting and setting the proper values.
     *
     * @return message content, of some type
     */
    Object messageContent() {
        if (senderOptions.getOption(ClientOptions.MSG_CONTENT).hasParsedValue()) {
            LOG.trace("set MSG_CONTENT");
            return senderOptions.getOption(ClientOptions.MSG_CONTENT).getValue();
        } else if (senderOptions.getOption(ClientOptions.MSG_CONTENT_FROM_FILE).hasParsedValue()) {
            LOG.trace("set MSG_CONTENT_FROM_FILE");
            final String fileName = senderOptions.getOption(ClientOptions.MSG_CONTENT_FROM_FILE).getValue();

            if (Boolean.parseBoolean(senderOptions.getOption(ClientOptions.MSG_CONTENT_STREAM).getValue())) {
                File binaryFile = new File(fileName);
                try {
                    return new BufferedInputStream(new FileInputStream(binaryFile));
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    System.exit(3);
                }
            }

            byte[] bytes = readBinaryContentFromFile(fileName);
            if (Boolean.parseBoolean(senderOptions.getOption(ClientOptions.MSG_CONTENT_BINARY).getValue())) {
                return bytes;
            } else {
                return new String(bytes, StandardCharsets.UTF_8);
            }
        }
        throw new AssertionError("This should never be thrown");
    }

    /**
     * Returns whether the list of Strings is empty.
     *
     * @param values list to be checked for emptiness
     * @return true if the list is empty, false otherwise
     */
    static boolean isEmptyMessage(List<String> values) {
        return (values.size() == 1
            && (values.get(0).equals("") || values.get(0).equals("\"\"") || values.get(0).equals("\'\'")));
    }

    @Override
    public void close() throws Exception {
    }
}
