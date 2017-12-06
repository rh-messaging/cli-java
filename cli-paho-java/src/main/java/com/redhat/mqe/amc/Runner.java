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

package com.redhat.mqe.amc;

import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

import java.io.IOException;
import java.util.Arrays;

/**
 * Created by mtoth on 4/26/16.
 */
public class Runner {

  private static OptionSpec<String> destination;
  private static OptionSpec<String> clientId;
  private static OptionSpec<String> content;
  private static OptionSpec<String> broker;
  private static OptionSpec<Integer> qos;
  private static OptionSpec<Integer> timeout;
  private static OptionSpec<Integer> msgCount;

  private static final String BROKER = "broker";
  private static final String DESTINATION = "destination";
  private static final String CLIENT_ID = "client-id";
  private static final String MSG_QOS = "msg-qos";
  private static final String MSG_CONTENT = "msg-content";
  private static final String MSG_COUNT = "msg-count";
  private static final String HELP = "help";
  private static final String TIMEOUT = "timeout";

  private static String cliDestination;
  private static String cliClientId;
  private static int cliQos;
  private static String cliContent;
  private static String cliBroker;
  private static int cliTimeout;
  private static int cliMsgCount;

  public static void main(String[] args) {
    try {
      OptionParser parser = createOptionParser();
      if (args.length >= 1) {
        parseInputOptions(parser, Arrays.copyOfRange(args, 1, args.length));
        if (args[0].equals("sender")) {
          createSender();
        } else if (args[0].equals("receiver")) {
          createReceiver();
        } else {
          printHelp(parser);
        }
      } else {
        printHelp(parser);
      }

    } catch (Exception e) {
      e.printStackTrace();
      System.exit(1);
    }
  }

  private static void printHelp(OptionParser parser) {
    try {
      System.out.println("Usage: (default credentials admin/admin)");
      System.out.println("<sender|receiver> <Option> [<Option> <Option>...]");
      parser.printHelpOn(System.out);
    } catch (IOException e) {
      e.printStackTrace();
      System.exit(1);
    }
  }

  private static void createReceiver() {
    Receiver receiver = new Receiver(cliBroker);
    receiver.receive(cliDestination, cliClientId, cliTimeout);
  }

  private static void createSender() {
    Sender sender = new Sender(cliBroker);
    sender.send(cliDestination, cliQos, cliContent, cliClientId, cliMsgCount);
  }


  private static void parseInputOptions(OptionParser parser, String[] args) {
    try {
      OptionSet optionSet = parser.parse(args);
      if (optionSet.has(HELP)) {
        printHelp(parser);
        System.exit(0);
      } else {
        cliBroker = optionSet.valueOf(broker);
        cliDestination = optionSet.valueOf(destination);
        cliClientId = optionSet.valueOf(clientId);
        cliQos = optionSet.valueOf(qos);
        cliContent = optionSet.valueOf(content);
        cliTimeout = optionSet.valueOf(timeout);
        cliMsgCount = optionSet.valueOf(msgCount);
      }
    } catch (OptionException optExc) {
      System.err.println("Unrecognized option " + optExc);
      printHelp(parser);
      System.exit(1);
    }
  }

  /**
   * Create accepting command line options for parser.
   *
   * @return option parser to be used
   */
  private static OptionParser createOptionParser() {
    OptionParser parser = new OptionParser();
    broker = parser.accepts(BROKER, "broker address").withRequiredArg()
        .ofType(String.class).defaultsTo("tcp://localhost:1883").describedAs("default mqtt broker address");

    destination = parser.accepts(DESTINATION, "mqtt topic name").withRequiredArg()
        .ofType(String.class).defaultsTo("mqttTopic").describedAs("default mqtt topic destination");

    clientId = parser.accepts(CLIENT_ID, "client id").withRequiredArg()
        .ofType(String.class).defaultsTo("mqttPahoClientId");

    content = parser.accepts(MSG_CONTENT, "message content").withRequiredArg()
        .ofType(String.class).defaultsTo("");

    qos = parser.accepts(MSG_QOS, "message QoS (0,1,2)").withRequiredArg().ofType(Integer.class).defaultsTo(1);
    timeout = parser.accepts(TIMEOUT, "receiver timeout in seconds").withRequiredArg().ofType(Integer.class).defaultsTo(10);
    msgCount = parser.accepts(MSG_COUNT, "number of messages").withRequiredArg().ofType(Integer.class).defaultsTo(1);

    parser.accepts(HELP, "This help");

    return parser;
  }
}
