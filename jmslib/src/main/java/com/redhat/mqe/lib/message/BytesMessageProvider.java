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
import java.io.Closeable;
import java.io.InputStream;

public class BytesMessageProvider extends MessageProvider implements AutoCloseable {
    private final Session session;
    /**
     * most recent message's content, for closing streamed message content
     */
    private Object content;

    public BytesMessageProvider(ClientOptions senderOptions, Session session) {
        super(senderOptions, session);
        this.session = session;
    }

    @Override
    public BytesMessage provideMessage(long msgCounter) throws JMSException {
        // do not cache instance, would not work for streaming messages
        return (BytesMessage) createMessage();
    }

    public BytesMessage createTypedMessage() throws JMSException {
        BytesMessage bytesMessage = session.createBytesMessage();

        LOG.debug("Filling ByteMessage with binary data");
        content = messageContent();
        if (content instanceof byte[]) {
            bytesMessage.writeBytes((byte[]) content);
        } else if (content instanceof InputStream) {
            final String jmsAmqInputStream = "JMS_AMQ_InputStream";
            bytesMessage.setObjectProperty(jmsAmqInputStream, content);
        }

        return bytesMessage;
    }

    @Override
    public void close() throws Exception {
        if (content instanceof Closeable) {
            ((Closeable) content).close();
        }
    }
}
