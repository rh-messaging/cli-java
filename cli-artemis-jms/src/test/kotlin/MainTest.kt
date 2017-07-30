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

import com.redhat.mqe.acc.Main
import org.junit.jupiter.api.Test

class AccMainTest : AbstractMainTest() {

    override val brokerUrl = "tcp://127.0.0.1:61616"

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

    override fun main(args: Array<String>) {
        return Main.main(args)
    }

//    @Test fun sendAndReceiveSingleMessage() {
////        val address = "tcp://127.0.0.1:5445"
//        val address = "tcp://127.0.0.1:61616"
//        val senderParameters =
//                "sender --log-msgs dict --broker $address --address lalaLand --count 1".split(" ").toTypedArray()
//        val receiverParameters =
//                "receiver --log-msgs dict --broker $address --address lalaLand --count 1".split(" ").toTypedArray()
//        print("Sending: ")
//        Main.main(senderParameters)
////        print("Receiving: ")
////        Main.main(receiverParameters)
//    }
//
//    @Test fun sendSingleMessageTls() {
//        val senderParameters =
//                "sender --log-msgs dict --broker tcp://127.0.0.1:61616 --address lalaLand --count 1 --conn-ssl-truststore-location /home/jdanek/Downloads/AMQ7/amq7cr1i0/ikeysiold/client-side-truststore.jks --conn-ssl-truststore-password secureexample --conn-ssl-keystore-location /home/jdanek/Downloads/AMQ7/amq7cr1i0/ikeysiold/client-side-keystore.jks --conn-ssl-keystore-password secureexample".split(" ").toTypedArray()
//        print("Sending: ")
//        Main.main(senderParameters)
//    }
//
//
//    @Test fun sendSingleMessage() {
//        print("Sending: ")
//        Main.main(
//                "sender  --timeout 120 --log-msgs dict --broker tcp://127.0.0.1:61616 --conn-reconnect True --conn-username admin --conn-password admin --address test_client_reconnect_msgsrv_sigkill --count 1000000".split(" ").toTypedArray()
//        )
//    }
//
//    @Test fun reconnect() {
//        val host = "tcp://127.0.0.1:61616"
//        val address = "reconnect"
//        Main.main(
//                "receiver  --timeout 120 --log-msgs dict --broker $host --conn-reconnect True --conn-username admin --conn-password admin --address $address --count 1".split(" ").toTypedArray()
//        )
//    }
//
//    @Test fun sendSingleMessageToTopic() {
//        val senderParameters =
//                ("sender --log-msgs dict --broker tcp://127.0.0.1:61616 --address topic://topic --count 1 ".split(" ").toTypedArray())
//        print("Sending: ")
//        Main.main(senderParameters)
//    }
//
//    @Test fun sendManyMessageToTopicInTxAndRollback() {
//        val senderParameters =
//                ("sender --log-msgs dict --broker tcp://127.0.0.1:61616 --address topic://topic --count 100 --duration 100 --tx-size 100 tx-action rollback".split(" ").toTypedArray())
//        print("Sending: ")
//        Main.main(senderParameters)
//    }
//
//    @Test fun receiveSingleMessage() {
//        val address = "target"
//        val parameters =
//                ("receiver --log-msgs dict --broker tcp://127.0.0.1:61616 --address $address --count 1 ".split(" ").toTypedArray())
//        print("Sending: ")
//        Main.main(parameters)
//    }
}
