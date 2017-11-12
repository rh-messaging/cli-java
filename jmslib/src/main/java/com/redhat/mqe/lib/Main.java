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

package com.redhat.mqe.lib;

import java.util.Arrays;

public class Main {
    public static void main(String[] args, Client clientFactory) throws Exception {
        CoreClient client = null;
        if (args.length > 0) {
            String[] subArgs = Arrays.copyOfRange(args, 1, args.length);

            switch (args[0]) {
                case "sender":
                    client = clientFactory.makeSenderClient();
                    break;
                case "receiver":
                    client = clientFactory.makeReceiverClient();
                    break;
                case "connector":
                    client = clientFactory.makeConnectorClient();
                    break;
            }
        }

        if (client == null) {
            printHelpAndExit();
        } else {
            // https://stackoverflow.com/questions/2198928/better-handling-of-thread-context-classloader-in-osgi
            ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
            try {
                Thread.currentThread().setContextClassLoader(Main.class.getClassLoader());
                client.startClient();
            } finally {
                Thread.currentThread().setContextClassLoader(originalClassLoader);
            }
        }
    }

    private static void printHelpAndExit() {
        System.out.println("first argument must be sender, receiver OR connector");
        System.exit(1);
    }
}
