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

package com.redhat.mqe

import AbstractMainTest
import com.google.common.truth.Truth.assertThat
import com.redhat.mqe.lib.MessageFormatter
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import picocli.CommandLine
import java.io.File
import java.nio.file.Files

class ProtonJ2ClientListener(private val clientListener: ClientListener) : ProtonJ2MessageFormatter() {
    override fun printMessageAsPython(format: MutableMap<String, Any>?) {
        clientListener.onMessage(format)
        super.printMessageAsPython(format)
    }
}

@Disabled("fails")
@Tag("external")
class ProtonJ2MainTest : AbstractMainTest() {

    override val brokerUrl = "amqp://127.0.0.1:61616"
    override val sslBrokerUrl = "amqps://127.0.0.1:61617"

    override val senderAdditionalOptions = """
--capacity 1
--conn-async-acks true
--conn-async-send true
--conn-auth-mechanisms anonymous
--conn-auth-sasl false
--conn-cache-ena false
--conn-cache-size 1
--conn-clientid aClientId
--conn-clientid-prefix aClientIdPrefix
--conn-close-timeout 1000
--conn-conn-timeout 1000
--conn-connid-prefix aConnIdPrefix
--conn-heartbeat 1000
--conn-local-msg-priority true
--conn-max-frame-size 4096
--conn-prefetch 1
--conn-prefetch-browser 1
--conn-prefetch-queue 1
--conn-prefetch-topic 1
--conn-prefetch-topic-dur 1
--conn-prefix-packet-size-ena false
--conn-queue-prefix aQueuePrefix
--conn-reconnect true
--conn-reconnect-backoff false
--conn-reconnect-backoff-multiplier 1
--conn-reconnect-initial-delay 1
--conn-reconnect-interval 1000
--conn-reconnect-limit 1000
--conn-reconnect-start-limit 1000
--conn-reconnect-timeout 1000
--conn-reconnect-warn-attempts 1
--conn-redeliveries-max 1
--conn-server-stack-trace-ena false
--conn-sync-send true
--conn-tcp-buf-size-recv 1
--conn-tcp-buf-size-send 1
--conn-tcp-conn-timeout 1000
--conn-tcp-keep-alive true
--conn-tcp-no-delay false
--conn-tcp-sock-linger 1000
--conn-tcp-sock-timeout 1000
--conn-tcp-traffic-class 1
--conn-tight-encoding-ena false
--conn-topic-prefix aTopicPrefix
--conn-valid-prop-names false
--msg-content-type aMsgContentType
--msg-correlation-id aCorrelationId
--msg-durable false
--msg-group-id aMsgGroupId
--msg-group-seq -1
--msg-no-timestamp true
--msg-priority 1
--timeout 2
--tx-size 1
--msg-reply-to aReplyToQueue
--msg-reply-to-group-id aReplyToGroupId
--msg-subject aMsgSubject
--msg-ttl 10000
--msg-user-id aMsgUserId
--property-type String
""".split(" ", "\n").toTypedArray()

    // cannot set Client ID, because more than one connection is created, and these would clash
//--conn-clientid aClientId
    override val connectorAdditionalOptions = """
--conn-async-acks true
--conn-async-send true
--conn-auth-mechanisms anonymous
--conn-auth-sasl false
--conn-cache-ena false
--conn-cache-size 1
--conn-clientid-prefix aClientIdPrefix
--conn-close-timeout 1000
--conn-conn-timeout 1000
--conn-connid-prefix aConnIdPrefix
--conn-heartbeat 1000
--conn-local-msg-priority true
--conn-max-frame-size 4096
--conn-prefetch 1
--conn-prefetch-browser 1
--conn-prefetch-queue 1
--conn-prefetch-topic 1
--conn-prefetch-topic-dur 1
--conn-prefix-packet-size-ena false
--conn-queue-prefix aQueuePrefix
--conn-reconnect true
--conn-reconnect-backoff false
--conn-reconnect-backoff-multiplier 1
--conn-reconnect-initial-delay 1
--conn-reconnect-interval 1000
--conn-reconnect-limit 1000
--conn-reconnect-start-limit 1000
--conn-reconnect-timeout 1000
--conn-reconnect-warn-attempts 1
--conn-redeliveries-max 1
--conn-server-stack-trace-ena false
--conn-sync-send true
--conn-tcp-buf-size-recv 1
--conn-tcp-buf-size-send 1
--conn-tcp-conn-timeout 1000
--conn-tcp-keep-alive true
--conn-tcp-no-delay false
--conn-tcp-sock-linger 1000
--conn-tcp-sock-timeout 1000
--conn-tcp-traffic-class 1
--conn-tight-encoding-ena false
--conn-topic-prefix aTopicPrefix
--conn-valid-prop-names false
""".split(" ", "\n").toTypedArray()

    override fun main_(listener: ClientListener, args: Array<String>) {
        val protonJ2ClientListener = ProtonJ2ClientListener(listener)
        val main = when (args[0]) {
            "sender" -> CommandLine(CliProtonJ2Sender(protonJ2ClientListener))
            "receiver" -> CommandLine(CliProtonJ2Receiver(protonJ2ClientListener))
            "connector" -> CommandLine(CliProtonJ2Connector())
            else -> throw NotImplementedError(args[0])
        }
        val returnCode = main.execute(*(args.drop(1).toTypedArray()))
    }

    override val prefix: String
        get() = "ProtonJ2MainTest"

    /**
     * Large message streaming from/to java.io.{Input,Output}Stream is artemis-jms-client only
     */
    @Test
    fun sendLargeMessageStreamFile() {
        val file = File.createTempFile(address, null)
        val outputDirectory = Files.createTempDirectory(address)
        val output = outputDirectory.resolve("message")
        val output0 = outputDirectory.resolve("message_0")
        try {
            file.writeText("aContent")
            val senderParameters =
                "sender --log-msgs dict --broker $brokerUrl --address $address --count 1 --msg-content-from-file $file --msg-content-binary true --msg-content-stream true".split(
                    " "
                ).toTypedArray()
            val receiverParameters =
                "receiver --log-msgs dict --broker $brokerUrl --address $address --count 1 --msg-binary-content-to-file $output".split(
                    " "
                ).toTypedArray()

            print("Sending: ")
            main(senderParameters)
            print("Receiving: ")
            main(receiverParameters)

            assertThat(output0.toFile().readBytes()).isEqualTo(file.readBytes())
        } finally {
            file.delete()
            outputDirectory.toFile().deleteRecursively()
        }
    }

    /**
     * Large message streaming from/to java.io.{Input,Output}Stream is artemis-jms-client only
     */
    @Test
    @Disabled("https://github.com/rh-messaging/cli-java/issues/50")
    fun sendAndReceiveLargeMessageStreamFile() {
        val file = File.createTempFile(address, "input")
        val outputDirectory = Files.createTempDirectory(address)
        val output = outputDirectory.resolve("message")
        val output0 = outputDirectory.resolve("message_0")
        try {
            file.writeText("aContent")
            val senderParameters =
                "sender --log-msgs dict --broker $brokerUrl --address $address --count 1 --msg-content-from-file $file --msg-content-binary true --msg-content-stream true".split(
                    " "
                ).toTypedArray()
            val receiverParameters =
                "receiver --log-msgs dict --broker $brokerUrl --address $address --count 1 --msg-binary-content-to-file $output --msg-content-stream true".split(
                    " "
                ).toTypedArray()

            print("Sending: ")
            main(senderParameters)
            print("Receiving: ")
            main(receiverParameters)

            assertThat(output0.toFile().readBytes()).isEqualTo(file.readBytes())
        } finally {
            file.delete()
            outputDirectory.toFile().deleteRecursively()
        }
    }
}
