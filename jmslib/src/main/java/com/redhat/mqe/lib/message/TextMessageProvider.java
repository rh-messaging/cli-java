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

import javax.jms.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TextMessageProvider extends MessageProvider {
    private final Session session;

    private TextMessage message;
    private boolean userMessageCounter = false;
    private String userMessageCounterText;

    public TextMessageProvider(ClientOptions senderOptions, Session session) {
        super(senderOptions, session);
        this.session = session;
    }

    @Override
    public TextMessage provideMessage(long msgCounter) throws JMSException {
        if (message == null) {
            message = (TextMessage) createMessage();
        }
        if (userMessageCounter) {
            ((TextMessage) message).setText(String.format(userMessageCounterText, msgCounter));
        }
        return message;
    }

    TextMessage createTypedMessage() throws JMSException {
        LOG.trace("set MSG_CONTENT");
        final Content content = new Content(globalContentType(), (String) messageContent(), false);

        TextMessage textMessage = session.createTextMessage();
        String textContent = content.getValue().toString();
        String pattern = "%[ 0-9]*d";
        Pattern r = Pattern.compile(pattern);
        Matcher matcher = r.matcher(textContent);

        if (matcher.find()) {
            userMessageCounter = true;
            userMessageCounterText = textContent;
        }
        textMessage.setText(textContent);
        return textMessage;
    }
}
