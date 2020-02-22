package com.redhat.amqx.main.actions;

import com.redhat.amqx.main.BrokerType;
import com.redhat.amqx.management.DestinationManager;
import com.redhat.amqx.management.ManagerFactory;
import org.apache.activemq.artemis.api.core.RoutingType;
import org.apache.commons.cli.*;

import java.io.IOException;

/**
 * QueueAction class manages destinations in Artemis/AMQ7 broker.
 * It does use core protocol so concept of addresses and queues/bindings are used.
 * There are no (JMS) queues or topics. Use {Queue,Topic}ArtemisManager for JMS objects.
 */
// TODO extend AddressAction is possible
public class QueueAction extends AbstractAction {

    private CommandLine cmdLine;
    private boolean isHelp;
    private String jmxURL;
    private String brokerName;
    private String destinationName;
    private String action;
    private String brokerType;
    private boolean listProperties;
    private boolean durable;
    private String address;
    private String selector;
    private String host;
    private int maxConsumers = -1;  // default value of maxConsumers
    private boolean purgeOnNoConsumers = false;

    public QueueAction(String[] args) {
        processCommand(args);
    }
    /**
     * Processes the command-line arguments
     *
     * @param args the command line arguments
     */
    protected void processCommand(String[] args) {
        CommandLineParser parser = new DefaultParser();

        // TODO missing addresses
        options.addOption("a", "action", true, "action name [ add, remove, list, properties, update, remove-msgs ]");
        options.addOption("n", "name", true, "destination name");
        options.addOption("p", "list-properties", true, "print properties of topic (default: true)");
        options.addOption("d", "durable", false, "create durable queue");
        options.addOption(null, "address", true, "bind this queue to this address name");
        options.addOption("s", "selector", true, "apply selector to this queue");
        options.addOption("m", MAX_CONSUMERS, true, "maximum number of consumers on this queue");
        options.addOption(null, PURGE_ON_NO_CONSUMER, true, "delete queue on no connected consumers");

        try {
            cmdLine = parser.parse(options, args);
        } catch (ParseException e) {
            help(options, -1);
        }

        isHelp = cmdLine.hasOption("help");
        jmxURL = cmdLine.getOptionValue('u');
        brokerName = cmdLine.getOptionValue('b');
        destinationName = cmdLine.getOptionValue('n');
        action = cmdLine.getOptionValue('a', LIST_ACTION);
        brokerType = cmdLine.getOptionValue('t', BrokerType.ARTEMIS.getDefaultUpstreamName());
        listProperties = Boolean.parseBoolean(cmdLine.getOptionValue('p', "true"));
        durable = cmdLine.hasOption('d');
        address = cmdLine.getOptionValue("address", null);
        selector = cmdLine.getOptionValue('s');
        host = cmdLine.getOptionValue("host");

        if (cmdLine.hasOption("m")) {
            maxConsumers = Integer.parseInt(cmdLine.getOptionValue("m"));
        }
        if (cmdLine.hasOption(PURGE_ON_NO_CONSUMER)) {
            purgeOnNoConsumers = Boolean.parseBoolean(cmdLine.getOptionValue(PURGE_ON_NO_CONSUMER));
        }

        setCredentials(cmdLine);
        setLogLevel(cmdLine.getOptionValue("log-level", DEFAULT_LOGGING_LEVEL));
    }


    public int run() {
        if (isHelp) {
            help(options, 0);
        } else {
            try {
                ManagerFactory mf = new ManagerFactory();
                DestinationManager queueManager = mf.createDestinationManager(jmxURL, getCredentials(), brokerName, brokerType, host);
                switch (action) {
                    case ADD_ACTION:
                        if (cmdLine.hasOption(MAX_CONSUMERS) || cmdLine.hasOption(PURGE_ON_NO_CONSUMER)) {
                            queueManager.addDestination(destinationName, durable, address, selector, maxConsumers, purgeOnNoConsumers);
                        } else {
                            queueManager.addDestination(destinationName, durable, address, selector);
                        }
                        break;
                    case REMOVE_ACTION:
                        queueManager.removeDestination(destinationName, address);
                        break;
                    case LIST_ACTION:
                        queueManager.listDestinations(listProperties);
                        break;
                    case PROPERTIES_ACTION:
                        queueManager.getDestinationProperties(address, destinationName);
                        break;
                    case UPDATE_ACTION:
                        queueManager.updateDestination(destinationName, durable, address, selector, maxConsumers, purgeOnNoConsumers, RoutingType.ANYCAST.toString());
                        break;
                    case REMOVE_MSGS_ACTION:
                        queueManager.removeMessages(destinationName, address);
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
