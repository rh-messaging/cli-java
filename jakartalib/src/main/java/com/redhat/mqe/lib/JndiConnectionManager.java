/*
 * Copyright (c) 2019 Red Hat, Inc.
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

package com.redhat.mqe.lib;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSException;
import jakarta.jms.Queue;
import jakarta.jms.Topic;
import javax.naming.Context;
import javax.naming.NamingException;
import java.util.Hashtable;

public class JndiConnectionManager extends ConnectionManager {
    private final Logger LOG = LoggerFactory.getLogger(JndiConnectionManager.class.getName());
    private Context context = null;

    JndiConnectionManager(ClientOptions clientOptions, String brokerUrl) {
        try {
            Hashtable<String, String> environment = new Hashtable<>();
            Option configureFromFile = clientOptions.getOption(ClientOptions.CONN_USE_CONFIG_FILE);
            if (configureFromFile.hasParsedValue() && !configureFromFile.getValue().equals("true")) {
                environment.put(Context.PROVIDER_URL, configureFromFile.getValue());
            }

            context = new javax.naming.InitialContext(environment);
            destination = createQueue(clientOptions.getOption(ClientOptions.ADDRESS).getValue());
            factory = (ConnectionFactory) lookup("amqFactory");
            connection = factory.createConnection();
        } catch (NamingException | JMSException e) {
            LOG.error(e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    protected Queue createQueue(String queueName) {
        try {
            return (Queue) lookup(queueName);
        } catch (NamingException e) {
            throw new RuntimeException(e);
        }
    }

    private Object lookup(String name) throws NamingException {
        if (name.isEmpty()) {
            throw new RuntimeException("Name to lookup in naming.Context cannot be empty");
        }
        return context.lookup(name);
    }

    @Override
    protected Topic createTopic(String topicName) {
        try {
            return (Topic) lookup(topicName);
        } catch (NamingException e) {
            throw new RuntimeException(e);
        }
    }
}
