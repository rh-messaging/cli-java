/*
 * Copyright (c) 2022 Red Hat, Inc.
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

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;

import java.util.function.Consumer;

/**
 * Utility class to configure log messages
 * <p>
 * <a href="https://logging.apache.org/log4j/2.x/manual/api.html">...</a>
 * <a href="https://logging.apache.org/log4j/2.x/manual/customconfig.html">...</a>
 */
public class LogConfigurator {
    /**
     * Restricted constructor
     */
    private LogConfigurator() {
    }


    private static void configureCommon(Consumer<LoggerConfig> customizeConfig) {
        final LoggerContext context = LoggerContext.getContext(false);
        final Configuration config = context.getConfiguration();
        LoggerConfig rootLogger = config.getRootLogger();

        customizeConfig.accept(rootLogger);

        context.updateLoggers();
    }

    public static void trace() {
        configureCommon((LoggerConfig config) -> config.setLevel(Level.TRACE));
    }

    /**
     * Configure the output to be at debug level
     */
    public static void debug() {
        configureCommon((LoggerConfig config) -> config.setLevel(Level.DEBUG));
    }

    /**
     * Configure the output to be at info (info) level
     */
    public static void info() {
        configureCommon((LoggerConfig config) -> config.setLevel(Level.INFO));
    }

    /**
     * Configure the output to be as error as possible
     */
    public static void error() {
        configureCommon((LoggerConfig config) -> config.setLevel(Level.ERROR));
    }
}
