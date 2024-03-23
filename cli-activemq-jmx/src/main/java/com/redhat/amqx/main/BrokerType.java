package com.redhat.amqx.main;

/**
 * Created by mtoth on 11/4/15.
 */
// TODO convert this to annotation type properties for BrokerType
public enum BrokerType {
    ARTEMIS("service:jmx:rmi:///jndi/rmi://localhost:1099/jmxrmi", "amq", "artemis"),
    ACTIVEMQ("service:jmx:rmi:///jndi/rmi://localhost:1099/karaf-root", "amq", "activemq");

    private final String defaultJMXURL;
    private final String defaultBrokerName;
    private final String defaultUpstreamName;

    BrokerType(String defaultJMXURL, String defaultBrokerName, String upstreamName) {
        this.defaultJMXURL = defaultJMXURL;
        this.defaultBrokerName = defaultBrokerName;
        this.defaultUpstreamName = upstreamName;
    }

    public String getDefaultJMXURL() {
        return defaultJMXURL;
    }

    public String getDefaultBrokerName() {
        return defaultBrokerName;
    }

    public String getDefaultUpstreamName() {
        return defaultUpstreamName;
    }
}
