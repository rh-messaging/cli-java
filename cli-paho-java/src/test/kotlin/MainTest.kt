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

import com.redhat.mqe.amc.Main
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class AmcMainTest : AbstractMainTest() {

    override val brokerUrl = "tcp://127.0.0.1:1883"
    override val sslBrokerUrl = "tcp://127.0.0.1:61617"  // or 8883

    override val senderAdditionalOptions = """
""".split(" ", "\n").toTypedArray()

// cannot set Client ID, because more than one connection is created, and these would clash
//--conn-clientid aClientId
    override val connectorAdditionalOptions = """
""".split(" ", "\n").toTypedArray()

    // skip them all
    override fun main(args: Array<String>) {
        Assumptions.assumeTrue(false, "skipping all cli-paho-java tests")
        Main.main(args)
    }

    @Disabled("ARTEMIS-1538 trustAll is ignored when specified in the connectionFactory URI")
    @Test
    override fun sendSingleMessageAllTrustingTls() {
    }
}
