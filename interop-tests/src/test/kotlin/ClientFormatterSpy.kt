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

import com.redhat.mqe.CliProtonJ2Receiver
import com.redhat.mqe.CliProtonJ2Sender
import com.redhat.mqe.ProtonJ2MessageFormatter
import com.redhat.mqe.acc.AccClientOptionManager
import com.redhat.mqe.acc.AccConnectionManagerFactory
import com.redhat.mqe.acc.AccCoreJmsMessageFormatter
import com.redhat.mqe.aoc.AocClientOptionManager
import com.redhat.mqe.aoc.AocConnectionManagerFactory
import com.redhat.mqe.jms.AacClientOptionManager
import com.redhat.mqe.jms.AacConnectionManagerFactory
import com.redhat.mqe.jms.AacReceiverOptions
import com.redhat.mqe.jms.AacSenderOptions
import com.redhat.mqe.lib.*
import picocli.CommandLine
import javax.jms.Message

interface IClientFormatterSpy {
    val messages: MutableList<Map<String, Any>>
    fun printMessageAsPython(format: MutableMap<String, Any>?)
    fun run()
}

class ClientFormatterSpy(private val formatter: JmsMessageFormatter) : JmsMessageFormatter(), IClientFormatterSpy {
    lateinit var client: CoreClient

    override val messages: MutableList<Map<String, Any>> = ArrayList()

    override fun formatMessage(msg: Message?, hashContent: Boolean): MutableMap<String, Any> =
        formatter.formatMessage(msg, hashContent)

    override fun printMessageAsPython(format: MutableMap<String, Any>?) {
        messages.add(format!!.toMap())
        super.printMessageAsPython(format)
    }

    override fun run() {
        client.startClient()
    }

    companion object {
        //region Qpid Jms

        fun makeAacSenderClient(args: Array<String>): ClientFormatterSpy {
            val connectionManagerFactory = AacConnectionManagerFactory()
            val messageFormatter: JmsMessageFormatter = AMQPJmsMessageFormatter()
            val clientOptionManager: ClientOptionManager = AacClientOptionManager()
            val options = AacSenderOptions()
            clientOptionManager.applyClientArguments(options, args)

            val spy = ClientFormatterSpy(messageFormatter)
            spy.client = SenderClient(connectionManagerFactory, spy, options)
            return spy
        }

        fun makeAacReceiverClient(args: Array<String>): ClientFormatterSpy {
            val connectionManagerFactory = AacConnectionManagerFactory()
            val messageFormatter: JmsMessageFormatter = AMQPJmsMessageFormatter()
            val clientOptionManager: ClientOptionManager = AacClientOptionManager()
            val options = AacReceiverOptions()
            clientOptionManager.applyClientArguments(options, args)

            val spy = ClientFormatterSpy(messageFormatter)
            spy.client = ReceiverClient(connectionManagerFactory, spy, options)
            return spy
        }

        fun makeAacBrowserClient(args: Array<String>): ClientFormatterSpy {
            val connectionManagerFactory = AacConnectionManagerFactory()
            val messageFormatter: JmsMessageFormatter = AMQPJmsMessageFormatter()
            val clientOptionManager: ClientOptionManager = AacClientOptionManager()
            val options = AacReceiverOptions()
            clientOptionManager.applyClientArguments(options, args)

            val spy = ClientFormatterSpy(messageFormatter)
            ReceiverClient(connectionManagerFactory, spy, options)
            spy.client = MessageBrowser(options, connectionManagerFactory, spy)
            return spy
        }
        //endregion

        //region Artemis Core

        fun makeAccSenderClient(args: Array<String>): ClientFormatterSpy {
            val connectionManagerFactory = AccConnectionManagerFactory()
            val messageFormatter: JmsMessageFormatter = AccCoreJmsMessageFormatter()
            val clientOptionManager: ClientOptionManager = AccClientOptionManager()
            val options = SenderOptions()
            clientOptionManager.applyClientArguments(options, args)

            val spy = ClientFormatterSpy(messageFormatter)
            spy.client = SenderClient(connectionManagerFactory, spy, options)
            return spy
        }

        fun makeAccReceiverClient(args: Array<String>): ClientFormatterSpy {
            val connectionManagerFactory = AccConnectionManagerFactory()
            val messageFormatter: JmsMessageFormatter = AccCoreJmsMessageFormatter()
            val clientOptionManager: ClientOptionManager = AccClientOptionManager()
            val options = ReceiverOptions()
            clientOptionManager.applyClientArguments(options, args)

            val spy = ClientFormatterSpy(messageFormatter)
            spy.client = ReceiverClient(connectionManagerFactory, spy, options)
            return spy
        }

        fun makeAccBrowserClient(args: Array<String>): ClientFormatterSpy {
            val connectionManagerFactory = AccConnectionManagerFactory()
            val messageFormatter: JmsMessageFormatter = AccCoreJmsMessageFormatter()
            val clientOptionManager: ClientOptionManager = AccClientOptionManager()
            val options = ReceiverOptions()
            clientOptionManager.applyClientArguments(options, args)

            val spy = ClientFormatterSpy(messageFormatter)
            ReceiverClient(connectionManagerFactory, spy, options)
            spy.client = MessageBrowser(options, connectionManagerFactory, spy)
            return spy
        }
        //endregion

        //region ActiveMQ

        fun makeAocSenderClient(args: Array<String>): ClientFormatterSpy {
            val connectionManagerFactory = AocConnectionManagerFactory()
            val messageFormatter: JmsMessageFormatter = OpenwireJmsMessageFormatter()
            val clientOptionManager: ClientOptionManager = AocClientOptionManager()
            val options = SenderOptions()
            clientOptionManager.applyClientArguments(options, args)

            val spy = ClientFormatterSpy(messageFormatter)
            spy.client = SenderClient(connectionManagerFactory, spy, options)
            return spy
        }

        fun makeAocReceiverClient(args: Array<String>): ClientFormatterSpy {
            val connectionManagerFactory = AocConnectionManagerFactory()
            val messageFormatter: JmsMessageFormatter = OpenwireJmsMessageFormatter()
            val clientOptionManager: ClientOptionManager = AocClientOptionManager()
            val options = ReceiverOptions()
            clientOptionManager.applyClientArguments(options, args)

            val spy = ClientFormatterSpy(messageFormatter)
            spy.client = ReceiverClient(connectionManagerFactory, spy, options)
            return spy
        }

        fun makeAocBrowserClient(args: Array<String>): ClientFormatterSpy {
            val connectionManagerFactory = AocConnectionManagerFactory()
            val messageFormatter: JmsMessageFormatter = OpenwireJmsMessageFormatter()
            val clientOptionManager: ClientOptionManager = AocClientOptionManager()
            val options = ReceiverOptions()
            clientOptionManager.applyClientArguments(options, args)

            val spy = ClientFormatterSpy(messageFormatter)
            ReceiverClient(connectionManagerFactory, spy, options)
            spy.client = MessageBrowser(options, connectionManagerFactory, spy)
            return spy
        }
        //endregion

        //region ProtonJ2

        fun makeProtonj2SenderClient(args: Array<String>): ProtonJ2ClientFormatterSpy {
            val messageFormatter = ProtonJ2MessageFormatter()
            val messageFormatterSpy = ProtonJ2ClientFormatterSpy()

            messageFormatterSpy.client = CommandLine(CliProtonJ2Sender(messageFormatterSpy))
            messageFormatterSpy.args = args

            return messageFormatterSpy;
        }

        fun makeProtonj2ReceiverClient(args: Array<String>): ProtonJ2ClientFormatterSpy {
            val messageFormatter = ProtonJ2MessageFormatter()
            val messageFormatterSpy = ProtonJ2ClientFormatterSpy()

            messageFormatterSpy.client = CommandLine(CliProtonJ2Receiver(messageFormatterSpy))
            messageFormatterSpy.args = args

            return messageFormatterSpy
        }

        fun makeProtonj2BrowserClient(args: Array<String>): ProtonJ2ClientFormatterSpy {
            return makeProtonj2ReceiverClient(args + "--recv-browse" + "true")
        }
        //endregion
    }
}
