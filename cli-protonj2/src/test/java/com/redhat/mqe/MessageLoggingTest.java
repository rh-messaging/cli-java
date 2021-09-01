/*
 * Copyright (c) 2021 Red Hat, Inc.
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

package com.redhat.mqe;

import com.redhat.mqe.lib.MessageFormatter;
import org.apache.qpid.protonj2.client.Message;
import org.apache.qpid.protonj2.client.exceptions.ClientException;

import java.util.List;
import java.util.stream.Collectors;

class PreviousImplementation {
    void logMessage(String address, Message message, boolean msgContentHashed) throws ClientException {
        StringBuilder sb = new StringBuilder();

        sb.append("{");

        addKeyValue(sb, "address", address);
        addKeyValue(sb, "group-id", message.groupId());
        addKeyValue(sb, "subject", message.subject());
        addKeyValue(sb, "user-id", message.userId());
        addKeyValue(sb, "correlation-id", message.correlationId());
        addKeyValue(sb, "content-encoding", message.contentEncoding());
        addKeyValue(sb, "priority", message.priority());
        addKeyValue(sb, "type", "string");  // ???
        addKeyValue(sb, "ttl", message.timeToLive());
        addKeyValue(sb, "absolute-expiry-time", message.absoluteExpiryTime());
        if (msgContentHashed) {
            // this is inlined addKeyValue, TODO do it nicer
            sb.append("'");
            sb.append("content");
            sb.append("': ");
            sb.append("'"); // extra quotes to format
            sb.append(MessageFormatter.hash(formatPython(message.body())));
            sb.append("'");
            sb.append(", ");
        } else {
            addKeyValue(sb, "content", message.body());
        }
        addKeyValue(sb, "redelivered", message.deliveryCount() > 1);
        addKeyValue(sb, "reply-to-group-id", message.replyToGroupId());
        addKeyValue(sb, "durable", message.durable());
        addKeyValue(sb, "group-sequence", message.groupSequence());
        addKeyValue(sb, "creation-time", message.creationTime());
        addKeyValue(sb, "content-type", message.contentType());
        addKeyValue(sb, "id", message.messageId());
        addKeyValue(sb, "reply-to", message.replyTo());

        // getPropertyNames? from JMS missing?
        StringBuilder sbb = new StringBuilder();
        sbb.append('{');
//        AtomicBoolean first = new AtomicBoolean(true);
        message.forEachProperty((s, o) -> {
//            if (!first.get()) {
//                sbb.append(", ");
//                first.set(false);
//            }
            addKeyValue(sbb, (String) s, o);  // this wanted to cast to string when I removed message generic type; what??? TODO
        });
        if (message.hasProperties()) {
            sbb.delete(sbb.length() - 2, sbb.length());  // remove last ", "
        }
        sbb.append('}');
        addKeyValue(sb, "properties", sbb); // ???

        sb.delete(sb.length() - 2, sb.length());  // remove last ", "

        sb.append("}");

        System.out.println(sb);
    }

    void addKeyValue(StringBuilder sb, String key, Object value) {
        sb.append("'");
        sb.append(key);
        sb.append("': ");
        sb.append(formatPython(value));
        sb.append(", ");
    }

    String formatPython(Object parameter) {
        if (parameter == null) {
            return "None";
        }
        if (parameter instanceof String) {
            return "'" + parameter + "'";
        }
        if (parameter instanceof Boolean) {
            return ((boolean)parameter) ? "True" : "False";
        }
        if (parameter instanceof StringBuilder) {
            return parameter.toString();
        }
        if (parameter instanceof List) {
            return "[" + ((List<Object>) parameter).stream().map(this::formatPython).collect(Collectors.joining(", ")) + "]";
        }
        return "'" + parameter + "'";
    }
}

public class MessageLoggingTest {
}
