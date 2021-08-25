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

import com.redhat.mqe.lib.ClientOptions;
import com.redhat.mqe.lib.Content;
import com.redhat.mqe.lib.MessagingException;
import com.redhat.mqe.lib.Utils;

import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Session;
import java.util.ArrayList;
import java.util.List;

public class MapMessageProvider extends MessageProvider {
    private final ClientOptions senderOptions;
    private final Session session;

    public MapMessageProvider(ClientOptions senderOptions, Session session) {
        super(senderOptions, session);
        this.senderOptions = senderOptions;
        this.session = session;
    }

    @Override
    public MapMessage provideMessage(long msgCounter) throws JMSException {
        return (MapMessage) createMessage();
    }

    @Override
    public MapMessage createTypedMessage() throws JMSException {
        MapMessage mapMessage = session.createMapMessage();
        fillMapMessage(senderOptions, mapMessage);
        return mapMessage;
    }

    /**
     * Fill MapMessage with a data provided by user input as *content*.
     *
     * @param senderOptions sender options
     * @param mapMessage    message to be filled with data
     */
    private void fillMapMessage(ClientOptions senderOptions, MapMessage mapMessage) throws JMSException {
        LOG.trace("set MSG_CONTENT_MAP_ITEM");

        List<String> values = senderOptions.getOption(ClientOptions.MSG_CONTENT_MAP_ITEM).getParsedValuesList();
        if (isEmptyMessage(values)) {
            return;
        }
        List<Content> content = new ArrayList<>();
        for (String parsedItem : values) {
            content.add(new Content(globalContentType(), parsedItem, true));
        }

        for (Content c : content) {
            LOG.trace("Filling MapMessage with: " + c.getValue() + " class=" + c.getType().getName());
            if (Utils.CLASSES.contains(c.getType())) {
                switch (c.getType().getSimpleName()) {
                    case "Integer":
                        mapMessage.setInt(c.getKey(), (Integer) c.getValue());
                        break;
                    case "Long":
                        mapMessage.setLong(c.getKey(), (Long) c.getValue());
                        break;
                    case "Float":
                        mapMessage.setFloat(c.getKey(), (Float) c.getValue());
                        break;
                    case "Double":
                        mapMessage.setDouble(c.getKey(), (Double) c.getValue());
                        break;
                    case "Boolean":
                        mapMessage.setBoolean(c.getKey(), (Boolean) c.getValue());
                        break;
                    case "String":
                        mapMessage.setString(c.getKey(), (String) c.getValue());
                        break;
                    default:
                        LOG.error("Sending unknown type element!");
                        mapMessage.setObject(c.getKey(), c.getValue());
                        break;
                }
                mapMessage.setObject(c.getKey(), c.getValue());

            } else {
                throw new MessagingException("Unknown data type in message Content. Do not know how to send it. Type=" + c.getType());
            }
        }
    }
}
