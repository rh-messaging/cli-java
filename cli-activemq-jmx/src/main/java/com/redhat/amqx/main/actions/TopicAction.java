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
public class TopicAction extends AbstractAction {
    private CommandLine cmdLine;
    private boolean isHelp;
    private String jmxURL;
    private String brokerName;
    private String topicName;
    private String action;
    private String brokerType;
    private boolean listProperties;
    private String host;
    private String address;
    private boolean durable;
    private String selector;
    private int maxConsumers;
    private boolean purgeNoConsumers;

    public TopicAction(String[] args) {
        processCommand(args);
    }

    /**
     * Processes the command-line arguments
     *
     * @param args the command line arguments
     */
    protected void processCommand(String[] args) {
        CommandLineParser parser = new DefaultParser();

        options.addOption("a", "action", true, "action name [ add, remove, list, properties, update, remove-msgs ]");
        options.addOption("n", "name", true, "topic name");
        options.addOption("p", "list-properties", true, "print properties of topic (default: true)");
        options.addOption("address", true, "bind this topic to this address name");

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
        topicName = cmdLine.getOptionValue('n');
        action = cmdLine.getOptionValue('a', LIST_ACTION);
        brokerType = cmdLine.getOptionValue('t', BrokerType.ARTEMIS.getDefaultUpstreamName());
        listProperties = Boolean.parseBoolean(cmdLine.getOptionValue('p', "true"));
        host = cmdLine.getOptionValue("host");
        address = cmdLine.getOptionValue("address", null);

        durable = cmdLine.hasOption('d');
        selector = cmdLine.getOptionValue('s');
        if (cmdLine.hasOption("m")) {
            maxConsumers = Integer.parseInt(cmdLine.getOptionValue("m"));
        }
        if (cmdLine.hasOption(PURGE_ON_NO_CONSUMER)) {
            purgeNoConsumers = Boolean.parseBoolean(cmdLine.getOptionValue(PURGE_ON_NO_CONSUMER));
        }

        setLogLevel(cmdLine.getOptionValue("log-level", DEFAULT_LOGGING_LEVEL));
        setCredentials(cmdLine);
    }


    public int run() {
        if (isHelp) {
            help(options, 0);
        } else {
            try {
                ManagerFactory mf = new ManagerFactory();
                DestinationManager topicManager = mf.createTopicManager(jmxURL, getCredentials(), brokerName, brokerType, host);
                switch (action) {
                    case ADD_ACTION:
                        if (cmdLine.hasOption(MAX_CONSUMERS) || cmdLine.hasOption(PURGE_ON_NO_CONSUMER)) {
                            topicManager.addDestination(topicName, durable, address, selector, maxConsumers, purgeNoConsumers);
                        } else {
                            topicManager.addDestination(topicName, durable, address, selector);
                        }
                        break;
                    case REMOVE_ACTION:
                        topicManager.removeDestination(topicName, address);
                        break;
                    case LIST_ACTION:
                        topicManager.listDestinations(listProperties);
                        break;
                    case PROPERTIES_ACTION:
                        topicManager.getDestinationProperties(address, topicName);
                        break;
                    case UPDATE_ACTION:
                        topicManager.updateDestination(topicName, durable, address, selector, maxConsumers, purgeNoConsumers, RoutingType.MULTICAST.toString());
                        break;
                    case REMOVE_MSGS_ACTION:
                        topicManager.removeMessages(topicName, address);
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
