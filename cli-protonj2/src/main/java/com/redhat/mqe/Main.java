package com.redhat.mqe;

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
import java.net.URL;
import java.nio.file.Files;
import java.security.MessageDigest;
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

@Command(
    name = "sender",
    mixinStandardHelpOptions = true,
    version = "1.0.0",
    description = "Opens AMQP connections"
)
class CliProtonJ2Sender implements Callable<Integer> {

    @Option(names = {"--log-msgs"}, description = "MD5, SHA-1, SHA-256, ...")
    private String logMsgs = "MD5";

    @Option(names = {"--broker"}, description = "MD5, SHA-1, SHA-256, ...")
    private String broker = "MD5";

    @Option(names = {"--conn-username"}, description = "MD5, SHA-1, SHA-256, ...")
    private String connUsername = "MD5";

    @Option(names = {"--conn-password"}, description = "MD5, SHA-1, SHA-256, ...")
    private String connPassword = "MD5";

    @Option(names = {"--address"}, description = "MD5, SHA-1, SHA-256, ...")
    private String address = "MD5";

    @Option(names = {"--count"}, description = "MD5, SHA-1, SHA-256, ...")
    private int count = 1;

    @Override
    public Integer call() throws Exception { // your business logic goes here...
        String prefix = "";
        if (!broker.startsWith("amqp://") || !broker.startsWith("amqps://")) {
            prefix = "amqp://";
        }
        final URI url = new URI(prefix + broker);
        final String serverHost = url.getHost();
        int serverPort = url.getPort();
        serverPort = (serverPort == -1) ? 5672 : serverPort;

        final Client client = Client.create();

        final ConnectionOptions options = new ConnectionOptions();
        options.user(connUsername);
        options.password(connPassword);

        /*
        TODO API usablility, hard to ask for queue when delaing with broker that likes to autocreate topics
         */
        SenderOptions senderOptions = new SenderOptions();
        // is it target or source? target.
        senderOptions.targetOptions().capabilities("queue");
        try (Connection connection = client.connect(serverHost, serverPort, options);
             Sender sender = connection.openSender(address, senderOptions)) {

            for (int i = 0; i < count; i++) {
                final Message<String> message = Message.create("");
                logMessage(address, message);
                sender.send(message);
            }
        }

        return 0;
    }

    private void logMessage(String address, Message<String> message) throws ClientException {
        StringBuilder sb = new StringBuilder();

        sb.append("{");

        addKeyValue(sb, "address", address);
        addKeyValue(sb, "group-id", message.groupId());
        addKeyValue(sb, "subject", message.subject());
        addKeyValue(sb, "user-id", message.userId());
        addKeyValue(sb, "correlation-id", message.correlationId());
        addKeyValue(sb, "content-encoding", message.contentEncoding());
        addKeyValue(sb, "priority", message.priority());
        addKeyValue(sb, "type", "string");  // ???
        addKeyValue(sb, "ttl", message.timeToLive());
        addKeyValue(sb, "absolute-expiry-time", message.absoluteExpiryTime());
        addKeyValue(sb, "content", message.body());
        addKeyValue(sb, "redelivered", message.deliveryCount() > 1);  // ????
        addKeyValue(sb, "reply-to-group-id", message.replyToGroupId());
        addKeyValue(sb, "durable", message.durable());
        addKeyValue(sb, "delivery-time", message);  // ???
        addKeyValue(sb, "group-sequence", message.groupSequence());
        addKeyValue(sb, "creation-time", message.creationTime());
        addKeyValue(sb, "content-type", message.contentType());
        addKeyValue(sb, "id", message.messageId());
        addKeyValue(sb, "reply-to", message.replyTo());
        addKeyValue(sb, "properties", message); // ???

        sb.delete(sb.length() - 2, sb.length());  // remove last ", "

        sb.append("}");

        System.out.println(sb);
    }

    private void addKeyValue(StringBuilder sb, String key, Object value) {
        sb.append("'");
        sb.append(key);
        sb.append("': ");
        sb.append(formatPython(value));
        sb.append(", ");
    }

    private String formatPython(Object parameter) {
        if (parameter == null) {
            return "None";
        }
        if (parameter instanceof String) {
            return "'" + parameter + "'";
        }
        if (parameter instanceof Boolean) {
            return ((boolean)parameter) ? "True" : "False";
        }
        return  "'" + parameter + "'";
    }
}

@Command(
    name = "receiver",
    mixinStandardHelpOptions = true,
    version = "1.0.0",
    description = "Opens AMQP connections"
)
class CliProtonJ2Receiver implements Callable<Integer> {

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
