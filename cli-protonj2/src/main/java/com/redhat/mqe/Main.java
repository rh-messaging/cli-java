package com.redhat.mqe;

import com.redhat.mqe.lib.JmsMessagingException;
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
import java.security.NoSuchAlgorithmException;
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
    void logMessage(String address, Message message, boolean msgContentHashed) throws ClientException {
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
        if (msgContentHashed) {
            // this is inlined addKeyValue, TODO do it nicer
            sb.append("'");
            sb.append("content");
            sb.append("': ");
            sb.append("'"); // extra quotes to format
            sb.append(hash(formatPython(message.body())));
            sb.append("'");
            sb.append(", ");
        } else {
            addKeyValue(sb, "content", message.body());
        }
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
            return "[" + ((List<Object>) parameter).stream().map(this::formatPython).collect(Collectors.joining(", ")) + "]";
        }
        return "'" + parameter + "'";
    }

    protected boolean stringToBool(String string) {
        boolean bool = string.equalsIgnoreCase("true") || string.equalsIgnoreCase("yes");
        return bool;
    }

    private String hash(Object o) {
        if (o == null) {
            return null; // no point in hashing this value
        }
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            throw new JmsMessagingException("Unable to hash message", e);
        }
        String content = o.toString();
        return new BigInteger(1, md.digest(content.getBytes())).toString(16);
    }

    // can't call these as implemented in Utils, undefined JmsException; somehow fix this; break dep on jms api

    /**
     * @return number of seconds (including milisecs) since EPOCH.
     */
    public static double getTime() {
        return System.currentTimeMillis();
    }


    /**
     * Sleeps until next timed for/while loop iteration.
     * This method takes into account the length of the action it preceded.
     *
     * @param initialTimestamp initial timestamp (in get_time() double form) is passed from a connection started
     * @param msgCount         number of iterations
     * @param duration         total time of all iterations
     * @param nextCountIndex   next iteration index
     */

    public static void sleepUntilNextIteration(double initialTimestamp, int msgCount, double duration, int nextCountIndex) {
        if ((duration > 0) && (msgCount > 0)) {
            // initial overall duration approximation of whole loop (sender/receiver)
            double cummulative_dur = (1.0 * nextCountIndex * duration) / msgCount;
            while (true) {
                if (getTime() - initialTimestamp - cummulative_dur > -0.05)
                    break;
                try {
//                    LOG.trace("sleeping");
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
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

    @Option(names = {"--msg-content-hashed"})
    private String msgContentHashedString = "false";

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

    @Option(names = {"--duration"})
    private int duration;  // TODO do something with it

    @Option(names = {"--conn-auth-mechanisms"}, description = "MD5, SHA-1, SHA-256, ...")
    // todo, want to accept comma-separated lists; there is https://picocli.info/#_split_regex
    private List<AuthMechanism> connAuthMechanisms = new ArrayList<>();

    @Option(names = {"--msg-property"})  // picocli Map options works for this, sounds like
    private List<String> msgProperties = new ArrayList<>();

    @Option(names = {"--msg-content"})
    private String msgContent;

    @Option(names = {"--msg-content-from-file"})
    private String msgContentFromFile;

    @Option(names = {"--content-type"})
    private ContentType contentType;

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

    @Option(names = {"--msg-group-id"})
    private String msgGroupId;

    @Option(names = {"--msg-id"})
    private String msgId;  // todo, not just string is an option

    @Option(names = {"--msg-reply-to"})
    private String msgReplyTo;

    @Option(names = {"--msg-subject"})
    private String msgSubject;

    @Option(names = {"--msg-user-id"})
    private String msgUserId;

    @Option(names = {"--msg-priority"})
    private Byte msgPriority;

    // jms.populateJMSXUserID opt in qpid-jms
    // TODO: does not seem to have equivalent; what is the threat model for "prevent spoofing" in JMS docs?
    @Option(names = {"--conn-populate-user-id"})
    private String connPopulateUserIdString = "false";

    @Option(names = {"--msg-group-seq"})
    private Integer msgGroupSeq;

    @Option(names = {"--msg-reply-to-group-id"})
    private String msgReplyToGroupId;

    @Option(names = {"--ssn-ack-mode"})
    private SsnAckMode ssnAckMode;

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
                if (msgContentListItem != null && !msgContentListItem.isEmpty()) {  // TODO check only one of these is specified
                    // TODO have to cast strings to objects of correct types
                    message = Message.create(msgContentListItem);
                } else if (msgContentMapItems != null) {
                    Map<String, String> map = new HashMap<>();
                    for (String item : msgContentMapItems) {
                        String[] fields = item.split("[=~]", 2);
                        if (fields.length != 2) {
                            throw new RuntimeException("Wrong format " + Arrays.toString(fields));  // TODO do this in args parsing?
                        }
                        map.put(fields[0], fields[1]); // todo retype value
                    }
                    message = Message.create(map);
                } else if (msgContentFromFile != null) {
                    message = Message.create(Files.readString(Paths.get(msgContentFromFile)));  // todo maybe param type as Path? check exists
                } else {
                    message = Message.create(msgContent);
                }
                for (String property : msgProperties) {
                    String[] fields = property.split("[=~]", 2);  // todo do something with ~
                    if (fields.length != 2) {
                        throw new RuntimeException("Wrong format " + Arrays.toString(fields));  // TODO do this in args parsing
                    }
                    String key = fields[0];
                    String value = fields[1];  // more types
                    message.property(key, value);
                }
                if (msgId != null) {
                    message.messageId(msgId);
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
                if (msgGroupId != null) {
                    message.groupId(msgGroupId);
                }
                if (msgGroupSeq != null) {
                    message.groupSequence(msgGroupSeq);
                }
                if (msgReplyTo != null) {
                    message.replyTo(msgReplyTo);
                }
                if (msgReplyToGroupId != null) {
                    message.replyToGroupId(msgReplyToGroupId);
                }
                if (contentType != null) {
                    message.contentType(contentType.toString()); // TODO: maybe should do more with it? don't bother with enum?
                }
                // todo, not sure what to do here; should I use authenticated userid instead?
                if (stringToBool(connPopulateUserIdString)) {
                    message.userId(msgUserId.getBytes());
                }
                if (msgSubject != null) {
                    message.subject(msgSubject);
                }
                if (msgPriority != null) {
                    message.priority(msgPriority);
                }
                sender.send(message);  // TODO what's timeout for in a sender?
                logMessage(address, message, stringToBool(msgContentHashedString));
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

    @Option(names = {"--msg-content-hashed"})
    private String msgContentHashedString = "false";

    @Option(names = {"--broker"}, description = "MD5, SHA-1, SHA-256, ...")
    private String broker = "MD5";

    @Option(names = {"--conn-username"}, description = "MD5, SHA-1, SHA-256, ...")
    private String connUsername = "MD5";

    @Option(names = {"--conn-password"}, description = "MD5, SHA-1, SHA-256, ...")
    private String connPassword = "MD5";

    @Option(names = {"--conn-clientid"})
    private String connClientId;

    @Option(names = {"--durable-subscriber"})
    private String durableSubscriberString = "false";

    @Option(names = {"--durable-subscriber-name"})
    private String durableSubscriberName;

    // TODO not implemented
    @Option(names = {"--subscriber-unsubscribe"})
    private String subscriberUnsubscribeString;

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

    @Option(names = {"--process-reply-to"})
    private boolean processReplyTo = false;

    @Option(names = {"--duration"})  // todo
    private Integer duration;

    @Option(names = {"--duration-mode"}) // todo
    private DurationMode durationMode;

    @Option(names = {"--ssn-ack-mode"})
    private SsnAckMode ssnAckMode;

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

        ClientOptions clientOptions = new ClientOptions();
        // TODO api usability I had to hunt for this a bit; the idea is to have durable subscription: need specify connection id and subscriber name
        if (connClientId != null) {
            clientOptions.id(connClientId);
        }

        // TODO api usability; If I use the w/ clientOptions variant of Client.create, then .id defaults to null, and I get exception;
        //  ok, that just cannot be true ^^^; but it looks to be true; what!?!
        // aha, right; constructor does not check, factory method does check for null
        // proposed solution: allow null there, and let it mean autoassign; or tell us method to generate ID ourselves if we don't care
        Client client;
        if (clientOptions.id() != null) {
            client = Client.create(clientOptions);
        } else {
            client = Client.create();
        }

        final ConnectionOptions options = new ConnectionOptions();
        options.user(connUsername);
        options.password(connPassword);
        for (AuthMechanism mech : connAuthMechanisms) {
            options.saslOptions().addAllowedMechanism(mech.name());
        }

        /*
        TODO API usability, hard to ask for queue when dealing with broker that likes to autocreate topics
         */
        ReceiverOptions receiverOptions = new ReceiverOptions();
        // is it target or source? target.
        receiverOptions.sourceOptions().capabilities(destinationCapability);

        // todo: another usability, little hard to figure out this is analogue of jms to browse queues
        if (stringToBool(recvBrowseString)) {
            receiverOptions.sourceOptions().distributionMode(DistributionMode.COPY);
        }

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

            double initialTimestamp = getTime();
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
                    sleepUntilNextIteration(initialTimestamp, count, duration, i + 1);
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
