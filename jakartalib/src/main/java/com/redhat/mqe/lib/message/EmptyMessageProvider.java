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

import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.Session;

public class EmptyMessageProvider extends MessageProvider {
    private final Session session;

    public EmptyMessageProvider(ClientOptions senderOptions, Session session) {
        super(senderOptions, session);
        this.session = session;
    }

    @Override
    public Message provideMessage(long msgCounter) throws JMSException {
        return createMessage();
    }

    @Override
    public Message createTypedMessage() throws JMSException {
        return session.createMessage();
    }
}
