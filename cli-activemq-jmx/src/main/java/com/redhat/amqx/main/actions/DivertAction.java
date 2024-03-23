package com.redhat.amqx.main.actions;

import com.redhat.amqx.main.BrokerType;
import com.redhat.amqx.management.ManagerFactory;
import com.redhat.amqx.management.artemis.DivertArtemisManager;
import org.apache.activemq.artemis.core.server.ComponentConfigurationRoutingType;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.ParseException;

import java.io.IOException;

/**
 * QueueAction class manages destinations in Artemis/AMQ7 broker.
 * It does use core protocol so concept of addresses and queues/bindings are used.
 * There are no (JMS) queues or topics. Use {Queue,Topic}ArtemisManager for JMS objects.
 */
// TODO extend AddressAction is possible
public class DivertAction extends AbstractAction {

    private CommandLine cmdLine;
    private boolean isHelp;
    private String jmxURL;
    private String brokerName;
    private String divertName;
    private String action;
    private String brokerType;
    private boolean listProperties;
    private boolean exclusive;
    private String routingName;
    private String address;
    private String selector;
    private String host;
    private int maxConsumers = -1;  // default value of maxConsumers
    private boolean purgeOnNoConsumers = false;
    private String forwardingAddress;
    String divertRoutingType;

    public DivertAction(String[] args) {
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
        options.addOption("a", "action", true, "action name [ add, remove, list ]");
        options.addOption("n", "name", true, "destination name");
        options.addOption("p", "list-properties", true, "print properties of topic (default: true)");
        options.addOption(null, "address", true, "bind this queue to this address name");
        options.addOption("s", "selector", true, "apply selector to this queue");
        options.addOption("e", "exclusive", true, "whether this divert is exclusive or not");
        options.addOption("f", "forwarding-addr", true, "forwarding address for this divert");
        options.addOption("d", "divert-routing", true, "divert routing type [ multicast, anycast, strip, pass ]");
        options.addOption("o", "routing-addr", true, "routing name ");

        try {
            cmdLine = parser.parse(options, args);
        } catch (ParseException e) {
            help(options, -1);
        }

        isHelp = cmdLine.hasOption("help");
        jmxURL = cmdLine.getOptionValue('u');
        brokerName = cmdLine.getOptionValue('b');
        divertName = cmdLine.getOptionValue('n');
        action = cmdLine.getOptionValue('a', LIST_ACTION);
        brokerType = cmdLine.getOptionValue('t', BrokerType.ARTEMIS.getDefaultUpstreamName());
        listProperties = Boolean.parseBoolean(cmdLine.getOptionValue('p', "true"));
        address = cmdLine.getOptionValue("address", null);
        selector = cmdLine.getOptionValue('s');
        host = cmdLine.getOptionValue("host");
        routingName = cmdLine.getOptionValue("o");
        forwardingAddress = cmdLine.getOptionValue("f");
        exclusive = Boolean.parseBoolean(cmdLine.getOptionValue("e"));

        if (cmdLine.hasOption("d")) {
            divertRoutingType = resolveDivertRoutingType(cmdLine.getOptionValue("d"));
        } else {
            divertRoutingType = ComponentConfigurationRoutingType.STRIP.toString();
        }

        if (cmdLine.hasOption("m")) {
            maxConsumers = Integer.parseInt(cmdLine.getOptionValue("m"));
        }
        if (cmdLine.hasOption(PURGE_ON_NO_CONSUMER)) {
            purgeOnNoConsumers = Boolean.parseBoolean(cmdLine.getOptionValue(PURGE_ON_NO_CONSUMER));
        }

        setCredentials(cmdLine);
        setLogLevel(cmdLine.getOptionValue("log-level", DEFAULT_LOGGING_LEVEL));
    }

    private String resolveDivertRoutingType(String divertRoutingTypeOption)  {
        switch(divertRoutingTypeOption) {
            case "multicast":
                return ComponentConfigurationRoutingType.MULTICAST.toString();
            case "anycat":
                return ComponentConfigurationRoutingType.ANYCAST.toString();
            case "strip":
                return ComponentConfigurationRoutingType.STRIP.toString();
            case "pass":
                return ComponentConfigurationRoutingType.PASS.toString();

            default:
                logger.error("Unknown divert routing type: '" + divertRoutingTypeOption + "'. Exitting.");
                return null;

        }
    }


    public int run() {
        if (isHelp) {
            help(options, 0);
        } else {
            try {
                ManagerFactory mf = new ManagerFactory();
                DivertArtemisManager divertManager = mf.createDivertManager(jmxURL, getCredentials(), brokerName, brokerType, host);
                switch (action) {
                    case ADD_ACTION:
                        divertManager.addDivert(divertName, routingName, address, forwardingAddress, exclusive, selector, divertRoutingType);
                        break;
                    case REMOVE_ACTION:
                        divertManager.removeDivert(divertName, address);
                        break;
                    case LIST_ACTION:
                        divertManager.listDiverts(listProperties);
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
