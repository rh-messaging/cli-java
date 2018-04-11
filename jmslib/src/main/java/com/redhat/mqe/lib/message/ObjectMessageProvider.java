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
import javax.jms.ObjectMessage;
import javax.jms.Session;
import java.io.Serializable;

public class ObjectMessageProvider extends MessageProvider {
    private final Session session;

    public ObjectMessageProvider(ClientOptions senderOptions, Session session) {
        super(senderOptions, session);
        this.session = session;
    }

    @Override
    public ObjectMessage provideMessage(long msgCounter) throws JMSException {
        return (ObjectMessage) createMessage();
    }

    @Override
    ObjectMessage createTypedMessage() throws JMSException {
        LOG.debug("Filling object data");
        final Content content = new Content(globalContentType(), (String) messageContent(), false);

        ObjectMessage objectMessage = session.createObjectMessage();
        objectMessage.setObject((Serializable) content.getValue());
        return objectMessage;
    }
}
