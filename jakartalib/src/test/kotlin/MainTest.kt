/*
 * Copyright (c) 2022 Red Hat, Inc.
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

import org.junit.jupiter.api.Test

class MainTest {
    @Test
    fun notEnoughArguments_0() {
//        exit.expectSystemExitWithStatus(1)
//        Main.main(emptyArray<String>(), null, null)
    }

    @Test
    fun notEnoughArguments_1() {
//        Main.main(arrayOf("sender"), null, null) // should somehow break, NPE probably
    }

    @Test
    fun wrongArgument() {
//        Main.main(arrayOf("aString"), null, null) // should exit(1)
    }
}
