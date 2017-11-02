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

import com.redhat.mqe.acc.AccClientOptionManager
import com.redhat.mqe.acc.AccConnectionManagerFactory
import com.redhat.mqe.acc.AccCoreMessageFormatter
import com.redhat.mqe.aoc.AocClientOptionManager
import com.redhat.mqe.aoc.AocConnectionManagerFactory
import com.redhat.mqe.jms.AacClientOptionManager
import com.redhat.mqe.jms.AacConnectionManagerFactory
import com.redhat.mqe.jms.AacReceiverOptions
import com.redhat.mqe.jms.AacSenderOptions
import com.redhat.mqe.lib.*
import javax.jms.Message

class ClientFormatterSpy(private val formatter: MessageFormatter) : MessageFormatter() {
    lateinit var client: CoreClient
    val messages: MutableList<Map<String, Any>> = ArrayList()
    override fun formatMessage(msg: Message?): MutableMap<String, Any> = formatter.formatMessage(msg)

    override fun printMessageAsPython(format: MutableMap<String, Any>?) {
        messages.add(format!!.toMap())
        super.printMessageAsPython(format)
    }

    fun run() {
        client.startClient()
    }

    companion object {
        fun makeAacSenderClient(args: Array<String>): ClientFormatterSpy {
            val connectionManagerFactory = AacConnectionManagerFactory()
            val messageFormatter: MessageFormatter = AMQPMessageFormatter()
            val clientOptionManager: ClientOptionManager = AacClientOptionManager()
            val options = AacSenderOptions()
            clientOptionManager.applyClientArguments(options, args)

            val spy = ClientFormatterSpy(messageFormatter)
            spy.client = SenderClient(args, connectionManagerFactory, spy, options)
            return spy
        }

        fun makeAacReceiverClient(args: Array<String>): ClientFormatterSpy {
            val connectionManagerFactory = AacConnectionManagerFactory()
            val messageFormatter: MessageFormatter = AMQPMessageFormatter()
            val clientOptionManager: ClientOptionManager = AacClientOptionManager()
            val options = AacReceiverOptions()
            clientOptionManager.applyClientArguments(options, args)

            val spy = ClientFormatterSpy(messageFormatter)
            spy.client = ReceiverClient(args, connectionManagerFactory, spy, options)
            return spy
        }

        fun makeAacBrowserClient(args: Array<String>): ClientFormatterSpy {
            val connectionManagerFactory = AacConnectionManagerFactory()
            val messageFormatter: MessageFormatter = AMQPMessageFormatter()
            val clientOptionManager: ClientOptionManager = AacClientOptionManager()
            val options = AacReceiverOptions()
            clientOptionManager.applyClientArguments(options, args)

            val spy = ClientFormatterSpy(messageFormatter)
            ReceiverClient(args, connectionManagerFactory, spy, options)
            spy.client = MessageBrowser(options, connectionManagerFactory, spy)
            return spy
        }


        fun makeAccSenderClient(args: Array<String>): ClientFormatterSpy {
            val connectionManagerFactory = AccConnectionManagerFactory()
            val messageFormatter: MessageFormatter = AccCoreMessageFormatter()
            val clientOptionManager: ClientOptionManager = AccClientOptionManager()
            val options = SenderOptions()
            clientOptionManager.applyClientArguments(options, args)

            val spy = ClientFormatterSpy(messageFormatter)
            spy.client = SenderClient(args, connectionManagerFactory, spy, options)
            return spy
        }

        fun makeAccReceiverClient(args: Array<String>): ClientFormatterSpy {
            val connectionManagerFactory = AccConnectionManagerFactory()
            val messageFormatter: MessageFormatter = AccCoreMessageFormatter()
            val clientOptionManager: ClientOptionManager = AccClientOptionManager()
            val options = ReceiverOptions()
            clientOptionManager.applyClientArguments(options, args)

            val spy = ClientFormatterSpy(messageFormatter)
            spy.client = ReceiverClient(args, connectionManagerFactory, spy, options)
            return spy
        }

        fun makeAccBrowserClient(args: Array<String>): ClientFormatterSpy {
            val connectionManagerFactory = AccConnectionManagerFactory()
            val messageFormatter: MessageFormatter = AccCoreMessageFormatter()
            val clientOptionManager: ClientOptionManager = AccClientOptionManager()
            val options = ReceiverOptions()
            clientOptionManager.applyClientArguments(options, args)

            val spy = ClientFormatterSpy(messageFormatter)
            ReceiverClient(args, connectionManagerFactory, spy, options)
            spy.client = MessageBrowser(options, connectionManagerFactory, spy)
            return spy
        }


        fun makeAocSenderClient(args: Array<String>): ClientFormatterSpy {
            val connectionManagerFactory = AocConnectionManagerFactory()
            val messageFormatter: MessageFormatter = OpenwireMessageFormatter()
            val clientOptionManager: ClientOptionManager = AocClientOptionManager()
            val options = SenderOptions()
            clientOptionManager.applyClientArguments(options, args)

            val spy = ClientFormatterSpy(messageFormatter)
            spy.client = SenderClient(args, connectionManagerFactory, spy, options)
            return spy
        }

        fun makeAocReceiverClient(args: Array<String>): ClientFormatterSpy {
            val connectionManagerFactory = AocConnectionManagerFactory()
            val messageFormatter: MessageFormatter = OpenwireMessageFormatter()
            val clientOptionManager: ClientOptionManager = AocClientOptionManager()
            val options = ReceiverOptions()
            clientOptionManager.applyClientArguments(options, args)

            val spy = ClientFormatterSpy(messageFormatter)
            spy.client = ReceiverClient(args, connectionManagerFactory, spy, options)
            return spy
        }

        fun makeAocBrowserClient(args: Array<String>): ClientFormatterSpy {
            val connectionManagerFactory = AocConnectionManagerFactory()
            val messageFormatter: MessageFormatter = OpenwireMessageFormatter()
            val clientOptionManager: ClientOptionManager = AocClientOptionManager()
            val options = ReceiverOptions()
            clientOptionManager.applyClientArguments(options, args)

            val spy = ClientFormatterSpy(messageFormatter)
            ReceiverClient(args, connectionManagerFactory, spy, options)
            spy.client = MessageBrowser(options, connectionManagerFactory, spy)
            return spy
        }
    }
}
