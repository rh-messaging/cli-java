package com.redhat.amqx.formatters;

/**
 * PythonFormatter supports java -> python string operations magic.
 * This class should be better named as PythonFormatter
 * as it converts all strings to satisfy Python.
 */
public class PythonFormatter implements Formatter {
    /**
     * Convert JSON object to python dict structure as String representation.
     *
     * @param json string of JSON
     * @return python dictionary
     */
    public String convertJSON(String json) {
        // TODO for serialization from JSON to java object use jackson json library
        json = json.replaceAll("\"", "\'");
        json = json.replaceAll(":", ": ");
        json = json.replaceAll(",'", ", '");
        json = json.replaceAll("'?[Nn]one'?", "None");
        json = json.replaceAll("'?[Tt]rue'?", "True");
        json = json.replaceAll("'?[Ff]alse'?", "False");
        return json;
    }
    public void printConvertedJson(String json) {
        System.out.println(convertJSON(json));
    }
}



