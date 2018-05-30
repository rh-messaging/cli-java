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

import com.google.common.truth.Truth.assertThat
import com.redhat.mqe.ClientListener
import org.junit.jupiter.api.Assertions.assertTimeoutPreemptively
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.function.Executable
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvFileSource
import org.junit.jupiter.params.provider.ValueSource
import java.io.File
import java.math.BigInteger
import java.nio.file.Files
import java.security.MessageDigest
import java.security.Permission
import java.time.Duration
import java.time.LocalTime
import java.util.*
import kotlin.collections.ArrayList
import kotlin.test.fail

class SystemExitingWithStatus(val status: Int) : SecurityException()

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
    abstract val sslBrokerUrl: String
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

    abstract fun main_(listener: ClientListener, args: Array<String>)
    fun main(args: Array<String>): List<Map<String, Any>> {
        val messages = ArrayList<Map<String, Any>>()
        main_(object : ClientListener {
            override fun onMessage(message: Map<String, Any>) {
                messages.add(message)
            }

            override fun onOutput(output: String?) {
                TODO("not implemented")
            }

            override fun onError(error: String?) {
                TODO("not implemented")
            }
        }, args)
        return messages
    }

    @BeforeEach
    fun setup() {
        print("${LocalTime.now()} ")
        // https://stackoverflow.com/questions/41107/how-to-generate-a-random-alpha-numeric-string
        randomSuffix = BigInteger(130, random).toString(32)
    }

    @Tag("pairwise")
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

    @Test
    fun sendAndReceiveSingleMessageLogInterop() {
        val senderParameters =
            "sender --log-msgs interop --broker $brokerUrl --address $address --count 1".split(" ").toTypedArray()
        val receiverParameters =
            "receiver --log-msgs interop --broker $brokerUrl --address $address --count 1".split(" ").toTypedArray()
        assertTimeoutPreemptively(Duration.ofSeconds(10)) {
            print("Sending: ")
            main(senderParameters)
            print("Receiving: ")
            main(receiverParameters)
        }
    }

    @Test
    fun sendAndReceiveSingleMessageLogJson() {
        val senderParameters =
            "sender --log-msgs dict --out json --broker $brokerUrl --address $address --count 1".split(" ").toTypedArray()
        val receiverParameters =
            "receiver --log-msgs dict --out json --broker $brokerUrl --address $address --count 1".split(" ").toTypedArray()
        assertTimeoutPreemptively(Duration.ofSeconds(10)) {
            print("Sending: ")
            main(senderParameters)
            print("Receiving: ")
            main(receiverParameters)
        }
    }

    @Test
    fun sendAndReceiveSingleMessageLogJsonFloatType() {
        val senderParameters =
            "sender --log-msgs dict --out json --broker $brokerUrl --address $address --msg-property baf~42.2 --count 1".split(" ").toTypedArray()
        val receiverParameters =
            "receiver --log-msgs dict --out json --broker $brokerUrl --address $address --count 1".split(" ").toTypedArray()
        assertTimeoutPreemptively(Duration.ofSeconds(10)) {
            print("Sending: ")
            main(senderParameters)
            print("Receiving: ")
            main(receiverParameters)
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

    @Tag("pairwise")
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

    @Tag("pairwise")
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

    @Test
    fun sendAndReceiveAnInt() {
        assertNoSystemExit {
            val senderParameters =
                """sender --log-msgs dict --broker $brokerUrl --address $address --count 1 --content-type int --msg-content 1234""".split(" ").toTypedArray()
            val receiverParameters =
                "receiver --log-msgs dict --broker $brokerUrl --address $address --count 1".split(" ").toTypedArray()

            print("Sending: ")
            main(senderParameters)
            print("Receiving: ")
            main(receiverParameters)
        }
    }

    @Test
    fun sendAndReceiveMessageFromFile() {
        val file = File.createTempFile(address, null)
        try {
            file.writeText("aContent")
            val senderParameters =
                "sender --log-msgs dict --broker $brokerUrl --address $address --count 1 --msg-content-from-file $file".split(" ").toTypedArray()
            val receiverParameters =
                "receiver --log-msgs dict --broker $brokerUrl --address $address --count 1".split(" ").toTypedArray()

            print("Sending: ")
            main(senderParameters)
            print("Receiving: ")
            main(receiverParameters)
        } finally {
            file.delete()
        }
    }

    @Test
    fun sendAndReceiveBinaryMessageFromFile() {
        val file = File.createTempFile(address, null)
        try {
            file.writeText("aContent")
            val senderParameters =
                "sender --log-msgs dict --broker $brokerUrl --address $address --count 1 --msg-content-from-file $file --msg-content-binary true".split(" ").toTypedArray()
            val receiverParameters =
                "receiver --log-msgs dict --broker $brokerUrl --address $address --count 1".split(" ").toTypedArray()

            print("Sending: ")
            main(senderParameters)
            print("Receiving: ")
            main(receiverParameters)
        } finally {
            file.delete()
        }
    }

    @Test
    fun sendMessageFromNonexistentFile() {
        assertSystemExit(2, Executable {
            val file = "noSuchFile"
            val senderParameters =
                "sender --log-msgs dict --broker $brokerUrl --address $address --count 1 --msg-content-from-file $file".split(" ").toTypedArray()
            main(senderParameters)
        })
    }

    @Test
    fun sendBinaryMessageFromNonexistentFile() {
        assertSystemExit(2, Executable {
            val file = "noSuchFile"
            val senderParameters =
                "sender --log-msgs dict --broker $brokerUrl --address $address --count 1 --msg-content-from-file $file --msg-content-binary true".split(" ").toTypedArray()
            main(senderParameters)
        })
    }

    @Test
    fun sendAndReceiveTextMessageFromToFile() {
        val file = File.createTempFile(address, "input")
        val outputDirectory = Files.createTempDirectory(address)
        val output = outputDirectory.resolve("message")
        val output0 = outputDirectory.resolve("message_0")
        try {
            file.writeText("aContent")
            val senderParameters =
                "sender --log-msgs dict --broker $brokerUrl --address $address --count 1 --msg-content-from-file $file".split(" ").toTypedArray()
            val receiverParameters =
                "receiver --log-msgs dict --broker $brokerUrl --address $address --count 1 --msg-content-to-file $output".split(" ").toTypedArray()

            print("Sending: ")
            main(senderParameters)
            print("Receiving: ")
            main(receiverParameters)

            assertThat(output0.toFile().readText()).isEqualTo(file.readText())
        } finally {
            file.delete()
            outputDirectory.toFile().deleteRecursively()
        }
    }

    @Test
    fun sendAndReceiveMessageToTopic() {
        val senderParameters =
            "sender --log-msgs dict --broker $brokerUrl --address topic://$address --count 1".split(" ").toTypedArray()
        val receiverParameters =
            "receiver --log-msgs dict --broker $brokerUrl --address topic://$address --count 1".split(" ").toTypedArray()

        val t = Thread {
            print("Receiving: ")
            main(receiverParameters)
        }
        t.start()
        Thread.sleep(1000)
        print("Sending: ")
        main(senderParameters)
        t.join()
    }

    @Test
    open fun sendSingleMessageAllTrustingTls() {
        assertNoSystemExit {
            //        "sender --log-msgs dict --broker tcp://127.0.0.1:61617 --address lalaLand --count 1 --conn-ssl-truststore-location  --conn-ssl-truststore-password secureexample --conn-ssl-keystore-location /home/jdanek/Downloads/AMQ7/amq7cr1i0/ikeysiold/client-side-keystore.jks --conn-ssl-keystore-password secureexample".split(" ").toTypedArray()
            val senderParameters =
                "sender --log-msgs dict --broker $sslBrokerUrl --address $address --conn-ssl-verify-host false --conn-ssl-trust-all true --count 1".split(" ").toTypedArray()
            print("Sending: ")
            main(senderParameters)
        }
    }

    @Test
    open fun sendLargeMessageChangingLimit() {
        val senderParameters =
            ("sender --log-msgs dict" +
                " --address $address" +
                " --broker-uri $brokerUrl?" +
                "minLargeMessageSize=250000" +
                ""
                ).split(" ").toTypedArray()
        main(senderParameters)
    }

    @Test
    fun `send and receive with --msg-content-hashed option`() {
        val content = "aContent\n"  // c.f. sha1sum <<<aContent
        val md = MessageDigest.getInstance("SHA-1")
        val expected = BigInteger(1, md.digest(content.toByteArray())).toString(16)
        print(expected)
        assertNoSystemExit {
            val sent = main(arrayOf(
                "sender", "--log-msgs", "dict", "--broker", brokerUrl, "--address", address, "--count", "1", "--msg-content", content, "--msg-content-hashed"))
            val received = main(arrayOf(
                "receiver", "--log-msgs", "dict", "--broker", brokerUrl, "--address", address, "--count", "1", "--msg-content-hashed"))

            assertThat(sent.map { it["content"] as String }).containsExactlyElementsIn(listOf(expected))
            assertThat(received.map { it["content"] as String }).containsExactlyElementsIn(listOf(expected))
        }
    }
}
