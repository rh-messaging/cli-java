import com.google.common.truth.Truth
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Tags
import org.junit.jupiter.api.Test

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

@Tags(Tag("issue"), Tag("external"))
class QPIDJMS286Test {
    @Test
    fun `uri options are not visible in thread names`() {
        val f = org.apache.qpid.jms.JmsConnectionFactory(
            "amqp://127.0.0.1:5672?jms.username=anUserName&jms.password=aPassword&amqp.vhost=aVHostNotPassword")
        val c = f.createConnection()
        val s = c.createSession()

        val threadSet = Thread.getAllStackTraces().keys
        threadSet.forEach {
            println(it.name)
            Truth.assertThat(it.name).doesNotContain("Password")
        }

        s.close()
        c.close()
    }
}
