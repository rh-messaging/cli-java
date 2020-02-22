package com.redhat.amqx.main.actions;

import com.redhat.amqx.main.BrokerType;
import com.redhat.amqx.management.BrokerManager;
import com.redhat.amqx.management.ManagerFactory;
import org.apache.commons.cli.*;

import java.io.IOException;

/**
 * Created by opiske on 6/19/15.
 */
public class BrokerAction extends AbstractAction {

    public static final String RELOAD_ACTION = "reload";
    public static final String CONNECTORS_ACTION = "connectors";
    public static final String TOPOLOGY_ACTION = "topology";
    private CommandLine cmdLine;
    private boolean isHelp;
    private String jmxURL;
    private String action;
    private String brokerType;
    private String brokerName;
    private String host;
    private String connectionId;

    public BrokerAction(String[] args) {
        processCommand(args);
    }

    /**
     * Processes the command-line arguments
     *
     * @param args the command line arguments
     */
    protected void processCommand(String[] args) {
        CommandLineParser parser = new DefaultParser();

        options.addOption("a", "action", true, "get action on broker [reload, connectors, " +
                LIST_ACTION + ", " + PROPERTIES_ACTION + ", " + TOPOLOGY_ACTION + ", " + LIST_SESSIONS + "]");

        try {
            cmdLine = parser.parse(options, args);
        } catch (ParseException e) {
            System.err.println(e.getMessage());
            help(options, -1);
        }

        isHelp = cmdLine.hasOption("help");
        jmxURL = cmdLine.getOptionValue('u');
        brokerName = cmdLine.getOptionValue('b');
        action = cmdLine.getOptionValue('a', LIST_ACTION);
        brokerType = cmdLine.getOptionValue("t", BrokerType.ARTEMIS.getDefaultUpstreamName());
        host = cmdLine.getOptionValue("host");
        setLogLevel(cmdLine.getOptionValue("log-level", DEFAULT_LOGGING_LEVEL));
        connectionId = cmdLine.getOptionValue("connection-id", null);

        if (action == null) {
            logger.error("The action argument must be provided");
            help(options, 2);
        }

        setCredentials(cmdLine);
    }

    @Override
    public int run() {
        if (isHelp) {
            help(options, 0);
        } else {
            try {
                ManagerFactory mf = new ManagerFactory();
                BrokerManager brokerManager = mf.createBrokerManager(
                        jmxURL, getCredentials(), brokerName, brokerType, host);

                switch (action) {
                    case RELOAD_ACTION:
                        brokerManager.reload();
                        break;
                    case CONNECTORS_ACTION:
                        brokerManager.getTransportConnectors();
                        break;
                    case TOPOLOGY_ACTION:
                        brokerManager.getNetworkTopology();
                        break;
                    case LIST_ACTION:
                        brokerManager.getAllBrokerDestinations();
                        break;
                    case LIST_SESSIONS:
                        brokerManager.getSessions(connectionId);
                        break;
                    case PROPERTIES_ACTION:
                        brokerManager.getAllBrokerDestinationsProperties();
                        break;
                    default:
                        logger.error("Invalid action: " + action);
                        return -3;
                }

            } catch (IOException e) {
                logger.error("IOException occurred!: {}", e.getMessage(), e);
                return -1;
            } catch (Exception e) {
                logger.error("General exception occurred!: {}", e.getMessage(), e);
                return -2;
            }
        }
        return 0;
    }
}
