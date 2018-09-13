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

package com.redhat.mqe.lib

import com.redhat.mqe.lib.Main.main
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.Mockito.*
import javax.jms.*

class JmsMocks {
    val message = mock(Message::class.java)
    val producer = mock(MessageProducer::class.java)
    val consumer = mock(MessageConsumer::class.java)
    val session = mock(Session::class.java)
    val connection = mock(Connection::class.java)
    val connectionManager = mock(ConnectionManager::class.java)
    val connectionManagerFactory = mock(ConnectionManagerFactory::class.java)

    init {
        given(consumer.receive(anyLong()))
            .willReturn(message, null)
        given(session.createProducer(any(Destination::class.java)))
            .willReturn(producer)
        given(session.createProducer(null))
            .willReturn(producer)
        given(session.createMessage()).willReturn(message)
        given(session.createConsumer(isNull(), anyString(), anyBoolean()))
            .willReturn(consumer)
        given(connection.createSession(anyBoolean(), anyInt()))
            .willReturn(session)
        given(connectionManager.getConnection()).willReturn(connection)
        given(connectionManagerFactory.make(any(ClientOptions::class.java), anyString()))
            .willReturn(connectionManager)
    }
}

class InteractionTest {
    @Test
    fun `test run sender no params`() {
        val mocks = JmsMocks()

        val args = arrayOf("sender")
        val client = DaggerFakeClient.builder()
            .connectionManagerFactory(mocks.connectionManagerFactory)
            .messageFormatter(mock(JmsMessageFormatter::class.java))
            .clientOptionManager(mock(ClientOptionManager::class.java))
            .args(args)
            .build()

        main(args, client)  // or client.makeSenderClient().startClient()

        verify(mocks.producer, times(1)).send(mocks.message)
    }

    @Test
    fun `test run receiver no params`() {
        val mocks = JmsMocks()

        val args = arrayOf("receiver")
        val client = DaggerFakeClient.builder()
            .connectionManagerFactory(mocks.connectionManagerFactory)
            .messageFormatter(mock(JmsMessageFormatter::class.java))
            .clientOptionManager(mock(ClientOptionManager::class.java))
            .args(args)
            .build()

        main(args, client)  // or client.makeSenderClient().startClient()

        verify(mocks.consumer, times(2)).receive(anyLong())
    }
}
