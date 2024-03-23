package com.redhat.amqx.management.activemq;

import com.redhat.amqx.formatters.Formatter;
import com.redhat.amqx.formatters.PythonFormatter;
import com.redhat.amqx.main.BrokerType;
import com.redhat.amqx.management.ObjectReader;
import com.redhat.amqx.management.AbstractConnectionManager;
import com.redhat.amqx.management.Credentials;
import com.redhat.amqx.management.Resolver;
import com.redhat.amqx.management.exception.DestinationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.ObjectName;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.activemq.broker.jmx.DestinationViewMBean;

/**
 * Provides a common interface for managing AMQ endpoints
 */
// TODO add here annotations instead of using of BrokerType
abstract class AbstractActiveMQManager extends AbstractConnectionManager {
    private static final Logger logger = LoggerFactory.getLogger(AbstractActiveMQManager.class);
    private static final ObjectReader objectReader = new ObjectReader();
    protected final Formatter formatter = new PythonFormatter();

    public AbstractActiveMQManager(String url, final Credentials credentials, String brokerName, String hostname) throws IOException {
        super(url, credentials, brokerName, BrokerType.ACTIVEMQ, hostname);
    }

    @Override
    protected Resolver<?, ?, ?, ?, ?> initializeResolver() {
        return new ActiveMQResolver(mBeanServerConnection, getBrokerName());
    }

    /**
     * Get properties from given object (mbean)
     * @param objectNames array of objects to get properties from
     * @param keyProperty name of the property to look for
     * @return list of properties
     */
    protected List<String> getKeyProperty(ObjectName[] objectNames, String keyProperty) {
        List<String> names = new ArrayList<>();
        for (ObjectName name : objectNames) {
            names.add(name.getKeyProperty(keyProperty));
        }
        return names;
    }

    /**
     * Get name of the destination using key property on object
     * @param objectName object to get name from
     * @return name of the destination
     */
    protected String getDestinationName(ObjectName objectName) {
        ObjectName[] objectNames = {objectName};
        return getKeyProperty(objectNames, "destinationName").get(0);
    }

    abstract protected ObjectName[] getObjectNames() throws Exception;

    /**
     * List all destinations with their properties as map representation (name - properties dict.
     *
     * @return Mapping of Destination:Dict<properties>
     * @throws Exception
     */
    protected Map<String, String> listDestinationsWithProperties() throws Exception {
        ObjectName[] names = getObjectNames();
        Map<String, String> destMap = new HashMap<>();
        for (ObjectName destination : names) {
            String destinationName = getDestinationName(destination);
            String formattedProperties = getFormattedDestinationProperties(destinationName);
            destMap.put(destinationName, formattedProperties);
        }
        return destMap;
    }

    protected List<String> listDestinationsWithoutProperties() throws Exception {
        ObjectName[] names = getObjectNames();
        List<String> destinationList = new ArrayList<>();
        for (ObjectName destination : names) {
            destinationList.add(getDestinationName(destination));
        }
        return destinationList;
    }

    /**
     * Check whether destination exists.
     * @param names all destinations mbean objects
     * @param name of the checking for existance destination
     * @return true if destination exists
     */
    protected boolean destinationExists(ObjectName[] names, String name) {
        for (ObjectName objectName : names) {
            String destinationName = objectName.getKeyProperty("destinationName");

            if (destinationName.equals(name)) {
                return true;
            }
        }
        return false;
    }


    abstract protected String getFormattedDestinationProperties(String destinationName) throws Exception;

    /**
     * Get the properties of given destination type viewMBean by calling
     * all getters and setters on given DestinationMBean object using reflection api.
     *
     * @param objectMBean destination
     * @return the properties of this object as
     */
    protected Map<String, Object> getDestinationProperties(final DestinationViewMBean objectMBean)
            throws IllegalAccessException, InvocationTargetException, DestinationException {
        List<String> excludeList = new ArrayList<>();
        excludeList.add("getMessage");
        excludeList.add("getNotificationInfo"); // worked when empty list. TODO: write here why is it excluded
        excludeList.add("getStoreMessageSize"); // javax.management.AttributeNotFoundException: No such attribute: StoreMessageSize
        return objectReader.getObjectProperties(objectMBean, excludeList);
    }
}
