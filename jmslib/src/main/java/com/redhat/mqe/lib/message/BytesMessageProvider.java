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

import javax.jms.BytesMessage;
import javax.jms.JMSException;
import javax.jms.Session;

public class BytesMessageProvider extends MessageProvider {
    private final Session session;

    public BytesMessageProvider(ClientOptions senderOptions, Session session) {
        super(senderOptions, session);
        this.session = session;
    }

    @Override
    public BytesMessage provideMessage(long msgCounter) throws JMSException {
        return (BytesMessage) createMessage();
    }

    public BytesMessage createTypedMessage() throws JMSException {
        BytesMessage bytesMessage = session.createBytesMessage();

        LOG.debug("Filling ByteMessage with binary data");
        byte[] bytes = (byte[]) messageContent();
        bytesMessage.writeBytes(bytes);

        return bytesMessage;
    }

}
