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

import com.google.common.truth.Truth
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.TruthJUnit
import com.redhat.mqe.ClientListener
import org.apache.activemq.artemis.core.settings.impl.AddressFullMessagePolicy
import org.apache.activemq.artemis.core.settings.impl.AddressSettings
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.Assertions.assertTimeoutPreemptively
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Tags
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.function.Executable
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvFileSource
import org.junit.jupiter.params.provider.ValueSource
import util.Broker
import util.BrokerFixture
import java.io.File
import java.lang.reflect.UndeclaredThrowableException
import java.math.BigInteger
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@Tag("external")
abstract class AbstractMainTest : AbstractTest() {
    abstract val brokerUrl: String
    abstract val sslBrokerUrl: String

    /**
     * Set all single-value options to some harmless nondefault value here.
     * Used in a test to increase code coverage and catch some unforeseen option interactions.
     */
    abstract val senderAdditionalOptions: Array<String>
    abstract val connectorAdditionalOptions: Array<String>

    open val address: String
        get() = prefix + randomSuffix

    abstract fun main_(listener: ClientListener, args: Array<String>)
    fun main(args: Array<String>): List<Map<String, Any>> {
        val messages = ArrayList<Map<String, Any>>()
        main(args, messages)
        return messages
    }

    fun main(args: Array<String>, messages: MutableList<Map<String, Any>>): List<Map<String, Any>> {
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
        print(LocalTime.now())
        randomSuffix = generateRandomSuffix()
    }

    @Tags(Tag("pairwise"), Tag("external"))
    @ParameterizedTest
    @ValueSource(strings = ["sender", "receiver", "connector"])
    fun printHelp(client: String) {
        val parameters =
            "$client --help".split(" ").toTypedArray()
        assertSystemExit(0, Executable {
            main(parameters)
        })
    }

    @Tag("external")
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

    @Tag("external")
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

    @Tag("external")
    @Test
    fun connectConnector() {
        val connectorParameters =
            "connector --broker $brokerUrl --address $address --count 1".split(" ").toTypedArray()
        assertTimeoutPreemptively(Duration.ofSeconds(10)) {
            print("Connecting: ")
            assertNoSystemExit {
                main(connectorParameters)
            }
        }
    }

    @Tag("external")
    @Test
    fun sendAndReceiveSingleMessageUsingCredentials() {
        val senderParameters =
            "sender --log-msgs dict --broker $brokerUrl --address $address --conn-username admin --conn-password admin --count 1".split(
                " "
            ).toTypedArray()
        val receiverParameters =
            "receiver --log-msgs dict --broker $brokerUrl --address $address --conn-username admin --conn-password admin --count 1".split(
                " "
            ).toTypedArray()
        assertTimeoutPreemptively(Duration.ofSeconds(10)) {
            print("Sending: ")
            main(senderParameters)
            print("Receiving: ")
            main(receiverParameters)
        }
    }

    @Tag("external")
    @Test
    fun test_simple_transaction_sender_batch_size_leftovers() {
        print("Sending:\n ")
        val sent = main(
            "sender --log-msgs interop --broker $brokerUrl --conn-auth-mechanisms PLAIN --conn-username admin --conn-password admin --address $address --count 11 --tx-size 3 --tx-action commit".split(" ").toTypedArray()
        )
        print("Receiving:\n ")
        val received = main(
            "receiver --timeout 10 --log-msgs interop --broker $brokerUrl --conn-auth-mechanisms PLAIN --conn-username admin --conn-password admin --address $address --count 0".split(" ").toTypedArray()
        )
        assertThat(sent).hasSize(11)
        assertThat(received).hasSize(9)
    }

    @Tag("external")
    @Test
    fun sendBrowseAndReceiveSingleMessageWithEmptySelector() {
        val senderParameters =
            "sender --log-msgs dict --broker $brokerUrl --address $address --count 1".split(" ").toTypedArray()
        val receiverParameters =
            "receiver --log-msgs dict --broker $brokerUrl --address $address --msg-selector= --count 1".split(" ")
                .toTypedArray()
        assertTimeoutPreemptively(Duration.ofSeconds(10)) {
            print("Sending: ")
            main(senderParameters)
            print("Browsing: ")
            main(receiverParameters + "--recv-browse true".split(" ").toTypedArray())
            print("Receiving: ")
            main(receiverParameters)
        }
    }

    /**
     * Sends two messages and then uses selector to receive the second message.
     *
     * If selector was (erroneously) not applied, the first message would be received here.
     */
    @Tag("external")
    @Test
    fun sendBrowseAndReceiveSingleMessageWithNonemptySelector() {
        val senderParameters1 =
            "sender --log-msgs dict --broker $brokerUrl --address $address --count 1 --msg-property=a~1".split(" ").toTypedArray()
        val senderParameters2 =
            "sender --log-msgs dict --broker $brokerUrl --address $address --count 1".split(" ").toTypedArray()
        val receiverParameters =
            "receiver / --log-msgs=dict / --broker=$brokerUrl / --address=$address / --msg-selector=a IS NULL / --timeout=2".split(" / ").toTypedArray()
        assertTimeoutPreemptively(Duration.ofSeconds(10)) {
            print("Sending: ")
            main(senderParameters1)
            main(senderParameters2)
            print("Browsing: ")
            val browsed = main(receiverParameters + "--recv-browse true".split(" ").toTypedArray())
            assertThat(browsed).hasSize(1)
            // can't assert empty properties, openwire adds its own,
            //  `{__AMQ_CID=ID:fedora.jiridanek-42217-1666942674180-2:1}`
            assertThat(browsed[0]["properties"] as Map<String, Any>).doesNotContainKey("a")
            print("Receiving: ")
            val received = main(receiverParameters)
            assertThat(received).hasSize(1)
            assertThat(received[0]["properties"] as Map<String, Any>).doesNotContainKey("a")
        }
    }

    @Tag("external")
    @Test
    fun sendSingleMessageWithoutProtocolInBrokerUrl() {
        val brokerUrl = brokerUrl.substringAfterLast("/")
        val senderParameters =
            "sender --log-msgs dict --broker $brokerUrl --address $address --count 1".split(" ").toTypedArray()
        assertTimeoutPreemptively(Duration.ofSeconds(10)) {
            print("Sending: ")
            main(senderParameters)
        }
    }

    @Tag("external")
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

    @Tag("external")
    @Test
    fun sendAndReceiveSingleMessageLogJson() {
        val senderParameters =
            "sender --log-msgs dict --out json --broker $brokerUrl --address $address --count 1".split(" ")
                .toTypedArray()
        val receiverParameters =
            "receiver --log-msgs dict --out json --broker $brokerUrl --address $address --count 1".split(" ")
                .toTypedArray()
        assertTimeoutPreemptively(Duration.ofSeconds(10)) {
            print("Sending: ")
            main(senderParameters)
            print("Receiving: ")
            main(receiverParameters)
        }
    }

    @Tag("external")
    @Test
    fun sendAndReceiveSingleMessageLogJsonFloatType() {
        val senderParameters =
            "sender --log-msgs dict --out json --broker $brokerUrl --address $address --msg-property baf~42.2 --count 1".split(
                " "
            ).toTypedArray()
        val receiverParameters =
            "receiver --log-msgs dict --out json --broker $brokerUrl --address $address --count 1".split(" ")
                .toTypedArray()
        assertTimeoutPreemptively(Duration.ofSeconds(10)) {
            print("Sending: ")
            main(senderParameters)
            print("Receiving: ")
            main(receiverParameters)
        }
    }

    @Tags(Tag("pairwise"), Tag("external"))
    @ParameterizedTest
    @CsvFileSource(resources = ["/receiver.csv"])
    open fun sendAndReceiveWithAllReceiverCLISwitches(receiverDynamicOptions: String) {
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

    @Tags(Tag("pairwise"), Tag("external"))
    @ParameterizedTest
    @CsvFileSource(resources = ["/sender.csv"])
    open fun sendAndReceiveWithAllSenderCLISwitches(senderDynamicOptions: String) {
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

    @Tags(Tag("pairwise"), Tag("external"))
    @ParameterizedTest
    @CsvFileSource(resources = ["/connector.csv"])
    open fun connectConnectorWithAllSenderCLISwitches(senderDynamicOptions: String) {
        println(senderDynamicOptions)
        val connectorPrameters =
            "connector --broker $brokerUrl --address $address".split(" ").toTypedArray()

        assertNoSystemExit {
            print("Connecting: ")
            main(connectorPrameters + senderDynamicOptions.split(" ").toTypedArray() + connectorAdditionalOptions)
        }
    }

    @Tag("external")
    @Test
    fun sendAndReceiveListMessage() {
        val senderParameters =
            """sender --log-msgs dict --broker $brokerUrl --address $address --count 1 --msg-content-list-item --msg-content-list-item "String" --msg-content-list-item "~1" --msg-content-list-item "~1.0" --msg-content-list-item "1" --msg-content-list-item "1.0" --msg-content-list-item "~-1" --msg-content-list-item "~-1.3" --msg-content-list-item "-1" --msg-content-list-item "~~1"""".split(
                " "
            ).toTypedArray()
        val receiverParameters =
            "receiver --log-msgs dict --broker $brokerUrl --address $address --count 1".split(" ").toTypedArray()

        print("Sending: ")
        main(senderParameters)
        print("Receiving: ")
        main(receiverParameters)
    }

    @Tag("external")
    @Test
    fun sendAndReceiveAnInt() {
        assertNoSystemExit {
            val senderParameters =
                """sender --log-msgs dict --broker $brokerUrl --address $address --count 1 --content-type int --msg-content 1234""".split(
                    " "
                ).toTypedArray()
            val receiverParameters =
                "receiver --log-msgs dict --broker $brokerUrl --address $address --count 1".split(" ").toTypedArray()

            print("Sending: ")
            main(senderParameters)
            print("Receiving: ")
            main(receiverParameters)
        }
    }

    @Tag("external")
    @Test
    fun sendAndReceiveMessageFromFile() {
        val file = File.createTempFile(address, null)
        try {
            file.writeText("aContent")
            val senderParameters =
                "sender --log-msgs dict --broker $brokerUrl --address $address --count 1 --msg-content-from-file $file".split(
                    " "
                ).toTypedArray()
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

    @Tag("external")
    @Test
    fun sendAndReceiveBinaryMessageFromFile() {
        val file = File.createTempFile(address, null)
        try {
            file.writeText("aContent")
            val senderParameters =
                "sender --log-msgs dict --broker $brokerUrl --address $address --count 1 --msg-content-from-file $file --msg-content-binary true".split(
                    " "
                ).toTypedArray()
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

    @Tag("external")
    @Test
    fun sendMessageFromNonexistentFile() {
        assertSystemExit(2, Executable {
            val file = "noSuchFile"
            val senderParameters =
                "sender --log-msgs dict --broker $brokerUrl --address $address --count 1 --msg-content-from-file $file".split(
                    " "
                ).toTypedArray()
            main(senderParameters)
        })
    }

    @Tag("external")
    @Test
    fun sendBinaryMessageFromNonexistentFile() {
        assertSystemExit(2, Executable {
            val file = "noSuchFile"
            val senderParameters =
                "sender --log-msgs dict --broker $brokerUrl --address $address --count 1 --msg-content-from-file $file --msg-content-binary true".split(
                    " "
                ).toTypedArray()
            main(senderParameters)
        })
    }

    @Tag("external")
    @Test
    fun sendAndReceiveTextMessageFromToFile() {
        val file = File.createTempFile(address, "input")
        val outputDirectory = Files.createTempDirectory(address)
        val output = outputDirectory.resolve("message")
        val output0 = outputDirectory.resolve("message_0")
        try {
            file.writeText("aContent")
            val senderParameters =
                "sender --log-msgs dict --broker $brokerUrl --address $address --count 1 --msg-content-from-file $file".split(
                    " "
                ).toTypedArray()
            val receiverParameters =
                "receiver --log-msgs dict --broker $brokerUrl --address $address --count 1 --msg-content-to-file $output".split(
                    " "
                ).toTypedArray()

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

    @Tag("external")
    @Test
    fun sendAndReceiveMessageToTopic() {
        val senderParameters =
            "sender --log-msgs dict --broker $brokerUrl --address topic://$address --count 1".split(" ").toTypedArray()
        val receiverParameters =
            "receiver --log-msgs dict --broker $brokerUrl --address topic://$address --count 1".split(" ")
                .toTypedArray()

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

    @Tag("external")
    @Test
    open fun sendSingleMessageAllTrustingTls() {
        assertNoSystemExit {
            //        "sender --log-msgs dict --broker tcp://127.0.0.1:61617 --address lalaLand --count 1 --conn-ssl-truststore-location  --conn-ssl-truststore-password secureexample --conn-ssl-keystore-location /home/jdanek/Downloads/AMQ7/amq7cr1i0/ikeysiold/client-side-keystore.jks --conn-ssl-keystore-password secureexample".split(" ").toTypedArray()
            val senderParameters =
                "sender --log-msgs dict --broker $sslBrokerUrl --address $address --conn-ssl-verify-host false --conn-ssl-trust-all true --count 1".split(
                    " "
                ).toTypedArray()
            print("Sending: ")
            main(senderParameters)
        }
    }

    @Tag("external")
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

    @Tag("external")
    @Test
    fun `send and receive with --msg-content-hashed option`() {
        val content = "aContent\n"  // c.f. sha1sum <<<aContent
        val md = MessageDigest.getInstance("SHA-1")
        val expected = BigInteger(1, md.digest(content.toByteArray())).toString(16)
        print(expected)
        assertNoSystemExit {
            val sent = main(
                arrayOf(
                    "sender",
                    "--log-msgs",
                    "dict",
                    "--broker",
                    brokerUrl,
                    "--address",
                    address,
                    "--count",
                    "1",
                    "--msg-content",
                    content,
                    "--msg-content-hashed"
                )
            )
            val received = main(
                arrayOf(
                    "receiver",
                    "--log-msgs",
                    "dict",
                    "--broker",
                    brokerUrl,
                    "--address",
                    address,
                    "--count",
                    "1",
                    "--msg-content-hashed"
                )
            )

            assertThat(sent.map { it["content"] as String }).containsExactlyElementsIn(listOf(expected))
            assertThat(received.map { it["content"] as String }).containsExactlyElementsIn(listOf(expected))
        }
    }

    @Tag("external")
    @Test
    fun testSendingReceivingWithDuration() {
        assertNoSystemExit {
            val senderParameters =
                "sender --log-msgs dict --broker $brokerUrl --address $address --count=5 --duration=5".split(" ").toTypedArray()
            val receiverParameters =
                "receiver --log-msgs dict --broker $brokerUrl --address $address --count=5 --duration=5".split(" ").toTypedArray()
            print("Sending:\n ")
            main(senderParameters)
            print("Receiving:\n ")
            main(receiverParameters)
        }
    }

    //    void testMessageContentListItem() {
    //        '--msg-content-list-item', '', '--msg-content-list-item', 'String', '--msg-content-list-item', '~1', '--msg-content-list-item', '~1.0', '--msg-content-list-item', '1', '--msg-content-list-item', '1.0', '--msg-content-list-item', '~-1', '--msg-content-list-item', '~-1.3', '--msg-content-list-item', '-1', '--msg-content-list-item', '~~1'
    //    }

    @Tag("external")
    // @SetEnvironmentVariable(key = "PN_TRACE_FRM", value = "true")
    @Test
    @Throws(
        Throwable::class
    )
    open fun testDurableSubscriber() {

        // tests.JAMQNode000Tests.JAMQNodeTests.test_node_durable_topic_subscriber
        // topic: prefix, --conn-clientid --durable-subscriber --durable-subscriber-name
        println()
        println("Create subscriber")
        println()
        main(
            ("receiver --timeout 5 --log-msgs dict --broker " + brokerUrl + " --conn-auth-mechanisms PLAIN --conn-username admin --conn-password admin" +
                " --address topic://test_node_durable_topic_subscriber --count 0 --durable-subscriber True" +
                " --conn-clientid cliId0 --durable-subscriber-name ds0").split(" ").toTypedArray()
        )

        println()
        println("Send message")
        println()

        main(
            ("sender --timeout 5 --log-msgs dict --broker " + brokerUrl + " --conn-auth-mechanisms PLAIN --conn-username admin --conn-password admin" +
                " --address topic://test_node_durable_topic_subscriber --count 1").split(" ").toTypedArray()
        )

        println()
        println("Recover subscription, receive message")
        println()

        main(
            ("receiver --timeout 5 --log-msgs dict --broker " + brokerUrl + " --conn-auth-mechanisms PLAIN --conn-username admin --conn-password admin" +
                " --address topic://test_node_durable_topic_subscriber --count 0 --durable-subscriber True" +
                " --conn-clientid cliId0 --durable-subscriber-name ds0").split(" ").toTypedArray()
        )

        println()
        println("Unsubscribe")
        println()

        // --subscriber-unsubscribe True
        main(("receiver --timeout 5 --log-msgs dict --broker " + brokerUrl + " --conn-auth-mechanisms PLAIN --conn-username admin --conn-password admin" +
            " --address topic://test_node_durable_topic_subscriber --count 0" +
            " --subscriber-unsubscribe True" +
            " --conn-clientid cliId0 --durable-subscriber-name ds0").split(" ").toTypedArray()
        )

        println()
        println("Send message again")
        println()

        main(
            ("sender --timeout 5 --log-msgs dict --broker " + brokerUrl + " --conn-auth-mechanisms PLAIN --conn-username admin --conn-password admin" +
                " --address topic://test_node_durable_topic_subscriber --count 1").split(" ").toTypedArray()
        )

        println()
        println("Try receiving, message should not be there")
        println()

        val rcv2 = main(
            ("receiver --timeout 5 --log-msgs dict --broker " + brokerUrl + " --conn-auth-mechanisms PLAIN --conn-username admin --conn-password admin" +
                " --address topic://test_node_durable_topic_subscriber --count 0 --durable-subscriber True" +
                " --conn-clientid cliId0 --durable-subscriber-name ds0").split(" ").toTypedArray()
        )
        assertThat(rcv2).hasSize(0)

        println()
        println("Unsubscribe again")
        println()

        // --subscriber-unsubscribe True
        main(("receiver --timeout 5 --log-msgs dict --broker " + brokerUrl + " --conn-auth-mechanisms PLAIN --conn-username admin --conn-password admin" +
            " --address topic://test_node_durable_topic_subscriber --count 0" +
            " --subscriber-unsubscribe True" +
            " --conn-clientid cliId0 --durable-subscriber-name ds0").split(" ").toTypedArray()
        )
    }

    /**
     * Sends sufficient amount of messages to cause broker to block the sender.
     * Then it runs a receiver and empties the queue, unblocking the queue, and receiving all messages eventually.
     *
     * See dTests test JAMQNode000Tests/test_broker_blocks_client_many_messages_in_queue
     */
    @Test
    @ExtendWith(BrokerFixture::class)
    fun testQueueBlockUnblockSenderResumes(@BrokerFixture.TempBroker broker: Broker, @TempDir tempDir: Path) {
        // this does not work with qpid-jms, and we skip the test in dtests anyways
        TruthJUnit.assume().that(this::class.java.name).endsWith("ProtonJ2MainTest")

        val address = "test_broker_blocks_client_many_messages_in_queue"

        broker.configuration.isSecurityEnabled = false
        broker.configuration.bindingsDirectory = tempDir.resolve("data/bindings").toString()
        broker.configuration.journalDirectory = tempDir.resolve("data/journal").toString()
        broker.configuration.largeMessagesDirectory = tempDir.resolve("data/large").toString()
        broker.configuration.pagingDirectory = tempDir.resolve("data/paging").toString()
        val addressSettings = AddressSettings()
            .setMaxSizeBytes(10240)
            .setMaxSizeBytesRejectThreshold(10240)
            .setAddressFullMessagePolicy(AddressFullMessagePolicy.BLOCK)
        broker.configuration.addAddressSetting(address, addressSettings)
        broker.startBroker()

        val brokerUrl = "localhost:" + broker.addAMQPAcceptor()

        val pool = Executors.newCachedThreadPool()

        val senderMessages = Collections.synchronizedList<Map<String, Any>>(java.util.ArrayList())
        val senderFuture = pool.submit(Callable<List<Map<String, Any>>> {
            main(
                arrayOf(
                    "sender",
                    "--timeout=60",
                    "--log-msgs=dict",
                    "--msg-content-hashed=true",
                    "--broker=$brokerUrl",
                    "--conn-auth-mechanisms=PLAIN",
                    "--conn-username=admin",
                    "--conn-password=admin",
                    "--address=$address",
                    "--count=300",
                    "--msg-content=abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789abcd"
                ),
                senderMessages
            )
        })

        val addressControl = broker.makeAddressControl(address)
        var previousCount: Long = -1
        await().atMost(5, TimeUnit.SECONDS).ignoreException(UndeclaredThrowableException::class.java).until {
            previousCount = addressControl.messageCount
            true
        }

        val initialTime = Instant.now()

        var gotBlocked = false
        while (Duration.between(initialTime, Instant.now()).toSeconds() < 10) {
            TimeUnit.SECONDS.sleep(2)
            if (addressControl.messageCount > 0
                && addressControl.messageCount == previousCount
                && addressControl.messageCount < 300
            ) {
                gotBlocked = true
                break
            }
            previousCount = addressControl.messageCount
        }
        Truth.assertWithMessage("Sender should get blocked, $initialTime, ${Instant.now()}, $previousCount, ${senderMessages.size}")
            .that(gotBlocked).isTrue()
        println("Sender got blocked, $initialTime, ${Instant.now()}, $previousCount, ${senderMessages.size}")

        val receivedMessages = main(
            arrayOf(
                "receiver",
                "--timeout=60",
                "--log-msgs=dict",
                "--msg-content-hashed=true",
                "--broker=$brokerUrl",
                "--conn-auth-mechanisms=PLAIN",
                "--conn-username=admin",
                "--conn-password=admin",
                "--address=$address",
                "--count=300",
            )
        )

        assertThat(receivedMessages).hasSize(300)

        senderFuture.get()
        assertThat(senderMessages).hasSize(300)
    }
}
