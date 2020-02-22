package com.redhat.amqx.management.artemis;

import com.redhat.amqx.management.ObjectReader;
import org.apache.activemq.artemis.api.core.SimpleString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Artemis-specific object reader
 */
class ArtemisObjectReader extends ObjectReader {
    private static final Logger logger = LoggerFactory.getLogger(ArtemisObjectReader.class);

    @Override
    protected String formatInvokedObject(Object invokedObject, String methodName) {
        String ret;
        if (invokedObject instanceof SimpleString) {
            logger.error("SimpleString=" + invokedObject);
            ret = invokedObject.toString();
        }
        else {
            ret = super.formatInvokedObject(invokedObject, methodName);

        }
        return ret;
    }
}
