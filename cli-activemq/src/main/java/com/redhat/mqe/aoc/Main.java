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

package com.redhat.mqe.aoc;

import com.redhat.mqe.lib.ConnectionManagerFactory;
import com.redhat.mqe.lib.CoreClient;

public class Main {
    public static void main(String[] args) throws Exception {
        CoreClient.setClientType(CoreClient.CORE_CLIENT_TYPE);  //TODO(jdanek): get the value from ConnectionManagerFactory
        ConnectionManagerFactory connectionManagerFactory = new AocConnectionManagerFactory();
        com.redhat.mqe.lib.Main.main(args, new AocClientFactory(), connectionManagerFactory);
    }
}
