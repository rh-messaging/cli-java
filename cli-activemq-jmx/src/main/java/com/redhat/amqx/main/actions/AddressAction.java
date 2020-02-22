package com.redhat.amqx.main.actions;

import com.redhat.amqx.main.BrokerType;
import com.redhat.amqx.management.DestinationManager;
import com.redhat.amqx.management.ManagerFactory;
import org.apache.activemq.artemis.api.core.RoutingType;
import org.apache.commons.cli.*;

import java.io.IOException;

/**
 * This class implements the code for the 'queue' action
 */
public class AddressAction extends AbstractAction {
    private CommandLine cmdLine;
    private boolean isHelp;
    private String jmxURL;
    private String brokerName;
    private String addressName;
    private String action;
    private String brokerType;
    private boolean listProperties;
    private boolean durable;
    private String routingType;
    private String selector;
    private String host;

    public AddressAction(String[] args) {
        processCommand(args);
    }

    /**
     * Processes the command-line arguments
     *
     * @param args the command line arguments
     */
    protected void processCommand(String[] args) {
        CommandLineParser parser = new DefaultParser();

        options.addOption("a", "action", true, "action name [ add, remove, list, properties ]");
        options.addOption("n", "name", true, "address name");
        options.addOption("p", "list-properties", true, "print properties of address (default: true)");
        options.addOption("d", "durable", false, "create durable queue");
        options.addOption("r", "routingType", true, "routing type for this address 'anycast/multicast' (def:anycast)");
        options.addOption("s", "selector", true, "apply selector to this queue");

        try {
            cmdLine = parser.parse(options, args);
        } catch (ParseException e) {
            help(options, -1);
        }

        isHelp = cmdLine.hasOption("help");
        jmxURL = cmdLine.getOptionValue('u');
        brokerName = cmdLine.getOptionValue('b');
        addressName = cmdLine.getOptionValue('n');
        action = cmdLine.getOptionValue('a', LIST_ACTION);
        brokerType = cmdLine.getOptionValue('t', BrokerType.ARTEMIS.getDefaultUpstreamName());
        listProperties = Boolean.parseBoolean(cmdLine.getOptionValue('p', "true"));
        durable = cmdLine.hasOption('d');
        routingType = cmdLine.getOptionValue("routingType", RoutingType.ANYCAST.toString());
        selector = cmdLine.getOptionValue('s');
        host = cmdLine.getOptionValue("host");
        setCredentials(cmdLine);
        setLogLevel(cmdLine.getOptionValue("log-level", DEFAULT_LOGGING_LEVEL));

        if (routingType.toLowerCase().equals("multicast")) {
            routingType = RoutingType.MULTICAST.toString();
        } else {
            routingType = RoutingType.ANYCAST.toString();
        }

    }


    public int run() {
        if (isHelp) {
            help(options, 0);
        } else {
            try {
                ManagerFactory mf = new ManagerFactory();
                DestinationManager queueManager = mf.createQueueManager(jmxURL, getCredentials(), brokerName, brokerType, host);
                switch (action) {
                    case ADD_ACTION:
                        queueManager.addDestination(addressName, durable, routingType, selector);
                        break;
                    case REMOVE_ACTION:
                        queueManager.removeDestination(addressName, null);
                        break;
                    case LIST_ACTION:
                        queueManager.listDestinations(listProperties);
                        break;
                    case PROPERTIES_ACTION:
                        queueManager.getDestinationProperties(addressName, null);
                        break;
                    case UPDATE_ACTION:
                        queueManager.updateDestination(null, false, addressName, selector, -1, false, routingType);
                        break;
                    default:
                        logger.error("Invalid action: " + action);
                        return -3;
                }
            } catch (IOException e) {
                logger.error("Unable to " + action + " queue: " + e.getMessage(), e);

                return -1;
            } catch (Exception e) {
                logger.error("Unable to " + action + " queue: " + e.getMessage(), e);

                return -2;
            }
        }

        return 0;
    }
}
