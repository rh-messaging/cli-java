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
import org.junit.jupiter.api.BeforeEach
import java.util.stream.Stream
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import kotlin.reflect.full.companionObject


class InteropTest : AbstractTest() {
    override val prefix: String = "interopTestAddress"

    /**
     * Note: accesses the method name of its caller
     */
    fun address(sender: String, receiver: String): String {
        val testMethod = Throwable().stackTrace[1].methodName.replace(" ", "_")
        return "${prefix}-${testMethod}-${sender}-${receiver}-$randomSuffix"
    }

    @BeforeEach
    fun setUp() {
        randomSuffix = generateRandomSuffix()
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @MethodSource("clientCombinationsProvider")
    fun `send browse receive empty message`(senderName: String, receiverName: String) {
        val address = address(senderName, receiverName)
        val s = createClient(senderName, "sender", "-b 127.0.0.1:61616 -a $address --log-msgs dict".split(" "))
        val b = createClient(receiverName, "browser", "-b 127.0.0.1:61616 -a $address --log-msgs dict".split(" "))
        val r = createClient(receiverName, "receiver", "-b 127.0.0.1:61616 -a $address --log-msgs dict".split(" "))
        s.run()
        b.run()
        r.run()
        assertMessagesAreIdentical(address, s, b, r)
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @MethodSource("clientCombinationsProvider")
    fun `send browse receive expired durable message`(senderName: String, receiverName: String) {
        val address = address(senderName, receiverName)
        val artemisExpiryQueue = "ExpiryQueue"

        println("drain artemisExpiryQueue")
        val dr = createClient(receiverName, "receiver", "-b 127.0.0.1:61616 -a $artemisExpiryQueue --log-msgs dict --count 0".split(" "))
        dr.run()

        println("perform test")
        val s = createClient(senderName, "sender", "-b 127.0.0.1:61616 -a $address --log-msgs dict --msg-durable yes --msg-ttl 1".split(" "))
        val b = createClient(receiverName, "browser", "-b 127.0.0.1:61616 -a $artemisExpiryQueue --log-msgs dict --count 1".split(" "))
        // this does not guarantee test isolation
        val r = createClient(receiverName, "receiver", "-b 127.0.0.1:61616 -a $artemisExpiryQueue --log-msgs dict --count 1".split(" "))
        s.run()
        Thread.sleep(50 * 1000) // 25s is not enough for aac -> acc
        b.run()
        r.run()
        assertMessagesAreIdentical(address, s, b, r)
    }

    companion object {
        @JvmStatic
        fun clientCombinationsProvider(): Stream<Arguments> {
            val clients = listOf("aac", "acc", "aoc")
            return clients.flatMap { s -> clients.map { r -> Arguments.of(s, r) } }.stream()
        }
    }

    fun createClient(library: String, type: String, args: List<String>): ClientFormatterSpy {
        val methods = ClientFormatterSpy::class.companionObject!!.members
        val method = methods.first { it.name == "make${library.capitalize()}${type.capitalize()}Client" }
        return method.call(ClientFormatterSpy, args.toTypedArray()) as ClientFormatterSpy
    }

    fun assertMessagesAreIdentical(address: String, sender: ClientFormatterSpy, browser: ClientFormatterSpy, receiver: ClientFormatterSpy) {
        // sanity checks
        for (c in listOf(sender, browser, receiver)) {
            Truth.assertThat(c.messages).hasSize(1)
            c.messages.forEach { Truth.assertThat(it).containsEntry("address", address) }
        }

        compareMessages(browser.messages, sender.messages, skip = setOf("delivery-time"))
        compareMessages(receiver.messages, sender.messages, skip = setOf("delivery-time"))

        sender.messages.forEach { Truth.assertThat(it).containsEntry("properties", emptyMap<Any, Any>()) }
        browser.messages.forEach { Truth.assertThat(it).containsEntry("properties", mapOf("JMSXDeliveryCount" to 0)) }
        receiver.messages.forEach { Truth.assertThat(it).containsEntry("properties", emptyMap<Any, Any>()) }
    }

    /**
     * Skips properties when comparing messages
     */
    fun compareMessages(m1: List<Map<String, Any>>, m2: List<Map<String, Any>>, skip: Set<String> = emptySet()) {
        Truth.assertThat(m1).hasSize(m2.size)
        val set1 = m1.flatMap { it.keys }.toSet()
        val set2 = m2.flatMap { it.keys }.toSet()
        val intersection = set1.union(set2)
        val remainer1 = set1.subtract(intersection)
        val remainer2 = set2.subtract(intersection)
        println("m1 extra properties: $remainer1 m2 extra properties: $remainer2")
        for (m12 in m1.zip(m2)) {
            val m1_l1 = m12.first.filterKeys { intersection.contains(it) && !skip.contains(it) && "properties" != it }
            val m2_l1 = m12.second.filterKeys { intersection.contains(it) && !skip.contains(it) && "properties" != it }
            Truth.assertThat(m1_l1).containsExactlyEntriesIn(m2_l1)
        }
    }
}
