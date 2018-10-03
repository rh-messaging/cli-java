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

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Tags
import org.junit.jupiter.api.Test
import java.math.BigInteger
import java.util.*
import javax.jms.Connection
import javax.jms.ConnectionFactory
import javax.jms.Session

@Tags(Tag("issue"), Tag("external"))
class QPIDJMS357Test {
    val prefix: String = "QPIDJMS357Test_"
    lateinit var randomSuffix: String
    val address: String
        get() = prefix + randomSuffix

    val random = Random()

    @BeforeEach
    fun setUp() {
        // https://stackoverflow.com/questions/41107/how-to-generate-a-random-alpha-numeric-string
        randomSuffix = BigInteger(130, random).toString(32)
    }

    class Client {
        val INDIVIDUAL_ACKNOWLEDGE = 101

        lateinit var f: ConnectionFactory
        lateinit var c: Connection
        lateinit var s: Session

        fun start() {
            f = org.apache.qpid.jms.JmsConnectionFactory(
                "amqp://127.0.0.1:5672")
            c = f.createConnection()
            c.start()
            s = c.createSession(false, INDIVIDUAL_ACKNOWLEDGE)
        }

        fun stop() {
            s.close()
            c.stop()
            c.close()
        }
    }

    @Test
    fun `acknowledge can be switched to individual mode`() {
        val client1 = Client()
        client1.start()
        client1.apply {
            val queue = s.createQueue(address)
            println(queue)
            val producer = s.createProducer(queue)
            for (i in 1..3) {
                println(i)
                val m = s.createMessage()
                m.setIntProperty("i", i)
                producer.send(m)
            }
        }
        client1.stop()

        Thread.sleep(1000)

        val client2 = Client()
        client2.start()
        client2.apply {
            val queue = s.createQueue(address)
            println(queue)
            // leave first message unacknowledged, do not receive third
            val consumer1 = s.createConsumer(queue)
            val m11 = consumer1.receive(2000)
            val m12 = consumer1.receive(2000)
            s.setMessageListener { }
            m12.acknowledge()
            consumer1.close()
        }
        client2.stop()

        val client3 = Client()
        client3.start()
        client3.apply {
            val queue = s.createQueue(address)
            // now receive both the first and third message
            val consumer2 = s.createConsumer(queue)
            val m21 = consumer2.receive(2000)
            val m23 = consumer2.receive(2000)
            m21.acknowledge()
            m23.acknowledge()
            consumer2.close()

            assertThat(m21.getIntProperty("i")).isEqualTo(1)
            assertThat(m23.getIntProperty("i")).isEqualTo(3)
        }
        client3.stop()
    }
}
