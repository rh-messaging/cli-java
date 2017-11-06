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

import java.lang.reflect.InvocationTargetException;


/**
 * Create a content object from given input value.
 * Input value is a parsed String from *content* options.
 * Content object will try to auto-typecast values, if "~"
 * is found in the beginning of the item or in the map.
 * (~item, key~value).
 * Auto-typecasting to Integer, Long, Float, Double, Boolean
 * and String is done only when specifically wanted (provide ~).
 * It is ignored, when content type is provided.
 */
public class Content {
    private String key;
    private Object value;
    private Class<?> type;
    private boolean isMap;

    /**
     * Create content from defined rules.
     * Content can be created from single item (value), list of items or
     * a list of items as map (key=value).
     * If content type is explicitly provided no casting is done and content type is used.
     * Otherwise we use String as default content type.
     * Special scenario is when value is prepended with '~'. In this case, we try to
     * auto-typecast the value to some of the basic data types (see field Utils.CLASSES).
     *
     * @param contentType type of the content (String or auto-typecasted if not provided)
     * @param parsedValue value to be used for casting
     * @param isMap       whether we set key or not
     */
    public Content(String contentType, String parsedValue, boolean isMap) {
        String val;
        boolean allowExplicitRetype = true;
        this.isMap = isMap;
        try {
            if (isMap) {
                String splitValue = "";
                String splitter = "=";
                if (parsedValue.contains("=") && parsedValue.contains("~")) {
                    if (parsedValue.indexOf("=") < parsedValue.indexOf("~")) {
                        allowExplicitRetype = false;
                        splitValue = parsedValue.substring(parsedValue.indexOf(splitter) + 1);
                    }
                } else if (parsedValue.contains("=")) {
                    // last argument 'allowExplicityRetype' can be omitted as parsedValue will not by autotypecasted - no '~'
                    splitValue = parsedValue.substring(parsedValue.indexOf(splitter) + 1);
                } else {
                    splitter = "~";
                    splitValue = parsedValue.substring(parsedValue.indexOf(splitter));
                }
                this.type = Utils.getClassType(contentType, splitValue, allowExplicitRetype);

                this.key = parsedValue.substring(0, parsedValue.indexOf(splitter));
                val = parsedValue.substring(parsedValue.indexOf(splitter) + 1);
            } else {
                if (parsedValue.startsWith("~~")) {
                    contentType = "String";
                }
                this.type = Utils.getClassType(contentType, parsedValue, true);
                val = parsedValue;
            }
            try {
                this.value = Utils.getObjectValue(type, val, allowExplicitRetype);
            } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
                e.printStackTrace();
            }
        } catch (JmsMessagingException e) {
            e.printStackTrace();
        }
    }

    public String getKey() {
        return key;
    }

    public Object getValue() {
        return value;
    }

    public Class<?> getType() {
        return type;
    }

    public boolean isMap() {
        return isMap;
    }

    @Override
    public String toString() {
        return "Content{" +
            "key='" + key + '\'' +
            ", value=" + value +
            ", type=" + type +
            ", isMap=" + isMap +
            '}';
    }
}
