package com.redhat.amqx.management.activemq;

import com.redhat.amqx.main.BrokerType;
import com.redhat.amqx.management.BrokerManager;
import com.redhat.amqx.management.Credentials;
import com.redhat.amqx.management.Resolver;
import org.json.JSONObject;
import org.apache.activemq.broker.jmx.BrokerViewMBean;
import org.apache.activemq.plugin.jmx.RuntimeConfigurationViewMBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.JMX;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.io.IOException;

/**
 * This class uses JMX to manage the broker (via broker itself or plugins)
 */
public class BrokerActiveMQManager extends AbstractActiveMQManager implements BrokerManager {
    private static final Logger logger = LoggerFactory.getLogger(BrokerActiveMQManager.class);

    public BrokerActiveMQManager(final String url, final Credentials credentials, String brokerName, String host) throws IOException {
        super(url, credentials, brokerName, host);
    }

    private RuntimeConfigurationViewMBean getRunTimeConfigurationViewMBean() throws MalformedObjectNameException {
        ObjectName mbeanName = new ObjectName(
                "org.apache.activemq:type=Broker,brokerName=" + getBrokerName() +
                        ",service=RuntimeConfiguration,name=Plugin");

        return JMX.newMBeanProxy(getMBeanServerConnection(), mbeanName, RuntimeConfigurationViewMBean.class, true);
    }

    public void reload() throws MalformedObjectNameException {
        RuntimeConfigurationViewMBean viewMBean = getRunTimeConfigurationViewMBean();

        System.out.print("Updating now ... ");
        viewMBean.updateNow();
        System.out.println("done!");
    }

    /**
     * Get a list of transport connectors
     * @throws Exception
     */
    public void getTransportConnectors() throws Exception {
        BrokerViewMBean brokerViewMBean = getResolver(ActiveMQResolver.class).getBrokerView();

        logger.debug(new JSONObject(brokerViewMBean.getTransportConnectors()).toString());
    }

    @Override
    public void getNetworkTopology() throws Exception {
        logger.error("Not implemented!");
    }

    @Override
    public void getSessions(String connectionId) throws Exception {
        logger.error("Not implemented!");
    }

    @Override
    public BrokerType getBrokerType() {
        return BrokerType.ACTIVEMQ;
    }

    @Override
    public void getAllBrokerDestinations() throws Exception {
        // TODO
        logger.error("Not implemented!");
    }

    @Override
    public void getAllBrokerDestinationsProperties() throws Exception {
        // TODO
        logger.error("Not implemented!");
    }

    @Override
    protected Resolver<?, ?, ?, ?, ?> initializeResolver() {
        return new ActiveMQResolver(mBeanServerConnection, getBrokerName());
    }

    @Override
    protected ObjectName[] getObjectNames() throws Exception {
        return new ObjectName[0];
    }

    @Override
    protected String getFormattedDestinationProperties(String destinationName) throws Exception {
        return null;
    }
}
