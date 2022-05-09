/*
 * Copyright (c) 2022 Red Hat, Inc.
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
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import jakarta.jms.*

class InteractionTest {
    @Mock
    lateinit var message: Message
    @Mock
    lateinit var producer: MessageProducer
    @Mock
    lateinit var consumer: MessageConsumer
    @Mock
    lateinit var session: Session
    @Mock
    lateinit var connection: Connection
    @Mock
    lateinit var connectionManager: ConnectionManager
    @Mock
    lateinit var connectionManagerFactory: ConnectionManagerFactory

    @BeforeEach
    fun setUpMocks() {
        MockitoAnnotations.initMocks(this)

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

    @Test
    fun `test run sender no params`() {
        val args = arrayOf("sender")
        val client = createFakeClient(args)

        main(args, client)  // or client.makeSenderClient().startClient()

        verify(producer, times(1)).send(message)
    }

    @Test
    fun `test run receiver no params`() {
        val args = arrayOf("receiver")
        val client = createFakeClient(args)

        main(args, client)  // or client.makeSenderClient().startClient()

        verify(consumer, times(2)).receive(anyLong())
    }

    private fun createFakeClient(args: Array<String>): FakeClient {
        return DaggerFakeClient.builder()
            .connectionManagerFactory(connectionManagerFactory)
            .messageFormatter(mock(JmsMessageFormatter::class.java))
            .clientOptionManager(mock(ClientOptionManager::class.java))
            .args(args)
            .build()
    }
}
