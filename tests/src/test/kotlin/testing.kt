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
import org.junit.jupiter.api.function.Executable
import java.security.Permission
import kotlin.test.fail

class SystemExitingWithStatus(val status: Int) : Exception()
class NoExitSecurityManager(val parentManager: SecurityManager?) : SecurityManager() {
    override fun checkExit(status: Int) = throw SystemExitingWithStatus(status)
    override fun checkPermission(perm: Permission?) = Unit
}

fun assertSystemExit(status: Int, executable: Executable) {
    val previousManager = System.getSecurityManager()
    try {
        val manager = NoExitSecurityManager(previousManager)
        System.setSecurityManager(manager)

        executable.execute()

        fail("expected exception")
    } catch (e: SystemExitingWithStatus) {
        Truth.assertThat(e.status).isEqualTo(status)
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
