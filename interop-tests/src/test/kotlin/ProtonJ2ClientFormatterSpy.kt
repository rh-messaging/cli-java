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
import com.redhat.mqe.ProtonJ2MessageFormatter
import picocli.CommandLine

class ProtonJ2ClientFormatterSpy : ProtonJ2MessageFormatter(), IClientFormatterSpy {
    lateinit var args: Array<String>
    lateinit var client: CommandLine

    override val messages: MutableList<Map<String, Any>> = ArrayList()

    override fun printMessageAsPython(format: MutableMap<String, Any>?) {
        messages.add(format!!.toMap())
        super.printMessageAsPython(format)
    }

    override fun run() {
        val exitCode = client.execute(*args)
        Truth.assertThat(exitCode).isEqualTo(0)
    }
}
