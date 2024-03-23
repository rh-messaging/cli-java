package com.redhat.amqx.main;

import com.redhat.amqx.main.actions.*;

import java.util.Arrays;

/**
 * Program main class
 */
public class Main {
    private static final String ADDRESS = "address";
    private static final String JMS_TOPIC = "topic";
    private static final String DESTINATION = "queue";
    private static final String BROKER = "broker";
    private static final String DIVERT = "divert";
    private static final String VERSION_CMD = "--version";
    private static final String VERSION = "1.2.5-SNAPSHOT";

    private static void help(int code) {
        System.out.println("Usage: amqx <object> --host <brokerIp>:<port> --action <action> [--name <destinationName>]\n");

        System.out.printf("Objects:    {%s, %s, %s, %s, %s}\n", DESTINATION, JMS_TOPIC, ADDRESS, BROKER, DIVERT);
        System.out.printf("Actions:    Destination {%s, %s, %s, %s}\n", QueueAction.ADD_ACTION,
            QueueAction.REMOVE_ACTION, QueueAction.LIST_ACTION, QueueAction.PROPERTIES_ACTION);
        // TODO finish broker actions
        System.out.printf("            Broker {%s, %s, %s, %s}\n\n", BrokerAction.RELOAD_ACTION, BrokerAction.CONNECTORS_ACTION,
            BrokerAction.LIST_ACTION, BrokerAction.PROPERTIES_ACTION);
        System.out.println("'<Object> -h' for more details\n");
        System.out.printf("'%s' for version information\n", VERSION_CMD);

        System.exit(code);
    }


    public static void main(String[] args) {
        int ret = 0;

        if (args.length == 0) {
            help(2);
        }

        String first = args[0];
        String[] newArgs = Arrays.copyOfRange(args, 1, args.length);

        if (first.equals("--help") || first.equals("-h")) {
            help(0);
        }

        switch (first) {
            case DESTINATION:
                QueueAction destinationAction = new QueueAction(newArgs);
                System.exit(destinationAction.run());
                break;
            case ADDRESS:
                AddressAction queueAction = new AddressAction(newArgs);
                System.exit(queueAction.run());
                break;
            case JMS_TOPIC:
                TopicAction topicAction = new TopicAction(newArgs);
                System.exit(topicAction.run());
                break;
            case BROKER:
                BrokerAction brokerAction = new BrokerAction(newArgs);
                System.exit(brokerAction.run());
                break;
            case DIVERT:
                DivertAction divertAction = new DivertAction(newArgs);
                System.exit(divertAction.run());
            case VERSION_CMD:
                System.out.println(VERSION);
                System.exit(ret);
                break;
            default:
                System.out.println(String.format("Invalid action '%s'. Exiting", first));
                help(2);
                break;
        }
    }
}
