package com.redhat.amqx.formatters;

/**
 * Created by mtoth on 11/29/16.
 */
public interface Formatter {

    /**
     * Convert given string to given type of implementing formatter.
     * @param string
     * @return string format of given type
     */
    String convertJSON(String string);
}
