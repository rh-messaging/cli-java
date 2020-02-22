package com.redhat.amqx.management;

import com.redhat.amqx.management.exception.DestinationException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.AttributeNotFoundException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

/**
 * Supporting tools for AMQX client.
 */

public class ObjectReader {
    protected static final Logger logger = LoggerFactory.getLogger(ObjectReader.class);

    private static List<String> explicitMethodExcludeList;

    static {
        explicitMethodExcludeList = Arrays.asList("getClass", "getProxyClass", "isProxyClass", "getInvocationHandler");
    }

    private int getPrefixLength(final String methodName) {
        if (methodName.startsWith("get")) {
            return 3;
        } else if (methodName.startsWith("is")) {
            return 2;
        }

        return 0;
    }

    private String getPropertyNameByMethod(final String methodName, int prefixLength) {
        String propertyName = Character.toLowerCase(methodName.substring(prefixLength).charAt(0))
                + methodName.substring(prefixLength + 1);

        // if the remaining part has 2 or less chars, then treat it as an acronym
        if (methodName.length() <= 2) {
            propertyName = methodName.toLowerCase();
        }

        return propertyName;
    }

    /**
     * Call given @method on supplied @object using reflection api. Return the
     * Some methods may be deliberately excluded by supplying their names into excludeMethodList.
     *
     * @param method            to invoke and get information (if result of call is not null)
     * @param object            mbean object (topic/queueViewMBean)
     * @param excludeMethodList methods to be excluded from listing
     * @return Map of method-name and its call result
     */
    private Map<String, Object> extractMethodProperty(Method method, Object object,
                                                      List<String> excludeMethodList) throws DestinationException {
        if (excludeMethodList == null) {
            excludeMethodList = explicitMethodExcludeList;
        } else {
            if (!excludeMethodList.containsAll(explicitMethodExcludeList)) {
                excludeMethodList.addAll(explicitMethodExcludeList);
            }
        }
        // TODO add listDurableSubscription, listNonDurableSubscriptions, listX...?
        Map<String, Object> methodPropertyMap = new HashMap<>();
        String methodName = method.getName();


        int prefixLength = getPrefixLength(methodName);
        if (prefixLength == 0) {
            // skip this method, we are not interested in it
            return methodPropertyMap;
        }

        // get rid of get/is+lowercase first letter
        String propertyName = getPropertyNameByMethod(methodName, prefixLength);


        ProxyHandler proxyHandler = new ProxyHandler(object);

        try {
            if (excludeMethodList.contains(methodName)) {
                return methodPropertyMap;
            }
            Object invokedObject = proxyHandler.invoke(object, method, null);
//            Object invokedObject = method.invoke(object);
            methodPropertyMap.put(propertyName, invokedObject);
        } catch (InvocationTargetException e ) {

            e.printStackTrace();
            if (e.getCause().getCause() instanceof AttributeNotFoundException && excludeMethodList.contains(methodName)) {
//                System.out.println("attr not found, skip me");
                return new HashMap<>();
            }

        } catch (IllegalAccessException | IllegalArgumentException e) {
            throw new DestinationException(
                    String.format("Unable to access '%s' of '%s' object!", object.getClass(), method.getName()));
        } catch (Throwable e) {
            logger.debug(e.getCause().toString());
        }
        return methodPropertyMap;
    }

    /**
     * Format invoked object to either of String, list, dict or None.
     * @param invokedObject
     * @param methodName
     * @return the string returned from the invocation of the method
     */
    protected String formatInvokedObject(Object invokedObject, String methodName) {
        if (invokedObject != null && !invokedObject.equals("")) {
            if (methodName.toLowerCase().contains("json")) {
                return new JSONObject(invokedObject).toString();
            }
            if (isComplexReturnValue(invokedObject)) {
                return getComplexValueAsJSON(invokedObject);
            } else {
                return invokedObject.toString();
            }
        }
        return "None";
    }

    private String getComplexValueAsJSON(Object invokedObject) {
        String dictString;
        if (invokedObject.getClass().isArray()) {
            List<String> tmpList = new ArrayList<>();
            for (Object o : (Object[]) invokedObject) {
                if (isComplexReturnValue(o)) {
                    tmpList.add(getComplexValueAsJSON(o));
                } else {
                    tmpList.add(o.toString());
                }
            }

            dictString = new JSONObject(tmpList).toString();
        } else {
            // TODO if MAP else fail
            dictString = "{TODO, implement me}";
        }
        return dictString;
    }

    private boolean isComplexReturnValue(Object invokedObject) {
        return invokedObject.getClass().isArray() || invokedObject instanceof Collection;
    }

    /**
     * Invoke all possible methods on given object and store method-properties as map.
     *
     * @param object            to call method on
     * @param excludeMethodList methods, to be excluded from invoking on object
     * @return mapping of all key-property
     * @throws DestinationException
     */
    public Map<String, Object> getObjectProperties(Object object, List<String> excludeMethodList) throws DestinationException {
        Map<String, Object> propertiesMap = new HashMap<>();
        Map<String, Object> tmpMap;

        for (Method method : object.getClass().getMethods()) {
            tmpMap = extractMethodProperty(method, object, excludeMethodList);
            if (tmpMap != null && tmpMap.size() != 0) {
                propertiesMap.putAll(tmpMap);
            }
        }
        return propertiesMap;
    }
}

class ProxyHandler implements InvocationHandler {

    private Object delegate;

    public ProxyHandler(Object delegate) {
        this.delegate = delegate;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        ObjectReader.logger.debug("Inside the invocation handler for: " + method.getName());
        try {
            return method.invoke(delegate, args);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }
}
