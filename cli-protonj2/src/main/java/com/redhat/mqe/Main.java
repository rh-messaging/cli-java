package com.redhat.mqe;

import com.redhat.mqe.lib.Content;
import com.redhat.mqe.lib.MessageFormatter;
import com.redhat.mqe.lib.Utils;
import org.apache.qpid.protonj2.client.ClientOptions;
import org.apache.qpid.protonj2.client.DistributionMode;
import org.apache.qpid.protonj2.client.ReceiverOptions;
import org.apache.qpid.protonj2.client.SenderOptions;
import org.apache.qpid.protonj2.client.exceptions.ClientException;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import org.apache.qpid.protonj2.client.Client;
import org.apache.qpid.protonj2.client.Connection;
import org.apache.qpid.protonj2.client.ConnectionOptions;
import org.apache.qpid.protonj2.client.Delivery;
import org.apache.qpid.protonj2.client.Message;
import org.apache.qpid.protonj2.client.Receiver;
import org.apache.qpid.protonj2.client.Sender;

import java.io.File;
import java.math.BigInteger;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.redhat.mqe.lib.ClientOptionManager.QUEUE_PREFIX;
import static com.redhat.mqe.lib.ClientOptionManager.TOPIC_PREFIX;

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

@Command(
    name = "connector",
    mixinStandardHelpOptions = true,
    version = "1.0.0",
    description = "Opens AMQP connections"
)
class CliProtonJ2Connector implements Callable<Integer> {

    @Parameters(index = "0", description = "The file whose checksum to calculate.")
    private File file;

    @Option(names = {"-a", "--algorithm"}, description = "MD5, SHA-1, SHA-256, ...")
    private String algorithm = "MD5";

    @Override
    public Integer call() throws Exception { // your business logic goes here...
        byte[] fileContents = Files.readAllBytes(file.toPath());
        byte[] digest = MessageDigest.getInstance(algorithm).digest(fileContents);
        System.out.printf("%0" + (digest.length * 2) + "x%n", new BigInteger(1, digest));

        System.out.println("Hello World");

        final String serverHost = System.getProperty("HOST", "localhost");
        final int serverPort = Integer.getInteger("PORT", 5672);
        final String address = System.getProperty("ADDRESS", "hello-world-example");

        final Client client = Client.create();

        final ConnectionOptions options = new ConnectionOptions();
        options.user(System.getProperty("USER"));
        options.password(System.getProperty("PASSWORD"));

        try (Connection connection = client.connect(serverHost, serverPort, options);
             Receiver receiver = connection.openReceiver(address);
             Sender sender = connection.openSender(address)) {

            sender.send(Message.create("Hello World"));

            Delivery delivery = receiver.receive();
            Message<String> received = delivery.message();
            System.out.println("Received message with body: " + received.body());
        }

        return 0;
    }
}

enum AuthMechanism {
    PLAIN,
}

// todo list of features in general; supports kerberos, io-uring, epoll, websockets,
// does it support opening listening sockets? listening websocket?
// does support all JMS 2.0 capabilities? (in some way, assuming broker cooperates?)

        // TODO: API question: what is difference between autoSettle and autoAccept? why I want one but not the other?
        if(ssnAckMode != null) {
            if (ssnAckMode == SsnAckMode.client) {
                receiverOptions.autoAccept(false);
                receiverOptions.autoSettle(false);
            }
        }

        try (Connection connection = client.connect(serverHost, serverPort, options)) {
            Receiver receiver;
            if (stringToBool(durableSubscriberString)) {
                receiver = connection.openDurableReceiver(address, durableSubscriberName, receiverOptions);
            } else {
                receiver = connection.openReceiver(address, receiverOptions);
            }

            double initialTimestamp = Utils.getTime();
            for (int i = 0; i < count; i++) {

//                if (durationMode == DurationMode.sleepBeforeReceive) {
//                    LOG.trace("Sleeping before receive");
//                    Utils.sleepUntilNextIteration(initialTimestamp, msgCount, duration, i + 1);
//                }

                final Delivery delivery;
                if (timeout == 0) {
                    delivery = receiver.receive();  // todo: can default it to -1
                } else {
                    delivery = receiver.receive(timeout, TimeUnit.SECONDS);
                }

                if (delivery == null) {
                    break;
                }

                if (durationMode == DurationMode.afterReceive) {
//                    LOG.trace("Sleeping after receive");
                    Utils.sleepUntilNextIteration(initialTimestamp, count, duration, i + 1); // todo possibly it is i, different loop here
                }

                if (processReplyTo && delivery.message().replyTo() != null) {
                    String replyTo = delivery.message().replyTo();
                    Message<Object> message = delivery.message();
                    message.replyTo(null);
                    try (Sender sender = connection.openSender(replyTo)) {
                        sender.send(message);
                    }
                }

                int messageFormat = delivery.messageFormat();
                Message<String> message = delivery.message();

                // todo, is this what we mean?
                if (ssnAckMode != null && ssnAckMode == SsnAckMode.client) {
                    delivery.accept();
                }

                logMessage(address, message, stringToBool(msgContentHashedString));
            }

            // TODO API usability, how do I do durable subscription with detach, resume, etc; no mention of unsubscribe in the client anywhere
            receiver.close(); // TODO want to do autoclosable, need helper func, that's all
//            receiver.detach();
        }

        return 0;
    }

}
