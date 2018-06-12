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

import com.redhat.mqe.ClientListener
import com.redhat.mqe.jms.Main
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertTimeoutPreemptively
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.function.Executable
import java.time.Duration

class AacMainTest : AbstractMainTest() {

    override val brokerUrl = "amqp://127.0.0.1:5672"
    override val sslBrokerUrl = "amqps://127.0.0.1:5673"

    override val senderAdditionalOptions = """
--conn-tcp-sock-linger 1000
--capacity 1
--conn-async-send true
--conn-auth-mechanisms anonymous
--conn-auth-sasl false
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
--conn-tcp-buf-size-recv 1
--conn-tcp-buf-size-send 1
--conn-tcp-conn-timeout 1000
--conn-tcp-keep-alive true
--conn-tcp-no-delay false
--conn-tcp-sock-timeout 1000
--conn-tcp-traffic-class 1
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
--conn-async-send true
--conn-auth-mechanisms anonymous
--conn-auth-sasl false
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
--conn-tcp-buf-size-recv 1
--conn-tcp-buf-size-send 1
--conn-tcp-conn-timeout 1000
--conn-tcp-keep-alive true
--conn-tcp-no-delay false
--conn-tcp-sock-timeout 1000
--conn-tcp-traffic-class 1
--conn-topic-prefix aTopicPrefix
--conn-valid-prop-names false
""".split(" ", "\n").toTypedArray()

    override fun main_(listener: ClientListener, args: Array<String>) = Main.main(listener, args)

    /**
     * transport.logBytes option in qpid-jms
     */
    @Test
    fun sendAndReceiveSingleMessageLogBytes() {
        val senderParameters =
            "sender --log-msgs dict --out json --broker $brokerUrl --address $address --count 1 --log-bytes".split(" ").toTypedArray()
        val receiverParameters =
            "receiver --log-msgs dict --out json --broker $brokerUrl --address $address --count 1 --log-bytes".split(" ").toTypedArray()
        Assertions.assertTimeoutPreemptively(Duration.ofSeconds(10)) {
            print("Sending: ")
            main(senderParameters)
            print("Receiving: ")
            main(receiverParameters)
        }
    }

    // https://issues.apache.org/jira/browse/QPIDJMS-342
    @Tag("issue")
    @Test
    fun attemptConnectingToWrongPortWithTimeout() {
        val senderParameters =
            ("connector" +
                " --broker-uri failover:(amqp://127.0.0.1:1883)?jms.connectTimeout=1&failover.maxReconnectAttempts=1&failover.startupMaxReconnectAttempts=1" +
                ""
                ).split(" ").toTypedArray()
        assertTimeoutPreemptively(Duration.ofSeconds(5)) {
            assertSystemExit(1, Executable {
                main(senderParameters)
            })
        }
    }
}

//        @Test fun sendAndReceiveSingleMessageSsl() {
//        val senderParameters =
//                "sender --log-msgs dict --broker amqps://127.0.0.1:61616 --address lalaLand --count 1 --conn-ssl-truststore-location /home/jdanek/Downloads/AMQ7/amq7cr1i0/redhatqe.truststore --conn-ssl-truststore-password password --conn-ssl-keystore-location /home/jdanek/Downloads/AMQ7/amq7cr1i0/client.keystore --conn-ssl-keystore-password password".split(" ").toTypedArray()
//        val receiverParameters =
//                "receiver --log-msgs dict --broker amqp://127.0.0.1:5672 --address lalaLand --count 1".split(" ").toTypedArray()
//        print("Sending: ")
//        Main.main(senderParameters)
////        print("Receiving: ")
////        Main.main(receiverParameters)
//    }
//
//    @Test fun reconnect() {
//        val host = "amqp://127.0.0.1:61616"
//        val address = "reconnect"
//        Main.main(
//                "receiver  --timeout 120 --log-msgs dict --broker $host --conn-reconnect True --conn-username admin --conn-password admin --address $address --count 1".split(" ").toTypedArray()
//        )
//    }
//
//    @Test fun isAddressMulticast() {
////        val host = "amqp://172.17.0.4:5672 --conn-username a --conn-password b"
//        val host = "amqp://172.17.0.4:5672"
//        val address = "multicast"
//
//        val r1 = Thread {
//            Main.main(
//                    "receiver --log-msgs dict --broker $host --address $address --count 2 --timeout 10".split(" ").toTypedArray()
//            )
//        }
//
//        val r2 = Thread {
//            Main.main(
//                    "receiver --log-msgs dict --broker $host --address $address --count 2 --timeout 10".split(" ").toTypedArray()
//            )
//
//        }
//
//        val s1 = Thread {
//            Main.main(
//                    "sender --log-msgs dict --broker $host --address $address --count 2 --msg-content AMQP%d".split(" ").toTypedArray()
//            )
//        }
//
//        r1.start()
//        r2.start()
//        Thread.sleep(1 * 1000)
//        s1.start()
//
//        s1.join()
//        r1.join()
//        r2.join()
//    }
//
//    @Test fun sendSingleMessageToTopicAndRollback() {
//        val senderParameters =
//                ("sender --log-msgs dict --broker amqp://127.0.0.1:5672 --address topic://topic --count 1 --tx-action rollback tx-endloop-action rollback".split(" ").toTypedArray())
//        print("Sending: ")
//        Main.main(senderParameters)
//    }
//}
