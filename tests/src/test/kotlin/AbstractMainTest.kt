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
    override fun checkExit(status: Int) {
        throw SystemExitingWithStatus(status)
    }

    override fun checkPermission(perm: Permission?) {
    }
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

    val prefix: String = "lalaLand_"
    lateinit var randomSuffix: String
    val address: String
        get() = prefix + randomSuffix

    val random = Random()

    abstract fun main(args: Array<String>)

    @BeforeEach fun setup() {
        print(LocalTime.now())
        // https://stackoverflow.com/questions/41107/how-to-generate-a-random-alpha-numeric-string
        randomSuffix = BigInteger(130, random).toString(32)
    }

    @ParameterizedTest
    @ValueSource(strings = arrayOf("sender", "receiver", "connector"))
        //    @ExtendWith(ExpectedSystemExit::class)
    fun printHelp(client: String) {
        val parameters =
            "$client --help".split(" ").toTypedArray()
        val previousManager = System.getSecurityManager()
        try {
            val manager = NoExitSecurityManager(previousManager)
            System.setSecurityManager(manager);
            main(parameters)
            fail("expected exception")
        } catch (e: SystemExitingWithStatus) {
            assertThat(e.status).isEqualTo(0)
        } finally {
            System.setSecurityManager(previousManager)
        }
    }

    @Test fun sendAndReceiveSingleMessage() {
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

    @Test fun sendBrowseAndReceiveSingleMessage() {
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

    @Test fun connectConnector() {
        val connectorParameters =
            "connector --broker $brokerUrl --address $address --count 1".split(" ").toTypedArray()
        assertTimeoutPreemptively(Duration.ofSeconds(10)) {
            print("Connecting: ")
            main(connectorParameters)
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
//
//        --conn-max-inactivity-dur-init-delay 1000

//        --msg-content-binary true
//        works with --from file, not otherwise

//        println(senderAdditionalOptions.joinToString(separator = ", "))

//        string and int, in model
//        --content-type aContentType


//        the content type thing and the id must start with ID: shows only with the dynamic options
        assertNoSystemExit {
            print("Sending: ")
            main(senderParameters + senderDynamicOptions.split(" ").toTypedArray() + senderAdditionalOptions)
            print("Receiving: ")
            main(receiverParameters)
        }

        /**
         * --msg-content-from-file mytempfile




        TODO: all pairs for connection options and for jms features would be actually quite useful, I think
        this should be somehow autogenerated, have one source of truth for our clients and for pict; ok values, ok corner cases, bad corner cases
        (so that one can exclude corner cases for faster test run)




        --conn-ssl-context-proto <PROTOCOL>      protocol argument used when getting an
        SSLContext (default: TLS)
        --conn-ssl-dis-ciphered-suites <SUITES>  disabled cipher suites (comma
        separated list)
        --conn-ssl-dis-protos <PROTOCOLS>        disabled protocols (comma separated
        list) (default: SSLv2Hello,SSLv3)
        --conn-ssl-ena-ciphered-suites <SUITES>  enabled cipher suites (comma separated
        list); disabled ciphers are removed
        from this list.
        --conn-ssl-ena-protos <PROTOCOLS>        enabled protocols (comma separated
        list). No default, meaning the
        context default protocols are used
        --conn-ssl-key-alias <ALIAS>             alias to use when selecting a keypair
        from the keystore if required to
        send a client certificate to the
        server
        --conn-ssl-keystore-location <LOC>       default is to read from the system
        property "javax.net.ssl.keyStore"
        --conn-ssl-keystore-password <PASS>      default is to read from the system
        property "javax.net.ssl.
        keyStorePassword"
        --conn-ssl-store-type <TYPE>             store type (default: JKS)
        --conn-ssl-trust-all <ENABLED>           (default: false)
        --conn-ssl-truststore-location <LOC>     default is to read from the system
        property "javax.net.ssl.trustStore"
        --conn-ssl-truststore-password <PASS>    default is to read from the system
        property "javax.net.ssl.
        keyStorePassword"
        --conn-ssl-verify-host <ENABLED>         (default: true)




        --conn-username
        --conn-vhost



        --log-stats <LEVEL>                      report various statistic/debug
        information (default: INFO)






        --conn-password
         */
    }


//    @Test fun sendAndReceiveListMessage() {
//        val senderParameters =
//                """sender --log-msgs dict --broker amqp://127.0.0.1:5672 --address lalaLand --count 1 --msg-content-list-item --msg-content-list-item "String" --msg-content-list-item "~1" --msg-content-list-item "~1.0" --msg-content-list-item "1" --msg-content-list-item "1.0" --msg-content-list-item "~-1" --msg-content-list-item "~-1.3" --msg-content-list-item "-1" --msg-content-list-item "~~1"""".split(" ").toTypedArray()
//
//        val receiverParameters =
//                "receiver --log-msgs dict --broker amqp://127.0.0.1:5672 --address lalaLand --count 1".split(" ").toTypedArray()
//        print("Sending: ")
//        main(senderParameters)
//        print("Receiving: ")
//        main(receiverParameters)
//    }
//
//        @Test fun sendAndReceiveSingleMessageSsl() {
//        val senderParameters =
//                "sender --log-msgs dict --broker amqps://127.0.0.1:61616 --address lalaLand --count 1 --conn-ssl-truststore-location /home/jdanek/Downloads/AMQ7/amq7cr1i0/redhatqe.truststore --conn-ssl-truststore-password password --conn-ssl-keystore-location /home/jdanek/Downloads/AMQ7/amq7cr1i0/client.keystore --conn-ssl-keystore-password password".split(" ").toTypedArray()
//        val receiverParameters =
//                "receiver --log-msgs dict --broker amqp://127.0.0.1:5672 --address lalaLand --count 1".split(" ").toTypedArray()
//        print("Sending: ")
//        main(senderParameters)
////        print("Receiving: ")
////        Main.main(receiverParameters)
//    }
//
//    @Test fun reconnect() {
//        val host = "amqp://127.0.0.1:61616"
//        val address = "reconnect"
//        main(
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
//    @Test fun sendSingleMessageToTopic() {
//        val senderParameters =
//                ("sender --log-msgs dict --broker amqp://127.0.0.1:5672 --address topic://topic --count 1 ".split(" ").toTypedArray())
//        print("Sending: ")
//        Main.main(senderParameters)
//    }
//
//    @Test fun sendSingleMessageToTopicAndRollback() {
//        val senderParameters =
//                ("sender --log-msgs dict --broker amqp://127.0.0.1:5672 --address topic://topic --count 1 --tx-action rollback tx-endloop-action rollback".split(" ").toTypedArray())
//        print("Sending: ")
//        Main.main(senderParameters)
//    }
//
//    @Test fun sendSingleMessageToQueue() {
//        val senderParameters =
//                ("sender --log-msgs dict --broker amqp://127.0.0.1:5672 --address inQueue --count 1 ".split(" ").toTypedArray())
//        print("Sending: ")
//        Main.main(senderParameters)
//    }
//
//    @Test fun receiveSingleMessageToQueue() {
//        val senderParameters =
//                ("receiver --log-msgs dict --broker amqp://127.0.0.1:5672 --address outQueue --count 1 ".split(" ").toTypedArray())
//        print("Sending: ")
//        Main.main(senderParameters)
//    }
//
//    @Test fun `print sender help`() {
//        Main.main("sender --help".split(" ").toTypedArray())
//    }
//
//    @Test fun receiveSingleMessage() {
//        val address = "target"
//        val parameters =
//                ("receiver --log-msgs dict --broker amqp://127.0.0.1:5672 --address $address --count 1 ".split(" ").toTypedArray())
//        print("Sending: ")
//        Main.main(parameters)
//    }
}
