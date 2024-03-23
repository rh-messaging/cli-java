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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Class representing one option for client.
 * Option has it's name, default value, short (optional) and long option argument,
 * parsed value, whether it has argument or not, etc..
 */
public class Option {
    private final String name;
    private final String defaultValue;
    private final String longOptionName;
    private final String shortOptionName;
    private final String argumentExample;
    private final String description;
    private boolean isCliArgument = true;
    private boolean hasArgument = false;
    private String parsedValue = null;
    private List<String> parsedValuesList;
    private static final String pattern = "-?\\d+(.\\d+)?";

    /**
     * Special case, for broker host, port, client username, password
     * credentials (masked as broker_url parameter), protocol and other parameters,
     * which are not passed as arguments from command line input for client.
     *
     * @param name         of the option
     * @param defaultValue of the option
     */
    public Option(String name, String defaultValue) {
        this(name, null, null, defaultValue, null);
        isCliArgument = false;
    }

    /**
     * Long option constructor.
     *
     * @param longOptionName  long option string (Mandatory option)
     * @param shortOptionName short option string
     * @param argumentExample depicts the variable for option used in help
     * @param defaultValue    default value (if possible)
     * @param description     of this option
     */
    public Option(String longOptionName, String shortOptionName, String argumentExample, String defaultValue, String description) {
        this.name = longOptionName;
        if (longOptionName == null || longOptionName.isEmpty()) {
            throw new IllegalArgumentException("Long option can not be empty");
        }
        this.longOptionName = longOptionName;
        this.shortOptionName = shortOptionName;
        this.argumentExample = argumentExample;
        this.defaultValue = defaultValue;
        this.description = description;
        if (argumentExample != null && !argumentExample.isEmpty()) {
            this.hasArgument = true;
        }

        if (defaultValue.matches(pattern)) {
            ClientOptions.addNumericArgumentValueOptionList(this);
        }
    }

    public String getName() {
        return name;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    /**
     * Method returns the value which will be used by connection.
     * Either the default one, or overridden by user input.
     *
     * @return String option value to be used for this option. In some
     * cases it can return null, when option has multipleOptions as List.
     * In that case, get values using @method getParsedValuesList().
     */
    public String getValue() {
        if (hasParsedValue()) {
            if (ClientOptions.argsAcceptingMultipleValues.contains(name)) {
                return null;
            } else {
                return parsedValue;
            }
        }
        return defaultValue;
    }

    public String getDescription() {
        return description;
    }

    public String getLongOptionName() {
        return longOptionName;
    }

    public String getShortOptionName() {
        return shortOptionName;
    }

    public String getArgumentExample() {
        return argumentExample;
    }

    public boolean hasArgument() {
        return hasArgument;
    }

    /**
     * The method sets the user input value to the Option.
     * If argument accepts multiple values, set it as parsedValuesList
     * and parsedValue is null. Otherwise set content as parsedValue.
     *
     * @param parsedValue parsed user input for argument value
     */
    @SuppressWarnings("unchecked")
    public void setParsedValue(Object parsedValue) {
        if (parsedValue == null) {
            ClientOptions.LOG.error("Null parsed value! Error while creating Option.parsedValue!");
            System.exit(2);
        }
        // is complex type? list of args
        if (ClientOptions.argsAcceptingMultipleValues.contains(name)) {
            this.parsedValuesList = new ArrayList<>();
            if (parsedValue.equals("''") || parsedValue.equals("\"\"") || parsedValue.equals("")) {
                // send empty message
                parsedValuesList.add("");
            } else {
                for (String item : (List<String>) parsedValue) {
                    parsedValuesList.add(Utils.removeQuotes(item));
                }
                this.parsedValue = null;
            }
        } else {
            // check if the argument value is number
            if (ClientOptions.numericArgumentValueOptionList.contains(this) && !((String) parsedValue).matches(pattern)) {
                throw new IllegalArgumentException("Argument " + name + " has not a numeric value! Found:" + parsedValue);
            }
            this.parsedValue = Utils.removeQuotes((String) parsedValue);
        }
    }

    public boolean hasParsedValue() {
        return (this.parsedValue != null || this.parsedValuesList != null);
    }

    public List<String> getParsedValuesList() {
        return parsedValuesList;
    }

    public boolean isCliArgument() {
        return isCliArgument;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Option option = (Option) o;
        if (!Objects.equals(description, option.description)) return false;
        if (!Objects.equals(longOptionName, option.longOptionName)) return false;
        if (!Objects.equals(name, option.name)) return false;
        if (!Objects.equals(shortOptionName, option.shortOptionName)) return false;
        if (!Objects.equals(defaultValue, option.defaultValue)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (defaultValue != null ? defaultValue.hashCode() : 0);
        result = 31 * result + (longOptionName != null ? longOptionName.hashCode() : 0);
        result = 31 * result + (shortOptionName != null ? shortOptionName.hashCode() : 0);
        result = 31 * result + (description != null ? description.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Option{" +
            "name='" + name + '\'' +
            ", defaultValue='" + defaultValue + '\'' +
            ", longOptionName='" + longOptionName + '\'' +
            ", shortOptionName='" + shortOptionName + '\'' +
            ", argumentExample='" + argumentExample + '\'' +
            ", description='" + description + '\'' +
            ", isCliArgument=" + isCliArgument +
            ", hasArgument=" + hasArgument +
            ", parsedValue='" + parsedValue + '\'' +
            "}\n";
    }
}
