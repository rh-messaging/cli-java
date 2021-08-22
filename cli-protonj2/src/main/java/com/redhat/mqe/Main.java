package com.redhat.mqe;

import com.redhat.mqe.lib.ClientOptions;
import com.redhat.mqe.lib.ConnectionManager;
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
    void logMessage(String address, Message message) throws ClientException {
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
        addKeyValue(sb, "redelivered", message.deliveryCount() > 1);
        addKeyValue(sb, "reply-to-group-id", message.replyToGroupId());
        addKeyValue(sb, "durable", message.durable());
        addKeyValue(sb, "group-sequence", message.groupSequence());
        addKeyValue(sb, "creation-time", message.creationTime());
        addKeyValue(sb, "content-type", message.contentType());
        addKeyValue(sb, "id", message.messageId());
        addKeyValue(sb, "reply-to", message.replyTo());

        // getPropertyNames? from JMS missing?
        StringBuilder sbb = new StringBuilder();
        sbb.append('{');
//        AtomicBoolean first = new AtomicBoolean(true);
        message.forEachProperty((s, o) -> {
//            if (!first.get()) {
//                sbb.append(", ");
//                first.set(false);
//            }
            addKeyValue(sbb, (String) s, o);  // this wanted to cast to string when I removed message generic type; what??? TODO
        });
        if (message.hasProperties()) {
            sbb.delete(sbb.length() - 2, sbb.length());  // remove last ", "
        }
        sbb.append('}');
        addKeyValue(sb, "properties", sbb); // ???

        sb.delete(sb.length() - 2, sb.length());  // remove last ", "

        sb.append("}");

        System.out.println(sb);
    }

    void addKeyValue(StringBuilder sb, String key, Object value) {
        sb.append("'");
        sb.append(key);
        sb.append("': ");
        sb.append(formatPython(value));
        sb.append(", ");
    }

    String formatPython(Object parameter) {
        if (parameter == null) {
            return "None";
        }
        if (parameter instanceof String) {
            return "'" + parameter + "'";
        }
        if (parameter instanceof Boolean) {
            return ((boolean)parameter) ? "True" : "False";
        }
        if (parameter instanceof StringBuilder) {
            return parameter.toString();
        }
        if (parameter instanceof List) {
            return "[" + ((List<Object>)parameter).stream().map(this::formatPython).collect(Collectors.joining(", "))  + "]";
        }
        return  "'" + parameter + "'";
    }

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

@Command(
    name = "sender",
    mixinStandardHelpOptions = true,
    version = "1.0.0",
    description = "Opens AMQP connections"
)
class CliProtonJ2Sender extends CliProtonJ2SenderReceiver implements Callable<Integer> {

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

    @Option(names = {"--timeout"}, description = "MD5, SHA-1, SHA-256, ...")
    private int timeout;

    @Option(names = {"--conn-auth-mechanisms"}, description = "MD5, SHA-1, SHA-256, ...")  // todo, want to accept comma-separated lists; there is https://picocli.info/#_split_regex
    private List<AuthMechanism> connAuthMechanisms = new ArrayList<>();

    @Option(names = {"--msg-property"})  // picocli Map options works for this, sounds like
    private List<String> msgProperties = new ArrayList<>();

    @Option(names = {"--msg-content"})
    private String msgContent;

    @Option(names = {"--msg-durable"})
    private String msgDurableString = "false";

    @Option(names = {"--msg-ttl"})
    private Long msgTtl;

    @Option(names = {"--msg-content-list-item"})
    private List<String> msgContentListItem;

    @Option(names = {"--msg-content-map-item"})
    private List<String> msgContentMapItems;

    @Option(names = {"--msg-correlation-id"})
    private String msgCorrelationId;

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

        String destinationCapability = "queue";
        if (address.startsWith(TOPIC_PREFIX)) {
            address = address.substring((TOPIC_PREFIX.length()));
            destinationCapability = "topic";
        }
        if (address.startsWith(QUEUE_PREFIX)) {
            address = address.substring((QUEUE_PREFIX.length()));
        }

        final Client client = Client.create();

        final ConnectionOptions options = new ConnectionOptions();
        options.user(connUsername);
        options.password(connPassword);
        for (AuthMechanism mech : connAuthMechanisms) {
            options.saslOptions().addAllowedMechanism(mech.name());
        }

        /*
        TODO API usablility, hard to ask for queue when dealing with broker that likes to autocreate topics
         */
        SenderOptions senderOptions = new SenderOptions();
        // is it target or source? target.
        senderOptions.targetOptions().capabilities(destinationCapability);
        try (Connection connection = client.connect(serverHost, serverPort, options);
             Sender sender = connection.openSender(address, senderOptions)) {

            for (int i = 0; i < count; i++) {
                Message message;
                if (msgContentListItem != null && !msgContentListItem.isEmpty()) {
                    // TODO have to cast strings to objects of correct types
                    message = Message.create(msgContentListItem);
                } else if (msgContentMapItems != null) {
                    Map<String, String> map = new HashMap<>();
                    for(String item : msgContentMapItems) {
                        String[] fields = item.split("[=~]", 2);
                        if (fields.length != 2) {
                            throw new RuntimeException("Wrong format " + Arrays.toString(fields));  // TODO do this in args parsing?
                        }
                        map.put(fields[0], fields[1]); // todo retype value
                    }
                    message = Message.create(map);
                } else {
                    message = Message.create(msgContent);
                }
                for (String property : msgProperties) {
                    String[] fields = property.split("=", 2);
                    if (fields.length != 2) {
                        throw new RuntimeException("Wrong format " + Arrays.toString(fields));  // TODO do this in args parsing
                    }
                    String key = fields[0];
                    String value = fields[1];  // more types
                    message.property(key, value);
                }
                if (msgCorrelationId != null) {
                    message.correlationId(msgCorrelationId);
                }
                if (msgTtl != null) {
                    message.timeToLive(msgTtl);
                }
                if (stringToBool(msgDurableString)) {
                    message.durable(true);
                }
                sender.send(message);  // TODO what's timeout for in a sender?
                logMessage(address, message);
            }
        }

        return 0;
    }
}

@Command(
    name = "receiver",
    mixinStandardHelpOptions = true,
    version = "1.0.0",
    description = "Opens AMQP connections"
)
class CliProtonJ2Receiver extends CliProtonJ2SenderReceiver implements Callable<Integer> {

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

    @Option(names = {"--recv-browse"}, description = "browse queued messages instead of receiving them")
    private String recvBrowseString = "false";

    @Option(names = {"--count"}, description = "MD5, SHA-1, SHA-256, ...")
    private int count = 1;

    @Option(names = {"--timeout"}, description = "MD5, SHA-1, SHA-256, ...")
    private int timeout;

    @Option(names = {"--conn-auth-mechanisms"}, description = "MD5, SHA-1, SHA-256, ...")
    // todo, want to accept comma-separated lists; there is https://picocli.info/#_split_regex
    private List<AuthMechanism> connAuthMechanisms = new ArrayList<>();

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

        String destinationCapability = "queue";
        if (address.startsWith(TOPIC_PREFIX)) {
            address = address.substring((TOPIC_PREFIX.length()));
            destinationCapability = "topic";
        }
        if (address.startsWith(QUEUE_PREFIX)) {
            address = address.substring((QUEUE_PREFIX.length()));
        }

        final Client client = Client.create();

        final ConnectionOptions options = new ConnectionOptions();
        options.user(connUsername);
        options.password(connPassword);
        for (AuthMechanism mech : connAuthMechanisms) {
            options.saslOptions().addAllowedMechanism(mech.name());
        }

        /*
        TODO API usablility, hard to ask for queue when dealing with broker that likes to autocreate topics
         */
        ReceiverOptions receiverOptions = new ReceiverOptions();
        // is it target or source? target.
        receiverOptions.sourceOptions().capabilities(destinationCapability);

        // todo: another usability, little hard to figure out this is analogue of jms to browse queues
        if (stringToBool(recvBrowseString)) {
            receiverOptions.sourceOptions().distributionMode(DistributionMode.COPY);
        }

        try (Connection connection = client.connect(serverHost, serverPort, options);
             Receiver receiver = connection.openReceiver(address, receiverOptions)) {

            for (int i = 0; i < count; i++) {
                final Delivery delivery;
                if (timeout == 0) {
                    delivery = receiver.receive();  // todo: can default it to -1
                } else {
                    delivery = receiver.receive(timeout, TimeUnit.SECONDS);
                }

                if (delivery == null) {
                    break;
                }

                int messageFormat = delivery.messageFormat();
                Message<String> message = delivery.message();
                logMessage(address, message);
            }
        }

        return 0;
    }

}
