/*
 * Copyright (c) 2017 Red Hat, Inc.
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.redhat.mqe.lib;

import joptsimple.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Class provides support for parsing supplied options
 * from command line for given client.
 * Each supplied option is checked against template/default
 * client options list, then accordingly set for client.
 */
public abstract class ClientOptionManager {
    private static final Logger LOG = LoggerFactory.getLogger(ClientOptionManager.class);
    /**
     * Mapping from cli options to connection factory properties.
     * Null value means key should not become connection factory property.
     */
    protected final Map<String, String> CONNECTION_TRANSLATION_MAP = new HashMap<>();
    protected Map<String, String> connectionOptionsUrlMap = new HashMap<>();
    private List<Option> updatedOptions = new ArrayList<>();
    protected static final String QUEUE_PREFIX = "queue://";
    protected static final String TOPIC_PREFIX = "topic://";

    private static final String protocol = "(?<protocol>\\w+(?:\\+?\\w+)*://)?";
    private static final String credentials = "(?:(?<username>\\w*)?:?(?<password>\\w*)?@)?";
    private static final String hostname = "(?<hostname>\\[[a-fA-F0-9:]+\\]|[a-zA-Z0-9-_.]+):(?<port>[0-9]+)";
    private static final String query = "(?:\\?(?<query>.*)?)?";

    /**
     * Parses command line arguments and applies them to supplied ClientOptions.
     * <p>
     * ClientOption values are modified when particular client requires it.
     * For example, conversion from seconds to milliseconds.
     * <p>
     * ClientOptions that are derived from other options are also computed and set by this method.
     * Most importantly, this means BROKER_URI.
     *
     * @param clientOptions to be updated
     * @param args          user command line arguments to be parsed
     */
    public void applyClientArguments(ClientOptions clientOptions, String[] args) {
        OptionParser parser = createParser(clientOptions.getClientOptions());
        parseInputOptions(clientOptions, parser, args);
    }

    /**
     * Create jOpt parser from supplied client options.
     *
     * @param options allowed by client
     * @return jOpt parser
     */
    public static OptionParser createParser(List<Option> options) {
        OptionParser parser = new OptionParser();
        for (Option opt : options) {
            if (opt.isCliArgument()) {
                if (opt.getLongOptionName().equals(ClientOptions.HELP)) {
                    parser.acceptsAll(Arrays.asList(opt.getShortOptionName(),
                        opt.getLongOptionName()), opt.getDescription()).isForHelp();
                    continue;
                }

                // username and password options can have no argument
                if (opt.getLongOptionName().equals(ClientOptions.USERNAME)
                    || opt.getLongOptionName().equals(ClientOptions.PASSWORD)) {
                    parser.accepts(opt.getLongOptionName()).withOptionalArg().ofType(String.class).describedAs(opt.getDescription());
                    continue;
                }

                OptionSpecBuilder optionSpecBuilder;
                if (!opt.getShortOptionName().equals("") && !opt.getLongOptionName().equals("")) {
                    optionSpecBuilder = parser.acceptsAll(Arrays.asList(opt.getShortOptionName(),
                        opt.getLongOptionName()), opt.getDescription());
                } else if (opt.getLongOptionName().equals("")) {
                    optionSpecBuilder = parser.accepts(opt.getShortOptionName(), opt.getDescription());
                } else {
                    optionSpecBuilder = parser.accepts(opt.getLongOptionName(), opt.getDescription());
                }

                if (opt.hasArgument()) {
                    ArgumentAcceptingOptionSpec<String> optionSpec = optionSpecBuilder.withRequiredArg().describedAs(opt.getArgumentExample());
                    // TODO possibly all options with arguments should have default values(?)
                    if (!opt.getDefaultValue().equals("")) {
                        optionSpec.defaultsTo(opt.getDefaultValue());
                    }
                }
            }
        }
        return parser;
    }

    /**
     * Parse the options. Print help if "help" was supplied.
     *
     * @param clientOptions supplied client options
     * @param parser        jOpt parser to be used for parsing command line arguemnts
     * @param argv          user command line input
     */
    private void parseInputOptions(ClientOptions clientOptions, OptionParser parser, String[] argv) {
        try {
            OptionSet options = parser.parse(argv);

            if (options.has(ClientOptions.HELP)) {
                printHelp(parser);
                System.exit(0);
            } else {
                // Iterate over options & set client option/s accordingly
                for (Map.Entry<OptionSpec<?>, List<?>> entry : options.asMap().entrySet()) {
                    OptionSpec<?> spec = entry.getKey();
                    if (options.has(spec)) {
                        LOG.trace(spec + " : " + entry.getValue() + " :: " + options.has(spec));
                        // Set parsed option for the client option
                        try {
                            setClientOptions(clientOptions.getOption(getOptionName(spec.options())), options, clientOptions);
                        } catch (JmsMessagingException me) {
                            LOG.error(me.toString(), me.getMessage());
                            me.printStackTrace();
                            System.exit(2);
                        }
                    }
                }
                clientOptions.setUpdatedOptions(updatedOptions);
                createConnectionOptions(clientOptions);
                if (clientOptions.getUpdatedOptionsMap().keySet().contains(ClientOptions.BROKER)
                    || !clientOptions.getUpdatedOptionsMap().keySet().contains(ClientOptions.BROKER_URI)) {
                    checkAndSetOption(ClientOptions.BROKER,
                        clientOptions.getOption(ClientOptions.BROKER).getValue() + getConnectionOptionsAsUrl(),
                        clientOptions);
                }
                // Check if use transactions (set it all accordingly, if yes)
                if (options.has(ClientOptions.TX_SIZE) || options.has(ClientOptions.TX_ACTION) || options.has(ClientOptions.TX_ENDLOOP_ACTION)) {
                    checkAndSetOption(clientOptions.getOption(ClientOptions.TRANSACTED).getName(), "true", clientOptions);

                }
            }
        } catch (OptionException x) {
            System.err.println("Unknown parameter has been detected: " + x.getMessage());
            printHelp(parser);
            System.exit(2);
        }
    }

    protected void createConnectionOptions(ClientOptions clientOptions) {
//    for (Option clientOption : clientOptions.getClientOptions()) {
        for (Option clientOption : clientOptions.getUpdatedOptions()) {
            // OptionName starts with "conn-"
            if (clientOption.getName().startsWith(ClientOptions.CON_HEARTBEAT.substring(0, 4))) {
                addConnectionOptions(clientOption);
            }
        }
    }

    /**
     * Workaround for unmodifiable list, to always get the longOption (= option name).
     *
     * @param options list of current options
     * @return longOption Option name
     */
    private static String getOptionName(List<String> options) {
        List<String> tmp = new ArrayList<>(options);
        if (options.size() > 1) {
            Collections.sort(tmp, new StringLengthComparator());
        }
        return tmp.get(0);
    }

    /**
     * Set client option based on command line arguments.
     * Option.parsedValue is set accordingly.
     *
     * @param clientOption Option of the client
     * @param options      parsed option from command line
     * @throws JmsMessagingException
     */
    private void setClientOptions(Option clientOption, OptionSet options, ClientOptions clientOptions) throws JmsMessagingException {
        // Check for multiple argument input values
        if (options.valuesOf(clientOption.getName()).size() == 1
            && !ClientOptions.argsAcceptingMultipleValues.contains(clientOption.getName())) {
            LOG.trace("Single value: " + options.valueOf(clientOption.getName()));
            String optionName = clientOption.getName();

            switch (optionName) {
                case ClientOptions.BROKER:
                    setBrokerOptions(clientOptions, (String) options.valueOf(optionName));
                    // Set parsed value from options (set without "broker options" only when --broker arg.)
                    // TODO broker uses other (may not be parsed values!) FIX THIS
                    clientOption.setParsedValue(CoreClient.formBrokerUrl(clientOptions));
                    // formatAddress and set type & name for ConnectionManager
                    break;
                case ClientOptions.ADDRESS:
                    String address = (String) options.valueOf(ClientOptions.ADDRESS);
                    if (address.startsWith(TOPIC_PREFIX)) {
                        checkAndSetOption(ClientOptions.DESTINATION_TYPE, ConnectionManager.TOPIC_OBJECT, clientOptions);
                    }
                    if (address.startsWith(TOPIC_PREFIX) || address.startsWith(QUEUE_PREFIX)) {
                        address = address.substring(TOPIC_PREFIX.length()); // len(TOPIC_PREFIX==QUEUE_PREFIX)
                    }
                    checkAndSetOption(ClientOptions.ADDRESS, address, clientOptions);
                    LOG.trace("Address=" + clientOption.getValue());
                    break;
                default:
                    clientOption.setParsedValue(options.valueOf(clientOption.getName()));
                    break;
            }
        } else if (options.valuesOf(clientOption.getName()).size() == 0) {
            if (clientOption.getName().equals(ClientOptions.USERNAME)
                || clientOption.getName().equals(ClientOptions.PASSWORD)) {
                clientOption.setParsedValue(clientOption.getDefaultValue());
            } else {
                // no argument option (switch)
                clientOption.setParsedValue("true");
                LOG.trace("Enabled no argument option: " + clientOption.getName());
            }
        } else {
            // has multiple values, set only allowed types with multiple values
            LOG.trace("Multiple values: " + options.valuesOf(clientOption.getName()));
            if (ClientOptions.argsAcceptingMultipleValues.contains(clientOption.getName())) {
                clientOption.setParsedValue(options.valuesOf(clientOption.getName()));
            } else {
                // if (option not in allowedMultiArgsList):
                throw new JmsMessagingException("Option " + clientOption.getName()
                    + " has wrong argument(s): " + options.valueOf(clientOption.getName()));
            }
        }
        updatedOptions.add(clientOption);
    }

    /**
     * Gather data for protocol,username,password, primary host,port and options
     * provided from 'broker' or 'broker-url' argument.
     *
     * @param clientOptions
     * @param brokerUrl
     */
    protected void setBrokerOptions(ClientOptions clientOptions, String brokerUrl) {
        // TODO new discovery failover has been added
        // TODO failover.nested options for clients has been changed, broker in list can have custom options
        Map<String, String> patternOptionMapping = new HashMap<>();
        patternOptionMapping.put("protocol", ClientOptions.PROTOCOL);
        patternOptionMapping.put("username", ClientOptions.USERNAME);
        patternOptionMapping.put("password", ClientOptions.PASSWORD);
        patternOptionMapping.put("hostname", ClientOptions.BROKER_HOST);
        patternOptionMapping.put("port", ClientOptions.BROKER_PORT);
        patternOptionMapping.put("query", ClientOptions.BROKER_OPTIONS);

        // Search for the first protocol, hostname and port in provided url. Also get username and password if they are provided
        Pattern pattern = Pattern.compile(protocol + credentials + hostname + query);
        Matcher matcher = pattern.matcher(brokerUrl);
        if (matcher.find()) {
            for (String groupName : patternOptionMapping.keySet()) {
                try {
                    if (matcher.group(groupName) != null) {
                        checkAndSetOption(patternOptionMapping.get(groupName), matcher.group(groupName), clientOptions);
                    }
                } catch (IllegalArgumentException e) {
                    LOG.trace("Group {} not found", groupName);
                }
            }
        } else {
            LOG.error("Wrongly parsed BrokerURI:\"" + brokerUrl + "\"");
            System.exit(2);
        }
    }

    /**
     * Append an AMQP protocol to broker url for all possible connecting options (simple, failover, discovery)
     *
     * @param brokerUrl provided broker url by user
     * @return updated broker url with expected amqp protocol
     */
    protected String appendMissingProtocol(String brokerUrl) {
        StringBuilder newBrokerUrl = new StringBuilder();

        String[] brokerUrls = brokerUrl.split(",");
        Pattern pattern = Pattern.compile(protocol + hostname + query);

        String tmpUrl;
        for (String simpleBrokerUrl : brokerUrls) {
            Matcher matcher = pattern.matcher(simpleBrokerUrl);
            if (matcher.find()) {
                if (matcher.group("protocol") == null) {
                    String prefix = getUrlProtocol();
                    tmpUrl = prefix + "://" + matcher.group();
                } else {
                    tmpUrl = matcher.group();
                }
                newBrokerUrl.append(tmpUrl).append(",");
            } else {
                LOG.error("Unable to parse broker-url '" + brokerUrl + "'.");
                System.exit(2);
            }
        }
        newBrokerUrl.deleteCharAt(newBrokerUrl.length() - 1);
        LOG.trace("FinalBrokerUrl=" + newBrokerUrl.toString());
        return newBrokerUrl.toString();
    }

    protected abstract String getUrlProtocol();

    /**
     * Method sets broker options like hostname, port, credentials, protocol.
     * Also sets various simple options like transaction, & other..
     *
     * @param optionName    to be set to ClientOptions (broker_port, broker_host, password, transaction ..)
     * @param value         value of the option
     * @param clientOptions ClientOptions to be used/modified
     */

    protected static void checkAndSetOption(String optionName, String value, ClientOptions clientOptions) {
        LOG.trace(optionName + "->" + value);
        if (value != null) {
            if (value.endsWith("://")) {
                value = value.replace("://", "");
            }
            clientOptions.getOption(optionName).setParsedValue(value);
        }
    }

    /**
     * Merge two maps together. Client specific values are not overridden by default common options.
     *
     * @param commonOpts - default options for clients
     * @param clientOpts - customized options for given client
     * @return client options map, filled with default common options
     */
    public static List<Option> mergeOptionLists(List<Option> commonOpts, List<Option> clientOpts) {
        LOG.trace("OPTS common:" + commonOpts.size() + "+ cli:" + clientOpts.size());
        for (Option commonOption : commonOpts) {
            if (!clientOpts.contains(commonOption)) {
                // add default key-value into clientOpts
                clientOpts.add(commonOption);
            } else {
                LOG.warn("Option " + commonOption + " is already present in the client map. Skip?");
            }
        }
        return clientOpts;
    }

    static void printHelp(OptionParser parser) {
        try {
            // TODO customize help output
            parser.printHelpOn(System.out);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Fill connectionOptionsUrlMap with the data.
     * If needed convert the client input connection option
     * to the appropriate jms connection option.
     * Also, if needed, change seconds to milliseconds.
     */
    void addConnectionOptions(Option option) {
        if (CONNECTION_TRANSLATION_MAP.containsKey(option.getName())) {
            String jmsConOptionNames = CONNECTION_TRANSLATION_MAP.get(option.getName());
            if (jmsConOptionNames != null) {
                for (String jmsConOptionName : jmsConOptionNames.split(";")) {
                    if (option.getName().equals(ClientOptions.CON_HEARTBEAT)) {
                        Integer value = Math.round(Float.parseFloat(option.getValue()) * 1000);
                        connectionOptionsUrlMap.put(jmsConOptionName, value.toString());
                    } else {
                        connectionOptionsUrlMap.put(jmsConOptionName, option.getValue());
                    }
                    if (jmsConOptionName.equals("")) {
                        System.out.println("ignored options: " + option.getName());
                    }
                }
            }
        } else {
            if (option.getName().equals(ClientOptions.CON_RECONNECT)) {
                // use failover mechanism, conn-reconnect does not perform nor add any other action to the client
                return;
            }
            LOG.error("Connection option {} is not recognized! ", option.getName());
            System.exit(2);
        }
    }

//  /**
//   * Create connection url from given options.
//   *
//   * @return string of options starting as "?<conOpts..>"
//   */
//  static String getConnectionOptionsAsUrl() {
//    if (connectionOptionsUrlMap != null) {
//      UriBuilder urlBuilder = UriBuilder.fromPath("");
//      for (String optionName : connectionOptionsUrlMap.keySet()) {
//        urlBuilder.queryParam(optionName, connectionOptionsUrlMap.get(optionName));
//      }
//      return urlBuilder.build().toASCIIString();
//    } else {
//      return "";
//    }
//  }

    /**
     * Create connection url from given options.
     *
     * @return string of options starting as "?<conOpts..>"
     */
    private String getConnectionOptionsAsUrl() {
        StringBuilder conOptUrl = new StringBuilder();
        for (String optionName : connectionOptionsUrlMap.keySet()) {
            String divider = (conOptUrl.length() == 0) ? "?" : "&";
            try {
                conOptUrl
                    .append(divider)
                    .append(URLEncoder.encode(optionName, "UTF-8"))
                    .append("=")
                    .append(URLEncoder.encode(connectionOptionsUrlMap.get(optionName), "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                throw new IllegalArgumentException("Encoding 'UTF-8' is not supported", e);
            }
        }
        return conOptUrl.toString();
    }
}

class StringLengthComparator implements Comparator<String> {
    /**
     * Sort by descending order (from the longest to the shortest).
     *
     * @param s1 first String
     * @param s2 second String
     * @return -1 if s1 is longer, 0 when equally long string, 1 if s2 is longer
     */
    @Override
    public int compare(String s1, String s2) {
        if (s1.length() == s2.length()) return 0;
        return (s1.length() > s2.length()) ? -1 : 1;
    }
}
