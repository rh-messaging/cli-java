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
import org.apache.logging.log4j.*
import org.apache.logging.log4j.core.*
import org.apache.logging.log4j.core.appender.AbstractAppender
import org.apache.logging.log4j.core.config.*
import org.apache.qpid.jms.transports.TransportSupport
import org.junit.jupiter.api.*
import org.junit.jupiter.api.io.TempDir
import util.Broker
import java.io.File
import java.math.BigInteger
import java.nio.file.Path
import java.util.*
import javax.jms.Connection
import javax.jms.ConnectionFactory
import javax.jms.Session

@Tag("issue")
class QPIDJMS391Test {
    val prefix: String = "QPIDJMS391Test_"
    lateinit var randomSuffix: String
    val address: String
        get() = prefix + randomSuffix

    val random = Random()

    @BeforeEach
    fun checkIfOnRhel() {
        Assumptions.assumeTrue(File("/etc/redhat-release").exists())
    }

    @BeforeEach
    fun setUp() {
        // https://stackoverflow.com/questions/41107/how-to-generate-a-random-alpha-numeric-string
        randomSuffix = BigInteger(130, random).toString(32)
    }

    @Test
    fun `logging says that openssl is being used`(@TempDir tempDir: Path) {
        val keystore = this.javaClass.getResourceAsStream("server-side-keystore.jks")
        val broker = Broker(tempDir)
        configureBroker(broker)
        val amqpPort = broker.startBroker()
        val amqpsPort = broker.addAMQPSAcceptor(keystore)

        val listAppender = ArrayListAppender.installLogger(TransportSupport::class.java.name, Level.DEBUG)

        // the config option is only used when we create actual ssl connection
        val f: ConnectionFactory = org.apache.qpid.jms.JmsConnectionFactory(
            "amqps://127.0.0.1:$amqpsPort?transport.useOpenSSL=true&transport.trustAll=true&transport.verifyHost=false"
        )
        val c: Connection = f.createConnection(USER_NAME, PASSWORD)
        c.start()
        val s: Session = c.createSession(Session.AUTO_ACKNOWLEDGE)
        s.close()
        c.close()

        assertThat(listAppender.messages)
            .comparingElementsUsing(Correspondence.from(::regexpCorrespondence, "RegexpCorrespondence())"))
            .contains("OpenSSL Enabled: Version .* of OpenSSL will be used")

        broker.close()
    }

    private val USER_NAME = "someUser"
    private val PASSWORD = "somePassword"

    private fun configureBroker(broker: Broker) {
        val securityConfiguration = SecurityConfiguration()
        securityConfiguration.addUser(USER_NAME, PASSWORD)
        val activeMQJAASSecurityManager = ActiveMQJAASSecurityManager(
            "org.apache.activemq.artemis.spi.core.security.jaas.InVMLoginModule", securityConfiguration
        )
        broker.embeddedBroker.setSecurityManager(activeMQJAASSecurityManager)

        broker.configuration.isPersistenceEnabled = false
        broker.configuration.isSecurityEnabled = true
    }

    companion object {
        private const val overrideDefaultTLS = "com.ibm.jsse2.overrideDefaultTLS"

        @JvmStatic
        fun regexpCorrespondence(actual: String?, expected: String?): Boolean {
            return actual!!.matches(Regex(expected!!))
        }


        @JvmStatic
        @BeforeAll
        fun setProperty() {
            // ENTMQBR-640, enable TLSv1.1 and TLSv1.2 on IBM Java 8
            // https://www.ibm.com/support/knowledgecenter/en/SSYKE2_8.0.0/com.ibm.java.security.component.80.doc/security-component/jsse2Docs/matchsslcontext_tls.html
            System.setProperty(overrideDefaultTLS, "true")
        }

        @JvmStatic
        @AfterAll
        fun unsetProperty() {
            System.clearProperty(overrideDefaultTLS)
        }
    }
}

// https://stackoverflow.com/questions/59713891/appenderskeleton-log4j2
class ArrayListAppender : AbstractAppender("ArrayListAppender", null, null, true, Property.EMPTY_ARRAY) {
    val messages = ArrayList<String>()

    override fun append(event: LogEvent) {
        messages.add(event.message.formattedMessage)
    }

    companion object {

        /**
         *
         * Creates an instance and attaches it to log4j2 as logger for the given name.
         *
         * Blind alleys
         *
         *         val listAppender = ArrayListAppender()
         *         listAppender.start()
         *         (LogManager.getContext(true).getLogger(TransportSupport::class.java) as Logger).let {
         *             it.level = Level.DEBUG
         *             it.addAppender(listAppender)
         *         }
         *
         * That will reconfigure existing logger, and since we don't have config for this yet, it will reconfigure
         * the root logger (named ""). It looks up the closest parent, which in this test is going to be root.
         *
         */
        fun installLogger(loggerName: String, loggerLevel: Level?): ArrayListAppender {
            // note this code is specific for log4j2-core, does not use slf4j abstraction nor log4j2 abstraction
            val loggerContext = LogManager.getContext(false) as LoggerContext
            val configuration = loggerContext.configuration

            val listAppender = ArrayListAppender()
            listAppender.start()

            // https://logging.apache.org/log4j/2.x/manual/customconfig.html#AddingToCurrent
            configuration.addAppender(listAppender)
            val loggerConfig = LoggerConfig.newBuilder()
                .withLoggerName(loggerName)
                .withLevel(loggerLevel)
                .withAdditivity(true)
                .withRefs(
                    arrayOf(AppenderRef.createAppenderRef(listAppender.name, loggerLevel, null))
                )
                .withProperties(null)
                .withConfig(loggerContext.configuration)
                .withtFilter(null)
                .build()

            loggerConfig.addAppender(listAppender, loggerLevel, null)

            configuration.addLogger(loggerName, loggerConfig)

            loggerContext.updateLoggers()

            return listAppender
        }
    }
}
