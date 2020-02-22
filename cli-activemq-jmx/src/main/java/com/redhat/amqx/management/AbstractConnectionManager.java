package com.redhat.amqx.management;

import com.redhat.amqx.main.BrokerType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Abstract queue manager
 */
public abstract class AbstractConnectionManager {
    private static final Logger logger = LoggerFactory.getLogger(AbstractConnectionManager.class);
    private Resolver<?, ?, ?, ?, ?> resolver;
    private String brokerName;
    private JMXServiceURL jmxServiceURL;
    protected MBeanServerConnection mBeanServerConnection;

    public AbstractConnectionManager(String url, final Credentials credentials, String brokerName, BrokerType brokerType, String hostname) throws IOException {
        if (hostname != null) {
            // use hostname/ip as first connecting priority
            url = createJMXURL(hostname, brokerType.getDefaultJMXURL());
        } else if (url == null) {
            url = brokerType.getDefaultJMXURL();
        }
        // else { use default url};
        jmxServiceURL = new JMXServiceURL(url);

        if (brokerName == null || brokerName.equals("")) {
            this.brokerName = brokerType.getDefaultBrokerName();
        } else {
            this.brokerName = brokerName;
        }

        Map<String, String[]> env = null;

        if (credentials != null) {
            env = new HashMap<>();
            String username = credentials.getUsername();
            String password = credentials.getPassword();

            String[] creds = {username, password};
            env.put(JMXConnector.CREDENTIALS, creds);
        }
        logger.debug("Connecting to '" + url + "'");
        JMXConnector jmxc = JMXConnectorFactory.connect(jmxServiceURL, env);
        mBeanServerConnection = jmxc.getMBeanServerConnection();
        this.resolver = initializeResolver();
    }

    private String createJMXURL(String host, String defaultBrokerJMXURL) {
        // check for IPv6 address
        String ipv6string = "(?<hostname>([a-fA-F0-9]+:)+(:*[a-fA-F0-9]*)?):(?<port>[0-9]+)";
        Pattern ipv6Pattern = Pattern.compile(ipv6string);
        Matcher matcher = ipv6Pattern.matcher(host);
        if (matcher.find()) {
            // found ipv6 address, append opening & closing brackets
            host = '[' + matcher.group("hostname") + "]:" + matcher.group("port");
        }

        return defaultBrokerJMXURL.replaceFirst("jndi/rmi://.*?:[0-9]+", "jndi/rmi://" + host);
    }

    abstract protected Resolver<?, ?, ?, ?, ?> initializeResolver();

    public void viewDomains() throws IOException {
        String[] domains = mBeanServerConnection.getDomains();

        Arrays.sort(domains);
        for (String domain : domains) {
            System.out.println("\nDomain = " + domain);
        }
    }

    public void viewObjects() throws IOException {
        Set<ObjectName> names = new TreeSet<>(mBeanServerConnection.queryNames(null, null));
        for (ObjectName name : names) {
            System.out.println("\nObjectName = " + name);
        }
    }

    public JMXServiceURL getJmxServiceURL() {
        return jmxServiceURL;
    }

    public void setJmxServiceURL(JMXServiceURL jmxServiceURL) {
        this.jmxServiceURL = jmxServiceURL;
    }

    public MBeanServerConnection getMBeanServerConnection() {
        return mBeanServerConnection;
    }

    public void setMBeanServerConnection(MBeanServerConnection mBeanServerConnection) {
        this.mBeanServerConnection = mBeanServerConnection;
    }

    public Resolver<?, ?, ?, ?, ?> getResolver() {
        return resolver;
    }


    protected <T extends Resolver<?, ?, ?, ?, ?>> T getResolver(Class<T> clazz) {
        try {
            return clazz.cast(resolver);
        } catch (ClassCastException cce) {
            logger.error("Unable to typecast class '" + clazz + "' to resolver type!");
            return null;
        }
    }

    protected String getBrokerName() {
        return brokerName;
    }

    // TODO closeResources on exit/error
//    public void closeJMXConnection() {
//
//    }
}
