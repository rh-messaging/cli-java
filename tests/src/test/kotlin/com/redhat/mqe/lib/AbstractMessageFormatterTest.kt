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
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.function.Executable
import java.security.InvalidParameterException
import java.util.*
import javax.jms.BytesMessage
import javax.jms.Message
import javax.json.Json
import javax.json.JsonValue
import javax.json.spi.JsonProvider

class Formatter : MessageFormatter() {
    override fun formatMessage(msg: Message?): MutableMap<String, Any> {
        TODO("not implemented")
    }
}

abstract class AbstractMessageFormatterTest {
    private val formatter = Formatter()

    abstract fun getBytesMessage(): BytesMessage

    @Test
    fun `format content of empty bytes message`() {
        val bytesMessage = getBytesMessage()
        bytesMessage.reset()
        assertThat(formatter.formatContent(bytesMessage)).isNull()
    }

    @Test
    fun `test python formatString`() {
        val assertions = listOf(
            "" to "''",
            "a" to "'a'",
            "\"" to "'\"'",
            "'" to "'\\''"
//            "\\" to "'\\\\'" // FIXME: does not work
        )
        Assertions.assertAll(assertions.map {
            Executable {
                assertThat(formatter.formatString(it.first).toString()).isEqualTo(it.second)
            }
        }.stream())
    }
}
