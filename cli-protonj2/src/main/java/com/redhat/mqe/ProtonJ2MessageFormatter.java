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

import java.util.HashMap;
import java.util.Map;

public class ProtonJ2MessageFormatter extends MessageFormatter {

    public Map<String, Object> formatMessage(String address, Message<Object> message, boolean msgContentHashed) throws ClientException {
        Map<String, Object> map = new HashMap<>();
        map.put("address", address);
        map.put("group-id", message.groupId());
        map.put("subject", message.subject());
        map.put("user-id", message.userId());
        map.put("correlation-id", message.correlationId());
        map.put("content-encoding", message.contentEncoding());
        map.put("priority", (int) message.priority());   // TODO(ENTMQCL-2973) remove cast
        map.put("type", message.contentType());
//        map.put("ttl", message.timeToLive());  // todo, why do we do the weird thing below instead of this?
        map.put("ttl", getTtl(message));
        map.put("absolute-expiry-time", message.absoluteExpiryTime());
        if (msgContentHashed) {
            map.put("content", MessageFormatter.hash(formatObject(message.body())));
        } else {
            map.put("content", message.body());
        }
        map.put("redelivered", message.deliveryCount() > 1);
        map.put("reply-to-group-id", message.replyToGroupId());
        map.put("durable", message.durable());
        map.put("group-sequence", (long) message.groupSequence());  // TODO(ENTMQCL-2973) remove cast
        map.put("creation-time", message.creationTime());
        map.put("content-type", message.contentType());
        map.put("id", message.messageId());
        map.put("reply-to", message.replyTo());

        // getPropertyNames? from JMS missing?
        Map<String, Object> propertyMap = new HashMap<>();
        message.forEachProperty((s, o) -> {
            propertyMap.put((String) s, o);  // this wanted to cast to string when I removed message generic type; what??? TODO
        });
        map.put("properties", propertyMap);

        return map;
    }


    /**
     * Calculate TTL of given message from message
     * expiration time and message timestamp.
     * <p/>
     * Returns the time the message expires, which is the sum of the time-to-live value
     * specified by the client and the GMT at the time of the send
     * EXP_TIME = CLIENT_SEND+TTL (CLIENT_SEND??)
     * CLIENT_SEND time is approximately getJMSTimestamp() (time value between send()/publish() and return)
     * TODO - check for correctness
     *
     * @param message calculate TTL for this message
     * @return positive long number if TTL was calculated. Long.MIN_VALUE if error.
     */
    public static long getTtl(Message<Object> message) {
        long ttl = 0;
        try {
            long expiration = message.absoluteExpiryTime();
            long timestamp = message.creationTime();
            if (expiration != 0 && timestamp != 0) {
                ttl = expiration - timestamp;
            }
        } catch (ClientException jmse) {
//            LOG.error("Error while calculating TTL value.\n" + jmse.getMessage());
            jmse.printStackTrace();
            System.exit(1);
        }
        return ttl;
    }
}
