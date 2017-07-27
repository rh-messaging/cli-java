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

package com.redhat.mqe.jms;

import java.util.Arrays;

public class Main {
    private static final String SENDER = "sender";
    private static final String RECEIVER = "receiver";
    private static final String CONNECTOR = "connector";

    public static void main(String[] args) {
        String startHelp = "first argument must be sender, receiver OR connector";
        if (args.length > 0) {
            String[] subArgs;
            if (args.length > 1) {
                int startIndex = 1;
                subArgs = Arrays.copyOfRange(args, startIndex, args.length);
            } else {
                subArgs = new String[0];
            }
            switch (args[0]) {
                case SENDER:
                    aac1_sender.main(subArgs);
                    break;
                case RECEIVER:
                    aac1_receiver.main(subArgs);
                    break;
                case CONNECTOR:
                    aac1_connector.main(subArgs);
                    break;
                default:
                    System.out.println(startHelp);
            }
        } else {
            System.out.println(startHelp);
        }
    }
}
