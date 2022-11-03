package com.redhat.mqe;

import org.apache.qpid.protonj2.client.ConnectionOptions;
import org.jetbrains.annotations.NotNull;
import picocli.CommandLine;
import picocli.CommandLine.Command;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

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

class CliProtonJ2SenderReceiverConnector {
    @CommandLine.Option(names = {"--conn-username"}, description = "")
    private String connUsername = "MD5";
    @CommandLine.Option(names = {"--conn-password"}, description = "")
    private String connPassword = "MD5";
    @CommandLine.Option(names = {"--conn-auth-mechanisms"}, description = "MD5, SHA-1, SHA-256, ...")
    // todo, want to accept comma-separated lists; there is https://picocli.info/#_split_regex
    private List<AuthMechanism> connAuthMechanisms = new ArrayList<>();
    @CommandLine.Option(names = {"--conn-reconnect"})
    private String reconnectString = "false";
    @CommandLine.Option(names = {"--conn-heartbeat"})
    private Long connHeartbeat;
    @CommandLine.Option(names = {"--conn-ssl-verify-host"}, arity = "0..1")
    private Boolean connSslVerifyHost;
    @CommandLine.Option(names = {"--conn-ssl-trust-all"}, arity = "0..1")
    private Boolean connSslTrustAll;

    protected boolean stringToBool(String string) {
        boolean bool = string.equalsIgnoreCase("true") || string.equalsIgnoreCase("yes");
        return bool;
    }

    @NotNull
    protected ConnectionOptions getConnectionOptions() {
        final ConnectionOptions options = new ConnectionOptions();
        // TODO typo in javadoc: This option enables or disables reconnection to a remote remote peer after IO errors. To control
        // TODO API: unclear if reconnect is on or off by default (public static final boolean DEFAULT_RECONNECT_ENABLED = false;)
        if (stringToBool(reconnectString)) {
            options.reconnectEnabled(true);
        }
        if (connHeartbeat != null) {
            // TODO finish that 2x investigation for heartbeats and document it somewhere (jira?)
            options.idleTimeout(2 * connHeartbeat, TimeUnit.SECONDS);
        }
        options.user(connUsername);
        options.password(connPassword);
        for (AuthMechanism mech : connAuthMechanisms) {
            options.saslOptions().addAllowedMechanism(mech.name());
        }
        if (connSslVerifyHost != null || connSslTrustAll != null) {
            options.sslEnabled(true);
        }

        // TODO: why is there both `options.sslEnabled and options.sslOptions().sslEnabled()`?
        if (connSslVerifyHost != null) {
            options.sslOptions().verifyHost(connSslVerifyHost);
        }
        if (connSslTrustAll != null) {
            options.sslOptions().trustAll(connSslTrustAll);
        }

        // TODO: what do I actually need/want here?
        // TODO, same problem, lib has Symbols in ClientConstants class
        // cli proton cpp does not do this, btw
//        options.desiredCapabilities(
//            "sole-connection-for-container", "DELAYED_DELIVERY", "SHARED-SUBS", "ANONYMOUS-RELAY"
//        );
        return options;
    }
}

enum AuthMechanism {
    PLAIN, anonymous
}

// todo list of features in general; supports kerberos, io-uring, epoll, websockets,
// does it support opening listening sockets? listening websocket? NO
// does support all JMS 2.0 capabilities? (in some way, assuming broker cooperates?) It should

