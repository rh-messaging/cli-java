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

import com.google.common.truth.Correspondence
import com.google.common.truth.Truth.assertThat
import org.apache.log4j.Level
import org.apache.log4j.LogManager
import org.apache.qpid.jms.JmsConnection
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import util.Broker
import java.math.BigInteger
import java.nio.file.Path
import java.util.*
import javax.jms.Connection
import javax.jms.ConnectionFactory
import javax.jms.Session

@Tag("issue")
class QPIDJMS502Test {
    val prefix: String = "QPIDJMS502Test_"
    lateinit var randomSuffix: String
    val address: String
        get() = prefix + randomSuffix

    val random = Random()

    @BeforeEach
    fun setUp() {
        // https://stackoverflow.com/questions/41107/how-to-generate-a-random-alpha-numeric-string
        randomSuffix = BigInteger(130, random).toString(32)
    }

    @Test
    fun `failover reconnect is logged`(@TempDir tempDir: Path) {
        val broker = Broker(tempDir)
        configureBroker(broker)
        broker.startBroker()
        val amqpPort1 = broker.addAMQPAcceptor()
        val amqpPort2 = broker.addAMQPAcceptor()

        val ala = ArrayListAppender()
        LogManager.getLogger(JmsConnection::class.java).let {
            it.level = Level.INFO
            it.addAppender(ala)
        }

        val f: ConnectionFactory = org.apache.qpid.jms.JmsConnectionFactory(
            "failover:(amqp://127.0.0.1:$amqpPort1,amqp://127.0.0.1:$amqpPort2)")
        val c: Connection = f.createConnection()
        c.start()
        val s: Session = c.createSession(Session.AUTO_ACKNOWLEDGE)

        val oldConnection = broker.embeddedBroker.activeMQServer.remotingService.connections.first()
        val oldId = oldConnection.id
        oldConnection.destroy()

        await().untilAsserted {
            val connections = broker.embeddedBroker.activeMQServer.remotingService.connections
            assertThat(connections).hasSize(1);
            assertThat(connections.first().id).isNotEqualTo(oldId)
        }

        val messages = ala.loggingEvents.map { it.renderedMessage }
        assertThat(messages)
            .comparingElementsUsing(Correspondence.from(::regexpCorrespondence, "RegexpCorrespondence())"))
            .contains("Connection .* restored to server: .*")

        s.close()
        c.close()

        broker.close()
    }

    private fun configureBroker(broker: Broker) {
        broker.configuration.isSecurityEnabled = false
        broker.configuration.isPersistenceEnabled = false
    }

    companion object {
        @JvmStatic
        @BeforeAll
        internal fun configureLogging() {
//            val consoleAppender = ConsoleAppender(SimpleLayout(), ConsoleAppender.SYSTEM_OUT)
//            LogManager.getRootLogger().addAppender(consoleAppender)
        }

        @JvmStatic
        fun regexpCorrespondence(actual: String?, expected: String?): Boolean {
            return actual!!.matches(Regex(expected!!))
        }
    }
}
