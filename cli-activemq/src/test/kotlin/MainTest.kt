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

import com.redhat.mqe.aoc.Main

class AocMainTest : AbstractMainTest() {

    override val brokerUrl = "tcp://127.0.0.1:61616"

    override val senderAdditionalOptions =
        //--conn-tcp-tr//"""
//        --conn-prefix-packet-size-ena false
// affic-class 0
//--conn-reconnect true
//--conn-reconnect-backoff false
//--conn-reconnect-backoff-multiplier 1
//--conn-reconnect-initial-delay 1
//--conn-reconnect-interval 1000
//--conn-reconnect-limit 1000
//--conn-reconnect-start-limit 1000
//--conn-reconnect-warn-attempts 1
//--conn-reconnect-timeout 1000

//        """
        """
--capacity 1
--conn-async-send true
--conn-cache-ena false
--conn-cache-size 1
--conn-clientid aClientId
--conn-close-timeout 1000
--conn-heartbeat 1000
--conn-max-frame-size 4096
--conn-prefetch 1
--conn-prefetch-browser 1
--conn-prefetch-queue 1
--conn-prefetch-topic 1
--conn-prefetch-topic-dur 1
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
--conn-tight-encoding-ena false
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

    @Test fun triggerFail() {
        val senderArguments = "sender --broker $brokerUrl --address $address".split(" ").toTypedArray() + "--tx-endloop-action recover --tx-action None --sync-mode none --ssn-ack-mode client --msg-id ID:aMsgId --log-lib error --duration-mode before-send --msg-property key~42 --msg-content-map-item key~42 --log-msgs body".split(" ").toTypedArray() + senderAdditionalOptions

        val receiverParameters =
            "receiver --log-msgs dict --broker $brokerUrl --address $address --count 1".split(" ").toTypedArray()

        main(senderArguments)
        main(receiverParameters)
    }

}
//

//class AocMainTest {
//  class AccMainTest : AbstractMainTest() {
//
//    override val brokerUrl = "tcp://127.0.0.1:61616"
//
//    override fun main(args: Array<String>) {
//        return Main.main(args)
//    }uu  @Test fun sendAndReceiveSingleMessage() {
//        val senderParameters =
//                "sender --log-msgs dict --broker tcp://127.0.0.1:61616 --address lalaLand --count 1".split(" ").toTypedArray()
//        val receiverParameters =
//                "receiver --log-msgs dict --broker tcp://127.0.0.1:61616 --address lalaLand --count 1".split(" ").toTypedArray()
//        print("Sending: ")
//        Main.main(senderParameters)
//        print("Receiving: ")
//        Main.main(receiverParameters)
//    }
//
//    @Test fun sendSingleMessageTls() {
//        System.setProperty("javax.net.debug", "all")
//        val senderParameters =
//                "sender --log-msgs dict --broker ssl://127.0.0.1:61616 --address lalaLand --conn-ssl-keystore-password secureexample --count 1 --conn-ssl-truststore-location /home/jdanek/Downloads/AMQ7/amq7cr1i0/ikeysiold/client-side-truststore.jks --conn-ssl-truststore-password secureexample --conn-ssl-keystore-location /home/jdanek/Downloads/AMQ7/amq7cr1i0/ikeysiold/client-side-keystore.jks".split(" ").toTypedArray()
////                "sender --log-msgs dict --broker ssl://127.0.0.1:61616 --address lalaLand --count 1 --conn-ssl-truststore-location /home/jdanek/Downloads/AMQ7/amq7cr1i0/redhatqe.truststore --conn-ssl-truststore-password password --conn-ssl-keystore-location /home/jdanek/Downloads/AMQ7/amq7cr1i0/client.keystore --conn-ssl-keystore-password password".split(" ").toTypedArray()
//        print(senderParameters.joinToString(separator = ", "))
//        print("Sending: ")
//        Main.main(senderParameters)
//    }
//
//    @Test fun `--conn-ssl-keystore-password,`() {
//        CoreClient.setClientType(CoreClient.CORE_CLIENT_TYPE)
//        val clientOptionManager = AocClientOptionManager()
//        val options = SenderOptions()
//        val args = "sender --log-msgs dict --broker ssl://127.0.0.1:61616 --address lalaLand --count 1 --conn-ssl-truststore-location /home/jdanek/Downloads/AMQ7/amq7cr1i0/ikeysiold/client-side-truststore.jks --conn-ssl-truststore-password secureexample --conn-ssl-keystore-location /home/jdanek/Downloads/AMQ7/amq7cr1i0/ikeysiold/client-side-keystore.jks --conn-ssl-keystore-password secureexamples".split(" ").toTypedArray()
//        clientOptionManager.applyClientArguments(options, args)
//        print(options.getOption(ClientOptions.CON_SSL_KEYSTORE_PASS).value)
////        Truth.assertThat()
//    }
//
//    @Test fun sendSingleMessage() {
//        val senderParameters =
//                ("sender --log-msgs dict --broker tcp://127.0.0.1:61616 --address topic://topic --count 1 ".split(" ").toTypedArray())
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
//}
