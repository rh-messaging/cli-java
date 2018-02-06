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

import javax.jms.JMSException;
import javax.jms.Session;
import javax.jms.StreamMessage;
import java.util.List;

public class StreamMessageProvider extends MessageProvider {
    private final ClientOptions senderOptions;
    private final Session session;

    public StreamMessageProvider(ClientOptions senderOptions, Session session) {
        super(senderOptions, session);
        this.senderOptions = senderOptions;
        this.session = session;
    }

    @Override
    public StreamMessage provideMessage(long msgCounter) throws JMSException {
        return (StreamMessage) createMessage();
    }

    @Override
    StreamMessage createTypedMessage() throws JMSException {
        LOG.trace("set MSG_CONTENT_LIST_ITEM");

        // Create "ListMessage" using StreamMessage
        StreamMessage message = session.createStreamMessage();

        List<String> values = senderOptions.getOption(ClientOptions.MSG_CONTENT_LIST_ITEM).getParsedValuesList();
        if (isEmptyMessage(values)) {
            return message;
        }
        for (String parsedItem : values) {
            Content content = new Content(globalContentType(), parsedItem, false);
            message.writeObject(content.getValue());
        }
        return message;
    }
}
