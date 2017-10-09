/*
 * Copyright (c) 2017 Red Hat, Inc.
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

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigInteger
import java.util.*
import org.junit.jupiter.api.Assertions.assertTimeoutPreemptively
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.security.Permission
import java.time.Duration
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.function.Executable
import org.junit.jupiter.params.provider.CsvFileSource
import java.time.LocalTime
import kotlin.test.fail

class SystemExitingWithStatus(val status: Int) : Exception()

class NoExitSecurityManager(val parentManager: SecurityManager?) : SecurityManager() {
    override fun checkExit(status: Int) = throw SystemExitingWithStatus(status)
    override fun checkPermission(perm: Permission?) = Unit
}

fun assertSystemExit(status: Int, executable: Executable) {
    val previousManager = System.getSecurityManager()
    try {
        val manager = NoExitSecurityManager(previousManager)
        System.setSecurityManager(manager)

        executable.execute()

        fail("expected exception")
    } catch (e: SystemExitingWithStatus) {
        assertThat(e.status).isEqualTo(status)
    } finally {
        System.setSecurityManager(previousManager)
    }
}

fun assertNoSystemExit(executable: () -> Unit) {
    val previousManager = System.getSecurityManager()
    try {
        val manager = NoExitSecurityManager(previousManager)
        System.setSecurityManager(manager)

        executable()

    } catch (e: SystemExitingWithStatus) {
        fail("System.exit has been called")
    } finally {
        System.setSecurityManager(previousManager)
    }
}

abstract class AbstractMainTest {
    abstract val brokerUrl: String
    /**
     * Set all single-value options to some harmless nondefault value here.
     * Used in a test to increase code coverage and catch some unforeseen option interactions.
     */
    abstract val senderAdditionalOptions: Array<String>
    abstract val connectorAdditionalOptions: Array<String>

    val prefix: String = "lalaLand_"
    lateinit var randomSuffix: String
    val address: String
        get() = prefix + randomSuffix

    val random = Random()

    abstract fun main(args: Array<String>)

    @BeforeEach
    fun setup() {
        print(LocalTime.now())
        // https://stackoverflow.com/questions/41107/how-to-generate-a-random-alpha-numeric-string
        randomSuffix = BigInteger(130, random).toString(32)
    }

    @ParameterizedTest
    @ValueSource(strings = arrayOf("sender", "receiver", "connector"))
    fun printHelp(client: String) {
        val parameters =
            "$client --help".split(" ").toTypedArray()
        val previousManager = System.getSecurityManager()
        try {
            val manager = NoExitSecurityManager(previousManager)
            System.setSecurityManager(manager)
            main(parameters)
            fail("expected exception")
        } catch (e: SystemExitingWithStatus) {
            assertThat(e.status).isEqualTo(0)
        } finally {
            System.setSecurityManager(previousManager)
        }
    }

    @Test
    fun sendAndReceiveSingleMessage() {
        val senderParameters =
            "sender --log-msgs dict --broker $brokerUrl --address $address --count 1".split(" ").toTypedArray()
        val receiverParameters =
            "receiver --log-msgs dict --broker $brokerUrl --address $address --count 1".split(" ").toTypedArray()
        assertTimeoutPreemptively(Duration.ofSeconds(10)) {
            print("Sending: ")
            main(senderParameters)
            print("Receiving: ")
            main(receiverParameters)
        }
    }

    @Test
    fun sendBrowseAndReceiveSingleMessage() {
        val senderParameters =
            "sender --log-msgs dict --broker $brokerUrl --address $address --count 1".split(" ").toTypedArray()
        val receiverParameters =
            "receiver --log-msgs dict --broker $brokerUrl --address $address --count 1".split(" ").toTypedArray()
        assertTimeoutPreemptively(Duration.ofSeconds(10)) {
            print("Sending: ")
            main(senderParameters)
            print("Browsing: ")
            main(receiverParameters + "--recv-browse true".split(" ").toTypedArray())
            print("Receiving: ")
            main(receiverParameters)
        }
    }

    @Test
    fun connectConnector() {
        val connectorParameters =
            "connector --broker $brokerUrl --address $address --count 1".split(" ").toTypedArray()
        assertTimeoutPreemptively(Duration.ofSeconds(10)) {
            print("Connecting: ")
            main(connectorParameters)
        }
    }

    @Test
    fun sendAndReceiveSingleMessageUsingCredentials() {
        val senderParameters =
            "sender --log-msgs dict --broker $brokerUrl --address $address --conn-username admin --conn-password admin --count 1".split(" ").toTypedArray()
        val receiverParameters =
            "receiver --log-msgs dict --broker $brokerUrl --address $address --conn-username admin --conn-password admin --count 1".split(" ").toTypedArray()
        assertTimeoutPreemptively(Duration.ofSeconds(10)) {
            print("Sending: ")
            main(senderParameters)
            print("Receiving: ")
            main(receiverParameters)
        }
    }

    @Test
    fun sendBrowseAndReceiveSingleMessageWithEmptySelector() {
        val senderParameters =
            "sender --log-msgs dict --broker $brokerUrl --address $address --count 1".split(" ").toTypedArray()
        val receiverParameters =
            "receiver --log-msgs dict --broker $brokerUrl --address $address --msg-selector '' --count 1".split(" ").toTypedArray()
        assertTimeoutPreemptively(Duration.ofSeconds(10)) {
            print("Sending: ")
            main(senderParameters)
            print("Browsing: ")
            main(receiverParameters + "--recv-browse true".split(" ").toTypedArray())
            print("Receiving: ")
            main(receiverParameters)
        }
    }

    @Test
    fun sendSingleMessageWithoutProtocolInBrokerUrl() {
        val brokerUrl = brokerUrl.substringAfter(":")
        val senderParameters =
            "sender --log-msgs dict --broker $brokerUrl --address $address --count 1".split(" ").toTypedArray()
        assertTimeoutPreemptively(Duration.ofSeconds(10)) {
            print("Sending: ")
            main(senderParameters)
        }
    }

    @ParameterizedTest
    @CsvFileSource(resources = arrayOf("/receiver.csv"))
    fun sendAndReceiveWithAllReceiverCLISwitches(receiverDynamicOptions: String) {
        println(receiverDynamicOptions)
        val senderParameters =
            "sender --log-msgs dict --broker $brokerUrl --address $address --count 1".split(" ").toTypedArray()
        val receiverParameters =
            "receiver --broker $brokerUrl --address $address --count 1".split(" ").toTypedArray()

        assertNoSystemExit {
            print("Sending: ")
            main(senderParameters)
            print("Receiving: ")
            main(receiverParameters + receiverDynamicOptions.split(" ").toTypedArray())
        }
    }

    @ParameterizedTest
    @CsvFileSource(resources = arrayOf("/sender.csv"))
    fun sendAndReceiveWithAllSenderCLISwitches(senderDynamicOptions: String) {
        println(senderDynamicOptions)
        val senderParameters =
            "sender --broker $brokerUrl --address $address".split(" ").toTypedArray()
        val receiverParameters =
            "receiver --log-msgs dict --broker $brokerUrl --address $address --count 1".split(" ").toTypedArray()

        assertNoSystemExit {
            print("Sending: ")
            main(senderParameters + senderDynamicOptions.split(" ").toTypedArray() + senderAdditionalOptions)
            print("Receiving: ")
            main(receiverParameters)
        }
    }

    @ParameterizedTest
    @CsvFileSource(resources = arrayOf("/connector.csv"))
    fun connectConnectorWithAllSenderCLISwitches(senderDynamicOptions: String) {
        println(senderDynamicOptions)
        val connectorPrameters =
            "connector --broker $brokerUrl --address $address".split(" ").toTypedArray()

        assertNoSystemExit {
            print("Connecting: ")
            main(connectorPrameters + senderDynamicOptions.split(" ").toTypedArray() + connectorAdditionalOptions)
        }
    }

    @Test
    fun sendAndReceiveListMessage() {
        val senderParameters =
            """sender --log-msgs dict --broker $brokerUrl --address $address --count 1 --msg-content-list-item --msg-content-list-item "String" --msg-content-list-item "~1" --msg-content-list-item "~1.0" --msg-content-list-item "1" --msg-content-list-item "1.0" --msg-content-list-item "~-1" --msg-content-list-item "~-1.3" --msg-content-list-item "-1" --msg-content-list-item "~~1"""".split(" ").toTypedArray()
        val receiverParameters =
            "receiver --log-msgs dict --broker $brokerUrl --address $address --count 1".split(" ").toTypedArray()

        print("Sending: ")
        main(senderParameters)
        print("Receiving: ")
        main(receiverParameters)
    }
}
