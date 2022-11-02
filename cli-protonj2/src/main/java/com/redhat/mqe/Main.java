package com.redhat.mqe;

import picocli.CommandLine;
import picocli.CommandLine.Command;

import java.util.concurrent.Callable;

@Command(
    name = "cli-protonj2",
    mixinStandardHelpOptions = true,
    version = "1.0.0",
    description = "Sends and receives messages using Qpid Proton-J2 AMQP library.",
    subcommands = {
        CliProtonJ2Connector.class,
        CliProtonJ2Sender.class,
        CliProtonJ2Receiver.class
    }
)
class Main implements Callable<Integer> {
    public static final String CLI_JAVA_NULL_VALUE = "CLI_JAVA_" + CommandLine.Option.NULL_VALUE;

    @Override
    public Integer call() throws Exception { // your business logic goes here...
        return 0;
    }

    // this example implements Callable, so parsing, error handling and handling user
    // requests for usage help or version help can be done with one line of code.
    public static void main(String... args) {
        int exitCode = new CommandLine(new Main()).execute(args);
        System.exit(exitCode);
    }
}

class CliProtonJ2SenderReceiver {


    protected boolean stringToBool(String string) {
        boolean bool = string.equalsIgnoreCase("true") || string.equalsIgnoreCase("yes");
        return bool;
    }
}

enum AuthMechanism {
    PLAIN, anonymous
}

// todo list of features in general; supports kerberos, io-uring, epoll, websockets,
// does it support opening listening sockets? listening websocket? NO
// does support all JMS 2.0 capabilities? (in some way, assuming broker cooperates?) It should

