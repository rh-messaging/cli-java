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

package com.redhat.mqe.lib

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import javax.jms.BytesMessage
import javax.jms.Message

class Formatter : MessageFormatter() {
    override fun printMessageBodyAsText(message: Message?) {
        TODO("not implemented")
    }

    override fun printMessageAsDict(msg: Message?) {
        TODO("not implemented")
    }

    override fun printMessageAsInterop(msg: Message?) {
        TODO("not implemented")
    }
}

abstract class AbstractMessageFormatterTest {
    private val formatter = Formatter()

    abstract fun getBytesMessage(): BytesMessage

    @Test
    fun formatContentOfBytesMessage_empty() {
        val bytesMessage = getBytesMessage()
        bytesMessage.reset()
        assertThat(formatter.formatContent(bytesMessage).toString()).isEqualTo("None")
    }
}
