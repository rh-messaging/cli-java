package com.redhat.amqx.main.actions;

import com.redhat.amqx.main.LogConfigurator;
import com.redhat.amqx.management.Credentials;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides a common interface for command-line actions
 */
public abstract class AbstractAction {
    static final Logger logger = LoggerFactory.getLogger(AbstractAction.class);
    private static final String DEFAULT_USERNAME = "admin";
    private static final String DEFAULT_PASSWORD = "admin";
    private Credentials credentials;
    public static final String ADD_ACTION = "add";
    public static final String REMOVE_ACTION = "remove";
    public static final String LIST_ACTION = "list";
    public static final String LIST_SESSIONS = "sessions";
    public static final String PROPERTIES_ACTION = "properties";
    public static final String UPDATE_ACTION = "update";
    public static final String REMOVE_MSGS_ACTION = "remove-msgs";

    protected static final String PURGE_ON_NO_CONSUMER =  "purge-on-no-consumer";
    protected static final String MAX_CONSUMERS =  "max-consumers";

    protected static final String DEFAULT_LOGGING_LEVEL = "INFO";
    protected final Options options;


    public AbstractAction() {
        options = new Options();
        options.addOption("h", "help", false, "prints the help");
        options.addOption("u", "url", true, "JMX URL");
        options.addOption("b", "broker-name", true, "broker name");
        options.addOption("t", "broker-type", true, "type of the broker [artemis, activemq]");

        options.addOption(null, "host", true, "broker <ip>:<port> (used instead of jmx service url)");
        options.addOption(null, "username", true, "username, if authentication is required");
        options.addOption(null, "password", true, "password, if authentication is required");
        options.addOption(null, "no-auth", false, "do not try to authenticate");
        options.addOption("l", "log-level", true, "use logging level <trace|debug|info|error>");
        options.addOption("c", "connection-id", true, "connectionId of a client");
    }

    /**
     * Prints the help for the action and exit
     *
     * @param options the options object
     * @param code    the exit code
     */
    protected void help(final Options options, int code) {
        HelpFormatter formatter = new HelpFormatter();

        formatter.printHelp("amqx", options);
        System.exit(code);
    }

    protected void setCredentials(final CommandLine cmdLine) {
        boolean noAuth = cmdLine.hasOption("no-auth");

        String username = cmdLine.getOptionValue("username");
        if (username == null && !noAuth) {
            username = DEFAULT_USERNAME;
        }

        String password = cmdLine.getOptionValue("password");
        if (password == null && !noAuth) {
            password = DEFAULT_PASSWORD;
        }

        credentials = new Credentials(username, password);
    }

    protected static void setLogLevel(String level) {
        level = level.toUpperCase();
        switch (level) {
            case "TRACE":
                LogConfigurator.trace();
                break;
            case "DEBUG":
                LogConfigurator.debug();
                break;
            case "INFO":
                LogConfigurator.info();
                break;
            case "ERROR":
                LogConfigurator.error();
                break;
            default:
                LogConfigurator.info();
        }
    }

    protected Credentials getCredentials() {
        return credentials;
    }

    /**
     * Subclasses must implement the logic within this method
     *
     * @return
     */
    abstract public int run();
}
