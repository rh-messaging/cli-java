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
import org.apache.activemq.artemis.core.config.impl.SecurityConfiguration
import org.apache.activemq.artemis.spi.core.security.ActiveMQJAASSecurityManager
import org.apache.log4j.*
import org.apache.log4j.spi.LoggingEvent
import org.apache.qpid.jms.transports.TransportSupport
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junitpioneer.jupiter.TempDirectory
import util.Broker
import java.math.BigInteger
import java.nio.file.Path
import java.util.*
import javax.jms.Connection
import javax.jms.ConnectionFactory
import javax.jms.Session

@Tag("issue")
abstract class QPIDJMS391Test {
    val prefix: String = "QPIDJMS391Test_"
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
    @ExtendWith(TempDirectory::class)
    fun `logging says that openssl is being used`(@TempDirectory.TempDir tempDir: Path) {
        val keystore = this.javaClass.getResourceAsStream("server-side-keystore.jks")
        val broker = Broker(tempDir)
        configureBroker(broker)
        val amqpPort = broker.startBroker()
        val amqpsPort = broker.addAMQPSAcceptor(keystore)

        val ala = ArrayListAppender()
        LogManager.getLogger(TransportSupport::class.java).let {
            it.level = Level.DEBUG
            it.addAppender(ala)
        }

        // the config option is only used when we create actual ssl connection
        val f: ConnectionFactory = org.apache.qpid.jms.JmsConnectionFactory(
            "amqps://127.0.0.1:$amqpsPort?transport.useOpenSSL=true&transport.trustAll=true&transport.verifyHost=false")
        val c: Connection = f.createConnection(USER_NAME, PASSWORD)
        c.start()
        val s: Session = c.createSession(Session.AUTO_ACKNOWLEDGE)
        s.close()
        c.close()

        val messages = ala.loggingEvents.map { it.renderedMessage }
        assertThat(messages)
            .comparingElementsUsing(RegexpCorrespondence())
            .contains("OpenSSL Enabled: Version .* of OpenSSL will be used")

        broker.close()
    }

    private val USER_NAME = "someUser"
    private val PASSWORD = "somePassword"

    private fun configureBroker(broker: Broker) {
        val securityConfiguration = SecurityConfiguration()
        securityConfiguration.addUser(USER_NAME, PASSWORD)
        val activeMQJAASSecurityManager = ActiveMQJAASSecurityManager(
            "org.apache.activemq.artemis.spi.core.security.jaas.InVMLoginModule", securityConfiguration)
        broker.embeddedBroker.setSecurityManager(activeMQJAASSecurityManager)

        broker.configuration.isPersistenceEnabled = false
        broker.configuration.isSecurityEnabled = true
    }

    companion object {
        @JvmStatic
        @BeforeAll
        internal fun configureLogging() {
            val consoleAppender = ConsoleAppender(SimpleLayout(), ConsoleAppender.SYSTEM_OUT)
            LogManager.getRootLogger().addAppender(consoleAppender)
        }
    }
}

class RegexpCorrespondence : Correspondence<String, String>() {
    override fun toString(): String {
        return "Must match an expected regexp"
    }

    override fun compare(actual: String?, expected: String?): Boolean {
        return actual!!.matches(Regex(expected!!))
    }
}

class ArrayListAppender : AppenderSkeleton() {
    val loggingEvents = ArrayList<LoggingEvent>()

    override fun requiresLayout(): Boolean = false

    override fun append(loggingEvent: LoggingEvent) {
        loggingEvents.add(loggingEvent)
    }

    override fun close() = Unit
}

